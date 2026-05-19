# Test Cases: Bid Placement After Deposit

## Scenario 1: Fresh Deposit and Bid

1. Create user (balance = 0)
2. Deposit 1,000,000 VND
   - Check console: `[Wallet] Top-up completed`
   - UI shows: Balance = 1,000,000
3. Place bid 500,000 VND on running auction
   - Console shows: `Successfully reserved 500,000`
   - UI: Available = 500,000, Reserved = 500,000
4. Place second bid 400,000 VND
   - Console: `Same bidder case - delta: 400,000`
   - Reserved becomes 900,000, Available = 100,000

## Scenario 2: Insufficient Balance

1. Deposit 1,000,000 VND
2. Try to bid 1,100,000 VND
   - Expected: "Insufficient available wallet balance. Available: 1,000,000. Required: 1,100,000."
   - Console shows failed balance check

## Scenario 3: Being Outbid

**Setup:** User A and User B, each funded 1,000,000

1. User A bids 500,000 → Reserved A = 500,000
2. User B bids 600,000
   - Console shows:
     - `Releasing previous bidder - User A loses lead, releasing 500,000`
     - `Reserving new bidder - User B becomes leader, reserving 600,000`
   - Result:
     - User A: Reserved = 0, Available = 1,000,000 (can bid again)
     - User B: Reserved = 600,000, Available = 400,000
3. User A bids 700,000 → Works (funds were released)

## Scenario 4: Win and Settle

1. User wins auction with 500,000 reserved
2. Seller marks auction as PAID
   - Console: `captureReservedFunds`
   - Balance decreases by 500,000

## Scenario 5: Auction Cancelled

1. User has 500,000 reserved for winning bid
2. Auction is cancelled
   - Console: `releaseReservedFunds` or `refundCapturedFunds`
   - Balance returns to full amount
