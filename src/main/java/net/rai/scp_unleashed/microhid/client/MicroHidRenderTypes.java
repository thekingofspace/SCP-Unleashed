package net.rai.scp_unleashed.microhid.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.tesla.client.TeslaRenderTypes;

import java.util.function.Function;

public final class MicroHidRenderTypes {
    private static final Function<ResourceLocation, RenderType> Hand = Util.memoize(Texture -> RenderType.create("scp_unleashed_micro_hid_hand", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, false, true, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(Texture, true, false))
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLayeringState(RenderStateShard.NO_LAYERING)
                    .createCompositeState(false)));
    private static final Function<ResourceLocation, RenderType> Depth = Util.memoize(Texture -> RenderType.create("scp_unleashed_micro_hid_depth", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1536, false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(Texture, false, false))
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.DEPTH_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setLayeringState(RenderStateShard.NO_LAYERING)
                    .createCompositeState(false)));

    private MicroHidRenderTypes() {
    }

    public static RenderType world(ResourceLocation Texture) {
        return TeslaRenderTypes.additive(Texture);
    }

    public static RenderType hand(ResourceLocation Texture) {
        return Hand.apply(Texture);
    }

    public static RenderType depth(ResourceLocation Texture) {
        return Depth.apply(Texture);
    }
}
