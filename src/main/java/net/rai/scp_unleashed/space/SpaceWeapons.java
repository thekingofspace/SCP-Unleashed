package net.rai.scp_unleashed.space;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class SpaceWeapons {
    public static final String JamKey = "message.scp_unleashed.space.gun_jammed";

    private SpaceWeapons() {
    }

    public static boolean jammed(Player User, ItemStack Stack) {
        return SpaceAir.isAirless(User.level()) && !Stack.is(SpaceTags.SpaceWeapons);
    }

    public static Component hint() {
        return Component.translatable(JamKey).withStyle(ChatFormatting.GRAY);
    }
}
