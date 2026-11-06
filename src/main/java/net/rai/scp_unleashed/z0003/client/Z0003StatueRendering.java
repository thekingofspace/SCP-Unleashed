package net.rai.scp_unleashed.z0003.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.z0003.Z0003Frozen;
import net.rai.scp_unleashed.z0003.Z0003Petrify;
import net.rai.scp_unleashed.z0003.Z0003Statue;
import org.jetbrains.annotations.Nullable;

public final class Z0003StatueRendering {
    public static final float FrozenPartial = 1.0f;

    private Z0003StatueRendering() {
    }

    public static boolean frozen(Entity Shown) {
        return Z0003Petrify.isStatue(Shown);
    }

    @Nullable
    public static Z0003Frozen hold(Entity Shown) {
        Z0003Frozen Shot = Z0003StatueClient.snapshot(Shown);
        if (Shot == null || !(Shown instanceof LivingEntity Living)) {
            return null;
        }
        Z0003Frozen Live = Z0003Frozen.capture(Living);
        Shot.apply(Living);
        return Live;
    }

    public static void release(Entity Shown, @Nullable Z0003Frozen Live) {
        if (Live != null && Shown instanceof LivingEntity Living) {
            Live.apply(Living);
        }
    }

    public static MultiBufferSource wrap(MultiBufferSource Buffers, Entity Shown) {
        Z0003Statue Statue = Z0003Petrify.statueOf(Shown);
        float Crack = 0.0f;
        if (Statue != null && Statue.cracking()) {
            Crack = Statue.crackProgress(Shown.level().getGameTime(), Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false),
                    Z0003Petrify.crackTicks(Shown));
        }
        return new Z0003StoneBuffers(Buffers, Crack, Z0003RenderTypes.fallback());
    }
}
