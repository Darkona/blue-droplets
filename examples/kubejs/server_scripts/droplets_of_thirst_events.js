// Droplets of Thirst posts its events on NeoForge's bus, so KubeJS listens with NativeEvents.onEvent. They are posted on
// the server, only when something happens (never once per tick). Listen to Pre and Post: the classes in between are
// abstract. Nested classes are loaded by their binary name, with a $.

var DropletsAPI = Java.loadClass('com.darkona.dropletsofthirst.api.DropletsAPI')
var PurityLevel = Java.loadClass('com.darkona.dropletsofthirst.api.PurityLevel')
const DrinkPre = Java.loadClass('com.darkona.dropletsofthirst.api.event.DrinkEvent$Pre')
const ThirstChangePre = Java.loadClass('com.darkona.dropletsofthirst.api.event.ThirstChangeEvent$Pre')

// Dimensions where nothing but clean water is safe to drink.
const NO_DIRTY_WATER = ['minecraft:the_nether']

// Cancel drinking contaminated or dirty water in those dimensions: nothing is hydrated and no effects are rolled.
// Pre events are cancellable; Post events are not.
NativeEvents.onEvent(DrinkPre, event => {
  const dimension = event.entity.level.dimension.toString()
  if (NO_DIRTY_WATER.includes(dimension) && event.purity >= PurityLevel.MIN && event.purity <= PurityLevel.DIRTY.level()) {
    event.canceled = true
    event.entity.tell('You would not drink that here.')
  }
})

// Pure water refreshes more for players with the tag "hydro" (/tag <player> add hydro). Pre events can change the
// values; getPurity() is -1 for drinks without a purity.
NativeEvents.onEvent(DrinkPre, event => {
  if (event.purity == PurityLevel.PURE.level() && event.entity.tags.contains('hydro')) {
    event.setQuenched(event.quenched + 2)
  }
})

// No thirst loss in a hub. getCause() is DEPLETION, DRINK, EAT, RAIN, PEACEFUL, DEATH, COMMAND or API.
NativeEvents.onEvent(ThirstChangePre, event => {
  if (event.cause.name() == 'DEPLETION' && event.entity.tags.contains('hub')) {
    event.canceled = true
  }
})

// Thirst loss modifiers: (player, multiplier so far) => new multiplier. They run about once a second per player,
// never every tick, and the result is cached. Registering the same id again replaces it.
// After a /reload of scripts, registering again just replaces the previous modifier.
DropletsAPI.registerExhaustionModifier('kubejs:night_watch', (player, multiplier) =>
  player.level.isNight() ? multiplier * 0.75 : multiplier)
