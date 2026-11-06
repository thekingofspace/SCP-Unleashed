package net.rai.scp_unleashed.oil.client;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.oil.OilGrade;
import org.joml.Vector3f;

public final class OilFluidExtensions implements IClientFluidTypeExtensions {
    private final OilGrade Grade;
    private final ResourceLocation Still;
    private final ResourceLocation Flowing;
    private final ResourceLocation Overlay;

    public OilFluidExtensions(OilGrade Grade) {
        this.Grade = Grade;
        this.Still = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/oil/" + Grade.getName() + "_still");
        this.Flowing = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/oil/" + Grade.getName() + "_flow");
        this.Overlay = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/oil/" + Grade.getName() + "_overlay");
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
        int Colour = Grade.getFogColour();
        return new Vector3f((Colour >> 16 & 255) / 255.0f, (Colour >> 8 & 255) / 255.0f, (Colour & 255) / 255.0f);
    }

    @Override
    public void modifyFogRender(Camera View, FogRenderer.FogMode Mode, float RenderDistance, float PartialTick, float NearDistance, float FarDistance, FogShape Shape) {
        RenderSystem.setShaderFogStart(0.0f);
        RenderSystem.setShaderFogEnd(Grade.getFogEnd());
        RenderSystem.setShaderFogShape(FogShape.SPHERE);
    }
}
