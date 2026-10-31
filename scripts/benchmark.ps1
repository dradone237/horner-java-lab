<#
.SYNOPSIS
    Benchmark Horner vs evaluation naive ; le rapport Markdown est ecrit dans reports\.
.EXAMPLE
    .\scripts\benchmark.ps1
    .\scripts\benchmark.ps1 -Sizes "10,100,1000,10000" -Repeat 20
#>
param(
    [string]$Sizes = '10,100,1000,10000',
    [int]$Repeat = 10,
    [int]$Warmup = 5
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$report = Join-Path $root "reports\benchmark-$stamp.md"
& (Join-Path $root 'run.ps1') benchmark --sizes $Sizes --repeat $Repeat --warmup $Warmup --export $report
exit $LASTEXITCODE
