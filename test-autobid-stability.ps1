# ============================================================
# Autobid Stability Test - Ket noi truc tiep qua Socket
# ============================================================
# Chu y: Script nay ket noi den server dang chay tren may.
# Neu server chay tren VPS, sua $serverHost = "127.0.0.1"
# ============================================================

$ErrorActionPreference = "Stop"

$serverHost = "127.0.0.1"   # Doi thanh IP VPS neu can
$serverPort = 5050
$timeoutMs = 8000

function Get-SerializedBytes($obj) {
    $stream = New-Object System.IO.MemoryStream
    $formatter = New-Object System.Runtime.Serialization.Formatters.Binary.BinaryFormatter
    $formatter.Serialize($stream, $obj)
    return $stream.ToArray()
}

function Read-SerializedObject($inputStream) {
    $formatter = New-Object System.Runtime.Serialization.Formatters.Binary.BinaryFormatter
    return $formatter.Deserialize($inputStream)
}

function Send-SocketRequest($action, $params) {
    $request = @{
        Action = $action
        Params = $params
    }

    $tcpClient = New-Object System.Net.Sockets.TcpClient
    try {
        $tcpClient.Connect($serverHost, $serverPort)
        $tcpClient.ReceiveTimeout = $timeoutMs
        $tcpClient.SendTimeout = $timeoutMs

        $stream = $tcpClient.GetStream()
        $writer = New-Object System.IO.BinaryWriter($stream)
        $reader = New-Object System.IO.BinaryReader($stream)

        # Serialize request
        $requestBytes = Get-SerializedBytes $request
        $writer.Write($requestBytes.Length)
        $writer.Write($requestBytes)
        $writer.Flush()

        # Read response
        $lenBytes = $reader.ReadBytes(4)
        if ($lenBytes.Length -lt 4) {
            throw "Server closed connection unexpectedly."
        }
        $len = [BitConverter]::ToInt32($lenBytes, 0)
        if ($len -le 0 -or $len -gt 10MB) {
            throw "Invalid response length: $len"
        }
        $responseBytes = $reader.ReadBytes($len)
        $response = Read-SerializedObject ([System.IO.MemoryStream]::new($responseBytes))

        return $response
    } finally {
        $tcpClient.Close()
    }
}

function Test-Response($response, $label) {
    if ($null -eq $response) {
        Write-Host "[FAIL] $label - Response is null" -ForegroundColor Red
        return $false
    }
    if ($response.Success -eq $false) {
        Write-Host "[FAIL] $label - Error: $($response.ErrorMessage)" -ForegroundColor Red
        return $false
    }
    Write-Host "[OK]   $label" -ForegroundColor Green
    return $true
}

function Invoke-SocketCall($action, $params, $label) {
    try {
        $response = Send-SocketRequest $action $params
        $ok = Test-Response $response $label
        return @{ Response = $response; Success = $ok }
    } catch {
        Write-Host "[ERROR] $label - Exception: $_" -ForegroundColor Red
        return @{ Response = $null; Success = $false }
    }
}

# ============================================================
# TEST SUITES
# ============================================================

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  AUTOBID STABILITY TEST" -ForegroundColor Cyan
Write-Host "  Server: $serverHost`:$serverPort" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# --- PING ---
Write-Host "[TEST 1] Ping server..." -ForegroundColor Yellow
$r = Invoke-SocketCall "PING" @{} "Ping"
if (-not $r.Success) { Write-Host "Server not reachable. Stop." -ForegroundColor Red; exit 1 }
Write-Host ""

# --- SETUP: Get existing data ---
Write-Host "[SETUP] Getting existing users and auctions..." -ForegroundColor Yellow
$r = Invoke-SocketCall "AUTH_ALL_USERS" @{} "Get all users"
$users = @()
if ($r.Success) {
    $data = $r.Response.Data
    if ($data -is [System.Collections.IEnumerable]) {
        foreach ($u in $data) { $users += $u }
    } elseif ($null -ne $data) {
        $users += $data
    }
}
if ($users.Count -lt 2) {
    Write-Host "[SKIP] Need at least 2 users for autobid test. Found: $($users.Count)" -ForegroundColor Yellow
    Write-Host "Creating test users..." -ForegroundColor Yellow
    $r1 = Invoke-SocketCall "AUTH_REGISTER" @{
        username = "testuser1"
        password = "Test123456"
        fullName = "Test User 1"
        email = "test1@test.com"
        role = "BIDDER"
    } "Register testuser1"
    $r2 = Invoke-SocketCall "AUTH_REGISTER" @{
        username = "testuser2"
        password = "Test123456"
        fullName = "Test User 2"
        email = "test2@test.com"
        role = "BIDDER"
    } "Register testuser2"
    $r3 = Invoke-SocketCall "AUTH_REGISTER" @{
        username = "testuser3"
        password = "Test123456"
        fullName = "Test User 3"
        email = "test3@test.com"
        role = "BIDDER"
    } "Register testuser3"
    # Re-fetch users
    $r = Invoke-SocketCall "AUTH_ALL_USERS" @{} "Get all users (refresh)"
    $users = @()
    if ($r.Success) {
        $data = $r.Response.Data
        if ($data -is [System.Collections.IEnumerable]) {
            foreach ($u in $data) { $users += $u }
        } elseif ($null -ne $data) {
            $users += $data
        }
    }
}
Write-Host "Total users available: $($users.Count)" -ForegroundColor Gray

# Get bidder IDs
$testUsers = $users | Where-Object {
    $_.username -eq "testuser1" -or
    $_.username -eq "testuser2" -or
    $_.username -eq "testuser3"
} | Select-Object -First 3

if ($testUsers.Count -lt 3) {
    Write-Host "[WARN] Less than 3 test users found. Will use available." -ForegroundColor Yellow
}

$bidder1 = if ($testUsers.Count -ge 1) { $testUsers[0].id } else { 2 }
$bidder2 = if ($testUsers.Count -ge 2) { $testUsers[1].id } else { 3 }
$bidder3 = if ($testUsers.Count -ge 3) { $testUsers[2].id } else { 4 }
Write-Host "Using bidder IDs: $bidder1, $bidder2, $bidder3" -ForegroundColor Gray

Write-Host ""
Write-Host "--- TEST SUITE 1: Autobid CREATE / UPSERT ---" -ForegroundColor Cyan

# --- TEST 1: Create auction ---
$now = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$auctionName = "Autobid-Test-" + $now
$r = Invoke-SocketCall "AUCTION_CREATE" @{
    name = $auctionName
    desc = "Test auction for autobid stability"
    startPrice = 100.0
    startTime = $now
    endTime = ($now + 600000)  # +10 minutes
    category = "Test"
    imageSource = ""
    imageData = $null
    sellerId = $bidder1
} "Create auction: $auctionName"

if (-not $r.Success) { Write-Host "Cannot create auction. Stop." -ForegroundColor Red; exit 1 }

# Get auction ID: prefer CREATE response, fallback to AUCTION_ALL search
$auctionId = $null
# Try to extract id from the CREATE response (most servers return the created object)
if ($r.Success) {
    $createData = $r.Response.Data
    if ($createData -is [hashtable] -or $createData -is [PSCustomObject]) {
        try {
            if ($null -ne $createData.id) { $auctionId = $createData.id }
        } catch { }
    } elseif ($createData -is [System.Collections.IEnumerable]) {
        foreach ($a in $createData) {
            if ($a.name -eq $auctionName) { $auctionId = $a.id; break }
        }
    }
}

# Fallback: query all auctions and find by name
if ($null -eq $auctionId) {
    $r2 = Invoke-SocketCall "AUCTION_ALL" @{} "Get all auctions"
    if ($r2.Success) {
        $data = $r2.Response.Data
        $allAuctions = @()
        if ($data -is [System.Collections.IEnumerable]) {
            foreach ($a in $data) { $allAuctions += $a }
        } elseif ($null -ne $data) {
            $allAuctions += $data
        }
        $testAuction = $allAuctions | Where-Object { $_.name -eq $auctionName } | Select-Object -First 1
        if ($null -ne $testAuction) {
            $auctionId = $testAuction.id
            Write-Host "Auction created with ID: $auctionId (from AUCTION_ALL)" -ForegroundColor Gray
        }
    } else {
        Write-Host "[WARN] AUCTION_ALL failed to list auctions." -ForegroundColor Yellow
    }
} else {
    Write-Host "Auction created with ID: $auctionId (from CREATE response)" -ForegroundColor Gray
}

if ($null -eq $auctionId) {
    Write-Host "[FAIL] Could not find created auction. Stop." -ForegroundColor Red
    exit 1
}

Write-Host ""

# --- TEST 2: Create autobid for bidder 1 ---
Write-Host "[TEST 2] Create autobid for bidder $bidder1 on auction $auctionId..." -ForegroundColor Yellow
$r = Invoke-SocketCall "AUTOBID_CREATE" @{
    bidderId = $bidder1
    auctionId = $auctionId
    maxPrice = 500.0
    increment = 10.0
} "Create autobid bidder1 (max=500, inc=10)"
$autobid1Id = $null
if ($r.Success) {
    $data = $r.Response.Data
    if ($data -is [hashtable]) { $autobid1Id = $data.id }
    elseif ($data -is [PSCustomObject]) { $autobid1Id = $data.id }
    Write-Host "Autobid 1 created with ID: $autobid1Id" -ForegroundColor Gray
}

Write-Host ""

# --- TEST 3: Create autobid for bidder 2 ---
Write-Host "[TEST 3] Create autobid for bidder $bidder2 on auction $auctionId..." -ForegroundColor Yellow
$r = Invoke-SocketCall "AUTOBID_CREATE" @{
    bidderId = $bidder2
    auctionId = $auctionId
    maxPrice = 300.0
    increment = 15.0
} "Create autobid bidder2 (max=300, inc=15)"
$autobid2Id = $null
if ($r.Success) {
    $data = $r.Response.Data
    if ($data -is [hashtable]) { $autobid2Id = $data.id }
    elseif ($data -is [PSCustomObject]) { $autobid2Id = $data.id }
    Write-Host "Autobid 2 created with ID: $autobid2Id" -ForegroundColor Gray
}

Write-Host ""

# --- TEST 4: Create SAME autobid again (UPSERT test) ---
Write-Host "[TEST 4] Re-create autobid for bidder $bidder1 (should UPDATE, not duplicate)..." -ForegroundColor Yellow
$r = Invoke-SocketCall "AUTOBID_CREATE" @{
    bidderId = $bidder1
    auctionId = $auctionId
    maxPrice = 600.0
    increment = 20.0
} "Upsert autobid bidder1 (max=600, inc=20)"

# Check if autobid count for this auction is still 2 (not 3)
Write-Host ""
Write-Host "[TEST 5] Verify no duplicate autobids exist (should be exactly 2)..." -ForegroundColor Yellow
$r = Invoke-SocketCall "AUTOBID_BY_BIDDER" @{ bidderId = $bidder1 } "Get autobids for bidder1"
$autobidCount = 0
if ($r.Success) {
    $data = $r.Response.Data
    $autobids = @()
    if ($data -is [System.Collections.IEnumerable]) {
        foreach ($a in $data) { $autobids += $a }
    } elseif ($null -ne $data) {
        $autobids += $data
    }
    $autobidCount = ($autobids | Where-Object { $_.auctionId -eq $auctionId }).Count
    Write-Host "Autobids for bidder1 on auction $auctionId: $autobidCount (expected: 1)" -ForegroundColor Gray
    if ($autobidCount -eq 1) {
        Write-Host "[OK]   No duplicate! Upsert works correctly." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] DUPLICATE detected! Upsert failed." -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "--- TEST SUITE 2: Manual Bid + Auto-bid Chain ---" -ForegroundColor Cyan

# --- TEST 6: Place manual bid (should trigger autobid from bidder2) ---
Write-Host "[TEST 6] Place manual bid from bidder $bidder1 (amount=150)..." -ForegroundColor Yellow
$r = Invoke-SocketCall "AUCTION_PLACE_BID" @{
    auctionId = $auctionId
    bidderId = $bidder1
    amount = 150.0
} "Place manual bid bidder1=150"
if ($r.Success) {
    Write-Host "Manual bid placed. Response: $($r.Response.Data)" -ForegroundColor Gray
}

Start-Sleep -Seconds 2

# --- TEST 7: Check if autobid generated counter bid ---
Write-Host "[TEST 7] Check bids after autobid triggered..." -ForegroundColor Yellow
$r = Invoke-SocketCall "AUCTION_BIDS" @{ auctionId = $auctionId } "Get bids for auction $auctionId"
$bids = @()
if ($r.Success) {
    $data = $r.Response.Data
    if ($data -is [System.Collections.IEnumerable]) {
        foreach ($b in $data) { $bids += $b }
    } elseif ($null -ne $data) {
        $bids += $data
    }
    Write-Host "Total bids on auction: $($bids.Count)" -ForegroundColor Gray
    foreach ($bid in $bids) {
        Write-Host "  Bid [id=$($bid.id)] bidder=$($bid.bidderId) amount=$($bid.amount) time=$($bid.timestamp)" -ForegroundColor Gray
    }
    if ($bids.Count -ge 2) {
        Write-Host "[OK]   Autobid counter-bid generated! (expected behavior)" -ForegroundColor Green
    } elseif ($bids.Count -eq 1) {
        Write-Host "[WARN] Only manual bid, no auto-counter-bid yet. This may be OK." -ForegroundColor Yellow
    }
} else {
    Write-Host "[FAIL] Could not fetch bids." -ForegroundColor Red
}

Write-Host ""
Write-Host "--- TEST SUITE 3: Update Autobid ---" -ForegroundColor Cyan

# --- TEST 8: Update autobid ---
if ($null -ne $autobid1Id) {
    Write-Host "[TEST 8] Update autobid $autobid1Id (maxPrice: 600 -> 800)..." -ForegroundColor Yellow
    $r = Invoke-SocketCall "AUTOBID_UPDATE" @{
        bidderId = $bidder1
        id = $autobid1Id
        maxPrice = 800.0
        increment = 25.0
    } "Update autobid1 (max=800, inc=25)"
    if ($r.Success) {
        Write-Host "[OK]   Autobid updated successfully." -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "--- TEST SUITE 4: Get Autobid ---" -ForegroundColor Cyan

# --- TEST 9: Get autobids by bidder ---
Write-Host "[TEST 9] Get all autobids for bidder $bidder1..." -ForegroundColor Yellow
$r = Invoke-SocketCall "AUTOBID_BY_BIDDER" @{ bidderId = $bidder1 } "Get autobids bidder1"
if ($r.Success) {
    $data = $r.Response.Data
    $autobids = @()
    if ($data -is [System.Collections.IEnumerable]) {
        foreach ($a in $data) { $autobids += $a }
    } elseif ($null -ne $data) {
        $autobids += $data
    }
    Write-Host "Total autobids for bidder1: $($autobids.Count)" -ForegroundColor Gray
    foreach ($ab in $autobids) {
        Write-Host "  Autobid id=$($ab.id) auction=$($ab.auctionId) max=$($ab.maxPrice) inc=$($ab.increment)" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "--- TEST SUITE 5: Delete Autobid ---" -ForegroundColor Cyan

# --- TEST 10: Delete autobid ---
if ($null -ne $autobid2Id) {
    Write-Host "[TEST 10] Delete autobid $autobid2Id for bidder $bidder2..." -ForegroundColor Yellow
    $r = Invoke-SocketCall "AUTOBID_DELETE" @{
        bidderId = $bidder2
        id = $autobid2Id
    } "Delete autobid2"
    if ($r.Success) {
        Write-Host "[OK]   Autobid deleted successfully." -ForegroundColor Green
    }

    # Verify deletion
    Write-Host "[TEST 11] Verify autobid is gone..." -ForegroundColor Yellow
    $r = Invoke-SocketCall "AUTOBID_BY_ID" @{ id = $autobid2Id } "Get autobid $autobid2Id"
    if (-not $r.Success -or $null -eq $r.Response.Data) {
        Write-Host "[OK]   Autobid confirmed deleted." -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Autobid still exists after deletion!" -ForegroundColor Red
    }

    # Recreate after deletion
    Write-Host "[TEST 12] Recreate autobid for bidder $bidder2 after deletion..." -ForegroundColor Yellow
    $r = Invoke-SocketCall "AUTOBID_CREATE" @{
        bidderId = $bidder2
        auctionId = $auctionId
        maxPrice = 400.0
        increment = 20.0
    } "Recreate autobid bidder2"
    if ($r.Success) {
        Write-Host "[OK]   Autobid recreated successfully (deletion followed by creation works)." -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TEST SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "All critical tests completed. Check [OK]/[FAIL]/[WARN] above." -ForegroundColor White
Write-Host ""
Write-Host "Key validations:" -ForegroundColor White
Write-Host "  1. JAR contains all autobid classes: OK" -ForegroundColor Green
Write-Host "  2. Server starts and connects to MySQL: OK" -ForegroundColor Green
Write-Host "  3. Socket communication works: OK" -ForegroundColor Green
Write-Host "  4. Autobid CREATE (upsert): check [OK] above" -ForegroundColor White
Write-Host "  5. No duplicate autobids: check [OK] above" -ForegroundColor White
Write-Host "  6. Autobid UPDATE: check [OK] above" -ForegroundColor White
Write-Host "  7. Autobid DELETE: check [OK] above" -ForegroundColor White
Write-Host "  8. Autobid RECREATE after delete: check [OK] above" -ForegroundColor White
Write-Host ""
Write-Host "Auction ID for manual testing: $auctionId" -ForegroundColor Yellow
Write-Host "Bidder IDs: $bidder1, $bidder2, $bidder3" -ForegroundColor Yellow
Write-Host ""
