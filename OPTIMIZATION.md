# Optimization Notes

These are the main performance areas to keep an eye on as the restaurant systems grow. The mod should be fine for normal singleplayer use and small servers, but larger setups with many restaurants, marked seats, and active customers will benefit from the changes below.

## Current Grade

Performance is around **B-** right now.

The customer caps keep things under control, but several systems still use broad scans or frequent save/pathing updates. Those are fine at small scale and become noticeable when the number of restaurants or layout marks grows.

## Highest Value Changes

### 1. Index seating marks by type

`SeatingMarkSavedData` currently searches marks by streaming all saved points, filtering by type, sorting by distance, and then taking the first result.

That should be changed to keep marks grouped by type:

```text
Map<MarkType, List<MarkedPoint>>
```

Nearest-seat/counter/table lookups can then scan only the relevant type and keep the closest match in one pass. There is no need to sort the whole result list.

This is the biggest optimization target.

### 2. Avoid saving temporary occupancy

Seat, queue, and counter reservations call `setDirty()` when occupied or released. Those reservations are temporary runtime state, so they do not need to be persisted as aggressively as permanent marks.

Keep the actual marks saved, but consider not saving temporary occupants every time a customer reserves or releases a spot.

### 3. Throttle navigation updates

Customer goals call `navigation.moveTo(...)` repeatedly while moving to seats, counters, and exits.

Pathing should be reissued only when:

- the target changes
- the customer gets stuck
- a short cooldown expires, such as every 10 ticks

This reduces repeated path recalculation when many customers are active.

## Server Safety Limits

The mod already caps customers per restaurant by star rating. Add global caps too:

```text
maxCustomersPerRestaurant
maxCustomersPerDimension
maxCustomersTotal
```

This gives server owners a simple way to protect TPS if a world has many restaurants.

## Spawn System

Customer spawning checks all restaurants every 15 seconds in every loaded level. This is acceptable for small worlds, but it can be staggered.

Better approach:

- skip closed, inactive, or menu-less restaurants as early as possible
- process only part of the restaurant list per interval
- spread checks across multiple ticks instead of doing all restaurants at once

## Restaurant Layout Binding

Doorbell and cash register binding scans nearby blocks only when a restaurant is registered or refreshed. That is fine because it is not a tick-based system.

Keep it event-based. Do not move this scan into a per-tick check.

## Customer State Updates

`updateRequestName()` updates custom names and synced order strings. It should only run when something actually changes:

- order changes
- state changes
- patience changes
- service type changes

Avoid calling it repeatedly while the displayed text is identical.

## Stuck Customer Cleanup

Customers that cannot find a seat or counter should leave after a reasonable timeout. This is already partly handled through patience timers, but it is worth keeping strict.

Stuck customers are expensive because they can keep retrying pathing and reservation searches.

## Recommended Order

1. Index marks by type and remove sorting from nearest searches.
2. Stop saving temporary occupancy changes.
3. Throttle repeated `navigation.moveTo(...)` calls.
4. Add global customer caps.
5. Stagger restaurant spawn checks.

Those changes should move performance from roughly **B-** to **A-** for normal server use.
