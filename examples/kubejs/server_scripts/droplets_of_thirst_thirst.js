// Reading and changing a player's thirst. Changes only work on the server, which is where server_scripts run.

var DropletsAPI = Java.loadClass('com.darkona.dropletsofthirst.api.DropletsAPI')

// Right-click a clock to read your thirst; sneak and right-click to refill it.
ItemEvents.rightClicked('minecraft:clock', event => {
  const player = event.player
  const thirst = DropletsAPI.view(player)   // live view, nothing is copied
  if (player.shiftKeyDown) {
    DropletsAPI.setThirst(player, thirst.maxThirst())        // clamped to 0..20, quenched follows down
    DropletsAPI.setQuenched(player, thirst.maxThirst())      // clamped to 0..thirst
  } else {
    // thirst() and quenched() are points, like food: 20 points = 10 droplets
    player.tell(`Thirst ${thirst.thirst()}/${thirst.maxThirst()}, quenched ${thirst.quenched()}`)
  }
})

// Hydrate when eating something. drink() follows the same rules as a bottle: extra thirst can become quenched,
// and it posts the drink events. Use addThirst() for a change that is not a drink.
// Droplets of Thirst already hydrates food it knows: this is for values a datapack cannot express.
ItemEvents.foodEaten('minecraft:golden_carrot', event => {
  if (event.entity.isPlayer()) DropletsAPI.drink(event.entity, 4, 2)   // the event's entity can be any living entity
})

// Dry air: a sponge dries you out, without drink events or purity effects.
ItemEvents.rightClicked('minecraft:sponge', event => {
  DropletsAPI.addThirst(event.player, -2, 0)
})
