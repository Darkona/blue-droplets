# Minecraft 1.21.1

Blue Droplets for Minecraft 1.21.1 runs on **NeoForge 21.1.219 or newer** (branch `1.21.1-neoforge`, jar `blue-droplets-beta-1.21.1-1.0.0.jar`). Thirst, water purity, effects, config files, data maps, commands and the API work as the rest of this wiki describes for 26.3.

1.21.1 is the version with the most integrations: Create, Farmer's Delight and its addons, Cold Sweat, Vampirism, Supernatural, Reliquary and KubeJS, besides the Jade, JEI, AppleSkin, Serene Seasons and Traveler's Backpack support that 26.3 has. Most of those mods have no 26.x version yet. This page describes those integrations and the other differences from 26.3.

---

## Players

- **Sprinting**: with 3 droplets or less you cannot start a sprint. A sprint already in progress goes on.
- **The Nether**: the Nether multiplier and the hot climate of dirty water apply in dimensions whose type is ultra-warm, as the Nether.
- **Thirst Was Taken worlds** load with the players' thirst and the purity of their water. The four purity levels of Thirst Was Taken move to the six of Blue Droplets. Cauldrons do not keep their purity, since cauldrons no longer store one.
- **With Create installed**, cooking stops at clean (4), and pure water (5) only comes from the Sand Filter. Without Create, cooking reaches pure as on 26.3.

### Create

<!-- SCREENSHOT: a Create setup with two Sand Filters in a row between a pump and a tank, goggles tooltip showing the purity of each tank -->

- **Sand Filter**: a block that purifies water pumped through it, `sandFilterFiltrationAmount` levels per filter (1), up to `sandFilterMaxPurity` (5). Dirty water goes in on one side and purified water comes out on the other (down when placed, and a wrench turns it). Filters in a row facing the same way pass water on, one step each, with no pipes in between. It is the only way to pure water with Create. It keeps both tanks when broken or picked up with a wrench, and it has a Ponder scene.
- **Fans**: washing (water in front of the fan) takes water bottles and terracotta water bowls up one level per pass, up to clean. Water buckets are left out, because Create gives back the bucket's crafting remainder next to the result. Smoking and blasting fans use the smoker and furnace purification recipes.
- **Heated basins**: a mixer over a heated basin takes 250 mB of water up one level per mix, up to clean.
- **Compacting a cactus** gives 250 mB of acceptable water (3) and green dye.
- **Spouts, drains, pipes, pumps and hose pulleys** keep the purity of the water they move. An open pipe end that pulls water from the world or a cauldron takes the purity it has there, and water it pours is remembered as poured water. An Item Drain empties terracotta water bowls.
- **Goggles** show the purity of water and of fluids that carry it (Create's tea) in tanks and basins.
- Create 6.0.6 or newer is required when installed.

### Farmer's Delight and its addons

- **Drink values** for Farmer's Delight, Farmer's Respite, Brewin' and Chewin', Collector's Reap, Cook's Collection, Extra Delight, Ocean's Delight, Ender's Delight, Miner's Delight, Fruits Delight, Cultural Delights, Expanded Delight, Corn Delight, Rustic Delight, Crabber's Delight, End's Delight, My Nether's Delight and the Let's Do mods (Farm & Charm, HerbalBrews, Brewery). Each loads only when its mod is installed. Salty, pickled and strongly alcoholic items make you thirstier.
- **Clean water for cooking**: recipes of these mods that use water accept only water of a minimum purity. Murky (2) or better for recipes that boil the water, acceptable (3) or better for cold ones (fermenting, mixing, aging). Contaminated and dirty water are never accepted. Water with no purity stored counts as acceptable.
- **Cooking pot**: the Farmer's Delight cooking pot (and Miner's Delight's copper pot) purifies water bottles, buckets, terracotta bowls and Cold Sweat waterskins, one level per cook, like a campfire.
- **Kettles** boil water, so they refuse dirty water: the HerbalBrews tea kettle leaves water below murky in its water slot, and the Brewery brewing stations refuse it with a message.
- **Taps, sinks and wells** (Extra Delight's taps and sinks, the Let's Do sink, the Farm & Charm timber well) give water with the purity of the world's water where they stand.
- **Miner's Delight's water cup** carries a purity like a bottle, filled from the world, a cauldron or a Create spout.
- **Nourishment** (Farmer's Delight), **Stuffed** (Let's Do Bakery) and **Saturated** (Let's Do Brewery) pause thirst loss while they last.
- Chest loot of Brewin' and Chewin' and Farmer's Respite drinks, when those mods are installed.

### Cold Sweat

- The climate factor of thirst loss comes from Cold Sweat's body temperature instead of the biome: comfortable is normal thirst loss, burning is twice as much.
- A hot body temperature also counts as a hot climate for dirty water.
- Drinking water can cool the body (`coldsweat.drinkCooling`, off by default).
- The **waterskin** hydrates like a drink of water (thirst 6, quenched 3) with the purity of the water it holds. Filling it from a source, a cauldron or a tank gives it that water's purity, and the cooking packs purify it like a bottle (it comes out full).
- With Cold Sweat installed, Serene Seasons is ignored for thirst, since Cold Sweat's body temperature already follows the seasons.
- Cold Sweat 2.4 or newer is required when installed.

### Vampires

A vampire gets thirsty like anyone else, but only blood quenches its thirst. Its thirst bar has blood-red droplets.

- Thirst goes down with the same activity and climate rules as for other players.
- Water, other drinks, food, rain and drinking by hand give a vampire nothing: no thirst, no quenched and no purity effects. Hand drinking is off for vampires. The HUD shows no preview for these items.
- Blood has no purity, so it never makes a vampire sick, and it never counts towards Overhydrated.
- **Vampirism**: every drink of blood restores thirst: biting a creature, blood bottles, blood containers and blood food. One point of blood gives `vampirism.thirstPerBlood` thirst (1.0: the blood bar and the thirst bar are both 20, so a full blood refill is a full thirst bar) and quenched by the blood's saturation (`vampirism.quenchedPerBlood`). Altars and commands that fill the blood bar do not count. Vampirism stops a vampire from drinking a bottle while its blood bar is full, so blood and thirst usually go down together.
- **Supernatural**: its blood bottle gives thirst 6 and quenched 6. Supernatural has no bite, so bottles are the way to drink blood.
- Items in the item tag `blue_droplets:blood` hydrate vampires with their values in the drinks data map. By default it holds `#supernatural:blood`.

### Reliquary

Drinking from the Emperor's Chalice hydrates like a drink of pure water (thirst 4, quenched 5, no extra pure water bonus). The Infernal Chalice does not hydrate. Reliquary's potions give thirst 2 and quenched 3.

---

## Configuration

These keys exist on 1.21.1 and not on 26.3.

### `compat.toml`

| Key | Default | Meaning |
|---|---|---|
| `create.sandFilterFiltrationAmount` | `1` | Purity levels gained in a Sand Filter (0-5) |
| `create.sandFilterMbPerTick` | `10` | Millibuckets filtered per tick (1-1000) |
| `create.sandFilterMaxPurity` | `5` | Highest purity a Sand Filter raises water to (0-5). Purer water passes unchanged |
| `create.openEndedPipePurity` | `true` | Water an open pipe end pulls from the world or from a water cauldron keeps its purity there, as with buckets and the hose pulley. `false` = it reads as `defaultPurity` |
| `coldsweat.useBodyTemperature` | `true` | The climate factor comes from Cold Sweat's body temperature (`bodyTemperatureCurve`) instead of the biome. The dimension's thirst multiplier and `netherMultiplier` still come first |
| `coldsweat.bodyTemperatureCurve` | `["-100,0.8", "0,1.0", "50,1.3", "100,2.0", "150,3.0"]` | `"bodyTemperature,multiplier"` points in ascending order (Cold Sweat units: 0 comfortable, 100 burning, -100 freezing), straight lines between them, flat beyond the ends, times `depletion.multiplier` |
| `coldsweat.drinkCooling` | `0.0` | How much drinking water (containers, by hand, the Traveler's Backpack hose) cools the body, in Cold Sweat units. `0` = off. Cold Sweat's own waterskin is left alone: it already changes the temperature by its water |
| `coldsweat.drinkCoolingTicks` | `0` | `0`: `drinkCooling` lowers the body temperature once, and it drifts back with the surroundings. More: it lowers the base temperature for that many ticks instead, like Cold Sweat's cold foods (another drink restarts it) |
| `delight.kettleMinPurity` | `2` | Lowest water purity that kettles take (0-5): the HerbalBrews tea kettle, the Brewery brewing stations and any block in `blue_droplets:rejects_dirty_water`. `0` = any water |
| `delight.worldPurityWaterSources` | `true` | Taps and sinks of Extra Delight, the Let's Do sink and the Farm & Charm timber well give water with the world's purity where they stand. `false` = water without a purity, read as `defaultPurity` |
| `reliquary.emperorChaliceCooldown` | `0` | Ticks before the Emperor's Chalice can be used again after a drink. `0` = no cooldown |
| `vampirism.thirstPerBlood` | `1.0` | Thirst a Vampirism vampire gets per point of blood it drinks (0-20). `1.0` = a full blood refill fills the thirst bar |
| `vampirism.quenchedPerBlood` | `1.0` | Quenched per point of blood, times the blood's saturation in Vampirism (0.3 poor to 1.0 rich; a blood bottle is 0.45) |

`sereneseasons.*` does nothing while Cold Sweat is installed.

### Other files

| Key | Default | Meaning |
|---|---|---|
| `purity.toml` `hotDirtyWater.useColdSweat` | `true` | With Cold Sweat installed, its body temperature also counts as hot |
| `purity.toml` `hotDirtyWater.coldSweatMinBodyTemp` | `50.0` | Cold Sweat body temperature above which the player is hot (-150 to 150) |
| `client.toml` `Bar Colors.vampire` | `#B3121B` | Droplet colour for vampires (Vampirism, Supernatural). It wins over every other colour |
| `gameplay.toml` `effects.quenchnessPotion` | `true` | Switches the brewing mixes, which are registered in code. Not synced: keep the same value on the server and the clients, or the brewing stand may not accept prismarine crystals on the client |

- The pure water bonus also counts Cold Sweat waterskins.
- With purity off, Create goggles and the Sand Filter tooltip show no purity, the Sand Filter lets water through unchanged, and cactus compacting gives plain water. `sandFilterFiltrationAmount`, `sandFilterMaxPurity` and `openEndedPipePurity` do nothing.
- Moving from Thirst Was Taken: `common.toml` `Create compatibility.*` moves to `compat.toml` `create.*`.

---

## Modpack makers

### Data formats

- **Pack format**: datapacks declare `"pack_format": 48` in `pack.mcmeta`.
- **Recipe ingredients**: NeoForge custom ingredients use `"type"`, not `"neoforge:ingredient_type"`: `{"type": "neoforge:components", "items": "minecraft:potion", "components": {"minecraft:potion_contents": {"potion": "minecraft:water"}, "blue_droplets:purity": 3}}`. Results are `{"id": ..., "count": 1, "components": {...}}`.
- **Brewing**: the Potion of Quenchness mixes are registered in code (NeoForge's `RegisterBrewingRecipesEvent`), not as data. To use another ingredient, set `effects.quenchnessPotion = false` and add your own mix with KubeJS or a mod.
- **Loot**: loot tables and modifiers use the loot format before 26.3 (`functions` and `conditions` lists, `"function"` and `"condition"` keys), as shown on [Minecraft 26.2](Minecraft-26.2#modpack-makers). The chest loot modifiers are listed in `data/neoforge/loot_modifiers/global_loot_modifiers.json`, which carries the `blue_droplets:loot_config` condition. The ones for Brewin' and Chewin' and Farmer's Respite drinks (`add_loot_*_bc`, `add_loot_*_fr`) load only with those mods.
- **Attributes**: the `minecraft:attribute_modifiers` component is `{"modifiers": [...]}`, and Overhydrated modifies `minecraft:generic.movement_speed`.

### Defaults with other mods

| Tag | Default on 1.21.1 |
|---|---|
| `blue_droplets:purity_containers` (item) | Create builder's tea, Collector's Reap teas, Cold Sweat filled waterskin, Miner's Delight water cup |
| `blue_droplets:salty` (item) | The four vanilla items, plus the salty, pickled and strong alcoholic items of the Delight addons |
| `blue_droplets:blood` (item) | `#supernatural:blood`. The only items that hydrate a vampire (Vampirism, Supernatural), with their drinks data map values |
| `blue_droplets:carries_purity` (fluid) | `#minecraft:water`, Create tea. Fluids made in a Create basin from water keep the water's purity |
| `blue_droplets:rejects_dirty_water` (block) | Brewery wooden, copper and netherite brewing stations. Blocks filled by clicking with water that boil it: water below `delight.kettleMinPurity` is refused |
| `blue_droplets:pauses_thirst` (mob_effect) | Farmer's Delight Nourishment, Let's Do Bakery Stuffed, Let's Do Brewery Saturated |

The `blue_droplets:drinks` data map ships entries for the mods listed under [Farmer's Delight](#farmers-delight-and-its-addons), Create, Cold Sweat and Reliquary, each with a `neoforge:mod_loaded` condition.

On 1.21.1 the tag folders are singular, as on 26.3: `tags/item/`, `tags/block/`, `tags/fluid/`, `tags/mob_effect/`.

### Built-in datapacks

| Pack id | Default | Recipes |
|---|---|---|
| `mod/blue_droplets:datapacks/purify_smelting` | enabled | Furnace: two levels up per cook, up to clean (4) with Create and up to pure (5) without it |
| `mod/blue_droplets:datapacks/purify_campfire` | enabled | Campfire: one level up per cook, with the same caps |
| `mod/blue_droplets:datapacks/purify_smoking` | disabled | Smoker: as the furnace, twice as fast |
| `mod/blue_droplets:datapacks/purify_cooking_pot` | enabled | Farmer's Delight cooking pot and Miner's Delight copper pot: one level up per cook, with the campfire caps, for bottles, buckets, terracotta bowls and, with Cold Sweat, waterskins. Loads only with Farmer's Delight |
| `mod/blue_droplets:datapacks/clean_water_cooking` | enabled | Recipes of Farmer's Delight addons that use water accept only water of a minimum purity: 2 for boiled water, 3 for cold. It overwrites the addon recipes under their own ids (`extradelight:vat/kimchi_item`, `farm_and_charm:pot_cooking/nettle_tea`, …), each loaded only with its mod |

- Where a recipe changes with Create, its id ends in `_with_create` (result capped at 4, loads only with Create) or `_without_create` (loads only without Create, reaches 5), as in `water_bottle_from_smelting_to_clean_with_create`. The `pure` recipes exist only without Create and have no suffix. Recipes that exist only without Create carry `{"type": "neoforge:not", "value": {"type": "neoforge:mod_loaded", "modid": "create"}}`.
- The `purify_cooking_pot` recipes end in `_manual_only`, so Slice & Dice does not turn them into Create basin recipes.
- The Create recipes live in the mod's own data (`blue_droplets:compat/create/...`) and load only with Create and with purity on: fan washing (`create:splashing`), heated mixing (`create:mixing`, `heat_requirement: heated`), cactus compacting, and the Item Drain for terracotta bowls. Cactus compacting has a second copy, `blue_droplets:compat/create/cactus_without_purity`, with `{"type": "neoforge:not", "value": {"type": "blue_droplets:purity_enabled"}}`, so it still gives plain water with purity off.
- The cooking packs also purify Cold Sweat's filled waterskin (`blue_droplets:filled_waterskin_from_smelting_to_murky`, …), loaded only with Cold Sweat. There is no recipe for a waterskin with no purity stored, but waterskins filled with Blue Droplets installed always get one.
- With JEI, the "Water Purification" page also shows the Sand Filter, one step per purity with the `compat.toml` amount and maximum.

---

## Mod compatibility

Minimum versions when installed: Create 6.0.6, Farmer's Delight 1.3, Cold Sweat 2.4, Serene Seasons 10.1, Traveler's Backpack 10.1, Reliquary 2.0, JEI 19.21, Brewin' and Chewin' 4.5, Extra Delight 2.6.5, Farm & Charm 1.1.26, HerbalBrews 1.1.4, Brewery 2.1, Fruits Delight 1.2.14, Cultural Delights 0.18, Expanded Delight 0.1.4, Miner's Delight 1.4, Corn Delight 1.2.10, Rustic Delight 1.7, Crabber's Delight 1.3, End's Delight 2.6, My Nether's Delight 1.10. Built against Create 6.0.10, Cold Sweat 2.4.3.1, JEI 19.21.0.247 and KubeJS 2101.7.2.

- **Jade** 15.10.6 and newer work: the purity is shown by a Jade plugin. Tank purity needs Jade on the server too.
- **JEI** pages also show in EMI when JEI is installed next to it.
- **Expanded Delight** turns off two melon juice recipes of Farmer's Delight and Farmer's Respite. With Expanded Delight installed they stay off, and its Juicer makes the juice.

---

## KubeJS

Modpacks can use the API from KubeJS 7 scripts, with no Java. Load the classes with `Java.loadClass` and listen to the events with `NativeEvents.onEvent`. Working scripts are in [`examples/kubejs`](https://github.com/Darkona/blue-droplets/tree/1.21.1-neoforge/examples/kubejs) on the `1.21.1-neoforge` branch, tested with KubeJS 2101.7.2.

```js
var DropletsAPI = Java.loadClass('com.darkona.droplets.api.DropletsAPI')
var PurityLevel = Java.loadClass('com.darkona.droplets.api.PurityLevel')

// Read and change thirst (server scripts)
ItemEvents.rightClicked('minecraft:clock', event => {
  let thirst = DropletsAPI.view(event.player)
  event.player.tell(`Thirst ${thirst.thirst()}/${thirst.maxThirst()}`)
  if (event.player.shiftKeyDown) DropletsAPI.setThirst(event.player, 20)
})

// Hydrate when eating
ItemEvents.foodEaten('minecraft:golden_carrot', event => {
  if (event.entity.isPlayer()) DropletsAPI.drink(event.entity, 4, 2)
})

// Events: nested classes are loaded with a $
var DrinkPre = Java.loadClass('com.darkona.droplets.api.event.DrinkEvent$Pre')
NativeEvents.onEvent(DrinkPre, event => {
  if (event.purity <= PurityLevel.DIRTY.level() && event.entity.level.dimension.toString() == 'minecraft:the_nether')
    event.canceled = true
})

// Register a drink in a startup script, after the item exists
StartupEvents.postInit(() => {
  DropletsAPI.registerDrink(Item.of('kubejs:lemonade').item, 6, 4, PurityLevel.CLEAN.level())
})
```

- Use `getItemPurity`, `getFluidPurity`, `withItemPurity` and `withFluidPurity` instead of the `getPurity` and `withPurity` overloads. KubeJS cannot choose between an `ItemStack` and a `FluidStack` and fails with "the choice of Java method is ambiguous".
- Top level `const` and `let` are shared by all scripts of one type, so declare the loaded classes with `var` when several files load the same one.
- Registered drinks belong in `startup_scripts` (`StartupEvents.postInit`, when the items exist). `items.toml`, the `blue_droplets:drinks` data map and `#blue_droplets:no_thirst` still win over them.
- Exhaustion modifiers take a JavaScript function and a string id: `DropletsAPI.registerExhaustionModifier('kubejs:night', (player, multiplier) => player.level.isNight() ? multiplier * 0.75 : multiplier)`.
- In KubeJS, `entity.level` and `level.dimension` are properties, not methods.

---

## Mod developers

- The API has the same classes and methods, with the 1.21.1 names: `ResourceLocation` in place of `Identifier` (`ResourceLocation.fromNamespaceAndPath`).
- Fluids go through `IFluidHandler` and `FluidStack`. A water cauldron seen through NeoForge's fluid capability reports its water with the cauldron purity.
- `EatEvent` is also posted when another mod calls `Player#eat` directly.
- Built-in bar styles include the vampire style (priority 400).
- The Maven version is `beta-1.21.1-1.0.0`. Declare the dependency in `neoforge.mods.toml` with `type = "optional"`.
