package net.rai.scp_unleashed.dclass.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.guns.client.NpcGunPose;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Map;

public class HumanoidGearLayer<T extends DClassLike> extends GeoRenderLayer<T> {
    private static final ResourceLocation Wings = ResourceLocation.withDefaultNamespace("textures/entity/elytra.png");
    private final HumanoidModel<LivingEntity> Parent;
    private final HumanoidModel<LivingEntity> Inner;
    private final HumanoidModel<LivingEntity> Outer;
    private final ElytraModel<LivingEntity> Elytra;
    private final Map<SkullBlock.Type, SkullModelBase> Skulls;
    private boolean Pending;

    public HumanoidGearLayer(GeoRenderer<T> Renderer) {
        super(Renderer);
        Minecraft Game = Minecraft.getInstance();
        Parent = new HumanoidModel<>(Game.getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        Inner = new HumanoidModel<>(Game.getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        Outer = new HumanoidModel<>(Game.getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
        Elytra = new ElytraModel<>(Game.getEntityModels().bakeLayer(ModelLayers.ELYTRA));
        Elytra.young = false;
        Skulls = SkullBlockRenderer.createSkullRenderers(Game.getEntityModels());
    }

    @Override
    public void preRender(PoseStack Pose, T Owner, BakedGeoModel Baked, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                          float Partial, int Light, int Overlay) {
        Pending = true;
    }

    @Override
    public void renderForBone(PoseStack Pose, T Owner, GeoBone Bone, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float Partial, int Light, int Overlay) {
        if (Owner.isSpecimen() || Owner.isInvisible()) {
            return;
        }
        if (Pending && Bone.getParent() == null) {
            Pending = false;
            armour(Pose, Owner, Bone, Buffers, Partial, Light);
        }
        switch (Bone.getName()) {
            case "head" -> headItem(Pose, Owner, Buffers, Light);
            case "body" -> elytra(Pose, Owner, Buffers, Light, Partial);
            case "right_arm" -> held(Pose, Owner, Bone, HumanoidArm.RIGHT, Buffers, Light, Partial);
            case "left_arm" -> held(Pose, Owner, Bone, HumanoidArm.LEFT, Buffers, Light, Partial);
            default -> {
            }
        }
    }

    private static void toModelSpace(PoseStack Pose) {
        HumanoidArmourPass.toModelSpace(Pose);
    }

    private void armour(PoseStack Pose, T Owner, GeoBone Top, MultiBufferSource Buffers, float Partial, int Light) {
        GeoModel<T> Shape = getGeoModel();
        HumanoidArmourPass.renderWorn(Pose, Buffers, Owner, Owner::getItemBySlot, Name -> Shape.getAnimationProcessor().getBone(Name), Top, Partial, Light, Parent, Inner, Outer);
    }

    private void held(PoseStack Pose, T Owner, GeoBone Bone, HumanoidArm Arm, MultiBufferSource Buffers, int Light, float Partial) {
        boolean MainSide = Owner.getMainArm() == Arm;
        ItemStack Stack = MainSide ? Owner.getMainHandItem() : Owner.getOffhandItem();
        if (Stack.isEmpty()) {
            return;
        }
        boolean Left = Arm == HumanoidArm.LEFT;
        Pose.pushPose();
        toModelSpace(Pose);
        Pose.translate((Left ? 5.0f : -5.0f) / 16.0f, 2.0f / 16.0f, 0.0f);
        NpcGunPose.level(Pose, Owner, Stack, Bone, Left, Partial);
        Pose.mulPose(Axis.XP.rotationDegrees(-90.0f));
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f));
        Pose.translate((Left ? -1.0f : 1.0f) / 16.0f, 0.125f, -0.625f);
        ItemDisplayContext Context = Left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        LivingEntity Holder = Owner.isPuppet() && Owner.puppetOwner() != null ? Owner.puppetOwner() : Owner;
        Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(Holder, Stack, Context, Left, Pose, Buffers, Light);
        Pose.popPose();
    }

    private void headItem(PoseStack Pose, T Owner, MultiBufferSource Buffers, int Light) {
        ItemStack Stack = Owner.getItemBySlot(EquipmentSlot.HEAD);
        if (Stack.isEmpty() || Stack.getItem() instanceof ArmorItem Armour && Armour.getEquipmentSlot() == EquipmentSlot.HEAD) {
            return;
        }
        Pose.pushPose();
        toModelSpace(Pose);
        if (Stack.getItem() instanceof BlockItem Placed && Placed.getBlock() instanceof AbstractSkullBlock Skull) {
            Pose.scale(1.1875f, -1.1875f, -1.1875f);
            Pose.translate(-0.5, 0.0, -0.5);
            ResolvableProfile Profile = Stack.get(DataComponents.PROFILE);
            SkullBlock.Type Kind = Skull.getType();
            SkullModelBase Shape = Skulls.get(Kind);
            RenderType Look = SkullBlockRenderer.getRenderType(Kind, Profile);
            SkullBlockRenderer.renderSkull(null, 180.0f, 0.0f, Pose, Buffers, Light, Shape, Look);
        } else {
            CustomHeadLayer.translateToHead(Pose, false);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(Owner, Stack, ItemDisplayContext.HEAD, false, Pose, Buffers, Light);
        }
        Pose.popPose();
    }

    private void elytra(PoseStack Pose, T Owner, MultiBufferSource Buffers, int Light, float Partial) {
        ItemStack Stack = Owner.getItemBySlot(EquipmentSlot.CHEST);
        if (!Stack.is(Items.ELYTRA)) {
            return;
        }
        LivingEntity Wearer = Owner.puppetOwner() != null ? Owner.puppetOwner() : Owner;
        Pose.pushPose();
        toModelSpace(Pose);
        Pose.translate(0.0f, 0.0f, 0.125f);
        Elytra.setupAnim(Wearer, 0.0f, 0.0f, Owner.tickCount + Partial, 0.0f, 0.0f);
        VertexConsumer Consumer = ItemRenderer.getArmorFoilBuffer(Buffers, RenderType.armorCutoutNoCull(Wings), Stack.hasFoil());
        Elytra.renderToBuffer(Pose, Consumer, Light, OverlayTexture.NO_OVERLAY);
        Pose.popPose();
    }
}
