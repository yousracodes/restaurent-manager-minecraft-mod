# Restaurant Manager Mod

Restaurant Manager is a Minecraft mod that turns a closed room into a working restaurant. Players create a restaurant, set up the important service points, build a menu, and serve customers for emeralds and reputation.

## Main Gameplay Loop

1. Build an enclosed restaurant room.
2. Place a Restaurant Sign outside/at the entrance.
3. Add service blocks like the Door Bell and Cash Register.
4. Link chairs to tables with the Chair/Table Link Tool.
5. Create and apply restaurant menus.
6. Open the restaurant and wait for customers.
7. Serve dine-in plates or takeout packages.
8. Earn emeralds, money stats, completed-order stats, and reputation.

## Restaurant Detection

The Restaurant Sign is the anchor of the restaurant.

When the sign is placed, the mod looks behind the sign for a valid interior space. It scans a few blocks behind the sign, then flood-fills connected air blocks to find the room. If the space is enclosed, large enough, not too huge, and does not overlap another active restaurant, it becomes a registered restaurant.

A detected restaurant stores:

- Its unique id
- The sign position
- The interior bounds of the room
- Its name and owner
- Whether it is open
- Reputation and money/order statistics
- Linked service points like the door bell, counter, seats, tables, and menus

If the sign is broken, the restaurant becomes inactive. In survival, the sign keeps the restaurant id so it can be placed again and reconnect to the same restaurant data.

## Door Bell

The Door Bell acts as the customer entrance and exit point.

When a Door Bell is placed near a restaurant, the mod tries to attach it to the nearest restaurant layout. Customers use it as the first target when entering and as the exit point when leaving. If no explicit entrance/exit marker is used, the door bell becomes the practical entrance/exit marker.

## Cash Register

The Cash Register is the counter point, especially for takeout customers.

When placed near a restaurant, it is assigned to that restaurant and marked as a counter. Takeout customers move to the register and wait there for their package. The mod also calculates an interaction position based on the register direction, so customers stand on the correct side of the counter.

## Seating And Tables

Seating is controlled through marked points saved in the world.

The important mark types are:

- Seat
- Table
- Queue
- Counter
- Entrance
- Exit
- Door Bell

The Chair/Table Link Tool is used in two clicks:

1. Click a chair or seat position.
2. Click the table that belongs to that chair.

While holding the tool in either hand, nearby marked chairs have blue outlines (`#0072B2`) and tables have orange outlines (`#D55E00`). This colorblind-friendly pairing avoids relying on red versus green, and the outlines also follow each block's distinct shape. Aim at a linked chair or table to see its connection; after selecting a chair, aim at a table to preview the new link. Sneak-click a marker to remove it. The overlay disappears when you put the tool away.

The mod only accepts the link if both points belong to the same restaurant. Dine-in customers prefer linked seats, reserve one, sit down, and then look for food on the linked table.

## Menus And Food Requests

Restaurant Menu items let the player choose which foods the restaurant serves. Menus can be organized by category, then applied to the Restaurant Sign.

When a customer creates an order, the mod tries to pick from the restaurant's menu. If the restaurant has no valid menu food, it falls back to any edible food found in the item registry.

Customers can ask for 1 to 3 food items per visit.

## Customers

Customers spawn automatically near active open restaurants when a player is nearby. Better restaurants can handle more customers and have a better spawn chance.

There are two customer types:

- Dine-in customers
- Takeout customers

Dine-in customers enter, reserve a seat, sit at a linked table, and wait for food on a plate. They can be served directly with a plate, or they can detect a filled plate placed on their table.

Takeout customers go to the Cash Register or another counter mark and wait for a Takeout Package. The package must contain the requested food items.

If seats or counters are unavailable, customers may wait in queue spots. If they wait too long, they become impatient and can eventually leave.

## Serving Food

Plates and Takeout Packages can each hold up to 3 food items.

For dine-in service:

- Put the requested food on a Plate.
- Give it to the customer or place it on the linked table.
- Wrong or missing food makes the customer react negatively.

For takeout service:

- Put all requested food into a Takeout Package.
- Interact with the takeout customer while holding the package.
- The package is accepted only if it contains the correct order.

## Patience

Customers track how long they wait.

Their patience can move through:

- Happy
- Neutral
- Impatient
- Angry
- Leaving

Long waits and wrong orders make patience worse. If a customer waits too long, the order fails, the customer leaves, and the restaurant loses reputation.

## Rewards And Reputation

Food rewards are based on the food's nutrition and saturation. Better food gives more emeralds, with a cap to keep rewards reasonable.

Restaurant star rating increases the payout:

- 1 star: normal price
- Each extra star: higher reward scaling

When an order is completed:

- The player receives emeralds.
- The restaurant records money earned.
- Completed order stats increase.

When the whole customer visit is completed:

- Customers served increases.
- Reputation increases based on customer patience.
- Extra reputation can be added for menu variety.

If a customer leaves unserved:

- Failed order stats increase.
- Lost customer stats increase.
- Reputation decreases.

## Important Items And Blocks

- Restaurant Sign: detects and owns the restaurant.
- Door Bell: entrance and exit point for customers.
- Cash Register: counter point for takeout service.
- Restaurant Menu: stores menu categories and food choices.
- Chair/Table Link Tool: links seats to tables.
- Plate: holds dine-in food.
- Takeout Package: holds takeout food.

## In Short

The mod is built around a simple restaurant simulation: detect a room, register it as a restaurant, mark the layout, spawn customers, generate food requests, serve the correct items, then reward the player and update the restaurant's reputation.
