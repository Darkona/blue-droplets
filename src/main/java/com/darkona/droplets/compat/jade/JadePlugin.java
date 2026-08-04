package com.darkona.droplets.compat.jade;

import com.darkona.droplets.BlueDroplets;
import com.darkona.droplets.content.purity.WaterPurity;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade: water purity of water cauldrons (murky, or clean on a heat source) and of the tanks of any block exposing a fluid handler
 * (sent by the server). Jade finds and loads this class itself, only when installed; nothing else imports Jade.
 */
@WailaPlugin(BlueDroplets.ID)
public class JadePlugin implements IWailaPlugin
{
    @Override
    public void register(IWailaCommonRegistration registration)
    {
        registration.registerBlockDataProvider(PurityProvider.INSTANCE, BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration)
    {
        registration.registerBlockComponent(PurityProvider.INSTANCE, Block.class);
    }

    private enum PurityProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor>
    {
        INSTANCE;

        private static final Identifier UID = BlueDroplets.asResource("purity");
        private static final String KEY = UID.toString();

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor)
        {
            if (!WaterPurity.enabled())
                return;
            ResourceHandler<FluidResource> handler = accessor.getLevel().getCapability(Capabilities.Fluid.BLOCK, accessor.getPosition(),
                    accessor.getBlockState(), accessor.getBlockEntity(), null);
            if (handler == null)
                return;

            IntArrayList purities = new IntArrayList();
            for (int tank = 0; tank < handler.size(); tank++)
            {
                FluidResource fluid = handler.getResource(tank);
                if (!fluid.isEmpty() && handler.getAmountAsLong(tank) > 0 && (WaterPurity.hasPurity(fluid) || fluid.is(FluidTags.WATER)))
                    purities.add(WaterPurity.getPurity(fluid));
            }
            if (!purities.isEmpty())
                data.putIntArray(KEY, purities.toIntArray());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config)
        {
            if (!WaterPurity.enabled())
                return;
            BlockState state = accessor.getBlockState();
            if (state.is(Blocks.WATER_CAULDRON))
                add(tooltip, WaterPurity.cauldronPurity(accessor.getLevel(), accessor.getPosition()));
            else
                accessor.getServerData().getIntArray(KEY).ifPresent(purities -> {
                    for (int purity : purities)
                        add(tooltip, WaterPurity.sanitizePurity(purity));
                });
        }

        private static void add(ITooltip tooltip, int purity)
        {
            tooltip.add(Component.literal(WaterPurity.getPurityText(purity)).withColor(WaterPurity.getPurityColor(purity)));
        }

        @Override
        public Identifier getUid()
        {
            return UID;
        }
    }
}
