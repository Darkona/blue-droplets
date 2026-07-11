// Registers a drink from code. Blue Droplets resolves the items when it builds its tables (world load and /reload),
// so the item only has to exist by then.
// Players and modpacks keep the last word: items.toml, the #blue_droplets:no_thirst tag and the blue_droplets:drinks
// data map win over what code registers. If the values fit in a datapack, prefer that.

var DropletsAPI = Java.loadClass('com.darkona.droplets.api.DropletsAPI')
var PurityLevel = Java.loadClass('com.darkona.droplets.api.PurityLevel')

// A new item that is drunk. Values are points: 2 points are one droplet on the HUD.
StartupEvents.registry('item', event => {
  event.create('lemonade')
    .displayName('Lemonade')
    .useAnimation('drink')
    .food(food => food.nutrition(1).saturation(0.1).alwaysEdible())
})

// The item exists now: give it thirst 6 and quenched 4, and a purity for the effects rolled when drinking it.
StartupEvents.postInit(() => {
  const lemonade = Item.of('kubejs:lemonade').item
  DropletsAPI.registerDrink(lemonade, 6, 4, PurityLevel.CLEAN.level())
  console.info('[Blue Droplets example] registered kubejs:lemonade')
})
