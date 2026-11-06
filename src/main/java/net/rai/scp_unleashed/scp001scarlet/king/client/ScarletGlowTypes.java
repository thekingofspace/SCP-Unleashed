package net.rai.scp_unleashed.scp001scarlet.king.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

public final class ScarletGlowTypes extends RenderType {
    public static final RenderType Glow = create("scp_unleashed_scarlet_king_glow", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setWriteMaskState(COLOR_WRITE)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setOutputState(WEATHER_TARGET)
                    .createCompositeState(false));

    private ScarletGlowTypes(String Name, VertexFormat Format, VertexFormat.Mode Mode, int Size, boolean Crumbling, boolean Sort, Runnable Setup, Runnable Clear) {
        super(Name, Format, Mode, Size, Crumbling, Sort, Setup, Clear);
    }
}
