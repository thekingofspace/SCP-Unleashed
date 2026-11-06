package net.rai.scp_unleashed.guns.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.Scp_unleashed;

public final class GunRenderTypes {
    public static final RenderType Glow = RenderType.create(Scp_unleashed.ModId + "_guns_glow", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 8192, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(RenderStateShard.PARTICLES_TARGET)
                    .createCompositeState(false));
    public static final RenderType HandGlow = RenderType.create(Scp_unleashed.ModId + "_guns_hand_glow", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1024, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    public static final RenderType Smoke = RenderType.create(Scp_unleashed.ModId + "_guns_smoke", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 2048, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(RenderStateShard.PARTICLES_TARGET)
                    .createCompositeState(false));

    private GunRenderTypes() {
    }
}
