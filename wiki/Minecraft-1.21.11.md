# Minecraft 1.21.11

Droplets of Thirst for Minecraft 1.21.11 runs on **Minecraft 1.21.11 and NeoForge 21.11.42 or newer** (branch `1.21.11`, jar `droplets-of-thirst-1.21.11-1.0.0.jar`). Thirst, water purity, effects, config files, data maps, commands and the API work as the rest of this wiki describes for 26.3. This page lists what is different.

---

## Players

### Vampires (Supernatural)

A Supernatural vampire gets thirsty like anyone else, and only blood quenches its thirst. Its thirst bar has blood-red droplets.

- Thirst goes down with the same activity and climate rules as for other players.
- Water, other drinks, food, rain, Quenchness and drinking by hand give a vampire nothing: no thirst, no quenched and no purity effects. Hand drinking is off for vampires. The HUD shows no preview for these items.
- Supernatural's blood bottle gives thirst 6 and quenched 6. Blood has no purity, so it never makes a vampire sick.
- Items in the item tag `droplets_of_thirst:blood` hydrate vampires with their values in the drinks data map. By default it holds `#supernatural:blood`.

### Reliquary

Drinking from the Emperor's Chalice hydrates like a drink of pure water (thirst 4, quenched 5, no extra pure water bonus). The Infernal Chalice does not hydrate. Reliquary's potions give thirst 2 and quenched 3.

---

## Configuration

`compat.toml` has a `[reliquary]` section, read only with Reliquary installed:

| Key | Default | Meaning |
|---|---|---|
| `reliquary.emperorChaliceCooldown` | `0` | Ticks (20 = 1 second) before the Emperor's Chalice can be used again after a drink. `0` = no cooldown (0-72000) |

Other keys that exist on 1.21.11 and not on 26.3:

| Key | Default | Meaning |
|---|---|---|
| `client.toml` `Bar Colors.vampire` | `#B3121B` | Droplet colour for Supernatural vampires. It wins over every other colour |
| `gameplay.toml` `effects.quenchnessPotion` | `true` | Switches the brewing mixes, which are registered in code. Not synced: keep the same value on the server and the clients, or the brewing stand may not accept prismarine crystals on the client |

---

## Modpack makers

The data formats are those of [Minecraft 26.2](Minecraft-26.2#modpack-makers): brewing in code, the loot format with `functions` and `conditions` lists, and the Reliquary drink values. The differences:

- **Pack format**: datapacks declare `"min_format": 94, "max_format": 94` in `pack.mcmeta`.
- **Loot modifiers**: NeoForge 21.11 loads only the loot modifiers listed in `neoforge:loot_modifiers/global_loot_modifiers.json`. A datapack that adds its own modifiers must list them there. `gameplay.toml` `loot.enabled` turns the Droplets of Thirst chest loot off as on 26.3.
- **Blood**: the item tag `droplets_of_thirst:blood` and the drink value of `supernatural:blood_bottle`.

---

## Mod compatibility

Tested with Jade 21.1.7, JEI 27.44.0.104, AppleSkin 3.0.8, Serene Seasons 21.11.0.5, Traveler's Backpack 10.11.7, Reliquary 2.0.84 and Supernatural 3.3.4, all for Minecraft 1.21.11. Minimum versions when installed: JEI 27.44, Serene Seasons 21.11, Traveler's Backpack 10.11.5, Reliquary 2.0, Supernatural 3.3.

- Create, Farmer's Delight and its addons, Cold Sweat and Vampirism have no Minecraft 1.21.11 version, so this version does not have their integrations. [Minecraft 1.21.1](Minecraft-1.21.1) tells what these integrations do there.
- KubeJS has no Minecraft 1.21.11 version.

---

## Mod developers

- Declare the dependency in `neoforge.mods.toml` with `type = "optional"`.
- The Maven version is `1.21.11-1.0.0`.
- The API is the same as on 26.3.
