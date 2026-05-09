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

### API

- All classes moved from `dev.ghen.thirst` to `com.darkona.droplets` (main class `Thirst` is now `BlueDroplets`) and there is no compatibility shim: addons that call Thirst Was Taken classes directly, such as Green Feathers, need a version built for Blue Droplets. A stable public API is planned. `[1.21.1]`
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
