# Money Holding Mechanism

## Escrow Flow

```
Deposit 1,000,000 VND
  └── Balance: 1,000,000 | Available: 1,000,000 | Reserved: 0

Place bid 500,000 VND
  └── Balance: 1,000,000 | Available: 500,000 | Reserved: 500,000
  └── Money is HELD (escrow), not deducted

Outbid by another user
  └── Balance: 1,000,000 | Available: 1,000,000 | Reserved: 0
  └── Money is RELEASED back to available

Win auction → Seller marks PAID
  └── Balance: 500,000 | Available: 500,000 | Reserved: 0
  └── Money is CAPTURED from escrow

Auction cancelled
  └── Balance: 1,000,000 | Available: 1,000,000 | Reserved: 0
  └── Money is REFUNDED
```

## State Table

| Stage | Balance | Reserved | Available | Description |
|-------|---------|----------|-----------|-------------|
| Deposit | 1,000,000 | 0 | 1,000,000 | Fresh deposit |
| Place bid | 1,000,000 | 500,000 | 500,000 | Money held in escrow |
| Outbid | 1,000,000 | 0 | 1,000,000 | Money released |
| Win + PAID | 500,000 | 0 | 500,000 | Money captured |
| Cancelled | 1,000,000 | 0 | 1,000,000 | Money refunded |

## Key Methods

| Method | Action | Stage |
|--------|--------|-------|
| `reserveAdditionalFunds()` | Hold money in escrow | On bid |
| `releaseReservedFundsInternal()` | Release back to available | On outbid |
| `captureReservedFunds()` | Deduct from escrow | On PAID |
| `refundCapturedFunds()` | Return deducted money | On cancel |

## Verify It Works

Run server and watch for `[Wallet]` logs:
```
[Wallet] Top-up completed - Balance=1,000,000, Available=1,000,000
[Wallet] Successfully reserved 500,000 - New reserved: 500,000
[Wallet] Successfully released 500,000 - New reserved: 0
```
