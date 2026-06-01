function Add-JdkCandidate {
    param(
        [System.Collections.Generic.List[string]]$Candidates,
        [string]$JdkHome
    )

    if (-not [string]::IsNullOrWhiteSpace($JdkHome)) {
        $Candidates.Add($JdkHome)
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

$candidateHomes = [System.Collections.Generic.List[string]]::new()
Add-JdkCandidate -Candidates $candidateHomes -JdkHome $env:JDK21_HOME
Add-JdkCandidate -Candidates $candidateHomes -JdkHome $env:JAVA_HOME

$javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
if ($javaCommand -and $javaCommand.Source) {
    Add-JdkCandidate -Candidates $candidateHomes -JdkHome (Split-Path -Parent (Split-Path -Parent $javaCommand.Source))
}

$searchRoots = @(
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

$jdk21Home = $candidateHomes |
    Select-Object -Unique |
    Where-Object { Test-Jdk21Home -JdkHome $_ } |
    Select-Object -First 1

if (-not $jdk21Home) {
    throw "JDK 21 was not found. Install JDK 21, set JDK21_HOME or JAVA_HOME to its folder, or add its bin folder to PATH."
}

$env:JAVA_HOME = $jdk21Home
$env:Path = "$(Join-Path $jdk21Home "bin");$env:Path"
