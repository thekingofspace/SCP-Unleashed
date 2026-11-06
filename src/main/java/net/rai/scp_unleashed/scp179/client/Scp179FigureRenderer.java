package net.rai.scp_unleashed.scp179.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

public class Scp179FigureRenderer extends GeoObjectRenderer<Scp179Figure> {
    public Scp179FigureRenderer() {
        super(new Scp179FigureModel());
        addRenderLayer(new Scp179GlowLayer(this));
    }

    @Override
    public void preRender(PoseStack Pose, Scp179Figure Figure, BakedGeoModel Model, @Nullable MultiBufferSource Buffers, @Nullable VertexConsumer Buffer, boolean IsReRender,
                          float PartialTick, int Light, int Overlay, int Colour) {
        objectRenderTranslations = new Matrix4f(Pose.last().pose());
        scaleModelForRender(scaleWidth, scaleHeight, Pose, Figure, Model, IsReRender, PartialTick, Light, Overlay);
    }
}
