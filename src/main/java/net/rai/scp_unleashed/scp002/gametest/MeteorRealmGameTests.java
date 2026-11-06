package net.rai.scp_unleashed.scp002.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.scp002.MeteorCondition;
import net.rai.scp_unleashed.scp002.MeteorEntity;
import net.rai.scp_unleashed.scp002.Scp002Registry;
import net.rai.scp_unleashed.scp002.Scp002Strikes;
import net.rai.scp_unleashed.scp163.Scp163CraftEntity;
import net.rai.scp_unleashed.scp163.Scp163CrashCondition;
import net.rai.scp_unleashed.scp163.Scp163Crashes;
import net.rai.scp_unleashed.scp163.Scp163Registry;
import net.rai.scp_unleashed.scp2845.Scp2845Arrival;
import net.rai.scp_unleashed.scp2845.Scp2845Comet;
import net.rai.scp_unleashed.scp2845.Scp2845Records;
import net.rai.scp_unleashed.scp2845.Scp2845Registry;
import net.rai.scp_unleashed.scp538.gametest.Scp538TestPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class MeteorRealmGameTests {
    public static final String Arena = "gear_arena";
    public static final BlockPos HomePad = new BlockPos(6, 10, 3);
    public static final int AwayBase = 400;
    public static final int EndPadY = 120;
    public static final int NetherY = 100;
    public static final double Search = 12.0;
    public static final String Overworld = Level.OVERWORLD.location().toString();

    private MeteorRealmGameTests() {
    }

    private static ServerLevel level(GameTestHelper Helper, ResourceKey<Level> Key) {
        ServerLevel Found = Helper.getLevel().getServer().getLevel(Key);
        Helper.assertTrue(Found != null, Key.location() + " is missing from the test server");
        return Found;
    }

    private static BlockPos pad(ServerLevel World, BlockPos Centre, List<Runnable> Undo) {
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                BlockPos At = Centre.offset(Dx, 0, Dz);
                World.setBlock(At, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                Undo.add(() -> World.setBlock(At, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
            }
        }
        return Centre;
    }

    private static void standOn(Scp538TestPlayer Reader, ServerLevel World, BlockPos Pad) {
        Reader.setServerLevel(World);
        Reader.moveTo(Pad.getX() + 0.5, Pad.getY() + 1.0, Pad.getZ() + 0.5, 0.0f, 0.0f);
    }

    private static Scp538TestPlayer reader(GameTestHelper Helper, ServerLevel World, String Name, List<Runnable> Undo) {
        Scp538TestPlayer Reader = new Scp538TestPlayer(World, Name);
        Reader.enlist(true);
        Undo.add(() -> Reader.enlist(false));
        return Reader;
    }

    private static <T extends Entity> List<T> near(ServerLevel World, Class<T> Kind, BlockPos Pad) {
        return World.getEntitiesOfClass(Kind, new AABB(Pad).inflate(Search));
    }

    private static <T extends Entity> void sweep(List<Runnable> Undo, ServerLevel World, Class<T> Kind, BlockPos Pad) {
        Undo.add(() -> near(World, Kind, Pad).forEach(Entity::discard));
    }

    private static void finish(GameTestHelper Helper, List<Runnable> Undo) {
        GatewayGameTests.whenDone(Helper, () -> {
            for (int Index = Undo.size() - 1; Index >= 0; Index--) {
                Undo.get(Index).run();
            }
        });
    }

    private static BlockPos away(ResourceKey<Level> Key, int Lane) {
        return new BlockPos(AwayBase + Lane, Key == Level.END ? EndPadY : NetherY, AwayBase);
    }

    private static AnomalySpawners.Discovery discovery(ServerLevel World, BlockPos Pos, Scp538TestPlayer Reader, ResourceLocation Id) {
        return new AnomalySpawners.Discovery(World, Pos, Reader, Id, AnomalyDocuments.server().get(Id), true);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "smeteor_scp002")
    public static void scp002WaitsForTheOverworld(GameTestHelper Helper) {
        List<Runnable> Undo = new ArrayList<>();
        finish(Helper, Undo);
        GatewayGameTests.guard(Helper, () -> {
            MinecraftServer Server = Helper.getLevel().getServer();
            ServerLevel Home = Helper.getLevel();
            ServerLevel Nether = level(Helper, Level.NETHER);
            ServerLevel End = level(Helper, Level.END);
            BlockPos HomeGround = pad(Home, Helper.absolutePos(HomePad), Undo);
            BlockPos EndGround = pad(End, away(Level.END, 0), Undo);
            BlockPos NetherSpot = away(Level.NETHER, 0);
            sweep(Undo, Home, MeteorEntity.class, HomeGround);
            sweep(Undo, End, MeteorEntity.class, EndGround);
            sweep(Undo, Nether, MeteorEntity.class, NetherSpot);
            Scp538TestPlayer Reader = reader(Helper, Nether, "smeteor_002", Undo);
            Scp002Strikes Data = Scp002Strikes.get(Server);
            MeteorCondition Condition = new MeteorCondition(0, 0, 0, 0);
            Investigation Case = new Investigation(Scp002Registry.DocumentId, Home.getGameTime());
            standOn(Reader, Nether, NetherSpot);
            Condition.begin(Reader, Case);
            for (ServerLevel Away : List.of(Nether, End)) {
                standOn(Reader, Away, Away == End ? EndGround : NetherSpot);
                long Now = Server.overworld().getGameTime();
                Data.hasten(Now, 0);
                Data.tick(Server);
                Optional<Scp002Strikes.Strike> Entry = Data.forCase(Case.getId());
                Helper.assertTrue(Entry.isPresent(), "the SCP-002 strike vanished while its reader was in " + Away.dimension().location());
                Helper.assertTrue(Entry.get().getPhase() == Scp002Strikes.Stage.Waiting, "the SCP-002 meteor fired while its reader was in " + Away.dimension().location());
                Helper.assertTrue(Entry.get().getFireAt() > Now, "the SCP-002 strike was not put off while its reader was in " + Away.dimension().location());
                Helper.assertTrue(near(Away, MeteorEntity.class, Away == End ? EndGround : NetherSpot).isEmpty(), "a meteor appeared in " + Away.dimension().location());
                Helper.assertTrue(Condition.tick(Reader, Case).isEmpty(), "SCP-002 was found while its reader was in " + Away.dimension().location());
            }
            standOn(Reader, Home, HomeGround);
            Data.hasten(Server.overworld().getGameTime(), 0);
            Data.tick(Server);
            Scp002Strikes.Strike Fired = Data.forCase(Case.getId()).orElseThrow();
            Helper.assertTrue(Fired.getPhase() == Scp002Strikes.Stage.Inbound, "the SCP-002 meteor did not fire once its reader was back in the overworld");
            Helper.assertTrue(Overworld.equals(Fired.getDimension()), "the SCP-002 meteor fired into " + Fired.getDimension());
            List<MeteorEntity> Falling = near(Home, MeteorEntity.class, HomeGround);
            Helper.assertTrue(Falling.size() == 1, "expected one meteor over the overworld pad, found " + Falling.size());
            Helper.assertTrue(near(End, MeteorEntity.class, EndGround).isEmpty() && near(Nether, MeteorEntity.class, NetherSpot).isEmpty(), "a meteor appeared outside the overworld");
            Falling.forEach(Entity::discard);
            Data.landed(Fired.getId(), Home, HomeGround);
            standOn(Reader, End, EndGround);
            Helper.assertTrue(Condition.tick(Reader, Case).isEmpty(), "the overworld landing was reported while the reader stood in the End");
            standOn(Reader, Home, HomeGround);
            Optional<StartCondition.Finding> Found = Condition.tick(Reader, Case);
            Helper.assertTrue(Found.isPresent() && Found.get().Pos().equals(HomeGround), "the overworld landing was not reported once the reader came back");
            Helper.assertTrue(Scp002Strikes.spawn(discovery(Home, HomeGround, Reader, Scp002Registry.DocumentId)), "the landed SCP-002 strike was not claimed");
            Helper.assertTrue(Data.forCase(Case.getId()).isEmpty(), "the claimed SCP-002 strike stayed pending");
            int Before = Data.list().size();
            Data.strikeAt(End, EndGround, Vec3.atCenterOf(EndGround));
            Data.tick(Server);
            Helper.assertTrue(Data.list().size() == Before, "an exact SCP-002 strike in the End stayed pending");
            Helper.assertTrue(MeteorEntity.launch(End, EndGround, Vec3.atCenterOf(EndGround), null, Direction.SOUTH) == null, "a meteor launched into the End");
            Helper.assertFalse(Scp002Strikes.spawn(discovery(End, EndGround, Reader, Scp002Registry.DocumentId)), "an SCP-002 discovery in the End called a meteor");
            Helper.assertTrue(Data.list().size() == Before, "an SCP-002 discovery in the End queued a strike");
            MeteorEntity Stray = Scp002Registry.Meteor.get().create(End);
            Helper.assertTrue(Stray != null, "could not make a stray meteor");
            Stray.moveTo(EndGround.getX() + 0.5, EndGround.getY() + 1.0, EndGround.getZ() + 0.5, 0.0f, 0.0f);
            Stray.tick();
            Helper.assertTrue(Stray.isRemoved(), "a meteor kept ticking in the End");
            Helper.assertTrue(near(End, MeteorEntity.class, EndGround).isEmpty(), "a meteor was left in the End");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "smeteor_scp163")
    public static void scp163WaitsForTheOverworld(GameTestHelper Helper) {
        List<Runnable> Undo = new ArrayList<>();
        finish(Helper, Undo);
        GatewayGameTests.guard(Helper, () -> {
            MinecraftServer Server = Helper.getLevel().getServer();
            ServerLevel Home = Helper.getLevel();
            ServerLevel Nether = level(Helper, Level.NETHER);
            ServerLevel End = level(Helper, Level.END);
            BlockPos HomeGround = pad(Home, Helper.absolutePos(HomePad), Undo);
            BlockPos EndGround = pad(End, away(Level.END, 32), Undo);
            BlockPos NetherSpot = away(Level.NETHER, 32);
            sweep(Undo, Home, Scp163CraftEntity.class, HomeGround);
            sweep(Undo, End, Scp163CraftEntity.class, EndGround);
            sweep(Undo, Nether, Scp163CraftEntity.class, NetherSpot);
            Scp538TestPlayer InNether = reader(Helper, Nether, "smeteor_163_nether", Undo);
            Scp538TestPlayer InEnd = reader(Helper, End, "smeteor_163_end", Undo);
            standOn(InNether, Nether, NetherSpot);
            standOn(InEnd, End, EndGround);
            Scp163Crashes Data = Scp163Crashes.get(Server);
            Scp163CrashCondition Condition = new Scp163CrashCondition(0, 0, 0, 0);
            Investigation NetherCase = new Investigation(Scp163Registry.DocumentId, Home.getGameTime());
            Investigation EndCase = new Investigation(Scp163Registry.DocumentId, Home.getGameTime());
            Condition.begin(InNether, NetherCase);
            Condition.begin(InEnd, EndCase);
            long Now = Server.overworld().getGameTime();
            Data.tick(Server);
            for (Investigation Case : List.of(NetherCase, EndCase)) {
                Optional<Scp163Crashes.Crash> Entry = Data.forCase(Case.getId());
                String Where = Case == EndCase ? "the End" : "the Nether";
                Helper.assertTrue(Entry.isPresent(), "the SCP-163 crash vanished while its reader was in " + Where);
                Helper.assertTrue(Entry.get().getPhase() == Scp163Crashes.Stage.Waiting, "the SCP-163 craft fired while its reader was in " + Where);
                Helper.assertTrue(Entry.get().getFireAt() > Now, "the SCP-163 crash was not put off while its reader was in " + Where);
            }
            Helper.assertTrue(near(End, Scp163CraftEntity.class, EndGround).isEmpty() && near(Nether, Scp163CraftEntity.class, NetherSpot).isEmpty(),
                    "an SCP-163 craft appeared outside the overworld");
            Helper.assertTrue(Condition.tick(InEnd, EndCase).isEmpty() && Condition.tick(InNether, NetherCase).isEmpty(), "SCP-163 was found outside the overworld");
            standOn(InNether, Home, HomeGround);
            standOn(InEnd, Home, HomeGround);
            Helper.runAfterDelay(Scp163Crashes.RetryTicks + 6, () -> GatewayGameTests.guard(Helper, () -> {
                List<Scp163CraftEntity> Falling = near(Home, Scp163CraftEntity.class, HomeGround);
                Helper.assertTrue(Falling.size() == 2, "expected two SCP-163 craft over the overworld pad, found " + Falling.size());
                Helper.assertTrue(near(End, Scp163CraftEntity.class, EndGround).isEmpty() && near(Nether, Scp163CraftEntity.class, NetherSpot).isEmpty(),
                        "an SCP-163 craft appeared outside the overworld");
                Falling.forEach(Entity::discard);
                for (Investigation Case : List.of(NetherCase, EndCase)) {
                    Scp538TestPlayer Reader = Case == EndCase ? InEnd : InNether;
                    Scp163Crashes.Crash Fired = Data.forCase(Case.getId()).orElseThrow();
                    Helper.assertTrue(Fired.getPhase() == Scp163Crashes.Stage.Inbound, "the SCP-163 craft did not fire once its reader was back in the overworld");
                    Helper.assertTrue(Overworld.equals(Fired.getDimension()), "the SCP-163 craft fired into " + Fired.getDimension());
                    Data.landed(Fired.getId(), Home, HomeGround);
                    standOn(Reader, End, EndGround);
                    Helper.assertTrue(Condition.tick(Reader, Case).isEmpty(), "the overworld crash was reported while the reader stood in the End");
                    standOn(Reader, Home, HomeGround);
                    Optional<StartCondition.Finding> Found = Condition.tick(Reader, Case);
                    Helper.assertTrue(Found.isPresent() && Found.get().Pos().equals(HomeGround), "the overworld crash was not reported once the reader came back");
                    Helper.assertTrue(Scp163Crashes.spawn(discovery(Home, HomeGround, Reader, Scp163Registry.DocumentId)), "the landed SCP-163 crash was not claimed");
                    Helper.assertTrue(Data.forCase(Case.getId()).isEmpty(), "the claimed SCP-163 crash stayed pending");
                }
                int Before = Data.list().size();
                Data.crashAt(End, EndGround, Vec3.atCenterOf(EndGround));
                Data.tick(Server);
                Helper.assertTrue(Data.list().size() == Before, "an exact SCP-163 crash in the End stayed pending");
                Helper.assertTrue(Scp163CraftEntity.launch(End, EndGround, null, Direction.SOUTH) == null, "an SCP-163 craft launched into the End");
                Helper.assertFalse(Scp163Crashes.spawn(discovery(End, EndGround, InEnd, Scp163Registry.DocumentId)), "an SCP-163 discovery in the End called a craft");
                Helper.assertTrue(Data.list().size() == Before, "an SCP-163 discovery in the End queued a crash");
                Scp163CraftEntity Stray = Scp163Registry.Craft.get().create(End);
                Helper.assertTrue(Stray != null, "could not make a stray craft");
                Stray.moveTo(EndGround.getX() + 0.5, EndGround.getY() + 1.0, EndGround.getZ() + 0.5, 0.0f, 0.0f);
                Stray.tick();
                Helper.assertTrue(Stray.isRemoved(), "an SCP-163 craft kept ticking in the End");
                Helper.succeed();
            }));
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "smeteor_scp2845")
    public static void scp2845WaitsForTheOverworld(GameTestHelper Helper) {
        List<Runnable> Undo = new ArrayList<>();
        finish(Helper, Undo);
        GatewayGameTests.guard(Helper, () -> {
            MinecraftServer Server = Helper.getLevel().getServer();
            ServerLevel Home = Helper.getLevel();
            ServerLevel Nether = level(Helper, Level.NETHER);
            ServerLevel End = level(Helper, Level.END);
            BlockPos HomeGround = pad(Home, Helper.absolutePos(HomePad), Undo);
            BlockPos EndGround = pad(End, away(Level.END, 64), Undo);
            BlockPos NetherSpot = away(Level.NETHER, 64);
            sweep(Undo, Home, Scp2845Comet.class, HomeGround);
            sweep(Undo, End, Scp2845Comet.class, EndGround);
            sweep(Undo, Nether, Scp2845Comet.class, NetherSpot);
            Scp538TestPlayer Reader = reader(Helper, Nether, "smeteor_2845", Undo);
            Scp2845Arrival Condition = new Scp2845Arrival(0, 0, 0, 0);
            Investigation Case = new Investigation(Scp2845Registry.DocumentId, Home.getGameTime());
            Scp2845Records Records = Scp2845Records.get(Server);
            Undo.add(() -> Records.clearLanding(Case.getId()));
            CompoundTag State = Case.getState();
            standOn(Reader, Nether, NetherSpot);
            Condition.begin(Reader, Case);
            for (ServerLevel Away : List.of(Nether, End)) {
                standOn(Reader, Away, Away == End ? EndGround : NetherSpot);
                State.putLong(Scp2845Arrival.ReadyKey, Server.overworld().getGameTime());
                Helper.assertTrue(Condition.tick(Reader, Case).isEmpty(), "SCP-2845 was found while its reader was in " + Away.dimension().location());
                Helper.assertFalse(State.contains(Scp2845Arrival.LaunchedKey), "the SCP-2845 comet launched while its reader was in " + Away.dimension().location());
                Helper.assertTrue(near(Away, Scp2845Comet.class, Away == End ? EndGround : NetherSpot).isEmpty(), "a comet appeared in " + Away.dimension().location());
            }
            standOn(Reader, Home, HomeGround);
            State.putLong(Scp2845Arrival.ReadyKey, Server.overworld().getGameTime());
            Helper.assertTrue(Condition.tick(Reader, Case).isEmpty(), "SCP-2845 was found before its comet landed");
            Helper.assertTrue(State.contains(Scp2845Arrival.LaunchedKey), "the SCP-2845 comet did not launch once its reader was back in the overworld");
            Helper.assertTrue(Overworld.equals(State.getString(Scp2845Arrival.DimensionKey)), "the SCP-2845 comet launched into " + State.getString(Scp2845Arrival.DimensionKey));
            List<Scp2845Comet> Falling = near(Home, Scp2845Comet.class, HomeGround);
            Helper.assertTrue(Falling.size() == 1, "expected one comet over the overworld pad, found " + Falling.size());
            Helper.assertTrue(near(End, Scp2845Comet.class, EndGround).isEmpty() && near(Nether, Scp2845Comet.class, NetherSpot).isEmpty(), "a comet appeared outside the overworld");
            Falling.forEach(Entity::discard);
            Records.landed(Case.getId(), HomeGround);
            standOn(Reader, End, EndGround);
            Helper.assertTrue(Condition.tick(Reader, Case).isEmpty(), "the overworld comet was reported while the reader stood in the End");
            standOn(Reader, Home, HomeGround);
            Optional<StartCondition.Finding> Found = Condition.tick(Reader, Case);
            Helper.assertTrue(Found.isPresent() && Found.get().Pos().equals(HomeGround), "the overworld comet was not reported once the reader came back");
            Records.clearLanding(Case.getId());
            State.putString(Scp2845Arrival.DimensionKey, Level.END.location().toString());
            long Now = Server.overworld().getGameTime();
            Helper.assertTrue(Condition.tick(Reader, Case).isEmpty(), "a comet stranded in the End was reported");
            Helper.assertFalse(State.contains(Scp2845Arrival.LaunchedKey), "a comet stranded in the End was not called back");
            Helper.assertTrue(State.getLong(Scp2845Arrival.ReadyKey) > Now, "a comet stranded in the End was not put off");
            Helper.assertFalse(Scp2845Arrival.manifest(discovery(End, EndGround, Reader, Scp2845Registry.DocumentId)), "an SCP-2845 discovery in the End called a comet");
            Helper.assertTrue(Scp2845Comet.launch(End, EndGround, Vec3.atCenterOf(EndGround), null) == null, "a comet launched into the End");
            Helper.assertTrue(near(End, Scp2845Comet.class, EndGround).isEmpty(), "a comet was left in the End");
            Scp2845Comet Stray = Scp2845Registry.CometType.get().create(End);
            Helper.assertTrue(Stray != null, "could not make a stray comet");
            Stray.moveTo(EndGround.getX() + 0.5, EndGround.getY() + 1.0, EndGround.getZ() + 0.5, 0.0f, 0.0f);
            Stray.tick();
            Helper.assertTrue(Stray.isRemoved(), "a summoned comet kept ticking in the End");
            Helper.succeed();
        });
    }

    private static int run(MinecraftServer Server, CommandSourceStack Source, String Line) {
        try {
            return Server.getCommands().getDispatcher().execute(Line, Source);
        } catch (CommandSyntaxException Problem) {
            return 0;
        }
    }

    private static Set<UUID> strikes(MinecraftServer Server) {
        Set<UUID> Ids = new HashSet<>();
        Scp002Strikes.get(Server).list().forEach(Entry -> Ids.add(Entry.getId()));
        return Ids;
    }

    private static Set<UUID> crashes(MinecraftServer Server) {
        Set<UUID> Ids = new HashSet<>();
        Scp163Crashes.get(Server).list().forEach(Entry -> Ids.add(Entry.getId()));
        return Ids;
    }

    private static String spot(BlockPos Pos) {
        return Pos.getX() + " " + Pos.getY() + " " + Pos.getZ();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "smeteor_commands")
    public static void commandsOnlyCallMeteorsInTheOverworld(GameTestHelper Helper) {
        List<Runnable> Undo = new ArrayList<>();
        finish(Helper, Undo);
        GatewayGameTests.guard(Helper, () -> {
            MinecraftServer Server = Helper.getLevel().getServer();
            ServerLevel Home = Helper.getLevel();
            ServerLevel End = level(Helper, Level.END);
            BlockPos HomeGround = pad(Home, Helper.absolutePos(HomePad), Undo);
            BlockPos EndGround = pad(End, away(Level.END, 96), Undo);
            for (ServerLevel World : List.of(Home, End)) {
                BlockPos Around = World == End ? EndGround : HomeGround;
                sweep(Undo, World, MeteorEntity.class, Around);
                sweep(Undo, World, Scp163CraftEntity.class, Around);
                sweep(Undo, World, Scp2845Comet.class, Around);
            }
            Scp538TestPlayer Reader = reader(Helper, End, "smeteor_cmd", Undo);
            standOn(Reader, End, EndGround);
            CommandSourceStack Away = Server.createCommandSourceStack().withLevel(End).withPosition(Vec3.atBottomCenterOf(EndGround.above())).withPermission(4).withSuppressedOutput();
            Set<UUID> Strikes = strikes(Server);
            Set<UUID> Crashes = crashes(Server);
            Helper.assertTrue(run(Server, Away, "scp_unleashed scp002 strike " + spot(EndGround)) == 0, "the SCP-002 strike command worked in the End");
            Helper.assertTrue(run(Server, Away, "scp_unleashed scp002 meteor smeteor_cmd") == 0, "the SCP-002 meteor command targeted a player in the End");
            Helper.assertTrue(strikes(Server).equals(Strikes), "an SCP-002 command queued a strike from the End");
            Helper.assertTrue(run(Server, Away, "scp_unleashed scp163 arrive smeteor_cmd") == 0, "the SCP-163 arrive command targeted a player in the End");
            Helper.assertTrue(crashes(Server).equals(Crashes), "the SCP-163 command queued a crash from the End");
            Helper.assertTrue(run(Server, Away, "scp_unleashed scp2845 comet") == 0, "the SCP-2845 comet command worked in the End");
            Helper.assertTrue(run(Server, Away, "scp_unleashed scp2845 strike " + spot(EndGround)) == 0, "the SCP-2845 strike command worked in the End");
            Helper.assertTrue(near(End, Scp2845Comet.class, EndGround).isEmpty(), "an SCP-2845 command put a comet in the End");
            Reader.setGameMode(GameType.CREATIVE);
            Reader.setShiftKeyDown(true);
            Reader.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp002Registry.Specimen.get()));
            UseOnContext Use = new UseOnContext(Reader, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(EndGround), Direction.UP, EndGround, false));
            Helper.assertTrue(Scp002Registry.Specimen.get().useOn(Use) == InteractionResult.FAIL, "the SCP-002 specimen called a meteor in the End");
            Helper.assertTrue(strikes(Server).equals(Strikes), "the SCP-002 specimen queued a strike in the End");
            Helper.assertTrue(near(End, MeteorEntity.class, EndGround).isEmpty() && near(End, Scp163CraftEntity.class, EndGround).isEmpty(), "a command left something falling in the End");
            standOn(Reader, Home, HomeGround);
            CommandSourceStack Here = Server.createCommandSourceStack().withLevel(Home).withPosition(Vec3.atBottomCenterOf(HomeGround.above())).withPermission(4).withSuppressedOutput();
            Helper.assertTrue(run(Server, Here, "scp_unleashed scp002 strike " + spot(HomeGround)) == 1, "the SCP-002 strike command failed in the overworld");
            Helper.assertTrue(run(Server, Here, "scp_unleashed scp002 meteor smeteor_cmd") == 1, "the SCP-002 meteor command failed for a player in the overworld");
            Helper.assertTrue(run(Server, Here, "scp_unleashed scp163 arrive smeteor_cmd") == 1, "the SCP-163 arrive command failed for a player in the overworld");
            Scp002Strikes StrikeData = Scp002Strikes.get(Server);
            Scp163Crashes CrashData = Scp163Crashes.get(Server);
            Set<UUID> NewStrikes = strikes(Server);
            NewStrikes.removeAll(Strikes);
            Set<UUID> NewCrashes = crashes(Server);
            NewCrashes.removeAll(Crashes);
            NewStrikes.forEach(Id -> StrikeData.landed(Id, Home, HomeGround));
            NewCrashes.forEach(Id -> CrashData.landed(Id, Home, HomeGround));
            Helper.assertTrue(NewStrikes.size() == 2, "the overworld SCP-002 commands queued " + NewStrikes.size() + " strikes");
            Helper.assertTrue(NewCrashes.size() == 1, "the overworld SCP-163 command queued " + NewCrashes.size() + " crashes");
            Helper.assertTrue(run(Server, Here, "scp_unleashed scp2845 strike " + spot(HomeGround)) == 1, "the SCP-2845 strike command failed in the overworld");
            List<Scp2845Comet> Falling = near(Home, Scp2845Comet.class, HomeGround);
            Helper.assertTrue(Falling.size() == 1, "the overworld SCP-2845 strike made " + Falling.size() + " comets");
            Falling.forEach(Entity::discard);
            Helper.assertTrue(strikes(Server).equals(Strikes) && crashes(Server).equals(Crashes), "the overworld command strikes were not cleared");
            Helper.succeed();
        });
    }
}
