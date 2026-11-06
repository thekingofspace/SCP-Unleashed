package net.rai.scp_unleashed.scp1730.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730State;
import net.rai.scp_unleashed.scp1730.Scp1730Symbols;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp1730TurretBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final double Range = 16.0;
    public static final double Arc = 70.0;
    public static final int LockTicks = 16;
    public static final int FireEvery = 8;
    public static final float Damage = 2.5f;
    public static final double Spread = 0.05;
    public static final int ThinkEvery = 2;
    public static final float MaxHealth = 45.0f;
    public static final float DroopPitch = -72.0f;
    private static final RawAnimation FireOnce = RawAnimation.begin().thenPlay("fire");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int TargetId = -1;
    private int Lock;
    private int Cooldown;
    private int Alert;
    private int Zone = -2;
    private int Run = -1;
    private float Health = MaxHealth;
    private boolean Wrecked;
    private boolean Live;
    public int WreckAge;
    public float AimYaw;
    public float AimPitch;
    public float AimYawOld;
    public float AimPitchOld;

    public Scp1730TurretBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp1730Registry.TurretEntity.get(), Pos, State);
    }

    public Vec3 muzzle() {
        return Vec3.atCenterOf(worldPosition).add(0.0, -0.2, 0.0);
    }

    public Direction facing() {
        return getBlockState().getValue(Scp1730TurretBlock.Facing);
    }

    public void alert(int Ticks) {
        Alert = Math.max(Alert, Ticks);
    }

    public int targetId() {
        return TargetId;
    }

    public boolean wrecked() {
        return Wrecked;
    }

    public boolean live() {
        return Live;
    }

    public float health() {
        return Health;
    }

    public void damage(ServerLevel World, float Amount, Vec3 Point) {
        if (Wrecked || Amount <= 0.0f) {
            return;
        }
        Health -= Amount;
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Point.x, Point.y, Point.z, 5, 0.06, 0.06, 0.06, 0.1);
        World.playSound(null, worldPosition, Scp1730Registry.TurretHit.get(), SoundSource.BLOCKS, 0.7f, 0.9f + World.random.nextFloat() * 0.2f);
        if (Health <= 0.0f) {
            wreck(World);
            return;
        }
        setChanged();
        World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void wreck(ServerLevel World) {
        if (Wrecked) {
            return;
        }
        Wrecked = true;
        Health = 0.0f;
        TargetId = -1;
        Lock = 0;
        Alert = 0;
        setChanged();
        World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        Vec3 Centre = muzzle();
        World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Centre.x, Centre.y, Centre.z, 24, 0.2, 0.2, 0.2, 0.25);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.x, Centre.y + 0.2, Centre.z, 6, 0.15, 0.1, 0.15, 0.02);
        World.sendParticles(ParticleTypes.LAVA, Centre.x, Centre.y, Centre.z, 2, 0.1, 0.1, 0.1, 0.0);
        World.playSound(null, worldPosition, Scp1730Registry.TurretCrunch.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        World.playSound(null, worldPosition, Scp1730Registry.TurretFizz.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
    }

    public boolean powered() {
        if (level == null) {
            return false;
        }
        if (Scp1730World.inside(level) && level.getServer() != null) {
            Scp1730State State = Scp1730State.get(level.getServer());
            if (Zone == -2 || Run != State.run()) {
                Run = State.run();
                Zone = Scp1730World.zoneOf(Run, worldPosition);
            }
            return State.powered(Zone);
        }
        return level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.above());
    }

    public static double geckoFacing(Direction Side) {
        return 180.0 - Side.toYRot();
    }

    public static double yawTo(Vec3 From, Vec3 To) {
        return Math.toDegrees(Math.atan2(-(To.x - From.x), To.z - From.z));
    }

    public boolean inArc(Vec3 Point) {
        if (Alert > 0) {
            return true;
        }
        double Wanted = yawTo(muzzle(), Point);
        return Math.abs(Mth.wrapDegrees(Wanted - facing().toYRot())) <= Arc;
    }

    @Nullable
    public Player findTarget(ServerLevel World) {
        Vec3 From = muzzle();
        Player Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Player Each : World.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(Range), Each -> Each.isAlive() && !Each.isSpectator() && !Each.isCreative())) {
            Vec3 Chest = Each.position().add(0.0, Each.getBbHeight() * 0.6, 0.0);
            double Distance = From.distanceTo(Chest);
            if (Distance > Range || Distance >= BestDistance || !inArc(Chest)) {
                continue;
            }
            if (Scp1730Symbols.clearSight(World, Each, Chest, From, worldPosition, null)) {
                Best = Each;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp1730TurretBlockEntity Turret) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        if (Turret.Wrecked) {
            if (World.random.nextInt(120) == 0) {
                World.playSound(null, Pos, Scp1730Registry.TurretSpark.get(), SoundSource.BLOCKS, 0.35f, 0.9f + World.random.nextFloat() * 0.3f);
            }
            return;
        }
        if (Turret.Alert > 0) {
            Turret.Alert--;
        }
        if (Turret.Cooldown > 0) {
            Turret.Cooldown--;
        }
        if ((World.getGameTime() + Pos.asLong()) % ThinkEvery != 0) {
            return;
        }
        int Before = Turret.TargetId;
        boolean WasLive = Turret.Live;
        Turret.Live = Turret.powered();
        if (!Turret.Live) {
            Turret.TargetId = -1;
            Turret.Lock = 0;
        } else {
            Entity Current = Turret.TargetId >= 0 ? World.getEntity(Turret.TargetId) : null;
            Player Found = Turret.findTarget(Server);
            if (Found == null) {
                Turret.TargetId = -1;
                Turret.Lock = 0;
            } else {
                if (Current != Found) {
                    Turret.Lock = 0;
                    World.playSound(null, Pos, Scp1730Registry.TurretLock.get(), SoundSource.HOSTILE, 0.7f, 1.2f);
                }
                Turret.TargetId = Found.getId();
                Turret.Lock += ThinkEvery;
                if (Turret.Lock >= LockTicks && Turret.Cooldown <= 0) {
                    Turret.fire(Server, Found);
                }
            }
        }
        if (Before != Turret.TargetId || WasLive != Turret.Live) {
            Turret.setChanged();
            World.sendBlockUpdated(Pos, State, State, 3);
        }
    }

    public void fire(ServerLevel World, Player Target) {
        Cooldown = FireEvery;
        Vec3 From = muzzle();
        Vec3 Chest = Target.position().add(0.0, Target.getBbHeight() * 0.6, 0.0);
        Vec3 Way = Chest.subtract(From).normalize().add((World.random.nextDouble() - 0.5) * Spread, (World.random.nextDouble() - 0.5) * Spread,
                (World.random.nextDouble() - 0.5) * Spread);
        Vec3 End = From.add(Way.normalize().scale(From.distanceTo(Chest)));
        World.playSound(null, worldPosition, Scp1730Registry.TurretFire.get(), SoundSource.HOSTILE, 1.0f, 0.95f + World.random.nextFloat() * 0.1f);
        triggerAnim("action", "fire");
        double Length = From.distanceTo(End);
        for (double Step = 0.5; Step < Length; Step += 0.75) {
            Vec3 At = From.add(Way.normalize().scale(Step));
            World.sendParticles(ParticleTypes.CRIT, At.x, At.y, At.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        World.sendParticles(ParticleTypes.SMALL_FLAME, From.x, From.y, From.z, 2, 0.05, 0.05, 0.05, 0.01);
        if (Target.getBoundingBox().inflate(0.15).clip(From, From.add(Way.normalize().scale(Length + 1.0))).isPresent()) {
            Target.hurt(new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp1730Registry.TurretDamage)), Damage);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp1730TurretBlockEntity Turret) {
        Turret.AimYawOld = Turret.AimYaw;
        Turret.AimPitchOld = Turret.AimPitch;
        if (Turret.Wrecked) {
            Turret.WreckAge++;
            Turret.AimPitch = Mth.lerp(0.12f, Turret.AimPitch, DroopPitch);
            Vec3 Centre = Turret.muzzle();
            RandomSource Random = World.random;
            if (Turret.WreckAge < 100 ? Random.nextInt(3) == 0 : Random.nextInt(40) == 0) {
                World.addParticle(ParticleTypes.SMOKE, Centre.x + (Random.nextDouble() - 0.5) * 0.3, Centre.y + 0.1, Centre.z + (Random.nextDouble() - 0.5) * 0.3, 0.0, 0.03, 0.0);
            }
            if (Turret.WreckAge < 40 ? Random.nextInt(2) == 0 : Random.nextInt(55) == 0) {
                for (int Index = 0; Index < 1 + Random.nextInt(3); Index++) {
                    World.addParticle(ParticleTypes.ELECTRIC_SPARK, Centre.x + (Random.nextDouble() - 0.5) * 0.25, Centre.y, Centre.z + (Random.nextDouble() - 0.5) * 0.25,
                            (Random.nextDouble() - 0.5) * 0.15, -0.05 - Random.nextDouble() * 0.1, (Random.nextDouble() - 0.5) * 0.15);
                }
            }
            return;
        }
        Entity Target = Turret.TargetId >= 0 ? World.getEntity(Turret.TargetId) : null;
        float WantYaw;
        float WantPitch;
        if (Target != null) {
            Vec3 From = Turret.muzzle();
            Vec3 Chest = Target.position().add(0.0, Target.getBbHeight() * 0.6, 0.0);
            Vec3 Way = Chest.subtract(From);
            double Flat = Math.sqrt(Way.x * Way.x + Way.z * Way.z);
            WantYaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(-Way.x, -Way.z)) - geckoFacing(Turret.facing()));
            WantPitch = (float) Math.toDegrees(Math.atan2(Way.y, Math.max(0.01, Flat)));
        } else {
            WantYaw = (float) (Math.sin(World.getGameTime() * 0.03 + Pos.asLong()) * 35.0);
            WantPitch = -18.0f;
        }
        Turret.AimYaw = Turret.AimYaw + Mth.wrapDegrees(WantYaw - Turret.AimYaw) * 0.35f;
        Turret.AimPitch = Mth.lerp(0.35f, Turret.AimPitch, Mth.clamp(WantPitch, -80.0f, 20.0f));
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.saveAdditional(Tag, Lookup);
        Tag.putFloat("Health", Health);
        Tag.putBoolean("Wrecked", Wrecked);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.loadAdditional(Tag, Lookup);
        Health = Tag.contains("Health") ? Tag.getFloat("Health") : MaxHealth;
        Wrecked = Tag.getBoolean("Wrecked");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Lookup) {
        CompoundTag Tag = saveWithoutMetadata(Lookup);
        Tag.putInt("Target", TargetId);
        Tag.putBoolean("Live", Live);
        return Tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.handleUpdateTag(Tag, Lookup);
        read(Tag);
    }

    private void read(CompoundTag Tag) {
        if (Tag.contains("Target")) {
            TargetId = Tag.getInt("Target");
        }
        if (Tag.contains("Health")) {
            Health = Tag.getFloat("Health");
        }
        if (Tag.contains("Wrecked")) {
            Wrecked = Tag.getBoolean("Wrecked");
        }
        if (Tag.contains("Live")) {
            Live = Tag.getBoolean("Live");
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection Link, ClientboundBlockEntityDataPacket Packet0, HolderLookup.Provider Lookup) {
        read(Packet0.getTag());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "action", 0, Animated -> Animated.getController().isPlayingTriggeredAnimation() ? PlayState.CONTINUE : PlayState.STOP)
                .receiveTriggeredAnimations().triggerableAnim("fire", FireOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
