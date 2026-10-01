## 1.1.0

### Added
- Large widget size (4 rows tall or more) with the mining and network data from the mempool.kilombino.com header: what 1 TH/s earns per day, the cheapest rent of 1 TH/s, the energy of 1 XBT in kWh, YSH and the blockchain size.
- The same data on the app screen.

### Changed
- The widget asks for new data on a random minute of the quarter hour, so many phones don't all ask at once.
- The server can tell the app to poll less often, and the app slows down by itself when the server doesn't answer.

## 1.0.2

### Changed
- New app icon: a round gold Bitcoin coin, also used inside the widget and the app.
- The SHA256d chain is now called the Spamchain everywhere in the app.

## 1.0.1

### Changed
- Round app icon, like the Kilombino Bitcoin-Blake2b wallet (adaptive icon that fills the whole circle).
- Round logo inside the widget and the app.
- Published and signed by Kilombino, with the same signing key as the Kilombino Bitcoin-Blake2b wallet.

## 1.0.0

### Added
- Home screen widget with the live XBT (Bitcoin-BLAKE2b / BIP110) price in USD and its 24h change.
- Full size adds the ratio to the Spamchain (in Poolsats or Poolcoins), the 24h range, the block height and the time of the last update.
- Compact layout for small widgets, chosen automatically when the widget is resized.
- Refreshes every 15 minutes in the background, plus a refresh button that respects xbt.live's one-request-per-minute limit.
- Old data is flagged when the phone has been offline for a while, instead of showing a blank widget.
- App screen with the same data, an "Add widget to home screen" button and a Poolsats/Poolcoins switch.
- English and Spanish.
