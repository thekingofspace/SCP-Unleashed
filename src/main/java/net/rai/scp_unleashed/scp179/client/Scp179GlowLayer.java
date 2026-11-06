package net.rai.scp_unleashed.scp179.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class Scp179GlowLayer extends GeoRenderLayer<Scp179Figure> {
    public Scp179GlowLayer(GeoRenderer<Scp179Figure> Renderer) {
        super(Renderer);
    }

    @Override
    public void render(PoseStack Pose, Scp179Figure Figure, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (Buffers == null) {
            return;
        }
        RenderType Glow = RenderType.eyes(Scp179FigureModel.GlowPath);
        float Level = Figure.glow();
        int Colour = FastColor.ARGB32.colorFromFloat(1.0f, Level, Level, Level);
        getRenderer().reRender(Model, Pose, Buffers, Figure, Glow, Buffers.getBuffer(Glow), PartialTick, LightTexture.FULL_BRIGHT, PackedOverlay, Colour);
    }
}
