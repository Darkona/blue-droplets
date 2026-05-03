# Changelog

Changes by feature, newest version first. Upstream issues are cited as `thirst#NN` (ghen-git/Thirst-Mod).

## Unreleased (Minecraft 1.21.1, NeoForge)

### Mod compatibility

- Mixins into Create, Jade, Farmer's Delight and Tough As Nails live in a separate optional mixin config and are only applied when that mod is installed; if one no longer matches the installed version it is logged and skipped instead of crashing the game (thirst#280). `[1.21.1]`
- Declared Create, Jade, AppleSkin, Supernatural, Farmer's Delight, Cold Sweat and Vampirism as optional dependencies. Create must be 6.0.6 or newer when installed: older Create 6 versions drop fluid components from recipe outputs, so purity was lost (related to thirst#259). Built against Create 6.0.10. `[1.21.1]`
- The published jar no longer declares Create, Farmer's Delight, Cold Sweat, Jade or AppleSkin as transitive dependencies (thirst#218). `[1.21.1]`
- Create: compacting a cactus now gives 250 mB of purified water plus green dye; the recipe failed to load on Create 6 (thirst#214). The terracotta bowl filling recipe uses Create 6's current format. `[1.21.1]`
- Brewin' and Chewin' and Farmer's Respite chest loot is only added when those mods are installed. `[1.21.1]`
- Playing without Vampirism, Cold Sweat or AppleSkin can no longer crash with `NoClassDefFoundError`: code that talks to those mods is only loaded when they are installed (thirst#251). `[1.21.1]`
- Tough As Nails is now marked incompatible: the game refuses to start with both mods and explains why. `[1.21.1]`

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
