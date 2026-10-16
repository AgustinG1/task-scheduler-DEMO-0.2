[CmdletBinding()]
param(
    [string]$Issuer,
    [string]$ClientId,
    [ValidateRange(1024, 65535)]
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'
$taskSchedulerJar = Join-Path $PSScriptRoot 'target/task-scheduler-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $taskSchedulerJar)) {
    throw 'Primero construye desde la raiz: mvn -f workspace/pom.xml -B -ntp verify'
}
$taskSchedulerJava = Get-Command java -ErrorAction Stop

# Check the same loopback address used by the app before requesting credentials.
$taskSchedulerPortProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
try {
    $taskSchedulerPortProbe.Start()
}
catch {
    throw ('El puerto ' + $Port + ' no esta disponible. Usa -Port con un puerto libre y actualiza las URLs de Auth0 para ese puerto.')
}
finally {
    $taskSchedulerPortProbe.Stop()
}

if ([string]::IsNullOrWhiteSpace($Issuer)) {
    $Issuer = $env:AUTH0_ISSUER
}
if ([string]::IsNullOrWhiteSpace($Issuer)) {
    $Issuer = Read-Host 'Issuer de Auth0 (https://TU_DOMINIO/; con barra final)'
}
# Uri.TryCreate tolerates surrounding whitespace, but Spring's issuer discovery does not.
$Issuer = $Issuer.Trim()
$taskSchedulerIssuerUri = $null
if (-not [Uri]::TryCreate($Issuer, [UriKind]::Absolute, [ref]$taskSchedulerIssuerUri) -or
    $Issuer -match '\s' -or
    $taskSchedulerIssuerUri.Scheme -ne 'https' -or
    $taskSchedulerIssuerUri.AbsolutePath -ne '/' -or
    -not [string]::IsNullOrEmpty($taskSchedulerIssuerUri.Query) -or
    -not [string]::IsNullOrEmpty($taskSchedulerIssuerUri.Fragment) -or
    -not [string]::IsNullOrEmpty($taskSchedulerIssuerUri.UserInfo) -or
    -not $Issuer.EndsWith('/')) {
    throw 'El issuer debe ser una URL HTTPS del dominio Auth0 con barra final, sin ruta adicional, usuario ni parametros.'
}
if ([string]::IsNullOrWhiteSpace($ClientId)) {
    $ClientId = $env:AUTH0_CLIENT_ID
}
if ([string]::IsNullOrWhiteSpace($ClientId)) {
    $ClientId = Read-Host 'Client ID de la aplicacion web de desarrollo'
}
if ([string]::IsNullOrWhiteSpace($ClientId)) {
    throw 'El Client ID es obligatorio.'
}
$ClientId = $ClientId.Trim()

$taskSchedulerPrevious = @{}
foreach ($taskSchedulerVariable in @('AUTH0_ISSUER','AUTH0_CLIENT_ID','AUTH0_CLIENT_SECRET')) {
    $taskSchedulerPrevious[$taskSchedulerVariable] = [Environment]::GetEnvironmentVariable($taskSchedulerVariable, 'Process')
}
$taskSchedulerSecureSecret = $null
$taskSchedulerRuntimeJar = Join-Path $PSScriptRoot ('target/run/task-scheduler-' + [Guid]::NewGuid().ToString('N') + '.jar')
try {
    # Run a private copy so a Maven rebuild does not overwrite a running JAR on Windows.
    New-Item -ItemType Directory -Path (Split-Path -Parent $taskSchedulerRuntimeJar) -Force | Out-Null
    Copy-Item -LiteralPath $taskSchedulerJar -Destination $taskSchedulerRuntimeJar
    if ([string]::IsNullOrWhiteSpace($env:AUTH0_CLIENT_SECRET)) {
        $taskSchedulerSecureSecret = Read-Host 'Client Secret (entrada oculta; no se guarda en archivos)' -AsSecureString
        if ($taskSchedulerSecureSecret.Length -eq 0) {
            throw 'El Client Secret es obligatorio.'
        }
        $taskSchedulerSecretPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($taskSchedulerSecureSecret)
        try {
            $env:AUTH0_CLIENT_SECRET = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($taskSchedulerSecretPointer)
        }
        finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($taskSchedulerSecretPointer)
        }
    }
    $env:AUTH0_ISSUER = $Issuer
    $env:AUTH0_CLIENT_ID = $ClientId
    Write-Host ('Iniciando TaskScheduler local en http://localhost:' + $Port + ' (Ctrl+C para detener)...')
    & $taskSchedulerJava.Source -jar $taskSchedulerRuntimeJar "--server.port=$Port" "--spring.profiles.active=local,auth0" "--server.address=127.0.0.1"
    if ($LASTEXITCODE -ne 0) {
        throw ('TaskScheduler termino con codigo ' + $LASTEXITCODE)
    }
}
finally {
    foreach ($taskSchedulerVariable in $taskSchedulerPrevious.Keys) {
        [Environment]::SetEnvironmentVariable($taskSchedulerVariable, $taskSchedulerPrevious[$taskSchedulerVariable], 'Process')
    }
    if ($null -ne $taskSchedulerSecureSecret) {
        $taskSchedulerSecureSecret.Dispose()
    }
    if (Test-Path -LiteralPath $taskSchedulerRuntimeJar) {
        Remove-Item -LiteralPath $taskSchedulerRuntimeJar -ErrorAction SilentlyContinue
    }
}
