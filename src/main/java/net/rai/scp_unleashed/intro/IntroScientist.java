package net.rai.scp_unleashed.intro;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.gear.GearRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class IntroScientist extends DClassLike {
    public static final float HeadTurn = 7.0f;
    public static final float PitchTurn = 5.0f;
    public static final float PitchLimit = 40.0f;
    @Nullable
    private UUID Owner;
    private boolean Standing;

    public IntroScientist(EntityType<? extends IntroScientist> Type, Level World) {
        super(Type, World);
        setNoAi(true);
        setInvulnerable(true);
        setSilent(true);
        setPersistenceRequired();
        dress();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0);
    }

    private void dress() {
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(GearRegistry.ScientistLabCoat.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(GearRegistry.ScientistSlacks.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(GearRegistry.ScientistShoes.get()));
        for (EquipmentSlot Slot : EquipmentSlot.values()) {
            setDropChance(Slot, 0.0f);
        }
    }

    public void setOwner(@Nullable UUID Player) {
        Owner = Player;
    }

    @Nullable
    public UUID owner() {
        return Owner;
    }

    public void setStanding(boolean Up) {
        Standing = Up;
    }

    public boolean standing() {
        return Standing;
    }

    @Override
    public float glanceRate() {
        return 0.6f;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && level() instanceof ServerLevel World) {
            Player Target = Owner == null ? null : World.getPlayerByUUID(Owner);
            if (Target == null) {
                if (tickCount > 40) {
                    discard();
                }
                return;
            }
            face(Target.getEyePosition());
        }
    }

    private void face(Vec3 Eye) {
        Vec3 Mine = getEyePosition();
        double Dx = Eye.x - Mine.x;
        double Dy = Eye.y - Mine.y;
        double Dz = Eye.z - Mine.z;
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        float WantYaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        float WantPitch = Mth.clamp((float) (-(Mth.atan2(Dy, Flat) * Mth.RAD_TO_DEG)), -PitchLimit, PitchLimit);
        float Yaw = Mth.approachDegrees(getYHeadRot(), WantYaw, HeadTurn);
        setYHeadRot(Yaw);
        setXRot(Mth.approach(getXRot(), WantPitch, PitchTurn));
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity Other) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        return InteractionResult.FAIL;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (Owner != null) {
            Saved.putUUID("Owner", Owner);
        }
        Saved.putBoolean("Standing", Standing);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Owner = Saved.hasUUID("Owner") ? Saved.getUUID("Owner") : null;
        Standing = Saved.getBoolean("Standing");
    }
}
