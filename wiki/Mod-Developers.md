# Mod developers

Blue Droplets has a small public API in `com.darkona.droplets.api`. It ships inside the mod jar and alone as
`bluedroplets-api` (with sources) so you can compile against it without pulling in the mod's internals. Everything
outside `com.darkona.droplets.api` is internal and may change in any version.

`DropletsAPI` is the place to start; every public method is documented. `DropletsAPI.API_VERSION` goes up when the API
changes incompatibly.

## Dependency

For now the artifacts are only published to the local Maven repository of the project (`mcmodsrepo`); a public Maven
is still to be decided. Until then, build Blue Droplets and publish it locally (`./gradlew publishToMavenLocal`), or use
the jars from `build/libs/`.

```groovy
repositories {
    mavenLocal()
}

dependencies {
    // The API only: nothing of Blue Droplets' internals is on your compile classpath.
    compileOnly "com.darkona.droplets:bluedroplets-api:${bluedroplets_version}"
    // The whole mod, to run it in your dev environment (optional).
    localRuntime "com.darkona.droplets:BlueDroplets:${bluedroplets_version}"
}
```

In `neoforge.mods.toml`, declare Blue Droplets as optional:

```toml
[[dependencies.yourmod]]
modId = "bluedroplets"
type = "optional"
versionRange = "[0,)"
ordering = "NONE"
side = "BOTH"
```

### Soft dependency

Keep every call in a class that is only loaded when Blue Droplets is installed, and guard the entry point:

```java
if (ModList.get().isLoaded(DropletsAPI.MOD_ID)) {   // or the literal "bluedroplets"
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
boolean on = thirst.isEnabled();                 // /bluedroplets enable
float multiplier = thirst.lastModifier();        // cached climate/armor/effects/mods multiplier
```

Item values and purity:

```java
ThirstValues values = DropletsAPI.getDrinkValues(stack);  // null if it restores nothing
if (values != null && !values.estimated()) { ... }       // estimated = guessed from recipes

int purity = DropletsAPI.getPurity(stack);               // DIRTY (0) .. PURIFIED (3)
ItemStack clean = DropletsAPI.withPurity(stack, DropletsAPI.PURIFIED);  // a copy
int here = DropletsAPI.getWaterPurity(level, pos);       // server side
```

## Changing thirst (server)

```java
DropletsAPI.setThirst(player, 10);            // clamped; quenched follows down
DropletsAPI.addThirst(player, -2, 0);         // not a drink: no effects, no drink events
DropletsAPI.drink(player, 4, 2);              // like drinking: extra thirst may become quenched
DropletsAPI.drink(player, 4, 2, DropletsAPI.DIRTY);  // also rolls the purity effects
DropletsAPI.addExhaustion(player, 0.5f);      // multiplied like Blue Droplets' own activities
```

All of these keep `0 <= quenched <= thirst <= 20`, post the events below and reach the client with the next sync.

## Registering drinks

Call these once, from your mod constructor or common setup. Items are resolved each time the tables are built (world
load and `/reload`), so `DeferredItem`s are fine. Players and modpacks keep the last word: `items.toml`, the
`#bluedroplets:no_thirst` tag and the `bluedroplets:drinks` data map override what code registers (see
[Modpack makers](Modpack-Makers.md#where-an-items-values-come-from)). If your values fit in a datapack, prefer shipping
a `bluedroplets:drinks` data map entry instead.

```java
DropletsAPI.registerDrink(MyItems.LEMONADE, 6, 4);                          // thirst, quenched
DropletsAPI.registerDrink(MyItems.SPRING_WATER, 6, 2, DropletsAPI.PURIFIED); // with a purity for its effects
```

Whether an item hydrates as food or as a drink follows the item: if it can be eaten, when eaten.

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

Prefer the attribute `bluedroplets:thirst_drain` (a multiplier, base 1.0): equipment, effects and enchantments change
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

`/bluedroplets debug exhaustion` shows the combined effect of registered modifiers as "other mods".

## Events

All on `NeoForge.EVENT_BUS`, in `com.darkona.droplets.api.event`, posted on the server and only when something
happens (never once per tick). Listen to `Pre`/`Post`, not to the abstract base classes.

| Event | When | You can |
|---|---|---|
| `ThirstChangeEvent.Pre` | thirst or quenched is about to change; `getCause()`: `DEPLETION`, `DRINK`, `RAIN`, `PEACEFUL`, `DEATH`, `COMMAND`, `API` | cancel, `setNewThirst`, `setNewQuenched` |
| `ThirstChangeEvent.Post` | after the change | read old and new values |
| `DrinkEvent.Pre` | before a drink's purity effects and hydration; `getItem()` is empty for hand drinking and `DropletsAPI.drink` | cancel, `setThirst`, `setQuenched`, `setPurity` |
| `DrinkEvent.Post` | after it | read the values and `hydrated()` |
| `PurityEffectEvent` | purity effects were rolled | cancel (no effects, hydrates), edit `getEffects()`, `setHydrates` |
| `DehydrationDamageEvent` | a player at zero thirst is about to be hurt | cancel, `setAmount` |

```java
@SubscribeEvent
public static void noThirstInTheHub(ThirstChangeEvent.Pre event) {
    if (event.getCause() == ThirstChangeEvent.Cause.DEPLETION && isHub(event.getEntity().level()))
        event.setCanceled(true);
}

@SubscribeEvent
public static void coldWaterRefreshes(DrinkEvent.Pre event) {
    if (event.getPurity() == DropletsAPI.PURIFIED)
        event.setQuenched(event.getQuenched() + 2);
}

@SubscribeEvent
public static void immuneToDirtyWater(PurityEffectEvent event) {
    if (hasIronStomach(event.getEntity()))
        event.setCanceled(true);
}
```

## Deprecated

`RegisterThirstValueEvent` (posted on each table rebuild) still works but is not in the API jar; use
`registerDrink`, `registerDrinkProvider` and `registerContainer`. `IThirst`, `PlayerThirst`, `ModAttachment` and
`ThirstHelper` are internal: read through `DropletsAPI.view` and change thirst through `DropletsAPI`, which keeps
the invariants and posts the events.
