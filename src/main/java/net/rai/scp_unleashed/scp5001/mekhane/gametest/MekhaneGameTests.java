package net.rai.scp_unleashed.scp5001.mekhane.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModItems;
import net.rai.scp_unleashed.materials.MaterialsRegistry;
import net.rai.scp_unleashed.scp5001.core.Scp5001Duel;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneConfig;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneDirector;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneProtocol;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneTimeline;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneValues;
import net.rai.scp_unleashed.scp5001.mekhane.Scp5001MekhaneRegistry;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001EmergencyConsoleBlock;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001EmergencyIntakeBlockEntity;
import net.rai.scp_unleashed.scp5001.mekhane.entity.Mekhane;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneFlight;
import net.rai.scp_unleashed.scp5001.xk.YaldabaothFlight;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class MekhaneGameTests {
    public static final String Arena = "scp5001mekhane_arena";
    public static final BlockPos Middle = new BlockPos(4, 1, 4);
    public static final int Jump = 50;

    private MekhaneGameTests() {
    }

    public static final class StubSerpent extends ArmorStand implements Scp5001Duel {
        public final List<String> Calls = new ArrayList<>();
        public final List<Float> Binds = new ArrayList<>();
        public final List<Vec3> Path = new ArrayList<>();
        public final YaldabaothFlight Flight = new YaldabaothFlight();

        public StubSerpent(EntityType<? extends ArmorStand> Type, Level World) {
            super(Type, World);
            setNoGravity(true);
            setInvisible(true);
        }

        public void launch(Vec3 Start) {
            Flight.manifest(Start, new Vec3(1.0, 0.0, 0.3), false, Start.y);
        }

        public void step() {
            if (isRemoved() || !Flight.present() || !(level() instanceof ServerLevel World)) {
                return;
            }
            Flight.tick(World, World.random);
            net.rai.scp_unleashed.scp5001.xk.YaldabaothVoice.tick(World, Flight);
            Path.add(Flight.head());
        }

        @Override
        public Vec3 duelHead() {
            return Flight.present() ? Flight.head() : position();
        }

        @Override
        public void duelEngage(Entity Opponent, Vec3 Arena) {
            Calls.add(Opponent instanceof Mekhane ? "engage" : "engage?");
            Flight.engage(Opponent == null ? -1 : Opponent.getId(), Arena);
        }

        @Override
        public void duelAttack(int Move, Vec3 Target) {
            Calls.add("attack" + Move);
            Flight.attack(Move, Target);
        }

        @Override
        public void duelReact(Vec3 From, float Power) {
            Calls.add("react");
            Flight.react(From, Power);
        }

        @Override
        public void duelBind(Vec3 Portal, float Progress) {
            if (Binds.isEmpty() || Calls.isEmpty() || !Calls.get(Calls.size() - 1).equals("bind")) {
                Calls.add("bind");
            }
            Binds.add(Progress);
            Flight.bind(Portal, Progress);
        }

        @Override
        public void duelBanish() {
            Calls.add("banish");
            Flight.banish();
            if (level() instanceof ServerLevel World) {
                net.rai.scp_unleashed.scp5001.xk.YaldabaothVoice.tick(World, Flight);
            }
            discard();
        }
    }

    public static double spread(List<Vec3> Points) {
        if (Points.isEmpty()) {
            return 0.0;
        }
        double MinX = Double.MAX_VALUE;
        double MaxX = -Double.MAX_VALUE;
        double MinZ = Double.MAX_VALUE;
        double MaxZ = -Double.MAX_VALUE;
        for (Vec3 Each : Points) {
            MinX = Math.min(MinX, Each.x);
            MaxX = Math.max(MaxX, Each.x);
            MinZ = Math.min(MinZ, Each.z);
            MaxZ = Math.max(MaxZ, Each.z);
        }
        return Math.max(MaxX - MinX, MaxZ - MinZ);
    }

    private static void reset(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel().getServer().overworld();
        List<Entity> Stale = new ArrayList<>();
        for (Entity Each : World.getAllEntities()) {
            if (Each instanceof StubSerpent) {
                Stale.add(Each);
            }
        }
        Stale.forEach(Entity::discard);
        MekhaneProtocol Protocol = MekhaneProtocol.get(World.getServer());
        MekhaneDirector.abort(World, Protocol, World.getGameTime());
        Scp5001State.get(World.getServer()).phase(Scp5001State.Phase.Dormant, World.getGameTime());
        Protocol.emitter(Helper.absolutePos(Middle));
        MekhaneConfig.overrideForTests(-1, -1, -1);
    }

    private static void finish(GameTestHelper Helper) {
        reset(Helper);
        MekhaneConfig.overrideForTests(-1, -1, -1);
        Helper.succeed();
    }

    private static StubSerpent serpent(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        StubSerpent Made = new StubSerpent(EntityType.ARMOR_STAND, World);
        Vec3 At = Vec3.atBottomCenterOf(Helper.absolutePos(Middle.above(2)));
        Made.moveTo(At.x, At.y, At.z, 0.0f, 0.0f);
        World.addFreshEntity(Made);
        Made.launch(At.add(60.0, 100.0, 20.0));
        MekhaneProtocol.get(World.getServer()).serpentId(Made.getUUID());
        Helper.onEachTick(Made::step);
        return Made;
    }

    private static MinecraftServer server(GameTestHelper Helper) {
        return Helper.getLevel().getServer();
    }

    private static void startDuel(GameTestHelper Helper, int Elapsed) {
        MinecraftServer Server = server(Helper);
        ServerLevel World = Server.overworld();
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        Scp5001State State = Scp5001State.get(Server);
        long Now = World.getGameTime();
        Protocol.fill();
        MekhaneDirector.beginSummon(World, Protocol, State, Now);
        State.phase(Scp5001State.Phase.Duel, Now);
        Protocol.stage(MekhaneProtocol.Stage.Duel, Now - Elapsed);
        Protocol.cursor(Elapsed - 1);
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp5001mekhane_console")
    public static void consoleWakesOnlyDuringXk(GameTestHelper Helper) {
        reset(Helper);
        MinecraftServer Server = server(Helper);
        ServerLevel World = Server.overworld();
        BlockPos Console = Middle.east(2);
        Helper.setBlock(Console, Scp5001MekhaneRegistry.Console.get().defaultBlockState());
        Helper.assertFalse(MekhaneDirector.engage(World), "the console engaged while dormant");
        Helper.startSequence()
                .thenExecuteAfter(25, () -> {
                    BlockState State = Helper.getBlockState(Console);
                    Helper.assertFalse(State.getValue(Scp5001EmergencyConsoleBlock.Awake), "the console woke outside XK");
                    Scp5001State.get(Server).phase(Scp5001State.Phase.Xk2, World.getGameTime());
                })
                .thenExecuteAfter(25, () -> {
                    Helper.assertTrue(Helper.getBlockState(Console).getValue(Scp5001EmergencyConsoleBlock.Awake), "the console stayed dark during XK");
                    Helper.assertTrue(MekhaneDirector.engage(World), "the console did not engage during XK");
                    Helper.assertTrue(MekhaneProtocol.get(Server).stage() == MekhaneProtocol.Stage.Engaged, "the protocol is not engaged");
                    Helper.assertFalse(MekhaneDirector.engage(World), "the protocol engaged twice");
                })
                .thenExecute(() -> finish(Helper));
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp5001mekhane_intake")
    public static void intakeCountsIronAndElectronics(GameTestHelper Helper) {
        reset(Helper);
        MinecraftServer Server = server(Helper);
        ServerLevel World = Server.overworld();
        MekhaneConfig.overrideForTests(10, 5, -1);
        BlockPos Bay = Middle.west(2);
        Helper.setBlock(Bay, Scp5001MekhaneRegistry.Intake.get().defaultBlockState());
        Scp5001EmergencyIntakeBlockEntity Intake = (Scp5001EmergencyIntakeBlockEntity) Helper.getBlockEntity(Bay);
        IItemHandler Handler = Intake.handler();
        ItemStack Early = Handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 4), false);
        Helper.assertTrue(Early.getCount() == 4, "the intake accepted iron before the protocol was engaged");
        Scp5001State.get(Server).phase(Scp5001State.Phase.Xk1, World.getGameTime());
        Helper.assertTrue(MekhaneDirector.engage(World), "could not engage");
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        Helper.assertTrue(Handler.insertItem(0, new ItemStack(Items.DIRT, 8), false).getCount() == 8, "dirt was accepted");
        Helper.assertTrue(Handler.insertItem(0, new ItemStack(ModItems.CreativeWire.get(), 2), false).getCount() == 2, "creative wire was accepted");
        Handler.insertItem(0, new ItemStack(Items.IRON_NUGGET, 9), false);
        Helper.assertTrue(Protocol.iron() == MekhaneValues.Scale, "nine nuggets should be one ingot but were " + Protocol.iron());
        Handler.insertItem(0, new ItemStack(Items.IRON_BLOCK, 1), false);
        Helper.assertTrue(Protocol.iron() == 10L * MekhaneValues.Scale, "a block should add nine ingots but iron is " + Protocol.iron());
        Helper.assertTrue(Handler.insertItem(0, new ItemStack(Items.RAW_IRON, 3), false).getCount() == 3, "iron was accepted past the demand");
        ItemStack Wires = Handler.insertItem(0, new ItemStack(ModItems.CopperWire.get(), 4), false);
        Helper.assertTrue(Wires.isEmpty() && Protocol.electronics() == MekhaneValues.Scale, "four copper wires should be one point but were " + Protocol.electronics());
        ItemStack Circuits = Handler.insertItem(0, new ItemStack(MaterialsRegistry.AdvancedCircuit.get(), 3), false);
        Helper.assertTrue(Circuits.getCount() == 2 && Protocol.electronics() == 5L * MekhaneValues.Scale, "one advanced circuit should finish the electronics, left "
                + Circuits.getCount() + " with " + Protocol.electronics());
        Helper.assertTrue(Protocol.met(), "both totals should be met");
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001mekhane_summon")
    public static void totalsSummonThenDuel(GameTestHelper Helper) {
        reset(Helper);
        MinecraftServer Server = server(Helper);
        ServerLevel World = Server.overworld();
        StubSerpent Stub = serpent(Helper);
        Scp5001State State = Scp5001State.get(Server);
        State.phase(Scp5001State.Phase.Xk1, World.getGameTime());
        Helper.assertTrue(MekhaneDirector.engage(World), "could not engage");
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        Protocol.fill();
        Helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    Helper.assertTrue(State.phase() == Scp5001State.Phase.Summoning, "meeting the totals did not start the summoning, phase " + State.phase());
                    Helper.assertTrue(Protocol.stage() == MekhaneProtocol.Stage.Summon, "the protocol is not summoning");
                    Protocol.stageTick(World.getGameTime() - MekhaneTimeline.SummonTicks + 2);
                })
                .thenExecuteAfter(6, () -> {
                    Helper.assertTrue(State.phase() == Scp5001State.Phase.Duel, "the summoning did not become a duel, phase " + State.phase());
                    Helper.assertTrue(Protocol.stage() == MekhaneProtocol.Stage.Duel, "the protocol is not duelling");
                    Helper.assertTrue(World.getEntity(Protocol.mekhaneId()) instanceof Mekhane, "Mekhane did not manifest");
                    Helper.assertTrue(Stub.Calls.contains("engage"), "the serpent was never engaged: " + Stub.Calls);
                })
                .thenExecute(() -> finish(Helper));
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp5001mekhane_timeline")
    public static void duelTimelineRunsInOrder(GameTestHelper Helper) {
        reset(Helper);
        MinecraftServer Server = server(Helper);
        ServerLevel World = Server.overworld();
        StubSerpent Stub = serpent(Helper);
        Scp5001State State = Scp5001State.get(Server);
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        startDuel(Helper, 0);
        List<Vec3> Giant = new ArrayList<>();
        Helper.onEachTick(() -> {
            if (Protocol.stage() == MekhaneProtocol.Stage.Duel) {
                if (Protocol.mekhaneId() != null && World.getEntity(Protocol.mekhaneId()) instanceof Mekhane Found) {
                    Giant.add(Found.position());
                }
                Protocol.stageTick(Protocol.stageTick() - (Jump - 1));
            } else if (Protocol.stage() == MekhaneProtocol.Stage.Clear) {
                Protocol.stageTick(Protocol.stageTick() - 200);
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Protocol.stage() == MekhaneProtocol.Stage.Idle, "the protocol has not finished, stage " + Protocol.stage());
            Helper.assertTrue(State.phase() == Scp5001State.Phase.Dormant, "the site did not return to dormant, phase " + State.phase());
            List<String> Calls = Stub.Calls;
            int Engage = Calls.indexOf("engage");
            int Attack = Calls.indexOf("attack" + Scp5001Duel.Lunge);
            int Coil = Calls.indexOf("attack" + Scp5001Duel.Coil);
            int React = Calls.indexOf("react");
            int Bind = Calls.indexOf("bind");
            int Banish = Calls.indexOf("banish");
            Helper.assertTrue(Engage == 0, "engage was not first: " + Calls);
            Helper.assertTrue(Engage < React && React < Attack && Attack < Coil && Coil < Bind && Bind < Banish, "calls out of order: " + Calls);
            Helper.assertTrue(Banish == Calls.size() - 1, "calls after the banish: " + Calls);
            Helper.assertTrue(!Stub.Binds.isEmpty() && Stub.Binds.get(0) < 0.2f && Stub.Binds.get(Stub.Binds.size() - 1) > 0.8f, "bind did not run 0 to 1: " + Stub.Binds);
            for (int Index = 1; Index < Stub.Binds.size(); Index++) {
                Helper.assertTrue(Stub.Binds.get(Index) >= Stub.Binds.get(Index - 1), "bind went backwards: " + Stub.Binds);
            }
            Helper.assertTrue(Protocol.mekhaneId() == null, "Mekhane is still tracked");
            Helper.assertTrue(spread(Giant) > 60.0, "Mekhane barely moved: spread " + spread(Giant));
            double Low = Giant.stream().mapToDouble(Point -> Point.y).min().orElse(0.0);
            double High = Giant.stream().mapToDouble(Point -> Point.y).max().orElse(0.0);
            Helper.assertTrue(High - Low > 50.0, "Mekhane never changed height: " + (High - Low));
            Helper.assertTrue(spread(Stub.Path) > 40.0, "the serpent stayed in one place: spread " + spread(Stub.Path));
            reset(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp5001mekhane_path")
    public static void flightPathStaysReadableAndSmooth(GameTestHelper Helper) {
        Vec3 Previous = MekhaneFlight.path(0);
        Vec3 Velocity = Vec3.ZERO;
        for (int Tick = 1; Tick < MekhaneTimeline.Ascend; Tick++) {
            Vec3 Here = MekhaneFlight.path(Tick);
            double Reach = Math.sqrt(Here.x * Here.x + Here.z * Here.z);
            Helper.assertTrue(Reach <= 150.0, "Mekhane leaves the readable area at " + Tick + ": " + Reach);
            Helper.assertTrue(Here.y >= 15.0 && Here.y + 33.0 <= 260.0, "Mekhane leaves the readable height at " + Tick + ": " + Here.y);
            Vec3 Step = Here.subtract(Previous);
            Helper.assertTrue(Step.length() <= 3.5, "Mekhane snaps at " + Tick + ": " + Step.length());
            Helper.assertTrue(Step.subtract(Velocity).length() <= 0.35, "Mekhane jerks at " + Tick + ": " + Step.subtract(Velocity).length());
            Velocity = Step;
            Previous = Here;
        }
        double Static = 0.0;
        int Still = 0;
        for (int Tick = 0; Tick < MekhaneTimeline.Banish; Tick += 20) {
            if (MekhaneFlight.path(Tick + 20).subtract(MekhaneFlight.path(Tick)).length() < 1.0) {
                Still += 20;
                Static = Math.max(Static, Still);
            } else {
                Still = 0;
            }
        }
        Helper.assertTrue(Static <= 140.0, "Mekhane sits still for " + Static + " ticks");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 4400, batch = "scp5001mekhane_plot")
    public static void plotWholeDuel(GameTestHelper Helper) {
        String Folder = System.getenv("SCP_MEKHANE_PLOT");
        if (Folder == null || Folder.isBlank()) {
            Helper.succeed();
            return;
        }
        reset(Helper);
        MinecraftServer Server = server(Helper);
        ServerLevel World = Server.overworld();
        StubSerpent Stub = serpent(Helper);
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        startDuel(Helper, 0);
        Stub.launch(MekhaneDirector.arena(Protocol).add(80.0, 10.0, 40.0));
        StringBuilder Lines = new StringBuilder("tick,mx,my,mz,yaw,dive,bank,sx,sy,sz,mode,id,opponent\n");
        Vec3 Base = Protocol.arena();
        double Ground = MekhaneDirector.ground(Protocol);
        StringBuilder Sounds = new StringBuilder("tick,event,x,y,z,volume,pitch\n");
        listen(Sounds, Base, Ground, () -> World.getGameTime() - Protocol.stageTick());
        Helper.onEachTick(() -> {
            if (Protocol.stage() != MekhaneProtocol.Stage.Duel || Protocol.mekhaneId() == null || !(World.getEntity(Protocol.mekhaneId()) instanceof Mekhane Found)) {
                return;
            }
            long Elapsed = World.getGameTime() - Protocol.stageTick();
            Vec3 Head = Stub.Flight.head();
            Lines.append(String.format(java.util.Locale.ROOT, "%d,%.2f,%.2f,%.2f,%.1f,%.1f,%.1f,%.2f,%.2f,%.2f,%s,%d,%d%n", Elapsed, Found.getX() - Base.x, Found.getY() - Ground,
                    Found.getZ() - Base.z, Found.getYRot(), Found.dive(1.0f), Found.bank(1.0f), Head.x - Base.x, Head.y - Ground, Head.z - Base.z, Stub.Flight.mode().name(),
                    Found.getId(), Stub.Flight.opponent()));
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Protocol.stage() == MekhaneProtocol.Stage.Clear || Protocol.stage() == MekhaneProtocol.Stage.Idle, "duel still running");
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(Folder, "mekhane_duel_paths.csv"), Lines.toString());
                java.nio.file.Files.writeString(java.nio.file.Path.of(Folder, "mekhane_duel_sounds.csv"), Sounds.toString());
            } catch (java.io.IOException Problem) {
                throw new IllegalStateException(Problem);
            } finally {
                net.rai.scp_unleashed.scp5001.core.FightAudio.Tap = null;
            }
            reset(Helper);
        });
    }

    private static void listen(StringBuilder Sounds, Vec3 Base, double Ground, java.util.function.LongSupplier Clock) {
        net.rai.scp_unleashed.scp5001.core.FightAudio.reset();
        net.rai.scp_unleashed.scp5001.core.FightAudio.Tap = Played -> Sounds.append(String.format(java.util.Locale.ROOT, "%d,%s,%.2f,%.2f,%.2f,%.3f,%.3f%n",
                Clock.getAsLong(), Played.Event(), Played.X() - Base.x, Played.Y() - Ground, Played.Z() - Base.z, Played.Volume(), Played.Pitch()));
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp5001mekhane_audio")
    public static void fightAudioLayersAndRests(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel().getServer().overworld();
        List<net.rai.scp_unleashed.scp5001.core.FightAudio.Played> Heard = new ArrayList<>();
        net.rai.scp_unleashed.scp5001.core.FightAudio.reset();
        net.rai.scp_unleashed.scp5001.core.FightAudio.Tap = Heard::add;
        Vec3 At = Vec3.atCenterOf(Helper.absolutePos(Middle)).add(0.0, 80.0, 0.0);
        long Begin = World.getGameTime();
        boolean First = net.rai.scp_unleashed.scp5001.core.FightAudio.cue(World, At, "test.strike", 20, net.rai.scp_unleashed.scp5001.core.FightAudio.layer(Scp5001MekhaneRegistry.AxeImpact),
                net.rai.scp_unleashed.scp5001.core.FightAudio.layer(Scp5001MekhaneRegistry.Thunder, 1.0f, 4));
        boolean Second = net.rai.scp_unleashed.scp5001.core.FightAudio.cue(World, At, "test.strike", 20, net.rai.scp_unleashed.scp5001.core.FightAudio.layer(Scp5001MekhaneRegistry.AxeImpact));
        Helper.assertTrue(First && !Second, "the rest did not hold back a repeated cue");
        Helper.assertTrue(Scp5001MekhaneRegistry.Thunder.get().getRange(1.0f) == net.rai.scp_unleashed.scp5001.core.FightAudio.Range, "fight sounds must reach the whole site");
        Helper.assertTrue(net.rai.scp_unleashed.scp5001.xk.Scp5001XkRegistry.Roar.get().getRange(1.0f) == net.rai.scp_unleashed.scp5001.core.FightAudio.Range, "roars must reach the whole site");
        Helper.runAfterDelay(8, () -> {
            net.rai.scp_unleashed.scp5001.core.FightAudio.Tap = null;
            Helper.assertTrue(Heard.size() == 2, "expected two layers, heard " + Heard.size());
            Helper.assertTrue(Heard.get(0).Event().endsWith("axe_impact") && Heard.get(0).Tick() == Begin, "the impact must land at once");
            Helper.assertTrue(Heard.get(1).Event().endsWith("thunder") && Heard.get(1).Tick() >= Begin + 4, "the tail must trail the impact");
            for (net.rai.scp_unleashed.scp5001.core.FightAudio.Played Each : Heard) {
                Helper.assertTrue(Each.Volume() <= 1.0f && Each.Volume() > 0.8f, "layer volume out of mix range: " + Each.Volume());
                Helper.assertTrue(Each.Pitch() >= 0.95f && Each.Pitch() <= 1.05f, "layer pitch drifted: " + Each.Pitch());
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp5001mekhane_plotsummon")
    public static void plotSummonSounds(GameTestHelper Helper) {
        String Folder = System.getenv("SCP_MEKHANE_PLOT");
        if (Folder == null || Folder.isBlank()) {
            Helper.succeed();
            return;
        }
        reset(Helper);
        MinecraftServer Server = server(Helper);
        ServerLevel World = Server.overworld();
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        Scp5001State State = Scp5001State.get(Server);
        Protocol.fill();
        MekhaneDirector.beginSummon(World, Protocol, State, World.getGameTime());
        Vec3 Base = Protocol.arena();
        double Ground = MekhaneDirector.ground(Protocol);
        BlockPos Emitter = MekhaneDirector.site(World, Protocol, State);
        StringBuilder Sounds = new StringBuilder("tick,event,x,y,z,volume,pitch\n");
        Sounds.append(String.format(java.util.Locale.ROOT, "-1,emitter,%.2f,%.2f,%.2f,0,0%n", Emitter.getX() + 0.5 - Base.x, Emitter.getY() + 0.5 - Ground, Emitter.getZ() + 0.5 - Base.z));
        long Begin = World.getGameTime();
        listen(Sounds, Base, Ground, () -> World.getGameTime() - Begin);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Protocol.stage() != MekhaneProtocol.Stage.Summon, "summon still running");
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(Folder, "mekhane_summon_sounds.csv"), Sounds.toString());
            } catch (java.io.IOException Problem) {
                throw new IllegalStateException(Problem);
            } finally {
                net.rai.scp_unleashed.scp5001.core.FightAudio.Tap = null;
            }
            reset(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001mekhane_respawn")
    public static void directorRespawnsAfterUnload(GameTestHelper Helper) {
        reset(Helper);
        MinecraftServer Server = server(Helper);
        ServerLevel World = Server.overworld();
        serpent(Helper);
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        startDuel(Helper, 400);
        Helper.runAfterDelay(6, () -> {
            Helper.assertTrue(Protocol.mekhaneId() != null && World.getEntity(Protocol.mekhaneId()) instanceof Mekhane, "no giant in the duel");
            Mekhane First = (Mekhane) World.getEntity(Protocol.mekhaneId());
            java.util.UUID Before = First.getUUID();
            First.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            Helper.runAfterDelay(2, () -> {
                Helper.assertTrue(Protocol.mekhaneId() != null && !Before.equals(Protocol.mekhaneId()), "the director did not replace the unloaded giant");
                Helper.assertTrue(World.getEntity(Protocol.mekhaneId()) instanceof Mekhane Again && Again.isAlive(), "the replacement giant is not in the world");
                Mekhane Again = (Mekhane) World.getEntity(Protocol.mekhaneId());
                int Elapsed = (int) (World.getGameTime() - Protocol.stageTick());
                double Off = Again.position().distanceTo(MekhaneDirector.place(Protocol, Elapsed));
                Helper.assertTrue(Off < MekhaneDirector.DashReach + 10.0, "the replacement giant appeared " + Off + " blocks off its path");
                Helper.assertTrue(giants(World) == 1, "the duel holds " + giants(World) + " giants after the respawn");
                finish(Helper);
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001mekhane_hidden")
    public static void hiddenGiantIsKeptNotCloned(GameTestHelper Helper) {
        reset(Helper);
        MinecraftServer Server = server(Helper);
        ServerLevel World = Server.overworld();
        serpent(Helper);
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        startDuel(Helper, 400);
        Helper.runAfterDelay(6, () -> {
            Mekhane First = MekhaneDirector.giant(World, Protocol);
            Vec3 Far = First.position().add(48000.0, 0.0, 48000.0);
            First.moveTo(Far.x, Far.y, Far.z, First.getYRot(), 0.0f);
            Helper.assertTrue(World.getEntity(First.getUUID()) == null, "the far chunk is loaded, so the giant was never hidden");
            Mekhane Kept = MekhaneDirector.giant(World, Protocol);
            Helper.assertTrue(Kept == First, "the director cloned a giant that was only in an unloaded chunk");
            Helper.runAfterDelay(3, () -> {
                Helper.assertTrue(First.getUUID().equals(Protocol.mekhaneId()) && World.getEntity(First.getUUID()) == First, "the hidden giant was not brought back");
                Helper.assertTrue(giants(World) == 1, "the duel holds " + giants(World) + " giants after the giant was hidden");
                finish(Helper);
            });
        });
    }

    private static int giants(ServerLevel World) {
        return World.getEntities(Scp5001MekhaneRegistry.MekhaneType.get(), Each -> Each.isAlive()).size();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp5001mekhane_resume")
    public static void resumeRestartsCurrentBeat(GameTestHelper Helper) {
        reset(Helper);
        MinecraftServer Server = server(Helper);
        ServerLevel World = Server.overworld();
        StubSerpent Stub = serpent(Helper);
        Scp5001State State = Scp5001State.get(Server);
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        startDuel(Helper, 1900);
        Protocol.mekhaneId(null);
        long Now = World.getGameTime();
        MekhaneDirector.resume(World, Protocol, State, Now);
        Helper.assertTrue(Now - Protocol.stageTick() == MekhaneTimeline.BindBeat, "resume did not rewind to the bind beat, elapsed " + (Now - Protocol.stageTick()));
        Helper.assertTrue(Protocol.cursor() == MekhaneTimeline.BindBeat - 1, "resume did not rewind the cue cursor");
        Helper.assertTrue(Stub.Calls.contains("engage"), "resume did not re-engage the serpent: " + Stub.Calls);
        Helper.assertTrue(World.getEntity(Protocol.mekhaneId()) instanceof Mekhane, "resume did not bring Mekhane back");
        Helper.startSequence()
                .thenExecuteAfter(4, () -> {
                    Helper.assertTrue(Stub.Calls.contains("attack" + Scp5001Duel.Coil), "the bind beat did not replay: " + Stub.Calls);
                    Helper.assertTrue(State.phase() == Scp5001State.Phase.Duel, "the phase left the duel");
                })
                .thenExecute(() -> finish(Helper));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001mekhane_watchdog")
    public static void watchdogRestartsStalledDuel(GameTestHelper Helper) {
        reset(Helper);
        MinecraftServer Server = server(Helper);
        ServerLevel World = Server.overworld();
        Scp5001State State = Scp5001State.get(Server);
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        MekhaneConfig.overrideForTests(-1, -1, 20);
        startDuel(Helper, 400);
        Protocol.serpentId(null);
        long Started = World.getGameTime();
        Helper.startSequence()
                .thenExecuteAfter(10, () -> Helper.assertTrue(Protocol.stage() == MekhaneProtocol.Stage.Duel && World.getGameTime() - Protocol.stageTick() <= 401,
                        "the duel advanced without an opponent"))
                .thenWaitUntil(() -> Helper.assertTrue(Protocol.restarts() == 1, "the watchdog has not fired yet"))
                .thenExecute(() -> Helper.assertTrue(State.phase() == Scp5001State.Phase.Summoning || State.phase() == Scp5001State.Phase.Duel,
                        "the watchdog left the XK pause, phase " + State.phase()))
                .thenExecute(() -> serpent(Helper))
                .thenWaitUntil(() -> {
                    Helper.assertTrue(Protocol.stage() == MekhaneProtocol.Stage.Duel, "the duel did not restart");
                    Helper.assertTrue(World.getGameTime() - Protocol.stageTick() < 200, "the duel did not restart from the standoff");
                })
                .thenExecute(() -> Helper.assertTrue(World.getGameTime() - Started < 150, "the watchdog took too long"))
                .thenExecute(() -> finish(Helper));
    }
}
