package net.rai.scp_unleashed.scp999.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp999.Scp999Blob;
import net.rai.scp_unleashed.scp999.Scp999Registry;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class Scp999GelLayer extends GeoRenderLayer<Scp999Blob> {
    public static final ResourceLocation GelTexture = Scp999Registry.location("textures/entity/scp999/scp999_blob_gel.png");
    public static final int Clear = 0xFFFFFFFF;

    public Scp999GelLayer(GeoRenderer<Scp999Blob> Renderer) {
        super(Renderer);
    }

    @Override
    public void render(PoseStack Pose, Scp999Blob Blob, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (Buffers == null || ShaderCompat.renderingShadowPass() || Blob.isInvisible()) {
            return;
        }
        RenderType Gel = RenderType.entityTranslucent(GelTexture);
        getRenderer().reRender(Model, Pose, Buffers, Blob, Gel, Buffers.getBuffer(Gel), PartialTick, PackedLight, PackedOverlay, Clear);
    }
}
