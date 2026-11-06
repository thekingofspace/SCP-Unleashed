package net.rai.scp_unleashed.scp3008.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Scp3008StoreEffects extends DimensionSpecialEffects {
    public static final Vector3f DayAmbient = new Vector3f(0.86f, 0.88f, 0.93f);
    public static final Vector3f NightAmbient = new Vector3f(0.012f, 0.014f, 0.026f);
    public static final float NightFog = 0.035f;

    public Scp3008StoreEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 FogColour, float Brightness) {
        float Lit = Scp3008ClientState.brightness();
        return FogColour.scale(Mth.lerp(Lit, NightFog, 1.0f));
    }

    @Override
    public boolean isFoggyAt(int X, int Y) {
        return false;
    }

    @Override
    public void adjustLightmapColors(ClientLevel World, float PartialTicks, float SkyDarken, float BlockFlicker, float SkyLight, int BlockLevel, int SkyLevel, Vector3f Colours) {
        float Lit = Scp3008ClientState.brightness(PartialTicks);
        float Block = LightTexture.getBrightness(World.dimensionType(), BlockLevel) * BlockFlicker;
        float BlockGreen = Block * ((Block * 0.6f + 0.4f) * 0.6f + 0.4f);
        float BlockBlue = Block * (Block * Block * 0.6f + 0.4f);
        float Red = Mth.lerp(Lit, NightAmbient.x, DayAmbient.x);
        float Green = Mth.lerp(Lit, NightAmbient.y, DayAmbient.y);
        float Blue = Mth.lerp(Lit, NightAmbient.z, DayAmbient.z);
        Colours.set(1.0f - (1.0f - Mth.clamp(Block, 0.0f, 1.0f)) * (1.0f - Red),
                1.0f - (1.0f - Mth.clamp(BlockGreen, 0.0f, 1.0f)) * (1.0f - Green),
                1.0f - (1.0f - Mth.clamp(BlockBlue, 0.0f, 1.0f)) * (1.0f - Blue));
    }

    @Override
    public boolean renderClouds(ClientLevel World, int Ticks, float PartialTick, PoseStack Pose, double CameraX, double CameraY, double CameraZ, Matrix4f ModelView, Matrix4f Projection) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel World, int Ticks, float PartialTick, Matrix4f ModelView, Camera View, Matrix4f Projection, boolean Foggy, Runnable SetupFog) {
        if (!ShaderCompat.useFallback()) {
            Scp3008Sky.ceiling(View, ModelView, PartialTick);
        }
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel World, int Ticks, float PartialTick, LightTexture Light, double CameraX, double CameraY, double CameraZ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel World, int Ticks, Camera View) {
        return true;
    }
}
