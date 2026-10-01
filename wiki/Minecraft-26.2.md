# Minecraft 26.2

Droplets of Thirst for Minecraft 26.2 runs on **NeoForge 26.2.0.88 or newer** (branch `26.2`, jar `droplets-of-thirst-26.2-1.0.0.jar`). Thirst, water purity, effects, config files, data maps, commands and the API work as the rest of this wiki describes for 26.3. This page lists what is different.

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

- **Pack format**: datapacks declare `"min_format": 107, "max_format": 107` in `pack.mcmeta`. Recipe ingredients use the same format as 26.3 (`"neoforge:ingredient_type": "neoforge:components"`).
- **Brewing**: the mod registers the Potion of Quenchness mixes in code. They are not `minecraft:brewing` recipes, so a datapack cannot change them, and the `droplets_of_thirst:quenchness_potion` condition does not exist. To use another ingredient, set `effects.quenchnessPotion = false` and add your own mix through a mod that registers brewing mixes. Gunpowder and dragon's breath make splash and lingering potions, as with vanilla potions.
- **Loot**: loot tables and global loot modifiers use the loot format before 26.3: a `functions` list with `"function"` as the key of each function, and a `conditions` list with `"condition"` as the key. A chest loot modifier:

  ```json
  {
    "neoforge:conditions": [{ "type": "droplets_of_thirst:loot_config" }],
    "type": "neoforge:add_table",
    "conditions": [{ "condition": "neoforge:loot_table_id", "loot_table_id": "minecraft:chests/simple_dungeon" }],
    "table": "droplets_of_thirst:chests/simple_dungeon"
  }
  ```

  And the purity of a loot bottle:

  ```json
  "functions": [
    { "function": "minecraft:set_components", "components": { "droplets_of_thirst:purity": 3 }, "conditions": [{ "condition": "droplets_of_thirst:purity_enabled" }] },
    { "function": "minecraft:set_count", "count": { "type": "minecraft:uniform", "min": 1, "max": 3 } },
    { "function": "minecraft:set_potion", "id": "minecraft:water" }
  ]
  ```

- **Drink values**: the `droplets_of_thirst:drinks` data map also has `reliquary:emperor_chalice` (4, 5, purity 5) and `reliquary:potion` (2, 3), each with a `neoforge:mod_loaded` condition.

---

## Mod compatibility

Tested with Jade 26.2.10, JEI 30.38.0.231, AppleSkin 3.0.10, Serene Seasons 26.1.2.0.6, Traveler's Backpack 11.3.4 and Reliquary 2.0.92, all for Minecraft 26.2. Minimum versions when installed: JEI 30, Serene Seasons 26.1.2.0.4, Traveler's Backpack 11.3.0, Reliquary 2.0.

- **Serene Seasons** numbers its Minecraft 26.2 versions 26.1.2.0.4 to 26.1.2.0.6. All of them work.
- **KubeJS** has no version for Minecraft 26.2.
- Create, Farmer's Delight and its addons, Cold Sweat, Supernatural and Vampirism have no Minecraft 26.2 version, so this version does not have their integrations either. [Minecraft 1.21.1](Minecraft-1.21.1) tells what they do there.

---

## Mod developers

- Declare the dependency in `neoforge.mods.toml` with `type = "optional"`. FML for 26.2 is version 11.
- The Maven version is `26.2-1.0.0`.
- The API is the same as on 26.3.
