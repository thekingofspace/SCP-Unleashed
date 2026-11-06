package net.rai.scp_unleashed.security.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.rai.scp_unleashed.security.MotionDetectorBlockEntity;

public final class SecurityScreens {
    private SecurityScreens() {
    }

    public static void openDetector(BlockPos Pos) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level != null && Game.level.getBlockEntity(Pos) instanceof MotionDetectorBlockEntity Detector) {
            Game.setScreen(new MotionDetectorScreen(Pos.immutable(), Game.level.getBlockState(Pos).getBlock().getName(), Detector.getPort()));
        }
    }
}
