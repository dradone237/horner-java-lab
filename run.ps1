<#
.SYNOPSIS
    Lanceur PowerShell de HORNER JAVA LAB (compatible Windows PowerShell 5.1).
.DESCRIPTION
    Sans argument : ouvre l'interface graphique.
    Avec arguments : les transmet a la ligne de commande.
    -Build : reconstruit le JAR avec Maven avant le lancement.
.EXAMPLE
    .\run.ps1
    .\run.ps1 eval --coeffs "2,-6,2,-1" --x 3
    .\run.ps1 -Build
#>
param(
    [switch]$Build,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$AppArgs
)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$jar = Join-Path $PSScriptRoot 'target\horner-java-lab-1.0.0.jar'

function Find-Java {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        return (Join-Path $env:JAVA_HOME 'bin\java.exe')
    }
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

$java = Find-Java
if (-not $java) {
    Write-Host '[ERREUR] Java est introuvable.' -ForegroundColor Red
    Write-Host 'Installez Java 21 : winget install EclipseAdoptium.Temurin.21.JDK'
    exit 1
}

$versionLine = (& cmd /c "`"$java`" -version 2>&1" | Select-Object -First 1)
if ($versionLine -match '"(\d+)(\.(\d+))?') {
    $major = [int]$Matches[1]
    if ($major -eq 1 -and $Matches[3]) { $major = [int]$Matches[3] }
} else {
    $major = 0
}
if ($major -lt 21) {
    Write-Host "[ERREUR] Java 21 ou plus recent est requis (detecte : $versionLine)." -ForegroundColor Red
    exit 1
}

if ($Build -or -not (Test-Path $jar)) {
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if (-not $mvn) {
        $portable = Join-Path $env:USERPROFILE '.maven\apache-maven-3.9.16\bin\mvn.cmd'
        if (Test-Path $portable) { $mvn = $portable } else { $mvn = $null }
    } else {
        $mvn = $mvn.Source
    }
    if (-not $mvn) {
        Write-Host '[ERREUR] Le JAR est absent et Maven est introuvable pour le construire.' -ForegroundColor Red
        exit 2
    }
    Write-Host 'Construction du projet (mvn clean package)...'
    & $mvn -q clean package
    if ($LASTEXITCODE -ne 0) {
        Write-Host '[ERREUR] La construction Maven a echoue.' -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

if (-not $AppArgs -or $AppArgs.Count -eq 0) {
    $javaw = $java -replace 'java\.exe$', 'javaw.exe'
    if (-not (Test-Path $javaw)) { $javaw = $java }
    Start-Process -FilePath $javaw -ArgumentList @('-jar', "`"$jar`"") -WorkingDirectory $PSScriptRoot
    exit 0
}

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
& $java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -jar $jar @AppArgs
exit $LASTEXITCODE
