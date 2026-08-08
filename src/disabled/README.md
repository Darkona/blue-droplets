# Disabled integrations

Integrations with mods that have no build for this Minecraft version (26.2): Create, Farmer's Delight and its addons, Cold Sweat, Supernatural and Vampirism. The code, mixins, game tests, recipes, loot, tags, drink values and assets are kept here as they were for Minecraft 1.21.1, so they are not lost.

Nothing under `src/disabled` is compiled or packaged: it is not a Gradle source set (see `build.gradle`). Their mixin entries are in `resources/blue_droplets.compat.mixins.json` and their optional dependencies in `resources/META-INF/neoforge.mods.toml.disabled`. The drink values and item tags of these mods are written here by `scripts/delight/apply.py` (namespaces outside `active_namespaces` in `scripts/delight/mods.json`).

Re-enabling one is a port of its own: move its files back under `src/main`, update it to the mod's API for this Minecraft version, add its dependencies to `build.gradle` and `neoforge.mods.toml`, and run its game tests.
