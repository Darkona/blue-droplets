# Minecraft 1.18.2

Blue Droplets for Minecraft 1.18.2 runs on **Forge 40.2.3 or newer** (branch `1.18.2`, jar `blue-droplets-beta-1.18.2-1.0.0.jar`). 40.2.3 is the first Forge 40 that applies the mixins of the bundled MixinExtras. Thirst, water purity, effects, config files, data maps and commands work as the rest of this wiki describes for 26.3.

It works like the [1.19.2 version](Minecraft-1.19.2), which works like [1.20.1](Minecraft-1.20.1): purity is NBT, recipes and loot use the Forge formats, and it has most integrations of [1.21.1](Minecraft-1.21.1). This page lists what is different from 1.19.2.

---

## Players

- Everything on the [1.19.2 page](Minecraft-1.19.2#players) applies: dehydration damage is a plain damage source, the Sand Filter is only in the Blue Droplets tab, and the opt-in datapacks stay off until turned on.
- **Thirst Was Taken worlds** (1.18.2, version 1.3.x) load with the players' thirst and the purity of their water.
- There are no mangrove swamps in 1.18.2.

---

## Configuration

Same keys as on 1.19.2, 1.20.1 and 1.21.1.

---

## Modpack makers

The data formats are those of [1.19.2](Minecraft-1.19.2#modpack-makers) and [1.20.1](Minecraft-1.20.1#modpack-makers). The differences:

- **Pack format**: `"pack_format": 8`.
- **Biome tags**: the default `biome_water` uses the tags Forge 40 has. Deserts are `#forge:is_sandy` (with `#minecraft:is_badlands` for badlands), swamps `#forge:is_swamp`, snowy biomes `#forge:is_snowy` and mountains `#minecraft:is_mountain`, which on 1.18.2 holds Forge's peaks and slopes. Forge 40 has no `forge:is_desert` or `forge:is_mountain`. The windswept biomes are listed by id. Every vanilla biome gets the same purity as on 26.3.

---

## Mod compatibility

Tested with Create 0.5.1.i, Jade 5.3.2, JEI 10.2.1, AppleSkin 2.5.1, Cold Sweat 2.4.3, Serene Seasons 7.0.0.15, Traveler's Backpack 7.1.49, Reliquary 2.0.19 and Farmer's Delight 1.2.3, all for 1.18.2, and Vampirism 1.8.8.

- **Create 0.5.1**: the Sand Filter, its Ponder scene, fan and basin purification, spouts, drains, pipes and goggles work as on 1.19.2.
- **Jade 5** still uses the Waila API: the purity lines are the same, with their switch in Jade's plugin settings.
- **JEI**: Blue Droplets is built against the JEI 9.7 API and runs on JEI 9.7.2 and 10.2.1. The hydration and purification pages are the same.
- **Farmer's Delight addons**, as data (drink values and, where a recipe uses water, the clean water rule): Brewin' and Chewin' 1.0.1, Ocean's Delight 1.0.0, Ender's Delight 1.2.1, Miner's Delight 1.1.1, Corn Delight 1.0.6, Crabber's Delight 1.1.2 and End's Delight 1.2.1. Farmer's Delight's cooking pot and wheat dough, Miner's Delight's copper pot and Corn Delight's raw tortilla need acceptable water or better (3). Miner's Delight's water cup carries a purity like a bottle.
- **Serene Seasons** 7.0.0.15 reports its version as `0.0NONE`, so any version is accepted.
- **Vampirism** vampires work as on 1.21.1: their thirst goes down, only blood quenches it, and every drink of blood counts, with the same `compat.toml` `vampirism` keys. Vampirism 1.8.8 has no event for drinking blood, so Blue Droplets reads it with a small hook of its own. The item tag `blue_droplets:blood` is empty here: add items to it, with drinks data map values, to let vampires drink them.

Not on 1.18.2, besides what 1.19.2 lacks (Supernatural vampires, Farm & Charm, HerbalBrews, Brewery, Extra Delight, Expanded Delight, KubeJS):

- **Fruits Delight, Cultural Delights, Rustic Delight and My Nether's Delight** have no 1.18.2 Forge version.
- **Miner's Delight with Create**: its 1.18.2 version ships no Create recipes for the cup.
- **Brewin' and Chewin' kegs** take water of any purity.

---

## Mod developers

- The API is the same as on 1.19.2, with Forge 40 names. The events are Forge 40 `PlayerEvent`s: `getPlayer()` gives the player.
- The thirst bar is the overlay "Blue Droplets Thirst" of Forge's `OverlayRegistry`, right above the food bar. Hide it with `OverlayRegistry.enableOverlay` or cancel it in `RenderGameOverlayEvent.PreLayer`.
- The loot modifier `blue_droplets:add_table` has a JSON serializer (Forge 40 has no codecs for loot modifiers). Its files are the same as on 1.20.1.
- Declare the dependency in `mods.toml` with `mandatory = false`. The Maven version is `beta-1.18.2-1.0.0`.
