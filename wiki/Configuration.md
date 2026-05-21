# Configuration

All TOML files live in `config/bluedroplets/`. They are NeoForge configs: missing keys are added with their default and a comment, values out of range are reset to the default, invalid list entries are removed, and edits are picked up while the game runs. When a server's file changes, the server rebuilds the drink tables, recomputes every player's thirst loss and sends the new values to all players; no `/reload` or relog needed.

Per-id data (items, biomes, dimensions) lives in datapacks instead; see [Modpack makers](Modpack-Makers.md).

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

| Key | Default | Meaning |
|---|---|---|
| `multiplier` | `1.2` | How much faster thirst goes down than hunger |
| `inPeaceful` | `false` | Thirst goes down in Peaceful |
| `netherMultiplier` | `3.0` | Replaces the climate multiplier in ultra-warm dimensions |
| `fireResistancePercent` | `0` | Thirst loss with Fire Resistance, in percent |
| `nauseaDepletes` | `true` | Nausea makes thirst go down |

### `[regeneration]`

| Key | Default | Meaning |
|---|---|---|
| `haltedWhenThirsty` | `true` | Health regenerates slower or not at all when thirst is not full |
| `depletesThirst` | `true` | Regenerating health makes thirst go down |
| `climateDependent` | `true` | That loss is scaled by the climate multiplier |

### `[sprint]`, `[drinking]`, `[hand]`, `[loot]`

| Key | Default | Meaning |
|---|---|---|
| `sprint.blockedWhenThirsty` | `true` | No sprinting with 3 droplets or less |
| `drinking.extraThirstToQuenched` | `true` | Thirst restored above full turns into quenched |
| `drinking.waterBottleStackSize` | `64` | Stack size of water bottles |
| `drinking.rain` | `true` | Drink rain by looking up |
| `hand.enabled` | `false` | Drink water in the world by sneaking and right-clicking with an empty hand |
| `hand.bothHandsEmpty` | `true` | Both hands must be empty |
| `hand.thirst` / `hand.quenched` | `3` / `2` | Restored per sip |
| `hand.cooldownTicks` | `10` | Minimum ticks between two sips |
| `loot.enabled` | `true` | Drinks are added to vanilla chest loot |

## `purity.toml`

| Key | Default | Meaning |
|---|---|---|
| `general.enabled` | `true` | Water has a purity at all. `false`: nothing stores or shows purity (tooltips, Jade, Create goggles), no purity effects, and the purification recipes are not loaded (they are checked when datapacks load: `/reload` or restart) |
| `general.defaultPurity` | `2` | Purity of water with none stored |
| `general.quenchWhenDebuffed` | `true` | Drinking still restores thirst when a purity effect blocks hydration |
| `world.*` | | See [Water purity in the world](Modpack-Makers.md#water-purity-in-the-world) |

### `[effects]`

One list per purity: `dirty`, `slightlyDirty`, `acceptable`, `purified`. Each entry is `"effect_id,durationTicks,amplifier,chancePercent[,blocksHydration]"`.

| Key | Default |
|---|---|
| `dirty` | `["minecraft:nausea,100,0,100", "minecraft:hunger,600,0,100", "minecraft:poison,200,0,30,true"]` |
| `slightlyDirty` | `["minecraft:nausea,100,0,50", "minecraft:hunger,600,0,50", "minecraft:poison,200,0,10,true"]` |
| `acceptable` | `["minecraft:nausea,100,0,5", "minecraft:hunger,600,0,5"]` |
| `purified` | `[]` |

- Any mob effect id works, also from other mods. An unknown id is skipped (listed by `/bluedroplets config check`).
- **One roll per drink** is shared by the whole list: an entry applies when the roll is below its chance. With the defaults, poisoned water always also gives nausea and hunger, as before.
- `blocksHydration` (`true`/`false`, default `false`): when that entry applies, the drink restores no thirst, unless `general.quenchWhenDebuffed` is `true` (the default).
- Old configs: the eight `*Percentage` values are turned into these lists once, with the old effects and durations (nausea 5 s and hunger 30 s share the nausea chance; poison 10 s blocks hydration).

## `compat.toml`

| Key | Default | Meaning |
|---|---|---|
| `create.sandFilterFiltrationAmount` | `1` | Purity levels gained in a Sand Filter |
| `create.sandFilterMbPerTick` | `10` | Millibuckets filtered per tick |

## `items.toml`

| Key | Default | Meaning |
|---|---|---|
| `overrides.drinks` / `overrides.foods` | `[]` | `["namespace:item" or "#tag", thirst, quenched]`; win over datapacks |
| `overrides.blacklist` | `[]` | Items that never restore thirst |
| `containers.containers` | `[]` | Drinks that carry a water purity |
| `keywords.enabled` | `false` | Give values to items by name patterns |

## `client.toml`

| Key | Default | Meaning |
|---|---|---|
| `Thirst Bar.thirstBarXOffset` / `thirstBarYOffset` | `0` | Moves the thirst bar |
