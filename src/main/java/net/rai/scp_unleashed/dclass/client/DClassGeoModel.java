package net.rai.scp_unleashed.dclass.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.HumanoidEyes;
import net.rai.scp_unleashed.guns.client.NpcGunPose;
import net.rai.scp_unleashed.dclass.duty.client.DClassScrubPose;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class DClassGeoModel<T extends DClassLike> extends SharedGeoModel<T> {
    public static final ResourceLocation ModelPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/dclass/dclass.geo.json");
    public static final ResourceLocation AnimationPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "animations/entity/dclass/dclass.animation.json");
    public static final float PupilTravel = 0.5f;
    public static final float HeadPitchLimit = 50.0f;
    public static final float HeadYawLimit = 75.0f;
    public static final String[] Settled = {"waist", "body", "head", "right_arm", "left_arm", "right_leg", "left_leg"};

    @Override
    public ResourceLocation getModelResource(T Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(T Owner) {
        return DClassSkins.textureFor(Owner.appearance());
    }

    @Override
    public ResourceLocation getAnimationResource(T Owner) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(T Owner, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Nullable
    private GeoBone bone(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    @Override
    public void setCustomAnimations(T Owner, long InstanceId, AnimationState<T> Animated) {
        float Partial = Animated.getPartialTick();
        settle();
        int Style = DClassAppearance.style(Owner.appearance());
        hide("hair_curly", Style != DClassAppearance.Curly);
        hide("hair_mohawk", Style != DClassAppearance.Mohawk);
        hide("hair_tail", Style != DClassAppearance.Long);
        HumanoidEyes Eyes = Owner.eyes();
        float Closed = Eyes == null ? 0.0f : Eyes.closed(Partial);
        if (Owner.eyesShut()) {
            Closed = 1.0f;
        }
        lid("right_lid", Closed);
        lid("left_lid", Closed);
        float LookX = Eyes == null ? 0.0f : Eyes.lookX(Partial);
        float LookY = Eyes == null ? 0.0f : Eyes.lookY(Partial);
        pupil("right_pupil", LookX, LookY);
        pupil("left_pupil", LookX, LookY);
        GeoBone Mouth = bone("mouth");
        if (Mouth != null) {
            boolean Chewing = Owner.action() == DClassLike.ActionEating || Owner.isUsingItem() && isMouthUse(Owner.getUseItem()) || Owner.isPuppet() && Owner.Mirror.Using
                    && (Owner.Mirror.UseKind == UseAnim.EAT || Owner.Mirror.UseKind == UseAnim.DRINK);
            Mouth.setHidden(!(Chewing && (Owner.tickCount / 3) % 2 == 0) && !Owner.isDeadOrDying());
        }
        if (Owner.isPuppet() && Owner.Mirror.Copied) {
            PuppetPoser.apply(this::bone, Owner.Mirror);
            return;
        }
        if (Owner.isSpecimen() || Owner.isRestingPose()) {
            return;
        }
        GeoBone Head = bone("head");
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        float HeadX = 0.0f;
        float HeadY = 0.0f;
        if (Head != null && Data != null) {
            HeadX = Mth.clamp(Data.headPitch(), -HeadPitchLimit, HeadPitchLimit) * Mth.DEG_TO_RAD;
            HeadY = Mth.clamp(Mth.wrapDegrees(Data.netHeadYaw()), -HeadYawLimit, HeadYawLimit) * Mth.DEG_TO_RAD;
            if (Owner.isSwimmingPose() && Owner.swim(Partial) > 0.5f) {
                HeadX = -45.0f * Mth.DEG_TO_RAD;
            }
            Head.setRotX(Head.getRotX() + HeadX);
            Head.setRotY(Head.getRotY() + HeadY);
        }
        if (Owner.isUndead()) {
            set("right_arm", Mth.HALF_PI * 0.95f, 0.08f, 0.0f);
            set("left_arm", Mth.HALF_PI * 0.95f, -0.08f, 0.0f);
            return;
        }
        arms(Owner, -HeadX, -HeadY, Partial);
        DClassScrubPose.apply(this::bone, Owner, Partial);
    }

    private void arms(DClassLike Owner, float VanillaHeadX, float VanillaHeadY, float Partial) {
        boolean RightMain = Owner.getMainArm() == HumanoidArm.RIGHT;
        boolean Using = Owner.isPuppet() ? Owner.Mirror.Using : Owner.isUsingItem();
        InteractionHand UseHand = Owner.isPuppet() ? Owner.Mirror.UseHand : Owner.getUsedItemHand();
        UseAnim Kind = Owner.isPuppet() ? Owner.Mirror.UseKind : Using ? Owner.getUseItem().getUseAnimation() : UseAnim.NONE;
        boolean UseRight = (UseHand == InteractionHand.MAIN_HAND) == RightMain;
        if (Using) {
            String Arm = UseRight ? "right_arm" : "left_arm";
            float Side = UseRight ? 1.0f : -1.0f;
            switch (Kind) {
                case EAT, DRINK -> {
                    float Jitter = Mth.sin((Owner.tickCount + Partial) * 1.8f) * 0.08f;
                    set(Arm, 1.35f + Jitter + VanillaHeadX * -0.3f, Side * 0.55f, 0.0f);
                }
                case BLOCK -> set(Arm, 0.95f, Side * 0.52f, 0.0f);
                case BOW, CROSSBOW -> {
                    set("right_arm", Mth.HALF_PI - VanillaHeadX, 0.1f - VanillaHeadY, 0.0f);
                    set("left_arm", Mth.HALF_PI - VanillaHeadX, -0.5f - VanillaHeadY, 0.0f);
                }
                case SPEAR -> set(Arm, Mth.PI * 0.95f, 0.0f, 0.0f);
                case SPYGLASS -> set(Arm, Mth.clamp(1.9198622f - VanillaHeadX, -3.3f, 2.4f), Side * 0.2617994f - VanillaHeadY, 0.0f);
                case TOOT_HORN -> set(Arm, 1.4835298f - Mth.clamp(VanillaHeadX, -1.2f, 1.2f), Side * 0.5235988f - VanillaHeadY, 0.0f);
                default -> add(Arm, 0.3f, 0.0f, 0.0f);
            }
        } else if (!NpcGunPose.apply(this::bone, Owner, gunAiming(Owner), VanillaHeadX, VanillaHeadY, Partial)) {
            if (!Owner.getMainHandItem().isEmpty()) {
                add(RightMain ? "right_arm" : "left_arm", 0.25f, 0.0f, 0.0f);
            }
            if (!Owner.getOffhandItem().isEmpty()) {
                add(RightMain ? "left_arm" : "right_arm", 0.25f, 0.0f, 0.0f);
            }
        }
        float Swing = Owner.getAttackAnim(Partial);
        if (Swing > 0.0f) {
            boolean SwingRight = (Owner.swingingArm == InteractionHand.MAIN_HAND) == RightMain;
            String Arm = SwingRight ? "right_arm" : "left_arm";
            float Twist = Mth.sin(Mth.sqrt(Swing) * Mth.TWO_PI) * 0.2f;
            add("body", 0.0f, -Twist, 0.0f);
            float Eased = 1.0f - (float) Math.pow(1.0f - Swing, 4.0);
            float Lift = Mth.sin(Eased * Mth.PI);
            float Chop = Mth.sin(Swing * Mth.PI) * -(VanillaHeadX - 0.7f) * 0.75f;
            add(Arm, Lift * 1.2f + Chop, -Twist * 2.0f, Mth.sin(Swing * Mth.PI) * -0.4f);
        }
    }

    protected boolean gunAiming(DClassLike Owner) {
        return NpcGunPose.aiming(Owner);
    }

    private static boolean isMouthUse(ItemStack Stack) {
        UseAnim Kind = Stack.getUseAnimation();
        return Kind == UseAnim.EAT || Kind == UseAnim.DRINK;
    }

    private void hide(String Name, boolean Hidden) {
        GeoBone Found = bone(Name);
        if (Found != null) {
            Found.setHidden(Hidden);
        }
    }

    private void lid(String Name, float Closed) {
        GeoBone Found = bone(Name);
        if (Found != null) {
            Found.setHidden(Closed < 0.05f);
            Found.setScaleY(Math.max(0.05f, Closed));
        }
    }

    private void pupil(String Name, float LookX, float LookY) {
        GeoBone Found = bone(Name);
        if (Found != null) {
            Found.setPosX(-LookX * PupilTravel);
            Found.setPosY(-LookY * PupilTravel);
        }
    }

    private void add(String Name, float X, float Y, float Z) {
        GeoBone Found = bone(Name);
        if (Found != null) {
            Found.setRotX(Found.getRotX() + X);
            Found.setRotY(Found.getRotY() + Y);
            Found.setRotZ(Found.getRotZ() + Z);
        }
    }

    private void set(String Name, float X, float Y, float Z) {
        GeoBone Found = bone(Name);
        if (Found != null) {
            Found.setRotX(X);
            Found.setRotY(Y);
            Found.setRotZ(Z);
        }
    }

    private void settle() {
        for (String Name : Settled) {
            GeoBone Found = bone(Name);
            if (Found != null) {
                Found.setPosX(0.0f);
                Found.setPosY(0.0f);
                Found.setPosZ(0.0f);
            }
        }
    }
}
