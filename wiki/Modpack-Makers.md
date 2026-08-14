# Modpack makers

Blue Droplets is configured in two layers:

- **Datapacks** hold everything that belongs to a registry id: item values, tags, biomes, dimensions, recipes, loot. They reload with `/reload` and the server sends clients what they need.
- **TOML files** in `config/blue_droplets/` hold global numbers and switches, plus a few per-item overrides. See [Configuration](Configuration).

All paths below are inside a datapack (`data/<namespace>/...`). Bad entries are logged and skipped. They never crash the game or disconnect players.

The formats on this page are those of Minecraft 26.3: datapacks declare `"min_format": 121, "max_format": 121` in `pack.mcmeta`. Other versions read other formats, listed on their own page (see the [home page](Home)).

---

## Drink and food values: `blue_droplets:drinks`

A NeoForge [data map](https://docs.neoforged.net/docs/resources/server/datamaps/) on items. File: `data/<namespace>/data_maps/item/drinks.json`, in any namespace. All packs are merged in load order.

```json
{
  "values": {
    "minecraft:apple": { "thirst": 2, "quenched": 3 },
    "#c:drinks/juice": { "thirst": 8, "quenched": 13 },
    "examplemod:spring_water": { "thirst": 6, "quenched": 8, "purity": 4 }
  },
  "remove": ["minecraft:carrot"]
}
```

| Field | Type | Meaning |
|---|---|---|
| `thirst` | int -20 to 20 | Thirst restored, in points (2 points = 1 droplet on the HUD). Negative values remove thirst (salty) |
| `quenched` | int, -20 or more | Quenched restored (the hidden reserve of thirst, like saturation). Negative values remove it |
| `purity` | int 0-5, optional | Purity of this drink when the stack stores none (0 contaminated to 5 pure). Drinks with a purity roll the purity effects. Without it, only water containers do |

- Keys are item ids or `#tags`. An entry from a later pack replaces the earlier one for that item. `"replace": true` at the top clears everything loaded before this file, and `remove` drops entries loaded before it (ids or tags).
- An unknown item id is logged as an error by NeoForge. For items of optional mods, put a condition on the entry:

  ```json
  "examplemod:lemonade": {
    "neoforge:conditions": [{ "type": "neoforge:mod_loaded", "modid": "examplemod" }],
    "thirst": 6, "quenched": 4
  }
  ```

- Items that are eaten and items that are drunk use the same map. Blue Droplets tells them apart by the item itself: food hydrates when its food component feeds the player, a drink when it is finished.
- Negative values make an item **salty**: eating or drinking it removes thirst and quenched (never below 0, and quenched stays at or below thirst). Salty items never count towards overhydration, roll purity effects only if they are water containers, show red droplets in the tooltip and flash in red on the HUD the droplets they would take away.
- The defaults are in `data/blue_droplets/data_maps/item/drinks.json`: vanilla items and the terracotta water bowl. Water bottles and the terracotta water bowl give 4 and 5, the melon slice 4 and 4, rotten flesh, spider eyes, pufferfish and poisonous potatoes -3 and -3. To change a default, add your own entry (a later pack wins) or `remove` it.
- The map is synced to clients. Tooltips and the HUD use the server's values.

### Block foods: `blue_droplets:hydrating_blocks`

Blocks eaten in place, like cake, are not items, so they have their own data map on blocks: `data/<namespace>/data_maps/block/hydrating_blocks.json`, with the same `thirst` and `quenched` fields. `purity` is ignored, because food never rolls purity effects. A bite counts when right-clicking the block raised the player's food level, so it works for any block that feeds the player directly (other mods' pies too). Negative values work here too.

```json
{
  "values": {
    "minecraft:cake": { "thirst": 1, "quenched": 1 },
    "#minecraft:candle_cakes": { "thirst": 1, "quenched": 1 }
  }
}
```

### Where an item's values come from

Each item takes its values from the first of these that has them. The others are ignored for that item:

1. **Blacklist**: the item tag `#blue_droplets:no_thirst` or `overrides.blacklist` in `items.toml`. The item restores no thirst at all.
2. **TOML overrides**: `overrides.drinks` and `overrides.foods` in `items.toml`.
3. **Datapacks**: the `blue_droplets:drinks` data map.
4. **Other mods' code**: `DropletsAPI.registerDrink`, then value providers of mods whose values depend on the stack. See [Mod developers](Mod-Developers).
5. **Salty tag**: items in `#blue_droplets:salty` get `salty.thirstPenalty` and `salty.quenchedPenalty` from `items.toml` (-2 and -2). Explicit values from 1-4 win, so a tagged item with its own entry keeps it.
6. **Keywords** (`items.toml`, section `keywords`, off by default).
7. **Estimated from recipes** (`items.toml`, section `inference`, off by default), only for items that nothing above gives values. Estimates are never negative. Tooltips add "(est.)", and `/blue_droplets infer <item>` shows how the number was made. See [Configuration](Configuration#recipe-inference).

The server resolves this table on world load and on `/reload`, and sends it to every client.

---

## Water purity in the world

Water picked up from the world (buckets, bottles, bowls, drinking by hand, dispensers, the Traveler's Backpack hose) gets a purity from 0 to 5:

1. **Poured water**: a source poured by a bucket keeps the purity it was poured with (see [Poured water](Configuration#poured-water)).
2. **Salt water**: if `saltWaterPurity` is 0-5 and the biome is in `#blue_droplets:salt_water`, that fixed value is used and nothing else applies.
3. **Base purity**, the first that is set:
   1. `base` of the biome in the `blue_droplets:biome_water` data map;
   2. the biome tags `#blue_droplets:water_purity/5` down to `/0`, checked in that order;
   3. `base` of the dimension type in the `blue_droplets:dimension_water` data map;
   4. `worldWaterBasePurity` in `purity.toml` (1).
4. Plus the **altitude** delta, the **still/running** delta and the biome's `delta`. The result is kept between 0 and the biome's `max` (5 by default).

Global settings in `purity.toml`, section `[world]`:

| Key | Default | Meaning |
|---|---|---|
| `altitudeBands` | `["30,59,1", "60,99,2", "100,4096,3", "-47,-16,1", "-79,-48,2", "-4096,-80,3"]` | `"minY,maxY,delta"`, both ends included. The first band containing the water's Y adds its delta. Relative to sea level: +1 from 30 to 59 blocks above it, +2 from 60 to 99, +3 from 100 up, and the same 16, 48 and 80 blocks below it |
| `altitudeRelativeToSeaLevel` | `true` | Bands are measured from the dimension's sea level (63 in the Overworld) instead of absolute Y |
| `stillWaterPurificationAmount` | `0` | Added to source water (-5 to 5) |
| `runningWaterPurificationAmount` | `1` | Added to flowing water (0 to 5) |
| `worldWaterBasePurity` | `1` | Base purity when neither biome nor dimension sets one |
| `saltWaterPurity` | `0` | Fixed purity in `#blue_droplets:salt_water` biomes (oceans). Sea water is always contaminated, which stands in for salt water. `-1` = off |

### Cauldrons

A water cauldron stores no purity. Whatever went into it (rain, dripstone, a bucket of contaminated or pure water), the water taken out of it (buckets, bottles, bowls) is always **murky (2)**, or **clean (4)** while the cauldron stands on a block of `#blue_droplets:cauldron_heat_sources` (blocks with a `lit` property only when lit). A cauldron never gives pure water: cooking does that. Jade shows the purity the water would have right now.

This holds for any water container another mod lets you fill from a cauldron, as long as it is a registered purity container, and for pumps and pipes of other mods that drain the cauldron through NeoForge's fluid capability.

### Biome tags

- `data/blue_droplets/tags/worldgen/biome/water_purity/0.json` to `/5.json`: plain biome tags. None ship with Blue Droplets: the defaults come from the `blue_droplets:biome_water` data map below, and biomes it does not list start at `worldWaterBasePurity`.
- `data/blue_droplets/tags/worldgen/biome/salt_water.json`: ships with `#minecraft:is_ocean`. Not used when `saltWaterPurity` is -1.

```json
{ "values": ["minecraft:cherry_grove", "#c:is_mountain"] }
```

### `blue_droplets:biome_water`

Biome data map, `data/<namespace>/data_maps/worldgen/biome/biome_water.json`. The defaults use the standard `c:` biome tags, so biomes from other mods are covered too:

| Biomes | Keys | `base` | `max` | Water (still / running) |
|---|---|---|---|---|
| Jungle, dark forest | `#c:is_jungle`, `minecraft:dark_forest` | 1 | 5 | 1 / 2, plus altitude |
| Taiga, birch forests | `#c:is_taiga`, `minecraft:birch_forest`, `minecraft:old_growth_birch_forest` | 2 | 5 | 2 / 3, plus altitude |
| Snowy biomes, frozen river | `#c:is_snowy`, `minecraft:frozen_river` | 2 | 5 | 2 / 3, plus altitude |
| Mountains, windswept hills | `#c:is_mountain`, `#c:is_windswept` | 3 | 5 | 3 / 4, plus altitude |
| Deserts and badlands | `#c:is_desert`, `#c:is_badlands` | 0 | 2 | 0 / 1, never more than 2 |
| Swamps and mangroves | `#c:is_swamp`, `minecraft:mangrove_swamp` | 0 | 1 | 0 / 1, never more than 1 |
| Everything else (plains, forests, rivers) | none | 1 (`worldWaterBasePurity`) | 5 | 1 / 2, plus altitude |

Oceans are fixed at 0 by `saltWaterPurity`.

When a biome is in several tags the entry listed last wins, and it replaces the earlier one completely. The shipped file lists the jungle, taiga, cold and mountain biomes first, then deserts and badlands, then swamps last, so a swamp or desert that a mod also tags as mountain keeps the swamp or desert values. Entries you add for the same biome or tag replace the shipped ones (a later datapack wins), and `"replace": true` at the top of your file drops all of them.

```json
{
  "values": {
    "minecraft:plains": { "base": 2 },
    "#c:is_snowy": { "delta": 1 }
  }
}
```

| Field | Type | Default | Meaning |
|---|---|---|---|
| `base` | int 0-5, optional | none | Base purity. Wins over the tags and the dimension |
| `delta` | int -5 to 5 | 0 | Added after altitude and running water |
| `max` | int 0-5 | 5 | Highest purity water can have here |

### `blue_droplets:dimension_water`

Dimension type data map, `data/<namespace>/data_maps/dimension_type/dimension_water.json`. It is keyed by **dimension type** (`minecraft:overworld`, `minecraft:the_nether`, a mod's type id), so dimensions that share a type share the value.

```json
{ "values": { "minecraft:the_end": { "base": 5 } } }
```

| Field | Type | Meaning |
|---|---|---|
| `base` | int 0-5, optional | Base purity for biomes without their own |
| `thirst_multiplier` | float 0-10, optional | Replaces the whole climate factor of thirst loss in this dimension type. Without it the Nether uses `netherMultiplier` from `gameplay.toml` |

Blue Droplets sets `minecraft:the_end` to `thirst_multiplier` `0.6`, so the End counts as cold. A datapack entry for `minecraft:the_end` replaces it. Both world data maps are only read on the server.

---

## Effects

### Dehydration

`blue_droplets:dehydration` is a harmful effect that makes thirst drop faster, like Hunger does for food: 0.005 exhaustion per tick per level, times `effects.dehydrationMultiplier`. Thirst never goes below zero. By default only `[hotDirtyWater]` gives it. Add it to a purity effect list for more, for example `"blue_droplets:dehydration,600,0,25"` in `effects.dirty` for a 25% chance of 30 s of Dehydration I from dirty water.

### Quenchness and its potions

`blue_droplets:quenchness` restores (level) thirst and (level) quenched every `effects.quenchnessIntervalTicks` (40), like Regeneration does for health. The potions are in the `minecraft:potion` registry, so loot tables, `set_potion` and commands can use them: `/give @p minecraft:potion[potion_contents={potion:"blue_droplets:quenchness"}]`.

| Potion | Effect | Brewing |
|---|---|---|
| `blue_droplets:quenchness` | Quenchness I, 0:45 | awkward potion + prismarine crystals |
| `blue_droplets:long_quenchness` | Quenchness I, 1:30 | Quenchness + redstone |
| `blue_droplets:strong_quenchness` | Quenchness II, 0:22 | Quenchness + glowstone dust |

The mixes are 15 `minecraft:brewing` recipes in `data/blue_droplets/recipe/brewing/`: each mix for the potion, splash and lingering containers, plus the gunpowder and dragon's breath container changes. Every one carries the load condition `blue_droplets:quenchness_potion`, which follows `gameplay.toml` `effects.quenchnessPotion`. To use another ingredient, put a recipe with the same id in your datapack, or turn the switch off and add your own mixes without that condition. The first mix, `blue_droplets:brewing/potion_awkward_prismarine_crystals`:

```json
{
  "neoforge:conditions": [{ "type": "blue_droplets:quenchness_potion" }],
  "type": "minecraft:brewing",
  "input": { "item": "minecraft:potion", "potion_contents": { "potions": "minecraft:awkward" } },
  "reagent": { "item": "minecraft:prismarine_crystals" },
  "output": { "id": "minecraft:potion", "components": { "minecraft:potion_contents": { "potion": "blue_droplets:quenchness" } } }
}
```


### Hydrated

`blue_droplets:hydrated` slows thirst loss: all thirst exhaustion is multiplied by `effects.hydratedMultiplier` (0.5) once per level, so Hydrated II quarters it. It shows in `/blue_droplets debug exhaustion` as `hydrated`. Sources: `/effect give @p blue_droplets:hydrated 60 0`, the purity effect lists (for example `"blue_droplets:hydrated,600,0,100"` in `effects.pure`) and the full-hydration bonus (`[hydration] fullBonus`, off by default). There is no potion for it.

### Overhydrated

`blue_droplets:overhydrated` makes the player 10% slower per level (an attribute modifier on `minecraft:movement_speed`, like Slowness). Drinking far past full gives it (`[overhydration]`, on by default), and `/effect` works too.

Vanilla effects that change thirst: Nausea drains it (`depletion.nauseaDepletes`), Fire Resistance reduces it (`fireResistancePercent`), Hunger does not (its extra food exhaustion is left out in `MIRROR_FOOD` mode), and, with `effects.waterBreathingReducesThirst` (off), Water Breathing or Conduit Power reduce it while fully underwater.

---

## Thirst drain attribute: `blue_droplets:thirst_drain`

Every player has the attribute `blue_droplets:thirst_drain` (base 1.0, 0 to 10). Thirst loss is multiplied by it, so anything that carries attribute modifiers changes thirst without code:

- items: the vanilla `minecraft:attribute_modifiers` component (`/give`, loot tables, other mods);
- enchantments (datapack): the `minecraft:attributes` effect component;
- effects, Curios, other mods: plain `AttributeModifier`s;
- commands: `/attribute @s blue_droplets:thirst_drain base set 0.5`.

```json
"minecraft:attribute_modifiers": [
  { "type": "blue_droplets:thirst_drain", "id": "examplemod:cooling_helmet", "amount": -0.25, "operation": "add_multiplied_base", "slot": "head" }
]
```

---

## Tags

| Tag | Registry | Default | Meaning |
|---|---|---|---|
| `blue_droplets:purity_containers` | item | empty | Drinks that carry a water purity: they show it in the tooltip and roll its effects. They are not filled from the world |
| `blue_droplets:no_thirst` | item | empty | Never restores thirst, whatever the config, datapacks or other mods say |
| `blue_droplets:salty` | item | rotten flesh, spider eye, pufferfish, poisonous potato | Items with no other values get the `items.toml` `salty` penalties. The four defaults have their own entries (-3) in the drinks data map, which win |
| `blue_droplets:purity_opt_out` | item | empty | Never gets a purity: no purity tooltip, no purity effects, and its fluid is not given one. For other mods' water containers that break when water items carry extra data |
| `blue_droplets:cauldron_heat_sources` | block | `#minecraft:campfires`, `#minecraft:fire`, magma block, lava | Heat sources under a water cauldron: its water comes out clean (4) instead of murky (2). Blocks with a `lit` property only count while lit |
| `blue_droplets:pauses_thirst` | mob_effect | empty | While the player has one of these effects, thirst exhaustion stops building up. Drinking and regeneration still work |
| `blue_droplets:stops_thirst` | mob_effect | Corail Tombstone's Ghostly Shape, if installed | While the player has one of these effects, thirst does not tick at all: no exhaustion, no dehydration damage, no regeneration cost |

Files: `data/blue_droplets/tags/item/<name>.json`, `tags/block/<name>.json`, `tags/mob_effect/<name>.json`. Use `{"id": "othermod:item", "required": false}` for optional mods. Effects are read when the player's effects change and once a second, not every tick.

---

## Purification recipes

Purifying water uses vanilla recipe types (`minecraft:smelting`, `minecraft:campfire_cooking`, `minecraft:smoking`) with NeoForge component ingredients: the ingredient matches water of one purity and the result stores a higher one. JEI and other recipe viewers show them like any other recipe. JEI also gets a "Water Purification" page for what is not a recipe: the water cauldron, plain and over a heat source. The purification page is hidden when `purity.enabled` is `false`. A "Hydration" page lists every item that changes thirst with the values the server resolved, so it shows your changes after `/reload`.

The recipes ship as optional built-in datapacks, listed in the datapack screen when creating a world and in `/datapack list`:

| Pack id | Default | Recipes |
|---|---|---|
| `mod/blue_droplets:datapacks/purify_smelting` | enabled | Furnace: two levels up per cook, up to pure (5) |
| `mod/blue_droplets:datapacks/purify_campfire` | enabled | Campfire: one level up per cook, up to pure (5) |
| `mod/blue_droplets:datapacks/purify_smoking` | disabled | Smoker: as the furnace, twice as fast |

Each pack covers water bottles, water buckets and terracotta water bowls.

- Turn a method off or on per world with `/datapack disable "mod/blue_droplets:datapacks/purify_campfire"` or `/datapack enable ...`. The choice is saved with the world.
- Recipe ids follow `<item>_from_<method>_to_<level>`, for example `blue_droplets:water_bottle_from_smelting_to_murky`. To change one, put a recipe with the same id in your own datapack above it.
- Water with no purity stored counts as acceptable (3) and is matched by the recipes that take level 3.
- Every purification recipe carries the condition `{"type": "blue_droplets:purity_enabled"}`, so none load when `purity.enabled` is `false`. Use it in your own purity recipes too.

One level of purification for a modded water container:

```json
{
  "neoforge:conditions": [{ "type": "blue_droplets:purity_enabled" }],
  "type": "minecraft:campfire_cooking",
  "ingredient": {
    "neoforge:ingredient_type": "neoforge:components",
    "items": "examplemod:canteen",
    "components": { "blue_droplets:purity": 1 }
  },
  "result": { "id": "examplemod:canteen", "components": { "blue_droplets:purity": 2 } },
  "cookingtime": 300
}
```

A water bottle is a potion, so its ingredient also names the potion: `"items": "minecraft:potion", "components": {"minecraft:potion_contents": {"potion": "minecraft:water"}, "blue_droplets:purity": 1}`.

Water with no purity stored does not match a `"blue_droplets:purity": 3` ingredient. Match it with `"strict": true` and the other components only, and join both in a `neoforge:compound` ingredient, as the built-in recipes do:

```json
"ingredient": {
  "neoforge:ingredient_type": "neoforge:compound",
  "children": [
    { "neoforge:ingredient_type": "neoforge:components", "items": "minecraft:water_bucket", "components": { "blue_droplets:purity": 3 } },
    { "neoforge:ingredient_type": "neoforge:components", "items": "minecraft:water_bucket", "components": {}, "strict": true }
  ]
}
```

---

## Chest loot

Blue Droplets adds water bottles, acceptable (3) or pure (5), to the chests of dungeons, mineshafts, shipwrecks (supply), Nether fortresses and bastions. Each chest is one loot table in `data/blue_droplets/loot_table/chests/` and one global loot modifier in `data/blue_droplets/loot_modifiers/`. NeoForge loads every file in that folder, with no `global_loot_modifiers.json` list. Both carry the load condition `blue_droplets:loot_config`, which follows `gameplay.toml` `loot.enabled`.

Loot tables and modifiers use the Minecraft 26.3 loot format: `modifier` in place of `functions`, a single `condition` in place of the `conditions` list, and `type` as the key of every function and condition. Several conditions go in `{"type": "minecraft:all_of", "terms": [...]}`.

```json
{
  "neoforge:conditions": [{ "type": "blue_droplets:loot_config" }],
  "type": "neoforge:add_table",
  "condition": { "type": "neoforge:loot_table_id", "loot_table_id": "minecraft:chests/simple_dungeon" },
  "table": "blue_droplets:chests/simple_dungeon"
}
```

The purity of a bottle in a loot table goes on a `set_components` function with the loot condition `blue_droplets:purity_enabled`, so the bottle comes without a purity when purity is off:

```json
"modifier": [
  { "type": "minecraft:set_components", "components": { "blue_droplets:purity": 3 }, "condition": { "type": "blue_droplets:purity_enabled" } },
  { "type": "minecraft:set_count", "count": { "type": "minecraft:uniform", "min": 1, "max": 3 } },
  { "type": "minecraft:set_potion", "id": "minecraft:water" }
]
```

Loot in the old format loads without an error, but its functions and conditions are ignored, and a global loot modifier with the old `conditions` list adds its table to every loot table in the game. `scripts/loot/convert.py` in the repository converts loot tables and modifiers to the 26.3 format.

---

## Presets

Two optional presets change the balance. The default is Blue Droplets' own. Each has two parts, applied by hand (nothing rewrites your config):

| Part | casual | hardcore |
|---|---|---|
| Built-in datapack, disabled by default: enable it when creating the world or with `/datapack enable` | `mod/blue_droplets:datapacks/preset_casual`: Nether climate factor 1.5 instead of 3 | `mod/blue_droplets:datapacks/preset_hardcore`: Nether climate factor 4 |
| TOML keys to copy into `config/blue_droplets/` ([`examples/presets/`](https://github.com/Darkona/blue-droplets/tree/main/examples/presets) in the repository) | `multiplier` 0.8, riding ×0.5, dehydration stops at 5 hearts on Normal and never kills, drinking by hand on, no poison: contaminated water 50% nausea and hunger, dirty 25%, murky 10% | `multiplier` 1.6, weather, day, sun and water multipliers, riding ×1, a rain sip every 2 s, drinking by hand with a 1 s cooldown, no running water bonus, world water starts contaminated, contaminated water 60% poison |

The TOML files only list the keys they change. Blue Droplets adds the rest with their defaults.

---

## Resource packs

The terracotta water bowl's item model (`assets/blue_droplets/items/terracotta_water_bowl.json`) tints its water layer with the item tint source `blue_droplets:water_purity` (field `default`, an RGB colour used when the stack has no purity). A resource pack can put the same tint source on other water containers. Water bottles are tinted through vanilla's potion tint.
