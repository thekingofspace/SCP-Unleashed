package net.rai.scp_unleashed.raid.hostile.client;

import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.guns.npc.NpcGunner;
import net.rai.scp_unleashed.raid.chaos.ChaosInsurgent;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class HostileRaiderGeoModel extends DClassGeoModel<HostileRaider> {
    public static final float[] RopeRight = {2.95f, 0.05f, -0.16f};
    public static final float[] RopeLeft = {2.62f, -0.05f, 0.16f};
    public static final float[] RopeRightLeg = {0.42f, 0.0f, 0.06f};
    public static final float[] RopeLeftLeg = {0.12f, 0.0f, -0.08f};
    public static final float[] Cradle = {0.8f, 0.32f, 0.0f};
    public static final float[] CradleLeft = {0.9f, -0.42f, 0.0f};

    @Nullable
    private GeoBone find(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    private void put(String Name, float X, float Y, float Z) {
        GeoBone Found = find(Name);
        if (Found != null) {
            Found.setRotX(X);
            Found.setRotY(Y);
            Found.setRotZ(Z);
        }
    }

    @Override
    public void setCustomAnimations(HostileRaider Owner, long InstanceId, AnimationState<HostileRaider> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        if (Owner.isDeadOrDying()) {
            return;
        }
        float Partial = Animated.getPartialTick();
        if (Owner instanceof ChaosInsurgent Insurgent && Insurgent.onRope()) {
            float Sway = Insurgent.ropeSway(Partial);
            put("right_arm", RopeRight[0] + Sway, RopeRight[1], RopeRight[2]);
            put("left_arm", RopeLeft[0] - Sway, RopeLeft[1], RopeLeft[2]);
            put("right_leg", RopeRightLeg[0], RopeRightLeg[1], RopeRightLeg[2]);
            put("left_leg", RopeLeftLeg[0], RopeLeftLeg[1], RopeLeftLeg[2]);
            put("body", 0.0f, 0.0f, 0.0f);
            return;
        }
        ItemStack Held = Owner.getMainHandItem();
        if (!Held.isEmpty() && !NpcGunner.isGun(Held)) {
            put("right_arm", Cradle[0], Cradle[1], Cradle[2]);
            put("left_arm", CradleLeft[0], CradleLeft[1], CradleLeft[2]);
        }
    }
}
