package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.UUID;

/**
 * Players for thirst tests. Vanilla's mock server player is creative, and creative players neither lose thirst nor
 * overhydrate, so these are NeoForge fake players in survival. The server does not tick them: tests call what they need.
 */
final class TestSupport
{
    private TestSupport() {}

    static ServerPlayer player(GameTestHelper helper)
    {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "droplets-test"));
        player.snapTo(helper.absoluteVec(Vec3.ZERO));
        return player;
    }

    /**
     * The stack an item use left in the hand: what a successful use hands back, or the used stack otherwise.
     */
    static ItemStack heldResult(InteractionResult result, ItemStack used)
    {
        return result instanceof InteractionResult.Success success && success.heldItemTransformedTo() != null ? success.heldItemTransformedTo() : used;
    }

    /**
     * An empty bucket in a mock player's hand filled with a bucket of {@code water} through the fluid capability, the
     * way fluid handlers of other mods fill it.
     */
    static ItemStack fillBucket(GameTestHelper helper, FluidResource water)
    {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        ResourceHandler<FluidResource> bucket = ItemAccess.forPlayerInteraction(player, InteractionHand.MAIN_HAND).getCapability(Capabilities.Fluid.ITEM);
        try (Transaction transaction = Transaction.openRoot())
        {
            if (bucket != null && bucket.insert(water, FluidType.BUCKET_VOLUME, transaction) == FluidType.BUCKET_VOLUME)
                transaction.commit();
        }
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    static PlayerThirst thirst(ServerPlayer player)
    {
        return player.getData(ModAttachment.PLAYER_THIRST);
    }
}
