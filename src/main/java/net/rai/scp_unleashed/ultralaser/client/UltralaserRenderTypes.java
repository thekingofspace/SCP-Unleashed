package net.rai.scp_unleashed.ultralaser.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.function.Function;

public final class UltralaserRenderTypes {
    public static final ResourceLocation BeamTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/misc/ultralaser/beam.png");
    public static final ResourceLocation FlareTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/misc/ultralaser/flare.png");
    public static final ResourceLocation RingTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/misc/ultralaser/ring.png");
    private static final Function<ResourceLocation, RenderType> Glow = Util.memoize(UltralaserRenderTypes::create);

    private UltralaserRenderTypes() {
    }

    public static RenderType glow(ResourceLocation Texture) {
        return Glow.apply(Texture);
    }

    private static RenderType create(ResourceLocation Texture) {
        return RenderType.create("scp_unleashed_ultralaser_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 4096, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(Texture, false, false))
                        .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(false));
    }
}
