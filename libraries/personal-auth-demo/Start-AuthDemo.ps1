[CmdletBinding()]
param(
    [string]$Issuer,
    [string]$ClientId,
    [ValidateRange(1024, 65535)]
    [int]$Port = 8082
)

$ErrorActionPreference = 'Stop'
$authDemoJar = Join-Path $PSScriptRoot 'target/personal-auth-demo-0.1.0-SNAPSHOT-exec.jar'
if (-not (Test-Path -LiteralPath $authDemoJar)) {
    throw 'Primero construye desde la raiz: mvn -f libraries/pom.xml -B -ntp verify'
}
$authDemoJava = Get-Command java -ErrorAction Stop

# Check the same loopback address used by the app before requesting credentials.
$authDemoPortProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
try {
    $authDemoPortProbe.Start()
}
catch {
    throw ('El puerto ' + $Port + ' no esta disponible. Usa -Port con un puerto libre y actualiza las URLs de Auth0 para ese puerto.')
}
finally {
    $authDemoPortProbe.Stop()
}

if ([string]::IsNullOrWhiteSpace($Issuer)) {
    $Issuer = $env:AUTH0_ISSUER
}
if ([string]::IsNullOrWhiteSpace($Issuer)) {
    $Issuer = Read-Host 'Issuer de Auth0 (https://TU_DOMINIO/; con barra final)'
}
# Uri.TryCreate tolerates surrounding whitespace, but Spring's issuer discovery does not.
$Issuer = $Issuer.Trim()
$authDemoIssuerUri = $null
if (-not [Uri]::TryCreate($Issuer, [UriKind]::Absolute, [ref]$authDemoIssuerUri) -or
    $Issuer -match '\s' -or
    $authDemoIssuerUri.Scheme -ne 'https' -or
    $authDemoIssuerUri.AbsolutePath -ne '/' -or
    -not [string]::IsNullOrEmpty($authDemoIssuerUri.Query) -or
    -not [string]::IsNullOrEmpty($authDemoIssuerUri.Fragment) -or
    -not [string]::IsNullOrEmpty($authDemoIssuerUri.UserInfo) -or
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

$authDemoPrevious = @{}
foreach ($authDemoVariable in @('AUTH0_ISSUER','AUTH0_CLIENT_ID','AUTH0_CLIENT_SECRET')) {
    $authDemoPrevious[$authDemoVariable] = [Environment]::GetEnvironmentVariable($authDemoVariable, 'Process')
}
$authDemoSecureSecret = $null
$authDemoRuntimeJar = Join-Path $PSScriptRoot ('target/run/auth-demo-' + [Guid]::NewGuid().ToString('N') + '.jar')
try {
    # Run a private copy so a Maven rebuild does not overwrite a running JAR on Windows.
    New-Item -ItemType Directory -Path (Split-Path -Parent $authDemoRuntimeJar) -Force | Out-Null
    Copy-Item -LiteralPath $authDemoJar -Destination $authDemoRuntimeJar
    if ([string]::IsNullOrWhiteSpace($env:AUTH0_CLIENT_SECRET)) {
        $authDemoSecureSecret = Read-Host 'Client Secret (entrada oculta; no se guarda en archivos)' -AsSecureString
        if ($authDemoSecureSecret.Length -eq 0) {
            throw 'El Client Secret es obligatorio.'
        }
        $authDemoSecretPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($authDemoSecureSecret)
        try {
            $env:AUTH0_CLIENT_SECRET = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($authDemoSecretPointer)
        }
        finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($authDemoSecretPointer)
        }
    }
    $env:AUTH0_ISSUER = $Issuer
    $env:AUTH0_CLIENT_ID = $ClientId
    Write-Host ('Iniciando demo en http://localhost:' + $Port + ' (Ctrl+C para detener)...')
    & $authDemoJava.Source -jar $authDemoRuntimeJar "--server.port=$Port"
    if ($LASTEXITCODE -ne 0) {
        throw ('La demo termino con codigo ' + $LASTEXITCODE)
    }
}
finally {
    foreach ($authDemoVariable in $authDemoPrevious.Keys) {
        [Environment]::SetEnvironmentVariable($authDemoVariable, $authDemoPrevious[$authDemoVariable], 'Process')
    }
    if ($null -ne $authDemoSecureSecret) {
        $authDemoSecureSecret.Dispose()
    }
    if (Test-Path -LiteralPath $authDemoRuntimeJar) {
        Remove-Item -LiteralPath $authDemoRuntimeJar -ErrorAction SilentlyContinue
    }
}
