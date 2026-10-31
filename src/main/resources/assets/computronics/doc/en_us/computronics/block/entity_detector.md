# Entity Detector

High-precision directional entity and player scanner with configurable detection cone.

## Component Name
`entity_detector`

## Methods

* `getFacing(): string`
  Returns facing direction (`north`, `south`, `west`, `east`).
* `getRange(): number`
  Returns scanning range.
* `setRange(range: number): number`
  Sets scanning range (1 to 32 blocks).
* `count([range: number]): number`
  Counts all entities in front of detector.
* `getPlayerCount([range: number]): number`
  Counts players in detection cone.
* `isPlayerInRange([range: number]): boolean`
  Returns true if any player is in range.
* `scan([range: number]): table`
  Returns array of entity tables (`name`, `uuid`, `type`, `distance`, `x`, `y`, `z`, `isPlayer`, `health`, `maxHealth`, `armor`, `mainHand`).
* `setSignalEnabled(enabled: boolean): boolean`
  Enables or disables automatic signal emission when entities enter zone.
* `isSignalEnabled(): boolean`
  Checks if signal emission is active.
