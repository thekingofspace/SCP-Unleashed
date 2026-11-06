package net.rai.scp_unleashed.scp106.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class Scp106PocketEffects extends DimensionSpecialEffects {
    public Scp106PocketEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 FogColour, float Brightness) {
        return new Vec3(Scp106PocketClient.FogRed, Scp106PocketClient.FogGreen, Scp106PocketClient.FogBlue);
    }

    @Override
    public boolean isFoggyAt(int X, int Z) {
        return true;
    }

    @Override
    public float[] getSunriseColor(float TimeOfDay, float PartialTick) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel World, int Ticks, float PartialTick, Matrix4f ModelView, Camera View, Matrix4f Projection, boolean Foggy, Runnable SetupFog) {
        return true;
    }

    @Override
    public boolean renderClouds(ClientLevel World, int Ticks, float PartialTick, PoseStack Pose, double CamX, double CamY, double CamZ, Matrix4f ModelView, Matrix4f Projection) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel World, int Ticks, float PartialTick, LightTexture Light, double CamX, double CamY, double CamZ) {
        return true;
    }
}
