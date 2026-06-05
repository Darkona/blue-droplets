# Configuration

All TOML files live in `config/bluedroplets/`. They are NeoForge configs: missing keys are added with their default and a comment, values out of range are reset to the default, invalid list entries are removed, and edits are picked up while the game runs. When a server's file changes, the server rebuilds the drink tables, recomputes every player's thirst loss and sends the new values to all players; no `/reload` or relog needed.

Per-id data (items, biomes, dimensions) lives in datapacks instead; see [Modpack makers](Modpack-Makers.md).

## Checking a config

Problems that single keys cannot show (unknown effect or item ids, overlapping altitude bands, curve points out of order, invalid keyword patterns, `slowRegenMinThirst` above `fullRegenMinThirst`) are logged as **one warning** each time the world loads, after `/reload` and after a config file changes. Bad entries are skipped; nothing crashes.

Commands (operators, permission level 2):

| Command | Shows |
|---|---|
| `/bluedroplets config check` | The same list of problems, or "no problems found" |
| `/bluedroplets debug exhaustion [player]` | Mode, every factor of the thirst loss multiplier (and where the climate factor comes from), the `thirst_drain` attribute, the total, exhaustion, thirst and quenched |
| `/bluedroplets debug purity` | Purity of the water you look at (or the block at your feet): salt water rule, base and where it comes from, altitude, still/running and biome deltas, cap |
| `/bluedroplets infer <item>` | How recipe inference would estimate the item: every recipe that makes it, each ingredient's value, multiplier, result count, the estimate and why it would not be used |

## Files

| File | Type | Content |
|---|---|---|
| `gameplay.toml` | common | Thirst loss, drinking, damage, regeneration, sprint, loot |
| `purity.toml` | common | Water purity in the world and its effects |
| `items.toml` | common | Per-item overrides, purity containers, keywords |
| `compat.toml` | common | One section per optional mod (`[create]`, …) |
| `client.toml` | client | HUD and tooltips; each player's own |

The gameplay files are **common** configs, not per-world server configs: they exist before a world is loaded (tooltips and stack sizes need them) and one file serves every world. On a dedicated server the server's values win: the ones the client needs (sprint rule, hand drinking and its two-hands rule, drink tables, purity containers, `defaultPurity`, water bottle stack size) are sent to each player when joining, after `/reload` and after a config change, and dropped when leaving.

### Moving from older versions

On first start, if a new file does not exist yet, its values are copied from the old files (`common.toml`, `item_settings.toml`, `container.toml`, `keyword.toml`; also those copied from Thirst Was Taken's `config/thirst/`). The old files are then renamed to `*.toml.old` and no longer read; a warning in the log lists what was moved. Modpacks that ship `defaultconfigs/` must use the new file names and keys.

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
| `Purity-related Effects.*Percentage` (8 values) | `purity.toml` `effects.dirty`, `slightlyDirty`, `acceptable`, `purified` (lists, see below) |
| `Create compatibility.*` | `compat.toml` `create.*` |
| `item_settings.toml` `drinks`, `foods`, `itemsBlacklist` | `items.toml` `overrides.drinks`, `overrides.foods`, `overrides.blacklist` |
| `container.toml` `Containers` | `items.toml` `containers.containers` |
| `keyword.toml` (all keys) | `items.toml` `keywords.*` (`enabled`, `drinkThirst`, `drinkQuenched`, `soupThirst`, …, `blacklist`, `drink`, `soup`, `fruit`) |

## `gameplay.toml`

### `[depletion]`

Thirst loss per tick is `activity × scale × M`, where `M = climate × fire protection × fire resistance × rain/thunder × day/night × sun × altitude × water × Hydrated × bluedroplets:thirst_drain`. Every 4.0 of exhaustion removes one quenched point, or one thirst point when quenched is 0. `M` (except the attribute) is recomputed every second per player and right away after armor, effect, dimension or config changes; `/bluedroplets debug exhaustion` shows each factor.

| Key | Default | Meaning |
|---|---|---|
| `mode` | `MIRROR_FOOD` | Where activity comes from. `MIRROR_FOOD`: the exhaustion vanilla adds to hunger (as before; includes other mods that exhaust hunger). `OWN`: Blue Droplets counts the activities of `[depletion.activity]` itself, with vanilla's numbers by default, without reading hunger |
| `basalPerTick` | `0.0` | Exhaustion added every tick even when idle |
| `exhaustionPerPoint` | `4.0` | Exhaustion that removes one point |
| `nauseaPerTick` | `0.06` | Exhaustion per tick while nauseous |
| `multiplier` | `1.2` | How much faster thirst goes down than hunger (inside the climate multiplier) |
| `inPeaceful` | `false` | Thirst goes down in Peaceful |
| `netherMultiplier` | `3.0` | Replaces the climate multiplier in ultra-warm dimensions without their own `thirst_multiplier` |
| `fireResistancePercent` | `0` | Thirst loss with Fire Resistance, in percent |
| `nauseaDepletes` | `true` | Nausea makes thirst go down |
| `fireProtectionPerLevel` / `fireProtectionMaxLevels` | `0.046875` / `12` | Thirst loss removed per level of Fire Protection on armor, and the most levels counted |

`MIRROR_FOOD` stays the default: it also counts exhaustion from other mods and from vanilla actions `OWN` does not see, so switching would change the balance of existing packs.

### `[depletion.climate]`

| Key | Default | Meaning |
|---|---|---|
| `formula` | `LEGACY` | `LEGACY`: `multiplier × temperature / humidity` of the biome, softened below 1 (as before). `CURVE`: `multiplier × temperatureCurve(temperature) × humidityCurve(downfall)`. With Cold Sweat (`compat.toml` `coldsweat.useBodyTemperature`) the body temperature / 100 is used as temperature |
| `legacyHarshness` | `0.5` | LEGACY: part of a multiplier below 1 that is kept |
| `temperatureCurve` | `["-0.5,0.7", "0.8,1.0", "2.0,1.5"]` | CURVE: `"x,multiplier"` points in ascending x, straight lines between them |
| `humidityCurve` | `["0.0,1.2", "0.4,1.0", "1.0,0.8"]` | CURVE: same for the biome's downfall |
| `rain` / `thunder` | `1.0` / `1.0` | When rain falls on the player (thunder replaces rain in a storm) |
| `day` / `night` | `1.0` / `1.0` | In dimensions with a day cycle |
| `sun` | `1.0` | Day, not raining, sky visible |
| `inWater` / `underwater` | `1.0` / `1.0` | In water with the head out / fully underwater |
| `altitude` | `[]` | `"minY,maxY,multiplier"` from sea level; the first band containing the player applies |

A dimension type can replace the climate multiplier with `thirst_multiplier` in the `bluedroplets:dimension_water` data map (see [Modpack makers](Modpack-Makers.md)).

### `[depletion.activity]`

| Key | Default | Meaning |
|---|---|---|
| `sprintPerMeter` | `0.1` | OWN: per meter sprinted on the ground |
| `swimPerMeter` | `0.01` | OWN: per meter swum or walked in water |
| `jump` / `sprintJump` | `0.05` / `0.2` | OWN: per jump |
| `attack` | `0.1` | OWN: per attack |
| `blockBreak` | `0.005` | OWN: per block broken |
| `damageMultiplier` | `1.0` | OWN: times the damage type's exhaustion |
| `healPerHealth` | `6.0` | OWN: per health point regenerated from food |
| `ridingMultiplier` | `0.0` | Both modes: activity while riding (0 = none, as before) |
| `sleepingMultiplier` | `1.0` | Both modes: activity while sleeping |

### `[regeneration]`

| Key | Default | Meaning |
|---|---|---|
| `haltedWhenThirsty` | `true` | Health regenerates slower or not at all when thirst is not full |
| `depletesThirst` | `true` | Regenerating health makes thirst go down |
| `climateDependent` | `true` | That loss is scaled by the climate multiplier |
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

### `[effects]`

| Key | Default | Meaning |
|---|---|---|
| `dehydrationMultiplier` | `1.0` | Multiplier (0-10) of the thirst exhaustion the `bluedroplets:dehydration` effect adds every tick (0.005 per level). Thirst never goes below zero |
| `quenchnessIntervalTicks` | `40` | Quenchness restores (level) thirst and (level) quenched every this many ticks (1-1200) |
| `quenchnessPotion` | `true` | Brewing recipes of the Quenchness potions. Not synced: keep the same value on the server and the clients, or the brewing stand may not accept prismarine crystals client side |
| `waterBreathingReducesThirst` | `false` | While fully underwater with Water Breathing or Conduit Power, thirst loss is also multiplied by `underwaterBreathingMultiplier` (part of the `water` factor) |
| `underwaterBreathingMultiplier` | `0.5` | See above (0-10), on top of `climate.underwater` |
| `hydratedMultiplier` | `0.5` | Thirst loss with the `bluedroplets:hydrated` effect, applied once per level (0.5: Hydrated I halves it, II quarters it; 0-1) |

### `[hydration]`

| Key | Default | Meaning |
|---|---|---|
| `fullBonus` | `false` | Staying fully hydrated gives Hydrated I (thirst#186): thirst 20 and at least `minQuenched` for `fullBonusSeconds`; refreshed while it holds, checked once per second |
| `minQuenched` | `10` | Quenched needed (0-20) |
| `fullBonusSeconds` | `30` | Seconds fully hydrated before the effect is given (1-3600) |
| `fullBonusDurationTicks` | `200` | Duration of the Hydrated effect it gives, so it lasts this long after the player stops being fully hydrated (40-12000) |

### `[overhydration]`

Thirst and quenched drunk past full (items, hand drinking, rain, `DropletsAPI.drink`; not the Quenchness effect) add up as overflow. At the threshold the player gets `bluedroplets:overhydrated` (harmful, 10% slower movement per level) and the overflow starts over. On by default.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | `false` turns the mechanic off (overflow is no longer counted) |
| `threshold` | `20` | Overflow that causes Overhydrated (1-1000). A water bottle (6/8) drunk while full adds 14 |
| `decayPerSecond` | `1.0` | Overflow lost per second (0-100), applied once a second |
| `durationTicks` | `400` | Duration of Overhydrated (20-12000) |
| `nausea` | `true` | Also 5 seconds of Nausea when it is applied |

The level is I, plus one per half threshold past it, or one more than the active Overhydrated; at most III. `OverhydrationEvent` can cancel it or change duration and level.

### `[sprint]`, `[drinking]`, `[hand]`, `[death]`, `[loot]`

| Key | Default | Meaning |
|---|---|---|
| `sprint.blockedWhenThirsty` | `true` | No sprinting with `minThirst` or less |
| `sprint.minThirst` | `6` | 3 droplets (sent to clients) |
| `drinking.extraThirstToQuenched` | `true` | Thirst restored above full turns into quenched |
| `drinking.waterBottleStackSize` | `64` | Stack size of water bottles |
| `drinking.canFillFromFlowingWater` | `true` | Glass bottles and terracotta bowls can be filled from flowing water; `false` = source blocks only, as in vanilla. Buckets always need a source (sent to clients) |
| `drinking.rain` | `true` | Drink rain by looking up |
| `drinking.rainMaxPitch` | `-80` | How far up to look (-90 = straight up) |
| `drinking.rainIntervalTicks` | `11` | Ticks between two sips |
| `drinking.rainThirst` / `rainQuenched` | `1` / `1` | Restored per sip |
| `hand.enabled` | `false` | Drink water in the world by sneaking and right-clicking with an empty hand |
| `hand.bothHandsEmpty` | `true` | Both hands must be empty |
| `hand.thirst` / `hand.quenched` | `3` / `2` | Restored per sip |
| `hand.cooldownTicks` | `10` | Minimum ticks between two sips |
| `hand.effects` | `true` | Swing the arm and splash particles when drinking by hand (visual only) |
| `death.respawnThirst` / `respawnQuenched` | `20` / `5` | Values after respawning; `-1` keeps what the player died with |
| `loot.enabled` | `true` | Drinks are added to vanilla chest loot |

The maximum thirst stays 20: the HUD, its overlays and commands assume it.

## `purity.toml`

| Key | Default | Meaning |
|---|---|---|
| `general.enabled` | `true` | Water has a purity at all. `false`: nothing stores or shows purity (tooltips, Jade, Create goggles), no purity effects, and the purification recipes are not loaded (they are checked when datapacks load: `/reload` or restart) |
| `general.defaultPurity` | `2` | Purity of water with none stored |
| `general.quenchWhenDebuffed` | `true` | Drinking still restores thirst when a purity effect blocks hydration |
| `world.*` | | See [Water purity in the world](Modpack-Makers.md#water-purity-in-the-world) |
| `cauldron.boiling` | `false` | A water cauldron standing on a heat source (block tag `#bluedroplets:cauldron_heat_sources`: lit campfires, fire, magma, lava) slowly gains purity |
| `cauldron.boilingMaxPurity` | `3` | Purity boiling stops at (0-3) |
| `cauldron.boilingChance` | `0.25` | Chance of +1 purity on each random tick of the cauldron (0-1). At `randomTickSpeed` 3 a block gets a random tick about every 68 s, so 0.25 is about +1 purity every 4.5 minutes; raising `randomTickSpeed` speeds it up too |

### `[effects]`

One list per purity: `dirty`, `slightlyDirty`, `acceptable`, `purified`. Each entry is `"effect_id,durationTicks,amplifier,chancePercent[,blocksHydration]"`.

| Key | Default |
|---|---|
| `dirty` | `["minecraft:nausea,100,0,100", "minecraft:hunger,600,0,100", "minecraft:poison,200,0,30,true"]` |
| `slightlyDirty` | `["minecraft:nausea,100,0,50", "minecraft:hunger,600,0,50", "minecraft:poison,200,0,10,true"]` |
| `acceptable` | `["minecraft:nausea,100,0,5", "minecraft:hunger,600,0,5"]` |
| `purified` | `[]` |

- Any mob effect id works, also from other mods, including `bluedroplets:dehydration` (for example `"bluedroplets:dehydration,600,0,20"`). An unknown id is skipped (listed by `/bluedroplets config check`).
- **One roll per drink** is shared by the whole list: an entry applies when the roll is below its chance. With the defaults, poisoned water always also gives nausea and hunger, as before.
- `blocksHydration` (`true`/`false`, default `false`): when that entry applies, the drink restores no thirst, unless `general.quenchWhenDebuffed` is `true` (the default).
- Old configs: the eight `*Percentage` values are turned into these lists once, with the old effects and durations (nausea 5 s and hunger 30 s share the nausea chance; poison 10 s blocks hydration).

### `[hotDirtyWater]`

Drinking water of low purity (bottle or by hand) in a hot climate also gives `bluedroplets:dehydration`, on top of the `[effects]` list. It is decided at drink time only.

| Key | Default | Meaning |
|---|---|---|
| `enabled` | `true` | Turn the mechanic off |
| `maxPurity` | `0` | Highest purity that counts (0 dirty ... 3 purified) |
| `durationTicks` | `600` | Duration of the effect |
| `amplifier` | `0` | Amplifier (0 is level I) |
| `minBiomeTemperature` | `1.0` | Biome base temperature from which the climate is hot (desert, savanna, badlands 2.0; jungle 0.95) |
| `useColdSweat` | `true` | With Cold Sweat installed, its body temperature also counts |
| `coldSweatMinBodyTemp` | `50.0` | Cold Sweat body temperature (its units: 0 neutral, 100 burning, -100 freezing) above which the player is hot |

An ultra-warm dimension (the Nether) always counts as hot.

## `compat.toml`

| Key | Default | Meaning |
|---|---|---|
| `create.sandFilterFiltrationAmount` | `1` | Purity levels gained in a Sand Filter |
| `create.sandFilterMbPerTick` | `10` | Millibuckets filtered per tick |
| `create.sandFilterMaxPurity` | `3` | Highest purity a Sand Filter raises water to (0-3); purer water passes unchanged. Filters in a row facing the same way pass water on, one step each |
| `create.openEndedPipePurity` | `true` | Water that an open pipe end pulls from the world or from a water cauldron keeps its purity there (as buckets and the hose pulley); `false` = it reads as `defaultPurity` |
| `coldsweat.useBodyTemperature` | `true` | The climate multiplier uses Cold Sweat's body temperature instead of the biome's |

## `items.toml`

| Key | Default | Meaning |
|---|---|---|
| `overrides.drinks` / `overrides.foods` | `[]` | `["namespace:item" or "#tag", thirst, quenched]`; win over datapacks. Thirst -20 to 20, quenched -20 or more; negative values remove them (salty) |
| `overrides.blacklist` | `[]` | Items that never restore thirst |
| `containers.containers` | `[]` | Drinks that carry a water purity |
| `salty.thirstPenalty` / `salty.quenchedPenalty` | `-2` / `-2` | Values (-20 to 20) of items in the item tag `bluedroplets:salty` that have none from overrides, datapacks or other mods |
| `keywords.enabled` | `false` | Give values to items by name patterns |
| `inference.enabled` | `false` | Estimate values from recipe ingredients (see below) |
| `inference.onlyConsumables` | `true` | Only items that are eaten or drunk get an estimate; others still pass their value on |
| `inference.maxDepth` | `4` | Recipe steps followed down from an item (1-16) |
| `inference.craftingMultiplier` / `cookingMultiplier` / `otherMultiplier` | `1.0` | Multiplier of crafting table recipes, of furnace/blast furnace/smoker/campfire recipes, and of every other recipe type |
| `inference.maxThirst` / `maxQuenched` | `8` / `10` | Caps of an estimate |
| `inference.minThirst` | `1` | Estimates below this thirst are dropped |
| `inference.blacklist` | `[]` | `"namespace:item"`, `"#namespace:tag"` or `"@namespace"`: never estimated and worth 0 as an ingredient |
| `inference.ignoredRecipeTypes` | `["minecraft:stonecutting", "minecraft:smithing"]` | Recipe types that are not used |

### Recipe inference

Off by default. When on, the server gives values to items that have none from any other source (blacklist, overrides, datapacks, other mods' code, keywords always win) by looking at the recipes that make them:

- An ingredient is worth the average thirst and quenched of the items it accepts that have values (explicit or estimated themselves). Ingredients worth nothing (a bowl, a glass bottle) add nothing.
- A recipe is worth the sum of its ingredients times the multiplier of its category, divided by how many items it makes, capped by `maxThirst`/`maxQuenched`. When several recipes make the item, the highest one wins.
- Any recipe type works, also other mods' machines, as long as the recipe lists its item ingredients and result the standard way. Fluids used by recipes (Create mixing, pots with water) are not seen. Special recipes (suspicious stew, fireworks) are skipped.
- Loops (ingot to block to ingot) are cut, and so is anything deeper than `maxDepth`.
- Items that are drunk go to the drink table and the rest to the food table; tooltips show "Thirst N, quenched M (est.)". The values are sent to clients like the rest of the table.

It runs when the world loads, after `/reload` and when `items.toml` changes, never while playing. The log shows one line with how many items were estimated and how long it took. Recipes that could not be read are listed in the config warning and in `/bluedroplets config check`.

## `client.toml`

| Key | Default | Meaning |
|---|---|---|
| `Purity tooltip.onlyShowPurityWhenShifting` | `false` | Shows the purity line of water containers only while Shift is held |
| `Tooltip.showTooltipIcons` | `true` | Thirst and quenched of items as icons in the tooltip (`(est.)` for estimated values); when off, estimated values are shown as a text line |
| `Thirst Bar.thirstBarXOffset` / `thirstBarYOffset` | `0` | Moves the thirst bar |
| `Thirst Bar.hideBarWhenFull` | `false` | Hides the thirst bar while thirst is 20 and the player holds nothing that restores thirst (main or off hand); the bars above it move down and the quenched outline, preview and underlay are hidden too |
| `Thirst Bar.hideBarDelayTicks` | `60` | Ticks the bar stays visible after thirst becomes full (0-1200) |
| `Thirst Bar.showQuenchedOverlay` | `true` | Outline on the droplets for the current quenched (like AppleSkin's saturation outline) |
| `Thirst Bar.showDrinkPreview` | `true` | While holding something that restores thirst (main or off hand), flashes the thirst and quenched it would give |
| `Thirst Bar.showExhaustionUnderlay` | `false` | Bar under the droplets that fills with thirst exhaustion until the next point is lost |
| `Thirst Bar.buffWave` | `true` | Droplets bounce one at a time, like hearts under Regeneration, while a positive thirst effect (Quenchness, Hydrated) is active |
| `Bar Colors.vampire` | `#B3121B` | Droplet colour for vampires (Vampirism, Supernatural) |
| `Bar Colors.dehydration` | `#8B5A2B` | Droplet colour while Dehydration is active |
| `Bar Colors.overhydrated` | `#9DB0C0` | Droplet colour while Overhydrated is active |
| `Bar Colors.poison` | `#7DAA3C` | Droplet colour while Poison is active |
| `Bar Colors.quenchness` | `#5FE3FF` | Droplet colour while Quenchness is active |
| `Bar Colors.hydrated` | `#7FE0C0` | Droplet colour while Hydrated is active |

Colours are `#RRGGBB`; an invalid value is reset to its default with a warning in the log. When several apply, the order is vampire, Dehydration, Poison, Quenchness, Hydrated.
