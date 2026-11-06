package net.rai.scp_unleashed.scp539.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.rai.scp_unleashed.scp539.Scp539Stone;

public class Scp539StoneRenderer extends EntityRenderer<Scp539Stone> {
    public static final float Scale = 0.62f;
    public static final float SpinPerTick = 38.0f;
    public static final float Tilt = 9.0f;
    public static final float Lift = 0.1f;
    public static final double NearCamera = 12.25;

    private final ItemRenderer Items;

    public Scp539StoneRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        Items = Context.getItemRenderer();
        shadowRadius = 0.12f;
        shadowStrength = 0.5f;
    }

    @Override
    public void render(Scp539Stone Stone, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        if (Stone.tickCount < 2 && entityRenderDispatcher.camera.getEntity().distanceToSqr(Stone) < NearCamera) {
            return;
        }
        Pose.pushPose();
        Pose.translate(0.0f, Lift, 0.0f);
        Pose.mulPose(Axis.YP.rotationDegrees((Stone.tickCount + PartialTick) * SpinPerTick));
        Pose.mulPose(Axis.XP.rotationDegrees(Tilt));
        Pose.scale(Scale, Scale, Scale);
        Items.renderStatic(Stone.getItem(), ItemDisplayContext.NONE, Light, OverlayTexture.NO_OVERLAY, Pose, Buffers, Stone.level(), Stone.getId());
        Pose.popPose();
        super.render(Stone, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp539Stone Stone) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
