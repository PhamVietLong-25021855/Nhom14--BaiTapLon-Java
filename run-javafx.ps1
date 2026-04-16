$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$javafxLib = Join-Path $root "javafx-sdk\lib"
$sourceRoot = Join-Path $root "User\src"
$resourceRoot = Join-Path $root "User\resources"
$outputDir = Join-Path $root "out\javafx-app"

if (-not (Test-Path $javafxLib)) {
    Write-Error "Khong tim thay JavaFX SDK tai $javafxLib"
    exit 1
}

New-Item -ItemType Directory -Force $outputDir | Out-Null
$files = Get-ChildItem -Path $sourceRoot -Recurse -Filter *.java | ForEach-Object { $_.FullName }

javac --module-path $javafxLib --add-modules javafx.controls,javafx.fxml -d $outputDir $files
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (Test-Path $resourceRoot) {
    Get-ChildItem -Path $resourceRoot -Recurse -File | ForEach-Object {
        $relativePath = $_.FullName.Substring($resourceRoot.Length + 1)
        $destination = Join-Path $outputDir $relativePath
        $destinationDir = Split-Path -Parent $destination
        New-Item -ItemType Directory -Force $destinationDir | Out-Null
        Copy-Item $_.FullName $destination -Force
    }
}

java --module-path $javafxLib --add-modules javafx.controls,javafx.fxml -cp $outputDir userauth.Main
