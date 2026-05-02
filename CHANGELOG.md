# Changelog

Changes by feature, newest version first. Upstream issues are cited as `thirst#NN` (ghen-git/Thirst-Mod).

## Unreleased (Minecraft 1.21.1, NeoForge)

### Mod compatibility

- Mixins into Create, Jade, Farmer's Delight and Tough As Nails live in a separate optional mixin config and are only applied when that mod is installed; if one no longer matches the installed version it is logged and skipped instead of crashing the game (thirst#280). `[1.21.1]`

- Declared Create, Jade, AppleSkin, Supernatural, Farmer's Delight, Cold Sweat and Vampirism as optional dependencies. Create must be 6.0.6 or newer when installed. `[1.21.1]`
- Tough As Nails is now marked incompatible: the game refuses to start with both mods and explains why. `[1.21.1]`

### Project

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
