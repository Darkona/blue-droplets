# Mod developers

Droplets of Thirst has a small public API in `com.darkona.dropletsofthirst.api`. It ships inside the mod jar and alone as
`droplets-of-thirst-api` (with sources) so you can compile against it without pulling in the mod's internals. Everything
outside `com.darkona.dropletsofthirst.api` is internal and may change in any version.

`DropletsAPI` is the place to start; every public method is documented. `DropletsAPI.API_VERSION` goes up when the API
changes incompatibly.

## Dependency

There is no public Maven repository. Each [GitHub release](https://github.com/Darkona/droplets-of-thirst/releases) has the mod jar and the API jar, each with a sources jar. Use those jars as files, or build Droplets of Thirst from the tag of your version and publish it locally (`./gradlew publishToMavenLocal`). The version is `<minecraft>-<mod version>`, for example `1.21.1-1.0.0`, as in the jar name.

```groovy
repositories {
    mavenLocal()
}

dependencies {
    // The API only: nothing of Droplets of Thirst's internals is on your compile classpath.
    compileOnly "com.darkona.dropletsofthirst:droplets-of-thirst-api:${droplets_of_thirst_version}"
    // The whole mod, to run it in your dev environment (optional).
    localRuntime "com.darkona.dropletsofthirst:droplets-of-thirst:${droplets_of_thirst_version}"
}
```

In `neoforge.mods.toml`, declare Droplets of Thirst as optional:

```toml
[[dependencies.yourmod]]
modId = "droplets_of_thirst"
type = "optional"
versionRange = "[0,)"
ordering = "NONE"
side = "BOTH"
```

### Soft dependency

Keep every call in a class that is only loaded when Droplets of Thirst is installed, and guard the entry point:

```java
if (ModList.get().isLoaded(DropletsAPI.MOD_ID)) {   // or the literal "droplets_of_thirst"
    DropletsCompat.init();
}
```

Referencing `DropletsAPI.MOD_ID` inlines the string at compile time, so it is safe outside that class too.

## Sides

- **Reading** works on both sides. A client sees its own player's values as last synced by the server.
- **Changing** thirst is server side. On a client, `setThirst`, `drink` and the other changes do nothing, return
  `false` and log one warning.
- **Events** are posted on the server only.

## Reading

```java
DropletsView thirst = DropletsAPI.view(player);  // live view, no copy
int points = thirst.thirst();                    // 0..20 (thirst.maxThirst())
int quenched = thirst.quenched();                // like saturation, never above thirst
float exhaustion = thirst.exhaustion();          // towards the next point lost
boolean on = thirst.isEnabled();                 // /droplets_of_thirst enable
float multiplier = thirst.lastModifier();        // cached climate/armor/effects/mods multiplier
```

Item values and purity:

```java
ThirstValues values = DropletsAPI.getDrinkValues(stack);  // null if it restores nothing
if (values != null && !values.estimated()) { ... }       // estimated = guessed from recipes

int purity = DropletsAPI.getPurity(stack);               // MIN_PURITY (0) .. MAX_PURITY (5)
PurityLevel named = PurityLevel.byLevel(purity);          // CONTAMINATED .. PURE, null if out of range
ItemStack pure = DropletsAPI.withPurity(stack, PurityLevel.PURE.level());  // a copy
int here = DropletsAPI.getWaterPurity(level, pos);       // server side; poured water keeps its purity
```

Purity is an int from 0 to 5; `PurityLevel` names the levels and gives each its id (`contaminated`, `dirty`, `murky`, `acceptable`, `clean`, `pure`, as in the config keys), its translation key (`droplets_of_thirst.purity.<id>`) and the colour Droplets of Thirst uses for its name.

| Level | `PurityLevel` | Default effects when drunk |
|---|---|---|
| 0 | `CONTAMINATED` | Nausea and Hunger, 40% Poison (blocks hydration) |
| 1 | `DIRTY` | 60% Nausea and Hunger, 15% Poison (blocks hydration) |
| 2 | `MURKY` | 25% Nausea and Hunger |
| 3 | `ACCEPTABLE` | 5% Nausea and Hunger; `defaultPurity` of water with none stored |
| 4 | `CLEAN` | Nothing |
| 5 | `PURE` | Nothing, and the `pureWater` bonus for water |

Purity can be turned off (`purity.toml` `general.enabled = false`, synced to clients). Then `isPurityEnabled()` is `false`, `withPurity` returns an unchanged copy, drinking never rolls purity effects and `DrinkEvent` carries `NO_PURITY`. `getPurity` and `getWaterPurity` still answer (`defaultPurity` for most water), so check `isPurityEnabled()` before letting a purity change anything in your mod.

## Changing thirst (server)

```java
DropletsAPI.setThirst(player, 10);            // clamped; quenched follows down
DropletsAPI.addThirst(player, -2, 0);         // not a drink: no effects, no drink events
DropletsAPI.drink(player, 4, 2);              // like drinking: extra thirst may become quenched
DropletsAPI.drink(player, 4, 2, PurityLevel.DIRTY.level());  // also rolls the purity effects
DropletsAPI.addExhaustion(player, 0.5f);      // multiplied like Droplets of Thirst's own activities
```

All of these keep `0 <= quenched <= thirst <= 20`, post the events below and reach the client with the next sync.

## Registering drinks

Call these once, from your mod constructor or common setup. Items are resolved each time the tables are built (world
load and `/reload`), so `DeferredItem`s are fine. Players and modpacks keep the last word: `items.toml`, the
`#droplets_of_thirst:no_thirst` tag and the `droplets_of_thirst:drinks` data map override what code registers (see
[Modpack makers](Modpack-Makers#where-an-items-values-come-from)). If your values fit in a datapack, prefer shipping
a `droplets_of_thirst:drinks` data map entry instead.

```java
DropletsAPI.registerDrink(MyItems.LEMONADE, 6, 4);                          // thirst, quenched
DropletsAPI.registerDrink(MyItems.SPRING_WATER, 6, 2, PurityLevel.CLEAN.level()); // with a purity for its effects
```

Whether an item hydrates as food or as a drink follows the item: if it can be eaten, when eaten.

Values are points, like thirst: 2 points = 1 droplet on the HUD (20 points = 10 droplets). Thirst goes from -20 to 20
and quenched from -20 up; negative values make the item **salty**, so eating or drinking it removes thirst and quenched:

```java
DropletsAPI.registerDrink(MyItems.SALTED_FISH, -2, -2);
```

`ThirstValues.saltiness()`, `DrinkEvent#getSaltiness()` and `EatEvent#getSaltiness()` give the thirst points an item removes (`-thirst`, or 0 when
it is not salty); `isSalty()` is `getSaltiness() > 0`. On the `Pre` events it follows `setThirst`, so a listener that
makes a drink salty sets a negative thirst.

To hydrate from your own code without an item, `DropletsAPI.drink(player, thirst, quenched[, purity])` posts
`DrinkEvent` (and rolls purity effects) and `DropletsAPI.eat(player, thirst, quenched)` posts `EatEvent`.

### Values that depend on the stack

For items whose value depends on their components (a flask with a fluid, a potion, a tank), register a provider for
the item. It is called on both sides, often (tooltips, the HUD): return cached instances.

```java
private static final ThirstValues FULL = new ThirstValues(8, 4);
private static final ThirstValues HALF = new ThirstValues(4, 2);

DropletsAPI.registerDrinkProvider(MyItems.FLASK, stack -> switch (Flask.charges(stack)) {
    case 0 -> null;          // nothing to drink
    case 1 -> HALF;
    default -> FULL;
});
```

A provider comes after `registerDrink` in precedence; keywords and recipe estimates never apply to its item.

### Water containers

Items that hold water with purity (the tooltip shows it and drinking rolls its effects):

```java
DropletsAPI.registerContainer(MyItems.CANTEEN, MyItems.WATER_CANTEEN);  // empty, filled
DropletsAPI.registerContainer(MyItems.WATER_SKIN);                      // filled only: drunk, never filled from the world
```

Filling them from the world is up to your item; give the result a purity with `DropletsAPI.withPurity`.

## Thirst loss

Prefer the attribute `droplets_of_thirst:thirst_drain` (a multiplier, base 1.0): equipment, effects and enchantments change
it with ordinary attribute modifiers and no code on your side.

For rules the attribute can't express (additive terms, your own climate or seasons), register a modifier. It runs on the
server about once a second per player and when armor, effects or the dimension change, never every tick; the result
is cached. Modifiers run in id order and receive the multiplier so far:

```java
DropletsAPI.registerExhaustionModifier(ResourceLocation.fromNamespaceAndPath("mymod", "season"),
        (player, multiplier) -> Seasons.isSummer(player.level()) ? multiplier * 1.25f : multiplier);

// When what your modifier reads changes, don't wait for the next refresh:
DropletsAPI.refreshExhaustionModifier(player);
```

`/droplets_of_thirst debug exhaustion` shows the combined effect of registered modifiers as "other mods".

## Thirst bar colour

The droplets are tinted while a style is active, like vanilla hunger turns green under Hunger. Built-in styles and
their priorities: vampire (Vampirism, Supernatural) 400, Dehydration 300, Overhydrated 250, Poison 200, Quenchness 100, Hydrated 50; the active style
with the highest priority wins. Add your own on the client, for example in `FMLClientSetupEvent`:

```java
DropletsAPI.registerBarStyle(ResourceLocation.fromNamespaceAndPath("mymod", "frozen"),
        player -> player.hasEffect(MyEffects.FROZEN), 0x9FD8FF, 250);
```

The colour is `0xRRGGBB`. The predicate runs every frame for the local player: keep it cheap and allocation free
(`hasEffect` with a holder is fine). Registering the same id again replaces the style. On a dedicated server the call
does nothing useful but is harmless.

Positive thirst effects can make the bar wave (one droplet at a time bounces, like hearts under Regeneration; built
in: Quenchness, Hydrated). Also on the client:

```java
DropletsAPI.registerWaveEffect(MyEffects.HYDRATED);
```

## Events

All on `NeoForge.EVENT_BUS`, in `com.darkona.dropletsofthirst.api.event`, posted on the server and only when something
happens (never once per tick). Listen to `Pre`/`Post`, not to the abstract base classes.

| Event | When | You can |
|---|---|---|
| `ThirstChangeEvent.Pre` | thirst or quenched is about to change; `getCause()`: `DEPLETION`, `DRINK`, `EAT`, `RAIN`, `PEACEFUL`, `DEATH`, `COMMAND`, `API` | cancel, `setNewThirst`, `setNewQuenched` |
| `ThirstChangeEvent.Post` | after the change | read old and new values |
| `EatEvent.Pre` | before food hydrates: food items (no drink animation, not a water container; also `Player#eat` called by other mods), block foods (`droplets_of_thirst:hydrating_blocks`, `getItem()` empty) and `DropletsAPI.eat`; `getSaltiness()`/`isSalty()` | cancel, `setThirst`, `setQuenched` |
| `EatEvent.Post` | after it | read the values and `hydrated()` |
| `DrinkEvent.Pre` | before a drink's purity effects and hydration: items with the drink animation (potions, milk, honey bottle, most modded drinks), water containers, hand drinking and `DropletsAPI.drink`; `getItem()` is empty for hand drinking and `DropletsAPI.drink`; `getSaltiness()`/`isSalty()` for values that remove thirst | cancel, `setThirst`, `setQuenched`, `setPurity` (negative values remove thirst) |
| `DrinkEvent.Post` | after it | read the values and `hydrated()` |
| `PurityEffectEvent` | purity effects were rolled; the Dehydration of `hotDirtyWater` (murky or dirtier water in a hot climate) is already in `getEffects()` | cancel (no effects, hydrates), edit `getEffects()`, `setHydrates` |
| `DehydrationDamageEvent` | a player at zero thirst is about to be hurt | cancel, `setAmount` |
| `OverhydrationEvent` | a player drank past full until the overflow reached `overhydration.threshold` and is about to get Overhydrated (the overflow resets either way) | cancel, `setDuration`, `setAmplifier`; read `getOverflow()` |

```java
@SubscribeEvent
public static void noThirstInTheHub(ThirstChangeEvent.Pre event) {
    if (event.getCause() == ThirstChangeEvent.Cause.DEPLETION && isHub(event.getEntity().level()))
        event.setCanceled(true);
}

@SubscribeEvent
public static void coldWaterRefreshes(DrinkEvent.Pre event) {
    if (event.getPurity() == PurityLevel.PURE.level())
        event.setQuenched(event.getQuenched() + 2);
}

@SubscribeEvent
public static void saltLovers(EatEvent.Pre event) {
    if (event.isSalty() && isMerfolk(event.getEntity()))
        event.setThirst(event.getSaltiness()); // salty food hydrates them instead
}

@SubscribeEvent
public static void immuneToDirtyWater(PurityEffectEvent event) {
    if (hasIronStomach(event.getEntity()))
        event.setCanceled(true);
}
```

## KubeJS

Modpacks can use the API from KubeJS 7 scripts, with no Java. Load the classes with `Java.loadClass` and listen to the events with `NativeEvents.onEvent`. Working scripts are in `examples/kubejs` in the repository; the tests of `./gradlew runGameTestServer -PwithKubeJS` run them inside KubeJS 2101.7.2 (build 377).

```js
var DropletsAPI = Java.loadClass('com.darkona.dropletsofthirst.api.DropletsAPI')
var PurityLevel = Java.loadClass('com.darkona.dropletsofthirst.api.PurityLevel')

// Read and change thirst (server scripts)
ItemEvents.rightClicked('minecraft:clock', event => {
  let thirst = DropletsAPI.view(event.player)
  event.player.tell(`Thirst ${thirst.thirst()}/${thirst.maxThirst()}`)
  if (event.player.shiftKeyDown) DropletsAPI.setThirst(event.player, 20)
})

// Hydrate when eating
ItemEvents.foodEaten('minecraft:golden_carrot', event => {
  if (event.entity.isPlayer()) DropletsAPI.drink(event.entity, 4, 2)
})

// Read purity: DropletsAPI.getItemPurity(stack), DropletsAPI.getWaterPurity(level, pos)

// Events: nested classes are loaded with a $
var DrinkPre = Java.loadClass('com.darkona.dropletsofthirst.api.event.DrinkEvent$Pre')
NativeEvents.onEvent(DrinkPre, event => {
  if (event.purity <= PurityLevel.DIRTY.level() && event.entity.level.dimension.toString() == 'minecraft:the_nether')
    event.canceled = true
})

// Register a drink in a startup script, after the item exists
StartupEvents.postInit(() => {
  DropletsAPI.registerDrink(Item.of('kubejs:lemonade').item, 6, 4, PurityLevel.CLEAN.level())
})
```

- Use `getItemPurity`, `getFluidPurity`, `withItemPurity` and `withFluidPurity` instead of the `getPurity` and `withPurity` overloads: KubeJS cannot choose between an `ItemStack` and a `FluidStack` and fails with "the choice of Java method is ambiguous".
- Top level `const` and `let` are shared by all scripts of one type, so declare the loaded classes with `var` when several files load the same one.
- Registered drinks belong in `startup_scripts` (`StartupEvents.postInit`, when the items exist). `items.toml`, the `droplets_of_thirst:drinks` data map and `#droplets_of_thirst:no_thirst` still win over them.
- Exhaustion modifiers take a JavaScript function and a string id: `DropletsAPI.registerExhaustionModifier('kubejs:night', (player, multiplier) => player.level.isNight() ? multiplier * 0.75 : multiplier)`.
- In KubeJS, `entity.level` and `level.dimension` are properties, not methods.

## Deprecated

`RegisterThirstValueEvent` (posted on each table rebuild) still works but is not in the API jar; use
`registerDrink`, `registerDrinkProvider` and `registerContainer`. `IThirst`, `PlayerThirst`, `ModAttachment` and
`ThirstHelper` are internal: read through `DropletsAPI.view` and change thirst through `DropletsAPI`, which keeps
the invariants and posts the events.
