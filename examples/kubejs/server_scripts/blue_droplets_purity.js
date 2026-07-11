// Purity goes from 0 (contaminated) to 5 (pure). PurityLevel names the levels.

var DropletsAPI = Java.loadClass('com.darkona.droplets.api.DropletsAPI')
var PurityLevel = Java.loadClass('com.darkona.droplets.api.PurityLevel')

// Right-click a water bottle to see its purity, the water values of the item and the water where you stand.
ItemEvents.rightClicked('minecraft:potion', event => {
  const stack = event.item
  const player = event.player
  if (!DropletsAPI.isPurityEnabled()) return    // getPurity still answers with purity off, but it means nothing

  const level = DropletsAPI.getItemPurity(stack)          // getFluidPurity(fluidStack) for fluids
  const named = PurityLevel.byLevel(level)                // null if out of range
  player.tell(`This bottle is ${named.id()} (${level}/${PurityLevel.MAX})`)

  // What the item restores, resolved from config, datapacks and registered drinks; null if nothing
  const values = DropletsAPI.getDrinkValues(stack)
  if (values != null) player.tell(`Restores thirst ${values.thirst()}, quenched ${values.quenched()}`)

  // Purity of the water at a position: server side only (biome and dimension data are not synced).
  // Water poured with a bucket keeps the purity of what was poured.
  const here = DropletsAPI.getWaterPurity(event.level, player.blockPosition())
  player.tell(`The water here is ${PurityLevel.byLevel(here).id()}`)
})

// Writing purity: withItemPurity returns a copy. (withPurity(stack, level) also exists, but KubeJS cannot choose between
// its ItemStack and FluidStack overloads: it fails with "the choice of Java method is ambiguous".)
ItemEvents.rightClicked('minecraft:glass_bottle', event => {
  const pure = DropletsAPI.withItemPurity(Item.of('minecraft:potion', '[potion_contents={potion:"minecraft:water"}]'),
    PurityLevel.PURE.level())
  event.player.give(pure)
})
