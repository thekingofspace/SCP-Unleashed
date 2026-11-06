package net.rai.scp_unleashed.scp179.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.rai.scp_unleashed.scp179.SatelliteTerminalBlockEntity;

public final class Scp179ClientHooks {
    private Scp179ClientHooks() {
    }

    public static void openTerminal(BlockPos Pos) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level != null && Game.level.getBlockEntity(Pos) instanceof SatelliteTerminalBlockEntity) {
            Game.setScreen(new SatelliteTerminalScreen(Pos.immutable(), Game.level.getBlockState(Pos).getBlock().getName()));
        }
    }
}
