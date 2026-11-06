package net.rai.scp_unleashed.scp018;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp018.network.BallPathPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Scp018Ball extends Projectile implements IEntityWithComplexSpawn {
    public static final double Nudge = 1.0e-4;
    public static final double EndSlack = 0.02;
    public static final double PauseGap = 2.5;
    public static final double TinySegment = 1.0e-3;
    public static final int JamLimit = 24;
    public static final int MaxPlanSteps = 64;
    public static final int MaxAdvanceSteps = 96;
    public static final int MaxSpawnSegments = 64;
    public static final float PickRadius = 0.3f;
    public static final double RenderRange = 96.0;

    private static Consumer<Scp018Ball> ClientHook = Ball -> {
    };

    private final BallPath Path = new BallPath();
    private final List<BallSegment> Outbox = new ArrayList<>();
    private ItemStack Carried = ItemStack.EMPTY;
    private double LastTime = Double.NaN;
    private double SpawnTime = Double.NEGATIVE_INFINITY;
    private int LastHitId = -1;
    private double LastHitTime = Double.NEGATIVE_INFINITY;
    private int Finish = -1;
    private double FinishTime;
    private Vec3 FinishPoint = Vec3.ZERO;
    private int JamCount;
    private long LastSignal = Long.MIN_VALUE;
    private double LastFloatCheck = Double.NEGATIVE_INFINITY;
    @Nullable
    private Object ClientData;

    public Scp018Ball(EntityType<? extends Scp018Ball> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    public static Scp018Ball thrown(ServerLevel World, LivingEntity Thrower, ItemStack Stack, double Speed) {
        Scp018Ball Ball = new Scp018Ball(Scp018Registry.BallEntity.get(), World);
        Ball.setOwner(Thrower);
        Ball.Carried = Stack.copyWithCount(1);
        Vec3 Look = Thrower.getLookAngle();
        Vec3 Centre = Thrower.getEyePosition().add(Look.scale(0.2)).subtract(0.0, 0.1, 0.0);
        Vec3 Carry = Thrower.getKnownMovement();
        Vec3 Velocity = Look.scale(Speed).add(Carry.x, Thrower.onGround() ? 0.0 : Carry.y, Carry.z);
        Ball.setCentre(Centre);
        Ball.setDeltaMovement(Velocity);
        double Now = World.getGameTime();
        Ball.SpawnTime = Now;
        Ball.LastTime = Now;
        Ball.restart(World, Centre, Velocity, Now);
        Ball.Outbox.clear();
        World.addFreshEntity(Ball);
        return Ball;
    }

    public static void setClientHook(Consumer<Scp018Ball> Hook) {
        ClientHook = Hook;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    public BallPath path() {
        return Path;
    }

    @Nullable
    public Object clientData() {
        return ClientData;
    }

    public void setClientData(@Nullable Object Data) {
        ClientData = Data;
    }

    public ItemStack carried() {
        return Carried.isEmpty() ? new ItemStack(Scp018Registry.BallItem.get()) : Carried.copy();
    }

    public Vec3 centre() {
        return position().add(0.0, BallTuning.Radius, 0.0);
    }

    public void setCentre(Vec3 Centre) {
        setPos(Centre.x, Centre.y - BallTuning.Radius, Centre.z);
    }

    public double speedNow() {
        if (Path.isEmpty()) {
            return getDeltaMovement().length();
        }
        return Path.speedAt(level().isClientSide ? LastTime : level().getGameTime());
    }

    public void setClientTime(double Time) {
        LastTime = Time;
    }

    public double clientTime() {
        return LastTime;
    }

    public void acceptPath(List<BallSegment> Segments) {
        Path.merge(Segments);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel World) {
            serverTick(World);
        } else {
            ClientHook.accept(this);
        }
    }

    private void serverTick(ServerLevel World) {
        double Now = World.getGameTime();
        if (Double.isNaN(LastTime) || Path.isEmpty() && Finish < 0) {
            if (SpawnTime == Double.NEGATIVE_INFINITY) {
                SpawnTime = Now - BallTuning.OwnerGraceTicks;
            }
            LastTime = Now;
            restart(World, centre(), getDeltaMovement(), Now);
        } else if (Now - LastTime > PauseGap || Now < LastTime) {
            double Offset = Now - LastTime;
            Path.shift(Offset);
            SpawnTime += Offset;
            LastHitTime += Offset;
            FinishTime += Offset;
            LastFloatCheck += Offset;
            LastTime = Now;
            Outbox.clear();
            Outbox.addAll(Path.segments());
        }
        if (Finish < 0) {
            advance(World, Now);
        }
        if (Finish < 0 && !isRemoved()) {
            checkFloat(World, Now);
            extend(World, Now + BallTuning.Lookahead);
            Vec3 Here = Path.positionAt(Now);
            if (Here != null) {
                setCentre(Here);
            }
            setDeltaMovement(Path.velocityAt(Now));
        }
        LastTime = Now;
        flush();
        if (Finish >= 0 && !isRemoved()) {
            setCentre(FinishPoint);
            setDeltaMovement(Vec3.ZERO);
            if (Now >= FinishTime + BallTuning.TerminalDelay) {
                finish(World);
            }
        }
    }

    private void restart(ServerLevel World, Vec3 Start, Vec3 Velocity, double Time) {
        Path.clear();
        BallSegment First = plan(World, Start, Velocity, Time);
        Path.add(First);
        Outbox.add(First);
        extend(World, Time + BallTuning.Lookahead);
    }

    private void replan(ServerLevel World, Vec3 Start, Vec3 Velocity, double Time) {
        Path.clear();
        BallSegment First = plan(World, Start, Velocity, Time);
        Path.add(First);
        Outbox.add(First);
    }

    private void append(BallSegment Segment) {
        Path.add(Segment);
        Outbox.add(Segment);
    }

    private void extend(ServerLevel World, double Horizon) {
        int Guard = 0;
        while (!Path.isEmpty() && Guard++ < MaxPlanSteps) {
            BallSegment Last = Path.last();
            if (Last.isTerminal() || Last.endTime() >= Horizon) {
                return;
            }
            append(next(World, Last));
        }
    }

    private void checkFloat(ServerLevel World, double Now) {
        if (Path.isEmpty() || !Path.first().isFloating()) {
            return;
        }
        BallSegment Resting = Path.first();
        if (Now - Math.max(LastFloatCheck, Resting.StartTime()) < BallTuning.RestCheckTicks) {
            return;
        }
        LastFloatCheck = Now;
        Vec3 Here = Resting.Start();
        double Wet = BallWater.submersion(World, Here);
        Vec3 Flow = BallWater.flow(World, Here);
        if (BallWater.blocked(World, this, Here) || !BallWater.settled(World, this, Here, Vec3.ZERO, Wet, Flow)) {
            replan(World, Here, Vec3.ZERO, Now);
        }
    }

    private BallSegment next(ServerLevel World, BallSegment Last) {
        Vec3 End = Last.end();
        Vec3 Incoming = Last.endVelocity();
        if (Last.isBounce()) {
            return plan(World, End.add(Last.Normal().scale(Nudge)), bounce(Incoming, Last.Normal(), Last.inWater()), Last.endTime());
        }
        return plan(World, End, Incoming, Last.endTime());
    }

    public static Vec3 bounce(Vec3 Incoming, Vec3 Normal, boolean Water) {
        double Into = Incoming.dot(Normal);
        Vec3 Reflected = Incoming.subtract(Normal.scale(2.0 * Into));
        if (Water) {
            return Reflected;
        }
        if (-Into >= BallTuning.GrazeSpeed) {
            Reflected = Reflected.scale(BallTuning.BounceGain);
        }
        double Away = Reflected.dot(Normal);
        if (Normal.y > 0.5 && Away < BallTuning.MinimumHop) {
            Reflected = Reflected.add(Normal.scale(BallTuning.MinimumHop - Away));
        }
        return Reflected;
    }

    private BallSegment plan(ServerLevel World, Vec3 Start, Vec3 Velocity, double Time) {
        double Wet = BallWater.submersion(World, Start);
        if (Wet > 0.0) {
            return planWater(World, Start, Velocity, Time, Wet);
        }
        BallSegment Probe = BallSegment.of(Start, Velocity, Time, BallTuning.MaxSegmentTicks, BallSegment.KindSplit, Vec3.ZERO, -1);
        BallSweep.Hit Found = BallSweep.sweep(World, this, Probe, 0.0, BallTuning.MaxSegmentTicks, true, true, this::mayStrike);
        if (Found == null) {
            JamCount = 0;
            return Probe;
        }
        if (Found.Unloaded()) {
            return BallSegment.of(Start, Velocity, Time, Found.Tau(), BallSegment.KindDrop, Vec3.ZERO, -1);
        }
        if (Found.Water()) {
            JamCount = 0;
            return BallSegment.of(Start, Velocity, Time, Found.Tau(), BallSegment.KindSplash, Vec3.ZERO, -1);
        }
        JamCount = Found.Tau() < TinySegment ? JamCount + 1 : 0;
        if (JamCount > JamLimit) {
            return BallSegment.of(Start, Velocity, Time, Found.Tau(), BallSegment.KindDrop, Vec3.ZERO, -1);
        }
        Vec3 Outgoing = bounce(Probe.velocityAtLocal(Found.Tau()), Found.Normal(), false);
        int Kind = Outgoing.length() >= BallTuning.ExplodeSpeed ? BallSegment.KindExplode : Found.Struck() != null ? BallSegment.KindEntity : BallSegment.KindBlock;
        return BallSegment.of(Start, Velocity, Time, Found.Tau(), Kind, Found.Normal(), Found.Struck() != null ? Found.Struck().getId() : -1);
    }

    private BallSegment planWater(ServerLevel World, Vec3 Start, Vec3 Incoming, double Time, double Wet) {
        Vec3 Flow = BallWater.flow(World, Start);
        if (BallWater.settled(World, this, Start, Incoming, Wet, Flow)) {
            JamCount = 0;
            return BallSegment.floating(Start, Time);
        }
        double Ticks = BallWater.stepTicks(Incoming);
        Vec3 Velocity = BallWater.step(Incoming, Wet, Flow, Ticks);
        BallSegment Probe = BallSegment.water(Start, Velocity, Time, Ticks, BallSegment.KindSplit, Vec3.ZERO, -1);
        BallSweep.Hit Found = BallSweep.sweep(World, this, Probe, 0.0, Ticks, true, false, this::mayStrike);
        if (Found == null) {
            JamCount = 0;
            return Probe;
        }
        if (Found.Unloaded()) {
            return BallSegment.water(Start, Velocity, Time, Found.Tau(), BallSegment.KindDrop, Vec3.ZERO, -1);
        }
        JamCount = Found.Tau() < TinySegment ? JamCount + 1 : 0;
        if (JamCount > JamLimit) {
            JamCount = 0;
            return BallSegment.floating(Start, Time);
        }
        int Kind = Found.Struck() != null ? BallSegment.KindEntity : BallSegment.KindBlock;
        return BallSegment.water(Start, Velocity, Time, Found.Tau(), Kind, Found.Normal(), Found.Struck() != null ? Found.Struck().getId() : -1);
    }

    private boolean mayStrike(Entity Target, double Time) {
        Entity Owner = getOwner();
        if (Owner != null && Target == Owner && Time < SpawnTime + BallTuning.OwnerGraceTicks) {
            return false;
        }
        return !(Target.getId() == LastHitId && Time < LastHitTime + BallTuning.RehitTicks);
    }

    private void advance(ServerLevel World, double Now) {
        double Cursor = Math.min(LastTime, Now);
        int Guard = 0;
        while (Finish < 0 && !Path.isEmpty() && Guard++ < MaxAdvanceSteps) {
            BallSegment Current = Path.first();
            double End = Current.endTime();
            double Until = Math.min(Now, End - EndSlack);
            if (Until > Cursor && Current.Duration() > 0.0 && !Current.isFloating()) {
                int Planned = Current.Kind() == BallSegment.KindEntity ? Current.Target() : -1;
                BallSweep.Hit Found = BallSweep.sweep(World, this, Current, Math.max(0.0, Cursor - Current.StartTime()), Until - Current.StartTime(), false,
                        !Current.inWater(), (Target, Time) -> Target.getId() != Planned && mayStrike(Target, Time));
                if (Found != null && Found.Water()) {
                    Cursor = Current.StartTime() + Found.Tau();
                    splashed(World, Current, Found, Cursor);
                    continue;
                }
                if (Found != null && !Found.Unloaded()) {
                    Cursor = Current.StartTime() + Found.Tau();
                    struck(World, Current, Found, Cursor);
                    continue;
                }
            }
            if (End > Now) {
                return;
            }
            if (Path.size() == 1 && !Current.isTerminal()) {
                append(next(World, Current));
            }
            Path.removeFirst();
            Cursor = End;
            arrive(World, Current, End);
        }
    }

    private void splashed(ServerLevel World, BallSegment Current, BallSweep.Hit Found, double Time) {
        Vec3 Point = Current.positionAtLocal(Found.Tau());
        Vec3 Incoming = Current.velocityAtLocal(Found.Tau());
        Path.clear();
        Outbox.add(BallSegment.resting(Point, Time, BallSegment.KindSplash, BallSegment.ModeAir));
        replan(World, Point, Incoming, Time);
    }

    private void struck(ServerLevel World, BallSegment Current, BallSweep.Hit Found, double Time) {
        Vec3 Point = Current.positionAtLocal(Found.Tau());
        Vec3 Incoming = Current.velocityAtLocal(Found.Tau());
        Path.clear();
        int Kind = Found.Struck() != null ? BallSegment.KindEntity : BallSegment.KindBlock;
        Outbox.add(BallSegment.resting(Point, Time, Kind, Current.Mode()));
        if (Found.Struck() != null && impact(World, Found.Struck(), Incoming, Point, Time)) {
            return;
        }
        Vec3 Outgoing = bounce(Incoming, Found.Normal(), Current.inWater());
        if (!Current.inWater() && Outgoing.length() >= BallTuning.ExplodeSpeed) {
            end(BallSegment.KindExplode, Point, Time);
            return;
        }
        signal(World, Time);
        replan(World, Point.add(Found.Normal().scale(Nudge)), Outgoing, Time);
    }

    private void arrive(ServerLevel World, BallSegment Current, double Time) {
        Vec3 Point = Current.end();
        switch (Current.Kind()) {
            case BallSegment.KindBlock -> {
                if (BallSweep.blockStillThere(World, this, Point, Current.Normal())) {
                    signal(World, Time);
                } else {
                    replan(World, Point, Current.endVelocity(), Time);
                }
            }
            case BallSegment.KindEntity -> {
                Entity Target = World.getEntity(Current.Target());
                if (BallSweep.entityStillThere(Target, Point)) {
                    impact(World, Target, Current.endVelocity(), Point, Time);
                } else {
                    replan(World, Point, Current.endVelocity(), Time);
                }
            }
            case BallSegment.KindExplode -> end(BallSegment.KindExplode, Point, Time);
            case BallSegment.KindDrop -> end(BallSegment.KindDrop, Point, Time);
            case BallSegment.KindStop -> end(BallSegment.KindStop, Point, Time);
            case BallSegment.KindSplash -> World.gameEvent(this, GameEvent.SPLASH, Point);
            default -> {
            }
        }
    }

    private void end(int Kind, Vec3 Point, double Time) {
        if (Finish >= 0) {
            return;
        }
        Finish = Kind;
        FinishTime = Time;
        FinishPoint = Point;
        Path.clear();
        Outbox.add(BallSegment.resting(Point, Time, Kind, BallSegment.ModeAir));
    }

    private boolean impact(ServerLevel World, Entity Target, Vec3 Incoming, Vec3 Point, double Time) {
        double Speed = Incoming.length();
        LastHitId = Target.getId();
        LastHitTime = Time;
        signal(World, Time);
        if (Target instanceof Player Catcher && Speed < BallTuning.DamageSpeed) {
            if (tryCatch(Catcher)) {
                end(BallSegment.KindStop, Point, Time);
                discard();
                return true;
            }
            return false;
        }
        if (Speed >= BallTuning.DamageSpeed) {
            Entity Owner = getOwner();
            Entity Cause = Owner == Target ? null : Owner;
            DamageSource Source = World.damageSources().source(Scp018Registry.ImpactDamage, Cause, this);
            setDeltaMovement(Incoming);
            if (Target.hurt(Source, BallTuning.damageFor(Speed))) {
                World.playSound(null, Point.x, Point.y, Point.z, Scp018Registry.Hit.get(), SoundSource.NEUTRAL, 0.5f + (float) BallTuning.danger(Speed) * 0.5f,
                        0.9f + (float) BallTuning.danger(Speed) * 0.4f);
            }
        }
        return false;
    }

    public boolean tryCatch(Player Catcher) {
        if (isRemoved() || Catcher.isSpectator()) {
            return false;
        }
        ItemStack Stack = carried();
        if (!Catcher.getInventory().add(Stack)) {
            return false;
        }
        if (level() instanceof ServerLevel World) {
            World.getChunkSource().broadcast(this, new ClientboundTakeItemEntityPacket(getId(), Catcher.getId(), 1));
        }
        level().playSound(null, Catcher.getX(), Catcher.getEyeY(), Catcher.getZ(), Scp018Registry.Catch.get(), SoundSource.PLAYERS, 0.7f, 0.95f + random.nextFloat() * 0.1f);
        return true;
    }

    private void signal(ServerLevel World, double Time) {
        long Tick = (long) Math.floor(Time);
        if (Tick != LastSignal) {
            LastSignal = Tick;
            World.gameEvent(this, GameEvent.PROJECTILE_LAND, position());
        }
    }

    private void finish(ServerLevel World) {
        Vec3 Point = FinishPoint;
        if (Finish == BallSegment.KindExplode) {
            World.explode(this, null, null, Point.x, Point.y, Point.z, BallTuning.ExplosionPower, false, Level.ExplosionInteraction.MOB);
        }
        if (Finish == BallSegment.KindExplode || Finish == BallSegment.KindDrop) {
            ItemEntity Dropped = new ItemEntity(World, Point.x, Point.y - BallTuning.Radius, Point.z, carried(), 0.0, 0.12, 0.0);
            Dropped.setDefaultPickUpDelay();
            World.addFreshEntity(Dropped);
        }
        discard();
    }

    private void flush() {
        if (Outbox.isEmpty()) {
            return;
        }
        List<BallSegment> Sent = List.copyOf(Outbox.size() > BallPathPayload.MaxSegments ? Outbox.subList(Outbox.size() - BallPathPayload.MaxSegments, Outbox.size()) : Outbox);
        Outbox.clear();
        if (!isRemoved()) {
            PacketDistributor.sendToPlayersTrackingEntity(this, new BallPathPayload(getId(), Sent));
        }
    }

    @Override
    public void playerTouch(Player User) {
        if (!(level() instanceof ServerLevel World) || isRemoved() || Finish >= 0) {
            return;
        }
        double Now = World.getGameTime();
        BallSegment Here = Path.at(Now);
        if (Here == null || !Here.inWater() || Path.speedAt(Now) >= BallTuning.DamageSpeed) {
            return;
        }
        if (User == getOwner() && Now < SpawnTime + BallTuning.OwnerGraceTicks) {
            return;
        }
        if (tryCatch(User)) {
            end(BallSegment.KindStop, centre(), Now);
            discard();
        }
    }

    @Override
    public InteractionResult interact(Player User, InteractionHand Hand) {
        if (isRemoved() || Finish >= 0 || speedNow() >= BallTuning.DamageSpeed) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (tryCatch(User)) {
            Finish = BallSegment.KindStop;
            discard();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void lerpTo(double X, double Y, double Z, float Yaw, float Pitch, int Steps) {
        if (Path.isEmpty()) {
            super.lerpTo(X, Y, Z, Yaw, Pitch, Steps);
        }
    }

    @Override
    public void lerpMotion(double X, double Y, double Z) {
        if (Path.isEmpty()) {
            setDeltaMovement(X, Y, Z);
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        List<BallSegment> Segments = Path.segments();
        if (Segments.size() > MaxSpawnSegments) {
            Segments = Segments.subList(Segments.size() - MaxSpawnSegments, Segments.size());
        }
        Buffer.writeVarInt(Segments.size());
        for (BallSegment Segment : Segments) {
            BallSegment.SegmentCodec.encode(Buffer, Segment);
        }
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        int Count = Buffer.readVarInt();
        List<BallSegment> Segments = new ArrayList<>();
        for (int Index = 0; Index < Count; Index++) {
            Segments.add(BallSegment.SegmentCodec.decode(Buffer));
        }
        Path.merge(Segments);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (!Carried.isEmpty()) {
            Tag.put("Item", Carried.save(registryAccess()));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.contains("Item")) {
            Carried = ItemStack.parseOptional(registryAccess(), Tag.getCompound("Item"));
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public float getPickRadius() {
        return PickRadius;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity Attacker) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return carried();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Range = RenderRange * getViewScale();
        return Distance < Range * Range;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(getDeltaMovement().length() * 1.5 + 0.25);
    }
}
