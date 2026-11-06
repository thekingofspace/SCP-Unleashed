package net.rai.scp_unleashed.fission.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.rai.scp_unleashed.Scp_unleashed;
import org.joml.Vector3f;

public final class WasteWaterExtensions implements IClientFluidTypeExtensions {
    public static final WasteWaterExtensions Instance = new WasteWaterExtensions();
    public static final ResourceLocation Still = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/fission/waste_water_still");
    public static final ResourceLocation Flowing = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/fission/waste_water_flow");
    public static final ResourceLocation Overlay = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/fission/waste_water_overlay");
    public static final float FogEnd = 7.0f;

    private WasteWaterExtensions() {
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
    public int getTintColor() {
        return 0xFFFFFFFF;
    }

    @Override
    public Vector3f modifyFogColor(Camera View, float PartialTick, ClientLevel World, int RenderDistance, float DarkenWorldAmount, Vector3f FluidFogColor) {
        return new Vector3f(0.24f, 0.31f, 0.09f);
    }

    @Override
    public void modifyFogRender(Camera View, FogRenderer.FogMode Mode, float RenderDistance, float PartialTick, float NearDistance, float FarDistance, FogShape Shape) {
        RenderSystem.setShaderFogStart(0.0f);
        RenderSystem.setShaderFogEnd(FogEnd);
        RenderSystem.setShaderFogShape(FogShape.SPHERE);
    }
}
