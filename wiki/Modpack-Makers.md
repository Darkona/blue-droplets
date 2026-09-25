# Modpack makers

Droplets of Thirst is configured in two layers:

- **Datapacks** hold everything that belongs to a registry id: item values, tags, biomes, dimensions. They reload with `/reload` and are sent to clients by the server.
- **TOML files** in `config/droplets_of_thirst/` hold global numbers and switches, plus a few explicit per-item overrides. See [Configuration](Configuration).

All paths below are inside a datapack (`data/<namespace>/...`). Bad entries are logged and skipped; they never crash the game or disconnect players.

## Drink and food values: `droplets_of_thirst:drinks`

A NeoForge [data map](https://docs.neoforged.net/docs/resources/server/datamaps/) on items. File: `data/<namespace>/data_maps/item/drinks.json` (any namespace; all packs are merged in load order).

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
| `thirst` | int -20 to 20 | Thirst restored, in points (2 points = 1 droplet on the HUD); negative removes thirst (salty) |
| `quenched` | int ≥ -20 | Quenched restored (hidden "saturation" of thirst); negative removes quenched |
| `purity` | int 0-5, optional | Purity of this drink when the stack stores none (0 contaminated, 1 dirty, 2 murky, 3 acceptable, 4 clean, 5 pure). Drinks with a purity roll the purity effects; without it only water containers do |

- Keys are item ids or `#tags`. An entry from a later pack replaces the earlier one for that item. `"replace": true` at the top clears everything loaded before this file.
- `remove` drops entries loaded before this file (ids or tags).
- An unknown item id is logged as an error by NeoForge. For items of optional mods, put a condition on the entry:

  ```json
  "farmersdelight:apple_cider": {
    "neoforge:conditions": [{ "type": "neoforge:mod_loaded", "modid": "farmersdelight" }],
    "thirst": 8, "quenched": 13
  }
  ```

- Items with food properties (eaten) and items without (drunk) use the same map; Droplets of Thirst tells them apart by the item itself.
- Negative values make an item **salty**: eating or drinking it removes thirst and quenched (never below 0; quenched stays at or below thirst). Salty items never count towards overhydration, roll purity effects only if they are water containers, show red droplets in the tooltip and flash in red the droplets they would take away on the HUD. Saltiness is measured in the same points as thirst: 2 points = 1 droplet on the HUD (20 points = 10 droplets). No vanilla item is salty by default. For example:

  ```json
  "minecraft:cooked_cod": { "thirst": -2, "quenched": -2 },
  "minecraft:dried_kelp": { "thirst": -1, "quenched": -2 },
  "minecraft:cooked_salmon": { "thirst": 1, "quenched": -1 }
  ```

  Or tag them `#droplets_of_thirst:salty` and let `items.toml` `salty.thirstPenalty`/`quenchedPenalty` (-2/-2) give the values.
- Droplets of Thirst ships its defaults in `data/droplets_of_thirst/data_maps/item/drinks.json`: vanilla items, its own items and, only when those mods are installed, Farmer's Delight, Farmer's Respite, Brewin' and Chewin', Collector's Reap, Create (builder's tea) and Supernatural. To change a default, add your own entry (a later pack wins) or `remove` it.
- The map is synced to clients; tooltips and the HUD use the server's values.

### Block foods: `droplets_of_thirst:hydrating_blocks`

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

1. **Blacklist**: the item tag `#droplets_of_thirst:no_thirst` or `blacklist` in `items.toml` (the item restores no thirst at all).
2. **TOML overrides**: the `drinks` and `foods` lists in `items.toml` (section `overrides`).
3. **Datapacks**: the `droplets_of_thirst:drinks` data map.
4. **Other mods' code**: `DropletsAPI.registerDrink` (and the older `RegisterThirstValueEvent`), then per-item value providers of mods whose values depend on the stack. See [Mod developers](Mod-Developers).
5. **Salty tag**: items in `#droplets_of_thirst:salty` (empty by default) get `salty.thirstPenalty` and `salty.quenchedPenalty` from `items.toml` (-2 / -2). Explicit values from 1-4 win, so a tagged item with its own entry keeps it.
6. **Keywords** (`items.toml`, section `keywords`, off by default).
7. **Estimated from recipes** (`items.toml`, section `inference`, off by default): only for items none of the above gives values. Negative (salty) ingredient values count as 0, so estimates are never negative. Tooltips add "(est.)"; `/droplets_of_thirst infer <item>` shows how the number was made. See [Configuration](Configuration#recipe-inference).

The server resolves this table on world load and on `/reload` and sends it to every client, so all players see the server's values.

## Water purity in the world

Water picked up from the world (buckets, bottles, bowls, drinking by hand, Create hose pulleys and open pipe ends, and the taps, sinks and wells of Extra Delight and Farm & Charm, which take it from where they stand) gets a purity from 0 (contaminated) to 5 (pure). The six levels are 0 contaminated, 1 dirty, 2 murky, 3 acceptable, 4 clean and 5 pure:

1. **Salt water**: if `saltWaterPurity` (`purity.toml`, default 0; -1 = off) is 0-5 and the biome is in `#droplets_of_thirst:salt_water`, that fixed value is used and nothing else applies.
2. **Base purity**, the first that is set:
   1. `base` of the biome in the `droplets_of_thirst:biome_water` data map;
   2. biome tags `#droplets_of_thirst:water_purity/5` down to `/0` (checked in that order);
   3. `base` of the dimension type in the `droplets_of_thirst:dimension_water` data map;
   4. `worldWaterBasePurity` in `purity.toml` (default 1).
3. Plus the **altitude** delta, the **still/running** delta and the biome's `delta`; the result is kept between 0 and the biome's `max` (default 5).

Global settings in `purity.toml`, section `world`:

| Key | Default | Meaning |
|---|---|---|
| `altitudeBands` | `["30,59,1", "60,99,2", "100,4096,3", "-47,-16,1", "-79,-48,2", "-4096,-80,3"]` | `"minY,maxY,delta"`, both ends included; the first band containing the water's Y adds its delta. Default, relative to sea level: +1 from 30 to 59 blocks above it, +2 from 60 to 99, +3 from 100 up; +1 from 16 to 47 blocks below it, +2 from 48 to 79, +3 from 80 down |
| `altitudeRelativeToSeaLevel` | `true` | Measure the bands from the dimension's sea level (63 in the Overworld) instead of absolute Y |
| `stillWaterPurificationAmount` | `0` | Added to source water (-5 to 5) |
| `runningWaterPurificationAmount` | `1` | Added to flowing water (0 to 5) |
| `worldWaterBasePurity` | `1` | Base purity when neither biome nor dimension sets one |
| `saltWaterPurity` | `0` | Fixed purity in `#droplets_of_thirst:salt_water` biomes (oceans): sea water is always contaminated (0), which stands in for salt water; -1 = off |

### Cauldrons

A water cauldron stores no purity: whatever went into it (rain, dripstone, a bucket of contaminated or pure water), water taken out of it (buckets, bottles, bowls, Create open pipe ends) is always **murky (2)**, or **clean (4)** while the cauldron stands on a heat source from the block tag `#droplets_of_thirst:cauldron_heat_sources` (blocks with a `lit` property only when lit). A cauldron never gives pure water (5): use a Sand Filter or the purification recipes for that. Jade shows the purity the water would have right now. This applies to any water container another mod lets you fill from a cauldron, as long as it is a registered purity container, and to pumps and pipes of other mods that drain the cauldron through the NeoForge fluid capability.

### Biome tags

- `data/droplets_of_thirst/tags/worldgen/biome/water_purity/0.json` … `/5.json`: standard biome tags. None ship with Droplets of Thirst: the default values come from the `droplets_of_thirst:biome_water` data map below, and biomes it does not list start at `worldWaterBasePurity`.
- `data/droplets_of_thirst/tags/worldgen/biome/salt_water.json`: ships with `#minecraft:is_ocean`; not used when `saltWaterPurity` is -1.

```json
{ "values": ["minecraft:cherry_grove", "#c:is_mountain"] }
```

### `droplets_of_thirst:biome_water`

Biome data map, `data/<namespace>/data_maps/worldgen/biome/biome_water.json`. Droplets of Thirst ships these values, using the standard `c:` biome tags so biomes from other mods are covered too:

| Biomes | Tags | `base` | `max` | Result (still / running) |
|---|---|---|---|---|
| Jungle, dark forest | `#c:is_jungle`, `minecraft:dark_forest` | 1 | 5 | 1 / 2, plus altitude |
| Taiga, birch forests | `#c:is_taiga`, `minecraft:birch_forest`, `minecraft:old_growth_birch_forest` | 2 | 5 | 2 / 3, plus altitude |
| Snowy biomes, frozen river | `#c:is_snowy`, `minecraft:frozen_river` | 2 | 5 | 2 / 3, plus altitude |
| Mountains, windswept hills | `#c:is_mountain`, `#c:is_windswept` | 3 | 5 | 3 / 4, plus altitude |
| Deserts and badlands | `#c:is_desert`, `#c:is_badlands` | 0 | 2 | 0 / 1, never more than 2 |
| Swamps and mangroves | `#c:is_swamp`, `minecraft:mangrove_swamp` | 0 | 1 | 0 / 1, never more than 1 |
| Everything else (plains, forests, rivers) | none | worldWaterBasePurity (1) | 5 | 1 / 2, plus altitude |

Oceans (`#droplets_of_thirst:salt_water`) are fixed at 0 by `saltWaterPurity`.

When a biome is in several tags the entry listed last in the file wins, and it replaces the earlier one completely, so the shipped file lists the jungle, taiga, cold and mountain biomes first, then deserts and badlands, then swamps and mangroves last: a swamp or desert that a mod also tags as mountain gets the swamp or desert values. To change them, ship your own `biome_water.json` in a datapack: entries you add for the same biome or tag replace the shipped ones (a later datapack wins), and `"replace": true` at the top of your file drops all the shipped values. Set `"minecraft:plains": { "base": 2 }` to change one biome.

An example:

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
| `base` | int 0-5, optional | — | Base purity; wins over the tags and the dimension |
| `delta` | int -5 to 5 | 0 | Added after altitude and running water |
| `max` | int 0-5 | 5 | Highest purity water can have here |

### `droplets_of_thirst:dimension_water`

Dimension type data map, `data/<namespace>/data_maps/dimension_type/dimension_water.json`. It is keyed by **dimension type** (`minecraft:overworld`, `minecraft:the_nether`, a mod's type id), so dimensions that share a type share the value.

```json
{ "values": { "minecraft:the_end": { "base": 5 } } }
```

| Field | Type | Meaning |
|---|---|---|
| `base` | int 0-5, optional | Base purity for biomes without their own |
| `thirst_multiplier` | float 0-10, optional | Replaces the climate multiplier of thirst loss in this dimension type (the Nether uses `netherMultiplier` from `gameplay.toml` unless set here) |

Droplets of Thirst sets `minecraft:the_end` to `thirst_multiplier` `0.6`, so the End counts as cold (about a snowy biome); a datapack entry for `minecraft:the_end` replaces it.

Both world data maps are only read on the server.

## Dehydration effect

`droplets_of_thirst:dehydration` is a harmful effect that makes thirst drop faster, like Hunger does for food: 0.005 exhaustion per tick per level, times `effects.dehydrationMultiplier` in `gameplay.toml`. Thirst never goes below zero. Nothing applies it by default. Give it with `/effect give @p droplets_of_thirst:dehydration 30 1`, or add it to a purity effect list in `purity.toml` (`"effect_id,durationTicks,amplifier,chancePercent[,blocksHydration]"`), for example `"droplets_of_thirst:dehydration,600,0,25"` in `effects.dirty` for a 25% chance of 30 s of Dehydration I from dirty water. See [Configuration](Configuration#effects).

## Quenchness effect and potions

`droplets_of_thirst:quenchness` is a beneficial effect: every `effects.quenchnessIntervalTicks` (default 40) it restores (level) thirst and (level) quenched, like Regeneration for health. Vampires (Vampirism, Supernatural) get nothing from it: only blood hydrates them. Potions (registry `minecraft:potion`, usable in loot tables, `set_potion`, recipes and `/give @p minecraft:potion[potion_contents={potion:"droplets_of_thirst:quenchness"}]`):

| Potion | Effect | Brewing |
|---|---|---|
| `droplets_of_thirst:quenchness` | Quenchness I, 0:45 | awkward potion + prismarine crystals |
| `droplets_of_thirst:long_quenchness` | Quenchness I, 1:30 | Quenchness + redstone |
| `droplets_of_thirst:strong_quenchness` | Quenchness II, 0:22 | Quenchness + glowstone dust |

Splash, lingering and tipped arrows work as for vanilla potions. `effects.quenchnessPotion = false` in `gameplay.toml` removes the three brewing recipes (the potions stay registered); to use another ingredient, turn it off and add your own mix with KubeJS or a mod (`RegisterBrewingRecipesEvent`).

## Hydrated effect

`droplets_of_thirst:hydrated` is a beneficial effect that slows thirst loss: all thirst exhaustion is multiplied by `effects.hydratedMultiplier` (default 0.5) once per level, so Hydrated II with the default quarters it. It is part of the cached thirst loss multiplier (`/droplets_of_thirst debug exhaustion` shows it as `hydrated`). Sources: `/effect give @p droplets_of_thirst:hydrated 60 0`, the purity effect lists in `purity.toml` (for example `"droplets_of_thirst:hydrated,600,0,100"` in `effects.purified`), and the optional full-hydration bonus (`[hydration] fullBonus`, off by default). There is no potion for it.

## Overhydrated effect

`droplets_of_thirst:overhydrated` is a harmful effect: 10% slower movement per level (an attribute modifier on `minecraft:generic.movement_speed`, like Slowness). Drinking far past full gives it (`[overhydration]` in `gameplay.toml`, on by default; see [Configuration](Configuration#overhydration)); `/effect` works too. The thirst bar turns greyish blue while it lasts (`Bar Colors.overhydrated` in `client.toml`).

Vanilla effects that change thirst: Nausea drains it (`depletion.nauseaDepletes`), Fire Resistance reduces it (`fireResistancePercent`), Hunger does not (its extra food exhaustion is left out in `MIRROR_FOOD` mode), and, with `effects.waterBreathingReducesThirst` (off), Water Breathing or Conduit Power reduce it while fully underwater (`effects.underwaterBreathingMultiplier`, 0.5).

## Thirst drain attribute: `droplets_of_thirst:thirst_drain`

Every player has the attribute `droplets_of_thirst:thirst_drain` (base 1.0, 0 to 10). Thirst loss is multiplied by it, so anything that can carry attribute modifiers can change thirst without code:

- items: the vanilla `minecraft:attribute_modifiers` component (`/give`, loot tables, other mods);
- enchantments (datapack): the `minecraft:attributes` effect component;
- effects, Curios, other mods: ordinary `AttributeModifier`s;
- commands: `/attribute @s droplets_of_thirst:thirst_drain base set 0.5`.

```json
"minecraft:attribute_modifiers": {
  "modifiers": [{ "type": "droplets_of_thirst:thirst_drain", "id": "examplemod:cooling_helmet", "amount": -0.25,
                  "operation": "add_multiplied_base", "slot": "head" }]
}
```


## Item, fluid, block and effect tags

| Tag | Registry | Default | Meaning |
|---|---|---|---|
| `droplets_of_thirst:purity_containers` | item | Create builder's tea, Collector's Reap teas | Drinks that carry a water purity: filled with purity by machines (Create spouts), show it in the tooltip and roll its effects. They are not filled from the world |
| `droplets_of_thirst:no_thirst` | item | empty | Never restores thirst, whatever the config, datapacks or other mods say |
| `droplets_of_thirst:salty` | item | empty | Items with no other values get the `items.toml` `salty` penalties (they make the player thirstier), eaten or drunk |
| `droplets_of_thirst:purity_opt_out` | item | empty | Never gets a purity: not filled with purity, no purity tooltip, no purity effects, and its fluid is not given one. Use it for other mods' water containers that break when water items carry extra data |
| `droplets_of_thirst:carries_purity` | fluid | `#minecraft:water`, Create tea | Fluids made in a Create basin from water keep the water's purity |
| `droplets_of_thirst:cauldron_heat_sources` | block | `#minecraft:campfires`, `#minecraft:fire`, magma block, lava | Heat sources under a water cauldron: its water comes out clean (4) instead of murky (2). Blocks with a `lit` property (campfires, furnaces) only count while lit |
| `droplets_of_thirst:rejects_dirty_water` | block | Brewery wooden, copper and netherite brewing stations | Blocks filled with water by clicking that boil it, like kettles: clicking them with water below `compat.toml` `delight.kettleMinPurity` (default 2) does nothing and tells the player the water is too dirty |
| `droplets_of_thirst:pauses_thirst` | mob_effect | Farmer's Delight Nourishment, Let's Do Bakery Stuffed, Let's Do Brewery Saturated | While the player has one of these effects, thirst exhaustion stops building up; drinking and regeneration still work |
| `droplets_of_thirst:stops_thirst` | mob_effect | Corail Tombstone Ghostly Shape | While the player has one of these effects, thirst does not tick at all: no exhaustion, no Dehydration damage, no regeneration cost |

Files: `data/droplets_of_thirst/tags/item/purity_containers.json`, `data/droplets_of_thirst/tags/item/no_thirst.json`, `data/droplets_of_thirst/tags/item/purity_opt_out.json`, `data/droplets_of_thirst/tags/fluid/carries_purity.json`, `data/droplets_of_thirst/tags/block/cauldron_heat_sources.json`, `data/droplets_of_thirst/tags/block/rejects_dirty_water.json`, `data/droplets_of_thirst/tags/mob_effect/pauses_thirst.json`, `data/droplets_of_thirst/tags/mob_effect/stops_thirst.json`. Use `{"id": "othermod:item", "required": false}` for optional mods. Effects are read when the player's effects change and once a second, not every tick.

## Purification recipes

Purifying water uses vanilla recipe types (`minecraft:smelting`, `minecraft:campfire_cooking`, `minecraft:smoking`) with NeoForge component ingredients: the ingredient matches water of a given purity and the result stores a higher one. JEI and other recipe viewers show them like any other recipe.

JEI (and EMI when JEI is installed too) also gets a "Water Purification" page for what is not a recipe: the water cauldron, plain and over a block of `droplets_of_thirst:cauldron_heat_sources`, and with Create the Sand Filter, one step per purity with the `compat.toml` amount and maximum (up to 5, the only way to reach pure water with Create). The page is hidden when `purity.enabled` is `false`. A "Hydration" page lists every item that changes thirst with the values the server resolved, so it shows your datapack and config changes after `/reload`.

The recipes ship as optional built-in datapacks, one per method, listed in the datapack screen when creating a world and in `/datapack list`:

| Pack id | Default | Recipes |
|---|---|---|
| `mod/droplets_of_thirst:datapacks/purify_smelting` | enabled | Furnace: +2 levels per cook, up to clean (4) with Create and up to pure (5) without it |
| `mod/droplets_of_thirst:datapacks/purify_campfire` | enabled | Campfire: +1 level per cook, up to clean (4) with Create and up to pure (5) without it |
| `mod/droplets_of_thirst:datapacks/purify_smoking` | disabled | Smoker: same as the furnace (+2 levels), twice as fast |
| `mod/droplets_of_thirst:datapacks/purify_cooking_pot` | enabled | Farmer's Delight cooking pot (and Miner's Delight copper pot): +1 level per cook, with the campfire caps, for bottles, buckets, terracotta bowls and, with Cold Sweat, waterskins. Loads only with Farmer's Delight |
| `mod/droplets_of_thirst:datapacks/clean_water_cooking` | enabled | Recipes of Farmer's Delight addons that use water (Extra Delight, Brewin' and Chewin', Let's Do Farm & Charm, Fruits Delight's Create recipes, Cultural Delights' aging vat, Expanded Delight, Corn Delight and Extra Delight's Create recipes) accept only water of a minimum purity: 2 (murky or better) for recipes that boil the water, 3 (acceptable or better) for cold ones. Contaminated and dirty water are never accepted |

- Turn a method off or on per world with `/datapack disable "mod/droplets_of_thirst:datapacks/purify_campfire"` / `/datapack enable ...`; the choice is saved with the world.
- To change a recipe, put a recipe with the same id (`droplets_of_thirst:water_bottle_from_smelting_to_murky`, …) in your own datapack above it.
- `clean_water_cooking` overwrites the addon recipes under their own ids (`extradelight:vat/kimchi_item`, `farm_and_charm:pot_cooking/nettle_tea`, …), each one loaded only with its mod. Put a recipe with the same id in a pack above it to change one, or turn the whole pack off with `/datapack disable "mod/droplets_of_thirst:datapacks/clean_water_cooking"`. With purity turned off the water stores no purity, so those recipes accept any water as the originals do.
- The `purify_cooking_pot` recipes end in `_manual_only`, so Slice & Dice does not turn them into Create basin recipes; without Create they reach pure (5), with Create they stop at clean (4), as the other packs do.
- Recipe ids follow `<item>_from_<method>_to_<level>`, for example `water_bottle_from_smelting_to_murky`. Where the recipe changes with Create the id ends in `_with_create` (result capped at 4, loads only with Create) or `_without_create` (loads only without Create, reaches 5), as in `water_bottle_from_smelting_to_clean_with_create`. The `pure` recipes exist only without Create and carry no suffix. Water with no purity stored counts as acceptable (3) and is matched by the recipes that take level 3.
- **With Create installed** the recipes that give pure water (5) do not load, and the furnace and smoker stop at clean (4). Pure water then only comes from the Sand Filter, so the filters are worth building. Without Create, cooking reaches pure as in the table. The recipes that only exist without Create carry `{"type": "neoforge:not", "value": {"type": "neoforge:mod_loaded", "modid": "create"}}`.
- **Create** recipes (in the mod's own data, loaded only with Create and with purity on): fan washing (`create:splashing`) takes water bottles and terracotta water bowls up one level, contaminated → dirty → murky → acceptable → clean (`droplets_of_thirst:compat/create/water_bottle_from_splashing_to_dirty`, …); a mixer over a heated basin (`create:mixing`, `heat_requirement: heated`) turns 250 mB of water up one level, up to clean (`droplets_of_thirst:compat/create/water_from_heated_mixing_to_dirty`, …); compressed cactus gives acceptable (3) water; an Item Drain empties terracotta water bowls (`create:emptying`). Water buckets are not washed: Create gives back the bucket's crafting remainder next to the result. Smoking and blasting fans use the smoker and furnace recipes above; with the smoker pack on, a blasting fan burns what a smoker can cook, water included, as Create does with food.
- **Cold Sweat**: each pack also purifies the filled waterskin (`droplets_of_thirst:filled_waterskin_from_smelting_to_murky`, …), loaded only with Cold Sweat. The waterskin comes out full, with Cold Sweat's default water temperature, since cooking recipes give a fixed result; water is free to refill anyway. There is no recipe for a waterskin with no purity stored, because a component ingredient cannot say "no purity" for an item that always carries other data; waterskins filled with Droplets of Thirst installed always get one.
- Every purification recipe carries the condition `{"type": "droplets_of_thirst:purity_enabled"}`, so none load when `purity.enabled` is `false`. Use it in your own purity recipes too. A recipe that should still exist without purity needs a second copy with `{"type": "neoforge:not", "value": {"type": "droplets_of_thirst:purity_enabled"}}`, as Create's cactus compacting does (`droplets_of_thirst:compat/create/cactus_without_purity`).
- The same id is also a loot condition, checked each time loot is rolled: put `"conditions": [{"condition": "droplets_of_thirst:purity_enabled"}]` on a `minecraft:set_components` function that stores `droplets_of_thirst:purity`, as the built-in chest loot does, so the drink comes without a purity when it is off.

Example of one level of purification for a modded water container:

```json
{
  "neoforge:conditions": [{ "type": "droplets_of_thirst:purity_enabled" }],
  "type": "minecraft:campfire_cooking",
  "ingredient": {
    "type": "neoforge:components",
    "items": "examplemod:canteen",
    "components": { "droplets_of_thirst:purity": 1 }
  },
  "result": { "id": "examplemod:canteen", "count": 1, "components": { "droplets_of_thirst:purity": 2 } },
  "cookingtime": 300
}
```

Water with no purity stored counts as `defaultPurity` (3, acceptable) in the game but does not match a `"droplets_of_thirst:purity": 3` ingredient; match it with `{"type": "neoforge:components", "items": "...", "components": {}, "strict": true}` as the built-in recipes do.

## Presets

Two optional presets change the balance; the default is Droplets of Thirst's own behaviour. Each has two parts, applied by hand (nothing rewrites your config):

| Part | casual | hardcore |
|---|---|---|
| Built-in datapack (disabled by default; enable when creating the world or with `/datapack enable`) | `mod/droplets_of_thirst:datapacks/preset_casual`: Nether climate ×1.5 | `mod/droplets_of_thirst:datapacks/preset_hardcore`: Nether climate ×4 |
| TOML keys to copy into `config/droplets_of_thirst/` ([`examples/presets/`](../examples/presets/) in the repository) | `multiplier` 0.8, riding ×0.5, dehydration stops at 5 hearts on Normal and never kills, drinking by hand on, no poison: contaminated water 50% nausea and hunger, dirty 25%, murky 10% | `multiplier` 1.6, weather/day/sun/water multipliers, riding ×1, rain every 2 s, drinking by hand with a 1 s cooldown, no running water bonus, world water starts contaminated, contaminated water: 60% poison |

The TOML files only list the keys they change; Droplets of Thirst adds the rest with their defaults.
