# Modpack makers

Blue Droplets is configured in two layers:

- **Datapacks** hold everything that belongs to a registry id: item values, tags, biomes, dimensions. They reload with `/reload` and are sent to clients by the server.
- **TOML files** in `config/bluedroplets/` hold global numbers and switches, plus a few explicit per-item overrides. See [Configuration](Configuration.md).

All paths below are inside a datapack (`data/<namespace>/...`). Bad entries are logged and skipped; they never crash the game or disconnect players.

## Drink and food values: `bluedroplets:drinks`

A NeoForge [data map](https://docs.neoforged.net/docs/resources/server/datamaps/) on items. File: `data/<namespace>/data_maps/item/drinks.json` (any namespace; all packs are merged in load order).

```json
{
  "values": {
    "minecraft:apple": { "thirst": 2, "quenched": 3 },
    "#c:drinks/juice": { "thirst": 8, "quenched": 13 },
    "examplemod:spring_water": { "thirst": 6, "quenched": 8, "purity": 3 }
  },
  "remove": ["minecraft:carrot"]
}
```

| Field | Type | Meaning |
|---|---|---|
| `thirst` | int -20 to 20 | Thirst restored, in points (2 points = 1 droplet on the HUD); negative removes thirst (salty) |
| `quenched` | int ≥ -20 | Quenched restored (hidden "saturation" of thirst); negative removes quenched |
| `purity` | int 0-3, optional | Purity of this drink when the stack stores none (0 dirty, 1 slightly dirty, 2 acceptable, 3 purified). Drinks with a purity roll the purity effects; without it only water containers do |

- Keys are item ids or `#tags`. An entry from a later pack replaces the earlier one for that item. `"replace": true` at the top clears everything loaded before this file.
- `remove` drops entries loaded before this file (ids or tags).
- An unknown item id is logged as an error by NeoForge. For items of optional mods, put a condition on the entry:

  ```json
  "farmersdelight:apple_cider": {
    "neoforge:conditions": [{ "type": "neoforge:mod_loaded", "modid": "farmersdelight" }],
    "thirst": 8, "quenched": 13
  }
  ```

- Items with food properties (eaten) and items without (drunk) use the same map; Blue Droplets tells them apart by the item itself.
- Negative values make an item **salty**: eating or drinking it removes thirst and quenched (never below 0; quenched stays at or below thirst). Salty items never count towards overhydration, roll purity effects only if they are water containers, show red droplets in the tooltip and flash in red the droplets they would take away on the HUD. Saltiness is measured in the same points as thirst: 2 points = 1 droplet on the HUD (20 points = 10 droplets). No vanilla item is salty by default. For example:

  ```json
  "minecraft:cooked_cod": { "thirst": -2, "quenched": -2 },
  "minecraft:dried_kelp": { "thirst": -1, "quenched": -2 },
  "minecraft:cooked_salmon": { "thirst": 1, "quenched": -1 }
  ```

  Or tag them `#bluedroplets:salty` and let `items.toml` `salty.thirstPenalty`/`quenchedPenalty` (-2/-2) give the values.
- Blue Droplets ships its defaults in `data/bluedroplets/data_maps/item/drinks.json`: vanilla items, its own items and, only when those mods are installed, Farmer's Delight, Farmer's Respite, Brewin' and Chewin', Collector's Reap, Create (builder's tea) and Supernatural. To change a default, add your own entry (a later pack wins) or `remove` it.
- The map is synced to clients; tooltips and the HUD use the server's values.

### Block foods: `bluedroplets:hydrating_blocks`

Blocks eaten in place, like cake, are not items, so they have their own data map on blocks:
`data/<namespace>/data_maps/block/hydrating_blocks.json`, with the same `thirst` and `quenched` fields (`purity` is
ignored; food never rolls purity effects). A bite counts when right-clicking the block raised the player's food level,
so it works for any block that feeds the player directly (other mods' pies too), but not for a bite that gives no food.
Default:

```json
{
  "values": {
    "minecraft:cake": { "thirst": 1, "quenched": 1 },
    "#minecraft:candle_cakes": { "thirst": 1, "quenched": 1 }
  }
}
```

Negative values work here too (salty block foods).

### Where an item's values come from

Each item takes its values from the first of these that has it; the others are ignored for that item:

1. **Blacklist**: the item tag `#bluedroplets:no_thirst` or `blacklist` in `items.toml` (the item restores no thirst at all).
2. **TOML overrides**: the `drinks` and `foods` lists in `items.toml` (section `overrides`).
3. **Datapacks**: the `bluedroplets:drinks` data map.
4. **Other mods' code**: `DropletsAPI.registerDrink` (and the older `RegisterThirstValueEvent`), then per-item value providers of mods whose values depend on the stack. See [Mod developers](Mod-Developers.md).
5. **Salty tag**: items in `#bluedroplets:salty` (empty by default) get `salty.thirstPenalty` and `salty.quenchedPenalty` from `items.toml` (-2 / -2). Explicit values from 1-4 win, so a tagged item with its own entry keeps it.
6. **Keywords** (`items.toml`, section `keywords`, off by default).
7. **Estimated from recipes** (`items.toml`, section `inference`, off by default): only for items none of the above gives values. Negative (salty) ingredient values count as 0, so estimates are never negative. Tooltips add "(est.)"; `/bluedroplets infer <item>` shows how the number was made. See [Configuration](Configuration.md#recipe-inference).

The server resolves this table on world load and on `/reload` and sends it to every client, so all players see the server's values.

## Water purity in the world

Water picked up from the world (buckets, bottles, bowls, drinking by hand, Create pumps and drains) gets a purity from 0 (dirty) to 3 (purified):

1. **Salt water**: if `saltWaterPurity` (`purity.toml`, default -1 = off) is 0-3 and the biome is in `#bluedroplets:salt_water`, that fixed value is used and nothing else applies.
2. **Base purity**, the first that is set:
   1. `base` of the biome in the `bluedroplets:biome_water` data map;
   2. biome tags `#bluedroplets:water_purity/3`, `/2`, `/1`, `/0` (checked in that order);
   3. `base` of the dimension type in the `bluedroplets:dimension_water` data map;
   4. `worldWaterBasePurity` in `purity.toml` (default 0).
3. Plus the **altitude** delta, the **still/running** delta and the biome's `delta`; the result is kept between 0 and the biome's `max` (default 3).

Global settings in `purity.toml`, section `world`:

| Key | Default | Meaning |
|---|---|---|
| `altitudeBands` | `["38,4096,1", "-4096,-16,1"]` | `"minY,maxY,delta"`, both ends included; the first band containing the water's Y adds its delta. Default: +1 in mountains and in caves |
| `altitudeRelativeToSeaLevel` | `true` | Measure the bands from the dimension's sea level (63 in the Overworld) instead of absolute Y |
| `stillWaterPurificationAmount` | `0` | Added to source water (-3 to 3) |
| `runningWaterPurificationAmount` | `1` | Added to flowing water (0 to 3) |
| `worldWaterBasePurity` | `0` | Base purity when neither biome nor dimension sets one |
| `saltWaterPurity` | `-1` | Fixed purity in `#bluedroplets:salt_water` biomes; -1 = off |
| `rainCauldronPurity` | `-1` | Purity of rain collected in a cauldron; -1 = none stored (reads as `defaultPurity`) |
| `dripstoneCauldronPurity` | `-1` | Purity of water dripped into a cauldron by pointed dripstone; -1 = none stored |

Rain or dripstone adding water to a cauldron that already has water keeps the lower of the two purities.

### Biome tags

- `data/bluedroplets/tags/worldgen/biome/water_purity/0.json` … `/3.json`: standard biome tags. None ship with Blue Droplets, so by default all water starts at `worldWaterBasePurity`.
- `data/bluedroplets/tags/worldgen/biome/salt_water.json`: ships with `#minecraft:is_ocean`; only used when `saltWaterPurity` is set.

```json
{ "values": ["minecraft:cherry_grove", "#c:is_mountain"] }
```

### `bluedroplets:biome_water`

Biome data map, `data/<namespace>/data_maps/worldgen/biome/biome_water.json`:

```json
{
  "values": {
    "minecraft:swamp": { "base": 0, "max": 1 },
    "#c:is_snowy": { "delta": 1 }
  }
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `base` | int 0-3, optional | — | Base purity; wins over the tags and the dimension |
| `delta` | int -3 to 3 | 0 | Added after altitude and running water |
| `max` | int 0-3 | 3 | Highest purity water can have here |

### `bluedroplets:dimension_water`

Dimension type data map, `data/<namespace>/data_maps/dimension_type/dimension_water.json`. It is keyed by **dimension type** (`minecraft:overworld`, `minecraft:the_nether`, a mod's type id), so dimensions that share a type share the value.

```json
{ "values": { "minecraft:the_end": { "base": 3 } } }
```

| Field | Type | Meaning |
|---|---|---|
| `base` | int 0-3, optional | Base purity for biomes without their own |
| `thirst_multiplier` | float 0-10, optional | Replaces the climate multiplier of thirst loss in this dimension type (the Nether uses `netherMultiplier` from `gameplay.toml` unless set here) |

Both world data maps are only read on the server.

## Dehydration effect

`bluedroplets:dehydration` is a harmful effect that makes thirst drop faster, like Hunger does for food: 0.005 exhaustion per tick per level, times `effects.dehydrationMultiplier` in `gameplay.toml`. Thirst never goes below zero. Nothing applies it by default. Give it with `/effect give @p bluedroplets:dehydration 30 1`, or add it to a purity effect list in `purity.toml` (`"effect_id,durationTicks,amplifier,chancePercent[,blocksHydration]"`), for example `"bluedroplets:dehydration,600,0,25"` in `effects.dirty` for a 25% chance of 30 s of Dehydration I from dirty water. See [Configuration](Configuration.md#effects).

## Quenchness effect and potions

`bluedroplets:quenchness` is a beneficial effect: every `effects.quenchnessIntervalTicks` (default 40) it restores (level) thirst and (level) quenched, like Regeneration for health. Potions (registry `minecraft:potion`, usable in loot tables, `set_potion`, recipes and `/give @p minecraft:potion[potion_contents={potion:"bluedroplets:quenchness"}]`):

| Potion | Effect | Brewing |
|---|---|---|
| `bluedroplets:quenchness` | Quenchness I, 0:45 | awkward potion + prismarine crystals |
| `bluedroplets:long_quenchness` | Quenchness I, 1:30 | Quenchness + redstone |
| `bluedroplets:strong_quenchness` | Quenchness II, 0:22 | Quenchness + glowstone dust |

Splash, lingering and tipped arrows work as for vanilla potions. `effects.quenchnessPotion = false` in `gameplay.toml` removes the three brewing recipes (the potions stay registered); to use another ingredient, turn it off and add your own mix with KubeJS or a mod (`RegisterBrewingRecipesEvent`).

## Hydrated effect

`bluedroplets:hydrated` is a beneficial effect that slows thirst loss: all thirst exhaustion is multiplied by `effects.hydratedMultiplier` (default 0.5) once per level, so Hydrated II with the default quarters it. It is part of the cached thirst loss multiplier (`/bluedroplets debug exhaustion` shows it as `hydrated`). Sources: `/effect give @p bluedroplets:hydrated 60 0`, the purity effect lists in `purity.toml` (for example `"bluedroplets:hydrated,600,0,100"` in `effects.purified`), and the optional full-hydration bonus (`[hydration] fullBonus`, off by default). There is no potion for it.

## Overhydrated effect

`bluedroplets:overhydrated` is a harmful effect: 10% slower movement per level (an attribute modifier on `minecraft:generic.movement_speed`, like Slowness). Drinking far past full gives it (`[overhydration]` in `gameplay.toml`, on by default; see [Configuration](Configuration.md#overhydration)); `/effect` works too. The thirst bar turns greyish blue while it lasts (`Bar Colors.overhydrated` in `client.toml`).

Vanilla effects that change thirst: Nausea drains it (`depletion.nauseaDepletes`), Fire Resistance reduces it (`fireResistancePercent`), Hunger does not (its extra food exhaustion is left out in `MIRROR_FOOD` mode), and, with `effects.waterBreathingReducesThirst` (off), Water Breathing or Conduit Power reduce it while fully underwater (`effects.underwaterBreathingMultiplier`, 0.5).

## Thirst drain attribute: `bluedroplets:thirst_drain`

Every player has the attribute `bluedroplets:thirst_drain` (base 1.0, 0 to 10). Thirst loss is multiplied by it, so anything that can carry attribute modifiers can change thirst without code:

- items: the vanilla `minecraft:attribute_modifiers` component (`/give`, loot tables, other mods);
- enchantments (datapack): the `minecraft:attributes` effect component;
- effects, Curios, other mods: ordinary `AttributeModifier`s;
- commands: `/attribute @s bluedroplets:thirst_drain base set 0.5`.

```json
"minecraft:attribute_modifiers": {
  "modifiers": [{ "type": "bluedroplets:thirst_drain", "id": "examplemod:cooling_helmet", "amount": -0.25,
                  "operation": "add_multiplied_base", "slot": "head" }]
}
```


## Item and fluid tags

| Tag | Registry | Default | Meaning |
|---|---|---|---|
| `bluedroplets:purity_containers` | item | Create builder's tea, Collector's Reap teas | Drinks that carry a water purity: filled with purity by machines (Create spouts), show it in the tooltip and roll its effects. They are not filled from the world |
| `bluedroplets:no_thirst` | item | empty | Never restores thirst, whatever the config, datapacks or other mods say |
| `bluedroplets:salty` | item | empty | Items with no other values get the `items.toml` `salty` penalties (they make the player thirstier), eaten or drunk |
| `bluedroplets:purity_opt_out` | item | empty | Never gets a purity: not filled with purity, no purity tooltip, no purity effects, and its fluid is not given one. Use it for other mods' water containers that break when water items carry extra data |
| `bluedroplets:carries_purity` | fluid | `#minecraft:water`, Create tea | Fluids made in a Create basin from water keep the water's purity |
| `bluedroplets:cauldron_heat_sources` | block | `#minecraft:campfires`, `#minecraft:fire`, magma block, lava | Heat sources that boil a water cauldron above them when `purity.toml` `cauldron.boiling` is on. Blocks with a `lit` property (campfires, furnaces) only count while lit |

Files: `data/bluedroplets/tags/item/purity_containers.json`, `data/bluedroplets/tags/item/no_thirst.json`, `data/bluedroplets/tags/item/purity_opt_out.json`, `data/bluedroplets/tags/fluid/carries_purity.json`, `data/bluedroplets/tags/block/cauldron_heat_sources.json`. Use `{"id": "othermod:item", "required": false}` for optional mods.

## Purification recipes

Purifying water uses vanilla recipe types (`minecraft:smelting`, `minecraft:campfire_cooking`, `minecraft:smoking`) with NeoForge component ingredients: the ingredient matches water of a given purity and the result stores a higher one. JEI and other recipe viewers show them like any other recipe.

The recipes ship as optional built-in datapacks, one per method, listed in the datapack screen when creating a world and in `/datapack list`:

| Pack id | Default | Recipes |
|---|---|---|
| `mod/bluedroplets:datapacks/purify_smelting` | enabled | Furnace: dirty → acceptable, slightly dirty / acceptable / none stored → purified |
| `mod/bluedroplets:datapacks/purify_campfire` | enabled | Campfire: one level per cook |
| `mod/bluedroplets:datapacks/purify_smoking` | disabled | Smoker: same as the furnace, twice as fast |

- Turn a method off or on per world with `/datapack disable "mod/bluedroplets:datapacks/purify_campfire"` / `/datapack enable ...`; the choice is saved with the world.
- To change a recipe, put a recipe with the same id (`bluedroplets:water_bottle_from_smelting_purified`, …) in your own datapack above it.
- Every purification recipe carries the condition `{"type": "bluedroplets:purity_enabled"}`, so none load when `purity.enabled` is `false`. Use it in your own purity recipes too.

Example of one level of purification for a modded water container:

```json
{
  "neoforge:conditions": [{ "type": "bluedroplets:purity_enabled" }],
  "type": "minecraft:campfire_cooking",
  "ingredient": {
    "type": "neoforge:components",
    "items": "examplemod:canteen",
    "components": { "bluedroplets:purity": 0 }
  },
  "result": { "id": "examplemod:canteen", "count": 1, "components": { "bluedroplets:purity": 1 } },
  "cookingtime": 300
}
```

Water with no purity stored counts as `defaultPurity` (2, acceptable) in the game but does not match a `"bluedroplets:purity": 2` ingredient; match it with `{"type": "neoforge:components", "items": "...", "components": {}, "strict": true}` as the built-in recipes do.

## Presets

Two optional presets change the balance; the default is Blue Droplets' own behaviour. Each has two parts, applied by hand (nothing rewrites your config):

| Part | casual | hardcore |
|---|---|---|
| Built-in datapack (disabled by default; enable when creating the world or with `/datapack enable`) | `mod/bluedroplets:datapacks/preset_casual`: Nether climate ×1.5 | `mod/bluedroplets:datapacks/preset_hardcore`: Nether climate ×4 |
| TOML keys to copy into `config/bluedroplets/` ([`docs/presets/`](../docs/presets/) in the repository) | `multiplier` 0.8, riding ×0.5, dehydration stops at 5 hearts on Normal and never kills, drinking by hand on, dirty water: 50% nausea and hunger, no poison | `multiplier` 1.6, weather/day/sun/water multipliers, riding ×1, rain every 2 s, drinking by hand with a 1 s cooldown, no running water bonus, dirty water: 60% poison |

The TOML files only list the keys they change; Blue Droplets adds the rest with their defaults.
