package net.rai.scp_unleashed.scp4666;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666NoteEvent;
import net.rai.scp_unleashed.scp4666.block.Scp4666ExitDoorBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666OwnedBlockEntity;
import net.rai.scp_unleashed.scp4666.block.Scp4666PresentBlock;
import net.rai.scp_unleashed.scp4666.network.Scp4666FxPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp4666Endings {
    public static final int FateNone = 0;
    public static final int FateGood = 1;
    public static final int FateBad = 2;
    public static final int LoomTicks = 60;
    public static final int DragTicks = 70;
    public static final int BeatTicks = 100;
    public static final int SackTicks = 50;
    public static final int TakeTicks = 30;
    public static final int Swings = 6;
    public static final float SwingDamage = 1.0f;
    public static final float SafeHealth = 6.0f;
    public static final double HoldSlack = 0.35;
    private static final Map<UUID, Integer> Forced = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, Scene> Scenes = new ConcurrentHashMap<>();

    private static final class Scene {
        final List<UUID> Queue = new ArrayList<>();
        final Map<UUID, Vec3> Anchors = new HashMap<>();
        UUID Victim;
        int Ticks;
        int ManId = -1;
    }

    private Scp4666Endings() {
    }

    public static void force(UUID Id, int Fate) {
        if (Fate == FateNone) {
            Forced.remove(Id);
        } else {
            Forced.put(Id, Fate);
        }
    }

    public static void clear(MinecraftServer Server) {
        Scenes.remove(Server);
        Scp4666Pose.releaseAll(Server);
    }

    public static boolean running(MinecraftServer Server) {
        Scene Current = Scenes.get(Server);
        return Current != null && (Current.Victim != null || !Current.Queue.isEmpty());
    }

    public static int fateFor(ServerPlayer User, Scp4666State State, float Roll) {
        Integer Pinned = Forced.remove(User.getUUID());
        if (Pinned != null) {
            return Pinned;
        }
        return Roll < Scp4666Director.luck(State.visitor(User.getUUID())) ? FateGood : FateBad;
    }

    public static void begin(ServerLevel World, Scp4666Map Layout, Scp4666State State, List<ServerPlayer> Living) {
        State.setPhase(Scp4666State.PhaseEnding);
        State.setClock(Scp4666Clock.Dawn);
        Scene Current = new Scene();
        Scenes.put(World.getServer(), Current);
        List<ServerPlayer> Lucky = new ArrayList<>();
        for (ServerPlayer User : Living) {
            int Fate = fateFor(User, State, World.getRandom().nextFloat());
            State.visitor(User.getUUID()).Fate = Fate;
            if (Fate == FateGood) {
                Lucky.add(User);
                Scp4666NoteEvent.survived(User);
            } else {
                Current.Queue.add(User.getUUID());
                State.doomed().add(User.getUUID());
            }
        }
        State.touch();
        int Index = 0;
        for (ServerPlayer User : Lucky) {
            BlockPos Spot = Index < Layout.Presents.size() ? Layout.Presents.get(Index) : Layout.Presents.get(Index % Math.max(1, Layout.Presents.size())).above(Index / Math.max(1, Layout.Presents.size()));
            Scp4666PresentBlock.place(World, Spot, User);
            Index++;
            User.displayClientMessage(Component.translatable(Scp4666Director.Prefix + "morning_good").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        if (!Lucky.isEmpty()) {
            openExit(World, Layout);
            Vec3 Tree = Vec3.atCenterOf(Layout.Tree);
            Scp4666Cues.sound(World, Tree, List.of(Scp4666Registry.Chime, Scp4666Registry.ChimeBell), 1.2f, 1.2f);
        }
        for (UUID Id : Current.Queue) {
            ServerPlayer Victim = player(World, Id);
            if (Victim != null) {
                seize(World, Layout, Current, Victim);
            }
        }
        if (Current.Queue.isEmpty()) {
            State.setPhase(Scp4666State.PhaseOver);
        }
        Scp4666Director.sync(World, State);
    }

    public static void openExit(ServerLevel World, Scp4666Map Layout) {
        BlockState Lower = Scp4666Registry.ExitDoor.get().defaultBlockState().setValue(Scp4666ExitDoorBlock.Facing, Direction.NORTH).setValue(Scp4666ExitDoorBlock.Half, DoubleBlockHalf.LOWER);
        World.setBlock(Layout.ExitDoor, Lower, Block.UPDATE_ALL);
        World.setBlock(Layout.ExitDoor.above(), Lower.setValue(Scp4666ExitDoorBlock.Half, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    private static void seize(ServerLevel World, Scp4666Map Layout, Scene Current, ServerPlayer Victim) {
        Vec3 Spot = Vec3.atBottomCenterOf(Layout.BasementWake).add((Current.Anchors.size() % 3 - 1) * 1.2, 0.0, 0.0);
        Current.Anchors.put(Victim.getUUID(), Spot);
        place(World, Victim, Spot);
        Scp4666Pose.lie(Victim, true);
        PacketDistributor.sendToPlayer(Victim, new Scp4666FxPayload(Scp4666FxPayload.Hold, Spot.x, Spot.y, Spot.z, 1));
        PacketDistributor.sendToPlayer(Victim, new Scp4666FxPayload(Scp4666FxPayload.Wake, Spot.x, Spot.y, Spot.z, 1));
        Victim.displayClientMessage(Component.translatable(Scp4666Director.Prefix + "morning_bad").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
    }

    private static void place(ServerLevel World, ServerPlayer Victim, Vec3 Spot) {
        Victim.teleportTo(World, Spot.x, Spot.y, Spot.z, EnumSet.of(RelativeMovement.X_ROT, RelativeMovement.Y_ROT), Victim.getYRot(), Victim.getXRot());
        Victim.setDeltaMovement(Vec3.ZERO);
        Victim.hurtMarked = true;
    }

    public static boolean held(ServerPlayer User) {
        Scene Current = Scenes.get(User.server);
        return Current != null && Current.Anchors.containsKey(User.getUUID());
    }

    public static void release(ServerPlayer User) {
        Scene Current = Scenes.get(User.server);
        if (Current == null) {
            return;
        }
        Current.Anchors.remove(User.getUUID());
        Current.Queue.remove(User.getUUID());
        Scp4666Pose.lie(User, false);
        PacketDistributor.sendToPlayer(User, new Scp4666FxPayload(Scp4666FxPayload.Hold, 0.0, 0.0, 0.0, 0));
        if (User.getUUID().equals(Current.Victim)) {
            Current.Victim = null;
            Current.Ticks = 0;
        }
    }

    public static void rejoin(ServerPlayer User) {
        Scp4666State State = Scp4666State.get(User.server);
        if (State.phase() != Scp4666State.PhaseEnding || !State.doomed().contains(User.getUUID())) {
            return;
        }
        Scene Current = Scenes.computeIfAbsent(User.server, Key -> new Scene());
        if (!Current.Queue.contains(User.getUUID()) && !User.getUUID().equals(Current.Victim)) {
            Current.Queue.add(User.getUUID());
        }
        seize(User.serverLevel(), Scp4666Map.get(User.server), Current, User);
    }

    public static void tick(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        Scene Current = Scenes.get(World.getServer());
        if (Current == null) {
            return;
        }
        for (Map.Entry<UUID, Vec3> Entry : Current.Anchors.entrySet()) {
            ServerPlayer Victim = player(World, Entry.getKey());
            if (Victim != null && Victim.level() == World && Victim.isAlive() && Victim.position().distanceTo(Entry.getValue()) > HoldSlack) {
                place(World, Victim, Entry.getValue());
            } else if (Victim != null) {
                Victim.setDeltaMovement(0.0, Math.min(0.0, Victim.getDeltaMovement().y), 0.0);
            }
        }
        if (Current.Victim == null) {
            while (!Current.Queue.isEmpty()) {
                UUID Next = Current.Queue.removeFirst();
                ServerPlayer Victim = player(World, Next);
                if (Victim != null && Victim.level() == World && Victim.isAlive()) {
                    Current.Victim = Next;
                    Current.Ticks = 0;
                    break;
                }
            }
            if (Current.Victim == null) {
                finish(World, Current, State);
                return;
            }
        }
        ServerPlayer Victim = player(World, Current.Victim);
        if (Victim == null || Victim.level() != World || !Victim.isAlive()) {
            Current.Victim = null;
            return;
        }
        step(World, Layout, Current, Victim);
        Current.Ticks++;
    }

    private static void finish(ServerLevel World, Scene Current, Scp4666State State) {
        Entity Man = Current.ManId >= 0 ? World.getEntity(Current.ManId) : null;
        if (Man != null) {
            Man.discard();
        }
        Scenes.remove(World.getServer());
        Scp4666Pose.releaseAll(World.getServer());
        if (State.phase() == Scp4666State.PhaseEnding) {
            State.setPhase(Scp4666State.PhaseOver);
            Scp4666Director.sync(World, State);
        }
    }

    @Nullable
    private static ServerPlayer player(ServerLevel World, UUID Id) {
        return World.getPlayerByUUID(Id) instanceof ServerPlayer Found ? Found : null;
    }

    @Nullable
    private static Scp4666YuleMan man(ServerLevel World, Scene Current) {
        return Current.ManId >= 0 && World.getEntity(Current.ManId) instanceof Scp4666YuleMan Found && Found.isAlive() ? Found : null;
    }

    private static void step(ServerLevel World, Scp4666Map Layout, Scene Current, ServerPlayer Victim) {
        int Ticks = Current.Ticks;
        Vec3 Anchor = Current.Anchors.getOrDefault(Victim.getUUID(), Victim.position());
        Vec3 Middle = Vec3.atBottomCenterOf(Layout.BasementCentre);
        Scp4666YuleMan Man = man(World, Current);
        if (Ticks == 0) {
            if (Man == null) {
                Vec3 Over = Anchor.add(1.1, 0.0, 0.4);
                float Yaw = (float) (Mth.atan2(Anchor.z - Over.z, Anchor.x - Over.x) * Mth.RAD_TO_DEG) - 90.0f;
                Man = Scp4666Happenings.spawn(World, Over, Yaw);
                if (Man != null) {
                    Current.ManId = Man.getId();
                }
            }
            if (Man != null) {
                Man.loom(Victim);
            }
            Scp4666Cues.sound(World, Anchor, List.of(Scp4666Registry.Breath, Scp4666Registry.BreathLow), 0.9f, 0.6f);
            Scp4666Cues.after(World, 30, Level -> Scp4666Cues.sound(Level, Anchor, List.of(Scp4666Registry.LaughDeep), 1.0f, 1.0f));
            return;
        }
        int DragStart = LoomTicks;
        int BeatStart = DragStart + DragTicks;
        int SackStart = BeatStart + BeatTicks;
        int TakeAt = SackStart + SackTicks;
        if (Ticks == DragStart && Man != null) {
            Man.drag(Victim);
        }
        if (Ticks >= DragStart && Ticks < BeatStart) {
            double Share = (double) (Ticks - DragStart) / DragTicks;
            Vec3 Point = Anchor.lerp(Middle, Share);
            Current.Anchors.put(Victim.getUUID(), Point);
            place(World, Victim, Point);
            if (Man != null) {
                Vec3 Ahead = Point.add(Middle.subtract(Anchor).normalize().scale(1.3));
                Man.moveTo(Ahead.x, Ahead.y, Ahead.z, Man.getYRot(), 0.0f);
                Man.faceToward(Point);
            }
            if ((Ticks - DragStart) % 14 == 0) {
                Scp4666Cues.sound(World, Point, List.of(Scp4666Registry.Drag), 1.0f, 0.8f + World.getRandom().nextFloat() * 0.1f);
            }
            if (Ticks == DragStart + 12 || (Ticks == DragStart + 46 && World.getRandom().nextBoolean())) {
                Scp4666Cues.sound(World, Point, List.of(Scp4666Registry.LaughDeep), 1.1f, 0.92f + World.getRandom().nextFloat() * 0.08f);
            }
            return;
        }
        if (Ticks == BeatStart && Man != null) {
            Man.beat(Victim);
        }
        if (Ticks >= BeatStart && Ticks < SackStart) {
            int Into = Ticks - BeatStart;
            int Gap = BeatTicks / (Swings + 1);
            if (Into == Gap * 2 + Gap / 2 || (Into == Gap * 4 + Gap / 2 && World.getRandom().nextBoolean())) {
                Scp4666Cues.sound(World, Victim.position(), List.of(Scp4666Registry.LaughDeep), 1.0f, 0.95f + World.getRandom().nextFloat() * 0.1f);
            }
            if (Into > 0 && Into % Gap == 0 && Into / Gap <= Swings) {
                Scp4666Cues.sound(World, Victim.position(), List.of(Scp4666Registry.Thud, Scp4666Registry.ThudHeavy), 1.2f, 0.7f + World.getRandom().nextFloat() * 0.15f);
                PacketDistributor.sendToPlayer(Victim, new Scp4666FxPayload(Scp4666FxPayload.Thud, Victim.getX(), Victim.getY(), Victim.getZ(), Into / Gap));
                if (Victim.getHealth() > SafeHealth && !Victim.isCreative()) {
                    Victim.hurt(new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp4666Registry.Beaten)), SwingDamage);
                }
            }
            return;
        }
        if (Ticks == SackStart) {
            if (Man != null) {
                Man.lift(Victim);
            }
            Scp4666Cues.sound(World, Victim.position(), List.of(Scp4666Registry.Sack), 1.1f, 0.8f);
            Vec3 Heard = Victim.position();
            Scp4666Cues.after(World, 10, Level -> Scp4666Cues.sound(Level, Heard, List.of(Scp4666Registry.LaughDeep, Scp4666Registry.BreathLow), 1.0f, 0.9f));
            PacketDistributor.sendToPlayer(Victim, new Scp4666FxPayload(Scp4666FxPayload.Sack, Victim.getX(), Victim.getY(), Victim.getZ(), SackTicks));
            return;
        }
        if (Ticks == TakeAt) {
            Current.Anchors.remove(Victim.getUUID());
            PacketDistributor.sendToPlayer(Victim, new Scp4666FxPayload(Scp4666FxPayload.Hold, 0.0, 0.0, 0.0, 0));
            DamageSource Source = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp4666Registry.Taken));
            Victim.hurt(Source, Float.MAX_VALUE);
            if (Victim.isAlive() && !Victim.isCreative() && !Victim.isSpectator()) {
                Victim.setHealth(0.0f);
                Victim.die(Source);
            }
            if (Victim.isAlive()) {
                Scp4666Pose.lie(Victim, false);
            } else {
                Scp4666Pose.forget(Victim);
            }
            Scp4666State.get(World.getServer()).doomed().remove(Victim.getUUID());
            return;
        }
        if (Ticks > TakeAt + TakeTicks) {
            Current.Victim = null;
            Current.Ticks = 0;
            if (Man != null) {
                Man.loomIdle();
            }
        }
    }
}
