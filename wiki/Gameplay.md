# Gameplay

This page describes the defaults. A modpack can change every number here through the [configuration](Configuration) or [datapacks](Modpack-Makers).

---

## The thirst bar

The thirst bar sits right above the hunger bar: 10 droplets, 20 points of thirst. Like hunger, thirst has a hidden reserve, **quenched**, that is used up before the droplets start to empty. Quenched is never higher than thirst.

<!-- SCREENSHOT: close-up of the HUD: full hunger bar and a half-empty thirst bar, with the quenched outline visible on the droplets -->

Holding something that restores thirst makes the droplets it would fill flash on the bar, and an outline on the droplets shows the current quenched. The droplets change colour while an effect applies to them: brown with Dehydration, grey-blue while overhydrated, green with Poison, cyan with Quenchness, mint with Hydrated.

<!-- SCREENSHOT: thirst bar tinted brown under Dehydration, next to a normal one (two crops side by side) -->

---

## What makes you thirsty

Thirst goes down with the same activity that makes you hungry (sprinting, jumping, fighting, breaking blocks, healing), a little faster than hunger. The climate scales it:

- **Hot and dry biomes** (desert, badlands, savanna) drain thirst faster. **Cold and wet ones** drain it slower. High ground is colder, so it drains a bit slower too.
- **The Nether** drains thirst three times faster. **The End** counts as cold, about like a snowy biome.
- **Fire Resistance** stops the loss, and **Fire Protection** on armor lowers it.
- **Nausea** drains thirst.
- **Peaceful**: thirst does not go down, and slowly fills up.

With [Serene Seasons](#other-mods) installed, the season changes the biome temperature, so winter lowers the loss where it cools the biome.

---

## When thirst runs low

- **3 droplets or less**: you cannot sprint, even with a full hunger bar. A sprint in progress stops too, as with hunger.
- **Not full**: health regenerates slower. Fast regeneration needs a full bar, and normal regeneration needs at least 19 points (9.5 droplets).
- **Empty**: you take damage every 2 seconds. On Easy it stops at 5 hearts, on Normal at half a heart, and on Hard it can kill you.

After respawning, thirst is full.

---

## Drinking

| Source | Thirst | Quenched | Notes |
|---|---|---|---|
| Water bottle | 4 | 5 | Two droplets. Pure water (5) gives 6 and 8 |
| Terracotta water bowl | 4 | 5 | Same as a bottle. The empty bowl goes back to your inventory |
| Sip by hand | 1 | 1 | Sneak and right-click water with both hands empty |
| Rain | 1 | 1 | Look up while it rains on you, one sip every half second |
| Milk bucket | 4 | 4 | |
| Honey bottle | 3 | 4 | |

Glass bottles and terracotta bowls also fill from flowing water, not only from source blocks. Buckets still need a source.

Chests in dungeons, mineshafts, shipwrecks, Nether fortresses and bastions can hold a few water bottles, acceptable or pure.

<!-- SCREENSHOT: player sneaking at a river bank with empty hands, splash particles from drinking by hand -->

### Food

Some food hydrates a little and some makes you thirstier. Plain food (bread, meat, most dishes) does nothing to thirst. The tooltip of every item that changes thirst shows how much, as droplets.

| Food | Thirst | Quenched |
|---|---|---|
| Melon slice | 4 | 4 |
| Beetroot soup | 3 | 3 |
| Apple, golden apple, potato, sweet and glow berries, chorus fruit, mushroom, rabbit and suspicious stew | 2 | 2 |
| Carrot, golden carrot, beetroot | 1 | 1 |
| Cake, per slice | 1 | 1 |
| Rotten flesh, spider eye, pufferfish, poisonous potato | -3 | -3 |

<!-- SCREENSHOT: item tooltip of a melon slice showing the thirst droplets and quenched icons -->

### Drinking too much

Drinking while full is not free. What does not fit adds up, and three bottles drunk past full in a short time give you **Overhydrated**: 10% slower movement and a few seconds of Nausea. The extra drains off at one point per second.

---

## Water purity

Water has one of six purity levels. Bottles, bowls and buckets of water show it in their tooltip, and the water in bottles and bowls is tinted by it, from brown to near white.

| Level | Name | Drinking it |
|---|---|---|
| 0 | Contaminated | Nausea and Hunger, 40% chance of Poison |
| 1 | Dirty | 60% chance of Nausea and Hunger, 15% chance of Poison |
| 2 | Murky | 25% chance of Nausea and Hunger |
| 3 | Acceptable | 5% chance of Nausea and Hunger |
| 4 | Clean | Nothing |
| 5 | Pure | Nothing, and it hydrates more: 6 thirst and 8 quenched per bottle |

Poisoned water still hydrates by default. A server can make Poison cancel the drink instead (`purity.toml` `general.quenchWhenDebuffed`).

Water of level 2 or worse drunk in a hot climate (a desert, a savanna, badlands, the Nether) also gives **Dehydration**, which makes thirst drop faster for 30 seconds.

<!-- SCREENSHOT: six water bottles in a row in the inventory, one per purity, showing the tint from brown to near white -->

### Where clean water is

Water taken from the world gets its purity from where it is:

- **Oceans** are always contaminated (0). They stand in for salt water.
- **Most biomes** (plains, forests, rivers) start dirty (1).
- **Taiga, birch forests and snowy biomes** start murky (2). **Mountains and windswept hills** start acceptable (3).
- **Deserts and badlands** start contaminated and never get better than murky (2). **Swamps and mangroves** start contaminated and never get better than dirty (1).
- **Running water** is one level better than still water.
- **Height** helps: water 30 blocks above sea level is one level better, 60 blocks two, 100 blocks three. Deep underground works the same way: 16, 48 and 80 blocks below sea level.

`/blue_droplets debug purity` (operators) shows the purity of the water you look at and why.

Water you pour keeps its purity. Pouring a bucket of sea water into a mountain lake and filling it again gives sea water back, and an infinite source made from poured water takes the worst purity beside it.

### Cleaning water

| Method | Result |
|---|---|
| Water cauldron | Murky (2), whatever went in |
| Water cauldron over a heat source (campfire, fire, magma block, lava) | Clean (4) |
| Campfire | One level up per cook |
| Furnace | Two levels up per cook |
| Smoker | Two levels up per cook, twice as fast. Off by default: turn on the datapack `mod/blue_droplets:datapacks/purify_smoking` |

Cooking works on water bottles, water buckets and terracotta water bowls, and it reaches pure (5). The cauldron never gives pure water.

<!-- SCREENSHOT: water cauldron on a lit campfire, player filling a bottle from it (tooltip "Clean") -->

With JEI installed, the "Water Purification" page shows the cauldron and its heat sources, and the cooking recipes show in their own pages.

If purity is turned off by the server, all water is the same, and only thirst matters.

---

## Items

| Item | How to get it | Use |
|---|---|---|
| Clay bowl | 3 clay balls in a V shape give 4 | Cook it into a terracotta bowl |
| Terracotta bowl | Clay bowl in a furnace or on a campfire | Fill it with water like a glass bottle, from the world or a cauldron |
| Terracotta water bowl | Terracotta bowl filled with water | Drink it like a water bottle. It can be purified like a bottle |

Everything is also in the Blue Droplets creative tab, where the water containers are listed once per purity.

---

## Effects and potions

| Effect | What it does | Source |
|---|---|---|
| Quenchness | Restores 1 thirst and 1 quenched every 2 seconds per level | Potion of Quenchness |
| Hydrated | Thirst drains at half the speed per level | `/effect`, or datapacks and config |
| Dehydration | Thirst drains faster, like Hunger does for food | Dirty water in a hot climate |
| Overhydrated | 10% slower per level | Drinking far past full |

| Potion | Effect | Brewing |
|---|---|---|
| Potion of Quenchness | Quenchness I, 0:45 | Awkward potion + prismarine crystals |
| Long | Quenchness I, 1:30 | Potion of Quenchness + redstone |
| Strong | Quenchness II, 0:22 | Potion of Quenchness + glowstone dust |

Gunpowder turns them into splash potions and dragon's breath into lingering potions, as for vanilla potions.

---

## Other mods

- **Jade**: shows the purity of a water cauldron (the purity its water comes out with) and of water in tanks.
- **JEI**: a "Hydration" page lists every item that changes thirst, and a "Water Purification" page shows the cauldron.
- **AppleSkin**: turning off one of AppleSkin's food visuals also turns off its thirst counterpart. Blue Droplets draws its own quenched outline, drink preview and tooltip icons, with or without AppleSkin.
- **Serene Seasons**: the season changes the climate of thirst loss.
- **Traveler's Backpack**: drinking water through the hose hydrates like a water bottle, with the purity of the water in the tank. Water the hose sucks from the world keeps its purity.

Tough As Nails, Legendary Survival Overhaul, Homeostatic, Survive and Thirst Was Taken 2 have their own thirst. With one of them installed the game starts with a warning, and both thirst systems run until one is turned off. On Minecraft 1.x, Thirst Was Taken and Thirst Was Reclaimed (mod id `thirst`) cannot be installed together with Blue Droplets; on 26.x, where Thirst Was Taken has no build, Thirst Was Reclaimed only gets the warning too.

Other versions of Blue Droplets work with more mods, Create and Farmer's Delight among them. See the version table on the [home page](Home).
