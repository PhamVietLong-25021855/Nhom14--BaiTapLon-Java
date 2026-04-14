$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$javafxLib = Join-Path $root "javafx-sdk\lib"
$sourceRoot = Join-Path $root "User\src"
$outputDir = Join-Path $root "out\javafx-app"

if (-not (Test-Path $javafxLib)) {
    Write-Error "Khong tim thay JavaFX SDK tai $javafxLib"
    exit 1
}

New-Item -ItemType Directory -Force $outputDir | Out-Null
$files = Get-ChildItem -Path $sourceRoot -Recurse -Filter *.java | ForEach-Object { $_.FullName }

javac --module-path $javafxLib --add-modules javafx.controls -d $outputDir $files
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

java --module-path $javafxLib --add-modules javafx.controls -cp $outputDir userauth.Main
