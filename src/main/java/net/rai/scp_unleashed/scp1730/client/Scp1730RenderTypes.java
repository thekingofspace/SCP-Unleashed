package net.rai.scp_unleashed.scp1730.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public final class Scp1730RenderTypes {
    public static final RenderType Glow = RenderType.create("scp1730_glow_shell", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 16384, false, false,
            RenderType.CompositeState.builder().setShaderState(RenderStateShard.POSITION_COLOR_SHADER).setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL).setWriteMaskState(RenderStateShard.COLOR_WRITE).createCompositeState(false));

    private Scp1730RenderTypes() {
    }
}
