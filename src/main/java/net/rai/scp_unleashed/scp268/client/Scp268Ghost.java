package net.rai.scp_unleashed.scp268.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp268.Scp268;

public final class Scp268Ghost {
    public static final int FadedAlpha = 0x8C;

    private Scp268Ghost() {
    }

    public static boolean isFaded(LivingEntity Shown) {
        Minecraft Client = Minecraft.getInstance();
        return Shown == Client.player && Scp268.isNeglected(Shown);
    }

    public static int fade(int Colour) {
        int Alpha = (Colour >>> 24) * FadedAlpha / 255;
        return (Alpha << 24) | (Colour & 0xFFFFFF);
    }
}
