package net.rai.scp_unleashed.rift.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.VeilRenderBridge;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;

public final class RiftRenderTypes {
    public static final ResourceLocation FallbackTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/shadercompat/rift_fallback.png");
    public static final RenderType FallbackCore = RenderType.entityCutoutNoCull(FallbackTexture, false);
    public static final RenderType FallbackGlow = RenderType.eyes(FallbackTexture);
    public static final ResourceLocation CoreProgram = VeilPost.program("rift/core");
    public static final ResourceLocation GlowProgram = VeilPost.program("rift/glow");

    public static final RenderStateShard.TransparencyStateShard Premultiplied = new RenderStateShard.TransparencyStateShard("scp_unleashed_rift_premultiplied", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    public static final RenderType Core = RenderType.create("scp_unleashed_rift_core", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(VeilRenderBridge.shaderState(CoreProgram))
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setOverlayState(RenderStateShard.NO_OVERLAY)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(false));

    public static final RenderType Glow = RenderType.create("scp_unleashed_rift_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(VeilRenderBridge.shaderState(GlowProgram))
                    .setTransparencyState(Premultiplied)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setOverlayState(RenderStateShard.NO_OVERLAY)
                    .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    private RiftRenderTypes() {
    }

    public static boolean ready() {
        return VeilPost.programReady(CoreProgram) && VeilPost.programReady(GlowProgram);
    }

    public static boolean fallback() {
        return ShaderCompat.useFallback() || !ready();
    }

    public static boolean available() {
        return fallback() || ready();
    }

    public static boolean shadowPass() {
        return ShaderCompat.renderingShadowPass();
    }
}
