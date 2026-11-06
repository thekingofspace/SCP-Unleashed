package net.rai.scp_unleashed.dclass.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public final class HumanoidArmourPass {
    public static final float ModelTop = 1.5f;
    public static final int White = 0xFFFFFFFF;
    public static final float GimbalLimit = 0.99999f;
    public static final EquipmentSlot[] Order = {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD};

    public record Motion(float LimbSwing, float LimbSwingAmount, float Partial, float AgeInTicks, float NetHeadYaw, float HeadPitch) {
        public static Motion of(LivingEntity Wearer, float Partial) {
            boolean Seated = Wearer.isPassenger() && Wearer.getVehicle() != null && Wearer.getVehicle().shouldRiderSit();
            float Amount = 0.0f;
            float Swing = 0.0f;
            if (!Seated && Wearer.isAlive()) {
                Amount = Math.min(1.0f, Wearer.walkAnimation.speed(Partial));
                Swing = Wearer.walkAnimation.position(Partial) * (Wearer.isBaby() ? 3.0f : 1.0f);
            }
            float Body = Mth.rotLerp(Partial, Wearer.yBodyRotO, Wearer.yBodyRot);
            float HeadTurn = Mth.rotLerp(Partial, Wearer.yHeadRotO, Wearer.yHeadRot);
            float Pitch = Mth.lerp(Partial, Wearer.xRotO, Wearer.getXRot());
            return new Motion(Swing, Amount, Partial, Wearer.tickCount + Partial, Mth.wrapDegrees(HeadTurn - Body), Pitch);
        }
    }

    public interface BoneSource {
        @Nullable
        GeoBone bone(String Name);
    }

    public interface ModelHook {
        Model find(HumanoidModel<LivingEntity> Base);
    }

    public interface BufferHook {
        VertexConsumer buffer(ResourceLocation Texture);
    }

    public interface GearSource {
        ItemStack gear(EquipmentSlot Slot);
    }

    private HumanoidArmourPass() {
    }

    public static void toModelSpace(PoseStack Pose) {
        Pose.scale(-1.0f, -1.0f, 1.0f);
        Pose.translate(0.0f, -ModelTop, 0.0f);
    }

    public static void pose(HumanoidModel<?> Parent, BoneSource Bones, GeoBone Top) {
        posePart(Parent.head, Bones.bone("head"), Top);
        GeoBone Hat = Bones.bone("hat");
        if (Hat == null) {
            Parent.hat.copyFrom(Parent.head);
        } else {
            posePart(Parent.hat, Hat, Top);
        }
        posePart(Parent.body, Bones.bone("body"), Top);
        posePart(Parent.rightArm, Bones.bone("right_arm"), Top);
        posePart(Parent.leftArm, Bones.bone("left_arm"), Top);
        posePart(Parent.rightLeg, Bones.bone("right_leg"), Top);
        posePart(Parent.leftLeg, Bones.bone("left_leg"), Top);
    }

    public static void posePart(ModelPart Part, @Nullable GeoBone Bone, GeoBone Top) {
        if (Bone == null) {
            Part.resetPose();
            return;
        }
        Matrix4f Relative = relative(Bone, Top);
        Vector3f Pivot = Relative.transformPosition(new Vector3f(Bone.getPivotX() / 16.0f, Bone.getPivotY() / 16.0f, Bone.getPivotZ() / 16.0f));
        Part.x = -Pivot.x * 16.0f;
        Part.y = 24.0f - Pivot.y * 16.0f;
        Part.z = Pivot.z * 16.0f;
        Matrix3f Linear = Relative.get3x3(new Matrix3f());
        float ScaleX = new Vector3f(Linear.m00, Linear.m01, Linear.m02).length();
        float ScaleY = new Vector3f(Linear.m10, Linear.m11, Linear.m12).length();
        float ScaleZ = new Vector3f(Linear.m20, Linear.m21, Linear.m22).length();
        if (ScaleX < 1.0e-6f || ScaleY < 1.0e-6f || ScaleZ < 1.0e-6f) {
            Part.resetPose();
            Part.xScale = Math.max(ScaleX, 1.0e-4f);
            Part.yScale = Math.max(ScaleY, 1.0e-4f);
            Part.zScale = Math.max(ScaleZ, 1.0e-4f);
            return;
        }
        Linear.scale(1.0f / ScaleX, 1.0f / ScaleY, 1.0f / ScaleZ);
        float R00 = Linear.m00;
        float R10 = Linear.m01;
        float R20 = -Linear.m02;
        float R01 = Linear.m10;
        float R11 = Linear.m11;
        float R21 = -Linear.m12;
        float R22 = Linear.m22;
        float Sine = Mth.clamp(-R20, -1.0f, 1.0f);
        Part.yRot = (float) Math.asin(Sine);
        if (Math.abs(R20) < GimbalLimit) {
            Part.xRot = (float) Math.atan2(R21, R22);
            Part.zRot = (float) Math.atan2(R10, R00);
        } else {
            Part.xRot = 0.0f;
            Part.zRot = (float) Math.atan2(-R01, R11);
        }
        Part.xScale = ScaleX;
        Part.yScale = ScaleY;
        Part.zScale = ScaleZ;
    }

    public static Matrix4f relative(GeoBone Bone, GeoBone Top) {
        List<GeoBone> Path = new ArrayList<>();
        GeoBone Walker = Bone;
        while (Walker != null && Walker != Top) {
            Path.add(0, Walker);
            Walker = Walker.getParent();
        }
        Matrix4f Chain = chain(Path);
        if (Walker == Top) {
            return Chain;
        }
        List<GeoBone> TopPath = new ArrayList<>();
        for (GeoBone Up = Top; Up != null; Up = Up.getParent()) {
            TopPath.add(0, Up);
        }
        return chain(TopPath).invert().mul(Chain);
    }

    private static Matrix4f chain(List<GeoBone> Path) {
        PoseStack Stack = new PoseStack();
        for (GeoBone Step : Path) {
            RenderUtil.prepMatrixForBone(Stack, Step);
        }
        return new Matrix4f(Stack.last().pose());
    }

    public static void setPartVisibility(HumanoidModel<?> Shape, EquipmentSlot Slot) {
        Shape.setAllVisible(false);
        switch (Slot) {
            case HEAD -> {
                Shape.head.visible = true;
                Shape.hat.visible = true;
            }
            case CHEST -> {
                Shape.body.visible = true;
                Shape.rightArm.visible = true;
                Shape.leftArm.visible = true;
            }
            case LEGS -> {
                Shape.body.visible = true;
                Shape.rightLeg.visible = true;
                Shape.leftLeg.visible = true;
            }
            case FEET -> {
                Shape.rightLeg.visible = true;
                Shape.leftLeg.visible = true;
            }
            default -> {
            }
        }
    }

    public static Model prepare(HumanoidModel<LivingEntity> Parent, HumanoidModel<LivingEntity> Base, EquipmentSlot Slot, ModelHook Hook) {
        Parent.copyPropertiesTo(Base);
        setPartVisibility(Base, Slot);
        return Hook.find(Base);
    }

    public static void drawLayers(Model Shown, PoseStack Pose, BufferHook Buffers, int Light, List<ResourceLocation> Textures, int[] Colours) {
        for (int Index = 0; Index < Textures.size(); Index++) {
            if (Colours[Index] != 0) {
                Shown.renderToBuffer(Pose, Buffers.buffer(Textures.get(Index)), Light, OverlayTexture.NO_OVERLAY, Colours[Index]);
            }
        }
    }

    public static void dress(HumanoidModel<?> Parent, LivingEntity Wearer, float Partial) {
        Parent.attackTime = Wearer.getAttackAnim(Partial);
        Parent.riding = Wearer.isPassenger() && Wearer.getVehicle() != null && Wearer.getVehicle().shouldRiderSit();
        Parent.young = Wearer.isBaby();
        Parent.crouching = Wearer.isCrouching();
        boolean RightMain = Wearer.getMainArm() == HumanoidArm.RIGHT;
        HumanoidModel.ArmPose MainPose = Wearer.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        HumanoidModel.ArmPose OffPose = Wearer.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        Parent.rightArmPose = RightMain ? MainPose : OffPose;
        Parent.leftArmPose = RightMain ? OffPose : MainPose;
    }

    public static void renderWorn(PoseStack Pose, MultiBufferSource Buffers, LivingEntity Wearer, GearSource Gear, BoneSource Bones, GeoBone Top, float Partial, int Light,
                                  HumanoidModel<LivingEntity> Parent, HumanoidModel<LivingEntity> Inner, HumanoidModel<LivingEntity> Outer) {
        ItemStack[] Worn = new ItemStack[Order.length];
        boolean Any = false;
        for (int Index = 0; Index < Order.length; Index++) {
            Worn[Index] = Gear.gear(Order[Index]);
            Any |= !Worn[Index].isEmpty();
        }
        if (!Any) {
            return;
        }
        pose(Parent, Bones, Top);
        dress(Parent, Wearer, Partial);
        Motion Moving = Motion.of(Wearer, Partial);
        Pose.pushPose();
        toModelSpace(Pose);
        for (int Index = 0; Index < Order.length; Index++) {
            renderSlot(Pose, Buffers, Wearer, Worn[Index], Order[Index], Light, Parent, Order[Index] == EquipmentSlot.LEGS ? Inner : Outer, Moving);
        }
        Pose.popPose();
    }

    public static void renderSlot(PoseStack Pose, MultiBufferSource Buffers, LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot, int Light,
                                  HumanoidModel<LivingEntity> Parent, HumanoidModel<LivingEntity> Base, Motion Moving) {
        if (Stack.isEmpty() || renderProvided(Pose, Buffers, Wearer, Stack, Slot, Light, Parent, Base, Moving)) {
            return;
        }
        if (!(Stack.getItem() instanceof ArmorItem Armour) || Armour.getEquipmentSlot() != Slot) {
            return;
        }
        Model Shown = prepare(Parent, Base, Slot, Found -> ClientHooks.getArmorModel(Wearer, Stack, Slot, Found));
        if (Shown instanceof GeoArmorRenderer<?> Geo) {
            renderGeo(Geo, Pose, Buffers, Wearer, Stack, Slot, Light, Base, Moving);
            return;
        }
        boolean UsesInner = Slot == EquipmentSlot.LEGS;
        ArmorMaterial Material = Armour.getMaterial().value();
        IClientItemExtensions Extensions = IClientItemExtensions.of(Stack);
        Extensions.setupModelAnimations(Wearer, Stack, Slot, Shown, Moving.LimbSwing(), Moving.LimbSwingAmount(), Moving.Partial(), Moving.AgeInTicks(),
                Moving.NetHeadYaw(), Moving.HeadPitch());
        int Fallback = Extensions.getDefaultDyeColor(Stack);
        List<ResourceLocation> Textures = new ArrayList<>();
        int[] Colours = new int[Material.layers().size()];
        for (int Index = 0; Index < Material.layers().size(); Index++) {
            ArmorMaterial.Layer Layer = Material.layers().get(Index);
            Colours[Index] = Extensions.getArmorLayerTintColor(Stack, Wearer, Layer, Index, Fallback);
            Textures.add(Colours[Index] == 0 ? null : ClientHooks.getArmorTexture(Wearer, Stack, Layer, UsesInner, Slot));
        }
        drawLayers(Shown, Pose, Texture -> Buffers.getBuffer(RenderType.armorCutoutNoCull(Texture)), Light, Textures, Colours);
        ArmorTrim Trim = Stack.get(DataComponents.TRIM);
        if (Trim != null) {
            TextureAtlasSprite Sprite = Minecraft.getInstance().getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET)
                    .getSprite(UsesInner ? Trim.innerTexture(Armour.getMaterial()) : Trim.outerTexture(Armour.getMaterial()));
            Shown.renderToBuffer(Pose, Sprite.wrap(Buffers.getBuffer(Sheets.armorTrimsSheet(Trim.pattern().value().decal()))), Light, OverlayTexture.NO_OVERLAY);
        }
        if (Stack.hasFoil()) {
            Shown.renderToBuffer(Pose, Buffers.getBuffer(RenderType.armorEntityGlint()), Light, OverlayTexture.NO_OVERLAY);
        }
    }

    private static boolean renderProvided(PoseStack Pose, MultiBufferSource Buffers, LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot, int Light,
                                          HumanoidModel<LivingEntity> Parent, HumanoidModel<LivingEntity> Base, Motion Moving) {
        Item Worn = Stack.getItem();
        if (!(Worn instanceof GeoItem) || !(Worn instanceof Equipable Gear) || Gear.getEquipmentSlot() != Slot) {
            return false;
        }
        HumanoidModel<?> Provided = GeoRenderProvider.of(Worn).getGeoArmorRenderer(Wearer, Stack, Slot, Base);
        if (Provided == null) {
            return false;
        }
        Parent.copyPropertiesTo(Base);
        setPartVisibility(Base, Slot);
        if (Provided instanceof GeoArmorRenderer<?> Geo) {
            renderGeo(Geo, Pose, Buffers, Wearer, Stack, Slot, Light, Base, Moving);
        } else {
            ClientHooks.copyModelProperties(Base, Provided);
            Provided.renderToBuffer(Pose, null, Light, OverlayTexture.NO_OVERLAY, White);
        }
        return true;
    }

    public static void renderGeo(GeoArmorRenderer<?> Geo, PoseStack Pose, MultiBufferSource Buffers, LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot,
                                 int Light, HumanoidModel<LivingEntity> Base, Motion Moving) {
        Geo.prepForRender(Wearer, Stack, Slot, Base, Buffers, Moving.Partial(), Moving.LimbSwing(), Moving.LimbSwingAmount(), Moving.NetHeadYaw(), Moving.HeadPitch());
        ClientHooks.copyModelProperties(Base, Geo);
        showGeoSlot(Geo, Slot);
        Geo.renderToBuffer(Pose, null, Light, OverlayTexture.NO_OVERLAY, White);
    }

    public static <G extends Item & GeoItem> void showGeoSlot(GeoArmorRenderer<G> Geo, EquipmentSlot Slot) {
        GeoModel<G> Shape = Geo.getGeoModel();
        if (Shape == null) {
            return;
        }
        GeoBone Head = Geo.getHeadBone(Shape);
        GeoBone Body = Geo.getBodyBone(Shape);
        GeoBone RightArm = Geo.getRightArmBone(Shape);
        GeoBone LeftArm = Geo.getLeftArmBone(Shape);
        GeoBone RightLeg = Geo.getRightLegBone(Shape);
        GeoBone LeftLeg = Geo.getLeftLegBone(Shape);
        GeoBone RightBoot = Geo.getRightBootBone(Shape);
        GeoBone LeftBoot = Geo.getLeftBootBone(Shape);
        reveal(Head, Slot == EquipmentSlot.HEAD);
        reveal(Body, Slot == EquipmentSlot.CHEST);
        reveal(RightArm, Slot == EquipmentSlot.CHEST);
        reveal(LeftArm, Slot == EquipmentSlot.CHEST);
        reveal(RightLeg, Slot == EquipmentSlot.LEGS);
        reveal(LeftLeg, Slot == EquipmentSlot.LEGS);
        reveal(RightBoot, Slot == EquipmentSlot.FEET);
        reveal(LeftBoot, Slot == EquipmentSlot.FEET);
    }

    private static void reveal(@Nullable GeoBone Bone, boolean Shown) {
        if (Bone != null) {
            Bone.setHidden(!Shown);
        }
    }
}
