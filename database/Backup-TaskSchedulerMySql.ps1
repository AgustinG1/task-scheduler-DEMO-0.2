[CmdletBinding()]
param(
    [string]$DatabaseHost = 'mysql-26d78475-task-scheduler0110.e.aivencloud.com',
    [ValidateRange(1, 65535)][int]$DatabasePort = 22513,
    [ValidatePattern('^[A-Za-z0-9_]+$')][string]$Database = 'defaultdb',
    [string]$DatabaseUser = 'avnadmin',
    [string]$CaPath = (Join-Path $PSScriptRoot '../backups/mysql-auth/aiven-ca.pem'),
    [switch]$CheckClient
)

$ErrorActionPreference = 'Stop'
$backupDocker = Get-Command docker -ErrorAction Stop
$backupImage = 'mysql@sha256:2952e3be7807f06fc18de50b3ea1a632d5c70d63482ff7d7376fe3aa8999babf'
$backupRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../backups/mysql-auth'))
$backupCa = (Resolve-Path -LiteralPath $CaPath -ErrorAction Stop).Path
if ($backupCa.Contains(',') -or $backupRoot.Contains(',')) { throw 'Docker --mount requiere rutas sin comas.' }
if ([string]::IsNullOrWhiteSpace($DatabaseHost) -or [string]::IsNullOrWhiteSpace($DatabaseUser)) {
    throw 'Host y usuario son obligatorios.'
}
& $backupDocker.Source info --format '{{.ServerVersion}}' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Inicia Docker Desktop y espera a que el motor este disponible.' }
& $backupDocker.Source image inspect $backupImage --format '{{.Id}}' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Falta la imagen oficial. Ejecuta: docker pull mysql:8.4.8' }

$backupClientArguments = @(
    ('--host=' + $DatabaseHost), ('--port=' + $DatabasePort), ('--user=' + $DatabaseUser),
    '--password', '--protocol=TCP',
    '--ssl-mode=VERIFY_IDENTITY', '--ssl-ca=/cert/ca.pem',
    '--single-transaction', '--no-tablespaces', '--set-gtid-purged=OFF',
    '--column-statistics=0', '--routines', '--events', '--triggers', '--hex-blob',
    '--default-character-set=utf8mb4', '--result-file=/backup/database.sql.partial',
    '--databases', $Database)
# Parse the actual production options before prompting or creating a backup.
# No network and no password; use a disposable result path inside the container.
# Put --help after all options and before the positional database name.
$backupCheckArguments = @($backupClientArguments[0..($backupClientArguments.Count - 2)] | ForEach-Object {
    if ($_ -eq '--password') { } # --help exits without authentication; omit the credential prompt.
    elseif ($_ -eq '--result-file=/backup/database.sql.partial') { '--result-file=/tmp/check-options.sql' }
    else { $_ }
})
& $backupDocker.Source run --rm --network none --mount ('type=bind,source=' + $backupCa + ',target=/cert/ca.pem,readonly') $backupImage mysqldump @backupCheckArguments --help $Database | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'El cliente mysqldump rechazo las opciones. No se inicio el respaldo ni se solicito contrasena.' }
Write-Host 'Opciones verificadas con mysqldump 8.4.8, sin conexion a la base.'
if ($CheckClient) { return }

$backupRunName = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0, 8)
$backupDirectory = Join-Path $backupRoot $backupRunName
New-Item -ItemType Directory -Path $backupDirectory | Out-Null
$backupPartial = Join-Path $backupDirectory 'database.sql.partial'
$backupFinal = Join-Path $backupDirectory 'database.sql'
Write-Host 'Se solicitara la contrasena MySQL de Aiven. Pegala en Enter password; la entrada esta oculta.'
Write-Host 'No se modifican datos del origen. No ejecutes DDL sobre el origen mientras se genera el dump.'
# The client requests the password in its TTY. It never enters PowerShell variables,
# arguments, Docker environment variables, logs, or configuration files.
$backupArguments = @('run', '--rm', '-it',
    '--mount', ('type=bind,source=' + $backupDirectory + ',target=/backup'),
    '--mount', ('type=bind,source=' + $backupCa + ',target=/cert/ca.pem,readonly'),
    $backupImage, 'mysqldump') + $backupClientArguments
& $backupDocker.Source @backupArguments
if ($LASTEXITCODE -ne 0) {
    throw ('Dump incompleto. Revisa ' + $backupDirectory + '; cualquier archivo .partial no debe usarse como respaldo valido.')
}
if (-not (Test-Path -LiteralPath $backupPartial) -or (Get-Item -LiteralPath $backupPartial).Length -eq 0) {
    throw 'El cliente termino sin un dump no vacio.'
}
Move-Item -LiteralPath $backupPartial -Destination $backupFinal
$backupManifest = [ordered]@{
    createdUtc = [DateTime]::UtcNow.ToString('o')
    sourceHost = $DatabaseHost
    sourcePort = $DatabasePort
    database = $Database
    clientImage = $backupImage
    dumpFile = 'database.sql'
    bytes = (Get-Item -LiteralPath $backupFinal).Length
    sha256 = (Get-FileHash -LiteralPath $backupFinal -Algorithm SHA256).Hash
    restoreVerified = $false
}
$backupManifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $backupDirectory 'backup.json') -Encoding UTF8
Write-Host ('Dump completado: ' + $backupDirectory)
& (Join-Path $PSScriptRoot 'Test-TaskSchedulerMySqlBackup.ps1') -BackupDirectory $backupDirectory
