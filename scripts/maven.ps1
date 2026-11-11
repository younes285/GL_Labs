# Prefer the project-local JDK and Maven without changing the system installation.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localTools = Join-Path $projectRoot '.tools'
$localJdk = Get-ChildItem -Path (Join-Path $localTools 'jdk-*') -Directory -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1
$localMaven = Get-ChildItem -Path (Join-Path $localTools 'apache-maven-*') -Directory -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1
$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:PATH
$configuredJavaHome = Join-Path $localTools 'java-home.txt'
Push-Location $projectRoot
try {
    if ($localJdk) {
        $env:JAVA_HOME = $localJdk.FullName
    } elseif (Test-Path -LiteralPath $configuredJavaHome) {
        $env:JAVA_HOME = (Get-Content -LiteralPath $configuredJavaHome -Raw).Trim()
    }
    if ($env:JAVA_HOME) {
        $env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH
    }
    if ($localMaven) {
        $maven = Join-Path $localMaven.FullName 'bin\mvn.cmd'
    } else {
        $maven = (Get-Command mvn.cmd -ErrorAction Stop).Source
    }
    $javaVersion = (& java --version | Out-String)
    if ($javaVersion -notmatch '(openjdk|java) (17|21)\.') {
        throw 'Use JDK 17 or 21. Install one and set JAVA_HOME, or use the project-local .tools JDK.'
    }
    & $maven "-Dmaven.repo.local=$projectRoot/.m2/repository" @args
    $result = $LASTEXITCODE
} finally {
    $env:JAVA_HOME = $originalJavaHome
    $env:PATH = $originalPath
    Pop-Location
}
exit $result
