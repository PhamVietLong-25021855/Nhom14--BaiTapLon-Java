# Bug Report: Bid Placement After Deposit

## Problem
Users can deposit money successfully but cannot place bids.

## Root Causes

### 1. Missing Wallet Service in AuctionService Constructor
**File:** `AuctionService.java`

When `AuctionService` is constructed with only 2 parameters, `walletService` is set to `null`. This bypasses all fund validation.

```java
public AuctionService(AuctionDAO auctionDAO, AutoBidDAO autoBidDAO) {
    this(auctionDAO, autoBidDAO, null);  // walletService = null
}
```

### 2. Insufficient Logging
No visibility into wallet balance checks and fund reservations, making debugging impossible.

## Fixes Applied

### AuctionService.java
- Added try-catch to convert `ValidationException` to `InvalidBidException` in `placeBid()`
- Clear error messages showing why bid failed

### WalletService.java
Added detailed `[Wallet]` logging to 5 methods:
- `createTopUpRequest()` — top-up before/after balance
- `ensureSufficientAvailableBalanceForBid()` — balance check with usable amount
- `reserveAdditionalFunds()` — reservation attempt and result
- `releaseReservedFundsInternal()` — fund release
- `applyReservationTransition()` — fund transition between bidders

## Console Log Reference

**Top-up success:**
```
[Wallet] Top-up completed for user 5 - After: Balance=1,000,000, Available=1,000,000
```

**Bid success:**
```
[Wallet] User 5 - Balance: 1,000,000, Available: 1,000,000, Bid Amount: 500,000
[Wallet] Successfully reserved 500,000 for user 5 - New reserved: 500,000
```

**Bid failure (not enough funds):**
```
[Wallet] Insufficient available wallet balance. Available funds: 100,000. Required: 500,000.
```
