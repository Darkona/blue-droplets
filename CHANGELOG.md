# Changelog

Changes by feature, newest version first. Upstream issues are cited as `thirst#NN` (ghen-git/Thirst-Mod).

## Unreleased (Minecraft 1.21.1, NeoForge)

### Rebrand: Blue Droplets

- The mod is now **Blue Droplets** (mod id `bluedroplets`, package `com.darkona.droplets`), a continuation of ghen's Thirst Was Taken. The jar is `BlueDroplets-<version>.jar`. `[1.21.1]`
- All ids moved from `thirst:` to `bluedroplets:` (items, blocks, effects, recipes, loot tables, loot modifiers, damage type, the `loot_config` condition, translation keys). Datapacks and resource packs that target `thirst:` ids need updating. `[1.21.1]`
- The command is now `/bluedroplets`; `/thirst` still works as an alias. `[1.21.1]`
- Thirst Was Taken and Thirst Was Reclaimed (mod id `thirst`) are marked incompatible: the game refuses to start with both installed and explains why. `[1.21.1]`
- Credits now name ghen's Thirst Was Taken as the original mod. `[1.21.1]`
- Worlds from Thirst Was Taken load without losing anything: old `thirst:` items, blocks, block entities (Sand Filter), effects, the purity component and the player thirst data are mapped to the new ids and saved under them from then on. `[1.21.1]`
- Player thirst, quenched and the per-player "thirst enabled" flag, water purity on items and fluids, and cauldron purity all carry over from Thirst Was Taken saves. `[1.21.1]`
- Config files moved to `config/bluedroplets/`. On first start, if that folder does not exist and `config/thirst/` does, the old files are copied over and a warning is logged. Modpacks that ship `defaultconfigs/thirst/` must rename it to `defaultconfigs/bluedroplets/`. `[1.21.1]`
- Added an MIT `LICENSE` that keeps the original Thirst Was Taken copyright notice; it is also shipped inside the jar. `[1.21.1]`
- New English README: credits, migration notes, compatibility and version roadmap. `[1.21.1]`
- Temporary placeholder logo (a blue droplet) replaces the Thirst Was Taken logo until the final artwork is ready. `[1.21.1]`

### Mod compatibility

- Mixins into Create, Jade and Farmer's Delight live in a separate optional mixin config and are only applied when that mod is installed; if one no longer matches the installed version it is logged and skipped instead of crashing the game (thirst#280). `[1.21.1]`
- Declared Create, Jade, AppleSkin, Supernatural, Farmer's Delight, Cold Sweat and Vampirism as optional dependencies. Create must be 6.0.6 or newer when installed: older Create 6 versions drop fluid components from recipe outputs, so purity was lost (related to thirst#259). Built against Create 6.0.10. `[1.21.1]`
- The published jar no longer declares Create, Farmer's Delight, Cold Sweat, Jade or AppleSkin as transitive dependencies (thirst#218). `[1.21.1]`
- Create: compacting a cactus now gives 250 mB of purified water plus green dye; the recipe failed to load on Create 6 (thirst#214). The terracotta bowl filling recipe uses Create 6's current format. `[1.21.1]`
- Brewin' and Chewin' and Farmer's Respite chest loot is only added when those mods are installed. `[1.21.1]`
- Playing without Vampirism, Cold Sweat, AppleSkin or Supernatural can no longer crash with `NoClassDefFoundError`: code that talks to those mods is only loaded when they are installed (thirst#251). `[1.21.1]`
- Tough As Nails is now marked incompatible: the game refuses to start with both mods and explains why. `[1.21.1]`
- Removed all Tough As Nails integration: canteen filling, turning off TaN's thirst, TaN tooltips, the TaN water bottle recipes and the TaN drinks in the default `item_settings.toml`. This also removes the canteen bugs (filling in protected areas, lost Water Cleansing, filling from flowing water; thirst#269, thirst#265, thirst#174) and the double thirst bar (thirst#232, thirst#173). Existing `toughasnails:*` entries in an old `item_settings.toml` are skipped. `[1.21.1]`

### Bug fixes

- Pouring water with purity into a cauldron or tank now always updates its purity; the old delayed-task helper could skip the update when several happened on consecutive ticks, and kept tasks alive after leaving a singleplayer world. `[1.21.1]`

- Nausea, Farmer's Delight Nourishment, Let's Do Bakery Stuffed, Let's Do Brewery Saturated and Corail Tombstone Ghostly Shape now work whatever other effects the player has; before, only the first active effect was checked. Effects are matched by id (`farmersdelight:nourishment`, `bakery:stuffed`, `brewery:saturated`, `tombstone:ghostly_shape`), no longer by name fragments. `[1.21.1]`

- Drink, food and container lists (`item_settings.toml`, `container.toml`, `keyword.toml`) are read when the world loads and again on `/reload`, after tags are loaded: `#namespace:tag` entries now work and config edits apply with `/reload` instead of a restart (thirst#153, thirst#155). `[1.21.1]`
- Unknown item or tag ids in those lists are skipped with one warning per id instead of silently becoming air (thirst#272); ids from mods that are not installed are only logged at debug level. `[1.21.1]`
- Asking for the thirst values of an item that has none returns 0 instead of crashing (thirst#239). `[1.21.1]`
- Reopening a singleplayer world no longer registers the config containers again. `[1.21.1]`
- Keyword matching (`keyword.toml`) runs once over all items when the lists are built, instead of on every tooltip and use; this also removes a rare crash when the client and the integrated server checked items at the same time. `[1.21.1]`
- `itemsBlacklist` now also removes items added by the config lists or by keywords, not only those added by other mods' code. `[1.21.1]`
- Looking at a water item's tooltip (or any other read of its purity) no longer adds a purity component to it. That write only happened on the client, so the stack could desync from the server and stop stacking with identical items (thirst#262, thirst#150, thirst#180, thirst#264). `[1.21.1]`
- A water cauldron with no stored purity now gives water of the default purity (`defaultPurity`, "acceptable" by default) instead of an invalid value; invalid purity values on items and fluids also read as `defaultPurity`. Items or fluids with no purity component read as `defaultPurity`, and both forms are accepted. `[1.21.1]`

### API

- All classes moved from `dev.ghen.thirst` to `com.darkona.droplets` (main class `Thirst` is now `BlueDroplets`) and there is no compatibility shim: addons that call Thirst Was Taken classes directly, such as Green Feathers, need a version built for Blue Droplets. A stable public API is planned. `[1.21.1]`
- `RegisterThirstValueEvent` is now posted every time the tables are built (world load and `/reload`), on the game bus, from the logical side that owns the data; `addDrink`, `addFood` and both `addContainer` methods keep their signatures. Its constructor changed and `ThirstEventFactory` was removed. `[1.21.1]`
- Removed `ThirstHelper.VALID_DRINKS`, `VALID_FOODS`, `containers`, `init()` and the `keyword*` fields, plus `LoadedValue` and `ConfigHelper`; use `ThirstHelper.isDrink/isFood/getThirst/getQuenched`. `WaterPurity.addContainer` still works (deprecated). `[1.21.1]`
- Removed the public flags `PlayerThirst.checkTombstoneEffects`, `checkFDEffects`, `checkLetsDoBakeryEffects` and `checkLetsDoBreweryEffects`. `[1.21.1]`
- Removed `ThirstHelper.shouldUseColdSweatCaps`, `PlayerThirst.checkVampirismEffects`, `ThirstBarRenderer.checkIfPlayerIsVampire` and `compat.supernatural.SupernaturalHelper`; mod detection is now internal. The AppleSkin overlay classes moved from `foundation.gui.appleskin` to `compat.appleskin`. `[1.21.1]`

### Project

- Removed the empty access transformer file and its declaration (thirst#234). `[1.21.1]`
- Issue tracker and homepage links now point to https://github.com/Darkona/thirst-was-taken. `[1.21.1]`
- Added this changelog. `[1.21.1]`

## Planned

Ports start once the previous version in the chain is stable:

1. 26.3 (NeoForge)
2. 1.20.1 (one jar for Forge 47.x and NeoForge 47.1)
3. 1.19.2 (Forge)
4. 1.18.2 (Forge)
5. 1.12.2 (Forge)
6. 1.7.10 (Forge)
