package com.darkona.droplets.foundation.network;

import com.darkona.droplets.content.thirst.PlayerThirst;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.foundation.common.capability.ModAttachment;
import com.darkona.droplets.foundation.config.SyncedValues;
import com.darkona.droplets.foundation.network.message.PlayerThirstSyncMessage;
import com.darkona.droplets.foundation.network.message.ThirstValuesSyncMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Client side of the payloads sent to the client; only loaded there.
 */
public final class ClientPayloadHandlers
{
    private ClientPayloadHandlers() {}

    public static void thirst(PlayerThirstSyncMessage message)
    {
        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;
        PlayerThirst cap = ModAttachment.thirst(player);
        cap.setThirst(message.thirst());
        cap.setQuenched(message.quenched());
        cap.setExhaustion(message.exhaustion());
        cap.setSyncedRules(message.flags());
    }

    public static void values(ThirstValuesSyncMessage message)
    {
        ThirstHelper.useServerTables(message.drinks(), message.foods(), message.containers(), message.estimated());
        SyncedValues.useServerValues(message.defaultPurity(), message.waterBottleStackSize(), message.purityEnabled(), message.canFillFromFlowingWater(),
                message.pureThirstBonus(), message.pureQuenchedBonus());
    }
}
