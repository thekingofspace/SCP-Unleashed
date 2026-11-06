package net.rai.scp_unleashed.scp407.gametest;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.computer.ComputerRegistry;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp407.Scp407Commands;
import net.rai.scp_unleashed.scp407.Scp407Doom;
import net.rai.scp_unleashed.scp407.Scp407Enclosure;
import net.rai.scp_unleashed.scp407.Scp407Ending;
import net.rai.scp_unleashed.scp407.Scp407Fusion;
import net.rai.scp_unleashed.scp407.Scp407Growth;
import net.rai.scp_unleashed.scp407.Scp407GrowthBlock;
import net.rai.scp_unleashed.scp407.Scp407Hearing;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import net.rai.scp_unleashed.scp407.Scp407Session;
import net.rai.scp_unleashed.scp407.Scp407Sessions;
import net.rai.scp_unleashed.scp407.Scp407Song;
import net.rai.scp_unleashed.scp407.Scp407Spawning;
import net.rai.scp_unleashed.scp407.Scp407Sprout;
import net.rai.scp_unleashed.scp407.Scp407Titan;
import net.rai.scp_unleashed.senses.SensesRegistry;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.SpeakerBlock;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;
import net.rai.scp_unleashed.speaker.SpeakerRegistry;
import net.rai.scp_unleashed.speaker.cassette.AnomalousLibrary;
import net.rai.scp_unleashed.wire.WireMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp407GameTests {
    public static final String Arena = "scp407_arena";
    public static final int RoomSize = 7;
    public static final int RoomHeight = 4;
    public static final long Seed = 407L;
    public static final int HatchLimit = 40;
    public static final int EatLimit = 600;

    private Scp407GameTests() {
    }

    static FakePlayer listener(GameTestHelper Helper, double X, double Y, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new Mortal(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        User.setGameMode(GameType.SURVIVAL);
        World.addNewPlayer(User);
        return User;
    }

    static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    static Mob dclass(GameTestHelper Helper, double X, double Y, double Z) {
        Mob Body = DClassRegistry.DClassType.get().create(Helper.getLevel());
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        Body.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Body.setNoAi(true);
        Helper.getLevel().addFreshEntity(Body);
        return Body;
    }

    static void room(GameTestHelper Helper, int MinX, int MinZ) {
        BlockState Wall = Blocks.STONE_BRICKS.defaultBlockState();
        for (int X = MinX - 1; X <= MinX + RoomSize; X++) {
            for (int Z = MinZ - 1; Z <= MinZ + RoomSize; Z++) {
                for (int Y = 2; Y <= RoomHeight + 2; Y++) {
                    boolean Shell = X < MinX || Z < MinZ || X >= MinX + RoomSize || Z >= MinZ + RoomSize || Y == RoomHeight + 2;
                    Helper.setBlock(new BlockPos(X, Y, Z), Shell ? Wall : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    static SpeakerBlockEntity speaker(GameTestHelper Helper, BlockPos Pos, Direction Facing) {
        Helper.setBlock(Pos.relative(Facing.getOpposite()), Blocks.STONE.defaultBlockState());
        Helper.setBlock(Pos, SpeakerRegistry.Speaker.get().defaultBlockState().setValue(SpeakerBlock.Facing, Facing));
        return (SpeakerBlockEntity) Helper.getLevel().getBlockEntity(Helper.absolutePos(Pos));
    }

    static Scp407Session play(GameTestHelper Helper, SpeakerBlockEntity Speaker) {
        Scp407Commands.play(Helper.getLevel(), Speaker, false);
        Scp407Session Session = Scp407Sessions.get(Helper.getLevel()).withSpeaker(Speaker.getBlockPos());
        Helper.assertTrue(Session != null, "Playing SCP-407 on a speaker did not start a playback");
        Session.reseed(Seed);
        return Session;
    }

    static Runnable within(GameTestHelper Helper, int Limit, String Message, BooleanSupplier Done) {
        long[] Began = {-1L};
        return () -> {
            if (Began[0] < 0L) {
                Began[0] = Helper.getTick();
            }
            if (Done.getAsBoolean()) {
                return;
            }
            if (Helper.getTick() - Began[0] >= Limit) {
                Helper.testInfo.fail(new GameTestAssertException(Message + " within " + Limit + " ticks"));
            }
            throw new GameTestAssertException(Message);
        };
    }

    static boolean inside(GameTestHelper Helper, Entity Found) {
        return Helper.getBounds().contains(Found.position());
    }

    static BlockPos titanSpot(GameTestHelper Helper, Scp407Session Session, int MinX, int MinZ) {
        ServerLevel World = Helper.getLevel();
        List<BlockPos> Spots = new ArrayList<>();
        for (int X = MinX; X < MinX + RoomSize; X++) {
            for (int Z = MinZ; Z < MinZ + RoomSize; Z++) {
                Spots.add(new BlockPos(X, 2, Z));
            }
        }
        BlockPos Middle = new BlockPos(MinX + RoomSize / 2 - 1, 2, MinZ + RoomSize / 2 - 1);
        Spots.sort(Comparator.comparingInt((BlockPos Spot) -> Spot.distManhattan(Middle)).thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        for (BlockPos Spot : Spots) {
            BlockPos Absolute = Helper.absolutePos(Spot);
            if (Session.Region.contains(Absolute.asLong()) && Scp407Spawning.standable(World, Absolute, Scp407Spawning.TitanHeadroom) && Scp407Spawning.vacant(World, Absolute)) {
                return Absolute;
            }
        }
        throw new GameTestAssertException("No room for the titan to rise");
    }

    static void skip(GameTestHelper Helper, Scp407Session Session, long Position) {
        Scp407Sessions.get(Helper.getLevel()).skip(Helper.getLevel(), Session, Position);
    }

    static void clean(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp407Sessions Data = Scp407Sessions.get(World);
        AABB Bounds = Helper.getBounds().inflate(1.0);
        for (Scp407Session Session : List.copyOf(Data.all())) {
            if (Bounds.contains(Session.Origin.getCenter())) {
                Data.remove(Session);
                Scp407Ending.clear(World, Session);
            }
        }
    }

    static int growthIn(GameTestHelper Helper, int MinX, int MinZ) {
        int Count = 0;
        for (int X = MinX; X < MinX + RoomSize; X++) {
            for (int Z = MinZ; Z < MinZ + RoomSize; Z++) {
                for (int Y = 2; Y <= RoomHeight + 1; Y++) {
                    if (Helper.getLevel().getBlockState(Helper.absolutePos(new BlockPos(X, Y, Z))).getBlock() instanceof Scp407GrowthBlock) {
                        Count++;
                    }
                }
            }
        }
        return Count;
    }

    static final class Mortal extends FakePlayer {
        Mortal(ServerLevel World, GameProfile Profile) {
            super(World, Profile);
        }

        @Override
        public boolean isInvulnerableTo(DamageSource Source) {
            return false;
        }

        @Override
        public void die(DamageSource Source) {
            CommonHooks.onLivingDeath(this, Source);
        }

        @Override
        public void teleportTo(double X, double Y, double Z) {
            moveTo(X, Y, Z, getYRot(), getXRot());
        }

        @Override
        public void teleportTo(ServerLevel Target, double X, double Y, double Z, float Yaw, float Pitch) {
            if (Target != level()) {
                super.teleportTo(Target, X, Y, Z, Yaw, Pitch);
                return;
            }
            stopRiding();
            moveTo(X, Y, Z, Yaw, Pitch);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp407_cassette")
    public static void cassetteInsert(GameTestHelper Helper) {
        Helper.setBlock(new BlockPos(4, 2, 4), SpeakerRegistry.SoundConsole.get().defaultBlockState());
        Helper.setBlock(new BlockPos(8, 2, 4), ComputerRegistry.Computer.get().defaultBlockState());
        FakePlayer First = listener(Helper, 4.5, 2, 6.5, "scp407_tape_a");
        FakePlayer Second = listener(Helper, 8.5, 2, 6.5, "scp407_tape_b");
        ServerLevel World = Helper.getLevel();
        for (Object[] Case : new Object[][]{{First, new BlockPos(4, 2, 4)}, {Second, new BlockPos(8, 2, 4)}}) {
            FakePlayer User = (FakePlayer) Case[0];
            BlockPos Pos = Helper.absolutePos((BlockPos) Case[1]);
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp407Registry.Cassette.get()));
            BlockState State = World.getBlockState(Pos);
            State.useItemOn(User.getItemInHand(InteractionHand.MAIN_HAND), World, User, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(Pos), Direction.SOUTH, Pos, false));
            Helper.assertTrue(User.getItemInHand(InteractionHand.MAIN_HAND).is(Scp407Registry.Cassette.get()), "Using the SCP-407 cassette on " + State.getBlock() + " used it up under the default archive rules");
            Helper.assertTrue(AnomalousLibrary.find(User, Scp407Registry.SongId).isPresent(), "The SCP-407 track was not added after using it on " + State.getBlock());
        }
        dismiss(Helper, First);
        dismiss(Helper, Second);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp407_hearing")
    public static void hearing(GameTestHelper Helper) {
        SpeakerBlockEntity Speaker = speaker(Helper, new BlockPos(4, 4, 10), Direction.EAST);
        for (int Y = 2; Y <= 5; Y++) {
            for (int Z = 13; Z <= 17; Z++) {
                Helper.setBlock(new BlockPos(9, Y, Z), Blocks.STONE.defaultBlockState());
            }
        }
        FakePlayer Seen = listener(Helper, 12.5, 2, 10.5, "scp407_seen");
        FakePlayer Hidden = listener(Helper, 12.5, 2, 15.5, "scp407_hidden");
        FakePlayer Plugged = listener(Helper, 12.5, 2, 7.5, "scp407_plugged");
        Plugged.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Earplugs.get()));
        Scp407Session Session = play(Helper, Speaker);
        Helper.startSequence().thenExecuteAfter(25, () -> {
            Helper.assertTrue(Seen.hasEffect(MobEffects.REGENERATION) && Seen.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "A player in sight within 16 blocks was not soothed");
            Helper.assertTrue(!Hidden.hasEffect(MobEffects.REGENERATION), "A player behind a wall heard the song");
            Helper.assertTrue(!Plugged.hasEffect(MobEffects.REGENERATION), "An earplugged player heard the song");
            Helper.assertTrue(Session.Listeners.containsKey(Seen.getUUID()) && !Session.Listeners.containsKey(Hidden.getUUID())
                    && !Session.Listeners.containsKey(Plugged.getUUID()), "The listener list is wrong");
            clean(Helper);
            dismiss(Helper, Seen);
            dismiss(Helper, Hidden);
            dismiss(Helper, Plugged);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp407_actChoice")
    public static void actChoice(GameTestHelper Helper) {
        room(Helper, 3, 3);
        SpeakerBlockEntity Inside = speaker(Helper, new BlockPos(3, 4, 6), Direction.EAST);
        Mob Indoor = dclass(Helper, 7.5, 2, 6.5);
        Helper.setBlock(new BlockPos(30, 2, 30), Blocks.STONE.defaultBlockState());
        SpeakerBlockEntity Outside = speaker(Helper, new BlockPos(30, 3, 30), Direction.UP);
        Mob Outdoor = dclass(Helper, 33.5, 2, 30.5);
        Scp407Enclosure.Result Probe = Scp407Enclosure.check(Helper.getLevel(), Inside.getBlockPos(), Direction.EAST);
        Helper.assertTrue(Probe.Enclosed(), "The room probe failed: " + Probe.Reason() + " after " + Probe.Cells().size() + " cells");
        Scp407Session Room = play(Helper, Inside);
        Scp407Session Field = play(Helper, Outside);
        Helper.assertTrue(Room != Field, "Speakers far apart joined one playback");
        Helper.startSequence().thenExecuteAfter(15, () -> {
            skip(Helper, Room, Scp407Sessions.ActDelay);
            skip(Helper, Field, Scp407Sessions.ActDelay);
        }).thenExecuteAfter(15, () -> {
            Helper.assertTrue(Room.Act == Scp407Session.ActGenesis, "The enclosed room did not pick the first act, got " + Room.Act);
            Helper.assertTrue(Field.Act == Scp407Session.ActWanderer, "The open field did not pick the second act, got " + Field.Act);
            BlockPos Low = Helper.absolutePos(new BlockPos(3, 2, 3));
            LongIterator Cells = Room.Region.iterator();
            while (Cells.hasNext()) {
                BlockPos Cell = BlockPos.of(Cells.nextLong());
                Helper.assertTrue(Cell.getX() >= Low.getX() && Cell.getX() < Low.getX() + RoomSize && Cell.getZ() >= Low.getZ() && Cell.getZ() < Low.getZ() + RoomSize
                        && Cell.getY() >= Low.getY() && Cell.getY() < Low.getY() + RoomHeight, "The room region leaked to " + Cell);
            }
            Helper.assertTrue(Room.Region.size() >= RoomSize * RoomSize * RoomHeight - 4, "The room region is too small: " + Room.Region.size());
            clean(Helper);
            Indoor.discard();
            Outdoor.discard();
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp407_growth")
    public static void growthStaysInside(GameTestHelper Helper) {
        room(Helper, 3, 3);
        Helper.setBlock(new BlockPos(5, 2, 5), Blocks.OAK_PLANKS.defaultBlockState());
        Helper.setBlock(new BlockPos(6, 2, 8), Blocks.CHEST.defaultBlockState());
        Helper.setBlock(new BlockPos(8, 1, 4), Blocks.DIRT.defaultBlockState());
        Helper.setBlock(new BlockPos(8, 2, 4), Blocks.SHORT_GRASS.defaultBlockState());
        for (int X = 0; X <= 14; X++) {
            Helper.setBlock(new BlockPos(X, 1, 12), Blocks.DIRT.defaultBlockState());
            Helper.setBlock(new BlockPos(X, 2, 12), Blocks.SHORT_GRASS.defaultBlockState());
        }
        SpeakerBlockEntity Speaker = speaker(Helper, new BlockPos(3, 4, 6), Direction.EAST);
        Mob Body = dclass(Helper, 7.5, 2, 6.5);
        Scp407Session Session = play(Helper, Speaker);
        Helper.startSequence().thenExecuteAfter(12, () -> skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            skip(Helper, Session, Session.ActTick + 3000);
        }).thenExecuteAfter(160, () -> {
            Helper.assertTrue(Session.Spread > 40, "Too little grew: " + Session.Spread);
            LongIterator Cells = Session.Grown.iterator();
            while (Cells.hasNext()) {
                long Key = Cells.nextLong();
                Helper.assertTrue(Session.Region.contains(Key), "Growth was placed outside the room at " + BlockPos.of(Key));
            }
            Helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(5, 2, 5));
            Helper.assertBlockPresent(Blocks.CHEST, new BlockPos(6, 2, 8));
            for (int X = 0; X <= 14; X++) {
                Helper.assertBlockPresent(Blocks.SHORT_GRASS, new BlockPos(X, 2, 12));
            }
            Helper.assertTrue(growthIn(Helper, 3, 3) == Session.Grown.size(), "Tracked growth does not match the blocks in the room");
            clean(Helper);
            Helper.assertTrue(growthIn(Helper, 3, 3) == 0, "Clearing left growth behind");
            Helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(5, 2, 5));
            Body.discard();
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp407_fused")
    public static void fusedCannotBeHurt(GameTestHelper Helper) {
        room(Helper, 3, 3);
        SpeakerBlockEntity Speaker = speaker(Helper, new BlockPos(3, 4, 6), Direction.EAST);
        FakePlayer User = listener(Helper, 7.5, 2, 6.5, "scp407_fused");
        Scp407Session Session = play(Helper, Speaker);
        Helper.startSequence().thenExecuteAfter(12, () -> skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            skip(Helper, Session, Session.ActTick + 600 + 5);
        }).thenWaitUntil(within(Helper, 120, "The listener was not enveloped", () -> {
            Scp407Session.Listener Entry = Session.Listeners.get(User.getUUID());
            return Entry != null && Entry.Fused && Entry.PodStage >= 3;
        })).thenExecute(() -> {
            float Before = User.getHealth();
            boolean Hurt = User.hurt(User.damageSources().generic(), 1000.0f);
            Helper.assertTrue(!Hurt && User.isAlive() && User.getHealth() >= Before, "An enveloped listener took damage");
            Helper.assertTrue(Scp407Fusion.isHeld(User), "The enveloped listener is not held in place");
            BlockPos Feet = User.blockPosition();
            int Pods = 0;
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                if (Helper.getLevel().getBlockState(Feet.relative(Side)).is(Scp407Registry.Pod.get())) {
                    Pods++;
                }
            }
            Helper.assertTrue(Pods >= 3, "The pod did not close around the listener, only " + Pods + " sides");
            clean(Helper);
            Helper.assertTrue(!Scp407Fusion.isHeld(User), "Clearing the playback left the listener held");
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 800, batch = "scp407_creatures")
    public static void sproutsHatchAndTitanEats(GameTestHelper Helper) {
        room(Helper, 3, 3);
        SpeakerBlockEntity Speaker = speaker(Helper, new BlockPos(2, 4, 6), Direction.EAST);
        Mob Body = dclass(Helper, 7.5, 2, 6.5);
        Scp407Session Session = play(Helper, Speaker);
        ServerLevel World = Helper.getLevel();
        Helper.startSequence().thenExecuteAfter(12, () -> skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            Body.discard();
            for (int Z = 4; Z <= 8; Z += 2) {
                BlockPos Bloom = Helper.absolutePos(new BlockPos(9, 2, Z));
                Helper.assertTrue(Scp407Growth.place(World, Session, Bloom, Scp407Registry.Bloom.get().defaultBlockState()), "A bloom could not grow at " + Bloom);
            }
            skip(Helper, Session, Session.ActTick + Scp407Spawning.SproutDelay + 1);
            Session.NextSprout = -1L;
        }).thenWaitUntil(within(Helper, HatchLimit, "No sprout hatched from the blooms", () -> Scp407Spawning.alive(World, Session, Scp407Sprout.class) >= 1)).thenExecute(() -> {
            for (UUID Id : Session.Creatures) {
                Entity Found = World.getEntity(Id);
                Helper.assertTrue(Found != null && inside(Helper, Found) && Session.Region.contains(Found.blockPosition().asLong()), "A sprout hatched outside the room");
            }
            Scp407Titan Titan = Scp407Spawning.raise(World, Session, titanSpot(Helper, Session, 3, 3));
            Helper.assertTrue(Titan != null, "The titan could not be raised in the room");
        }).thenWaitUntil(within(Helper, EatLimit, "The titan did not eat a sprout", () -> World.getEntity(Session.Titan) instanceof Scp407Titan Titan && Titan.eaten() >= 1)).thenExecute(() -> {
            for (UUID Id : Session.Creatures) {
                Entity Found = World.getEntity(Id);
                Helper.assertTrue(Found == null || inside(Helper, Found), "A creature left the test area");
            }
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp407_ending")
    public static void stoppingWithersEverything(GameTestHelper Helper) {
        room(Helper, 3, 3);
        SpeakerBlockEntity Speaker = speaker(Helper, new BlockPos(3, 4, 6), Direction.EAST);
        Mob Body = dclass(Helper, 7.5, 2, 6.5);
        FakePlayer User = listener(Helper, 6.5, 2, 4.5, "scp407_doomed");
        Scp407Session Session = play(Helper, Speaker);
        ServerLevel World = Helper.getLevel();
        UUID[] Creatures = new UUID[2];
        Helper.startSequence().thenExecuteAfter(12, () -> skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            skip(Helper, Session, Session.ActTick + 3000);
        }).thenExecuteAfter(60, () -> {
            Scp407Sprout Sprout = Scp407Spawning.hatch(World, Session, Helper.absolutePos(new BlockPos(8, 2, 8)), null);
            Scp407Titan Titan = Scp407Spawning.raise(World, Session, Helper.absolutePos(new BlockPos(5, 2, 8)));
            Helper.assertTrue(Sprout != null && Titan != null, "Could not add creatures to the playback");
            Creatures[0] = Sprout.getUUID();
            Creatures[1] = Titan.getUUID();
            Helper.assertTrue(Session.Grown.size() > 10, "Too little growth before stopping");
            Speaker.receiveMessage(new WireMessage(Speaker.getChannel(), SoundMessages.stopPayload(), Speaker.getBlockPos()));
            Helper.assertTrue(!Session.isPlaying(), "Stopping the speaker did not end the song");
        }).thenWaitUntil(within(Helper, 300, "The withering did not finish", () -> Scp407Sessions.get(World).byId(Session.Id) == null)).thenExecute(() -> {
            Helper.assertTrue(growthIn(Helper, 3, 3) == 0, "Growth blocks remain after the song ended");
            Helper.assertTrue(World.getEntity(Creatures[0]) == null && World.getEntity(Creatures[1]) == null, "A creature outlived the song");
            Helper.assertTrue(!Body.isAlive(), "The D-Class listener survived the end of the first act");
            Helper.assertTrue(!User.isAlive(), "The player listener survived the end of the first act");
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp407_wanderer")
    public static void wandererOnly(GameTestHelper Helper) {
        Helper.setBlock(new BlockPos(20, 2, 20), Blocks.STONE.defaultBlockState());
        SpeakerBlockEntity Speaker = speaker(Helper, new BlockPos(20, 3, 20), Direction.UP);
        Mob Body = dclass(Helper, 26.5, 2, 20.5);
        Scp407Session Session = play(Helper, Speaker);
        ServerLevel World = Helper.getLevel();
        UUID[] Titan = new UUID[1];
        Helper.startSequence().thenExecuteAfter(12, () -> skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.Act == Scp407Session.ActWanderer, "The open field did not pick the second act");
            skip(Helper, Session, Session.ActTick + Scp407Spawning.WandererDelay + 1);
        }).thenWaitUntil(within(Helper, 160, "The titan did not appear", () -> Session.TitanSpawned)).thenExecute(() -> {
            Titan[0] = Session.Titan;
            Helper.assertTrue(Session.Grown.isEmpty(), "Growth appeared in the open field");
            Helper.assertTrue(Scp407Spawning.alive(World, Session, Scp407Sprout.class) == 0, "Sprouts appeared in the open field");
            Scp407Ending.begin(World, Session);
        }).thenWaitUntil(within(Helper, 160, "The titan did not wither", () -> World.getEntity(Titan[0]) == null)).thenExecute(() -> {
            Helper.assertTrue(Body.isAlive(), "A second act listener died");
            Body.discard();
            clean(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp407_persistence")
    public static void restartThenEnd(GameTestHelper Helper) {
        room(Helper, 3, 3);
        SpeakerBlockEntity Speaker = speaker(Helper, new BlockPos(3, 4, 6), Direction.EAST);
        Mob Body = dclass(Helper, 7.5, 2, 6.5);
        Scp407Session Session = play(Helper, Speaker);
        ServerLevel World = Helper.getLevel();
        UUID Absent = UUID.randomUUID();
        Helper.startSequence().thenExecuteAfter(12, () -> skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            skip(Helper, Session, Session.ActTick + 3000);
        }).thenExecuteAfter(60, () -> {
            Session.Listeners.put(Absent, new Scp407Session.Listener(Absent, "away", false, 0L));
            Scp407Sessions Data = Scp407Sessions.get(World);
            CompoundTag Saved = Data.save(new CompoundTag(), World.registryAccess());
            Scp407Sessions Loaded = Scp407Sessions.load(Saved, World.registryAccess());
            Scp407Session Restored = Loaded.byId(Session.Id);
            Helper.assertTrue(Restored != null && Restored.Grown.size() == Session.Grown.size() && Restored.Listeners.size() == Session.Listeners.size(),
                    "The playback did not survive a save and load");
            Data.remove(Session);
            Data.adopt(Restored);
            Restored.StartTick = World.getGameTime() - Scp407Song.Length - 40L;
        }).thenWaitUntil(within(Helper, 220, "The reloaded playback did not wither", () -> growthIn(Helper, 3, 3) == 0 && !Body.isAlive())).thenExecute(() -> {
            Helper.assertTrue(Scp407Doom.get(World.getServer()).isPending(Absent), "An absent listener was not remembered");
            Mob Late = DClassRegistry.DClassType.get().create(World);
            Late.setUUID(Absent);
            Vec3 Spot = Helper.absoluteVec(new Vec3(10.5, 2, 10.5));
            Late.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
            Late.setNoAi(true);
            World.addFreshEntity(Late);
        }).thenWaitUntil(within(Helper, 100, "The absent listener did not wither when it returned", () -> {
            Entity Late = World.getEntity(Absent);
            return Late == null || !Late.isAlive();
        })).thenExecute(() -> Helper.assertTrue(!Scp407Doom.get(World.getServer()).isPending(Absent), "The returned listener is still pending")).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp407_control")
    public static void controlledDClassDiesOnce(GameTestHelper Helper) {
        room(Helper, 3, 3);
        SpeakerBlockEntity Speaker = speaker(Helper, new BlockPos(3, 4, 6), Direction.EAST);
        Mob Body = dclass(Helper, 7.5, 2, 6.5);
        FakePlayer User = listener(Helper, 7.5, 2, 7.5, "scp407_pilot");
        ServerLevel World = Helper.getLevel();
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(ControlManager.possess(User, Body), "The test player could not take control of the D-Class");
        Helper.assertTrue(Scp407Hearing.keyOf(User).equals(BodyId), "A controlled body is not keyed by its own id");
        Scp407Session Session = play(Helper, Speaker);
        Helper.startSequence().thenExecuteAfter(12, () -> skip(Helper, Session, Scp407Sessions.ActDelay)).thenExecuteAfter(4, () -> {
            Helper.assertTrue(Session.isGenesis(), "The room did not start the first act");
            Helper.assertTrue(Session.Listeners.containsKey(BodyId) && !Session.Listeners.containsKey(User.getUUID()), "The controlled body was not recorded as the listener");
            Scp407Ending.begin(World, Session);
        }).thenWaitUntil(within(Helper, 200, "The listeners were not struck", () -> Session.Struck)).thenExecute(() -> {
            if (ControlManager.hasPendingDeath(User)) {
                ControlManager.processDeath(User);
            }
        }).thenExecuteAfter(5, () -> {
            Helper.assertTrue(!ControlSessions.isControlling(User), "The pilot is still in the body after it withered");
            Helper.assertTrue(User.isAlive(), "The pilot died along with the body");
            Entity Left = World.getEntity(BodyId);
            Helper.assertTrue(Left == null || !Left.isAlive(), "The controlled D-Class body survived");
            Scp407Session.Listener Entry = Session.Listeners.get(BodyId);
            Helper.assertTrue(Entry != null && Entry.Struck, "The body was not marked as struck");
            Helper.assertTrue(!Scp407Doom.get(World.getServer()).isPending(BodyId) && !Scp407Doom.get(World.getServer()).isPending(User.getUUID()),
                    "A strike was left pending, so it could land twice");
            clean(Helper);
            dismiss(Helper, User);
        }).thenSucceed();
    }
}
