package net.rai.scp_unleashed.z0003.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.z0003.Z0003Arming;
import net.rai.scp_unleashed.z0003.Z0003DeviceItem;
import net.rai.scp_unleashed.z0003.Z0003Registry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Z0003Glow {
    public static final float SlowPeriod = 40.0f;
    public static final float FastPeriod = 4.0f;
    public static final float SlowFrom = 400.0f;
    public static final float FastFrom = 40.0f;
    public static final float Rate = SlowFrom / SlowPeriod;
    public static final int Levels = 2;
    public static final List<ModelResourceLocation> GlowModels = List.of(standalone("item/z0003/knot_glow"), standalone("item/z0003/knot_bright"));
    public static final ModelResourceLocation ActiveModel = standalone("item/z0003/knot_bright");

    private Z0003Glow() {
    }

    private static ModelResourceLocation standalone(String Path) {
        return ModelResourceLocation.standalone(Z0003Registry.location(Path));
    }

    public static double phase(double Left) {
        double Slow = Math.max(Left, SlowFrom);
        double Middle = Mth.clamp(Left, FastFrom, SlowFrom);
        double Fast = Math.min(Left, FastFrom);
        return -Slow / SlowPeriod + Rate * Math.log(SlowFrom / Middle) + (FastFrom - Fast) / FastPeriod;
    }

    public static float pulse(double Left) {
        double Turn = phase(Math.max(0.0, Left));
        return (float) (0.5 + 0.5 * Math.cos(Turn * Math.PI * 2.0));
    }

    public static int level(float Pulse) {
        return Mth.clamp(1 + (int) (Pulse * Levels), 1, Levels);
    }

    public static float property(ItemStack Stack, @Nullable ClientLevel World, @Nullable LivingEntity Holder, int Seed) {
        Z0003Arming Setting = Z0003DeviceItem.armingOf(Stack);
        ClientLevel Here = World != null ? World : Minecraft.getInstance().level;
        if (Setting == null || Here == null) {
            return 0.0f;
        }
        double Left = Setting.Due() - Here.getGameTime() - Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        return level(pulse(Left)) / (float) Levels;
    }
}
