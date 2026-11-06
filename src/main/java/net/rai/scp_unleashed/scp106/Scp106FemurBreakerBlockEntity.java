package net.rai.scp_unleashed.scp106;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlManager;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp106FemurBreakerBlockEntity extends BlockEntity {
    public static final int LeverTicks = 4;
    public static final int ImpactTick = 8;
    public static final int SummonTick = 50;
    public static final int RaiseStart = 40;
    public static final int RaiseEnd = 120;
    public static final int CooldownTicks = 140;
    public static final float FemurDamage = 14.0f;
    public static final int SlowTicks = 600;
    public static final double LeashReach = 10.0;
    public static final double ChairReach = 0.75;
    public static final long ExemptTicks = 600L;
    public static final double VictimReach = 4.0;
    public static final float RaisedAngle = 32.0f;
    public static final float StruckAngle = -40.0f;
    public static final float LeverPulled = 80.0f;
    public static final double ScreamRange = 64.0;

    private long TriggerTime = -1L;
    private final Map<UUID, Long> Exempt = new HashMap<>();
    @Nullable
    private UUID Victim;

    public Scp106FemurBreakerBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp106Registry.FemurBreakerEntity.get(), Pos, State);
    }

    public Direction getFront() {
        BlockState State = getBlockState();
        return State.getBlock() instanceof Scp106FemurBreakerBlock ? State.getValue(Scp106FemurBreakerBlock.Facing) : Direction.NORTH;
    }

    public Vec3 seatSpot() {
        return Scp106FemurLayout.seatSpot(worldPosition, getFront());
    }

    @Nullable
    public Scp106Restraint seat() {
        if (level == null) {
            return null;
        }
        List<Scp106Restraint> Found = level.getEntitiesOfClass(Scp106Restraint.class, new AABB(worldPosition).inflate(0.25), Entity::isAlive);
        return Found.isEmpty() ? null : Found.getFirst();
    }

    @Nullable
    public LivingEntity occupant() {
        Scp106Restraint Seat = seat();
        if (Seat == null) {
            return null;
        }
        for (Entity Passenger : Seat.getPassengers()) {
            if (Passenger instanceof LivingEntity Living) {
                return Living;
            }
        }
        return null;
    }

    public boolean isCycling() {
        return TriggerTime >= 0L;
    }

    public long triggerTime() {
        return TriggerTime;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp106FemurBreakerBlockEntity Machine) {
        if (World instanceof ServerLevel ServerWorld) {
            Machine.tickServer(ServerWorld, State);
        }
    }

    private void tickServer(ServerLevel World, BlockState State) {
        if (World.getGameTime() % 10 == 0) {
            seatPushed(World);
            boolean Taken = occupant() != null;
            if (State.getValue(Scp106FemurBreakerBlock.Occupied) != Taken) {
                Scp106FemurBreakerBlock.setOccupied(World, worldPosition, State, Taken);
            }
        }
        if (TriggerTime < 0L) {
            return;
        }
        long Elapsed = World.getGameTime() - TriggerTime;
        if (Elapsed == ImpactTick) {
            impact(World);
        } else if (Elapsed == SummonTick) {
            summon(World);
        } else if (Elapsed >= CooldownTicks || Elapsed < 0L) {
            TriggerTime = -1L;
            Victim = null;
            sync(World);
        }
    }

    public void useChair(Player User) {
        if (!(level instanceof ServerLevel World)) {
            return;
        }
        LivingEntity Occupant = occupant();
        if (Occupant == null) {
            if (User.isPassenger() || User.isSecondaryUseActive()) {
                return;
            }
            seatEntity(World, User);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.restrained").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (Occupant == User) {
            return;
        }
        release(User);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.unbuckled").withStyle(ChatFormatting.GRAY), true);
    }

    public void seatLeashed(Player User) {
        if (!(level instanceof ServerLevel World)) {
            return;
        }
        if (occupant() != null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.chair_taken").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        for (Mob Candidate : World.getEntitiesOfClass(Mob.class, User.getBoundingBox().inflate(LeashReach), Found -> Found.getLeashHolder() == User)) {
            if (!ControlBodies.isPersonLike(Candidate) && !(Candidate instanceof Villager)) {
                continue;
            }
            Candidate.dropLeash(true, !User.getAbilities().instabuild);
            seatEntity(World, Candidate);
            return;
        }
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.no_leashed").withStyle(ChatFormatting.GRAY), true);
    }

    private AABB chairZone() {
        Vec3 Spot = seatSpot();
        return new AABB(Spot.x - ChairReach, Spot.y - 0.25, Spot.z - ChairReach, Spot.x + ChairReach, Spot.y + 1.5, Spot.z + ChairReach);
    }

    private void seatPushed(ServerLevel World) {
        AABB Zone = chairZone();
        long Now = World.getGameTime();
        Exempt.entrySet().removeIf(Entry -> {
            Entity Known = World.getEntity(Entry.getKey());
            return Known == null || Now - Entry.getValue() > ExemptTicks || !Known.getBoundingBox().intersects(Zone);
        });
        if (TriggerTime >= 0L || occupant() != null) {
            return;
        }
        for (Mob Candidate : World.getEntitiesOfClass(Mob.class, Zone, Found -> Found.isAlive() && !Found.isPassenger() && !Found.isVehicle() && !Found.isLeashed()
                && ControlBodies.isPersonLike(Found) && !Exempt.containsKey(Found.getUUID()))) {
            seatEntity(World, Candidate);
            return;
        }
    }

    private void seatEntity(ServerLevel World, LivingEntity Sitter) {
        Scp106Restraint Old = seat();
        if (Old != null) {
            Old.discard();
        }
        Direction Front = getFront();
        Scp106Restraint Seat = new Scp106Restraint(World, seatSpot(), Front.toYRot());
        World.addFreshEntity(Seat);
        if (!Sitter.startRiding(Seat, true)) {
            Seat.discard();
            return;
        }
        Scp106FemurBreakerBlock.setOccupied(World, worldPosition, getBlockState(), true);
        World.playSound(null, worldPosition, Scp106Registry.Buckle.get(), SoundSource.BLOCKS, 1.0f, 0.9f + World.getRandom().nextFloat() * 0.15f);
    }

    public boolean release(@Nullable Player By) {
        Scp106Restraint Seat = seat();
        if (Seat == null) {
            return false;
        }
        if (level != null) {
            long Now = level.getGameTime();
            for (Entity Passenger : Seat.getPassengers()) {
                Exempt.put(Passenger.getUUID(), Now);
            }
        }
        Seat.ejectPassengers();
        Seat.discard();
        if (level instanceof ServerLevel World) {
            Scp106FemurBreakerBlock.setOccupied(World, worldPosition, getBlockState(), false);
            World.playSound(null, worldPosition, Scp106Registry.Buckle.get(), SoundSource.BLOCKS, 0.8f, 1.2f);
        }
        return true;
    }

    public void pullLever(Player User) {
        if (!(level instanceof ServerLevel World)) {
            return;
        }
        if (TriggerTime >= 0L) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.lever_resetting").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        LivingEntity Occupant = occupant();
        if (Occupant == null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.lever_empty").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (Occupant == User || User.getVehicle() instanceof Scp106Restraint) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.lever_reach").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        TriggerTime = World.getGameTime();
        Victim = Occupant.getUUID();
        Vec3 Lever = leverPoint();
        World.playSound(null, Lever.x, Lever.y, Lever.z, Scp106Registry.Lever.get(), SoundSource.BLOCKS, 1.2f, 0.9f);
        setChanged();
        sync(World);
    }

    public Vec3 leverPoint() {
        return pointOf(Scp106FemurLayout.LeverX, Scp106FemurLayout.LeverY + 8.0f, Scp106FemurLayout.LeverZ + 5.0f);
    }

    public Vec3 strikePoint() {
        return pointOf(8.0f, 7.0f, 3.5f);
    }

    private Vec3 pointOf(float X, float Y, float Z) {
        Direction Front = getFront();
        double Ox = X / 16.0 - 0.5;
        double Oz = Z / 16.0 - 0.5;
        double[] Turned = switch (Front) {
            case EAST -> new double[]{-Oz, Ox};
            case SOUTH -> new double[]{-Ox, -Oz};
            case WEST -> new double[]{Oz, -Ox};
            default -> new double[]{Ox, Oz};
        };
        return new Vec3(worldPosition.getX() + 0.5 + Turned[0], worldPosition.getY() + Y / 16.0, worldPosition.getZ() + 0.5 + Turned[1]);
    }

    private void impact(ServerLevel World) {
        Vec3 Strike = strikePoint();
        World.playSound(null, Strike.x, Strike.y, Strike.z, Scp106Registry.Slam.get(), SoundSource.BLOCKS, 1.8f, 0.9f + World.getRandom().nextFloat() * 0.1f);
        World.sendParticles(ParticleTypes.CRIT, Strike.x, Strike.y, Strike.z, 10, 0.25, 0.05, 0.2, 0.15);
        LivingEntity Occupant = occupant();
        if (Occupant == null || Victim == null || !Victim.equals(Occupant.getUUID())) {
            return;
        }
        RandomSource Random = World.getRandom();
        World.playSound(null, Strike.x, Strike.y, Strike.z, Scp106Registry.Crunch.get(), SoundSource.PLAYERS, 1.6f, 0.9f + Random.nextFloat() * 0.1f);
        World.playSound(null, Occupant.getX(), Occupant.getEyeY(), Occupant.getZ(), Scp106Registry.Scream.get(), SoundSource.PLAYERS, 3.0f, 0.95f + Random.nextFloat() * 0.1f);
        World.playSound(null, Occupant.getX(), Occupant.getEyeY(), Occupant.getZ(), Scp106Registry.ScreamLayer.get(), SoundSource.PLAYERS, 2.4f, 0.97f + Random.nextFloat() * 0.06f);
        Occupant.hurt(World.damageSources().source(Scp106Registry.FemurBreak), FemurDamage);
        if (Occupant.isAlive()) {
            Occupant.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SlowTicks, 2, false, true));
        }
    }

    private void summon(ServerLevel World) {
        if (Victim == null) {
            return;
        }
        Entity Found = World.getEntity(Victim);
        if (!(Found instanceof LivingEntity Bait) || !Bait.isAlive() || !Targeting.isTargetable(Bait) || Bait instanceof ServerPlayer User && ControlManager.hasPendingDeath(User)) {
            return;
        }
        if (Bait.position().distanceTo(seatSpot()) > VictimReach) {
            return;
        }
        Scp106OldMan Hunter = nearestOldMan(World, seatSpot(), true);
        if (Hunter == null) {
            distantLaugh(World);
            return;
        }
        Vec3 Spot = arrivalSpot(World);
        Hunter.lure(World, Spot, Bait);
    }

    public Vec3 arrivalSpot(ServerLevel World) {
        Direction Front = getFront();
        BlockPos Ahead = worldPosition.relative(Front);
        if (Scp106OldMan.openFor(World, Ahead)) {
            return Vec3.atBottomCenterOf(Ahead);
        }
        BlockPos Side = worldPosition.relative(Front.getClockWise());
        if (Scp106OldMan.openFor(World, Side)) {
            return Vec3.atBottomCenterOf(Side);
        }
        Vec3 Seat = seatSpot();
        return new Vec3(Seat.x + Front.getStepX() * 0.9, worldPosition.getY(), Seat.z + Front.getStepZ() * 0.9);
    }

    @Nullable
    public static Scp106OldMan nearestOldMan(ServerLevel World, Vec3 Near) {
        return nearestOldMan(World, Near, false);
    }

    @Nullable
    public static Scp106OldMan nearestOldMan(ServerLevel World, Vec3 Near, boolean FreeOnly) {
        Scp106OldMan Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Scp106OldMan Candidate : World.getEntities(Scp106Registry.OldMan.get(), Found -> Found.isAlive() && !(FreeOnly && Found.anchored()))) {
            double Distance = Candidate.distanceToSqr(Near);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Candidate;
            }
        }
        return Best;
    }

    private void distantLaugh(ServerLevel World) {
        RandomSource Random = World.getRandom();
        double Angle = Random.nextDouble() * Mth.TWO_PI;
        Vec3 Seat = seatSpot();
        Vec3 Far = Seat.add(Math.cos(Angle) * 14.0, 1.0, Math.sin(Angle) * 14.0);
        World.playSound(null, Far.x, Far.y, Far.z, Scp106Registry.LaughLow.get(), SoundSource.HOSTILE, 0.9f, 1.0f);
    }

    public float armAngle(float PartialTick) {
        if (level == null || TriggerTime < 0L) {
            return RaisedAngle;
        }
        float Elapsed = (float) (level.getGameTime() - TriggerTime) + PartialTick;
        if (Elapsed < LeverTicks) {
            return RaisedAngle;
        }
        if (Elapsed < ImpactTick) {
            float Share = (Elapsed - LeverTicks) / (ImpactTick - LeverTicks);
            return Mth.lerp(Share * Share, RaisedAngle, StruckAngle);
        }
        if (Elapsed < ImpactTick + 3) {
            float Share = (Elapsed - ImpactTick) / 3.0f;
            return StruckAngle + 4.0f * Mth.sin(Share * Mth.PI);
        }
        if (Elapsed < RaiseStart) {
            return StruckAngle;
        }
        if (Elapsed < RaiseEnd) {
            float Share = (Elapsed - RaiseStart) / (RaiseEnd - RaiseStart);
            float Eased = Share * Share * (3.0f - 2.0f * Share);
            return Mth.lerp(Eased, StruckAngle, RaisedAngle);
        }
        return RaisedAngle;
    }

    public float leverAngle(float PartialTick) {
        if (level == null || TriggerTime < 0L) {
            return 0.0f;
        }
        float Elapsed = (float) (level.getGameTime() - TriggerTime) + PartialTick;
        if (Elapsed < LeverTicks) {
            return LeverPulled * Elapsed / LeverTicks;
        }
        if (Elapsed < RaiseEnd - 20) {
            return LeverPulled;
        }
        if (Elapsed < RaiseEnd) {
            return LeverPulled * (1.0f - (Elapsed - (RaiseEnd - 20)) / 20.0f);
        }
        return 0.0f;
    }

    private void sync(ServerLevel World) {
        World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public AABB getRenderBounds() {
        return new AABB(worldPosition).inflate(2.0, 0.0, 2.0).expandTowards(0.0, 2.5, 0.0);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putLong("Trigger", TriggerTime);
        if (Victim != null) {
            Tag.putUUID("Victim", Victim);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        TriggerTime = Tag.contains("Trigger") ? Tag.getLong("Trigger") : -1L;
        Victim = Tag.hasUUID("Victim") ? Tag.getUUID("Victim") : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putLong("Trigger", TriggerTime);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
