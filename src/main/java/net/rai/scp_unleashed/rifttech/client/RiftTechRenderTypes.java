package net.rai.scp_unleashed.rifttech.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public final class RiftTechRenderTypes {
    public static final ResourceLocation ArcTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/rifttech/fx_arc.png");
    public static final ResourceLocation GlowTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/rifttech/fx_glow.png");
    public static final RenderType Arc = create("scp_unleashed_rifttech_arc", ArcTexture);
    public static final RenderType Glow = create("scp_unleashed_rifttech_glow", GlowTexture);
    public static final RenderType ArcCore = create("scp_unleashed_rifttech_arc_core", ArcTexture, RenderStateShard.COLOR_DEPTH_WRITE);
    public static final ResourceLocation VoidTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/rifttech/fx_void.png");
    public static final RenderType Void = RenderType.entityTranslucentEmissive(VoidTexture);

    private RiftTechRenderTypes() {
    }

    private static RenderType create(String Name, ResourceLocation Texture) {
        return create(Name, Texture, RenderStateShard.COLOR_WRITE);
    }

    private static RenderType create(String Name, ResourceLocation Texture, RenderStateShard.WriteMaskStateShard Mask) {
        return RenderType.create(Name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 2048, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(Texture, true, false))
                        .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                        .setWriteMaskState(Mask)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(false));
    }
}
