package dev.ghen.thirst.compat.coldsweat;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.world.entity.player.Player;

final class ColdSweatBridge
{
    private ColdSweatBridge() {}

    static double bodyTemperature(Player player)
    {
        return Temperature.get(player, Temperature.Trait.BODY);
    }
}
