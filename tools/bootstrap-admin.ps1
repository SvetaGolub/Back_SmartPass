param(
    [switch]$ConfirmEmptyDatabase,
    [switch]$SelfTest
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$sourceFile = Join-Path $repoRoot 'src\main\java\org\Proect\tools\BootstrapAdmin.java'
$cacheRoot = Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1'
$tempRoot = Join-Path $env:TEMP ('smartpass-admin-bootstrap-' + [guid]::NewGuid().ToString('N'))
$hadProcessPassword = [bool]$env:DB_PASSWORD
$hadProcessUser = [bool]$env:DB_USER
$hadProcessUrl = [bool]$env:DB_URL

if ($SelfTest -and $ConfirmEmptyDatabase) {
    throw 'Choose either -SelfTest or -ConfirmEmptyDatabase, not both.'
}
if (-not $SelfTest -and -not $ConfirmEmptyDatabase) {
    throw 'No changes were made. To create one ADMIN only in an empty local SmartPass database, rerun with -ConfirmEmptyDatabase.'
}

try {
    if (-not $env:DB_PASSWORD) {
        $env:DB_PASSWORD = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'User')
    }
    if (-not $SelfTest -and -not $env:DB_PASSWORD) {
        throw 'DB_PASSWORD is unavailable. Configure it for this local user without putting it in a command or file.'
    }

    if (-not $env:DB_USER) {
        $env:DB_USER = [Environment]::GetEnvironmentVariable('DB_USER', 'User')
    }
    if (-not $env:DB_USER) {
        $env:DB_USER = 'root'
    }

    if (-not $env:DB_URL) {
        $env:DB_URL = [Environment]::GetEnvironmentVariable('DB_URL', 'User')
    }
    if (-not $env:DB_URL) {
        $env:DB_URL = 'jdbc:mysql://localhost:3306/SmartPass?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false'
    }

    if (-not $SelfTest) {
        $match = [regex]::Match($env:DB_URL, '^jdbc:mysql://([^/:?#]+):(\d+)/([^?#]+)(?:\?.*)?$')
        if (-not $match.Success -or $match.Groups[1].Value -notin @('localhost', '127.0.0.1') -or
            $match.Groups[2].Value -ne '3306' -or $match.Groups[3].Value -ine 'SmartPass') {
            throw 'Bootstrap is restricted to localhost:3306/SmartPass; no changes were made.'
        }
    }

    $mysqlJar = Get-ChildItem (Join-Path $cacheRoot 'com.mysql\mysql-connector-j') -Filter 'mysql-connector-j-*.jar' -Recurse |
        Where-Object { $_.Name -notlike '*sources*' } |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName
    $securityJar = Get-ChildItem (Join-Path $cacheRoot 'org.springframework.security\spring-security-crypto') -Filter 'spring-security-crypto-*.jar' -Recurse |
        Where-Object { $_.Name -notlike '*sources*' } |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName
    $springJclJar = Get-ChildItem (Join-Path $cacheRoot 'org.springframework\spring-jcl') -Filter 'spring-jcl-*.jar' -Recurse |
        Where-Object { $_.Name -notlike '*sources*' } |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName
    if (-not $mysqlJar -or -not $securityJar -or -not $springJclJar) {
        throw 'Required MySQL/Spring Security jars are not cached. Run the project build first.'
    }

    New-Item -ItemType Directory -Path $tempRoot | Out-Null
    $dependencyPath = "$mysqlJar;$securityJar;$springJclJar"
    & javac -encoding UTF-8 -cp $dependencyPath -d $tempRoot $sourceFile
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not compile the local bootstrap utility.'
    }

    $classPath = "$tempRoot;$dependencyPath"
    if ($SelfTest) {
        & java -cp $classPath org.Proect.tools.BootstrapAdmin --self-test
    } else {
        & java -cp $classPath org.Proect.tools.BootstrapAdmin --confirm-empty-local-smartpass
    }
    if ($LASTEXITCODE -ne 0) {
        throw 'Bootstrap utility failed; check the message above. No password was printed or saved.'
    }
}
finally {
    if (-not $hadProcessPassword) {
        Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
    }
    if (-not $hadProcessUser) {
        Remove-Item Env:DB_USER -ErrorAction SilentlyContinue
    }
    if (-not $hadProcessUrl) {
        Remove-Item Env:DB_URL -ErrorAction SilentlyContinue
    }
    if (Test-Path $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force
    }
}
