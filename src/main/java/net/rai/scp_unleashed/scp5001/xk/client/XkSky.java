package net.rai.scp_unleashed.scp5001.xk.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.minecraft.world.level.material.FogType;
import net.rai.scp_unleashed.scp5001.xk.Scp5001XkRegistry;

public final class XkSky {
    public static final ResourceLocation RedSun = Scp5001XkRegistry.location("textures/environment/scp5001_red_sun.png");
    public static final float ElsewhereShare = 0.45f;
    public static final float Rate = 0.004f;
    private static final float[][] SkyKeys = {{0.0f, 0.80f, 0.50f, 0.42f, 0.0f}, {1.0f / 3.0f, 0.80f, 0.50f, 0.42f, 0.8f}, {2.0f / 3.0f, 0.55f, 0.08f, 0.06f, 0.95f},
            {1.0f, 0.15f, 0.015f, 0.02f, 1.0f}};
    private static final float[][] FogKeys = {{0.0f, 0.90f, 0.60f, 0.48f, 0.0f}, {1.0f / 3.0f, 0.90f, 0.60f, 0.48f, 0.8f}, {2.0f / 3.0f, 0.50f, 0.11f, 0.07f, 0.95f},
            {1.0f, 0.18f, 0.03f, 0.03f, 1.0f}};
    private static final float[][] CloudKeys = {{0.0f, 0.95f, 0.70f, 0.62f, 0.0f}, {1.0f / 3.0f, 0.95f, 0.70f, 0.62f, 0.7f}, {2.0f / 3.0f, 0.62f, 0.18f, 0.14f, 0.9f},
            {1.0f, 0.22f, 0.05f, 0.05f, 1.0f}};
    private static float Target;
    private static float Current;
    private static float CurrentO;

    private XkSky() {
    }

    public static void receive(float Sky, boolean Snap) {
        Target = Mth.clamp(Sky, 0.0f, 1.0f);
        if (Snap) {
            Current = Target;
            CurrentO = Target;
        }
    }

    public static void reset() {
        Target = 0.0f;
        Current = 0.0f;
        CurrentO = 0.0f;
    }

    public static void tick() {
        CurrentO = Current;
        Current += Mth.clamp(Target - Current, -Rate, Rate);
    }

    public static float progress(float Partial) {
        return Mth.lerp(Partial, CurrentO, Current);
    }

    public static float strength(ClientLevel World, float Partial) {
        float Raw = progress(Partial);
        if (Raw <= 0.0001f) {
            return 0.0f;
        }
        return World.dimension() == Level.OVERWORLD ? Raw : Raw * ElsewhereShare;
    }

    private static float[] keyed(float[][] Keys, float Progress) {
        for (int Index = 0; Index < Keys.length - 1; Index++) {
            float[] A = Keys[Index];
            float[] B = Keys[Index + 1];
            if (Progress <= B[0]) {
                float T = (Progress - A[0]) / (B[0] - A[0]);
                return new float[]{Mth.lerp(T, A[1], B[1]), Mth.lerp(T, A[2], B[2]), Mth.lerp(T, A[3], B[3]), Mth.lerp(T, A[4], B[4])};
            }
        }
        float[] Last = Keys[Keys.length - 1];
        return new float[]{Last[1], Last[2], Last[3], Last[4]};
    }

    private static Vec3 blend(float[][] Keys, Vec3 Vanilla, float Progress, float Reference) {
        if (Progress <= 0.0001f) {
            return Vanilla;
        }
        float[] Key = keyed(Keys, Progress);
        double Light = Mth.clamp(Math.max(Vanilla.x, Math.max(Vanilla.y, Vanilla.z)) / Reference, 0.0, 1.0);
        double Mix = Key[3];
        return new Vec3(Mth.lerp(Mix, Vanilla.x, Key[0] * Light), Mth.lerp(Mix, Vanilla.y, Key[1] * Light), Mth.lerp(Mix, Vanilla.z, Key[2] * Light));
    }

    public static Vec3 sky(Vec3 Vanilla, ClientLevel World, float Partial) {
        return blend(SkyKeys, Vanilla, strength(World, Partial), 1.0f);
    }

    public static Vec3 cloud(Vec3 Vanilla, ClientLevel World, float Partial) {
        return blend(CloudKeys, Vanilla, strength(World, Partial), 1.0f);
    }

    public static float darken(float Vanilla, ClientLevel World, float Partial) {
        float Progress = strength(World, Partial);
        return Vanilla * (1.0f - 0.35f * Progress);
    }

    public static void fog(ViewportEvent.ComputeFogColor Event) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null || Event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float Progress = strength(World, (float) Event.getPartialTick());
        if (Progress <= 0.0001f) {
            return;
        }
        Vec3 Made = blend(FogKeys, new Vec3(Event.getRed(), Event.getGreen(), Event.getBlue()), Progress, 0.85f);
        Event.setRed((float) Made.x);
        Event.setGreen((float) Made.y);
        Event.setBlue((float) Made.z);
    }

    public static float sunRed(float Partial) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null) {
            return 0.0f;
        }
        return Mth.clamp((strength(Client.level, Partial) - 0.4f) / 0.5f, 0.0f, 1.0f);
    }

    public static ResourceLocation sunTexture(ResourceLocation Vanilla) {
        return sunRed(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) >= 0.85f ? RedSun : Vanilla;
    }

    public static void tintSun() {
        float Red = sunRed(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (Red <= 0.0f) {
            return;
        }
        float[] Colour = RenderSystem.getShaderColor();
        float Alpha = Colour[3];
        if (Red >= 0.85f) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, Alpha);
        } else {
            RenderSystem.setShaderColor(1.0f, 1.0f - 0.8f * Red, 1.0f - 0.9f * Red, Alpha);
        }
    }

    public static void tintMoon() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null) {
            return;
        }
        float Progress = strength(Client.level, Client.getTimer().getGameTimeDeltaPartialTick(false));
        if (Progress <= 0.0f) {
            return;
        }
        float[] Colour = RenderSystem.getShaderColor();
        RenderSystem.setShaderColor(1.0f, 1.0f - 0.75f * Progress, 1.0f - 0.8f * Progress, Colour[3]);
    }

    public static boolean overworld(Level World) {
        return World.dimension() == Level.OVERWORLD;
    }
}
