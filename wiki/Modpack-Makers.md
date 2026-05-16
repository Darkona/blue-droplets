# Modpack makers

Blue Droplets is configured in two layers:

- **Datapacks** hold everything that belongs to a registry id: item values, tags, biomes, dimensions. They reload with `/reload` and are sent to clients by the server.
- **TOML files** in `config/bluedroplets/` hold global numbers and switches, plus a few explicit per-item overrides.

All paths below are inside a datapack (`data/<namespace>/...`). Bad entries are logged and skipped; they never crash the game or disconnect players.

## Drink and food values: `bluedroplets:drinks`

A NeoForge [data map](https://docs.neoforged.net/docs/resources/server/datamaps/) on items. File: `data/<namespace>/data_maps/item/drinks.json` (any namespace; all packs are merged in load order).

```json
{
  "values": {
    "minecraft:apple": { "thirst": 2, "quenched": 3 },
    "#c:drinks/juice": { "thirst": 8, "quenched": 13 },
    "examplemod:spring_water": { "thirst": 6, "quenched": 8, "purity": 3 }
  },
  "remove": ["minecraft:carrot"]
}
```

| Field | Type | Meaning |
|---|---|---|
| `thirst` | int 0-20 | Thirst restored |
| `quenched` | int ≥ 0 | Quenched restored (hidden "saturation" of thirst) |
| `purity` | int 0-3, optional | Purity of this drink when the stack stores none (0 dirty, 1 slightly dirty, 2 acceptable, 3 purified). Drinks with a purity roll the purity effects; without it only water containers do |

- Keys are item ids or `#tags`. An entry from a later pack replaces the earlier one for that item. `"replace": true` at the top clears everything loaded before this file.
- `remove` drops entries loaded before this file (ids or tags).
- An unknown item id is logged as an error by NeoForge. For items of optional mods, put a condition on the entry:

  ```json
  "farmersdelight:apple_cider": {
    "neoforge:conditions": [{ "type": "neoforge:mod_loaded", "modid": "farmersdelight" }],
    "thirst": 8, "quenched": 13
  }
  ```

- Items with food properties (eaten) and items without (drunk) use the same map; Blue Droplets tells them apart by the item itself.
- Blue Droplets ships its defaults in `data/bluedroplets/data_maps/item/drinks.json`. To change a default, add your own entry (a later pack wins) or `remove` it.
- The map is synced to clients; tooltips, AppleSkin and the HUD use the server's values.
