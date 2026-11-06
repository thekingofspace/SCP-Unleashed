package net.rai.scp_unleashed.extraction.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.rai.scp_unleashed.Scp_unleashed;

public final class ExtractionRenderTypes {
    public static final RenderType Beam = RenderType.create(Scp_unleashed.ModId + "_extraction_beam", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(RenderStateShard.PARTICLES_TARGET)
                    .createCompositeState(false));

    private ExtractionRenderTypes() {
    }
}
