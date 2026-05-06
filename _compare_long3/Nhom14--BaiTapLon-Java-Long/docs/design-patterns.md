# Design Patterns

## Singleton

`userauth.event.AuctionEventBus` is implemented as a singleton through `getInstance()`.

- The application shares one event hub for all auction updates.
- Controllers subscribe while active and unsubscribe when the screen is deactivated.

## Factory Method

`userauth.service.AuctionSettlementHandlerFactory` creates the terminal-state handler for a finished auction.

- `PAID` uses a dedicated settlement handler.
- `CANCELED` uses a separate handler with different validation and state mutation.
- `AuctionService` delegates `FINISHED -> PAID/CANCELED` to the factory output instead of hard-coding both branches inline.

## Observer

The observer flow is centered on:

- `AuctionEventBus`
- `AuctionEventListener`
- `AuctionEvent`

Current observers:

- `BidderDashboardViewController`
- `SellerDashboardViewController`

Current publishers:

- `AuctionService` when bids arrive, anti-sniping extends time, statuses change, and finished auctions are settled.
