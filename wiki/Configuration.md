# Configuration

All TOML files live in `config/blue_droplets/`. They are NeoForge configs: missing keys are added with their default and a comment, values out of range go back to the default, invalid list entries are removed, and edits are picked up while the game runs. When a server's file changes, the server rebuilds the drink tables, recomputes every player's thirst loss and sends the new values to all players, with no `/reload` or relog.

Values that belong to an item, biome or dimension live in datapacks instead: see [Modpack makers](Modpack-Makers).

This page lists the keys of Minecraft 26.3. Older versions have more keys, for the mods they work with: see the page of your version on the [home page](Home).

---

## Files

| File | Type | Content |
|---|---|---|
| `gameplay.toml` | common | Thirst loss, drinking, damage, regeneration, sprint, effects, loot |
| `purity.toml` | common | Water purity in the world and its effects |
| `items.toml` | common | Per-item overrides, purity containers, salty items, keywords, recipe inference |
| `compat.toml` | common | One section per optional mod (on 26.3, `[sereneseasons]`) |
| `client.toml` | client | HUD and tooltips; each player's own |

The gameplay files are common configs, not per-world server configs: they exist before a world loads (tooltips and stack sizes need them) and one file serves every world. On a dedicated server the server's values win. The ones the client needs (sprint rule, hand drinking and its two-hands rule, drink tables, purity containers, `purity.enabled`, `defaultPurity`, the pure water bonus, filling from flowing water, water bottle stack size) are sent to each player on join, after `/reload` and after a config change.

---

## Commands

All commands need operator rights (permission level 2). `/thirst` is an alias of `/blue_droplets`.

| Command | What it does |
|---|---|
| `/blue_droplets query <player>` | Shows the player's thirst and quenched |
| `/blue_droplets set <player> <thirst> <quenched>` | Sets them (0-20; quenched is capped at thirst) |
| `/blue_droplets enable <players> <true\|false>` | Turns thirst on or off for those players. Off, their thirst does not change |
| `/blue_droplets debug exhaustion [player]` | Mode, every factor of the thirst loss multiplier, the `thirst_drain` attribute, the total, exhaustion, thirst and quenched |
| `/blue_droplets debug purity` | Purity of the water you look at (or the block at your feet): poured water, salt water rule, base and where it comes from, altitude, still/running and biome deltas, cap |
| `/blue_droplets config check` | Every config problem found, or "no problems found" |
| `/blue_droplets infer <item>` | How recipe inference estimates the item: each recipe, each ingredient's value, multiplier, result count, the estimate and why it would not be used |

<!-- SCREENSHOT: chat output of /blue_droplets debug exhaustion in a desert at noon -->

### Checking a config

Problems that a single key cannot show (unknown effect or item ids, overlapping altitude bands, curve points out of order, invalid keyword patterns, `slowRegenMinThirst` above `fullRegenMinThirst`) are logged as one warning each time the world loads, after `/reload` and after a config file changes. Bad entries are skipped and nothing crashes. `/blue_droplets config check` lists the same problems.

---

## Moving from Thirst Was Taken

On first start, if a new file does not exist yet, its values are copied from the old files (`common.toml`, `item_settings.toml`, `container.toml`, `keyword.toml`, also from Thirst Was Taken's `config/thirst/`). The old files are then renamed to `*.toml.old` and no longer read. A warning in the log lists what was moved. Modpacks that ship `defaultconfigs/` must use the new file names and keys.

| Old file and key | New file and key |
|---|---|
| `common.toml` `General.thirstDepletionModifier` | `gameplay.toml` `depletion.multiplier` |
| `General.thirstDepletionInPeace` | `depletion.inPeaceful` |
| `General.netherThirstDeletionModifier` | `depletion.netherMultiplier` |
| `General.fireResistanceDehydration` | `depletion.fireResistancePercent` |
| `General.depletesWhenNausea` | `depletion.nauseaDepletes` |
| `General.moveSlowWhenThirsty` | `sprint.blockedWhenThirsty` |
| `General.DrinkRainWater` | `drinking.rain` |
| `General.EnableLoot` | `loot.enabled` |
| `Drinking Mechanics.waterBottleStacksize` | `drinking.waterBottleStackSize` |
| `Drinking Mechanics.ExtraHydrationConvertToQuenched` | `drinking.extraThirstToQuenched` |
| `Drinking Mechanics.dehydrationHaltsHealthRegen` | `regeneration.haltedWhenThirsty` |
| `Drinking Mechanics.healthRegenDepletesHydration` | `regeneration.depletesThirst` |
| `Drinking Mechanics.healthRegenDehydrationIsBiomeDependent` | `regeneration.climateDependent` |
| `Drinking Mechanics.canDrinkByHand`, `DrinkBothHandNeeded`, `handDrinkingHydration`, `handDrinkingQuenched`, `handDrinkingCooldown` | `hand.enabled`, `hand.bothHandsEmpty`, `hand.thirst`, `hand.quenched`, `hand.cooldownTicks` |
| `World.*` | `purity.toml` `world.*` (same key names) |
| `Purity-related Effects.defaultPurity`, `quenchThirstWhenDebuffed` | `purity.toml` `general.defaultPurity`, `general.quenchWhenDebuffed` |
| `Purity-related Effects.*Percentage` (8 values) | `purity.toml` `[effects]` lists (see below) |
| `item_settings.toml` `drinks`, `foods`, `itemsBlacklist` | `items.toml` `overrides.drinks`, `overrides.foods`, `overrides.blacklist` |
| `container.toml` `Containers` | `items.toml` `containers.containers` |
| `keyword.toml` (all keys) | `items.toml` `keywords.*` (`enabled`, `drinkThirst`, `drinkQuenched`, `soupThirst`, …, `blacklist`, `drink`, `soup`, `fruit`) |

Thirst Was Taken had four purity levels. Its purity values move to the six of Blue Droplets: 0 to 0, 1 to 1, 2 to 3, 3 to 5.

---

## `gameplay.toml`

### `[depletion]`

Thirst loss per tick is `activity × M`, where `M = climate × fire protection × fire resistance × rain/thunder × day/night × sun × altitude × water × Hydrated × blue_droplets:thirst_drain`. Every `exhaustionPerPoint` (4.0) of exhaustion removes one quenched point, or one thirst point when quenched is 0. `M` (except the attribute) is recomputed once a second per player and right away after armor, effect, dimension or config changes. `/blue_droplets debug exhaustion` shows each factor.

| Key | Default | Meaning |
|---|---|---|
| `mode` | `MIRROR_FOOD` | Where activity comes from. `MIRROR_FOOD`: the exhaustion vanilla adds to hunger, including what other mods add. `OWN`: Blue Droplets counts the activities of `[depletion.activity]` itself, with vanilla's numbers by default, without reading hunger |
| `basalPerTick` | `0.0` | Exhaustion added every tick, even idle (0-1) |
| `exhaustionPerPoint` | `4.0` | Exhaustion that removes one point (0.1-100) |
| `nauseaPerTick` | `0.06` | Exhaustion per tick while nauseous |
| `multiplier` | `1.2` | How much faster thirst goes down than hunger. Part of the climate factor (0-10) |
| `inPeaceful` | `false` | Thirst goes down in Peaceful |
| `netherMultiplier` | `3.0` | Replaces the climate factor where water evaporates (the Nether), unless the dimension type has its own `thirst_multiplier` (1-5) |
| `fireResistancePercent` | `0` | Thirst loss with Fire Resistance, in percent (0 = none) |
| `nauseaDepletes` | `true` | Nausea makes thirst go down |
| `fireProtectionPerLevel` / `fireProtectionMaxLevels` | `0.046875` / `12` | Thirst loss removed per level of Fire Protection on armor, and the most levels counted |

`MIRROR_FOOD` stays the default because it also counts exhaustion from other mods and from vanilla actions that `OWN` does not see.

"Where water evaporates" is the environment attribute `minecraft:gameplay/water_evaporates`, so a biome or dimension from a datapack or mod that sets it counts as the Nether.

### `[depletion.climate]`

| Key | Default | Meaning |
|---|---|---|
| `formula` | `LEGACY` | `LEGACY`: `multiplier × temperature / humidity` of the biome, softened below 1. `CURVE`: `multiplier × temperatureCurve(temperature) × humidityCurve(downfall)` |
| `legacyHarshness` | `0.5` | LEGACY: part of a multiplier below 1 that is kept (0.5 = halfway to 1) |
| `temperatureCurve` | `["-0.5,0.7", "0.8,1.0", "2.0,1.5"]` | CURVE: `"temperature,multiplier"` points in ascending order, straight lines between them, flat beyond the ends |
| `humidityCurve` | `["0.0,1.2", "0.4,1.0", "1.0,0.8"]` | CURVE: the same for the biome's downfall (0 dry to 1 wet) |
| `rain` / `thunder` | `1.0` / `1.0` | When rain falls on the player (thunder replaces rain in a storm) |
| `day` / `night` | `1.0` / `1.0` | In dimensions with a day cycle |
| `sun` | `1.0` | Day, not raining, sky visible |
| `inWater` / `underwater` | `1.0` / `1.0` | In water with the head out / fully underwater |
| `altitude` | `[]` | `"minY,maxY,multiplier"` from the dimension's sea level. The first band that contains the player applies |

The temperature is vanilla's biome temperature at the player's position, which gets colder with height, as vanilla uses it for snow. With Serene Seasons it is the temperature of the current season (`compat.toml` `[sereneseasons]`).

A dimension type can replace the whole climate factor with `thirst_multiplier` in the `blue_droplets:dimension_water` data map (see [Modpack makers](Modpack-Makers#blue_dropletsdimension_water)). Blue Droplets sets the End to `0.6`, as cold as a snowy biome.

### `[depletion.activity]`

In `OWN` mode these are the sources, with vanilla's hunger numbers. In `MIRROR_FOOD` mode only `ridingMultiplier` and `sleepingMultiplier` apply.

| Key | Default | Meaning |
|---|---|---|
| `sprintPerMeter` | `0.1` | OWN: per meter sprinted on the ground |
| `swimPerMeter` | `0.01` | OWN: per meter swum or walked in water |
| `jump` / `sprintJump` | `0.05` / `0.2` | OWN: per jump |
| `attack` | `0.1` | OWN: per attack |
| `blockBreak` | `0.005` | OWN: per block broken |
| `damageMultiplier` | `1.0` | OWN: times the exhaustion of the damage type taken |
| `healPerHealth` | `6.0` | OWN: per health point regenerated from food |
| `ridingMultiplier` | `0.0` | Both modes: activity while riding (0 = none) |
| `sleepingMultiplier` | `1.0` | Both modes: activity while sleeping |

### `[regeneration]`

| Key | Default | Meaning |
|---|---|---|
| `haltedWhenThirsty` | `true` | Health regenerates slower, or not at all, when thirst is not full |
| `depletesThirst` | `true` | Regenerating health makes thirst go down |
| `climateDependent` | `true` | That loss is scaled by the climate factor |
| `fullRegenMinThirst` | `20` | With `haltedWhenThirsty`: thirst needed for fast (saturation) regeneration |
| `slowRegenMinThirst` / `slowRegenIntervalTicks` | `19` / `8` | Below that, saturation still heals every 8 ticks with at least 19 thirst |
| `hungerRegenMinThirst` | `19` | Thirst needed for normal (food level) regeneration |
| `peacefulRegenAmount` / `peacefulRegenIntervalTicks` | `1` / `11` | Thirst restored in Peaceful (unless `depletion.inPeaceful`) |

### `[damage]`

| Key | Default | Meaning |
|---|---|---|
| `amount` / `intervalTicks` | `1.0` / `40` | Damage with no thirst left, and how often |
| `minHealthEasy` / `minHealthNormal` / `minHealthHard` | `10` / `1` / `0` | Dehydration only hurts above this health (Peaceful uses Easy) |
| `canKill` | `true` | `false`: a hit that would kill is skipped |

### `[sprint]`, `[drinking]`, `[hand]`, `[death]`, `[loot]`

| Key | Default | Meaning |
|---|---|---|
| `sprint.blockedWhenThirsty` | `true` | No sprinting with `minThirst` or less. A sprint in progress stops too |
| `sprint.minThirst` | `6` | 3 droplets |
| `drinking.extraThirstToQuenched` | `true` | Thirst restored above full turns into quenched |
| `drinking.waterBottleStackSize` | `64` | Stack size of water bottles (1-99) |
| `drinking.canFillFromFlowingWater` | `true` | Glass bottles and terracotta bowls fill from flowing water. `false` = source blocks only, as in vanilla. Buckets always need a source |
| `drinking.rain` | `true` | Drink rain by looking up |
| `drinking.rainMaxPitch` | `-80` | How far up to look (-90 = straight up) |
| `drinking.rainIntervalTicks` | `11` | Ticks between two sips |
| `drinking.rainThirst` / `rainQuenched` | `1` / `1` | Restored per sip |
| `hand.enabled` | `true` | Drink water in the world by sneaking and right-clicking with an empty hand |
| `hand.bothHandsEmpty` | `true` | Both hands must be empty |
| `hand.thirst` / `hand.quenched` | `1` / `1` | Restored per sip |
| `hand.cooldownTicks` | `10` | Minimum ticks between two sips |
| `hand.effects` | `true` | Arm swing and splash particles when drinking by hand (visual only) |
| `death.respawnThirst` / `respawnQuenched` | `20` / `5` | Values after respawning. `-1` keeps what the player died with |
| `loot.enabled` | `true` | Drinks are added to vanilla chest loot. It is a load condition of the loot data, so a change applies after `/reload` or on the next world load |

The maximum thirst stays 20: the HUD, its overlays and the commands assume it.

### `[effects]`

| Key | Default | Meaning |
|---|---|---|
| `dehydrationMultiplier` | `1.0` | Multiplier (0-10) of the exhaustion the Dehydration effect adds every tick (0.005 per level) |
| `quenchnessIntervalTicks` | `40` | Quenchness restores (level) thirst and (level) quenched every this many ticks (1-1200) |
| `quenchnessPotion` | `true` | Brewing recipes of the Potion of Quenchness. It is the load condition `blue_droplets:quenchness_potion` of those recipes: a change applies when datapacks load (a restart or `/reload`), and only the server's value counts |
| `hydratedMultiplier` | `0.5` | Thirst loss with Hydrated, applied once per level (0.5: Hydrated I halves it, II quarters it; 0-1) |
| `waterBreathingReducesThirst` | `false` | While fully underwater with Water Breathing or Conduit Power, thirst loss is also multiplied by `underwaterBreathingMultiplier` |
| `underwaterBreathingMultiplier` | `0.5` | See above (0-10), on top of `climate.underwater` |

### `[hydration]`

| Key | Default | Meaning |
|---|---|---|
| `fullBonus` | `false` | Staying fully hydrated gives Hydrated I: thirst 20 and at least `minQuenched` for `fullBonusSeconds`. Refreshed while it holds, checked once a second |
| `minQuenched` | `10` | Quenched needed (0-20) |
| `fullBonusSeconds` | `30` | Seconds fully hydrated before the effect is given (1-3600) |
| `fullBonusDurationTicks` | `200` | Duration of the effect, so it lasts this long after the player stops being fully hydrated (40-12000) |

### `[overhydration]`

Thirst and quenched drunk past full (items, hand drinking, rain, `DropletsAPI.drink`, but not the Quenchness effect) add up as overflow. At the threshold the player gets `blue_droplets:overhydrated` (10% slower movement per level) and the overflow starts over.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | `false` turns the mechanic off |
| `threshold` | `20` | Overflow that causes Overhydrated (1-1000). A water bottle drunk while full adds 9 |
| `decayPerSecond` | `1.0` | Overflow lost per second (0-100) |
| `durationTicks` | `400` | Duration of Overhydrated (20-12000) |
| `nausea` | `true` | Also 5 seconds of Nausea when it is applied |

The level is I, plus one per half threshold past it, or one more than an Overhydrated already active, up to III. `OverhydrationEvent` can cancel it or change duration and level.

---

## `purity.toml`

Every purity value in the config files is one of these six levels:

| Level | Name | Default effects when drunk |
|---|---|---|
| 0 | Contaminated | Nausea and Hunger, 40% Poison (blocks hydration) |
| 1 | Dirty | 60% Nausea and Hunger, 15% Poison (blocks hydration) |
| 2 | Murky | 25% Nausea and Hunger |
| 3 | Acceptable | 5% Nausea and Hunger |
| 4 | Clean | Nothing |
| 5 | Pure | Nothing, and the `pureWater` bonus |

| Key | Default | Meaning |
|---|---|---|
| `general.enabled` | `true` | Water has a purity at all. `false` leaves only thirst: see [Thirst only](#thirst-only-purity-off) |
| `general.defaultPurity` | `3` | Purity of water with none stored (0-5) |
| `general.quenchWhenDebuffed` | `true` | Drinking still restores thirst when a purity effect blocks hydration |
| `pureWater.thirstBonus` | `2` | Thirst added when drinking pure water (5) from a container: a bottle then gives 6 instead of 4. Bottles, buckets, bowls, the Traveler's Backpack hose and other purity containers count. A sip by hand does not, and neither do other drinks with a purity |
| `pureWater.quenchedBonus` | `3` | Quenched added the same way: a bottle then gives 8 instead of 5 |
| `world.*` | | See [Water purity in the world](Modpack-Makers#water-purity-in-the-world) |

### Poured water

Water poured into the world keeps its purity, so sea water poured into a meadow is not clean when you fill a bucket there again. There is no key for it: it follows `general.enabled`.

- A water source left by a bucket (a player's or a dispenser's) or by NeoForge's `FluidUtil.tryPlaceFluid` (the fluid containers of many mods) is remembered with the purity of the water poured. Water without a stored purity counts as `defaultPurity`.
- Anything that takes or drinks water there reads that purity: bottles, bowls, buckets, drinking by hand, dispensers, the Traveler's Backpack hose.
- The infinite source that forms between two poured sources takes the worst purity beside it. Picking up a poured source hands its purity to the water sources beside it, so the one that refills its place is not clean either.
- A remembered position that no longer holds a water source is ignored, and forgotten when read. Water placed in other ways (commands, mods that place the block directly) has the world's purity, unless it is next to poured water.
- It is stored per chunk with the world and never sent to clients. `/blue_droplets debug purity` shows "poured water" when it applies.

### Thirst only (purity off)

For the thirst bar alone, set `general.enabled = false`. The server's value is sent to clients, so players do not change their own file. With it off:

- No item or fluid gets a purity. Buckets, glass bottles and terracotta bowls filled from the world, a cauldron or a dispenser hold plain water, and chest loot drinks come without one.
- Nothing shows a purity: tooltips, Jade, the water tint. The creative tab lists each water container once instead of once per purity.
- Drinking any water restores thirst and never gives purity effects, including the Dehydration of `hotDirtyWater`. `DrinkEvent` reports `NO_PURITY`.
- The purification recipes are not loaded, and the JEI "Water Purification" page is hidden. Recipes are checked when datapacks load, so this part applies after `/reload` or a restart.
- The terracotta bowls stay, as plain water containers, because registered items cannot depend on a config.
- `/blue_droplets debug purity` only says that purity is off. For other mods, `DropletsAPI.isPurityEnabled()` returns `false` and `withPurity` returns an unchanged copy.
- The other keys of this file do nothing.
- Purity already stored on items from before is ignored and not shown. Such items may not stack with new water until they are used up.

### `[effects]`

One list per purity: `contaminated`, `dirty`, `murky`, `acceptable`, `clean`, `pure`. Each entry is `"effect_id,durationTicks,amplifier,chancePercent[,blocksHydration]"`.

| Key | Default |
|---|---|
| `contaminated` | `["minecraft:nausea,100,0,100", "minecraft:hunger,600,0,100", "minecraft:poison,200,0,40,true"]` |
| `dirty` | `["minecraft:nausea,100,0,60", "minecraft:hunger,600,0,60", "minecraft:poison,200,0,15,true"]` |
| `murky` | `["minecraft:nausea,100,0,25", "minecraft:hunger,600,0,25"]` |
| `acceptable` | `["minecraft:nausea,100,0,5", "minecraft:hunger,600,0,5"]` |
| `clean` | `[]` |
| `pure` | `[]` |

- Any mob effect id works, also from other mods, including `blue_droplets:dehydration` and `blue_droplets:hydrated` (for example `"blue_droplets:hydrated,600,0,100"` in `pure`). An unknown id is skipped and listed by `/blue_droplets config check`.
- One roll per drink is shared by the whole list: an entry applies when the roll is below its chance. So a 40% entry always comes together with the 100% ones.
- `blocksHydration` (`true`/`false`, default `false`): when that entry applies, the drink restores no thirst, unless `general.quenchWhenDebuffed` is `true` (the default).
- Old configs: the eight `*Percentage` values of Thirst Was Taken are turned into these lists once, with the old effects and durations. Its four levels go to `contaminated`, `dirty`, `acceptable` and `pure`, and `murky` and `clean` keep their defaults.

### `[hotDirtyWater]`

Drinking water of low purity (a container or by hand) in a hot climate also gives `blue_droplets:dehydration`, on top of the `[effects]` list. It is decided when drinking.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Turns the mechanic off |
| `maxPurity` | `2` | Highest purity that counts (0-5; 2 is murky) |
| `durationTicks` | `600` | Duration of the effect |
| `amplifier` | `0` | Amplifier (0 is level I) |
| `minBiomeTemperature` | `1.0` | Biome base temperature from which the climate is hot (desert, savanna, badlands 2.0; jungle 0.95) |

A place where water evaporates (the Nether) always counts as hot.

---

## `compat.toml`

### `[sereneseasons]`

Only read with Serene Seasons installed.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | The biome climate formula reads the biome temperature as Serene Seasons changes it with the season (its `biome_temp_adjustment` per sub-season, in Serene Seasons' `seasons.toml`). Biomes without seasons (`sereneseasons:blacklisted_biomes`), the Nether and a dimension's own thirst multiplier are left alone |
| `tropicalDrySeasonMultiplier` | `1.1` | Multiplies thirst loss in a tropical biome's dry season. The temperature is not changed: tropical biomes are hot all year |
| `tropicalWetSeasonMultiplier` | `1.0` | The same for the wet season |
| `springMultiplier`, `summerMultiplier`, `autumnMultiplier`, `winterMultiplier` | `1.0` | Multiply thirst loss in that season, in biomes with the four seasons. With Serene Seasons' default config summer does not warm biomes, so `summerMultiplier` is the simple way to make summer thirstier |

---

## `items.toml`

| Key | Default | Meaning |
|---|---|---|
| `overrides.drinks` / `overrides.foods` | `[]` | `[["namespace:item" or "#tag", thirst, quenched], ...]`. They win over datapacks and other mods. Thirst -20 to 20, quenched -20 or more. Negative values remove them (salty) |
| `overrides.blacklist` | `[]` | Items that never restore thirst |
| `containers.containers` | `[]` | Drinks that carry a water purity, added to the item tag `blue_droplets:purity_containers` |
| `salty.thirstPenalty` / `salty.quenchedPenalty` | `-2` / `-2` | Values (-20 to 20) of items in the item tag `blue_droplets:salty` that get none from overrides, datapacks or other mods |
| `keywords.enabled` | `false` | Give values to items whose translation key matches a pattern |
| `keywords.drinkThirst` / `drinkQuenched` | `10` / `14` | Values of items matching `keywords.drink` |
| `keywords.soupThirst` / `soupQuenched` | `4` / `5` | Values of items matching `keywords.soup` |
| `keywords.fruitThirst` / `fruitQuenched` | `2` / `3` | Values of items matching `keywords.fruit` |
| `keywords.blacklist`, `drink`, `soup`, `fruit` | regular expressions | Items matching `blacklist` are never picked. The others list words such as `juice`, `tea`, `stew`, `berry` |
| `inference.enabled` | `false` | Estimate values from recipe ingredients (see below) |
| `inference.onlyConsumables` | `true` | Only items that are eaten or drunk get an estimate. Others still pass their value on |
| `inference.maxDepth` | `4` | Recipe steps followed down from an item (1-16) |
| `inference.craftingMultiplier` / `cookingMultiplier` / `otherMultiplier` | `1.0` | Multiplier of crafting table recipes, of furnace, blast furnace, smoker and campfire recipes, and of every other recipe type |
| `inference.maxThirst` / `maxQuenched` | `8` / `10` | Caps of an estimate |
| `inference.minThirst` | `1` | Estimates below this thirst are dropped |
| `inference.blacklist` | `[]` | `"namespace:item"`, `"#namespace:tag"` or `"@namespace"`: never estimated, and worth 0 as an ingredient |
| `inference.ignoredRecipeTypes` | `["minecraft:stonecutting", "minecraft:smithing"]` | Recipe types that are not used |

### Recipe inference

Off by default. When on, the server gives values to items that have none from any other source (blacklist, overrides, datapacks, other mods' code and keywords always win), by looking at the recipes that make them:

- An ingredient is worth the average thirst and quenched of the items it accepts that have values, explicit or estimated. Ingredients worth nothing (a bowl, a glass bottle) add nothing, and salty ones count as 0.
- A recipe is worth the sum of its ingredients times the multiplier of its category, divided by how many items it makes, capped by `maxThirst`/`maxQuenched`. When several recipes make the item, the highest one wins.
- Any recipe type works, also other mods' machines, as long as the recipe lists its item ingredients and result the standard way. Fluids used by recipes are not seen. Special recipes (suspicious stew, fireworks) are skipped.
- Loops (ingot to block to ingot) are cut, and so is anything deeper than `maxDepth`.
- Items that are drunk go to the drink table and the rest to the food table. Tooltips show "(est.)". The values are sent to clients like the rest of the table.

It runs when the world loads, after `/reload` and when `items.toml` changes, never while playing. The log shows one line with how many items were estimated and how long it took. Recipes that could not be read are listed in the config warning and in `/blue_droplets config check`.

---

## `client.toml`

| Key | Default | Meaning |
|---|---|---|
| `Purity tooltip.onlyShowPurityWhenShifting` | `false` | Shows the purity line of water containers only while Shift is held |
| `Purity tooltip.tintWaterByPurity` | `true` | Tints the water in bottles and terracotta bowls by its purity, from brown (0) to near white (5). Off draws them as in vanilla. Nothing is tinted while purity is off |
| `Tooltip.showTooltipIcons` | `true` | Thirst and quenched of items as icons in the tooltip, `(est.)` for estimated values. Off, estimated values are shown as a text line |
| `Tooltip.followAppleSkin` | `true` | With AppleSkin installed, the quenched outline, drink preview, exhaustion underlay and tooltip icons are also hidden when their food counterpart is off in AppleSkin's config. The tooltip icons then also follow its "hold Shift" option. Nothing is drawn through AppleSkin |
| `Thirst Bar.thirstBarXOffset` / `thirstBarYOffset` | `0` | Moves the thirst bar, in pixels |
| `Thirst Bar.hideBarWhenFull` | `false` | Hides the thirst bar while thirst is 20 and the player holds nothing that restores thirst. The bars above it move down |
| `Thirst Bar.hideBarDelayTicks` | `60` | Ticks the bar stays visible after thirst becomes full (0-1200) |
| `Thirst Bar.showQuenchedOverlay` | `true` | Outline on the droplets for the current quenched, like AppleSkin's saturation outline |
| `Thirst Bar.showDrinkPreview` | `true` | While holding something that restores thirst (either hand), flashes the thirst and quenched it would give |
| `Thirst Bar.showExhaustionUnderlay` | `false` | Bar under the droplets that fills with thirst exhaustion until the next point is lost |
| `Thirst Bar.buffWave` | `true` | Droplets bounce one at a time, like hearts under Regeneration, while Quenchness or Hydrated is active |
| `Bar Colors.dehydration` | `#8B5A2B` | Droplet colour while Dehydration is active |
| `Bar Colors.overhydrated` | `#9DB0C0` | Droplet colour while Overhydrated is active |
| `Bar Colors.poison` | `#7DAA3C` | Droplet colour while Poison is active |
| `Bar Colors.quenchness` | `#5FE3FF` | Droplet colour while Quenchness is active |
| `Bar Colors.hydrated` | `#7FE0C0` | Droplet colour while Hydrated is active |

Colours are `#RRGGBB`. An invalid value is reset to its default with a warning in the log. When several apply, the first in the list above wins.

<!-- SCREENSHOT: thirst bar with showExhaustionUnderlay on, next to the hunger bar with AppleSkin's underlay -->
