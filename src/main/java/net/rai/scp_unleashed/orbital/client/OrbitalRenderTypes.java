package net.rai.scp_unleashed.orbital.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.function.Function;

public final class OrbitalRenderTypes {
    public static final ResourceLocation BeamTexture = texture("beam");
    public static final ResourceLocation RingTexture = texture("ring");
    public static final ResourceLocation ShockTexture = texture("shock");
    public static final ResourceLocation FlareTexture = texture("flare");
    public static final ResourceLocation FenceTexture = texture("fence");
    private static final Function<ResourceLocation, RenderType> Glow = Util.memoize(Texture -> create(Texture, "glow", RenderStateShard.ADDITIVE_TRANSPARENCY));
    private static final Function<ResourceLocation, RenderType> Tint = Util.memoize(Texture -> create(Texture, "tint", RenderStateShard.TRANSLUCENT_TRANSPARENCY));

    private OrbitalRenderTypes() {
    }

    private static ResourceLocation texture(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/misc/orbital/" + Name + ".png");
    }

    public static RenderType glow(ResourceLocation Texture) {
        return Glow.apply(Texture);
    }

    public static RenderType tint(ResourceLocation Texture) {
        return Tint.apply(Texture);
    }

    private static RenderType create(ResourceLocation Texture, String Kind, RenderStateShard.TransparencyStateShard Blend) {
        return RenderType.create("scp_unleashed_orbital_" + Kind, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 4096, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(Texture, false, false))
                        .setTransparencyState(Blend)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(false));
    }
}
