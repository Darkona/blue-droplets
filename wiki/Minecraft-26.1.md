# Minecraft 26.1

Droplets of Thirst for Minecraft 26.1 runs on **Minecraft 26.1.2 and NeoForge 26.1.2.109 or newer** (branch `26.1`, jar `droplets-of-thirst-26.1-1.0.0.jar`). Thirst, water purity, effects, config files, data maps, commands and the API work as the rest of this wiki describes for 26.3. This page lists what is different.

---

## Players

- **Reliquary**: drinking from the Emperor's Chalice hydrates like a drink of pure water (thirst 4, quenched 5, no extra pure water bonus). Its own food and damage stay as Reliquary has them. The Infernal Chalice does not hydrate. Reliquary's potions give thirst 2 and quenched 3.
- Everything else plays as on 26.3.

---

## Configuration

`compat.toml` has a `[reliquary]` section, read only with Reliquary installed:

| Key | Default | Meaning |
|---|---|---|
| `reliquary.emperorChaliceCooldown` | `0` | Ticks (20 = 1 second) before the Emperor's Chalice can be used again after a drink. `0` = no cooldown (0-72000) |

`gameplay.toml` `effects.quenchnessPotion` switches the brewing mixes, which the mod registers in code. The server does not sync this key. Keep the same value on the server and the clients, or the brewing stand can refuse prismarine crystals on the client.

---

## Modpack makers

The data formats are those of [Minecraft 26.2](Minecraft-26.2#modpack-makers): brewing in code, the loot format with `functions` and `conditions` lists, and the Reliquary drink values. The one difference is the pack format:

- **Pack format**: datapacks declare `"min_format": 101, "max_format": 101` in `pack.mcmeta`.

---

## Mod compatibility

Tested with Jade 26.1.11, JEI 29.43.0.106, AppleSkin 3.0.9, Serene Seasons 26.1.2.0.4, Traveler's Backpack 11.2.8, Reliquary 2.0.92 and KubeJS 8.0.6, all for Minecraft 26.1.2. Minimum versions when installed: JEI 29.43, Traveler's Backpack 11.2.8, Reliquary 2.0.

- **Serene Seasons 26.1.2.0.7** crashes the client on its own, in any world. Use 26.1.2.0.4 until Serene Seasons fixes it.
- **KubeJS 8.0.6** does not load on NeoForge 26.1.2.112. It works on 26.1.2.109.
- Create, Farmer's Delight and its addons, Cold Sweat, Supernatural and Vampirism have no Minecraft 26.1 version, so this version does not have their integrations. Vampirism has a 26.1 alpha, but it needs a library that nobody has published. [Minecraft 1.21.1](Minecraft-1.21.1) tells what these integrations do there.

---

## KubeJS

Modpacks can use the API from KubeJS 8 scripts, with no Java. Load the classes with `Java.loadClass` and listen to the events with `NativeEvents.onEvent`. Working scripts are in [`examples/kubejs`](https://github.com/Darkona/droplets-of-thirst/tree/26.1/examples/kubejs) on the `26.1` branch.

```js
var DropletsAPI = Java.loadClass('com.darkona.dropletsofthirst.api.DropletsAPI')
var PurityLevel = Java.loadClass('com.darkona.dropletsofthirst.api.PurityLevel')

// Read and change thirst (server scripts)
ItemEvents.rightClicked('minecraft:clock', event => {
  let thirst = DropletsAPI.view(event.player)
  event.player.tell(`Thirst ${thirst.thirst()}/${thirst.maxThirst()}`)
  if (event.player.shiftKeyDown) DropletsAPI.setThirst(event.player, 20)
})

// Events: nested classes are loaded with a $
var DrinkPre = Java.loadClass('com.darkona.dropletsofthirst.api.event.DrinkEvent$Pre')
NativeEvents.onEvent(DrinkPre, event => {
  if (event.purity <= PurityLevel.DIRTY.level() && event.entity.level.dimension.toString() == 'minecraft:the_nether')
    event.canceled = true
})

// Register a drink in a startup script, after the item exists
StartupEvents.postInit(() => {
  DropletsAPI.registerDrink(Item.getItem('kubejs:lemonade'), 6, 4, PurityLevel.CLEAN.level())
})
```

- Minecraft 26.1 cannot build item stacks in startup scripts: look the item up with `Item.getItem('id')` instead of `Item.of`.
- Use `getItemPurity`, `getFluidPurity`, `withItemPurity` and `withFluidPurity` instead of the `getPurity` and `withPurity` overloads. KubeJS cannot choose between an `ItemStack` and a `FluidStack` and fails with "the choice of Java method is ambiguous".
- All scripts of one type share their top level `const` and `let`, so declare the loaded classes with `var` when several files load the same one.
- A player's tags are `entityTags()`, and night is `isDarkOutside()`.
- Registered drinks belong in `startup_scripts`. `items.toml`, the `droplets_of_thirst:drinks` data map and `#droplets_of_thirst:no_thirst` still win over them.

---

## Mod developers

- Declare the dependency in `neoforge.mods.toml` with `type = "optional"`.
- The Maven version is `26.1-1.0.0`.
- The API is the same as on 26.3.
