package net.rai.scp_unleashed.disperser.client;

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
import net.rai.scp_unleashed.disperser.OrganicGrenadeEntity;
import net.rai.scp_unleashed.disperser.OrganicGrenadeItem;
import net.rai.scp_unleashed.grenade.GrenadeEntity;
import net.rai.scp_unleashed.grenade.client.GrenadeRenderer;

import java.util.HashMap;
import java.util.Map;

public class OrganicGrenadeRenderer extends EntityRenderer<OrganicGrenadeEntity> {
    private final ItemRenderer Items;
    private final Map<ResourceLocation, ItemStack> Shown = new HashMap<>();
    private final ItemStack Empty = OrganicGrenadeItem.loaded(null);

    public OrganicGrenadeRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.Items = Context.getItemRenderer();
        this.shadowRadius = 0.12F;
        this.shadowStrength = 0.6F;
    }

    @Override
    public void render(OrganicGrenadeEntity Grenade, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        if (Grenade.isSpent()) {
            return;
        }
        ResourceLocation Culture = Grenade.getCulture();
        ItemStack Stack = Culture == null ? Empty : Shown.computeIfAbsent(Culture, OrganicGrenadeItem::loaded);
        Pose.pushPose();
        Pose.translate(0.0, GrenadeEntity.CentreLift, 0.0);
        Pose.mulPose(Axis.YP.rotation(Grenade.getHeading()));
        Pose.mulPose(Axis.XP.rotation(Grenade.getSpin(PartialTick)));
        Pose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        Pose.scale(GrenadeRenderer.ModelScale, GrenadeRenderer.ModelScale, GrenadeRenderer.ModelScale);
        Items.renderStatic(Stack, ItemDisplayContext.NONE, PackedLight, OverlayTexture.NO_OVERLAY, Pose, Buffers, Grenade.level(), Grenade.getId());
        Pose.popPose();
        super.render(Grenade, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(OrganicGrenadeEntity Grenade) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
