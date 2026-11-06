package net.rai.scp_unleashed.space.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class MoonSkyEffects extends DimensionSpecialEffects {
    public MoonSkyEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 FogColour, float Brightness) {
        return Vec3.ZERO;
    }

    @Override
    public boolean isFoggyAt(int X, int Z) {
        return false;
    }

    @Nullable
    @Override
    public float[] getSunriseColor(float TimeOfDay, float PartialTick) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel World, int Ticks, float PartialTick, Matrix4f ModelView, Camera View, Matrix4f Projection, boolean Foggy, Runnable SetupFog) {
        MoonSky.render(World, PartialTick, ModelView, Projection);
        return true;
    }

    @Override
    public boolean renderClouds(ClientLevel World, int Ticks, float PartialTick, PoseStack Pose, double CameraX, double CameraY, double CameraZ,
                                Matrix4f ModelView, Matrix4f Projection) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel World, int Ticks, float PartialTick, LightTexture Lights, double CameraX, double CameraY, double CameraZ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel World, int Ticks, Camera View) {
        return true;
    }
}
