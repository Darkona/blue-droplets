package com.darkona.droplets.foundation.network.message;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.thirst.ThirstHelper;
import com.darkona.droplets.content.purity.WaterPurity;
import com.darkona.droplets.foundation.config.SyncedValues;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Values resolved by the server (config, data map, tags, keywords, {@code RegisterThirstValueEvent}, blacklist applied):
 * item → {thirst, quenched, purity} for drinks and foods, the items whose values are estimated from recipes, filled
 * items of data-driven purity containers, and the {@link SyncedValues}. Sent on join, after {@code /reload} and after a config file changes.
 */
public record ThirstValuesSyncMessage(Map<Item, int[]> drinks, Map<Item, int[]> foods, List<Item> estimated, List<Item> containers, int defaultPurity, int waterBottleStackSize, boolean purityEnabled) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ThirstValuesSyncMessage> TYPE = new Type<>(BlueDroplets.asResource("thirst_values"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Item> ITEM = ByteBufCodecs.registry(Registries.ITEM);
    private static final StreamCodec<ByteBuf, int[]> VALUES = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, values -> values[0],
            ByteBufCodecs.VAR_INT, values -> values[1],
            ByteBufCodecs.VAR_INT, values -> values[2],
            (thirst, quenched, purity) -> new int[]{thirst, quenched, purity});
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Item, int[]>> TABLE = ByteBufCodecs.map(HashMap::new, ITEM, VALUES);

    public static final StreamCodec<RegistryFriendlyByteBuf, ThirstValuesSyncMessage> STREAM_CODEC = NeoForgeStreamCodecs.composite(
            TABLE, ThirstValuesSyncMessage::drinks,
            TABLE, ThirstValuesSyncMessage::foods,
            ITEM.apply(ByteBufCodecs.list()), ThirstValuesSyncMessage::estimated,
            ITEM.apply(ByteBufCodecs.list()), ThirstValuesSyncMessage::containers,
            ByteBufCodecs.VAR_INT, ThirstValuesSyncMessage::defaultPurity,
            ByteBufCodecs.VAR_INT, ThirstValuesSyncMessage::waterBottleStackSize,
            ByteBufCodecs.BOOL, ThirstValuesSyncMessage::purityEnabled,
            ThirstValuesSyncMessage::new);

    public static ThirstValuesSyncMessage fromTables()
    {
        return new ThirstValuesSyncMessage(ThirstHelper.drinkTable(), ThirstHelper.foodTable(), List.copyOf(ThirstHelper.estimatedItems()), WaterPurity.dataContainerItems(),
                SyncedValues.defaultPurity(), SyncedValues.waterBottleStackSize(), SyncedValues.purityEnabled());
    }

    public static void clientHandle(final ThirstValuesSyncMessage message, final IPayloadContext context)
    {
        context.enqueueWork(() -> {
            ThirstHelper.useServerTables(message.drinks, message.foods, message.containers, message.estimated);
            SyncedValues.useServerValues(message.defaultPurity, message.waterBottleStackSize, message.purityEnabled);
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
