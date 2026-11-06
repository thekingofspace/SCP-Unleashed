package net.rai.scp_unleashed.tesla.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public final class TeslaRenderTypes {
    private static final Function<ResourceLocation, RenderType> Additive = Util.memoize(Texture -> create("scp_unleashed_tesla_additive", Texture, RenderStateShard.NO_LAYERING));
    private static final Function<ResourceLocation, RenderType> Surface = Util.memoize(Texture -> create("scp_unleashed_tesla_surface", Texture, RenderStateShard.POLYGON_OFFSET_LAYERING));

    private TeslaRenderTypes() {
    }

    public static RenderType additive(ResourceLocation Texture) {
        return Additive.apply(Texture);
    }

    public static RenderType surface(ResourceLocation Texture) {
        return Surface.apply(Texture);
    }

    private static RenderType create(String Name, ResourceLocation Texture, RenderStateShard.LayeringStateShard Layering) {
        return RenderType.create(Name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(Texture, true, false))
                        .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLayeringState(Layering)
                        .createCompositeState(false));
    }
}
