# Droplets of Thirst

Droplets of Thirst adds a thirst bar and water purity to Minecraft. Activity and heat make you thirsty, and the water you find can make you sick: swamp or sea water is unsafe until you clean it. The mod is made for modpacks, so every number is in a config file or a datapack. It continues [Thirst Was Taken](https://github.com/ghen-git/Thirst-Mod) by ghen.

<!-- SCREENSHOT: the HUD in survival, thirst bar (blue droplets) above the hunger bar, player holding a water bottle -->

This wiki describes the current version, **Minecraft 26.3 on NeoForge 26.3.0.36-beta**. If you play another version, read these pages together with the page of your version in the table below.

---

## Pages

- [Gameplay](Gameplay): the thirst bar, how to drink, where clean water comes from and how to purify it, effects and potions.
- [Configuration](Configuration): every config file and option, the commands, and how to play with thirst only (no water purity).
- [Modpack makers](Modpack-Makers): drink values, water purity in the world, tags, recipes, loot and datapacks.
- [Mod developers](Mod-Developers): the public API and events.

---

## Versions

Every version has the same thirst, water purity, effects, config files and commands. The versions differ in the loader, in the data formats of their Minecraft version and in the other mods they work with. Minecraft 1.21.1 has the most integrations: Create, Farmer's Delight and its addons, Cold Sweat, Vampirism, Supernatural, Reliquary and KubeJS. Most of those mods have no 26.x version yet, so the 26.x versions do not have those integrations.

| Minecraft | Loader | Branch | Notable differences from 26.3 |
|---|---|---|---|
| **26.3** (this wiki) | NeoForge 26.3.0.36-beta only | `26.3` | Works with Jade, JEI, AppleSkin, Serene Seasons and Traveler's Backpack |
| [26.2](Minecraft-26.2) | NeoForge 26.2.0.88 or newer | `26.2` | Reliquary. Loot tables in the old format, Quenchness brewing in code, pack format 107 |
| [26.1](Minecraft-26.1) | NeoForge 26.1.2.109 or newer (Minecraft 26.1.2) | `26.1` | Reliquary and KubeJS 8. Same data formats as 26.2, pack format 101 |
| [1.21.11](Minecraft-1.21.11) | NeoForge 21.11.42 or newer | `1.21.11` | Reliquary and Supernatural vampires. Same data formats as 26.2, pack format 94, loot modifiers listed in `global_loot_modifiers.json` |
| [1.21.1](Minecraft-1.21.1) | NeoForge 21.1.219 or newer | `1.21.1` | All integrations: Create (Sand Filter), Farmer's Delight and its addons, Cold Sweat, Vampirism, Supernatural, Reliquary, KubeJS 7 |
| [1.20.1](Minecraft-1.20.1) | Forge 47.1.3 or newer, and NeoForge 47.1 (one jar) | `1.20.1` | Purity in NBT. Most 1.21.1 integrations, no KubeJS |
| [1.19.2](Minecraft-1.19.2) | Forge 43.5.2 or newer | `1.19.2` | Purity in NBT, Create 0.5.1, fewer addons, no Supernatural vampires |
| [1.18.2](Minecraft-1.18.2) | Forge 40.2.3 or newer | `1.18.2` | As 1.19.2, with Forge 40 biome tags and fewer addons |

The [GitHub repository](https://github.com/Darkona/droplets-of-thirst) has the downloads, the changelog and the source code.
