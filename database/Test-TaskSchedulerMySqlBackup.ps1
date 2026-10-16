[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$BackupDirectory
)

$ErrorActionPreference = 'Stop'
$restoreDocker = Get-Command docker -ErrorAction Stop
$restoreImage = 'mysql@sha256:2952e3be7807f06fc18de50b3ea1a632d5c70d63482ff7d7376fe3aa8999babf'
$restoreBackupRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../backups/mysql-auth'))
$restoreDirectory = (Resolve-Path -LiteralPath $BackupDirectory -ErrorAction Stop).Path.TrimEnd('\', '/')
if (-not $restoreDirectory.StartsWith(($restoreBackupRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar), [StringComparison]::OrdinalIgnoreCase)) {
    throw 'El ensayo solo admite una subcarpeta de backups/mysql-auth del proyecto.'
}
if ($restoreDirectory.Contains(',') -or $PSScriptRoot.Contains(',')) { throw 'Docker --mount requiere rutas sin comas.' }
$restoreManifestPath = Join-Path $restoreDirectory 'backup.json'
$restoreManifest = Get-Content -LiteralPath $restoreManifestPath -Raw | ConvertFrom-Json
$restoreDump = Join-Path $restoreDirectory 'database.sql'
if ($restoreManifest.dumpFile -ne 'database.sql' -or $restoreManifest.database -notmatch '^[A-Za-z0-9_]+$') {
    throw 'Manifiesto de respaldo no valido.'
}
if ((Get-FileHash -LiteralPath $restoreDump -Algorithm SHA256).Hash -ne $restoreManifest.sha256) {
    throw 'El hash del dump no coincide. No se restaurara.'
}
& $restoreDocker.Source info --format '{{.ServerVersion}}' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Docker Desktop no esta disponible.' }
$restoreContainer = 'task-mysql-auth-' + [Guid]::NewGuid().ToString('N').Substring(0, 12)
$restoreInspection = Join-Path $PSScriptRoot 'inspection'
# This disposable local database has no host ports or network access. Its socket
# is available only through docker exec. Events stay off after importing a dump.
$restoreArguments = @('run', '-d', '--name', $restoreContainer,
    '--label', 'com.taskapp.purpose=auth-migration-rehearsal', '--network', 'none',
    '--env', 'MYSQL_ALLOW_EMPTY_PASSWORD=yes',
    '--mount', ('type=bind,source=' + $restoreDirectory + ',target=/backup,readonly'),
    '--mount', ('type=bind,source=' + $restoreInspection + ',target=/inspection,readonly'),
    $restoreImage, '--skip-networking', '--mysqlx=0', '--event-scheduler=OFF')
& $restoreDocker.Source @restoreArguments | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el contenedor aislado.' }
[ordered]@{container=$restoreContainer; startedUtc=[DateTime]::UtcNow.ToString('o'); network='none'; publishedPorts=@(); source='local dump; no connection to Aiven'} |
    ConvertTo-Json | Set-Content -LiteralPath (Join-Path $restoreDirectory 'rehearsal.json') -Encoding UTF8
Write-Host ('Esperando MySQL aislado: ' + $restoreContainer)
$restoreReady = $false
$restoreDeadline = [DateTime]::UtcNow.AddSeconds(120)
while ([DateTime]::UtcNow -lt $restoreDeadline) {
    # Wait for the final mysqld PID 1, not the entrypoint's temporary init server.
    # Separate native arguments avoid Windows PowerShell 5.1 stripping the
    # embedded shell quotes around $(cat ...). No shell expansion is needed.
    $restorePidOne = & $restoreDocker.Source exec $restoreContainer cat /proc/1/comm
    if ($LASTEXITCODE -eq 0 -and ([string]$restorePidOne).Trim() -eq 'mysqld') {
        & $restoreDocker.Source exec $restoreContainer mysqladmin --user=root ping --silent | Out-Null
        if ($LASTEXITCODE -eq 0) { $restoreReady = $true; break }
    }
    Start-Sleep -Milliseconds 500
}
if (-not $restoreReady) { throw ('MySQL no termino de arrancar. Contenedor conservado: ' + $restoreContainer) }
& $restoreDocker.Source exec $restoreContainer sh -c 'mysql --user=root --default-character-set=utf8mb4 < /backup/database.sql'
if ($LASTEXITCODE -ne 0) { throw ('Restauracion fallida; no se marca como verificada. Contenedor: ' + $restoreContainer) }
$restoreDatabase = [string]$restoreManifest.database
$restoreOtherEngines = & $restoreDocker.Source exec $restoreContainer mysql --user=root --batch --skip-column-names $restoreDatabase --execute "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' AND engine <> 'InnoDB'"
if ($LASTEXITCODE -ne 0 -or [int]$restoreOtherEngines -ne 0) {
    throw 'La copia incluye tablas fuera de InnoDB o no pudo comprobarse. No asumir consistencia de --single-transaction.'
}
& $restoreDocker.Source exec $restoreContainer sh -c ('mysql --user=root --batch --default-character-set=utf8mb4 ' + $restoreDatabase + ' < /inspection/pre_auth_inventory.mysql.sql > /tmp/inventory.tsv')
if ($LASTEXITCODE -ne 0) { throw 'Fallo el inventario de la copia restaurada.' }
& $restoreDocker.Source cp ($restoreContainer + ':/tmp/inventory.tsv') (Join-Path $restoreDirectory 'inventory.tsv')
if ($LASTEXITCODE -ne 0) { throw 'No se pudo conservar el inventario.' }
& $restoreDocker.Source exec $restoreContainer sh -c ('mysql --user=root --batch ' + $restoreDatabase + ' < /inspection/owner_baseline_counts.mysql.sql > /tmp/counts.tsv')
if ($LASTEXITCODE -ne 0) { throw 'El esquema restaurado no coincide con la linea base esperada; revisar inventory.tsv.' }
& $restoreDocker.Source cp ($restoreContainer + ':/tmp/counts.tsv') (Join-Path $restoreDirectory 'counts.tsv')
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron conservar los recuentos.' }
& $restoreDocker.Source exec $restoreContainer mysqldump --user=root --no-data --no-tablespaces --set-gtid-purged=OFF --routines --events --triggers --result-file=/tmp/schema.sql --databases $restoreDatabase
if ($LASTEXITCODE -ne 0) { throw 'No se pudo conservar el esquema restaurado.' }
& $restoreDocker.Source cp ($restoreContainer + ':/tmp/schema.sql') (Join-Path $restoreDirectory 'schema.sql')
if ($LASTEXITCODE -ne 0) { throw 'No se pudo copiar el esquema.' }
$restoreManifest.restoreVerified = $true
$restoreManifest | Add-Member -NotePropertyName restoredContainer -NotePropertyValue $restoreContainer -Force
$restoreManifest | Add-Member -NotePropertyName restoredUtc -NotePropertyValue ([DateTime]::UtcNow.ToString('o')) -Force
$restoreManifest | ConvertTo-Json | Set-Content -LiteralPath $restoreManifestPath -Encoding UTF8
Write-Host 'Restauracion verificada. Inventario, recuentos y esquema disponibles junto al dump.'
Write-Host ('Contenedor local conservado para ensayar la migracion: ' + $restoreContainer)
