package net.rai.scp_unleashed.scp024;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp024.network.Scp024BurstPayload;
import net.rai.scp_unleashed.scp024.network.Scp024DialoguePayload;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp024Runs {
    public static final int Rounds = 6;
    public static final int CountdownTicks = 60;
    public static final int EliminateTicks = 30;
    public static final int CelebrateTicks = 50;
    public static final int GrandHairs = 24;
    public static final int WelcomePages = 5;
    public static final int FinalePages = 3;
    public static final int SweepEvery = 10;
    public static final int OrphanEvery = 40;
    public static final int ReleaseTicks = 600;
    public static final int ArrivalBurst = 2;
    public static final int WinBurst = 3;
    public static final int FinaleBurst = 4;
    private static final Map<UUID, UUID> LiveTokens = new ConcurrentHashMap<>();

    private Scp024Runs() {
    }

    public static boolean isLive(@Nullable UUID Token) {
        return Token != null && LiveTokens.containsKey(Token);
    }

    @Nullable
    public static UUID contestantOf(@Nullable UUID Token) {
        return Token == null ? null : LiveTokens.get(Token);
    }

    @Nullable
    public static Scp024Run runOf(ServerPlayer User) {
        return Scp024World.get(User.server).run(User.getUUID());
    }

    public static Vec3 doorstep(BlockPos DoorOrigin, Direction Front, double Out) {
        BlockPos Other = DoorOrigin.relative(Scp024DoorBlock.across(Front));
        double X = (DoorOrigin.getX() + Other.getX()) * 0.5 + 0.5 + Front.getStepX() * Out;
        double Z = (DoorOrigin.getZ() + Other.getZ()) * 0.5 + 0.5 + Front.getStepZ() * Out;
        return new Vec3(X, DoorOrigin.getY(), Z);
    }

    public record Outside(ServerLevel World, Vec3 Pos, float Yaw) {
    }

    public static Outside outside(ServerPlayer User) {
        MinecraftServer Server = User.server;
        Scp024World Data = Scp024World.get(Server);
        BlockPos Door = Data.door();
        if (Door != null) {
            return new Outside(Server.overworld(), doorstep(Door, Data.front(), 2.5), Data.front().toYRot());
        }
        DimensionTransition Home = User.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        return new Outside(Home.newLevel(), Home.pos(), Home.yRot());
    }

    public static void sendOutside(ServerPlayer User, @Nullable String Reason) {
        Scp024Session.takeProps(User);
        Outside Spot = outside(User);
        Scp024Stage.teleport(User, Spot.World(), Spot.Pos(), Spot.Yaw(), 0.0f);
        Scp024Sounds.play(Spot.World(), Spot.Pos(), Scp024Registry.DoorOpen.get(), 1.0f, 0.9f);
        if (Reason != null) {
            User.displayClientMessage(Component.translatable(Reason).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
        }
    }

    public static void enterByDoor(ServerPlayer User, BlockPos DoorOrigin, Direction Front) {
        if (User.isSpectator()) {
            return;
        }
        if (Scp024Stage.inStage(User)) {
            Scp024Quips.say(User, "one_way");
            return;
        }
        Vec3 Step = doorstep(DoorOrigin, Front, 2.5);
        ServerLevel World = User.serverLevel();
        Scp024Sounds.play(World, Step, Scp024Registry.DoorOpen.get(), 1.0f, 0.8f);
        Scp024Sounds.play(World, Step, Scp024Registry.DoorWhoosh.get(), 0.9f, 0.7f);
        start(User, null);
    }

    public static boolean start(ServerPlayer User, @Nullable String Forced) {
        MinecraftServer Server = User.server;
        ServerLevel Stage = Scp024Stage.level(Server);
        if (Stage == null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp024.no_stage").withStyle(ChatFormatting.RED), true);
            return false;
        }
        Scp024World Data = Scp024World.get(Server);
        Scp024Run Existing = Data.run(User.getUUID());
        if (Existing != null) {
            if (Scp024Stage.inStage(User)) {
                if (Forced != null) {
                    forceGame(User, Forced);
                }
                return true;
            }
            endRun(Server, Existing, false, "restart");
        }
        Scp024Hud.clear(Server, User.getUUID());
        int Field = Data.claimField(User.getUUID(), now(Server), Index -> usable(Stage, Index));
        Scp024Run Run = new Scp024Run(User.getUUID(), Field);
        Run.Seed = User.getRandom().nextLong();
        Data.addRun(Run);
        BlockPos Base = Scp024Stage.fieldBase(Run.Field);
        Scp024Stage.hold(Stage, Scp024Stage.lobbyBox(Run.Field), Run.HoldKey);
        Scp024Palette.clear(Stage, Base, Scp024Stage.lobby());
        Scp024Stage.discardOwned(Stage, Scp024Stage.lobbyBox(Run.Field), null);
        buildLobby(Stage, Run);
        enterLobby(Stage, Run, User, ArrivalBurst);
        if (Forced != null) {
            Run.GameId = Forced;
            Run.Played.add(Forced);
            Run.Stage = Scp024Run.Phase.Announce;
            beginGame(Server, Run);
            return true;
        }
        Run.Stage = Scp024Run.Phase.Welcome;
        Scp024Sounds.cue(User, Scp024Cue.Welcome);
        List<Component> Pages = new ArrayList<>();
        List<Integer> Cues = new ArrayList<>();
        for (int Page = 0; Page < WelcomePages; Page++) {
            Pages.add(Component.translatable("dialogue.scp_unleashed.scp024.welcome." + Page));
            Cues.add(Page == WelcomePages - 1 ? Scp024Cue.Applause.ordinal() : Scp024Cue.None.ordinal());
        }
        dialogue(Run, User, Pages, Cues);
        return true;
    }

    private static long now(MinecraftServer Server) {
        return Server.overworld().getGameTime();
    }

    private static boolean usable(ServerLevel Stage, int Field) {
        if (!Stage.getWorldBorder().isWithinBounds(Scp024Stage.footprint(Field))) {
            return false;
        }
        for (ServerPlayer Other : Stage.players()) {
            if (Scp024Stage.inField(Other.getX(), Other.getZ(), Field)) {
                return false;
            }
        }
        return true;
    }

    private static void dialogue(Scp024Run Run, ServerPlayer User, List<Component> Pages, List<Integer> Cues) {
        Run.Token++;
        Run.Waiting = true;
        Run.Pages.clear();
        Run.Pages.addAll(Pages);
        Run.Cues.clear();
        Run.Cues.addAll(Cues);
        if (Run.Host != null) {
            Run.Host.setTalking(true);
            Run.Host.gesture("point");
        }
        PacketDistributor.sendToPlayer(User, new Scp024DialoguePayload(Run.Token, List.copyOf(Pages), List.copyOf(Cues)));
    }

    public static void hostClicked(ServerPlayer User) {
        Scp024Run Run = runOf(User);
        if (Run == null || Run.Ended) {
            return;
        }
        if (Run.Stage == Scp024Run.Phase.Finale && !Run.Waiting) {
            Scp024Quips.send(User, Component.translatable("dialogue.scp_unleashed.scp024.finale." + (FinalePages - 1), GrandHairs));
            return;
        }
        if (Run.Waiting && !Run.Pages.isEmpty()) {
            PacketDistributor.sendToPlayer(User, new Scp024DialoguePayload(Run.Token, List.copyOf(Run.Pages), List.copyOf(Run.Cues)));
        }
    }

    public static void reply(ServerPlayer User, int Token) {
        Scp024Run Run = runOf(User);
        if (Run == null || Run.Ended || !Run.Waiting || Token != Run.Token) {
            return;
        }
        Run.Waiting = false;
        if (Run.Host != null) {
            Run.Host.setTalking(false);
        }
        switch (Run.Stage) {
            case Welcome -> announce(User.server, Run, User);
            case Announce -> beginGame(User.server, Run);
            case Prize -> {
                if (Run.Wins >= Rounds) {
                    finale(User.server, Run, User);
                } else {
                    announce(User.server, Run, User);
                }
            }
            default -> {
            }
        }
    }

    private static void announce(MinecraftServer Server, Scp024Run Run, ServerPlayer User) {
        Run.GameId = Scp024Games.pick(Run.Played, User.getRandom());
        Run.Played.add(Run.GameId);
        Run.Seed = User.getRandom().nextLong();
        Run.Stage = Scp024Run.Phase.Announce;
        Scp024Game Game = Scp024Games.create(Run.GameId);
        if (Game == null) {
            return;
        }
        int Round = Run.Wins + 1;
        List<Component> Pages = new ArrayList<>();
        List<Integer> Cues = new ArrayList<>();
        Component Name = Game.displayName().copy().withStyle(ChatFormatting.GOLD);
        String Intro = Round == Rounds ? "dialogue.scp_unleashed.scp024.announce.final" : "dialogue.scp_unleashed.scp024.announce." + User.getRandom().nextInt(3);
        Pages.add(Component.translatable(Intro, Round, Name));
        Cues.add(Scp024Cue.Fanfare.ordinal());
        for (Component Rule : Game.rules()) {
            Pages.add(Rule);
            Cues.add(Scp024Cue.None.ordinal());
        }
        Pages.add(Component.translatable("dialogue.scp_unleashed.scp024.ready." + User.getRandom().nextInt(3)));
        Cues.add(Scp024Cue.Drumroll.ordinal());
        dialogue(Run, User, Pages, Cues);
    }

    private static void beginGame(MinecraftServer Server, Scp024Run Run) {
        ServerLevel Stage = Scp024Stage.level(Server);
        ServerPlayer User = Server.getPlayerList().getPlayer(Run.PlayerId);
        Scp024Game Game = Scp024Games.create(Run.GameId);
        if (Run.Ended || Stage == null || User == null || Game == null) {
            return;
        }
        endSession(Run);
        leaveLobby(Stage, Run);
        BlockPos Origin = Scp024Stage.roomOrigin(Run.Field, Scp024Games.index(Run.GameId));
        Scp024Session Session = new Scp024Session(Stage, Run.PlayerId, Origin, Game, Run.Seed);
        LiveTokens.put(Session.Token, Run.PlayerId);
        Scp024Stage.hold(Stage, Session.Bounds, Session.Token);
        Scp024Palette.clear(Stage, Origin, Session.Plan);
        Scp024Stage.discardOwned(Stage, Session.Bounds, null);
        Scp024Palette.build(Stage, Origin, Session.Plan);
        Session.lights(Scp024Lights.rig(Stage, Session.Token, Origin, Session.Plan, Game.floorY(), Game.lit(), Game.lit() ? Scp024Lights.Warm : Scp024Lights.Gloom));
        Run.Session = Session;
        int[] Start = Session.mark("start");
        Scp024Stage.teleport(User, Stage, Session.centre(Start), Scp024Stage.yaw(Start[3]), 0.0f);
        Run.Stage = Scp024Run.Phase.Countdown;
        Run.PhaseTicks = 0;
        ServerBossEvent Bar = Scp024Hud.bar(Run, User);
        if (Bar != null) {
            Bar.setName(Game.displayName());
            Bar.setProgress(1.0f);
            Bar.setColor(BossEvent.BossBarColor.YELLOW);
        }
        Scp024Sounds.cue(User, Scp024Cue.MusicFade);
        Scp024Sounds.cue(User, Scp024Cue.Applause);
    }

    private static void hideBar(Scp024Run Run) {
        Scp024Hud.hideBar(Run.PlayerId);
    }

    private static void title(ServerPlayer User, Component Big, @Nullable Component Small, int Stay) {
        User.connection.send(new ClientboundSetTitlesAnimationPacket(2, Stay, 6));
        User.connection.send(new ClientboundSetSubtitleTextPacket(Small != null ? Small : Component.empty()));
        User.connection.send(new ClientboundSetTitleTextPacket(Big));
    }

    private static void endSession(Scp024Run Run) {
        Scp024Session Session = Run.Session;
        if (Session == null) {
            return;
        }
        try {
            Session.Game.cleanup(Session);
        } catch (RuntimeException Problem) {
            Scp024Events.Log.error("SCP-024 game {} failed to clean up", Session.Game.id(), Problem);
        }
        Session.cleanup();
        Scp024Palette.clear(Session.World, Session.Origin, Session.Plan);
        Scp024Stage.discardOwned(Session.World, Session.Bounds, null);
        Scp024Stage.release(Session.World, Session.Bounds, Session.Token);
        LiveTokens.remove(Session.Token);
        ServerPlayer User = Session.World.getServer().getPlayerList().getPlayer(Run.PlayerId);
        if (User != null) {
            Scp024Session.takeProps(User);
        }
        Run.Session = null;
    }

    private static void buildLobby(ServerLevel Stage, Scp024Run Run) {
        Scp024Palette.build(Stage, Scp024Stage.fieldBase(Run.Field), Scp024Stage.lobby());
        score(Stage, Run);
    }

    private static void score(ServerLevel Stage, Scp024Run Run) {
        BlockPos Base = Scp024Stage.fieldBase(Run.Field);
        List<int[]> Lamps = Scp024Stage.lobby().marks("score");
        for (int Index = 0; Index < Lamps.size(); Index++) {
            int[] Mark = Lamps.get(Index);
            BlockState Lamp = Index < Run.Wins ? Scp024Registry.Marquee.get().defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState();
            Stage.setBlock(Base.offset(Mark[0], Mark[1], Mark[2]), Lamp, Scp024Palette.Quiet);
        }
    }

    private static void exitDoor(ServerLevel Stage, Scp024Run Run) {
        BlockPos Base = Scp024Stage.fieldBase(Run.Field);
        Scp024Plan Lobby = Scp024Stage.lobby();
        Lobby.forEach((X, Y, Z, Mat, Facing, Variant) -> {
            if (Mat == Scp024Mat.StageDoor) {
                Stage.setBlock(Base.offset(X, Y, Z), Scp024Palette.state(Scp024Mat.ExitDoor, Facing, Variant), Scp024Palette.Quiet);
            }
        });
    }

    private static void enterLobby(ServerLevel Stage, Scp024Run Run, ServerPlayer User, int Burst) {
        if (Run.Ended) {
            return;
        }
        leaveLobby(Stage, Run);
        Run.LobbyToken = UUID.randomUUID();
        LiveTokens.put(Run.LobbyToken, Run.PlayerId);
        BlockPos Base = Scp024Stage.fieldBase(Run.Field);
        Scp024Plan Lobby = Scp024Stage.lobby();
        int[] HostMark = Lobby.mark("host");
        Scp024Host Figure = Scp024Registry.Host.get().create(Stage);
        if (Figure != null) {
            Vec3 Spot = Scp024Stage.centre(Base, HostMark);
            float Yaw = Scp024Stage.yaw(HostMark[3]);
            Figure.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
            Figure.setYHeadRot(Yaw);
            Figure.setYBodyRot(Yaw);
            Figure.setGuest(Run.PlayerId);
            Scp024Stage.own(Figure, Run.LobbyToken);
            Stage.addFreshEntity(Figure);
            Run.Host = Figure;
        }
        Run.LobbyLights = Scp024Lights.rig(Stage, Run.LobbyToken, Base, Lobby, 1.0, true, Scp024Lights.Warm);
        Run.LobbyLights.host(Run.Host);
        Run.LobbyLights.mood(Scp024Lights.Mood.Converge, 120);
        int[] Spawn = Lobby.mark("spawn");
        Scp024Stage.teleport(User, Stage, Scp024Stage.centre(Base, Spawn), Scp024Stage.yaw(Spawn[3]), -8.0f);
        Scp024Sounds.play(Stage, Scp024Stage.centre(Base, Spawn), Scp024Registry.SpotOn.get(), 0.8f, 1.0f);
        score(Stage, Run);
        hideBar(Run);
        Scp024Sounds.cue(User, Scp024Cue.MusicLobby);
        burst(Run, User, Burst);
    }

    public static List<BlockPos> cannons(Scp024Run Run) {
        BlockPos Base = Scp024Stage.fieldBase(Run.Field);
        List<BlockPos> Found = new ArrayList<>();
        for (int[] Mark : Scp024Stage.lobby().marks("cannon")) {
            Found.add(Base.offset(Mark[0], Mark[1], Mark[2]));
        }
        return Found;
    }

    public static void burst(Scp024Run Run, ServerPlayer User, int Strength) {
        if (Run.Ended || Strength <= 0) {
            return;
        }
        BlockPos Base = Scp024Stage.fieldBase(Run.Field);
        int[] Spawn = Scp024Stage.lobby().mark("spawn");
        PacketDistributor.sendToPlayer(User, new Scp024BurstPayload(cannons(Run), Base.offset(Spawn[0], Spawn[1], Spawn[2] - 6), Strength));
        Scp024Sounds.play(User.serverLevel(), Scp024Stage.centre(Base, Spawn), Scp024Registry.ConfettiPop.get(), 1.0f, 0.8f);
    }

    private static void leaveLobby(@Nullable ServerLevel Stage, Scp024Run Run) {
        if (Run.Host != null) {
            Run.Host.discard();
            Run.Host = null;
        }
        if (Run.LobbyLights != null) {
            Run.LobbyLights.remove();
            Run.LobbyLights = null;
        }
        if (LiveTokens.remove(Run.LobbyToken) != null && Stage != null) {
            Scp024Stage.discardOwned(Stage, Scp024Stage.lobbyBox(Run.Field), Run.LobbyToken);
        }
    }

    public static void tick(MinecraftServer Server) {
        Scp024World Data = Scp024World.get(Server);
        for (Scp024Run Run : new ArrayList<>(Data.runs())) {
            if (Run.Ended) {
                Data.removeRun(Run);
                continue;
            }
            ServerPlayer User = Server.getPlayerList().getPlayer(Run.PlayerId);
            if (User == null) {
                endRun(Server, Run, false, "offline");
                continue;
            }
            if (!Scp024Stage.inStage(User)) {
                if (User.isAlive()) {
                    endRun(Server, Run, false, "left the stage");
                }
                continue;
            }
            if (User.isAlive() && !Scp024Stage.isMoving(User) && !Scp024Stage.inField(User, Run.Field)) {
                endRun(Server, Run, false, "strayed from field " + Run.Field);
                sendOutside(User, "message.scp_unleashed.scp024.cancelled");
                continue;
            }
            try {
                tickRun(Server, Run, User);
            } catch (RuntimeException Problem) {
                Scp024Events.Log.error("SCP-024 run for {} failed to tick", User.getGameProfile().getName(), Problem);
            }
        }
        ServerLevel Stage = Scp024Stage.level(Server);
        if (Stage != null && Server.getTickCount() % SweepEvery == 0) {
            for (ServerPlayer Stray : new ArrayList<>(Stage.players())) {
                if (Stray.isAlive() && Data.run(Stray.getUUID()) == null && !Scp024Stage.isMoving(Stray)) {
                    sendOutside(Stray, "message.scp_unleashed.scp024.cancelled");
                }
            }
            for (Map.Entry<Integer, UUID> Stranded : Data.strandedFields().entrySet()) {
                ServerPlayer Owner = Server.getPlayerList().getPlayer(Stranded.getValue());
                if (Owner != null && !Scp024Stage.inField(Owner, Stranded.getKey())) {
                    Data.ownerLeft(Stranded.getValue(), Stranded.getKey());
                }
            }
            Data.forgetFree(now(Server));
        }
        if (Stage != null && Server.getTickCount() % OrphanEvery == 0) {
            sweepOrphans(Stage);
        }
    }

    private static void sweepOrphans(ServerLevel Stage) {
        List<Entity> Orphans = new ArrayList<>();
        for (Entity Found : Stage.getAllEntities()) {
            if (Scp024Stage.isOwned(Found) && !isLive(Scp024Stage.owner(Found))) {
                Orphans.add(Found);
            }
        }
        for (Entity Found : Orphans) {
            Found.discard();
        }
    }

    private static void tickRun(MinecraftServer Server, Scp024Run Run, ServerPlayer User) {
        Run.PhaseTicks++;
        switch (Run.Stage) {
            case Countdown -> countdown(Server, Run, User);
            case Playing -> play(Server, Run, User);
            case Eliminating -> eliminating(Server, Run, User);
            case Celebrate -> {
                if (Run.Session != null && Run.Session.lights() != null) {
                    Run.Session.lights().tick(User);
                }
                if (Run.PhaseTicks % 10 == 0) {
                    Scp024Prizes.confetti(User.serverLevel(), User.getX(), User.getY() + 2.2, User.getZ(), 30);
                }
                if (Run.PhaseTicks >= CelebrateTicks && !Run.Ended) {
                    toPrize(Server, Run, User);
                }
            }
            default -> {
                if (Run.LobbyLights != null) {
                    Run.LobbyLights.tick(User);
                }
                if (User.getY() < Scp024Stage.StageY - 6) {
                    BlockPos Base = Scp024Stage.fieldBase(Run.Field);
                    int[] Spawn = Scp024Stage.lobby().mark("spawn");
                    Scp024Stage.teleport(User, User.serverLevel(), Scp024Stage.centre(Base, Spawn), Scp024Stage.yaw(Spawn[3]), 0.0f);
                }
            }
        }
    }

    private static void countdown(MinecraftServer Server, Scp024Run Run, ServerPlayer User) {
        Scp024Session Session = Run.Session;
        if (Session == null) {
            beginGame(Server, Run);
            return;
        }
        if (Session.lights() != null) {
            Session.lights().tick(User);
        }
        if (Run.PhaseTicks == 1 || Run.PhaseTicks == 21 || Run.PhaseTicks == 41) {
            int Count = 3 - (Run.PhaseTicks - 1) / 20;
            title(User, Component.literal(String.valueOf(Count)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Session.Game.displayName(), 20);
            Scp024Sounds.cue(User, Scp024Cue.Tick);
        }
        if (Run.PhaseTicks >= CountdownTicks) {
            title(User, Component.translatable("title.scp_unleashed.scp024.go").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), null, 14);
            Scp024Sounds.cue(User, Scp024Cue.Go);
            Scp024Quips.say(User, "go");
            Run.Stage = Scp024Run.Phase.Playing;
            Run.PhaseTicks = 0;
            Session.Game.start(Session);
            if (Run.Ended) {
                return;
            }
        }
    }

    private static void play(MinecraftServer Server, Scp024Run Run, ServerPlayer User) {
        Scp024Session Session = Run.Session;
        if (Session == null) {
            beginGame(Server, Run);
            return;
        }
        Session.advance();
        Scp024Game Game = Session.Game;
        Scp024Outcome Result;
        if (User.getY() < Session.Origin.getY() + Game.killDepth()) {
            Result = Scp024Outcome.Lose;
        } else {
            Result = Game.tick(Session);
        }
        if (Run.Ended || Run.Session != Session || !User.isAlive()) {
            return;
        }
        int Limit = Game.seconds() * 20;
        int Left = Limit > 0 ? Math.max(0, Limit - Session.ticks()) : 0;
        if (Result == Scp024Outcome.Continue && Limit > 0 && Left <= 0) {
            Result = Game.survival() ? Scp024Outcome.Win : Scp024Outcome.Lose;
            if (Result == Scp024Outcome.Lose) {
                Scp024Quips.say(User, "timeout");
            }
        }
        if (User.getHealth() <= 6.0f) {
            Session.tauntOnce("hurt");
        }
        if (Limit > 0 && Left <= 200 && Left > 0) {
            Session.tauntOnce("time");
            if (Session.lights() != null && !Session.lights().isTense()) {
                Session.lights().tense(true);
            }
        }
        if (Session.lights() != null) {
            Session.lights().tick(User);
        }
        updateBar(Run, User, Session, Limit, Left);
        if (Result == Scp024Outcome.Win) {
            win(Run, User);
        } else if (Result == Scp024Outcome.Lose) {
            lose(Run, User);
        }
    }

    private static void updateBar(Scp024Run Run, ServerPlayer User, Scp024Session Session, int Limit, int Left) {
        ServerBossEvent Bar = Scp024Hud.bar(Run, User);
        if (Bar == null) {
            return;
        }
        Component Name = Session.Game.displayName().copy();
        if (Limit > 0) {
            int Seconds = (Left + 19) / 20;
            Name = Component.translatable("bar.scp_unleashed.scp024.timed", Name, Seconds / 60, String.format("%02d", Seconds % 60));
            float Fraction = Left / (float) Limit;
            Bar.setProgress(Math.max(0.0f, Math.min(1.0f, Fraction)));
            Bar.setColor(Fraction > 0.5f ? BossEvent.BossBarColor.GREEN : Fraction > 0.2f ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED);
        } else {
            Bar.setProgress(1.0f);
            Bar.setColor(BossEvent.BossBarColor.PURPLE);
        }
        if (!Session.status().getString().isEmpty()) {
            Name = Component.translatable("bar.scp_unleashed.scp024.status", Name, Session.status());
        }
        Bar.setName(Component.translatable("bar.scp_unleashed.scp024.round", Name, Run.Wins + 1, Rounds));
    }

    private static void stopHazards(Scp024Session Session) {
        for (Entity Found : new ArrayList<>(Session.spawned())) {
            if (!(Found instanceof Scp024Spotlight)) {
                Found.discard();
            }
        }
    }

    private static void win(Scp024Run Run, ServerPlayer User) {
        if (Run.Ended) {
            return;
        }
        Scp024Session Session = Run.Session;
        Run.Wins++;
        Run.Stage = Scp024Run.Phase.Celebrate;
        Run.PhaseTicks = 0;
        if (Session != null) {
            stopHazards(Session);
            if (Session.lights() != null) {
                Session.lights().tense(false);
                Session.lights().mood(Scp024Lights.Mood.Flash, CelebrateTicks + 10);
            }
        }
        Scp024Session.takeProps(User);
        hideBar(Run);
        title(User, Component.translatable("title.scp_unleashed.scp024.winner").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("title.scp_unleashed.scp024.tally", Run.Wins, Rounds), 40);
        Scp024Sounds.cue(User, Scp024Cue.Fanfare);
        Scp024Sounds.cue(User, Scp024Cue.Cheer);
        Scp024Prizes.confetti(User.serverLevel(), User.getX(), User.getY() + 2.2, User.getZ(), 60);
        User.clearFire();
    }

    private static void toPrize(MinecraftServer Server, Scp024Run Run, ServerPlayer User) {
        ServerLevel Stage = Scp024Stage.level(Server);
        if (Stage == null || Run.Ended) {
            return;
        }
        endSession(Run);
        Run.Stage = Scp024Run.Phase.Prize;
        Scp024Prizes.Award Won = Scp024Prizes.roll(User.getRandom());
        enterLobby(Stage, Run, User, WinBurst);
        boolean Kept = Scp024Prizes.give(User, Won);
        List<Component> Pages = new ArrayList<>();
        List<Integer> Cues = new ArrayList<>();
        Pages.add(Component.translatable("dialogue.scp_unleashed.scp024.win." + User.getRandom().nextInt(3), Run.Wins));
        Cues.add(Scp024Cue.Applause.ordinal());
        Pages.add(Component.translatable("dialogue.scp_unleashed.scp024.drumroll"));
        Cues.add(Scp024Cue.Drumroll.ordinal());
        Pages.add(Component.translatable("dialogue.scp_unleashed.scp024.reveal", Won.name().copy().withStyle(ChatFormatting.GOLD), Won.joke()));
        Cues.add(Scp024Cue.Reveal.ordinal());
        if (!Kept) {
            Pages.add(Component.translatable("quip.scp_unleashed.scp024.no_room.0"));
            Cues.add(Scp024Cue.Laugh.ordinal());
        }
        int Left = Rounds - Run.Wins;
        if (Left > 0) {
            Pages.add(Component.translatable(Left == 1 ? "dialogue.scp_unleashed.scp024.progress.last" : "dialogue.scp_unleashed.scp024.progress", Run.Wins, Left));
            Cues.add(Scp024Cue.None.ordinal());
        }
        if (Run.Host != null) {
            Run.Host.gesture("bow");
        }
        dialogue(Run, User, Pages, Cues);
    }

    private static void finale(MinecraftServer Server, Scp024Run Run, ServerPlayer User) {
        ServerLevel Stage = Scp024Stage.level(Server);
        if (Stage == null || Run.Ended) {
            return;
        }
        Run.Stage = Scp024Run.Phase.Finale;
        burst(Run, User, FinaleBurst);
        exitDoor(Stage, Run);
        Scp024Prizes.give(User, Scp024Prizes.grand());
        Scp024Prizes.give(User, new Scp024Prizes.Award(Scp024Prizes.Prize.DollHairs, GrandHairs));
        Scp024Prizes.confetti(Stage, User.getX(), User.getY() + 2.4, User.getZ(), 80);
        if (Run.LobbyLights != null) {
            Run.LobbyLights.mood(Scp024Lights.Mood.Flash, 200);
        }
        Scp024Sounds.cue(User, Scp024Cue.Finale);
        title(User, Component.translatable("title.scp_unleashed.scp024.survivor").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("title.scp_unleashed.scp024.exit_open"), 60);
        List<Component> Pages = new ArrayList<>();
        List<Integer> Cues = new ArrayList<>();
        for (int Page = 0; Page < FinalePages; Page++) {
            Pages.add(Component.translatable("dialogue.scp_unleashed.scp024.finale." + Page, GrandHairs));
            Cues.add(Page == 0 ? Scp024Cue.Cheer.ordinal() : Page == 1 ? Scp024Cue.Reveal.ordinal() : Scp024Cue.Applause.ordinal());
        }
        if (Run.Host != null) {
            Run.Host.gesture("bow");
        }
        dialogue(Run, User, Pages, Cues);
    }

    private static void lose(Scp024Run Run, ServerPlayer User) {
        if (Run.Ended) {
            return;
        }
        Scp024Session Session = Run.Session;
        Run.Stage = Scp024Run.Phase.Eliminating;
        Run.PhaseTicks = 0;
        hideBar(Run);
        Scp024Session.takeProps(User);
        if (Session != null) {
            stopHazards(Session);
            if (Session.lights() != null) {
                Session.lights().mood(Scp024Lights.Mood.Red, EliminateTicks + 20);
            }
            BlockPos Under = User.blockPosition().below();
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    for (int Dy = 0; Dy >= -1; Dy--) {
                        BlockPos Cell = Under.offset(Dx, Dy, Dz);
                        BlockState Was = Session.World.getBlockState(Cell);
                        if (Session.Bounds.contains(Vec3.atCenterOf(Cell)) && !Was.isAir()) {
                            Session.World.levelEvent(2001, Cell, Block.getId(Was));
                            Session.World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Scp024Palette.Quiet);
                        }
                    }
                }
            }
            Scp024Sounds.play(Session.World, User.position(), Scp024Registry.Trapdoor.get(), 1.0f, 0.7f);
        }
        title(User, Component.translatable("title.scp_unleashed.scp024.eliminated").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), null, 30);
        Scp024Sounds.cue(User, Scp024Cue.Sting);
        Scp024Sounds.cue(User, Scp024Cue.Laugh);
        Scp024Quips.say(User, "eliminated");
    }

    private static void eliminating(MinecraftServer Server, Scp024Run Run, ServerPlayer User) {
        if (Run.Session != null && Run.Session.lights() != null) {
            Run.Session.lights().tick(User);
        }
        if (Run.PhaseTicks < EliminateTicks) {
            return;
        }
        Scp024Hazards.eliminate(User);
        if (!Run.Ended) {
            endRun(Server, Run, true, "eliminated");
        }
    }

    public static void leaveByDoor(ServerPlayer User) {
        Scp024Run Run = runOf(User);
        if (Run == null || Run.Ended || Run.Stage != Scp024Run.Phase.Finale) {
            Scp024Quips.say(User, "one_way");
            return;
        }
        endRun(User.server, Run, true, "exit door");
    }

    private static void safely(String Step, Runnable Action) {
        try {
            Action.run();
        } catch (RuntimeException Problem) {
            Scp024Events.Log.error("SCP-024 cleanup step {} failed", Step, Problem);
        }
    }

    public static void endRun(MinecraftServer Server, Scp024Run Run, boolean Eject, String Reason) {
        if (Run.Ended) {
            return;
        }
        Run.Ended = true;
        Run.Waiting = false;
        Scp024World Data = Scp024World.get(Server);
        Data.removeRun(Run);
        ServerLevel Stage = Scp024Stage.level(Server);
        safely("hud", () -> Scp024Hud.clear(Server, Run.PlayerId));
        safely("session", () -> endSession(Run));
        safely("lobby", () -> leaveLobby(Stage, Run));
        LiveTokens.remove(Run.LobbyToken);
        if (Stage != null) {
            safely("lobby entities", () -> Scp024Stage.discardOwned(Stage, Scp024Stage.lobbyBox(Run.Field), null));
            safely("lobby blocks", () -> Scp024Palette.clear(Stage, Scp024Stage.fieldBase(Run.Field), Scp024Stage.lobby()));
            safely("lobby hold", () -> Scp024Stage.release(Stage, Scp024Stage.lobbyBox(Run.Field), Run.HoldKey));
        }
        Scp024Events.Log.debug("SCP-024 run for {} ended: {}", Run.PlayerId, Reason);
        if (Scp024Hud.barsHolding(Run.PlayerId) > 0) {
            Scp024Events.Log.warn("SCP-024 run for {} ended ({}) but a show bar still holds the player", Run.PlayerId, Reason);
        }
        ServerPlayer User = Server.getPlayerList().getPlayer(Run.PlayerId);
        if (User != null) {
            Scp024Session.takeProps(User);
            if (Eject && User.isAlive() && Scp024Stage.inStage(User)) {
                sendOutside(User, null);
            }
        }
        Data.releaseField(Run.Field, Run.PlayerId, now(Server) + ReleaseTicks, User == null || Scp024Stage.inField(User, Run.Field));
    }

    public static void died(ServerPlayer User) {
        Scp024Run Run = runOf(User);
        if (Run != null) {
            endRun(User.server, Run, false, "death");
        }
        Scp024Hud.clear(User.server, User.getUUID());
    }

    public static void loggedOut(ServerPlayer User) {
        Scp024Run Run = runOf(User);
        if (Run != null) {
            endRun(User.server, Run, false, "logout");
        }
        Scp024Hud.clear(User.server, User.getUUID());
        Scp024Hud.forget(User.getUUID());
        Scp024Session.takeProps(User);
    }

    public static void leftStage(ServerPlayer User) {
        Scp024Run Run = runOf(User);
        if (Run != null) {
            endRun(User.server, Run, false, "dimension change");
        }
        Scp024Hud.clear(User.server, User.getUUID());
        Scp024Session.takeProps(User);
    }

    public static void settle(ServerPlayer User) {
        Scp024Run Run = runOf(User);
        if (Run != null && !Scp024Stage.inStage(User)) {
            endRun(User.server, Run, false, "respawn");
        }
        if (runOf(User) == null) {
            Scp024Hud.clear(User.server, User.getUUID());
            Scp024Session.takeProps(User);
            if (User.isAlive() && Scp024Stage.inStage(User) && !Scp024Stage.isMoving(User)) {
                sendOutside(User, "message.scp_unleashed.scp024.cancelled");
            }
        }
    }

    public static void started(MinecraftServer Server) {
        Scp024World Data = Scp024World.get(Server);
        Data.clearRuns();
        Data.strandActive(now(Server) + ReleaseTicks);
        LiveTokens.clear();
        Scp024Hud.reset();
    }

    public static boolean simulateStop(ServerPlayer User) {
        Scp024Run Run = runOf(User);
        if (Run == null) {
            return false;
        }
        endRun(User.server, Run, false, "simulated stop");
        return true;
    }

    public static void stopping(MinecraftServer Server) {
        for (Scp024Run Run : new ArrayList<>(Scp024World.get(Server).runs())) {
            endRun(Server, Run, false, "server stopping");
        }
        Scp024World.get(Server).clearRuns();
        LiveTokens.clear();
        Scp024Hud.reset();
    }

    public static int liveTokens() {
        return LiveTokens.size();
    }

    public static boolean use(ServerPlayer User, BlockPos Pos) {
        Scp024Run Run = runOf(User);
        if (Run == null || Run.Ended || Run.Session == null || Run.Stage != Scp024Run.Phase.Playing) {
            return false;
        }
        return Run.Session.Game.use(Run.Session, Run.Session.local(Pos));
    }

    public static void projectileHit(MinecraftServer Server, Projectile Shot, BlockPos Pos) {
        for (Scp024Run Run : Scp024World.get(Server).runs()) {
            Scp024Session Session = Run.Session;
            if (Session != null && !Run.Ended && Run.Stage == Scp024Run.Phase.Playing && Session.Bounds.inflate(1.0).contains(Vec3.atCenterOf(Pos))) {
                Session.Game.hit(Session, Shot, Session.local(Pos));
                return;
            }
        }
    }

    public static boolean forceGame(ServerPlayer User, String Id) {
        if (!Scp024Games.exists(Id)) {
            return false;
        }
        Scp024Run Run = runOf(User);
        if (Run == null || Run.Ended || !Scp024Stage.inStage(User)) {
            return start(User, Id);
        }
        endSession(Run);
        Run.Waiting = false;
        Run.GameId = Id;
        if (!Run.Played.contains(Id)) {
            Run.Played.add(Id);
        }
        Run.Seed = User.getRandom().nextLong();
        Run.Stage = Scp024Run.Phase.Announce;
        beginGame(User.server, Run);
        return true;
    }

    public static boolean force(ServerPlayer User, Scp024Outcome Result) {
        Scp024Run Run = runOf(User);
        if (Run == null || Run.Ended || Run.Session == null || Run.Stage != Scp024Run.Phase.Playing && Run.Stage != Scp024Run.Phase.Countdown) {
            return false;
        }
        if (Result == Scp024Outcome.Win) {
            win(Run, User);
        } else {
            lose(Run, User);
        }
        return true;
    }

    public static boolean leave(ServerPlayer User) {
        Scp024Run Run = runOf(User);
        if (Run == null) {
            return false;
        }
        endRun(User.server, Run, true, "leave command");
        return true;
    }

    public static void giveBack(ServerPlayer User, ItemStack Stack) {
        if (!User.getInventory().add(Stack)) {
            User.getInventory().placeItemBackInInventory(Stack);
        }
    }
}
