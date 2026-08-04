# Blue Droplets scripts for KubeJS

Short examples of what a modpack can do with the Blue Droplets API from KubeJS 8 (Minecraft 26.1, NeoForge).
Copy `server_scripts/` and `startup_scripts/` into the `kubejs/` folder of your instance, or run `./gradlew runGameTestServer -PwithKubeJS` in the mod's repository, which copies them into `run/kubejs-test/kubejs/` and runs the KubeJS game test.

- `startup_scripts/blue_droplets_drinks.js`: a new drink item registered with its thirst values.
- `server_scripts/blue_droplets_thirst.js`: read and change a player's thirst, hydrate when eating.
- `server_scripts/blue_droplets_purity.js`: read the purity of an item and of the water in the world.
- `server_scripts/blue_droplets_events.js`: react to drink and thirst events, add a thirst loss modifier.

The full API is documented in the wiki page "Mod developers".

Top level `const` and `let` are shared by every script of the same type in KubeJS, so two files declaring `const DropletsAPI` fail with "redeclaration of const". The examples use `var` for the classes they load.
