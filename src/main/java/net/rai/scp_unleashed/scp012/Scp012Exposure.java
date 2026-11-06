package net.rai.scp_unleashed.scp012;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.scp012.network.Scp012StatePayload;
import net.rai.scp_unleashed.cameras.CameraViews;
import net.rai.scp_unleashed.senses.Senses;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageGeometry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class Scp012Exposure {
    public static final int Cycle = 10;
    public static final int MeterInterval = 40;
    public static final int DamageInterval = 100;
    public static final int FirstDamageDelay = 60;
    public static final float DamageAmount = 1.0f;
    public static final int WindowRange = 8;
    public static final double ViewCone = 0.5;
    public static final double SmallWindowY = 9.0;
    public static final double TallLowerWindowY = 10.5;
    public static final double TallUpperWindowY = 22.5;
    public static final double WindowZ = 1.0;
    public static final int PageEvery = 5;
    private static final Map<UUID, Reader> Readers = new HashMap<>();

    private Scp012Exposure() {
    }

    public static boolean isExempt(Player User) {
        return !User.isAlive() || User.isCreative() || User.isSpectator() || !Targeting.canWitness(User);
    }

    public static void tick(ServerPlayer User) {
        if (Math.floorMod(User.tickCount + User.getId(), Cycle) != 0) {
            return;
        }
        Reader State = reader(User);
        Sighting Found = isExempt(User) ? null : find(User);
        if (Found == null) {
            if (State.Exposed) {
                State.Exposed = false;
                State.MeterClock = 0;
                State.Steps = 0;
            }
            sync(User);
            return;
        }
        if (!State.Exposed) {
            State.Exposed = true;
            State.MeterClock = 0;
            State.DamageClock = DamageInterval - FirstDamageDelay;
        }
        State.MeterClock += Cycle;
        State.DamageClock += Cycle;
        int Meter = Found.meter();
        if (State.MeterClock >= MeterInterval) {
            State.MeterClock -= MeterInterval;
            if (Meter < Scp012CompositionItem.Complete) {
                Meter = Found.advance(1);
                State.Steps++;
                if (State.Steps % PageEvery == 0) {
                    User.playNotifySound(Scp012Registry.Page.get(), SoundSource.PLAYERS, 0.45f, 0.78f + User.getRandom().nextFloat() * 0.1f);
                }
            }
            if (Meter >= Scp012CompositionItem.Complete) {
                Scp012Composer.claim(User);
            }
        }
        if (State.DamageClock >= DamageInterval) {
            State.DamageClock -= DamageInterval;
            drain(User);
        }
        State.Meter = Meter;
        sync(User);
    }

    private static void drain(ServerPlayer User) {
        if (User.hurt(User.damageSources().source(Scp012Registry.CompositionDamage), DamageAmount)) {
            float Pitch = 0.5f + User.getRandom().nextInt(3) * 0.03f;
            User.playNotifySound(Scp012Registry.Toll.get(), SoundSource.PLAYERS, 0.55f, Pitch);
            User.playNotifySound(Scp012Registry.TollRing.get(), SoundSource.PLAYERS, 0.3f, Pitch * 1.06f);
        }
    }

    @Nullable
    public static Sighting find(ServerPlayer User) {
        for (InteractionHand Hand : InteractionHand.values()) {
            if (Scp012CompositionItem.is(User.getItemInHand(Hand))) {
                return new Sighting(User, Hand, null);
            }
        }
        if (!(User.level() instanceof ServerLevel World)) {
            return null;
        }
        Vec3 Eye = CameraViews.eye(User);
        List<BlockPos> Candidates = Scp012Sources.near(World, Eye, WindowRange);
        StorageContainerBlockEntity Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Pos : Candidates) {
            StorageContainerBlockEntity Locker = StorageContainerBlock.containerAt(World, Pos);
            if (Locker == null || !Scp012Containment.isExposed(Locker) || !Scp012CompositionItem.is(Locker.peekContents())) {
                continue;
            }
            double Distance = sightDistance(World, User, Locker);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Locker;
            }
        }
        return Best == null ? null : new Sighting(User, null, Best);
    }

    public static double sightDistance(Level World, LivingEntity User, StorageContainerBlockEntity Locker) {
        if (Senses.isBlindfolded(User)) {
            return Double.MAX_VALUE;
        }
        Vec3 Eye = CameraViews.eye(User);
        Vec3 Look = CameraViews.look(User);
        Direction Facing = Locker.getFacing();
        Vec3 Normal = new Vec3(Facing.getStepX(), 0.0, Facing.getStepZ());
        BlockPos Pos = Locker.getStoragePos();
        boolean Tall = Locker.isTall();
        BlockPos Upper = Pos.above();
        Predicate<BlockPos> Ignored = Cell -> Cell.equals(Pos) || Tall && Cell.equals(Upper);
        double Reach = (double) WindowRange * WindowRange;
        double Best = Double.MAX_VALUE;
        double[] Heights = Tall ? new double[]{TallLowerWindowY, TallUpperWindowY} : new double[]{SmallWindowY};
        for (double Height : Heights) {
            Vec3 Window = StorageGeometry.point(Pos, Facing, StorageGeometry.CentreX, Height, WindowZ);
            Vec3 Towards = Window.subtract(Eye);
            double Distance = Towards.lengthSqr();
            if (Distance > Reach || Distance >= Best || Eye.subtract(Window).dot(Normal) <= 0.05) {
                continue;
            }
            if (Towards.normalize().dot(Look) < ViewCone) {
                continue;
            }
            if (clearSight(World, Eye, Window, Ignored)) {
                Best = Distance;
            }
        }
        return Best;
    }

    public static boolean clearSight(Level World, Vec3 From, Vec3 To, Predicate<BlockPos> Ignored) {
        Boolean Blocked = BlockGetter.traverseBlocks(From, To, World, (Getter, Pos) -> {
            if (Ignored.test(Pos)) {
                return null;
            }
            BlockState State = Getter.getBlockState(Pos);
            VoxelShape Shape = State.getVisualShape(Getter, Pos, CollisionContext.empty());
            if (Shape.isEmpty()) {
                return null;
            }
            return Shape.clip(From, To, Pos) != null ? Boolean.TRUE : null;
        }, Getter -> Boolean.FALSE);
        return !Blocked;
    }

    public static boolean isExposed(Player User) {
        Reader State = Readers.get(User.getUUID());
        return State != null && State.Exposed;
    }

    public static void sync(ServerPlayer User) {
        Reader State = reader(User);
        boolean Composer = Scp012Composer.isComposer(User);
        int Meter = State.Exposed ? State.Meter : 0;
        if (State.Sent && State.SentExposed == State.Exposed && State.SentComposer == Composer && State.SentMeter == Meter) {
            return;
        }
        State.Sent = true;
        State.SentExposed = State.Exposed;
        State.SentComposer = Composer;
        State.SentMeter = Meter;
        PacketDistributor.sendToPlayer(User, new Scp012StatePayload(Composer, State.Exposed, Meter));
    }

    public static void resend(ServerPlayer User) {
        Reader State = reader(User);
        State.Sent = false;
        State.Exposed = false;
        sync(User);
    }

    public static void forget(Player User) {
        Readers.remove(User.getUUID());
    }

    public static void clear() {
        Readers.clear();
    }

    private static Reader reader(Player User) {
        return Readers.computeIfAbsent(User.getUUID(), Key -> new Reader());
    }

    public record Sighting(LivingEntity User, @Nullable InteractionHand Hand, @Nullable StorageContainerBlockEntity Locker) {
        public ItemStack stack() {
            return Hand != null ? User.getItemInHand(Hand) : Locker == null ? ItemStack.EMPTY : Locker.peekContents();
        }

        public int meter() {
            return Scp012CompositionItem.completeness(stack());
        }

        public int advance(int Amount) {
            if (Hand != null) {
                ItemStack Held = User.getItemInHand(Hand);
                Scp012CompositionItem.setCompleteness(Held, Scp012CompositionItem.completeness(Held) + Amount);
                return Scp012CompositionItem.completeness(Held);
            }
            if (Locker == null) {
                return 0;
            }
            ItemStack Updated = Locker.peekContents().copy();
            Scp012CompositionItem.setCompleteness(Updated, Scp012CompositionItem.completeness(Updated) + Amount);
            Locker.setContents(Updated);
            return Scp012CompositionItem.completeness(Updated);
        }
    }

    private static final class Reader {
        private boolean Exposed;
        private int Meter;
        private int MeterClock;
        private int DamageClock;
        private int Steps;
        private boolean Sent;
        private boolean SentExposed;
        private boolean SentComposer;
        private int SentMeter;
    }
}
