package net.rai.scp_unleashed.z0003.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.z0003.Z0003Device;
import net.rai.scp_unleashed.z0003.Z0003Registry;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class Z0003FieldTint {
    public static final ResourceLocation LayerId = Z0003Registry.location("z0003_field_tint");
    public static final float MaxTint = 0.2f;
    public static final float EdgeTint = 0.32f;
    public static final float EdgeSoft = 1.5f;
    public static final int Green = 0x1FB84A;
    public static final int DeepGreen = 0x0A5A22;

    private static final Set<Z0003Device> Fields = Collections.newSetFromMap(new WeakHashMap<>());

    private Z0003FieldTint() {
    }

    public static void track(Z0003Device Device) {
        if (Device.isActive() && !Device.isRemoved()) {
            Fields.add(Device);
        } else {
            Fields.remove(Device);
        }
    }

    public static void reset() {
        Fields.clear();
    }

    public static float strength(Vec3 Eye, float Partial) {
        Minecraft Game = Minecraft.getInstance();
        float Strongest = 0.0f;
        for (Z0003Device Device : Fields) {
            if (Device.isRemoved() || Device.level() != Game.level || !Device.isActive()) {
                continue;
            }
            Vec3 Centre = Device.getPosition(Partial).add(0.0, Z0003Device.Height * 0.5, 0.0);
            float Reach = Device.fieldReach(Partial);
            float Inside = Mth.clamp((float) (Reach - Eye.distanceTo(Centre)) / EdgeSoft, 0.0f, 1.0f);
            Strongest = Math.max(Strongest, Inside * Device.fieldFade(Partial));
        }
        return Strongest;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null || Fields.isEmpty() || Z0003StatueClient.isLocked()) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Strength = strength(Game.gameRenderer.getMainCamera().getPosition(), Partial);
        if (Strength <= 0.003f) {
            return;
        }
        float Pulse = 0.9f + 0.1f * Mth.sin((Game.level.getGameTime() + Partial) * 0.12f);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        int Fill = Mth.clamp(Math.round(Strength * MaxTint * Pulse * 255.0f), 0, 255);
        int Edge = Mth.clamp(Math.round(Strength * EdgeTint * 255.0f), 0, 255);
        Graphics.fill(0, 0, Width, Height, Fill << 24 | Green);
        int Band = Height / 4;
        Graphics.fillGradient(0, 0, Width, Band, Edge << 24 | DeepGreen, DeepGreen);
        Graphics.fillGradient(0, Height - Band, Width, Height, DeepGreen, Edge << 24 | DeepGreen);
    }
}
