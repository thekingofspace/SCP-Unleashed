package net.rai.scp_unleashed.grenade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.grenade.GrenadeEntity;
import net.rai.scp_unleashed.grenade.GrenadeKind;
import net.rai.scp_unleashed.grenade.GrenadeRegistry;

import java.util.EnumMap;
import java.util.Map;

public class GrenadeRenderer extends EntityRenderer<GrenadeEntity> {
    public static final float ModelScale = 0.5F;
    private final ItemRenderer Items;
    private final Map<GrenadeKind, ItemStack> Shown = new EnumMap<>(GrenadeKind.class);

    public GrenadeRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.Items = Context.getItemRenderer();
        this.shadowRadius = 0.12F;
        this.shadowStrength = 0.6F;
    }

    @Override
    public void render(GrenadeEntity Grenade, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        if (Grenade.isSpent()) {
            return;
        }
        ItemStack Stack = Shown.computeIfAbsent(Grenade.getKind(), Kind -> new ItemStack(GrenadeRegistry.grenadeItem(Kind)));
        Pose.pushPose();
        Pose.translate(0.0, GrenadeEntity.CentreLift, 0.0);
        Pose.mulPose(Axis.YP.rotation(Grenade.getHeading()));
        Pose.mulPose(Axis.XP.rotation(Grenade.getSpin(PartialTick)));
        Pose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        Pose.scale(ModelScale, ModelScale, ModelScale);
        Items.renderStatic(Stack, ItemDisplayContext.NONE, PackedLight, OverlayTexture.NO_OVERLAY, Pose, Buffers, Grenade.level(), Grenade.getId());
        Pose.popPose();
        super.render(Grenade, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GrenadeEntity Grenade) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
