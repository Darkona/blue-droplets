# Minecraft 1.20.1

Droplets of Thirst for Minecraft 1.20.1 is one jar that runs on **Forge 47.1.3 or newer** and on **NeoForge 47.1**. It has the same features, config files and commands as the 1.21.1 version; this page lists what is different. Everything else in this wiki applies as written.

## Players

- Worlds from Thirst Was Taken for 1.20.1 load with the players' thirst and the purity of their water. Thirst Was Taken's four purity levels move to the six of Droplets of Thirst: dirty to contaminated, slightly dirty to dirty, acceptable to acceptable, purified to pure.
- Thirst Was Taken or Thirst Was Reclaimed installed next to Droplets of Thirst stops the game at the loading screen. Tough As Nails, Legendary Survival Overhaul, Homeostatic and Survive only show a warning: both thirst systems run.

## Modpack makers

- Purity on items and fluids is the NBT key `droplets_of_thirst:purity` (0-5). For example `/give @s minecraft:potion{Potion:"minecraft:water","droplets_of_thirst:purity":5}` or, in a loot table, the `minecraft:set_nbt` function with `"tag": "{\"droplets_of_thirst:purity\":5}"`.
- The data maps `droplets_of_thirst:drinks`, `hydrating_blocks`, `biome_water` and `dimension_water` read the same files as on 1.21.1 (`data/<namespace>/data_maps/...`), with the same fields. Conditions on an entry can be `neoforge:conditions` or `forge:conditions`; both use the Forge condition types (`mod_loaded`, `not`, `and`, `or`, `item_exists`, `tag_empty`, `true`, `false`).
- Biome tags are the 1.20.1 ones: `#forge:is_desert`, `#forge:is_swamp`, `#forge:is_snowy`, `#forge:is_mountain` and the vanilla `#minecraft:is_jungle`, `#minecraft:is_taiga`, `#minecraft:is_badlands`, `#minecraft:is_mountain`. There is no windswept tag: the built-in `biome_water` lists the four windswept biomes by id.
- Recipes use the 1.20.1 formats. A purity ingredient is `{"type": "forge:partial_nbt", "item": "minecraft:potion", "nbt": {"Potion": "minecraft:water", "droplets_of_thirst:purity": 3}}`; water with no purity stored is `{"type": "forge:nbt", "item": "minecraft:potion", "nbt": {"Potion": "minecraft:water"}}` (for a bucket or a bowl, `forge:nbt` without `nbt`). Results take `"nbt"`, and recipe conditions go in `"conditions"` with `forge:` types, for example `{"type": "droplets_of_thirst:purity_enabled"}` or `{"type": "forge:not", "value": {"type": "forge:mod_loaded", "modid": "create"}}`.
- Create's fluid ingredients match NBT as "at least these keys": `{"fluid": "minecraft:water", "amount": 250, "nbt": {"droplets_of_thirst:purity": 3}}`. They cannot ask for water without a purity, so Create's heated mixing does not purify water that has none; water drawn by Create's pumps, pipes and hose pulleys always has one.
- Chest loot: loot tables cannot be skipped by condition on 1.20.1. Droplets of Thirst adds its chest loot with the loot modifier `droplets_of_thirst:add_table` and the loot condition `droplets_of_thirst:loot_config` (`loot.enabled`, checked on every roll), and writes drinks of optional mods with the entry type `droplets_of_thirst:optional_item`, which gives nothing when the item does not exist.
- Poured water is remembered per dimension in the world's saved data (`data/droplets_of_thirst_poured_water.dat`).
- Pumps and pipes of other mods cannot drain a water cauldron on Forge 1.20.1 (cauldrons have no fluid handler there), so there is no cauldron purity for them to carry.
- The optional built-in datapacks (`purify_smelting`, `purify_campfire`, `purify_smoking`, `purify_cooking_pot`, `clean_water_cooking`, the presets) have the same ids: `mod/droplets_of_thirst:datapacks/<name>`.

## Mod compatibility

- Tested with Create 6.0.8, Jade 11.13.3, JEI 15.56, AppleSkin 2.5.1, Cold Sweat 2.4.3.2, Serene Seasons 9.1.0.3, Traveler's Backpack 9.1.57, Reliquary 2.0.65, Farmer's Delight 1.3.4 and the Farmer's Delight addons in the changelog, all for 1.20.1. Some of these mods need a newer Forge than 47.1 on their own (Serene Seasons asks for 47.3), so they do not run on NeoForge 47.1.
- Not on 1.20.1: KubeJS (1.20.1's KubeJS is a different API; use the Java API), Extra Delight (no 1.20.1 version) and Farm & Charm's timber well (not in the 1.20.1 version of Farm & Charm).
- Expanded Delight has no version for Minecraft 1.20.1 Forge, so it is not covered.
- Brewin' and Chewin' kegs on 1.20.1: their fluid ingredients cannot leave out low purities, so the clean water rule does not apply to keg recipes, and water that carries a purity does not ferment in the keg.
- Miner's Delight's mod id on 1.20.1 is `miners_delight`: its water cup is `miners_delight:water_cup`.

## Mod developers

- The API has the same classes and methods, with Forge types: events are Forge events on `MinecraftForge.EVENT_BUS` (the cancelable ones carry `@Cancelable`), `FluidStack` is `net.minecraftforge.fluids.FluidStack`, and `DropletsAPI.registerWaveEffect` takes a `MobEffect`.
- Declare the dependency in `mods.toml` with `mandatory = false` (FML 47 has no `type`).
- The thirst bar is the GUI overlay `droplets_of_thirst:thirst_level`, right above `food_level`; cancel it with `RenderGuiOverlayEvent.Pre`.
