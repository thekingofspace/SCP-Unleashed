package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class Scp610Harvester extends Scp610Utility {
    public static final double Reach = 24.0;
    public static final double Close = 1.6;
    public static final double Deliver = 3.0;
    public static final double Steer = 4.0;
    public static final int ScanTicks = 20;
    private int Carry;
    private int Kills;
    @Nullable
    private ItemEntity Meal;

    public Scp610Harvester(EntityType<? extends Scp610Harvester> Type, Level World) {
        super(Type, World, Scp610Form.Harvester);
    }

    @Override
    protected void registerWork() {
        goalSelector.addGoal(3, new Haul(this));
        goalSelector.addGoal(4, new Scp610Goals.Chase(this, 1.15));
    }

    public int carried() {
        return Carry;
    }

    public void load(int Amount) {
        Carry = Math.min(Scp610Biomass.CarryCap, Carry + Math.max(0, Amount));
        Kills++;
    }

    public int kills() {
        return Kills;
    }

    public boolean full() {
        return Carry >= Scp610Biomass.CarryCap;
    }

    public static boolean livestock(LivingEntity Found) {
        return Found instanceof Animal Beast && Beast.isAlive() && !(Beast instanceof TamableAnimal Pet && Pet.isTame()) && !Scp610Infection.isInfected(Beast)
                && !Scp610Infection.bloodless(Beast) && !InfectionImmunity.isImmune(Beast, Scp610Registry.Immune);
    }

    @Nullable
    public LivingEntity findPrey() {
        AABB Area = getBoundingBox().inflate(Reach, 6.0, Reach);
        List<LivingEntity> Found = level().getEntitiesOfClass(LivingEntity.class, Area, Scp610Harvester::livestock);
        LivingEntity Best = null;
        double BestGap = Reach * Reach;
        for (LivingEntity Each : Found) {
            double Gap = Each.distanceToSqr(this);
            if (Gap < BestGap) {
                BestGap = Gap;
                Best = Each;
            }
        }
        return Best;
    }

    @Nullable
    public ItemEntity findMeal() {
        AABB Area = getBoundingBox().inflate(Reach, 6.0, Reach);
        List<ItemEntity> Found = level().getEntitiesOfClass(ItemEntity.class, Area, Each -> Each.isAlive() && Scp610Biomass.meatValue(Each.getItem()) > 0);
        ItemEntity Best = null;
        double BestGap = Reach * Reach;
        for (ItemEntity Each : Found) {
            double Gap = Each.distanceToSqr(this);
            if (Gap < BestGap) {
                BestGap = Gap;
                Best = Each;
            }
        }
        return Best;
    }

    public int eat(ItemEntity Item) {
        int Value = Scp610Biomass.meatValue(Item.getItem());
        if (Value <= 0 || !Item.isAlive()) {
            return 0;
        }
        int Gained = 0;
        while (!Item.getItem().isEmpty() && Carry + Value <= Scp610Biomass.CarryCap) {
            Item.getItem().shrink(1);
            Carry += Value;
            Gained += Value;
        }
        if (Item.getItem().isEmpty()) {
            Item.discard();
        }
        if (Gained > 0) {
            playLayers(Scp610Sounds.WetLayers, 0.7f, 1.1f);
        }
        return Gained;
    }

    public boolean deliver(ServerLevel World) {
        Scp610Hive Hive = hive();
        if (Hive == null || Carry <= 0) {
            return false;
        }
        Scp610Biomass.add(Hive, Carry);
        Carry = 0;
        World.playSound(null, getX(), getY(), getZ(), Scp610Sounds.Merge.get(), SoundSource.HOSTILE, 0.8f, 1.2f);
        World.sendParticles(Scp610Infection.Speck, getX(), getY() + 0.6, getZ(), 12, 0.4, 0.3, 0.4, 0.0);
        return true;
    }

    @Nullable
    public Vec3 dropOff() {
        Scp610Hive Hive = hive();
        return Hive == null ? null : Vec3.atBottomCenterOf(Hive.anchor());
    }

    @Nullable
    public ItemEntity meal() {
        return Meal != null && Meal.isAlive() ? Meal : null;
    }

    @Override
    protected void threatened(LivingEntity Attacker) {
        if (!livestock(Attacker)) {
            super.threatened(Attacker);
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity Target) {
        if (Target != null && !livestock(Target)) {
            return;
        }
        super.setTarget(Target);
    }

    @Override
    protected void work() {
        LivingEntity Prey = getTarget();
        if (Prey != null && !Prey.isAlive()) {
            setTarget(null);
        }
        if (fleeing() || tickCount % ScanTicks != 0) {
            return;
        }
        if (full()) {
            Meal = null;
            setTarget(null);
            return;
        }
        if (meal() == null) {
            Meal = findMeal();
        }
        if (Meal == null && getTarget() == null) {
            setTarget(findPrey());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Carry", Carry);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Carry = Tag.getInt("Carry");
    }

    public static final class Haul extends Goal {
        private final Scp610Harvester Hauler;

        public Haul(Scp610Harvester Hauler) {
            this.Hauler = Hauler;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        private boolean homeward() {
            LivingEntity Prey = Hauler.getTarget();
            return Hauler.full() || Hauler.carried() > 0 && Hauler.meal() == null && (Prey == null || !Prey.isAlive());
        }

        @Override
        public boolean canUse() {
            return !Hauler.fleeing() && !Hauler.locksMovement() && (homeward() || Hauler.meal() != null);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!(Hauler.level() instanceof ServerLevel World)) {
                return;
            }
            if (homeward()) {
                Vec3 Drop = Hauler.dropOff();
                if (Drop == null) {
                    return;
                }
                BlockPos Here = Hauler.blockPosition();
                double Dx = Drop.x - Here.getX() - 0.5;
                double Dz = Drop.z - Here.getZ() - 0.5;
                if (Dx * Dx + Dz * Dz <= (Deliver + 1.5) * (Deliver + 1.5) && Math.abs(Drop.y - Hauler.getY()) < 4.0) {
                    Hauler.getNavigation().stop();
                    Hauler.deliver(World);
                } else {
                    Hauler.walkTo(Drop, 1.0);
                }
                return;
            }
            ItemEntity Food = Hauler.meal();
            if (Food == null) {
                return;
            }
            if (Hauler.near(Food.position(), Close + Hauler.getBbWidth() * 0.5)) {
                Hauler.getNavigation().stop();
                Hauler.eat(Food);
            } else if (Hauler.near(Food.position(), Steer)) {
                Hauler.getNavigation().stop();
                Hauler.getMoveControl().setWantedPosition(Food.getX(), Food.getY(), Food.getZ(), 1.05);
            } else {
                Hauler.walkTo(Food.position(), 1.05);
            }
        }
    }
}
