package net.rai.scp_unleashed.disperser;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.grenade.GrenadeEntity;
import net.rai.scp_unleashed.grenade.GrenadeKind;
import net.rai.scp_unleashed.grenade.GrenadeSounds;
import net.rai.scp_unleashed.grenade.GrenadeTuning;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabOrganisms;
import org.jetbrains.annotations.Nullable;

public class OrganicGrenadeEntity extends GrenadeEntity {
    private static final EntityDataAccessor<String> DataCulture = SynchedEntityData.defineId(OrganicGrenadeEntity.class, EntityDataSerializers.STRING);

    public OrganicGrenadeEntity(EntityType<? extends OrganicGrenadeEntity> Type, Level World) {
        super(Type, World);
        setKind(GrenadeKind.Gas);
    }

    public static OrganicGrenadeEntity thrown(ServerLevel World, LivingEntity Thrower, @Nullable ResourceLocation Culture, float Charge) {
        OrganicGrenadeEntity Grenade = new OrganicGrenadeEntity(DisperserRegistry.OrganicGrenadeType.get(), World);
        Grenade.setCulture(Culture);
        Grenade.setOwner(Thrower);
        Vec3 Eye = Thrower.getEyePosition();
        Vec3 Start = Eye.add(Thrower.getLookAngle().scale(HandReach)).subtract(0.0, HandDrop + CentreLift, 0.0);
        Grenade.setPos(Start.x, Start.y, Start.z);
        if (!World.noCollision(Grenade, Grenade.getBoundingBox())) {
            Grenade.setPos(Eye.x, Eye.y - CentreLift, Eye.z);
        }
        float Speed = Mth.lerp(Charge, GrenadeTuning.MinSpeed, GrenadeTuning.MaxSpeed);
        Grenade.shootFromRotation(Thrower, Thrower.getXRot(), Thrower.getYRot(), LobPitch, Speed, GrenadeTuning.ThrowSpread);
        World.addFreshEntity(Grenade);
        return Grenade;
    }

    public static OrganicGrenadeEntity dispensed(Level World, double X, double Y, double Z, @Nullable ResourceLocation Culture) {
        OrganicGrenadeEntity Grenade = new OrganicGrenadeEntity(DisperserRegistry.OrganicGrenadeType.get(), World);
        Grenade.setCulture(Culture);
        Grenade.setPos(X, Y - CentreLift, Z);
        return Grenade;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(DataCulture, "");
    }

    @Nullable
    public ResourceLocation getCulture() {
        String Raw = entityData.get(DataCulture);
        return Raw.isEmpty() ? null : ResourceLocation.tryParse(Raw);
    }

    public void setCulture(@Nullable ResourceLocation Culture) {
        entityData.set(DataCulture, Culture == null ? "" : Culture.toString());
    }

    @Override
    public void detonate(ServerLevel World, boolean Airless) {
        Vec3 Centre = position().add(0.0, DisperserTuning.BurstLift, 0.0);
        LabOrganism Organism = LabOrganisms.find(getCulture()).orElse(null);
        if (Organism == null) {
            GrenadeSounds.fizzle(World, Centre);
            World.sendParticles(ParticleTypes.SMOKE, Centre.x, Centre.y, Centre.z, 8, 0.08, 0.06, 0.08, 0.01);
            discard();
            return;
        }
        DisperserSpread.play(World, Centre, DisperserRegistry.Burst, 1.0F);
        DisperserSpread.spray(World, Centre, Organism.getMistColour(), DisperserTuning.BurstParticles, DisperserTuning.BurstSpeed, DisperserTuning.BurstRise);
        if (Airless) {
            DisperserSpread.release(World, Centre, Pos -> false, Organism, DisperserTuning.CloudRadius);
        } else {
            OrganismCloud.spawn(World, position(), Organism);
        }
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        ResourceLocation Culture = getCulture();
        if (Culture != null) {
            Tag.putString("Culture", Culture.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        setCulture(Tag.contains("Culture") ? ResourceLocation.tryParse(Tag.getString("Culture")) : null);
    }
}
