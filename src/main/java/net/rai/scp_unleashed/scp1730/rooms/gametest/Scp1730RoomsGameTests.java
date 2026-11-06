package net.rai.scp_unleashed.scp1730.rooms.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.integration.voicechat.VoiceGate;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.gametest.Scp1730GameTests;
import net.rai.scp_unleashed.scp1730.plan.Scp1730AnomalousCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730AnomalousRooms;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.scp1730.plan.Scp1730PlanCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730ReachCheck;
import net.rai.scp_unleashed.scp1730.plan.Scp1730RoomInfo;
import net.rai.scp_unleashed.scp1730.plan.Scp1730SupportCheck;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730EchoTrack;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730RoomKind;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730RoomLooks;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730RoomRules;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730RoomsPayload;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730RoomsRegistry;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730SpecialRoom;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730SpecialRooms;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730TelevisionBlock;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730TelevisionBlockEntity;
import net.rai.scp_unleashed.scp939.Scp939;
import net.rai.scp_unleashed.scp939.Scp939Registry;
import net.rai.scp_unleashed.scp939.Scp939Senses;
import net.rai.scp_unleashed.senses.SensesRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp1730RoomsGameTests {
    public static final String Arena = Scp1730GameTests.Arena;

    private Scp1730RoomsGameTests() {
    }

    static Scp1730SpecialRoom room(GameTestHelper Helper, Scp1730RoomKind Kind, int Floor, int MinX, int MinZ, int MaxX, int MaxZ, int Height, int[]... Doors) {
        BlockPos Low = Helper.absolutePos(new BlockPos(MinX, Floor, MinZ));
        List<Scp1730SpecialRoom.Door> Made = new ArrayList<>();
        for (int[] Door : Doors) {
            BlockPos At = Helper.absolutePos(new BlockPos(Door[0], 0, Door[1]));
            Made.add(new Scp1730SpecialRoom.Door(At.getX(), At.getZ(), Door[2]));
        }
        return new Scp1730SpecialRoom(Kind, Low.getX(), Low.getZ(), Low.getX() + MaxX - MinX, Low.getZ() + MaxZ - MinZ, Low.getY(), Height, List.copyOf(Made));
    }

    static ServerPlayer player(GameTestHelper Helper, Vec3 Relative, String Name) {
        return Scp1730GameTests.player(Helper, Helper.getLevel(), Helper.absoluteVec(Relative), Name);
    }

    static void walls(GameTestHelper Helper, int MinX, int MinZ, int MaxX, int MaxZ, int Height, int DoorX, int DoorZ, boolean AlongX) {
        for (int X = MinX; X <= MaxX; X++) {
            for (int Z = MinZ; Z <= MaxZ; Z++) {
                boolean Edge = X == MinX || X == MaxX || Z == MinZ || Z == MaxZ;
                Helper.setBlock(new BlockPos(X, 0, Z), Blocks.STONE);
                Helper.setBlock(new BlockPos(X, Height + 1, Z), Blocks.STONE);
                for (int Y = 1; Y <= Height; Y++) {
                    boolean Gap = AlongX ? Math.abs(X - DoorX) <= 1 && Z == DoorZ : Math.abs(Z - DoorZ) <= 1 && X == DoorX;
                    Helper.setBlock(new BlockPos(X, Y, Z), Edge && !(Gap && Y <= 3) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp1730rooms_placement")
    public static void placement(GameTestHelper Helper) {
        int Seeds = 160;
        int None = 0;
        int Most = 0;
        Map<String, Integer> Kinds = new HashMap<>();
        for (long Seed = 5000L; Seed < 5000L + Seeds; Seed++) {
            Scp1730Plan Plan = Scp1730Plan.fresh(Seed);
            List<String> Problems = Scp1730AnomalousCheck.check(Plan);
            Helper.assertTrue(Problems.isEmpty(), "seed " + Seed + " anomalous room problems: " + Problems);
            int Count = 0;
            for (Scp1730RoomInfo Room : Plan.rooms()) {
                if (Scp1730AnomalousRooms.Kinds.contains(Room.Type())) {
                    Count++;
                    Kinds.merge(Room.Type(), 1, Integer::sum);
                    Helper.assertTrue(Room.Zone() < 3, "an anomalous room was placed in the deep zone");
                }
            }
            None += Count == 0 ? 1 : 0;
            Most = Math.max(Most, Count);
        }
        Helper.assertTrue(Most <= Scp1730AnomalousRooms.MaxRooms, "a run held " + Most + " anomalous rooms");
        Helper.assertTrue(None >= Seeds / 20 && None <= Seeds * 9 / 20, "runs without anomalous rooms: " + None + " of " + Seeds);
        for (String Kind : Scp1730AnomalousRooms.Kinds) {
            Helper.assertTrue(Kinds.getOrDefault(Kind, 0) > 0, "the " + Kind + " room never appeared in " + Seeds + " runs");
            Helper.assertTrue(Kinds.getOrDefault(Kind, 0) < Seeds * 3 / 4, "the " + Kind + " room is too common: " + Kinds.get(Kind));
        }
        Helper.succeed();
    }

    static final java.util.Map<String, boolean[]> Feelings = new java.util.concurrent.ConcurrentHashMap<>();

    static boolean[] feel(String Text) {
        return Feelings.computeIfAbsent(Text, Key -> {
            net.minecraft.world.level.block.state.BlockState State = Scp1730World.state(Key);
            net.minecraft.world.phys.shapes.VoxelShape Shape = State.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            if (Shape.isEmpty()) {
                return new boolean[]{true, false, false};
            }
            net.minecraft.world.phys.AABB Body = new net.minecraft.world.phys.AABB(0.3, 0.1, 0.3, 0.7, 0.8, 0.7);
            net.minecraft.world.phys.AABB Column = new net.minecraft.world.phys.AABB(0.3, 0.0, 0.3, 0.7, 1.0, 0.7);
            boolean Blocks = false;
            double Top = 0.0;
            for (net.minecraft.world.phys.AABB Part : Shape.toAabbs()) {
                if (Part.intersects(Body)) {
                    Blocks = true;
                }
                if (Part.intersects(Column)) {
                    Top = Math.max(Top, Part.maxY);
                }
            }
            boolean Open = !Blocks;
            return new boolean[]{Open, !Open && Top <= 0.6, Top >= 0.9};
        });
    }

    public static final Scp1730ReachCheck.Feel Collision = new Scp1730ReachCheck.Feel() {
        @Override
        public boolean open(String State) {
            return State == null || feel(State)[0];
        }

        @Override
        public boolean low(String State) {
            return State != null && feel(State)[1];
        }

        @Override
        public boolean floor(String State) {
            return State != null && feel(State)[2];
        }
    };

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "scp1730rooms_seedSweep")
    public static void seedSweep(GameTestHelper Helper) {
        String Wanted = System.getenv("SCP1730ROOMS_SWEEP");
        int Seeds = Wanted == null ? 240 : Integer.parseInt(Wanted.trim());
        long[] Next = {20000L};
        long End = 20000L + Seeds;
        List<String> Problems = new java.util.concurrent.CopyOnWriteArrayList<>();
        Helper.onEachTick(() -> {
            for (int Step = 0; Step < 8 && Next[0] < End; Step++) {
                long Seed = Next[0]++;
                Scp1730Plan Plan = Scp1730Plan.fresh(Seed);
                for (String Each : Scp1730SupportCheck.check(Plan)) {
                    Problems.add("seed " + Seed + " support: " + Each);
                }
                for (String Each : Scp1730AnomalousCheck.check(Plan)) {
                    Problems.add("seed " + Seed + " anomalous: " + Each);
                }
                for (String Each : Scp1730ReachCheck.check(Plan, Collision)) {
                    Problems.add("seed " + Seed + " reach: " + Each);
                }
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Next[0] >= End, "sweep still running at seed " + Next[0]);
            Helper.assertTrue(Problems.isEmpty(), Problems.size() + " problems over " + Seeds + " seeds, e.g. " + Problems.subList(0, Math.min(4, Problems.size())));
            com.mojang.logging.LogUtils.getLogger().info("scp1730rooms seed sweep: {} seeds, no problems", Seeds);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp1730rooms_planCheck")
    public static void planCheck(GameTestHelper Helper) {
        int Checked = 0;
        for (long Seed = 9100L; Seed < 9400L && Checked < 12; Seed++) {
            Scp1730Plan Plan = Scp1730Plan.fresh(Seed);
            boolean Strange = Plan.rooms().stream().anyMatch(Room -> Scp1730AnomalousRooms.Kinds.contains(Room.Type()));
            if (!Strange) {
                continue;
            }
            List<String> Problems = Scp1730PlanCheck.check(Seed);
            Helper.assertTrue(Problems.isEmpty(), "plan check failed for seed " + Seed + ": " + Problems);
            Checked++;
        }
        Helper.assertTrue(Checked >= 8, "too few seeds held anomalous rooms: " + Checked);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp1730rooms_vaultPocket")
    public static void vaultPocket(GameTestHelper Helper) {
        int Halls = 0;
        for (long Seed = 300L; Seed < 700L && Halls < 6; Seed++) {
            Scp1730Plan Plan = Scp1730Plan.fresh(Seed);
            Scp1730RoomInfo Hall = null;
            Scp1730RoomInfo Small = null;
            for (Scp1730RoomInfo Room : Plan.rooms()) {
                if (Room.Type().equals(Scp1730AnomalousRooms.VaultHall)) {
                    Hall = Room;
                } else if (Room.Type().equals(Scp1730AnomalousRooms.Vault)) {
                    Small = Room;
                }
            }
            if (Small == null) {
                continue;
            }
            Helper.assertTrue(Hall != null, "seed " + Seed + " has a vault room without its hall");
            Helper.assertTrue(Scp1730AnomalousCheck.check(Plan).isEmpty(), "seed " + Seed + " vault pocket problems: " + Scp1730AnomalousCheck.check(Plan));
            Helper.assertTrue((Hall.MaxX() - Hall.MinX() + 1) * (Hall.MaxZ() - Hall.MinZ() + 1) > 4 * (Small.MaxX() - Small.MinX() + 1) * (Small.MaxZ() - Small.MinZ() + 1),
                    "the hall is not much bigger than the room it hides in");
            Scp1730SpecialRoom.Door Outer = new Scp1730SpecialRoom.Door(Small.Doors().getFirst()[0], Small.Doors().getFirst()[1], Small.Doors().getFirst()[2]);
            Scp1730SpecialRoom.Door Inner = new Scp1730SpecialRoom.Door(Hall.Doors().getFirst()[0], Hall.Doors().getFirst()[1], Hall.Doors().getFirst()[2]);
            int In = Math.floorMod(Inner.Dir() - Outer.Dir(), 4);
            int Out = Math.floorMod(Outer.Dir() - Inner.Dir(), 4);
            Vec3 Start = new Vec3(Outer.X() + 0.5 - Outer.outX() * 0.2 + Outer.sideX() * 0.7, Small.Floor() + 1, Outer.Z() + 0.5 - Outer.outZ() * 0.2 + Outer.sideZ() * 0.7);
            Vec3 There = Scp1730RoomRules.map(Start, Outer, Inner, Small.Floor(), Hall.Floor(), In);
            Vec3 Back = Scp1730RoomRules.map(There, Inner, Outer, Hall.Floor(), Small.Floor(), Out);
            Helper.assertTrue(Back.distanceTo(Start) < 1.0e-6, "the vault doorway does not map back onto itself");
            Helper.assertTrue(Inner.outward(There) < 0.0 && Math.abs(There.y - (Hall.Floor() + 1)) < 1.0e-6, "stepping in does not land inside the hall doorway");
            Halls++;
        }
        Helper.assertTrue(Halls >= 3, "too few vault rooms to check: " + Halls);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp1730rooms_serverTint")
    public static void serverTint(GameTestHelper Helper) {
        List<Scp1730SpecialRoom> Rooms = Scp1730SpecialRooms.forRun(3);
        Scp1730SpecialRoom Server = null;
        for (Scp1730SpecialRoom Each : Rooms) {
            if (Each.Kind() == Scp1730RoomKind.Server) {
                Server = Each;
            }
        }
        Helper.assertTrue(Server != null, "run 3 lists no server room for the client");
        Vec3 Middle = Server.centre().add(0.0, 1.0, 0.0);
        Helper.assertTrue(Server.contains(Middle), "the server room centre does not resolve as inside");
        Helper.assertTrue(!Server.contains(Middle.add(Server.MaxX() - Server.MinX() + 4, 0.0, 0.0)), "a point past the server room resolves as inside");
        Helper.assertTrue(!Server.contains(Middle.add(0.0, 12.0, 0.0)), "a point above the server room resolves as inside");
        FriendlyByteBuf Buffer = new FriendlyByteBuf(Unpooled.buffer());
        Scp1730RoomsPayload Sent = new Scp1730RoomsPayload(Scp1730World.Key.location().toString(), Rooms);
        Scp1730RoomsPayload.PayloadCodec.encode(Buffer, Sent);
        Scp1730RoomsPayload Read = Scp1730RoomsPayload.PayloadCodec.decode(Buffer);
        Helper.assertTrue(Read.Rooms().equals(Rooms), "the synced room list did not survive the trip to the client");
        float[] Inside = Scp1730RoomLooks.fog(0.0f, 192.0f, 1.0f, 0.0f, 0.0f);
        float[] Outside = Scp1730RoomLooks.fog(0.0f, 192.0f, 0.0f, 0.0f, 0.0f);
        Helper.assertTrue(Inside[1] <= Scp1730RoomLooks.ServerFar && Outside[1] == 192.0f, "the server room fog does not resolve inside only");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp1730rooms_lightClamp")
    public static void lightClamp(GameTestHelper Helper) {
        for (float Far : new float[]{4.0f, 16.0f, 64.0f, 512.0f}) {
            float[] Dark = Scp1730RoomLooks.fog(Far * 0.5f, Far, 0.0f, 0.0f, 1.0f);
            Helper.assertTrue(Dark[1] <= Scp1730RoomLooks.DarkFar + 1.0e-4f, "light reached " + Dark[1] + " blocks inside the lightless room");
            Helper.assertTrue(Dark[0] <= Dark[1], "the fog starts behind its end");
            float[] Plain = Scp1730RoomLooks.fog(Far * 0.5f, Far, 0.0f, 0.0f, 0.0f);
            Helper.assertTrue(Plain[1] == Far, "the clamp lingers outside the lightless room");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp1730rooms_silentSounds")
    public static void silentSounds(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730SpecialRoom Room = room(Helper, Scp1730RoomKind.Silent, 1, 2, 2, 12, 12, 3, new int[]{12, 7, 1});
        Scp1730SpecialRooms.pin(World, Room);
        try {
            Vec3 In = Helper.absoluteVec(new Vec3(7.5, 2.5, 7.5));
            Vec3 Out = Helper.absoluteVec(new Vec3(20.5, 2.5, 7.5));
            Holder<SoundEvent> Sound = SoundEvents.GENERIC_EXPLODE;
            PlayLevelSoundEvent.AtPosition Inside = EventHooks.onPlaySoundAtPosition(World, In.x, In.y, In.z, Sound, SoundSource.BLOCKS, 1.0f, 1.0f);
            PlayLevelSoundEvent.AtPosition Outside = EventHooks.onPlaySoundAtPosition(World, Out.x, Out.y, Out.z, Sound, SoundSource.BLOCKS, 1.0f, 1.0f);
            Helper.assertTrue(Inside.isCanceled(), "a sound inside the silent room still carried");
            Helper.assertTrue(!Outside.isCanceled(), "a sound outside the silent room was swallowed");
            VanillaGameEvent Step = NeoForge.EVENT_BUS.post(new VanillaGameEvent(World, GameEvent.STEP, In, GameEvent.Context.of(Blocks.STONE.defaultBlockState())));
            VanillaGameEvent Far = NeoForge.EVENT_BUS.post(new VanillaGameEvent(World, GameEvent.STEP, Out, GameEvent.Context.of(Blocks.STONE.defaultBlockState())));
            Helper.assertTrue(Step.isCanceled() && !Far.isCanceled(), "vibrations inside the silent room are not suppressed");
        } finally {
            Scp1730SpecialRooms.unpin(World, Room);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1730rooms_silentHearing")
    public static void silentHearing(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730SpecialRoom Room = room(Helper, Scp1730RoomKind.Silent, 1, 2, 2, 12, 12, 3, new int[]{12, 7, 1});
        Scp1730SpecialRooms.pin(World, Room);
        Scp939 Hunter = Scp939Registry.Scp939Type.get().create(World);
        Vec3 Lair = Helper.absoluteVec(new Vec3(7.5, 2.0, 25.5));
        Hunter.moveTo(Lair.x, Lair.y, Lair.z, 0.0f, 0.0f);
        Hunter.setNoAi(true);
        Hunter.setPersistenceRequired();
        World.addFreshEntity(Hunter);
        ServerPlayer User = player(Helper, new Vec3(7.5, 2.0, 9.5), "scp1730rooms_runner");
        User.setSprinting(true);
        Helper.runAfterDelay(3, () -> {
            try {
                Helper.assertTrue(Scp1730SpecialRooms.silenced(User), "the runner is not counted as inside the silent room");
                Helper.assertTrue(!Scp939Senses.senses(World, Hunter, User), "SCP-939 heard a sprinting player inside the silent room");
                Helper.assertTrue(VoiceGate.isSilenced(User.getUUID()) && VoiceGate.isDeafened(User.getUUID()), "voice chat is not gated inside the silent room");
                Vec3 Outside = Helper.absoluteVec(new Vec3(7.5, 2.0, 13.6));
                User.teleportTo(Outside.x, Outside.y, Outside.z);
                User.setSprinting(true);
                Helper.runAfterDelay(3, () -> {
                    try {
                        Helper.assertTrue(!VoiceGate.isSilenced(User.getUUID()) && !VoiceGate.isDeafened(User.getUUID()), "voice chat stayed gated after leaving the silent room");
                        if (!Scp939Senses.bright(World, User)) {
                            Helper.assertTrue(Scp939Senses.senses(World, Hunter, User), "SCP-939 could not hear the runner outside the silent room");
                        }
                        Helper.succeed();
                    } finally {
                        cleanup(World, Room, User);
                        Hunter.discard();
                    }
                });
            } catch (RuntimeException Problem) {
                cleanup(World, Room, User);
                Hunter.discard();
                throw Problem;
            }
        });
    }

    static void cleanup(ServerLevel World, Scp1730SpecialRoom Room, ServerPlayer User) {
        Scp1730SpecialRooms.unpin(World, Room);
        Scp1730RoomRules.forget(User);
        Scp1730GameTests.dismiss(User);
    }

    static Scp1730TelevisionBlockEntity television(GameTestHelper Helper) {
        walls(Helper, 4, 4, 14, 14, 3, 9, 14, true);
        Helper.setBlock(new BlockPos(9, 1, 5), Blocks.OAK_PLANKS);
        Helper.setBlock(new BlockPos(9, 2, 5), Scp1730RoomsRegistry.Television.get().defaultBlockState().setValue(Scp1730TelevisionBlock.Facing, Direction.SOUTH));
        Scp1730TelevisionBlockEntity Entity = (Scp1730TelevisionBlockEntity) Helper.getBlockEntity(new BlockPos(9, 2, 5));
        Entity.override(room(Helper, Scp1730RoomKind.Television, 0, 4, 4, 14, 14, 3, new int[]{9, 14, 2}));
        return Entity;
    }

    static void lookAt(ServerPlayer User, Scp1730TelevisionBlockEntity Entity) {
        Scp1730GameTests.look(User, Entity.screen());
    }

    static void lookAway(ServerPlayer User) {
        Scp1730GameTests.look(User, User.getEyePosition().add(0.0, 0.0, 6.0));
    }

    static boolean sealed(GameTestHelper Helper) {
        boolean All = true;
        for (int X = 8; X <= 10; X++) {
            for (int Y = 1; Y <= 3; Y++) {
                All &= Helper.getLevel().getBlockState(Helper.absolutePos(new BlockPos(X, Y, 14))).is(Scp1730Registry.Seal.get());
            }
        }
        return All;
    }

    static boolean open(GameTestHelper Helper) {
        boolean All = true;
        for (int X = 8; X <= 10; X++) {
            for (int Y = 1; Y <= 3; Y++) {
                All &= Helper.getLevel().getBlockState(Helper.absolutePos(new BlockPos(X, Y, 14))).isAir();
            }
        }
        return All;
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1730rooms_tvSeals")
    public static void tvSeals(GameTestHelper Helper) {
        Scp1730TelevisionBlockEntity Entity = television(Helper);
        Helper.assertTrue(open(Helper), "the doorway started closed");
        ServerPlayer User = player(Helper, new Vec3(9.5, 1.0, 10.5), "scp1730rooms_guest");
        lookAway(User);
        Helper.runAfterDelay(10, () -> {
            try {
                Helper.assertTrue(Entity.phase() != Scp1730TelevisionBlockEntity.Idle, "stepping inside did not start the television");
                Helper.assertTrue(sealed(Helper), "the doorway did not seal");
                Helper.assertTrue(Entity.getBlockState().getValue(Scp1730TelevisionBlock.ScreenState) != Scp1730TelevisionBlock.Screen.Off, "the screen stayed dark");
                Entity.forceFinish(Helper.getLevel());
                Helper.assertTrue(open(Helper), "the seals did not lift when the show ended");
                Helper.succeed();
            } finally {
                Scp1730GameTests.dismiss(User);
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp1730rooms_tvKills")
    public static void tvKills(GameTestHelper Helper) {
        Scp1730TelevisionBlockEntity Entity = television(Helper);
        ServerPlayer User = player(Helper, new Vec3(9.5, 1.0, 10.5), "scp1730rooms_viewer");
        lookAt(User, Entity);
        int Due = Scp1730TelevisionBlockEntity.StaticTicks + Scp1730TelevisionBlockEntity.GraceTicks + Scp1730TelevisionBlockEntity.LethalTicks + 14;
        Helper.runAfterDelay(Scp1730TelevisionBlockEntity.StaticTicks + Scp1730TelevisionBlockEntity.GraceTicks - 4, () -> {
            Helper.assertTrue(User.isAlive(), "the viewer died before the grace ran out");
        });
        Helper.runAfterDelay(Due, () -> {
            try {
                Helper.assertTrue(User.isDeadOrDying(), "watching the show did not kill, watched " + Entity.watched(User) + " phase " + Entity.phase());
                Helper.succeed();
            } finally {
                Entity.forceFinish(Helper.getLevel());
                Scp1730GameTests.dismiss(User);
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 760, batch = "scp1730rooms_tvAway")
    public static void tvAway(GameTestHelper Helper) {
        Scp1730TelevisionBlockEntity Entity = television(Helper);
        ServerPlayer User = player(Helper, new Vec3(9.5, 1.0, 10.5), "scp1730rooms_turned");
        lookAway(User);
        int End = Scp1730TelevisionBlockEntity.StaticTicks + Scp1730TelevisionBlockEntity.ShowTicks + 20;
        Helper.runAfterDelay(40, () -> Helper.assertTrue(sealed(Helper), "the doorway was not sealed during the show"));
        Helper.runAfterDelay(End, () -> {
            try {
                Helper.assertTrue(User.isAlive(), "looking away for the whole show still killed");
                Helper.assertTrue(Entity.phase() == Scp1730TelevisionBlockEntity.Idle, "the show did not end");
                Helper.assertTrue(open(Helper), "the doorway did not reopen after the show");
                Helper.assertTrue(Entity.readyAt() > Helper.getLevel().getGameTime(), "the television can start again without a cooldown");
                Helper.succeed();
            } finally {
                Scp1730GameTests.dismiss(User);
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp1730rooms_tvBlindfold")
    public static void tvBlindfold(GameTestHelper Helper) {
        Scp1730TelevisionBlockEntity Entity = television(Helper);
        ServerPlayer User = player(Helper, new Vec3(9.5, 1.0, 10.5), "scp1730rooms_blind");
        User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Blindfold.get()));
        lookAt(User, Entity);
        Helper.runAfterDelay(Scp1730TelevisionBlockEntity.StaticTicks + Scp1730TelevisionBlockEntity.GraceTicks + Scp1730TelevisionBlockEntity.LethalTicks * 4, () -> {
            try {
                Helper.assertTrue(Entity.phase() == Scp1730TelevisionBlockEntity.Showing, "the show was not running");
                Helper.assertTrue(User.isAlive(), "a blindfolded viewer died");
                Helper.succeed();
            } finally {
                Entity.forceFinish(Helper.getLevel());
                Scp1730GameTests.dismiss(User);
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp1730rooms_loopCount")
    public static void loopCount(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730SpecialRoom Room = room(Helper, Scp1730RoomKind.Loop, 1, 4, 6, 20, 12, 3, new int[]{4, 9, 3}, new int[]{20, 9, 1});
        Scp1730SpecialRooms.pin(World, Room);
        ServerPlayer User = player(Helper, new Vec3(6.5, 2.0, 9.5), "scp1730rooms_walker");
        try {
            List<Scp1730SpecialRoom> Rooms = List.of(Room);
            long Now = World.getGameTime() + 100000L;
            Scp1730RoomRules.player(World, User, Rooms, Now);
            Scp1730RoomRules.player(World, User, Rooms, Now + 1L);
            Scp1730RoomRules.LoopVisit Visit = Scp1730RoomRules.loopOf(User);
            Helper.assertTrue(Visit != null && Visit.Entry == 0, "the walker's entry door was not noted");
            int Limit = Visit.Limit;
            Helper.assertTrue(Limit >= Scp1730RoomRules.LoopMin && Limit <= Scp1730RoomRules.LoopMax, "loop count out of range: " + Limit);
            Vec3 Before = Helper.absoluteVec(new Vec3(20.3, 2.0, 9.5));
            Vec3 After = Helper.absoluteVec(new Vec3(20.8, 2.0, 9.5));
            for (int Pass = 0; Pass < Limit; Pass++) {
                Now += 100L;
                User.teleportTo(Before.x, Before.y, Before.z);
                Scp1730RoomRules.player(World, User, Rooms, Now);
                User.teleportTo(After.x, After.y, After.z);
                Scp1730RoomRules.player(World, User, Rooms, Now + 1L);
                double Lx = User.getX() - Helper.absoluteVec(Vec3.ZERO).x;
                Helper.assertTrue(Lx < 6.0, "pass " + (Pass + 1) + " of " + Limit + " walked out instead of looping, x " + Lx);
            }
            Now += 100L;
            User.teleportTo(Before.x, Before.y, Before.z);
            Scp1730RoomRules.player(World, User, Rooms, Now);
            User.teleportTo(After.x, After.y, After.z);
            Scp1730RoomRules.player(World, User, Rooms, Now + 1L);
            Helper.assertTrue(User.getX() > Before.x, "the loop never let the walker through after " + Limit + " passes");
            Vec3 Back = Helper.absoluteVec(new Vec3(4.7, 2.0, 9.5));
            Vec3 Gone = Helper.absoluteVec(new Vec3(4.2, 2.0, 9.5));
            Now += 100L;
            User.teleportTo(Back.x, Back.y, Back.z);
            Scp1730RoomRules.player(World, User, Rooms, Now);
            User.teleportTo(Gone.x, Gone.y, Gone.z);
            Scp1730RoomRules.player(World, User, Rooms, Now + 1L);
            Helper.assertTrue(User.getX() < Back.x, "turning back through the entry door was looped");
        } finally {
            cleanup(World, Room, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1730rooms_slowRoom")
    public static void slowRoom(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730SpecialRoom Room = room(Helper, Scp1730RoomKind.Slow, 1, 2, 2, 14, 14, 3);
        Scp1730SpecialRooms.pin(World, Room);
        Zombie Walker = Helper.spawn(EntityType.ZOMBIE, new BlockPos(8, 2, 8));
        Walker.setNoAi(true);
        Arrow Shot = new Arrow(EntityType.ARROW, World);
        Vec3 Launch = Helper.absoluteVec(new Vec3(5.5, 3.5, 5.5));
        Shot.moveTo(Launch.x, Launch.y, Launch.z);
        Shot.setNoGravity(true);
        Shot.setDeltaMovement(0.4, 0.0, 0.0);
        World.addFreshEntity(Shot);
        Helper.runAfterDelay(3, () -> {
            try {
                Helper.assertTrue(Walker.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Scp1730RoomRules.SlowMobId) != null, "a mob inside the slow room was not slowed");
                Helper.assertTrue(Shot.getDeltaMovement().length() < 0.4 * Scp1730RoomRules.SlowProjectile + 0.02, "a projectile inside the slow room kept its speed: " + Shot.getDeltaMovement().length());
                Vec3 Out = Helper.absoluteVec(new Vec3(22.5, 2.0, 22.5));
                Walker.teleportTo(Out.x, Out.y, Out.z);
                Vec3 Free = Helper.absoluteVec(new Vec3(20.5, 3.5, 26.5));
                Shot.teleportTo(Free.x, Free.y, Free.z);
                Helper.runAfterDelay(3, () -> {
                    try {
                        Helper.assertTrue(Walker.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Scp1730RoomRules.SlowMobId) == null, "the slowdown stayed on a mob that left");
                        Helper.assertTrue(Shot.getDeltaMovement().length() > 0.3, "a projectile that left the slow room stayed slow: " + Shot.getDeltaMovement().length());
                        Helper.succeed();
                    } finally {
                        Scp1730SpecialRooms.unpin(World, Room);
                        Walker.discard();
                        Shot.discard();
                    }
                });
            } catch (RuntimeException Problem) {
                Scp1730SpecialRooms.unpin(World, Room);
                Walker.discard();
                Shot.discard();
                throw Problem;
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp1730rooms_driftRoom")
    public static void driftRoom(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730SpecialRoom Room = room(Helper, Scp1730RoomKind.Drift, 1, 2, 2, 14, 14, 6);
        Scp1730SpecialRooms.pin(World, Room);
        ServerPlayer User = player(Helper, new Vec3(8.5, 2.0, 8.5), "scp1730rooms_floater");
        Helper.runAfterDelay(3, () -> {
            try {
                Helper.assertTrue(User.getAttribute(Attributes.GRAVITY).getModifier(Scp1730RoomRules.DriftId) != null, "gravity was not lowered inside the drift room");
                Helper.assertTrue(User.getAttributeValue(Attributes.GRAVITY) < 0.03, "gravity inside the drift room is " + User.getAttributeValue(Attributes.GRAVITY));
                Helper.assertTrue(Scp1730RoomRules.cushioned(User), "a fall inside the drift room is not cushioned");
                Vec3 Out = Helper.absoluteVec(new Vec3(22.5, 5.0, 22.5));
                User.teleportTo(Out.x, Out.y, Out.z);
                Helper.runAfterDelay(2, () -> {
                    try {
                        Helper.assertTrue(User.getAttribute(Attributes.GRAVITY).getModifier(Scp1730RoomRules.DriftId) == null, "low gravity stayed after leaving");
                        Helper.assertTrue(Scp1730RoomRules.cushioned(User) || User.onGround(), "a fall that started inside lost its cushion before landing");
                        Helper.succeed();
                    } finally {
                        cleanup(World, Room, User);
                    }
                });
            } catch (RuntimeException Problem) {
                cleanup(World, Room, User);
                throw Problem;
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp1730rooms_echoCap")
    public static void echoCap(GameTestHelper Helper) {
        Scp1730EchoTrack Track = new Scp1730EchoTrack();
        for (long Tick = 0; Tick < 900; Tick++) {
            Track.record(new Scp1730EchoTrack.Frame(Tick, Tick * 0.1, 64.0, 0.0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false, false));
            Helper.assertTrue(Track.size() <= Scp1730EchoTrack.MaxFrames, "the echo recording grew past its cap: " + Track.size());
        }
        Scp1730EchoTrack.Frame Shown = Track.replay(899);
        Helper.assertTrue(Shown != null && Shown.Tick() == 899 - Scp1730EchoTrack.Delay, "the echo does not replay the delayed frame");
        Helper.assertTrue(Track.replay(899 + Scp1730EchoTrack.MaxFrames + 10) == null, "the echo replays frames older than its recording");
        Helper.succeed();
    }
}
