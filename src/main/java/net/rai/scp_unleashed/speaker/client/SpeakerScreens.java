package net.rai.scp_unleashed.speaker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.rai.scp_unleashed.speaker.SoundConsoleBlockEntity;

public final class SpeakerScreens {
    private SpeakerScreens() {
    }

    public static void openConsole(BlockPos Pos) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level != null && Game.level.getBlockEntity(Pos) instanceof SoundConsoleBlockEntity Console) {
            Game.setScreen(new SoundConsoleScreen(Pos.immutable(), Game.level.getBlockState(Pos).getBlock().getName(), Console));
        }
    }
}
