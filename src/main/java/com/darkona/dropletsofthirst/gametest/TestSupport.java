package com.darkona.dropletsofthirst.gametest;

import com.darkona.dropletsofthirst.content.thirst.PlayerThirst;
import com.darkona.dropletsofthirst.foundation.common.capability.ModAttachment;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
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
        player.moveTo(helper.absoluteVec(Vec3.ZERO));
        return player;
    }

    static PlayerThirst thirst(ServerPlayer player)
    {
        return player.getData(ModAttachment.PLAYER_THIRST);
    }
}
