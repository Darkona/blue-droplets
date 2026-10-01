# Minecraft 1.19.2

Droplets of Thirst for Minecraft 1.19.2 runs on **Forge 43.5.2 or newer** (branch `1.19.2`, jar `droplets-of-thirst-1.19.2-1.0.0.jar`). Thirst, water purity, effects, config files, data maps and commands work as the rest of this wiki describes for 26.3.

It works like the [1.20.1 version](Minecraft-1.20.1): purity is NBT, recipes and loot use the Forge formats, and it has most integrations of [1.21.1](Minecraft-1.21.1). This page lists what is different from 1.20.1.

---

## Players

- **Dehydration damage** is a plain damage source (`dehydrate`), with the same rules as starving: it goes through armor, enchantments and Resistance. Minecraft 1.19.2 has no damage types, so datapacks cannot change it.
- **Creative tab**: the Sand Filter is only in the Droplets of Thirst tab. Create's tab does not list it.
- **Optional datapacks**: the presets and the smoker purification pack stay off until you turn them on, when creating the world or with `/datapack enable`. The others are on by default, also in existing worlds.
- **Thirst Was Taken worlds** (1.19.2, version 1.3.x) load with the players' thirst and the purity of their water, as on 1.20.1.

---

## Configuration

Same keys as on 1.20.1 and 1.21.1. `delight.kettleMinPurity` stays, although no kettle mod exists for 1.19.2. It applies to blocks that a modpack adds to `droplets_of_thirst:rejects_dirty_water`.

---

## Modpack makers

Everything the [1.20.1 page](Minecraft-1.20.1#modpack-makers) says about data holds here: purity in NBT, the `/give` syntax, the data maps, the Forge biome tags (`#forge:is_desert`, `#forge:is_swamp`, `#forge:is_snowy`, `#forge:is_mountain`, and the windswept biomes by id), recipes with `forge:partial_nbt`, the loot entries and conditions, the brewing recipe and poured water in the world's saved data. The differences:

- **Pack format**: `"pack_format": 10`.
- **Dehydration damage** has no damage type to tag.

---

## Mod compatibility

Tested with Create 0.5.1.i, Jade 8.9.2, JEI 11.39, AppleSkin 2.4.2, Cold Sweat 2.4.3, Serene Seasons 8.1.0.24, Traveler's Backpack 8.2.41, Reliquary 2.0.40 and Farmer's Delight 1.2.4, all for 1.19.2, and Vampirism 1.9.5.

- **Create 0.5.1**: the Sand Filter, its Ponder scene, fan and basin purification, spouts, drains, pipes and goggles work as with Create 6.
- **Farmer's Delight addons**, as data (drink values and, where a recipe uses water, the clean water rule): Brewin' and Chewin' 1.19-2.0, Ocean's Delight 1.0.2, Ender's Delight 1.2.2, Miner's Delight 1.1.1, Fruits Delight 0.5.9, Cultural Delights 0.16.0, Corn Delight 1.0.3, Rustic Delight 1.3.0, Crabber's Delight 1.1.4, End's Delight 2.1 and My Nether's Delight 1.7.6. Farmer's Delight's cooking pot and wheat dough, Miner's Delight's copper pot, Cultural Delights' corn dough and Corn Delight's raw tortilla need acceptable water or better (3). Cultural Delights' bean milk, which is cooked, needs murky or better (2).
- **Miner's Delight**'s water cup carries a purity like a bottle.
- **Serene Seasons** 8.1.0.24 reports its version as `0.0NONE`, so the mod accepts any version.
- **Vampirism** vampires work as on 1.21.1: their thirst goes down, only blood quenches it, and every drink of blood counts, with the same `compat.toml` `vampirism` keys. Vampirism 1.9.5 has no event for drinking blood, so Droplets of Thirst reads it with a small hook of its own. The item tag `droplets_of_thirst:blood` is empty here: add items to it, with drinks data map values, to let vampires drink them.

Not on 1.19.2:

- **Supernatural**: its 1.19.2 version works differently inside, and Droplets of Thirst cannot tell who is a vampire. Its vampires drink like other players, and the thirst bar keeps its colour.
- **Farm & Charm, HerbalBrews, Brewery, Extra Delight and Expanded Delight** have no 1.19.2 Forge version, so there are no taps, sinks, wells or kettles.
- **Miner's Delight with Create**: Miner's Delight 1.1.1 ships no Create recipes for its cup, so spouts and item drains do not fill or empty it.
- **Brewin' and Chewin' kegs, Cultural Delights' aging and Fruits Delight's Create mixing** take water of any purity.
- **KubeJS**: no examples. The Droplets of Thirst API works from any mod.

---

## Mod developers

- The API is the same as on 1.20.1: Forge events on `MinecraftForge.EVENT_BUS`, Forge's `FluidStack`, `ResourceLocation`.
- The thirst bar is the GUI overlay `droplets_of_thirst:thirst_level`, above the food bar, drawn with a `PoseStack`. Cancel it with `RenderGuiOverlayEvent.Pre`.
- Network: one `SimpleChannel`, `droplets_of_thirst:main`.
- Declare the dependency in `mods.toml` with `mandatory = false`. The Maven version is `1.19.2-1.0.0`.
