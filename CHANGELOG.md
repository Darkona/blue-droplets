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
- Create Sand Filter: water is no longer destroyed when the purified tank is full or holds water of another purity; the filter now only moves what the lower tank accepts and waits otherwise (thirst#192, thirst#205). Purity is never mixed. `[1.21.1]`
- Create Sand Filter: the upper (dirty) tank is now saved with the world and shown with goggles; before, both tanks shared one behaviour slot, so the dirty tank was lost on reload. Breaking the filter or picking it up with a wrench keeps both tanks in the item, and placing it restores them. `[1.21.1]`
- Create goggles: only water and fluids that carry purity show a purity; the purity word uses the same colours as item tooltips. `[1.21.1]`
- Create basins: fluids made from water with a purity now take that purity only when the recipe output has none, and only in the basin that crafts them; before, the recipe itself was changed, so the purity leaked to every other basin, could become invalid, and was also applied when Create only checked recipes. Which output fluids carry purity is now the fluid tag `bluedroplets:carries_purity` (water and Create's tea by default) instead of any fluid whose name contains "tea" (which also matched "steam"). `[1.21.1]`
- Jade: water purity is now shown by a Jade plugin instead of a mixin into Jade, so new Jade versions (15.10.6 and later) no longer break it (thirst#280). Looking at a water cauldron shows its purity; looking at a tank with water or a purity-carrying fluid shows one purity line per tank (needs Jade on the server for tanks). It can be toggled in Jade's plugin settings ("Water purity"). `[1.21.1]`

### Bug fixes

- Pouring water with purity into a cauldron or tank now always updates its purity; the old delayed-task helper could skip the update when several happened on consecutive ticks, and kept tasks alive after leaving a singleplayer world. `[1.21.1]`

- Nausea, Farmer's Delight Nourishment, Let's Do Bakery Stuffed, Let's Do Brewery Saturated and Corail Tombstone Ghostly Shape now work whatever other effects the player has; before, only the first active effect was checked. Effects are matched by id (`farmersdelight:nourishment`, `bakery:stuffed`, `brewery:saturated`, `tombstone:ghostly_shape`), no longer by name fragments. `[1.21.1]`

- Drink, food and container lists (`item_settings.toml`, `container.toml`, `keyword.toml`) are read when the world loads and again on `/reload`, after tags are loaded: `#namespace:tag` entries now work and config edits apply with `/reload` instead of a restart (thirst#153, thirst#155). `[1.21.1]`
- Unknown item or tag ids in those lists are skipped instead of silently becoming air, with one warning per load (world load or `/reload`) that lists all of them (thirst#272); ids from mods that are not installed are only listed at debug level. `[1.21.1]`
- Asking for the thirst values of an item that has none returns 0 instead of crashing (thirst#239). `[1.21.1]`
- Reopening a singleplayer world no longer registers the config containers again. `[1.21.1]`
- Keyword matching (`keyword.toml`) runs once over all items when the lists are built, instead of on every tooltip and use; this also removes a rare crash when the client and the integrated server checked items at the same time. `[1.21.1]`
- `itemsBlacklist` now also removes items added by the config lists or by keywords, not only those added by other mods' code. `[1.21.1]`
- Looking at a water item's tooltip (or any other read of its purity) no longer adds a purity component to it. That write only happened on the client, so the stack could desync from the server and stop stacking with identical items (thirst#262, thirst#150, thirst#180, thirst#264). `[1.21.1]`
- A water cauldron with no stored purity now gives water of the default purity (`defaultPurity`, "acceptable" by default) instead of an invalid value; invalid purity values on items and fluids also read as `defaultPurity`. Items or fluids with no purity component read as `defaultPurity`, and both forms are accepted. `[1.21.1]`
- Quenched can no longer exceed thirst: drinking rain water, commands, syncing and loading old saves all keep `0 <= quenched <= thirst <= 20`. `[1.21.1]`
- After recovering from zero thirst, the dehydration damage timer starts over; before, the first hit after dropping to zero again could come early. `[1.21.1]`
- No thirst packets are sent to fake players (machines and automation from other mods) or to players without a connection; this could crash with some mods. `[1.21.1]`
- Dispensers with an empty bucket facing a water block that cannot be picked up (for example kelp, or a modded block holding water) now use the vanilla behaviour instead of crashing. `[1.21.1]`
- A potion item without potion contents no longer crashes the water bottle stack size check. `[1.21.1]`
- **Dehydration on Normal difficulty no longer kills**: like vanilla starvation it stops at half a heart. Easy still stops at 5 hearts and Hard can still kill. `[1.21.1]`
- Only the Fire Protection enchantment on armor slows thirst loss; generic Protection no longer counts (thirst#195, thirst#219). The reduction per level and the 12-level cap are unchanged. `[1.21.1]`
- The "no sprinting when thirsty" rule now follows the server's `moveSlowWhenThirsty`, sent to the client with the thirst data; before, each client read its own config, so a client could turn it off. Sprinting also checks thirst and food separately, so exactly 6 food no longer lets you sprint when thirst is high. `[1.21.1]`
- The server now also checks that the hands are empty before drinking by hand. `[1.21.1]`
- Water purity is now always stored on items and fluids, also for the default purity ("acceptable"). Before, purity 2 was removed, so the "purified" campfire and furnace recipes for buckets, bottles and terracotta bowls never matched water filled from an acceptable cauldron or tank (thirst#259). Those recipes now also accept water with no purity stored (old saves, vanilla sources), which reads as the default purity. `[1.21.1]`
- Opening the Blue Droplets creative tab no longer crashes with "Accidentally adding the same item stack twice": the "acceptable" terracotta water bowl and the plain bowl were the same stack. The tab now lists buckets, bottles and terracotta water bowls in all four purities. `[1.21.1]`
- Filling a bucket, glass bottle or terracotta bowl from water in the world now sets its purity in one place, on the logical side doing the fill. The old code kept the purity in shared fields of the bucket and bottle items, so in singleplayer the client and the integrated server could swap each other's purity. Bottles of dragon's breath no longer get a purity. Buckets from other mods that reuse the vanilla bucket code no longer get purity from the world (they read as the default purity). `[1.21.1]`
- Drinking water rolls the purity effects once (before, water bottles rolled twice, thirst#204) and always hydrates once per drink, also when drinking from a stack of bottles or the last bottle (thirst#209). Water with no purity stored now gives the effects of the default purity instead of none. Purity effects and hydration from water containers and non-food drinks are handled on the server only, for players only: villagers, witches and other mobs drinking potions are no longer touched (related to thirst#203). Drinks consumed without the use animation (for example by automatic feeding from other mods) only hydrate if they are food. `[1.21.1]`
- **Drinking by hand has a cooldown** (`handDrinkingCooldown`, 10 ticks by default) and is fully checked by the server: `canDrinkByHand`, sneaking, empty hands, reach, spawn protection and that the player really looks at water (the server casts its own ray; the client no longer sends a position). Before, a modified client could drink unlimited water from anywhere, one click sent two drinks (one per hand), and water at negative coordinates was looked up one block off. The drinking sound now comes from the server and is heard by the player and those nearby. `[1.21.1]`
- On a dedicated server, clients now use the server's drink and food values, purity containers, blacklist and `defaultPurity` for tooltips, AppleSkin and the HUD, instead of their own config files (thirst#274, thirst#262, thirst#153). They are sent when joining and again after `/reload`, and dropped when leaving the server. Vanilla water bottles no longer show a purity different from the server's. `[1.21.1]`
- A potion item without potion contents no longer crashes item tooltips or campfire particles. Thirst no longer ticks (and sends no updates) while the player is dead. `[1.21.1]`

### Performance

- Thirst data is sent to the player only when thirst, quenched, the exhaustion shown by AppleSkin (in steps of 0.1) or a synced rule changes, at most once per tick, plus a full resync every 10 seconds and after respawning or changing dimension. Before, every player got one packet per tick while thirst was ticking. A player standing still now gets almost no thirst packets. `[1.21.1]`
- The thirst loss multiplier from climate (biome temperature and humidity, or Cold Sweat body temperature; Nether), Fire Protection and Fire Resistance is computed once per second per player (players are spread over different ticks), and right away after changing armor, gaining or losing an effect, changing dimension or respawning. Before, it was computed up to three times per tick. Walking into another biome, or a Cold Sweat temperature change, now takes effect within one second. `[1.21.1]`
- Checking whether an item is a water container with purity (tooltips, drinking, campfires, Create filling) is one map lookup instead of a scan over all containers. `[1.21.1]`
- The thirst bar always reads the current player's thirst; before, it kept the data of the previous player object for up to 2 seconds after respawning or changing dimension. The thirst bar and the AppleSkin overlays and tooltip no longer create objects every frame. `[1.21.1]`
- Purity effects use the player's random generator instead of creating a new one for every drink. `[1.21.1]`
- Create Sand Filter: an idle filter (not enough dirty water, or a full purified tank) does no fluid work at all each tick. `[1.21.1]`

### Config

- Drink and food entries must be `["namespace:item" or "#namespace:tag", thirst, quenched]` with thirst 0-20 and quenched 0 or more; anything else is skipped with a warning instead of crashing or hanging the game (thirst#178). NeoForge removes invalid entries from the file. `[1.21.1]`
- Numeric options now have ranges: purity values 0-3, percentages 0-100, hand-drinking and keyword values 0-20, water bottle stack size 1-99, `thirstDepletionModifier` 0-10, `sandFilterMbPerTick` 1-1000, `mountainsY`/`cavesY` -2048 to 2048. Out-of-range values are reset to the default with a warning. `[1.21.1]`
- The drink, food, blacklist and container lists can now be empty; before, an empty list was replaced by the defaults. `[1.21.1]`
- Removed the duplicate `collectorsreap:pink_limeade` default entry; its value stays 8/13. `[1.21.1]`
- New `handDrinkingCooldown` (common.toml, "Drinking Mechanics", 0-1200 ticks, default 10): minimum time between two sips when drinking by hand. `[1.21.1]`
- `DrinkBothHandNeeded` moved from `client.toml` to `common.toml` (section "Drinking Mechanics"): it is a gameplay rule, so the server decides and tells the client. A value set in `client.toml` is dropped; set it again in `common.toml`. `[1.21.1]`
- **Altitude bonus for water purity fixed and configurable** (thirst#216): `mountainsY` and `cavesY` are replaced by `altitudeBands` (`common.toml`, list of `"minY,maxY,delta"`, default `["38,4096,1", "-4096,-16,1"]`) measured from sea level by default (`altitudeRelativeToSeaLevel`). Caves keep their +1 below Y 48; the mountain +1 above Y 100 now actually applies (before, a wrong check made it impossible). Custom `mountainsY`/`cavesY` values are not migrated: write them as bands. The documented "-32 for aquatic biomes" never existed and is gone; use biome tags or `bluedroplets:biome_water` instead. `[1.21.1]`
- New `stillWaterPurificationAmount` (default 0), `rainCauldronPurity` and `dripstoneCauldronPurity` (default -1 = as before, no purity stored so the water reads as `defaultPurity`) in `common.toml`. Rain or dripstone adding water to a cauldron keeps the lower of both purities. `[1.21.1]`
- Removed the unused `kettlePurificationLevels`, `fermentationMoldingThreshold` and `fermentationMoldingHarshness` options (sections "Purification levels" and "Fermentation levels" of `common.toml`); they never did anything. Kettle and fermentation purification will come back as datapack recipes. `[1.21.1]`
- **Config files split by concern**: `common.toml`, `item_settings.toml`, `container.toml` and `keyword.toml` are replaced by `gameplay.toml`, `purity.toml`, `items.toml` and `compat.toml` (plus the unchanged `client.toml`), with shorter key names grouped in sections. On first start the values of the old files are copied to the new ones once and the old files are renamed to `*.toml.old`, with a warning in the log. Modpacks that ship `defaultconfigs/` must use the new names; see `wiki/Configuration.md` for the full table. `[1.21.1]`
- Editing a common config file while a server runs now applies everywhere right away: the drink tables are rebuilt and sent again, and every player's thirst loss multiplier and synced rules are refreshed (before, only `/reload` resent the tables and the multiplier waited up to a second). `[1.21.1]`
- On a dedicated server, clients now use the server's `hand.enabled` (drinking by hand) and `waterBottleStackSize`; before, a client with a different local value could not drink by hand or saw water bottles stack differently from the server. `[1.21.1]`
- New `purity.enabled` (`purity.toml`, default `true`): when `false`, water has no purity at all: nothing stores a purity component, tooltips, Jade and Create goggles show none, drinking never gives purity effects, and the purification recipes are not loaded. Synced to clients. `[1.21.1]`

### Datapacks

- New item data map `bluedroplets:drinks` (`data/<namespace>/data_maps/item/drinks.json`, entries `{"thirst": 0-20, "quenched": 0+, "purity": 0-3 optional}`): drink and food values now come from datapacks, reload with `/reload` and are synced to clients. See `wiki/Modpack-Makers.md`. `[1.21.1]`
- The default values for vanilla items and the terracotta water bowl moved from `item_settings.toml` to Blue Droplets' own `drinks.json`, with the same numbers. The `drinks` and `foods` lists in `item_settings.toml` are now overrides that win over datapacks. **Existing configs** that still contain the old default entries keep working unchanged (they act as overrides with the same values); remove them if a datapack should control those items. `[1.21.1]`
- A drink with a data map `purity` rolls the purity effects of that purity when drunk or eaten; water containers without a stored purity also use it instead of `defaultPurity`. Without it, nothing changes. `[1.21.1]`
- Each item now takes its thirst values from exactly one source, the first that has it: blacklist, `item_settings.toml`, the `bluedroplets:drinks` data map, other mods' code (`RegisterThirstValueEvent`), keywords. Before, an item listed as a drink by one source and as a food by another ended up in both lists. `[1.21.1]`
- Water purity in the world can now be set per biome and per dimension: biome tags `bluedroplets:water_purity/0` to `/3`, the biome data map `bluedroplets:biome_water` (`base`, `delta`, `max`) and the dimension type data map `bluedroplets:dimension_water` (`base`); otherwise `worldWaterBasePurity` (new, `common.toml`, default 0 = as before). Nothing is tagged by default, so purity is unchanged. `[1.21.1]`
- New biome tag `bluedroplets:salt_water` (oceans by default) and `saltWaterPurity` (`common.toml`, default -1 = off): when set, water in those biomes always has that purity (thirst#268). `[1.21.1]`
- New item tags `bluedroplets:no_thirst` (never restores thirst, like `itemsBlacklist`) and `bluedroplets:purity_opt_out` (never gets, shows or passes on a purity; for other mods' water containers that compare item data, related to thirst#150, thirst#180, thirst#264). Both are empty by default. `[1.21.1]`
- The default values for Farmer's Delight, Farmer's Respite, Brewin' and Chewin', Collector's Reap, Create's builder's tea and Supernatural's blood bottle moved from `item_settings.toml` to Blue Droplets' `drinks.json`, each entry with a `neoforge:mod_loaded` condition (same numbers). The default `drinks` and `foods` lists in `item_settings.toml` are now empty. Old configs that still list them keep working as overrides. `[1.21.1]`
- The default purity containers (Create's builder's tea, Collector's Reap pomegranate black tea and lime green tea) moved from `container.toml` to the new item tag `bluedroplets:purity_containers`; `container.toml` still adds to it (ids or `#tags`). The default `Containers` list is empty. `[1.21.1]`
- AppleSkin: the quenched preview for a held item is hidden only when the item cannot be eaten right now (full hunger and not always edible), instead of whenever the item was listed as a food. `[1.21.1]`
- The furnace and campfire water purification recipes moved into optional built-in datapacks, `mod/bluedroplets:datapacks/purify_smelting` and `mod/bluedroplets:datapacks/purify_campfire`, enabled by default (also in existing worlds); a world can turn a method off with `/datapack disable`. Recipe ids are unchanged. New optional pack `mod/bluedroplets:datapacks/purify_smoking`, disabled by default: purifies bottles, buckets and terracotta bowls in a smoker, like the furnace but twice as fast. All purification recipes carry the new condition `bluedroplets:purity_enabled`. `[1.21.1]`

### API

- All classes moved from `dev.ghen.thirst` to `com.darkona.droplets` (main class `Thirst` is now `BlueDroplets`) and there is no compatibility shim: addons that call Thirst Was Taken classes directly, such as Green Feathers, need a version built for Blue Droplets. A stable public API is planned. `[1.21.1]`
- `RegisterThirstValueEvent` is now posted every time the tables are built (world load and `/reload`), on the game bus, from the logical side that owns the data; `addDrink`, `addFood` and both `addContainer` methods keep their signatures. Its constructor changed and `ThirstEventFactory` was removed. `[1.21.1]`
- New `ThirstHelper.drinkTable()`/`foodTable()` (resolved, immutable) and `WaterPurity.defaultPurity()` (the server's value on remote clients). `[1.21.1]`
- `RegisterThirstValueEvent` now gets empty maps; its entries are merged after the config and the data map, so they can no longer add an item to the other list (drink vs food) of an item that already has values. `[1.21.1]`
- `ThirstHelper.drinkTable()`/`foodTable()` values are now `{thirst, quenched, purity}` (purity -1 when unset); new `ThirstHelper.getDrinkPurity(ItemStack)`. `WaterPurity.getPurity(ItemStack)` falls back to the data map purity when the stack stores none. Network protocol version is now `0.1.7` (the `bluedroplets:thirst_values` packet carries the purity). `[1.21.1]`
- Removed `ThirstHelper.VALID_DRINKS`, `VALID_FOODS`, `containers`, `init()` and the `keyword*` fields, plus `LoadedValue` and `ConfigHelper`; use `ThirstHelper.isDrink/isFood/getThirst/getQuenched`. `WaterPurity.addContainer` still works (deprecated). `[1.21.1]`
- Network protocol version is now `0.1.8`: the thirst sync packet carries the synced rules as bit flags (including hand drinking) and the `bluedroplets:thirst_values` packet carries the water bottle stack size; `WaterPurity.setServerDefaultPurity` was replaced by `foundation.config.SyncedValues`. `[1.21.1]`
- Network protocol version `0.1.6`: the thirst sync packet carries the sprint and two-hands rules, the drink-by-hand packet no longer carries a position, and a new `bluedroplets:thirst_values` packet carries the server's drink tables. Client and server must run the same Blue Droplets version. `ClientConfig.DRINK_BOTH_HAND_NEEDED` is now `CommonConfig.DRINK_BOTH_HAND_NEEDED`. Fixed gameplay numbers live in `core.ThirstConstants`. `[1.21.1]`
- Removed `ThirstBarRenderer.PLAYER_THIRST` and `compat.appleskin.ThirstValues`; `ThirstBarRenderer.cancelRender` is a `boolean` and `THIRST_ICONS` is final. `[1.21.1]`
- Water containers are looked up by their filled item: a predicate set with `ContainerWithPurity.setEqualsFilled` is only tested with stacks of that container's filled item, and changing the filled item after registering has no effect. Removed the unused `WaterPurity.isEmptyWaterContainer` and `WaterPurity.getFilledContainer`. `[1.21.1]`
- `IThirst.updateThirstData` no longer sends a packet right away: it asks for a sync at the end of the player's tick. `addExhaustion` no longer syncs. `core.ThirstConstants.SYNC_INTERVAL_TICKS` is now `PASSIVE_REGEN_INTERVAL_TICKS` (rain drinking and Peaceful regeneration). `[1.21.1]`
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
