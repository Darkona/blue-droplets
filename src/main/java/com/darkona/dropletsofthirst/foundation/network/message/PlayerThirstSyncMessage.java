package com.darkona.dropletsofthirst.foundation.network.message;

import com.darkona.dropletsofthirst.foundation.network.ClientPayloadHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Thirst data of the receiving player plus the server rules the client applies itself ({@code PlayerThirst.SYNC_*} bits).
 */
public record PlayerThirstSyncMessage(int thirst, int quenched, float exhaustion, int flags)
{
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeInt(thirst);
        buffer.writeInt(quenched);
        buffer.writeFloat(exhaustion);
        buffer.writeVarInt(flags);
    }

    public static PlayerThirstSyncMessage decode(FriendlyByteBuf buffer)
    {
        return new PlayerThirstSyncMessage(buffer.readInt(), buffer.readInt(), buffer.readFloat(), buffer.readVarInt());
    }

    public static void clientHandle(final PlayerThirstSyncMessage message, final Supplier<NetworkEvent.Context> context)
    {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPayloadHandlers.thirst(message));
    }
}
