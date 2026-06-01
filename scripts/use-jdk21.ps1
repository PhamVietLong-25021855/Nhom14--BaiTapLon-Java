if (-not $script:ProjectRoot) {
    $script:ProjectRoot = Split-Path -Parent $PSScriptRoot
}

function Add-JdkCandidate {
    param(
        [System.Collections.Generic.List[string]]$Candidates,
        [string]$JdkHome
    )

    if (-not [string]::IsNullOrWhiteSpace($JdkHome)) {
        $Candidates.Add($JdkHome)
    }
}

function Remove-ToolsChildDirectory {
    param(
        [string]$ToolsRoot,
        [string]$ChildPath
    )

    $toolsFullPath = [System.IO.Path]::GetFullPath($ToolsRoot)
    $childFullPath = [System.IO.Path]::GetFullPath($ChildPath)
    $requiredPrefix = $toolsFullPath.TrimEnd('\') + '\'
    if (-not $childFullPath.StartsWith($requiredPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a directory outside .tools: $ChildPath"
    }

    if (Test-Path -LiteralPath $childFullPath -PathType Container) {
        Remove-Item -LiteralPath $childFullPath -Recurse -Force
    }
}

function Install-Jdk21WithWinget {
    $winget = (Get-Command winget.exe -ErrorAction SilentlyContinue).Source
    if (-not $winget) {
        return $false
    }

    Write-Host "JDK 21 was not found. Trying to install Eclipse Temurin JDK 21 with winget..."
    & $winget install --id EclipseAdoptium.Temurin.21.JDK --exact --source winget --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "winget could not install JDK 21. Exit code: $LASTEXITCODE"
        return $false
    }

    return $true
}

function Install-Jdk21Locally {
    $toolsRoot = Join-Path $script:ProjectRoot ".tools"
    $targetRoot = Join-Path $toolsRoot "jdk-21"
    $tempRoot = Join-Path $toolsRoot "jdk-21-download"
    $zipPath = Join-Path $toolsRoot "temurin-jdk21.zip"
    $downloadUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"

    New-Item -ItemType Directory -Force -Path $toolsRoot | Out-Null

    try {
        Write-Host "Downloading Eclipse Temurin JDK 21 to .tools..."
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath -UseBasicParsing

        Remove-ToolsChildDirectory -ToolsRoot $toolsRoot -ChildPath $tempRoot
        New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null
        Expand-Archive -LiteralPath $zipPath -DestinationPath $tempRoot -Force

        $jdkRoot = Get-ChildItem -LiteralPath $tempRoot -Recurse -Filter java.exe |
            Where-Object { $_.FullName -like "*\bin\java.exe" } |
            ForEach-Object { Split-Path -Parent (Split-Path -Parent $_.FullName) } |
            Where-Object { Test-Jdk21Home -JdkHome $_ } |
            Select-Object -First 1

        if (-not $jdkRoot) {
            Write-Warning "Downloaded archive did not contain a usable JDK 21."
            return $false
        }

        Remove-ToolsChildDirectory -ToolsRoot $toolsRoot -ChildPath $targetRoot
        Move-Item -LiteralPath $jdkRoot -Destination $targetRoot
        Remove-ToolsChildDirectory -ToolsRoot $toolsRoot -ChildPath $tempRoot
        Remove-Item -LiteralPath $zipPath -Force -ErrorAction SilentlyContinue

        return (Test-Jdk21Home -JdkHome $targetRoot)
    } catch {
        Write-Warning "Could not download JDK 21 automatically: $($_.Exception.Message)"
        return $false
    }
}

function Test-Jdk21Home {
    param([string]$JdkHome)

    if ([string]::IsNullOrWhiteSpace($JdkHome)) {
        return $false
    }

    $javaExe = Join-Path $JdkHome "bin\java.exe"
    if (-not (Test-Path -LiteralPath $javaExe -PathType Leaf)) {
        return $false
    }

    $versionText = (& $javaExe -version 2>&1) -join " "
    return $versionText -match 'version "21(?:\.|")'
}

function Find-Jdk21Home {
    $candidateHomes = [System.Collections.Generic.List[string]]::new()
    Add-JdkCandidate -Candidates $candidateHomes -JdkHome $env:JDK21_HOME
    Add-JdkCandidate -Candidates $candidateHomes -JdkHome $env:JAVA_HOME
    Add-JdkCandidate -Candidates $candidateHomes -JdkHome (Join-Path $script:ProjectRoot ".tools\jdk-21")

    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand -and $javaCommand.Source) {
        Add-JdkCandidate -Candidates $candidateHomes -JdkHome (Split-Path -Parent (Split-Path -Parent $javaCommand.Source))
    }

    $searchRoots = @(
        (Join-Path $script:ProjectRoot ".tools"),
        (Join-Path $env:ProgramFiles "Java"),
        (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
        (Join-Path $env:ProgramFiles "Microsoft"),
        (Join-Path $env:LOCALAPPDATA "Programs\Eclipse Adoptium")
    )

    foreach ($searchRoot in $searchRoots) {
        if (-not (Test-Path -LiteralPath $searchRoot -PathType Container)) {
            continue
        }

        Get-ChildItem -LiteralPath $searchRoot -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -like "*21*" } |
            Sort-Object Name -Descending |
            ForEach-Object { Add-JdkCandidate -Candidates $candidateHomes -JdkHome $_.FullName }
    }

    $candidateHomes |
        Select-Object -Unique |
        Where-Object { Test-Jdk21Home -JdkHome $_ } |
        Select-Object -First 1
}

$jdk21Home = Find-Jdk21Home

if (-not $jdk21Home) {
    if (Install-Jdk21WithWinget) {
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
            [System.Environment]::GetEnvironmentVariable("Path", "User") + ";" + $env:Path
        $jdk21Home = Find-Jdk21Home
    }
}

if (-not $jdk21Home) {
    if (Install-Jdk21Locally) {
        $jdk21Home = Find-Jdk21Home
    }
}

if (-not $jdk21Home) {
    throw "JDK 21 was not found and automatic install failed. Install JDK 21 manually, set JDK21_HOME or JAVA_HOME to its folder, or add its bin folder to PATH."
}

$env:JAVA_HOME = $jdk21Home
$env:Path = "$(Join-Path $jdk21Home "bin");$env:Path"
