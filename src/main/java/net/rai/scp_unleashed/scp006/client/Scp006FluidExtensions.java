package net.rai.scp_unleashed.scp006.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.rai.scp_unleashed.scp006.Scp006Registry;
import org.joml.Vector3f;

public final class Scp006FluidExtensions implements IClientFluidTypeExtensions {
    public static final Scp006FluidExtensions Instance = new Scp006FluidExtensions();
    public static final ResourceLocation Still = Scp006Registry.location("block/scp006/still");
    public static final ResourceLocation Flowing = Scp006Registry.location("block/scp006/flow");
    public static final ResourceLocation Overlay = Scp006Registry.location("block/scp006/overlay");
    public static final ResourceLocation Underwater = ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");
    public static final int Tint = 0xFFFFFFFF;
    public static final float FogRed = 0.38f;
    public static final float FogGreen = 0.58f;
    public static final float FogBlue = 0.8f;
    public static final float FogShare = 0.72f;
    public static final float NightFloor = 0.35f;
    public static final float Clarity = 1.3f;

    private Scp006FluidExtensions() {
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
        return new Vector3f(Mth.lerp(FogShare, FluidFogColor.x(), FogRed * Day), Mth.lerp(FogShare, FluidFogColor.y(), FogGreen * Day),
                Mth.lerp(FogShare, FluidFogColor.z(), FogBlue * Day));
    }

    @Override
    public void modifyFogRender(Camera View, FogRenderer.FogMode Mode, float RenderDistance, float PartialTick, float NearDistance, float FarDistance, FogShape Shape) {
        RenderSystem.setShaderFogStart(NearDistance);
        RenderSystem.setShaderFogEnd(Math.min(RenderDistance, FarDistance * Clarity));
        RenderSystem.setShaderFogShape(Shape);
    }
}
