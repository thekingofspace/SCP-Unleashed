package net.rai.scp_unleashed.disperser;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabOrganisms;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OrganismCloud extends Entity {
    private static final EntityDataAccessor<String> DataCulture = SynchedEntityData.defineId(OrganismCloud.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DataRadius = SynchedEntityData.defineId(OrganismCloud.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DataDuration = SynchedEntityData.defineId(OrganismCloud.class, EntityDataSerializers.INT);

    private int Age;
    private int Pulses;

    public OrganismCloud(EntityType<? extends OrganismCloud> Type, Level World) {
        super(Type, World);
        noPhysics = true;
    }

    public static OrganismCloud spawn(ServerLevel World, Vec3 At, LabOrganism Organism) {
        OrganismCloud Cloud = new OrganismCloud(DisperserRegistry.Cloud.get(), World);
        Cloud.setPos(At.x, At.y, At.z);
        Cloud.setCulture(Organism.getId());
        Cloud.setRadius((float) DisperserTuning.CloudRadius);
        Cloud.setDuration(DisperserTuning.CloudTicks);
        World.addFreshEntity(Cloud);
        Cloud.pulse(World);
        return Cloud;
    }

    public static List<OrganismCloud> near(ServerLevel World, Vec3 At, double Reach) {
        return World.getEntitiesOfClass(OrganismCloud.class, new AABB(At, At).inflate(Reach));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataCulture, "");
        Builder.define(DataRadius, (float) DisperserTuning.CloudRadius);
        Builder.define(DataDuration, DisperserTuning.CloudTicks);
    }

    @Nullable
    public ResourceLocation getCulture() {
        String Raw = entityData.get(DataCulture);
        return Raw.isEmpty() ? null : ResourceLocation.tryParse(Raw);
    }

    public void setCulture(@Nullable ResourceLocation Culture) {
        entityData.set(DataCulture, Culture == null ? "" : Culture.toString());
    }

    public float getRadius() {
        return entityData.get(DataRadius);
    }

    public void setRadius(float Radius) {
        entityData.set(DataRadius, Math.max(0.5F, Radius));
    }

    public int getDuration() {
        return entityData.get(DataDuration);
    }

    public void setDuration(int Duration) {
        entityData.set(DataDuration, Math.max(1, Duration));
    }

    public int getAge() {
        return Age;
    }

    public int getPulses() {
        return Pulses;
    }

    public Vec3 source() {
        return position().add(0.0, DisperserTuning.CloudLift, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel World) {
            Age++;
            if (Age >= getDuration()) {
                discard();
                return;
            }
            if (Age % DisperserTuning.CloudPulseTicks == 0) {
                pulse(World);
            }
            if (Age % DisperserTuning.CloudHissTicks == 0) {
                float Left = 1.0F - Age / (float) getDuration();
                World.playSound(null, getX(), getY() + 0.5, getZ(), DisperserRegistry.Spit.get(), getSoundSource(), 0.25F + 0.35F * Left, 0.8F + World.getRandom().nextFloat() * 0.1F);
            }
            return;
        }
        drift();
    }

    public void pulse(ServerLevel World) {
        LabOrganism Organism = LabOrganisms.find(getCulture()).orElse(null);
        if (Organism == null) {
            return;
        }
        Vec3 Source = source();
        BlockPos Inside = BlockPos.containing(Source);
        BlockPos Base = blockPosition();
        DisperserSpread.release(World, Source, Pos -> Pos.equals(Inside) || Pos.equals(Base), Organism, getRadius() + DisperserTuning.CloudReachMargin);
        Pulses++;
    }

    private void drift() {
        int Duration = getDuration();
        int Left = Duration - tickCount;
        if (Left <= 0) {
            return;
        }
        float Fade = Mth.clamp(Left / (float) DisperserTuning.CloudFadeTicks, 0.0F, 1.0F);
        int Count = Math.max(1, Math.round(DisperserTuning.CloudParticles * Fade));
        int Colour = LabOrganisms.mistColourOf(getCulture());
        float Radius = getRadius();
        Level World = level();
        for (int Index = 0; Index < Count; Index++) {
            double Angle = random.nextDouble() * Mth.TWO_PI;
            double Reach = Math.sqrt(random.nextDouble()) * Radius;
            double X = getX() + Math.cos(Angle) * Reach;
            double Z = getZ() + Math.sin(Angle) * Reach;
            double Y = getY() + 0.05 + random.nextDouble() * DisperserTuning.CloudCeiling;
            BlockPos Spot = BlockPos.containing(X, Y, Z);
            if (!World.getBlockState(Spot).getCollisionShape(World, Spot).isEmpty()) {
                continue;
            }
            World.addParticle(DisperserSpread.haze(Colour), X, Y, Z, (random.nextDouble() - 0.5) * 0.03, 0.002 + random.nextDouble() * 0.006, (random.nextDouble() - 0.5) * 0.03);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        setCulture(Tag.contains("Culture") ? ResourceLocation.tryParse(Tag.getString("Culture")) : null);
        if (Tag.contains("Radius")) {
            setRadius(Tag.getFloat("Radius"));
        }
        if (Tag.contains("Duration")) {
            setDuration(Tag.getInt("Duration"));
        }
        Age = Tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        ResourceLocation Culture = getCulture();
        if (Culture != null) {
            Tag.putString("Culture", Culture.toString());
        }
        Tag.putFloat("Radius", getRadius());
        Tag.putInt("Duration", getDuration());
        Tag.putInt("Age", Age);
    }
}
