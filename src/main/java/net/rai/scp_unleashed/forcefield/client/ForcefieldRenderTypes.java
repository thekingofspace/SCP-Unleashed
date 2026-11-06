package net.rai.scp_unleashed.forcefield.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

import java.util.function.Function;

public final class ForcefieldRenderTypes {
    private static final RenderStateShard.TransparencyStateShard GlowBlend = new RenderStateShard.TransparencyStateShard("scp_unleashed_forcefield_glow_blend", () -> {
        RenderSystem.enableBlend();
        if (ForcefieldDrawPlan.GlowKeepsAlpha) {
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        } else {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        }
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    private static final Function<ResourceLocation, RenderType> Additive = Util.memoize(Texture -> RenderType.create("scp_unleashed_forcefield_additive",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(Texture, false, false))
                    .setTransparencyState(GlowBlend)
                    .setWriteMaskState(writeMask(true, ForcefieldDrawPlan.GlowWritesDepth))
                    .setCullState(cull(ForcefieldDrawPlan.GlowCulls))
                    .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> Stamp = Util.memoize(Texture -> RenderType.create("scp_unleashed_forcefield_depth",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(Texture, false, false))
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setWriteMaskState(writeMask(ForcefieldDrawPlan.StampWritesColour, ForcefieldDrawPlan.StampWritesDepth))
                    .setCullState(cull(ForcefieldDrawPlan.StampCulls))
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(false)));

    private ForcefieldRenderTypes() {
    }

    public static RenderType sheet(ResourceLocation Texture) {
        return RenderType.entityTranslucentEmissive(Texture);
    }

    public static RenderType glow(ResourceLocation Texture) {
        return ShaderCompat.useFallback() ? RenderType.entityTranslucentEmissive(Texture) : Additive.apply(Texture);
    }

    public static RenderType stamp(ResourceLocation Texture) {
        return Stamp.apply(Texture);
    }

    public static boolean additive() {
        return !ShaderCompat.useFallback();
    }

    private static RenderStateShard.CullStateShard cull(boolean Culls) {
        return Culls ? RenderStateShard.CULL : RenderStateShard.NO_CULL;
    }

    private static RenderStateShard.WriteMaskStateShard writeMask(boolean Colour, boolean Depth) {
        if (Colour && Depth) {
            return RenderStateShard.COLOR_DEPTH_WRITE;
        }
        return Colour ? RenderStateShard.COLOR_WRITE : RenderStateShard.DEPTH_WRITE;
    }
}
