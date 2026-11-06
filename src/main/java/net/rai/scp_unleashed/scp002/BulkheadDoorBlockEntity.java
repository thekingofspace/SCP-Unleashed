package net.rai.scp_unleashed.scp002;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.scp002.network.ConsumePayload;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BulkheadDoorBlockEntity extends BlockEntity {
    public static final int AutoCloseTicks = 100;
    public static final int MinDelay = 400;
    public static final int MaxDelay = 900;
    public static final int CalmDelay = 100;
    public static final int BiteInterval = 20;
    public static final float KillAfter = 40.0f;
    public static final float MaxBite = 12.0f;

    private final Set<UUID> Crossers = new HashSet<>();
    private final Map<UUID, Occupant> Occupants = new HashMap<>();
    private long OpenedAt = -1L;
    private boolean Awake;
    private long LastFeeding = -1L;
    private long NextFlicker;
    private long NextAmbience;
    private long NextRoomBeat;
    private int Motion = BulkheadMotion.Closed;
    private long MotionStart;
    private int Cues;
    private boolean Held;

    public BulkheadDoorBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp002Registry.DoorEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, BulkheadDoorBlockEntity Door) {
        if (World instanceof ServerLevel Server) {
            Door.tickDoor(Server, State);
            Door.tickRoom(Server);
        }
    }

    public Direction getFront() {
        return getBlockState().getValue(BulkheadDoorBlock.Facing);
    }

    public BlockPos getOrigin() {
        return Scp002Structure.originFromDoor(worldPosition, getFront());
    }

    public boolean isAwake() {
        return Awake;
    }

    public boolean isIdleClosed() {
        return Motion == BulkheadMotion.Closed;
    }

    public boolean isHeld() {
        return Held;
    }

    public void hold(ServerLevel World, boolean Holding) {
        if (Held != Holding) {
            Held = Holding;
            setChanged();
        }
        if (Held) {
            open(World);
        }
    }

    public BulkheadMotion.Pose getPose(float PartialTick) {
        if (level == null) {
            return BulkheadMotion.pose(Motion, 0.0f);
        }
        return BulkheadMotion.pose(Motion, (float) (level.getGameTime() - MotionStart) + PartialTick);
    }

    public AABB getRenderBounds() {
        return new AABB(worldPosition).expandTowards(0.0, 1.0, 0.0).inflate(1.6, 0.1, 1.6);
    }

    private void begin(ServerLevel World, int Next) {
        Motion = Next;
        MotionStart = World.getGameTime();
        Cues = 0;
        setChanged();
        World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private boolean cue(int Bit, long Elapsed, int At) {
        if (Elapsed < At || (Cues & Bit) != 0) {
            return false;
        }
        Cues |= Bit;
        return Elapsed - At <= BulkheadMotion.LateCue;
    }

    public void open(ServerLevel World) {
        if (Motion != BulkheadMotion.Closed) {
            return;
        }
        Crossers.clear();
        begin(World, BulkheadMotion.Opening);
        Scp002Sounds.doorOpen(World, worldPosition);
    }

    private void close(ServerLevel World) {
        BulkheadDoorBlock.setOpen(World, worldPosition, false);
        OpenedAt = -1L;
        Crossers.clear();
        begin(World, BulkheadMotion.Closing);
        Scp002Sounds.doorSwingBack(World, worldPosition);
    }

    private void tickDoor(ServerLevel World, BlockState State) {
        if (Held) {
            open(World);
        }
        long Now = World.getGameTime();
        if (MotionStart > Now) {
            MotionStart = Now;
        }
        long Elapsed = Now - MotionStart;
        boolean Passable = State.getValue(BulkheadDoorBlock.Open);
        if (Motion == BulkheadMotion.Opening) {
            if (cue(1, Elapsed, BulkheadMotion.HissCue)) {
                Scp002Sounds.doorHiss(World, worldPosition);
            }
            if (cue(8, Elapsed, BulkheadMotion.SwingCue)) {
                Scp002Sounds.doorSwing(World, worldPosition);
            }
            if (Elapsed >= BulkheadMotion.PassableAt && !Passable) {
                BulkheadDoorBlock.setOpen(World, worldPosition, true);
                Passable = true;
            }
            if (Elapsed >= BulkheadMotion.OpenTicks) {
                OpenedAt = Now;
                begin(World, BulkheadMotion.Open);
            }
        } else if (Motion == BulkheadMotion.Closing) {
            if (Passable) {
                BulkheadDoorBlock.setOpen(World, worldPosition, false);
                Passable = false;
            }
            if (cue(2, Elapsed, BulkheadMotion.SlamCue)) {
                Scp002Sounds.doorClose(World, worldPosition);
            }
            if (cue(4, Elapsed, BulkheadMotion.LatchCue)) {
                Scp002Sounds.doorLatch(World, worldPosition);
            }
            if (Elapsed >= BulkheadMotion.CloseTicks) {
                begin(World, BulkheadMotion.Closed);
            }
        } else if (Motion == BulkheadMotion.Open && !Passable) {
            BulkheadDoorBlock.setOpen(World, worldPosition, true);
            Passable = true;
        } else if (Motion == BulkheadMotion.Closed && Passable) {
            BulkheadDoorBlock.setOpen(World, worldPosition, false);
            Passable = false;
        }
        if (!Passable) {
            return;
        }
        AABB Doorway = new AABB(worldPosition).expandTowards(0.0, 1.0, 0.0);
        List<LivingEntity> InDoor = World.getEntitiesOfClass(LivingEntity.class, Doorway, LivingEntity::isAlive);
        for (LivingEntity Crossing : InDoor) {
            Crossers.add(Crossing.getUUID());
        }
        if (Motion != BulkheadMotion.Open || !InDoor.isEmpty()) {
            return;
        }
        if (OpenedAt < 0L || OpenedAt > Now) {
            OpenedAt = Now;
        }
        AABB Room = Scp002Structure.interior(getOrigin(), getFront());
        boolean Entered = false;
        for (UUID Id : Crossers) {
            Entity Found = World.getEntity(Id);
            if (Found != null && Found.isAlive() && Room.contains(Found.position())) {
                Entered = true;
                break;
            }
        }
        if (!Held && (Entered || Now - OpenedAt > AutoCloseTicks)) {
            close(World);
        }
    }

    public static boolean isPrey(LivingEntity Candidate) {
        if (!Candidate.isAlive() || Candidate.isSpectator() || Candidate instanceof ArmorStand || Candidate.isInvulnerable() || !Targeting.isTargetable(Candidate)) {
            return false;
        }
        return !(Candidate instanceof Player User) || !User.getAbilities().invulnerable;
    }

    private void tickRoom(ServerLevel World) {
        long Now = World.getGameTime();
        BlockPos Origin = getOrigin();
        Direction Front = getFront();
        AABB Room = Scp002Structure.interior(Origin, Front);
        Vec3 Heart = Room.getCenter();
        List<LivingEntity> Inside = World.getEntitiesOfClass(LivingEntity.class, Room, Candidate -> isPrey(Candidate) && Room.contains(Candidate.position()));
        Set<UUID> Present = new HashSet<>();
        boolean Feeding = false;
        boolean Uneasy = false;
        float Deepest = 0.0f;
        RandomSource Random = World.random;
        for (LivingEntity Prey : Inside) {
            Present.add(Prey.getUUID());
            Scp002Rooms.mark(Prey, Now);
            Occupant Guest = Occupants.computeIfAbsent(Prey.getUUID(), Id -> new Occupant(Now, Now + MinDelay + Random.nextInt(MaxDelay - MinDelay + 1)));
            if (Now >= Guest.ConsumeAt) {
                Feeding = true;
                Deepest = Math.max(Deepest, (Now - Guest.ConsumeAt) / 20.0f);
                feed(World, Room, Prey, Guest, Now);
            } else if (Now - Guest.Entered > (Guest.ConsumeAt - Guest.Entered) / 2) {
                Uneasy = true;
            }
        }
        Occupants.keySet().retainAll(Present);
        if (Now >= NextRoomBeat && (Feeding || Uneasy)) {
            if (Feeding) {
                NextRoomBeat = Now + Math.max(7, 24 - (int) Deepest);
                Scp002Sounds.heartbeat(World, Heart, Math.min(1.0f, 0.6f + Deepest * 0.02f), Math.min(1.1f, 0.8f + Deepest * 0.006f));
            } else {
                NextRoomBeat = Now + 44 + Random.nextInt(12);
                Scp002Sounds.heartbeat(World, Heart, 0.35f, 0.8f);
            }
        }
        if (Feeding) {
            LastFeeding = Now;
            if (!Awake) {
                wake(World, Origin, Front, Heart);
            }
        } else if (Awake && Now - LastFeeding > CalmDelay) {
            calm(World, Origin, Front);
        }
        if (Awake && Now >= NextFlicker) {
            flicker(World, Origin, Front, Random, Now);
        }
        if (!Present.isEmpty() && Now >= NextAmbience) {
            NextAmbience = Now + (Awake ? 30 + Random.nextInt(40) : 140 + Random.nextInt(200));
            Vec3 Spot = wallPoint(Room, Random);
            Scp002Sounds.ambience(World, Spot, Awake, Random);
        }
    }

    private void feed(ServerLevel World, AABB Room, LivingEntity Prey, Occupant Guest, long Now) {
        float Seconds = (Now - Guest.ConsumeAt) / 20.0f;
        RandomSource Random = World.random;
        if (!Guest.Announced) {
            Guest.Announced = true;
            Guest.NextBite = Now + 10L;
            if (Prey instanceof ServerPlayer Victim) {
                Victim.displayClientMessage(Component.translatable("scp002.scp_unleashed.consume.begin").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            }
        }
        if (Now >= Guest.NextBite) {
            Guest.NextBite = Now + BiteInterval;
            float Amount = Seconds >= KillAfter ? 1000.0f : Math.min(MaxBite, 0.5f + Seconds * 0.3f);
            DamageSource Source = World.damageSources().source(Scp002Registry.ConsumedDamage);
            int Slowness = Mth.clamp((int) (Seconds / 5.0f), 0, 3);
            Prey.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false, false));
            Prey.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, Slowness, false, false, false));
            if (Seconds > 6.0f) {
                Prey.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1, false, false, false));
            }
            Prey.hurt(Source, Amount);
            Scp002Sounds.tear(World, Prey.position().add(0.0, Prey.getBbHeight() * 0.5, 0.0), Random);
            tendrils(World, Room, Prey, Random, 4 + Math.min(8, (int) (Seconds / 3.0f)));
            if (Prey instanceof ServerPlayer Victim) {
                PacketDistributor.sendToPlayer(Victim, new ConsumePayload(Math.min(1.0f, 0.3f + Seconds / 18.0f)));
            }
        }
    }

    private static Vec3 wallPoint(AABB Room, RandomSource Random) {
        double X = Mth.lerp(Random.nextDouble(), Room.minX + 0.05, Room.maxX - 0.05);
        double Y = Mth.lerp(Random.nextDouble(), Room.minY + 0.05, Room.maxY - 0.05);
        double Z = Mth.lerp(Random.nextDouble(), Room.minZ + 0.05, Room.maxZ - 0.05);
        return switch (Random.nextInt(5)) {
            case 0 -> new Vec3(Room.minX + 0.05, Y, Z);
            case 1 -> new Vec3(Room.maxX - 0.05, Y, Z);
            case 2 -> new Vec3(X, Y, Room.minZ + 0.05);
            case 3 -> new Vec3(X, Room.maxY - 0.05, Z);
            default -> new Vec3(X, Y, Room.maxZ - 0.05);
        };
    }

    private static void tendrils(ServerLevel World, AABB Room, LivingEntity Prey, RandomSource Random, int Count) {
        Vec3 Target = Prey.position().add(0.0, Prey.getBbHeight() * 0.55, 0.0);
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Start = wallPoint(Room, Random);
            Vec3 Toward = Target.subtract(Start);
            double Length = Math.max(0.001, Toward.length());
            Vec3 Step = Toward.scale(1.0 / Length);
            World.sendParticles(Scp002Registry.Tendril.get(), Start.x, Start.y, Start.z, 0, Step.x, Step.y, Step.z, 0.05 + Length * 0.012);
        }
    }

    private void wake(ServerLevel World, BlockPos Origin, Direction Front, Vec3 Heart) {
        Awake = true;
        NextRoomBeat = World.getGameTime() + 4L;
        Scp002Structure.setAwake(World, Origin, Front, true);
        Scp002Sounds.groan(World, Heart, World.random);
        setChanged();
    }

    private void calm(ServerLevel World, BlockPos Origin, Direction Front) {
        Awake = false;
        Scp002Structure.setAwake(World, Origin, Front, false);
        BlockPos LampPos = Scp002Structure.toWorld(Origin, Front, Scp002Structure.LampCell);
        BlockState Lamp = World.getBlockState(LampPos);
        if (Lamp.getBlock() instanceof BoneLampBlock && !Lamp.getValue(BoneLampBlock.Lit)) {
            World.setBlock(LampPos, Lamp.setValue(BoneLampBlock.Lit, true), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    private void flicker(ServerLevel World, BlockPos Origin, Direction Front, RandomSource Random, long Now) {
        BlockPos LampPos = Scp002Structure.toWorld(Origin, Front, Scp002Structure.LampCell);
        BlockState Lamp = World.getBlockState(LampPos);
        if (!(Lamp.getBlock() instanceof BoneLampBlock)) {
            NextFlicker = Now + 200L;
            return;
        }
        boolean Lit = Lamp.getValue(BoneLampBlock.Lit);
        World.setBlock(LampPos, Lamp.setValue(BoneLampBlock.Lit, !Lit), Block.UPDATE_CLIENTS);
        NextFlicker = Now + (Lit ? 2 + Random.nextInt(7) : 8 + Random.nextInt(30));
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putLong("OpenedAt", OpenedAt);
        Tag.putBoolean("Awake", Awake);
        Tag.putLong("LastFeeding", LastFeeding);
        Tag.putInt("Motion", Motion);
        Tag.putLong("MotionStart", MotionStart);
        Tag.putBoolean("Held", Held);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        OpenedAt = Tag.contains("OpenedAt") ? Tag.getLong("OpenedAt") : -1L;
        Awake = Tag.getBoolean("Awake");
        LastFeeding = Tag.contains("LastFeeding") ? Tag.getLong("LastFeeding") : -1L;
        Motion = Mth.clamp(Tag.getInt("Motion"), BulkheadMotion.Closed, BulkheadMotion.Closing);
        MotionStart = Tag.getLong("MotionStart");
        Held = Tag.getBoolean("Held");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Awake", Awake);
        Tag.putInt("Motion", Motion);
        Tag.putLong("MotionStart", MotionStart);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static final class Occupant {
        private final long Entered;
        private final long ConsumeAt;
        private long NextBite;
        private boolean Announced;

        private Occupant(long Entered, long ConsumeAt) {
            this.Entered = Entered;
            this.ConsumeAt = ConsumeAt;
        }
    }
}
