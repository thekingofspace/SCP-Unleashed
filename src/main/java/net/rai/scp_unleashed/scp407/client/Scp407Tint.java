package net.rai.scp_unleashed.scp407.client;

import net.minecraft.util.Mth;
import software.bernie.geckolib.util.Color;

public final class Scp407Tint {
    public static final float[] Brown = {0.78f, 0.62f, 0.44f};
    public static final float[] Grey = {0.58f, 0.56f, 0.54f};

    private Scp407Tint() {
    }

    public static Color tint(float Share) {
        if (Share <= 0.0f) {
            return Color.WHITE;
        }
        float First = Mth.clamp(Share * 2.0f, 0.0f, 1.0f);
        float Second = Mth.clamp(Share * 2.0f - 1.0f, 0.0f, 1.0f);
        float Red = Mth.lerp(Second, Mth.lerp(First, 1.0f, Brown[0]), Grey[0]);
        float Green = Mth.lerp(Second, Mth.lerp(First, 1.0f, Brown[1]), Grey[1]);
        float Blue = Mth.lerp(Second, Mth.lerp(First, 1.0f, Brown[2]), Grey[2]);
        return Color.ofRGBA(Red, Green, Blue, 1.0f);
    }
}
