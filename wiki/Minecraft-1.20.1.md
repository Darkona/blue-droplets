# Minecraft 1.20.1

Droplets of Thirst for Minecraft 1.20.1 is one jar, `droplets-of-thirst-1.20.1-1.0.0.jar` (branch `1.20.1`), that runs on **Forge 47.1.3 or newer** and on **NeoForge 47.1**. Thirst, water purity, effects, config files, data maps and commands work as the rest of this wiki describes for 26.3.

It has most integrations of the 1.21.1 version, which 26.3 does not have: Create, Farmer's Delight and its addons, Cold Sweat, Vampirism, Supernatural and Reliquary. [Minecraft 1.21.1](Minecraft-1.21.1) tells what they do and lists their config keys. This page lists what is different from 26.3 and from 1.21.1.

---

## Players

- **As on 1.21.1**: with 3 droplets or less you cannot start a sprint, and a sprint in progress goes on. The Nether multiplier applies in ultra-warm dimensions. With Create installed, cooking stops at clean (4) and only the Sand Filter makes pure water.
- **Thirst Was Taken worlds** (1.20.1) load with the players' thirst and the purity of their water. Thirst Was Taken's four levels move to the six of Droplets of Thirst: dirty to contaminated, slightly dirty to dirty, acceptable to acceptable, purified to pure.
- **Other thirst mods**: Thirst Was Taken or Thirst Was Reclaimed next to Droplets of Thirst stops the game at the loading screen. Tough As Nails, Legendary Survival Overhaul, Homeostatic and Survive only show a warning on the loading screen and in the log, and both thirst systems run.
- **Dispensers**: a vanilla bucket that a full dispenser drops after filling comes out with no purity. Buckets that go back into the dispenser keep it.

---

## Configuration

The config files have the same keys as on 1.21.1, including the `compat.toml` sections `create`, `coldsweat`, `delight`, `reliquary` and `vampirism`, `hotDirtyWater.useColdSweat`, `hotDirtyWater.coldSweatMinBodyTemp` and `Bar Colors.vampire` (see [Minecraft 1.21.1](Minecraft-1.21.1#configuration)). Two keys behave differently:

- The mod checks `gameplay.toml` `loot.enabled` on every loot roll, so a change applies at once, without `/reload`.
- One Forge brewing recipe reads `gameplay.toml` `effects.quenchnessPotion` each time a potion brews.

---

## Modpack makers

### Purity is NBT

Purity on items and fluids is the NBT key `droplets_of_thirst:purity` (0-5), the same name as the component of later versions:

```
/give @s minecraft:potion{Potion:"minecraft:water","droplets_of_thirst:purity":5}
```

The player's thirst is a capability (`droplets_of_thirst:player_thirst`), kept on death and on the way back from the End.

### Data

- **Pack format**: `"pack_format": 15`. Folders are plural, as in vanilla 1.20.1: `recipes/`, `loot_tables/`, `tags/items/`, `tags/blocks/`, `tags/fluids/`. Effect tags stay in `tags/mob_effect/`.
- **Data maps**: `droplets_of_thirst:drinks`, `hydrating_blocks`, `biome_water` and `dimension_water` read the same files as on 26.3 (`data/<namespace>/data_maps/...`), with the same fields, `replace`, `remove` and tags. Droplets of Thirst reads them itself. Conditions on an entry can be `neoforge:conditions` or `forge:conditions`. Both use the Forge condition types (`mod_loaded`, `not`, `and`, `or`, `item_exists`, `tag_empty`, `true`, `false`).
- **Biome tags**: the default `biome_water` uses the 1.20.1 tags `#forge:is_desert`, `#forge:is_swamp`, `#forge:is_snowy`, `#forge:is_mountain` and the vanilla `#minecraft:is_jungle`, `#minecraft:is_taiga`, `#minecraft:is_badlands`, `#minecraft:is_mountain`. There is no windswept tag, so it lists the four windswept biomes by id. The purity of every vanilla biome is the same as on 26.3.
- **Recipes**: a purity ingredient is `{"type": "forge:partial_nbt", "item": "minecraft:potion", "nbt": {"Potion": "minecraft:water", "droplets_of_thirst:purity": 3}}`. Water with no purity stored is `{"type": "forge:nbt", "item": "minecraft:potion", "nbt": {"Potion": "minecraft:water"}}` (for a bucket or a bowl, `forge:nbt` without `nbt`). Results take `"nbt"`, and recipe conditions go in `"conditions"` with `forge:` types, for example `{"type": "droplets_of_thirst:purity_enabled"}` or `{"type": "forge:not", "value": {"type": "forge:mod_loaded", "modid": "create"}}`.

  ```json
  {
    "conditions": [{ "type": "droplets_of_thirst:purity_enabled" }],
    "type": "minecraft:smelting",
    "ingredient": { "type": "forge:partial_nbt", "item": "minecraft:potion", "nbt": { "Potion": "minecraft:water", "droplets_of_thirst:purity": 0 } },
    "result": { "item": "minecraft:potion", "count": 1, "nbt": { "Potion": "minecraft:water", "droplets_of_thirst:purity": 2 } },
    "cookingtime": 200
  }
  ```

- **Create fluid ingredients** match NBT as "at least these keys": `{"fluid": "minecraft:water", "amount": 250, "nbt": {"droplets_of_thirst:purity": 3}}`. They cannot ask for water without a purity, so Create's heated mixing does not purify water that has none. Water that Create's pumps, pipes and hose pulleys draw always has one.
- **Brewing**: one Forge brewing recipe in code brews the Potion of Quenchness. It is not data. Splash and lingering potions work as in vanilla.
- **Chest loot**: the loot modifiers are `droplets_of_thirst:add_table`, listed in `data/forge/loot_modifiers/global_loot_modifiers.json`, and carry the loot condition `droplets_of_thirst:loot_config`, checked on every roll. Drinks of optional mods use the loot entry type `droplets_of_thirst:optional_item`, which gives nothing when the item does not exist, because Forge 1.20.1 cannot skip loot tables by condition. The purity of a loot bottle is a `minecraft:set_nbt` function: `{"function": "minecraft:set_nbt", "tag": "{\"droplets_of_thirst:purity\":3}", "conditions": [{"condition": "droplets_of_thirst:purity_enabled"}]}`.
- **Attributes**: items carry `droplets_of_thirst:thirst_drain` modifiers in their `AttributeModifiers` NBT. Enchantments are not data in 1.20.1, so a datapack cannot give them the attribute.
- **Poured water** is remembered per dimension in the world's saved data (`data/droplets_of_thirst_poured_water.dat`), with the same rules.
- **Cauldrons**: pumps and pipes of other mods cannot drain a water cauldron (Forge 1.20.1 gives cauldrons no fluid handler). Buckets, bottles, bowls and Create open pipe ends take water from cauldrons with its purity.
- The built-in datapacks have the same ids as on 1.21.1: `mod/droplets_of_thirst:datapacks/<name>`.

---

## Mod compatibility

Tested with Create 6.0.8, Jade 11.13.3, JEI 15.56, AppleSkin 2.5.1, Cold Sweat 2.4.3.2, Serene Seasons 9.1.0.3, Traveler's Backpack 9.1.57, Reliquary 2.0.65, Farmer's Delight 1.3.4, Supernatural 2.12.1 and Vampirism 1.10.17, all for 1.20.1. JEI must be 15.56 or newer when installed. Some of these mods need a newer Forge than 47.1 on their own (Serene Seasons and GlitchCore ask for 47.3), so they do not run on NeoForge 47.1.

- **Farmer's Delight addons**: Brewin' and Chewin' 3.2.1, Ocean's Delight 1.0.2, Ender's Delight 1.1.4, Miner's Delight 1.4.5, Farm & Charm 1.0.14, HerbalBrews 1.0.12 and Brewery 2.0.6 work as on 1.21.1. Fruits Delight, Cultural Delights, Corn Delight, Rustic Delight, Crabber's Delight, End's Delight and My Nether's Delight get drink values and, where a crafting recipe uses water (Cultural Delights' corn dough, Corn Delight's raw tortilla), the clean water rule.
- **Miner's Delight**'s mod id on 1.20.1 is `miners_delight`: its water cup is `miners_delight:water_cup`. Create spouts and drains fill and empty it through Miner's Delight's own recipes.
- **Brewin' and Chewin' kegs** do not take water that carries a purity, and the clean water rule does not apply to them. Water with no purity stored works as before.
- **Create mixing** recipes of Fruits Delight (jams, juices, teas) and Rustic Delight's coffee accept water of any purity.
- **Vampires** work as on 1.21.1: their thirst goes down and only blood quenches it. Vampirism 1.10.17 counts every drink of blood, as on 1.21.1. Supernatural 2.12.1 has no blood tag, so `droplets_of_thirst:blood` lists its blood bottle directly (thirst 6, quenched 6). The red droplets follow Supernatural's own vampire check.

Not on 1.20.1:

- **KubeJS**: 1.20.1's KubeJS is a different API from the one of the examples. The Droplets of Thirst API itself works from any mod.
- **Extra Delight** and **Expanded Delight** have no 1.20.1 Forge version.
- **Farm & Charm's timber well** is not in the 1.20.1 version of Farm & Charm.

---

## Mod developers

- The API has the same classes and methods, with Forge types: events are `@Cancelable` Forge events on `MinecraftForge.EVENT_BUS`, `FluidStack` is `net.minecraftforge.fluids.FluidStack`, names use `ResourceLocation`, and `DropletsAPI.registerWaveEffect` takes a `MobEffect`.
- Declare the dependency in `mods.toml` with `mandatory = false` (FML 47 has no `type`).
- The thirst bar is the GUI overlay `droplets_of_thirst:thirst_level`, right above `food_level`. Cancel it with `RenderGuiOverlayEvent.Pre`.
- The Maven version is `1.20.1-1.0.0`.
