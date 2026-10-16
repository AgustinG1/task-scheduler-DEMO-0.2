[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$BackupDirectory)

# Solo ensayo local: no acepta host, puerto, password ni cadena JDBC externos.
$ErrorActionPreference = 'Stop'
$migrationRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../backups/mysql-auth')).TrimEnd('\', '/')
$migrationDirectory = (Resolve-Path -LiteralPath $BackupDirectory).Path.TrimEnd('\', '/')
if (-not $migrationDirectory.StartsWith(($migrationRoot + [IO.Path]::DirectorySeparatorChar), [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Solo se admite una subcarpeta de backups/mysql-auth.'
}
$migrationManifest = Get-Content -LiteralPath (Join-Path $migrationDirectory 'backup.json') -Raw | ConvertFrom-Json
if ($migrationManifest.restoreVerified -ne $true -or $migrationManifest.database -ne 'defaultdb' -or
    $migrationManifest.dumpFile -ne 'database.sql' -or
    (Get-FileHash -LiteralPath (Join-Path $migrationDirectory 'database.sql') -Algorithm SHA256).Hash -ne $migrationManifest.sha256) {
    throw 'El respaldo no esta verificado o su hash cambio.'
}
$migrationContainer = [string]$migrationManifest.restoredContainer
if ($migrationContainer -notmatch '^task-mysql-auth-[a-f0-9]{12}$') { throw 'Nombre de contenedor no valido.' }
$migrationInspectJson = & docker inspect $migrationContainer
if ($LASTEXITCODE -ne 0) { throw 'No se encuentra el contenedor restaurado.' }
$migrationInspect = ($migrationInspectJson | ConvertFrom-Json)[0]
if ($migrationInspect.Config.Labels.'com.taskapp.purpose' -ne 'auth-migration-rehearsal' -or
    $migrationInspect.HostConfig.NetworkMode -ne 'none' -or
    @($migrationInspect.HostConfig.PortBindings.PSObject.Properties).Count -ne 0 -or
    $migrationInspect.State.Running -ne $true -or
    '--skip-networking' -notin $migrationInspect.Config.Cmd) {
    throw 'Se exige nuestro contenedor local, activo, sin red, puertos ni TCP.'
}
$migrationReport = Join-Path $migrationDirectory 'owner-migration.json'
if (Test-Path -LiteralPath $migrationReport) { throw 'Ya hay un ensayo iniciado. Restaurar otra copia; no reintentar DDL parcialmente aplicado.' }

function Invoke-MigrationQuery([string]$Sql) {
    $migrationOutput = @(& docker exec $migrationContainer mysql --user=root --batch --skip-column-names --default-character-set=utf8mb4 defaultdb --execute $Sql)
    if ($LASTEXITCODE -ne 0) { throw 'Fallo SQL del ensayo local; conservar el contenedor para diagnostico.' }
    return $migrationOutput
}
function Invoke-MigrationFile([string]$Path) {
    & docker cp $Path ($migrationContainer + ':/tmp/auth-migration.sql') | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Fallo al copiar SQL al contenedor.' }
    $migrationOutput = @(& docker exec $migrationContainer sh -c 'mysql --user=root --batch --default-character-set=utf8mb4 defaultdb < /tmp/auth-migration.sql')
    if ($LASTEXITCODE -ne 0) { throw ('Fallo SQL en ' + [IO.Path]::GetFileName($Path) + '; no reintentar sin revisar.') }
    return $migrationOutput
}
$migrationTables = @(Invoke-MigrationQuery "SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' ORDER BY table_name")
$migrationExpectedTables = @('areas','asignaciones','catalogo_tarea_detalle','catalogos_tareas','empleado_equipo','empleados','equipo_area','equipo_catalogo','equipos','planillas','tarea_area','tarea_equipo','tareas')
if (($migrationTables -join ',') -ne ($migrationExpectedTables -join ',')) { throw 'No coincide la linea base de 13 tablas sin AppUser.' }
if ([int](@(Invoke-MigrationQuery "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND column_name='owner_id'")[0]) -ne 0) {
    throw 'La copia ya contiene propietarios: volver a restaurar antes del ensayo.'
}
[ordered]@{startedUtc=[DateTime]::UtcNow.ToString('o'); container=$migrationContainer; dumpSha256=$migrationManifest.sha256; complete=$false} |
    ConvertTo-Json | Set-Content -LiteralPath $migrationReport -Encoding UTF8

# Una segunda base privada dentro del MISMO contenedor permite comparar todos los
# campos originales y todas las filas, sin exportar sus valores a logs/evidencias.
Invoke-MigrationQuery 'CREATE DATABASE auth_before CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci' | Out-Null
$migrationChecks = @()
$migrationResults = @()
foreach ($migrationTable in $migrationTables) {
    if ($migrationTable -notmatch '^[a-z_]+$') { throw 'Identificador de tabla inesperado.' }
    $migrationColumns = @(Invoke-MigrationQuery ("SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='" + $migrationTable + "' ORDER BY ordinal_position"))
    $migrationKeys = @(Invoke-MigrationQuery ("SELECT column_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='" + $migrationTable + "' AND index_name='PRIMARY' ORDER BY seq_in_index"))
    if ($migrationKeys.Count -eq 0) { throw 'La comparacion requiere clave primaria.' }
    foreach ($migrationColumn in $migrationColumns) { if ($migrationColumn -notmatch '^[a-z_]+$') { throw 'Columna inesperada.' } }
    Invoke-MigrationQuery ('CREATE TABLE auth_before.' + $migrationTable + ' LIKE ' + $migrationTable + '; INSERT INTO auth_before.' + $migrationTable + ' SELECT * FROM ' + $migrationTable) | Out-Null
    $migrationJoin = ($migrationKeys | ForEach-Object { 'a.' + $_ + '=b.' + $_ }) -join ' AND '
    $migrationDifferences = ($migrationColumns | ForEach-Object { 'NOT(HEX(a.' + $_ + ') <=> HEX(b.' + $_ + '))' }) -join ' OR '
    $migrationChecks += [pscustomobject]@{table=$migrationTable; count=[long](@(Invoke-MigrationQuery ('SELECT COUNT(*) FROM ' + $migrationTable))[0]); sql=(
        'SELECT COUNT(*) FROM auth_before.' + $migrationTable + ' b LEFT JOIN ' + $migrationTable + ' a ON ' + $migrationJoin +
        ' WHERE a.' + $migrationKeys[0] + ' IS NULL OR ' + $migrationDifferences)}
}
foreach ($migrationFile in @('001_create_app_user.mysql.sql','002_add_owners.mysql.sql','003_assign_public_owner.mysql.sql','004_require_owners.mysql.sql')) {
    Invoke-MigrationFile (Join-Path $PSScriptRoot ('migrations/' + $migrationFile)) | Out-Null
    Write-Host ('Aplicado en copia local: ' + $migrationFile)
}
foreach ($migrationCheck in $migrationChecks) {
    $migrationCount = [long](@(Invoke-MigrationQuery ('SELECT COUNT(*) FROM ' + $migrationCheck.table))[0])
    $migrationChanged = [long](@(Invoke-MigrationQuery $migrationCheck.sql)[0])
    if ($migrationCount -ne $migrationCheck.count -or $migrationChanged -ne 0) { throw ('Cambios historicos inesperados en ' + $migrationCheck.table) }
    $migrationResults += [ordered]@{table=$migrationCheck.table; rows=$migrationCount; changedOriginalRows=$migrationChanged}
    Write-Host ($migrationCheck.table + ': ' + $migrationCount + ' filas; campos originales identicos')
}
$migrationIssuerBytes = [Text.Encoding]::UTF8.GetBytes('https://dev-ddgeakv1hckqsd7z.us.auth0.com/')
$migrationLengthBytes = [BitConverter]::GetBytes([int]$migrationIssuerBytes.Length)
if ([BitConverter]::IsLittleEndian) { [Array]::Reverse($migrationLengthBytes) }
$migrationDigest = [Security.Cryptography.SHA256]::Create()
try {
    $migrationHash = ([BitConverter]::ToString($migrationDigest.ComputeHash([byte[]]($migrationLengthBytes + $migrationIssuerBytes + [Text.Encoding]::UTF8.GetBytes('auth0|6ad1611ca88ce64aa99f1dda'))))).Replace('-','').ToLowerInvariant()
} finally { $migrationDigest.Dispose() }
if ([int](@(Invoke-MigrationQuery ("SELECT COUNT(*) FROM app_user WHERE identity_key='" + $migrationHash + "' AND active=1"))[0]) -ne 1) { throw 'No coincide el hash de identidad entre SQL y formato Java.' }
$migrationOwnedTables = @('areas','empleados','tareas','catalogos_tareas','equipos','planillas')
foreach ($migrationTable in $migrationOwnedTables) {
    if ([int](@(Invoke-MigrationQuery ('SELECT COUNT(*) FROM ' + $migrationTable + " t LEFT JOIN app_user u ON u.id=t.owner_id WHERE u.id IS NULL OR u.identity_key<>'" + $migrationHash + "'"))[0]) -ne 0) { throw 'Los registros no pertenecen todos a la identidad publica.' }
}
if ([int](@(Invoke-MigrationQuery "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND column_name='owner_id' AND is_nullable='NO'")[0]) -ne 6) { throw 'Falta NOT NULL de propietario.' }
[ordered]@{completedUtc=[DateTime]::UtcNow.ToString('o'); container=$migrationContainer; dumpSha256=$migrationManifest.sha256;
    complete=$true; sourceModified=$false; originalTables=$migrationResults; ownerColumnsNotNull=6;
    publicIdentityKey=$migrationHash; allHistoricalRowsOwnedByPublic=$true; relationshipsChecked=$true;
    limitations=@('Ensayo SQL local; no cambia Aiven ni despliega la app.','La coherencia entre propietarios en futuras escrituras depende de los servicios de la app.','Pendiente validar Hibernate y rutas reales sobre MySQL.') } |
    ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $migrationReport -Encoding UTF8
Write-Host 'Ensayo SQL completado: datos originales conservados y propietarios asignados solo en la copia local.'
