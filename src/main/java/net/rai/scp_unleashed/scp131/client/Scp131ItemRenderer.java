package net.rai.scp_unleashed.scp131.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp131.Scp131PodData;
import net.rai.scp_unleashed.scp131.Scp131PodItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class Scp131ItemRenderer extends GeoItemRenderer<Scp131PodItem> {
    public Scp131ItemRenderer() {
        super(new Scp131ItemModel());
        addRenderLayer(new Scp131GlassLayer<>(this));
    }

    @Override
    public long getInstanceId(Scp131PodItem Animatable) {
        ItemStack Stack = getCurrentItemStack();
        return Stack == null ? Long.MAX_VALUE : Scp131PodData.of(Stack).seed();
    }

    @Override
    public void preRender(PoseStack Pose, Scp131PodItem Animatable, BakedGeoModel Baked, @Nullable MultiBufferSource Buffers, @Nullable VertexConsumer Buffer, boolean IsReRender,
                          float PartialTick, int Light, int Overlay, int Colour) {
        super.preRender(Pose, Animatable, Baked, Buffers, Buffer, IsReRender, PartialTick, Light, Overlay, Colour);
        if (!IsReRender) {
            Pose.scale(Scp131GeoModel.ModelScale, Scp131GeoModel.ModelScale, Scp131GeoModel.ModelScale);
        }
    }

    @Override
    public void setupLightingForGuiRender() {
        Lighting.setupFor3DItems();
    }
}
