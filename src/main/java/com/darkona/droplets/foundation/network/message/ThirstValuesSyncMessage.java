package com.darkona.droplets.foundation.network.message;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.api.ThirstHelper;
import com.darkona.droplets.content.purity.WaterPurity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Values resolved by the server (config, data map, tags, keywords, {@code RegisterThirstValueEvent}, blacklist applied):
 * item → {thirst, quenched, purity} for drinks and foods, filled items of data-driven purity containers, and {@code defaultPurity}.
 */
public record ThirstValuesSyncMessage(Map<Item, int[]> drinks, Map<Item, int[]> foods, List<Item> containers, int defaultPurity) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ThirstValuesSyncMessage> TYPE = new Type<>(BlueDroplets.asResource("thirst_values"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Item> ITEM = ByteBufCodecs.registry(Registries.ITEM);
    private static final StreamCodec<ByteBuf, int[]> VALUES = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, values -> values[0],
            ByteBufCodecs.VAR_INT, values -> values[1],
            ByteBufCodecs.VAR_INT, values -> values[2],
            (thirst, quenched, purity) -> new int[]{thirst, quenched, purity});
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Item, int[]>> TABLE = ByteBufCodecs.map(HashMap::new, ITEM, VALUES);

    public static final StreamCodec<RegistryFriendlyByteBuf, ThirstValuesSyncMessage> STREAM_CODEC = StreamCodec.composite(
            TABLE, ThirstValuesSyncMessage::drinks,
            TABLE, ThirstValuesSyncMessage::foods,
            ITEM.apply(ByteBufCodecs.list()), ThirstValuesSyncMessage::containers,
            ByteBufCodecs.VAR_INT, ThirstValuesSyncMessage::defaultPurity,
            ThirstValuesSyncMessage::new);

    public static ThirstValuesSyncMessage fromTables()
    {
        return new ThirstValuesSyncMessage(ThirstHelper.drinkTable(), ThirstHelper.foodTable(), WaterPurity.dataContainerItems(), WaterPurity.defaultPurity());
    }

    public static void clientHandle(final ThirstValuesSyncMessage message, final IPayloadContext context)
    {
        context.enqueueWork(() -> {
            ThirstHelper.useServerTables(message.drinks, message.foods, message.containers);
            WaterPurity.setServerDefaultPurity(message.defaultPurity);
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
