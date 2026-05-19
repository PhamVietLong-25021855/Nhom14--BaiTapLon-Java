# Quick Action Guide - Bid Placement Fix

## Test in 5 Minutes

### Step 1: Recompile
```bash
mvn clean -DskipTests package
```

### Step 2: Run Server
```bash
cd server
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -cp "target/classes:target/lib/*" userauth.server.AuctionServerMain
```
Keep terminal open to see `[Wallet]` logs.

### Step 3: Test
1. Create user account
2. Top-up 1,000,000 VND
3. Place bid 500,000 VND on a running auction

### Step 4: Verify
Check console for these logs:

**After deposit:**
```
[Wallet] Top-up completed for user X - After: Balance=1,000,000, Available=1,000,000
```

**After bid:**
```
[Wallet] Successfully reserved 500,000 for user X
```

## Expected Results

| Operation | Balance | Available | Reserved |
|-----------|---------|-----------|----------|
| Top-up 1M | 1,000,000 | 1,000,000 | 0 |
| Bid 500K | 1,000,000 | 500,000 | 500,000 |
| Bid 300K more | 1,000,000 | 200,000 | 800,000 |

## Troubleshooting

**Problem:** "Insufficient available wallet balance"
- Check `[Wallet] Top-up completed` appears in console
- Verify balance matches what was deposited
- Try restarting the server

**Problem:** No `[Wallet]` logs appear
- Ensure console output is not redirected
- Verify server is actually running
