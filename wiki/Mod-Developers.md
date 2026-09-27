# Mod developers

Droplets of Thirst has a small public API in `com.darkona.dropletsofthirst.api`. It ships inside the mod jar and alone as `droplets-of-thirst-api` (with sources), so you can compile against it without the mod's internals on your classpath. Everything outside `com.darkona.dropletsofthirst.api` is internal and may change in any version.

`DropletsAPI` is the place to start, and every public method is documented. `DropletsAPI.API_VERSION` (1) goes up when the API changes incompatibly.

This page shows the API on Minecraft 26.3 (NeoForge, Mojang names, `Identifier`). The classes and methods are the same on every version, with the types of each Minecraft version: see the page of your version on the [home page](Home).

---

## Dependency

For now the artifacts are only published to the project's local Maven repository (`mcmodsrepo`). A public Maven is not decided yet. Until then, build Droplets of Thirst and publish it locally (`./gradlew publishToMavenLocal`), or use the jars from `build/libs/`. The version is `beta-26.3-1.0.0`.

```groovy
repositories {
    mavenLocal()
}

dependencies {
    // The API only: nothing of Droplets of Thirst's internals is on your compile classpath.
    compileOnly "com.darkona.dropletsofthirst:droplets-of-thirst-api:beta-26.3-1.0.0"
    // The whole mod, to run it in your dev environment (optional).
    localRuntime "com.darkona.dropletsofthirst:droplets-of-thirst:beta-26.3-1.0.0"
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

`DropletsAPI.MOD_ID` is a compile-time constant, so the string is inlined and referencing it is safe outside that class too.

---

## Sides

- **Reading** works on both sides. A client sees its own player's values as last synced by the server.
- **Changing** thirst is server side. On a client, `setThirst`, `drink` and the other changes do nothing, return `false` and log one warning.
- **Events** are posted on the server only.

---

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

`getItemPurity`, `getFluidPurity`, `withItemPurity` and `withFluidPurity` do the same as the `getPurity` and `withPurity` overloads, for callers that cannot pick an overload by type (scripting languages).

Purity is an int from 0 to 5. `PurityLevel` names the levels and gives each its id (`contaminated`, `dirty`, `murky`, `acceptable`, `clean`, `pure`, as in the config keys), its translation key (`droplets_of_thirst.purity.<id>`) and the colour Droplets of Thirst uses for its name.

| Level | `PurityLevel` | Default effects when drunk |
|---|---|---|
| 0 | `CONTAMINATED` | Nausea and Hunger, 40% Poison (blocks hydration) |
| 1 | `DIRTY` | 60% Nausea and Hunger, 15% Poison (blocks hydration) |
| 2 | `MURKY` | 25% Nausea and Hunger |
| 3 | `ACCEPTABLE` | 5% Nausea and Hunger. `defaultPurity` of water with none stored |
| 4 | `CLEAN` | Nothing |
| 5 | `PURE` | Nothing, and the `pureWater` bonus for water |

Purity can be turned off (`purity.toml` `general.enabled = false`, synced to clients). Then `isPurityEnabled()` is `false`, `withPurity` returns an unchanged copy, drinking never rolls purity effects and `DrinkEvent` carries `DropletsAPI.NO_PURITY`. `getPurity` and `getWaterPurity` still answer (`defaultPurity` for most water), so check `isPurityEnabled()` before a purity changes anything in your mod.

---

## Changing thirst (server)

```java
DropletsAPI.setThirst(player, 10);            // clamped; quenched follows down
DropletsAPI.setQuenched(player, 5);           // clamped to thirst
DropletsAPI.addThirst(player, -2, 0);         // not a drink: no effects, no drink events
DropletsAPI.drink(player, 4, 2);              // like drinking: extra thirst may become quenched
DropletsAPI.drink(player, 4, 2, PurityLevel.DIRTY.level());  // also rolls the purity effects
DropletsAPI.eat(player, 2, 2);                // like eating: posts EatEvent
DropletsAPI.addExhaustion(player, 0.5f);      // multiplied like Droplets of Thirst's own activities
```

All of these keep `0 <= quenched <= thirst <= 20`, post the events below and reach the client with the next sync.

---

## Registering drinks

Call these once, from your mod constructor or common setup. Items are resolved each time the tables are built (world load and `/reload`), so `DeferredItem`s are fine. Players and modpacks keep the last word: `items.toml`, the `#droplets_of_thirst:no_thirst` tag and the `droplets_of_thirst:drinks` data map win over what code registers (see [Modpack makers](Modpack-Makers#where-an-items-values-come-from)). If your values fit in a datapack, ship a `droplets_of_thirst:drinks` data map entry instead.

```java
DropletsAPI.registerDrink(MyItems.LEMONADE, 6, 4);                                // thirst, quenched
DropletsAPI.registerDrink(MyItems.SPRING_WATER, 6, 2, PurityLevel.CLEAN.level()); // with a purity for its effects
```

Whether an item hydrates as food or as a drink follows the item: if it can be eaten, when eaten.

Values are points, like thirst: 2 points = 1 droplet on the HUD. Thirst goes from -20 to 20 and quenched from -20 up. Negative values make the item **salty**, so eating or drinking it removes thirst and quenched:

```java
DropletsAPI.registerDrink(MyItems.SALTED_FISH, -2, -2);
```

`ThirstValues.saltiness()`, `DrinkEvent#getSaltiness()` and `EatEvent#getSaltiness()` give the thirst points an item removes (`-thirst`, or 0 when it is not salty). `isSalty()` is `getSaltiness() > 0`. On the `Pre` events it follows `setThirst`, so a listener that makes a drink salty sets a negative thirst.

### Values that depend on the stack

For items whose value depends on their components (a flask with a fluid, a potion, a tank), register a provider for the item. It is called on both sides and often (tooltips, the HUD), so return cached instances.

```java
private static final ThirstValues FULL = new ThirstValues(8, 4);
private static final ThirstValues HALF = new ThirstValues(4, 2);

DropletsAPI.registerDrinkProvider(MyItems.FLASK, stack -> switch (Flask.charges(stack)) {
    case 0 -> null;          // nothing to drink
    case 1 -> HALF;
    default -> FULL;
});
```

A provider comes after `registerDrink` in precedence. Keywords and recipe estimates never apply to its item.

### Water containers

Items that hold water with a purity (the tooltip shows it and drinking rolls its effects):

```java
DropletsAPI.registerContainer(MyItems.CANTEEN, MyItems.WATER_CANTEEN);  // empty, filled
DropletsAPI.registerContainer(MyItems.WATER_SKIN);                      // filled only: drunk, never filled from the world
```

Filling them from the world is up to your item. Give the result a purity with `DropletsAPI.withPurity`, and read the water's purity with `DropletsAPI.getWaterPurity`.

---

## Thirst loss

Prefer the attribute `droplets_of_thirst:thirst_drain` (a multiplier, base 1.0): equipment, effects and enchantments change it with plain attribute modifiers and no code on your side.

For rules the attribute cannot express (additive terms, your own climate or seasons), register a modifier. It runs on the server about once a second per player, and when armor, effects or the dimension change, never every tick. The result is cached. Modifiers run in id order and receive the multiplier so far:

```java
DropletsAPI.registerExhaustionModifier(Identifier.fromNamespaceAndPath("mymod", "season"),
        (player, multiplier) -> Seasons.isSummer(player.level()) ? multiplier * 1.25f : multiplier);

// When what your modifier reads changes, don't wait for the next refresh:
DropletsAPI.refreshExhaustionModifier(player);
```

`/droplets_of_thirst debug exhaustion` shows the combined effect of registered modifiers as "other mods".

---

## Thirst bar

The thirst bar is the GUI layer `droplets_of_thirst:thirst_level`, registered right above `VanillaGuiLayers.FOOD_LEVEL`. It hides with the rest of the HUD. Other layers that stack above the food bar (air, mounts, other mods) move up with it.

The droplets are tinted while a style is active, like vanilla hunger turns green under Hunger. Built-in styles and their priorities: Dehydration 300, Overhydrated 250, Poison 200, Quenchness 100, Hydrated 50. The active style with the highest priority wins. Add your own on the client, for example in `FMLClientSetupEvent`:

```java
DropletsAPI.registerBarStyle(Identifier.fromNamespaceAndPath("mymod", "frozen"),
        player -> player.hasEffect(MyEffects.FROZEN), 0x9FD8FF, 250);
```

The colour is `0xRRGGBB`. The predicate runs every frame for the local player, so keep it cheap and allocation free (`hasEffect` with a holder is fine). Registering the same id again replaces the style. On a dedicated server the call does nothing.

Positive thirst effects can make the bar wave, one droplet bouncing at a time like hearts under Regeneration (built in: Quenchness, Hydrated). Also on the client:

```java
DropletsAPI.registerWaveEffect(MyEffects.HYDRATED);   // a Holder<MobEffect>
```

---

## Events

All on `NeoForge.EVENT_BUS`, in `com.darkona.dropletsofthirst.api.event`, posted on the server and only when something happens (never once per tick). Listen to `Pre`/`Post`, not to the abstract base classes.

| Event | When | You can |
|---|---|---|
| `ThirstChangeEvent.Pre` | Thirst or quenched is about to change. `getCause()`: `DEPLETION`, `DRINK`, `EAT`, `RAIN`, `PEACEFUL`, `DEATH`, `COMMAND`, `API` | cancel, `setNewThirst`, `setNewQuenched` |
| `ThirstChangeEvent.Post` | After the change | read old and new values |
| `EatEvent.Pre` | Before food hydrates: food items (not drunk and not a water container), block foods of `droplets_of_thirst:hydrating_blocks` (`getItem()` empty) and `DropletsAPI.eat`. `getSaltiness()`, `isSalty()` | cancel, `setThirst`, `setQuenched` |
| `EatEvent.Post` | After it | read the values and `hydrated()` |
| `DrinkEvent.Pre` | Before a drink's purity effects and hydration: items drunk (potions, milk, honey bottle, most modded drinks), water containers, hand drinking and `DropletsAPI.drink`. `getItem()` is empty for hand drinking and `DropletsAPI.drink` | cancel, `setThirst`, `setQuenched`, `setPurity` |
| `DrinkEvent.Post` | After it | read the values and `hydrated()` |
| `PurityEffectEvent` | Purity effects were rolled. The Dehydration of `hotDirtyWater` is already in `getEffects()` | cancel (no effects, hydrates), edit `getEffects()`, `setHydrates` |
| `DehydrationDamageEvent` | A player at zero thirst is about to be hurt | cancel, `setAmount` |
| `OverhydrationEvent` | A player drank past full until the overflow reached `overhydration.threshold` and is about to get Overhydrated (the overflow resets either way) | cancel, `setDuration`, `setAmplifier`, read `getOverflow()` |

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

---

## Transfer API

Water carries its purity as the `droplets_of_thirst:purity` data component, on item stacks and on fluids. Through NeoForge's transfer API (`ResourceHandler`, `FluidResource`), the water of a bucket and the water a cauldron reports carry their purity, so a pump or pipe that drains a water cauldron gets murky water (clean over a heat source). Keep the component when you move water between tanks and it keeps its purity.

---

## Deprecated

`RegisterThirstValueEvent` (posted on each table rebuild) still works but is not in the API jar: use `registerDrink`, `registerDrinkProvider` and `registerContainer`. `IThirst`, `PlayerThirst`, `ModAttachment` and `ThirstHelper` are internal. Read through `DropletsAPI.view` and change thirst through `DropletsAPI`, which keeps the invariants and posts the events.
