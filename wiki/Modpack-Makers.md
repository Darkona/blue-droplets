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
| `thirst` | int 0-20 | Thirst restored |
| `quenched` | int ≥ 0 | Quenched restored (hidden "saturation" of thirst) |
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
- Blue Droplets ships its defaults in `data/bluedroplets/data_maps/item/drinks.json`: vanilla items, its own items and, only when those mods are installed, Farmer's Delight, Farmer's Respite, Brewin' and Chewin', Collector's Reap, Create (builder's tea) and Supernatural. To change a default, add your own entry (a later pack wins) or `remove` it.
- The map is synced to clients; tooltips, AppleSkin and the HUD use the server's values.

### Where an item's values come from

Each item takes its values from the first of these that has it; the others are ignored for that item:

1. **Blacklist**: the item tag `#bluedroplets:no_thirst` or `blacklist` in `items.toml` (the item restores no thirst at all).
2. **TOML overrides**: the `drinks` and `foods` lists in `items.toml` (section `overrides`).
3. **Datapacks**: the `bluedroplets:drinks` data map.
4. **Other mods' code** (`RegisterThirstValueEvent`).
5. **Keywords** (`items.toml`, section `keywords`, off by default).

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

Both world data maps are only read on the server.

## Item and fluid tags

| Tag | Registry | Default | Meaning |
|---|---|---|---|
| `bluedroplets:purity_containers` | item | Create builder's tea, Collector's Reap teas | Drinks that carry a water purity: filled with purity by machines (Create spouts), show it in the tooltip and roll its effects. They are not filled from the world |
| `bluedroplets:no_thirst` | item | empty | Never restores thirst, whatever the config, datapacks or other mods say |
| `bluedroplets:purity_opt_out` | item | empty | Never gets a purity: not filled with purity, no purity tooltip, no purity effects, and its fluid is not given one. Use it for other mods' water containers that break when water items carry extra data |
| `bluedroplets:carries_purity` | fluid | `#minecraft:water`, Create tea | Fluids made in a Create basin from water keep the water's purity |

Files: `data/bluedroplets/tags/item/purity_containers.json`, `data/bluedroplets/tags/item/no_thirst.json`, `data/bluedroplets/tags/item/purity_opt_out.json`, `data/bluedroplets/tags/fluid/carries_purity.json`. Use `{"id": "othermod:item", "required": false}` for optional mods.
