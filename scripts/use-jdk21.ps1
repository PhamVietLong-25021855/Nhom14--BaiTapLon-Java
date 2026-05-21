$candidateHomes = @()

if ($env:JDK21_HOME) {
    $candidateHomes += $env:JDK21_HOME
}

$candidateHomes += @(
    "C:\Program Files\Java\jdk-21.0.10",
    "C:\Program Files\Java\jdk-21",
    "C:\Program Files\Java\latest"
)

$jdk21Home = $candidateHomes |
    Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\java.exe")) } |
    Select-Object -First 1

if (-not $jdk21Home) {
    $jdk21Home = Get-ChildItem "C:\Program Files\Java" -Directory -Filter "jdk-21*" -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}

if (-not $jdk21Home) {
    throw "JDK 21 was not found. Install JDK 21 or set JDK21_HOME to the JDK 21 folder."
}

$javaExe = Join-Path $jdk21Home "bin\java.exe"
$versionOutput = & $javaExe -version 2>&1
$versionText = $versionOutput -join " "
if ($versionText -notmatch 'version "21\.') {
    throw "Expected JDK 21, but '$jdk21Home' reports: $versionText"
}

$env:JAVA_HOME = $jdk21Home
$env:Path = "$(Join-Path $jdk21Home "bin");$env:Path"
