# Configuration

All TOML files live in `config/bluedroplets/`. They are NeoForge configs: missing keys are added with their default and a comment, values out of range are reset to the default, invalid list entries are removed, and edits are picked up while the game runs.

Per-id data (items, biomes, dimensions) lives in datapacks instead; see [Modpack makers](Modpack-Makers.md).

## Files

| File | Type | Content |
|---|---|---|
| `gameplay.toml` | common | Thirst loss, drinking, damage, regeneration, sprint, loot |
| `purity.toml` | common | Water purity in the world and its effects |
| `items.toml` | common | Per-item overrides, purity containers, keywords |
| `compat.toml` | common | One section per optional mod (`[create]`, …) |
| `client.toml` | client | HUD and tooltips; each player's own |

The gameplay files are **common** configs, not per-world server configs: they exist before a world is loaded (tooltips and stack sizes need them) and one file serves every world. On a dedicated server the server's values win: the ones the client needs (sprint rule, hand drinking, drink tables, default purity) are sent to each player.

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
| `Purity-related Effects.*Percentage` | `purity.toml` `effects.*` |
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
| `general.defaultPurity` | `2` | Purity of water with none stored |
| `general.quenchWhenDebuffed` | `true` | Drinking still restores thirst when a purity effect blocks hydration |
| `world.*` | | See [Water purity in the world](Modpack-Makers.md#water-purity-in-the-world) |
| `effects.*Percentage` | | Chance of nausea and hunger, or poison, per purity |

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
