package net.rai.scp_unleashed.scp354.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.rai.scp_unleashed.scp354.Scp354Registry;
import org.joml.Vector3f;

public final class Scp354FluidExtensions implements IClientFluidTypeExtensions {
    public static final Scp354FluidExtensions Instance = new Scp354FluidExtensions();
    public static final ResourceLocation Still = Scp354Registry.location("block/scp354/still");
    public static final ResourceLocation Flowing = Scp354Registry.location("block/scp354/flow");
    public static final ResourceLocation Overlay = Scp354Registry.location("block/scp354/overlay");
    public static final ResourceLocation Underwater = Scp354Registry.location("textures/misc/scp354_underliquid.png");
    public static final int Tint = 0xFFFFFFFF;
    public static final float FogRed = 0.21f;
    public static final float FogGreen = 0.03f;
    public static final float FogBlue = 0.04f;
    public static final float NightFloor = 0.45f;
    public static final float FogEnd = 3.5f;

    private Scp354FluidExtensions() {
    }

    @Override
    public ResourceLocation getStillTexture() {
        return Still;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return Flowing;
    }

    @Override
    public ResourceLocation getOverlayTexture() {
        return Overlay;
    }

    @Override
    public ResourceLocation getRenderOverlayTexture(Minecraft Client) {
        return Underwater;
    }

    @Override
    public int getTintColor() {
        return Tint;
    }

    @Override
    public Vector3f modifyFogColor(Camera View, float PartialTick, ClientLevel World, int RenderDistance, float DarkenWorldAmount, Vector3f FluidFogColor) {
        float Day = NightFloor + (1.0f - NightFloor) * Mth.clamp(World.getSkyDarken(PartialTick), 0.0f, 1.0f);
        return new Vector3f(FogRed * Day, FogGreen * Day, FogBlue * Day);
    }

    @Override
    public void modifyFogRender(Camera View, FogRenderer.FogMode Mode, float RenderDistance, float PartialTick, float NearDistance, float FarDistance, FogShape Shape) {
        RenderSystem.setShaderFogStart(-1.0f);
        RenderSystem.setShaderFogEnd(FogEnd);
        RenderSystem.setShaderFogShape(FogShape.SPHERE);
    }
}
