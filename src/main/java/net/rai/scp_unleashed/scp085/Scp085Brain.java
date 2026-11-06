package net.rai.scp_unleashed.scp085;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.cameras.CameraViews;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class Scp085Brain {
    public static final double HopRange = 6.0;
    public static final double NoticeRange = 6.0;
    public static final int StayMin = 600;
    public static final int StayMax = 2400;
    public static final int ReactCooldown = 50;
    public static final int WaveCooldown = 1200;
    public static final int ShiftCooldown = 1800;
    public static final int FormMin = 240;
    public static final int FormMax = 480;
    public static final int NoteLines = 8;
    public static final int SignWords = 8;
    public static final int ReactWave = 0;
    public static final int ReactNote = 1;
    public static final int ReactSign = 2;
    private static final DustParticleOptions Graphite = new DustParticleOptions(new Vector3f(0.36f, 0.36f, 0.4f), 0.55f);

    private Scp085Brain() {
    }

    static void tick(ServerLevel World, BlockPos Pos, BlockState State, Scp085SheetBlockEntity Sheet) {
        long Now = World.getGameTime();
        Scp085Figure Figure = Scp085Figure.get(World);
        boolean Present = Figure.isAt(World, Pos);
        if (Present != Sheet.Here) {
            if (Present) {
                arrive(World, Pos, Sheet, null);
            } else {
                leave(Sheet, Now);
            }
        }
        if (!Sheet.Here) {
            return;
        }
        if (Math.floorMod(Now + Pos.hashCode(), 10) == 0) {
            notice(World, Pos, State, Sheet, Now);
        }
        ambience(World, Pos, State, Sheet, Now);
        if (Now >= Sheet.ActStart + Sheet.ActLength) {
            finish(World, Pos, State, Sheet, Now);
        }
    }

    private static void leave(Scp085SheetBlockEntity Sheet, long Now) {
        Sheet.Here = false;
        Sheet.Form = Scp085Form.Woman;
        Sheet.HopTarget = null;
        Sheet.QueuedSpot = -1;
        Sheet.PendingReact = -1;
        Sheet.Watcher = null;
        Sheet.start(Scp085Act.Idle, Now, 40, Sheet.ToX, Sheet.ToX, 0);
    }

    static void arrive(ServerLevel World, BlockPos Pos, Scp085SheetBlockEntity Sheet, @Nullable BlockPos From) {
        long Now = World.getGameTime();
        RandomSource Random = World.getRandom();
        Scp085Scene Scene = Sheet.scene();
        int Side;
        if (From != null) {
            Direction Right = facingOf(Sheet.getBlockState()).getCounterClockWise();
            double Dot = (From.getX() - Pos.getX()) * Right.getStepX() + (From.getZ() - Pos.getZ()) * Right.getStepZ();
            Side = Dot > 0.01 ? 1 : Dot < -0.01 ? -1 : (Random.nextBoolean() ? 1 : -1);
        } else {
            Side = 0;
        }
        int Start = Side > 0 ? Scene.walkMax() : Side < 0 ? Scene.walkMin() : (Scene.walkMin() + Scene.walkMax()) / 2;
        int End = Side == 0 ? Start : Start - Side * 10;
        Sheet.Here = true;
        Sheet.Form = Scp085Form.Woman;
        Sheet.QueuedSpot = -1;
        Sheet.PendingReact = -1;
        Sheet.HopTarget = null;
        Sheet.CameFrom = From;
        Sheet.Facing = Side > 0 ? -1 : Side < 0 ? 1 : (Random.nextBoolean() ? 1 : -1);
        Sheet.StayUntil = Now + StayMin + Random.nextInt(StayMax - StayMin);
        Sheet.start(Scp085Act.LeapIn, Now, Scp085Act.LeapTicks, Start, End, 0);
        Sheet.syncNow();
        play(World, Pos, Scp085Registry.Page.get(), 0.45f, 1.1f + Random.nextFloat() * 0.2f);
        play(World, Pos, Scp085Registry.Hop.get(), 0.7f, 1.0f + Random.nextFloat() * 0.2f);
    }

    private static void finish(ServerLevel World, BlockPos Pos, BlockState State, Scp085SheetBlockEntity Sheet, long Now) {
        RandomSource Random = World.getRandom();
        switch (Sheet.Act) {
            case LeapOut -> {
                transfer(World, Pos, Sheet);
                return;
            }
            case ShiftOut -> {
                Sheet.Form = Scp085Form.byIndex(Sheet.Param);
                Sheet.FormUntil = Now + FormMin + Random.nextInt(FormMax - FormMin);
            }
            case ShiftBack -> {
                Sheet.Form = Scp085Form.Woman;
                Sheet.ShiftReadyAt = Now + ShiftCooldown;
            }
            default -> {
            }
        }
        int X = Sheet.ToX;
        if (Sheet.Act == Scp085Act.Spot) {
            Scp085Scene.Spot Was = spotAt(Sheet.scene(), Sheet.Param);
            if (Was != null) {
                X = Mth.clamp(Was.X(), Sheet.scene().walkMin(), Sheet.scene().walkMax());
            }
        }
        if (Sheet.Act == Scp085Act.Walk && Sheet.QueuedSpot >= 0) {
            startSpot(World, Pos, Sheet, Now, Sheet.QueuedSpot);
            return;
        }
        next(World, Pos, State, Sheet, Now, X);
    }

    private static void next(ServerLevel World, BlockPos Pos, BlockState State, Scp085SheetBlockEntity Sheet, long Now, int X) {
        RandomSource Random = World.getRandom();
        Scp085Scene Scene = Sheet.scene();
        if (Sheet.Form != Scp085Form.Woman) {
            if (Now >= Sheet.FormUntil || Sheet.PendingReact >= 0) {
                Sheet.start(Scp085Act.ShiftBack, Now, Scp085Act.ShiftTicks, X, X, 0);
                play(World, Pos, Scp085Registry.Shift.get(), 0.8f, 1.0f + Random.nextFloat() * 0.2f);
                return;
            }
            if (Random.nextFloat() < 0.55f) {
                int To = roam(Scene, X, Random);
                Sheet.Facing = To >= X ? 1 : -1;
                Sheet.start(Scp085Act.AnimalMove, Now, travel(X, To, Sheet.Form), X, To, 0);
            } else {
                Sheet.start(Scp085Act.AnimalIdle, Now, 40 + Random.nextInt(60), X, X, 0);
            }
            return;
        }
        if (Sheet.PendingReact >= 0) {
            int Code = Sheet.PendingReact;
            Sheet.PendingReact = -1;
            react(World, Pos, Sheet, Now, X, Code);
            return;
        }
        if (Sheet.Watcher != null && Random.nextFloat() < 0.6f) {
            Sheet.start(Scp085Act.Watch, Now, 40 + Random.nextInt(30), X, X, 0);
            return;
        }
        if (Now >= Sheet.StayUntil && tryHop(World, Pos, State, Sheet, Now)) {
            return;
        }
        float Roll = Random.nextFloat();
        List<Scp085Scene.Spot> Spots = Scene.spots();
        if (Roll < 0.32f) {
            walk(Sheet, Now, X, roam(Scene, X, Random));
        } else if (Roll < 0.64f && !Spots.isEmpty()) {
            goSpot(World, Pos, Sheet, Now, X, Random.nextInt(Spots.size()));
        } else if (Roll < 0.76f && Now >= Sheet.ShiftReadyAt) {
            Sheet.start(Scp085Act.ShiftOut, Now, Scp085Act.ShiftTicks, X, X, 1 + Random.nextInt(3));
            play(World, Pos, Scp085Registry.Shift.get(), 0.8f, 1.0f + Random.nextFloat() * 0.2f);
        } else if (Roll < 0.83f) {
            Sheet.start(Scp085Act.Watch, Now, 40 + Random.nextInt(40), X, X, 0);
        } else {
            Sheet.start(Scp085Act.Idle, Now, 40 + Random.nextInt(80), X, X, 0);
        }
    }

    private static void walk(Scp085SheetBlockEntity Sheet, long Now, int X, int To) {
        Sheet.Facing = To >= X ? 1 : -1;
        Sheet.start(Scp085Act.Walk, Now, travel(X, To, Scp085Form.Woman), X, To, 0);
    }

    private static void goSpot(ServerLevel World, BlockPos Pos, Scp085SheetBlockEntity Sheet, long Now, int X, int Index) {
        Scp085Scene.Spot Target = Sheet.scene().spots().get(Index);
        if (Math.abs(Target.X() - X) > 1) {
            Sheet.QueuedSpot = Index;
            walk(Sheet, Now, X, Target.X());
            return;
        }
        startSpot(World, Pos, Sheet, Now, Index);
    }

    private static void startSpot(ServerLevel World, BlockPos Pos, Scp085SheetBlockEntity Sheet, long Now, int Index) {
        RandomSource Random = World.getRandom();
        Sheet.QueuedSpot = -1;
        Scp085Scene.Spot Target = spotAt(Sheet.scene(), Index);
        if (Target == null) {
            Sheet.start(Scp085Act.Idle, Now, 40, Sheet.ToX, Sheet.ToX, 0);
            return;
        }
        Sheet.Facing = Target.Facing();
        int Length = switch (Target.Pose()) {
            case Lie -> 300 + Random.nextInt(240);
            case SitSide, SitFront, SitGround -> 160 + Random.nextInt(160);
            case Crouch -> 80 + Random.nextInt(40);
            case LookOut -> 120 + Random.nextInt(80);
            case Twirl -> 48;
            case Write -> 160 + Random.nextInt(100);
        };
        Sheet.start(Scp085Act.Spot, Now, Length, Target.X(), Target.X(), Index);
        if (Target.Pose() == Scp085Pose.Twirl) {
            play(World, Pos, Scp085Registry.Page.get(), 0.25f, 1.5f);
        }
    }

    private static void notice(ServerLevel World, BlockPos Pos, BlockState State, Scp085SheetBlockEntity Sheet, long Now) {
        Player Seen = watcher(World, Pos, State);
        Sheet.Watcher = Seen == null ? null : Seen.getUUID();
        if (Seen == null || Sheet.Form != Scp085Form.Woman || !Sheet.Act.interruptible()) {
            return;
        }
        if (Sheet.Act == Scp085Act.Spot) {
            Scp085Scene.Spot Current = spotAt(Sheet.scene(), Sheet.Param);
            if (Current != null && !Current.Pose().wakeful()) {
                return;
            }
        }
        int X = currentX(Sheet, Now);
        if (Sheet.Act == Scp085Act.Spot) {
            X = Mth.clamp(X, Sheet.scene().walkMin(), Sheet.scene().walkMax());
        }
        Sheet.QueuedSpot = -1;
        if (Now >= Sheet.WaveReadyAt || !Seen.getUUID().equals(Sheet.LastWaved)) {
            Sheet.LastWaved = Seen.getUUID();
            Sheet.WaveReadyAt = Now + WaveCooldown;
            Sheet.start(Scp085Act.Wave, Now, Scp085Act.WaveTicks, X, X, 0);
        } else if (Sheet.Act != Scp085Act.Watch) {
            Sheet.start(Scp085Act.Watch, Now, 60, X, X, 0);
        }
    }

    @Nullable
    private static Player watcher(ServerLevel World, BlockPos Pos, BlockState State) {
        Direction Front = facingOf(State);
        Vec3 Normal = new Vec3(Front.getStepX(), 0.0, Front.getStepZ());
        Vec3 Plane = Vec3.atCenterOf(Pos).subtract(Normal.scale(0.48));
        Vec3 Target = Plane.add(Normal.scale(0.06));
        Player Best = null;
        double BestDistance = Double.MAX_VALUE;
        AABB Near = new AABB(Pos).inflate(NoticeRange);
        Predicate<Player> Fits = Found -> !Found.isSpectator() && Found.isAlive() && !Senses.isBlindfolded(Found) && !Scp268.isNeglected(Found) && Targeting.canWitness(Found);
        for (Player Candidate : CameraViews.withViewers(World, Near, World.getEntitiesOfClass(Player.class, Near, Fits), Player.class, Fits)) {
            Vec3 Eye = CameraViews.eye(Candidate);
            Vec3 Offset = Target.subtract(Eye);
            double Distance = Offset.length();
            if (Distance > NoticeRange || Distance < 0.25 || Eye.subtract(Plane).dot(Normal) < 0.2) {
                continue;
            }
            if (CameraViews.look(Candidate).dot(Offset.normalize()) < 0.93) {
                continue;
            }
            if (World.clip(new ClipContext(Eye, Target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Candidate)).getType() != HitResult.Type.MISS) {
                continue;
            }
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Candidate;
            }
        }
        return Best;
    }

    static void poke(ServerLevel World, BlockPos Pos, Scp085SheetBlockEntity Sheet, Player User) {
        long Now = World.getGameTime();
        if (Now < Sheet.ReactReadyAt || Sheet.Act == Scp085Act.LeapOut || Sheet.Act == Scp085Act.LeapIn) {
            return;
        }
        RandomSource Random = World.getRandom();
        Sheet.ReactReadyAt = Now + ReactCooldown;
        int Kind = Random.nextInt(3);
        if (Kind == Sheet.LastReact) {
            Kind = (Kind + 1 + Random.nextInt(2)) % 3;
        }
        Sheet.LastReact = Kind;
        int Detail = Kind == ReactNote ? Random.nextInt(NoteLines) : Kind == ReactSign ? Random.nextInt(SignWords) : 0;
        tell(User, Kind, Detail);
        int Code = Kind * 16 + Detail;
        if (Sheet.Form != Scp085Form.Woman || Sheet.Act == Scp085Act.ShiftOut || Sheet.Act == Scp085Act.ShiftBack) {
            Sheet.PendingReact = Code;
            if (Sheet.Act == Scp085Act.AnimalIdle || Sheet.Act == Scp085Act.AnimalMove) {
                int X = currentX(Sheet, Now);
                Sheet.start(Scp085Act.ShiftBack, Now, Scp085Act.ShiftTicks, X, X, 0);
                play(World, Pos, Scp085Registry.Shift.get(), 0.8f, 1.1f);
            }
            return;
        }
        int X = currentX(Sheet, Now);
        if (Sheet.Act == Scp085Act.Spot) {
            X = Mth.clamp(X, Sheet.scene().walkMin(), Sheet.scene().walkMax());
        }
        Sheet.QueuedSpot = -1;
        react(World, Pos, Sheet, Now, X, Code);
    }

    private static void tell(Player User, int Kind, int Detail) {
        Component Line = switch (Kind) {
            case ReactNote -> Component.translatable("message.scp_unleashed.scp085.note",
                    Component.translatable("message.scp_unleashed.scp085.note." + Detail).withStyle(ChatFormatting.WHITE));
            case ReactSign -> Component.translatable("message.scp_unleashed.scp085.sign",
                    Component.translatable("message.scp_unleashed.scp085.word." + Detail).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
            default -> Component.translatable("message.scp_unleashed.scp085.wave");
        };
        User.displayClientMessage(Line.copy().withStyle(ChatFormatting.GRAY), true);
    }

    static void react(ServerLevel World, BlockPos Pos, Scp085SheetBlockEntity Sheet, long Now, int X, int Code) {
        RandomSource Random = World.getRandom();
        int Kind = Code / 16;
        int Detail = Code % 16;
        Scp085Scene Scene = Sheet.scene();
        switch (Kind) {
            case ReactNote -> {
                Sheet.Facing = X < (Scene.walkMin() + Scene.walkMax()) / 2 ? 1 : -1;
                Sheet.start(Scp085Act.Note, Now, Scp085Act.NoteTicks, X, X, Detail);
                play(World, Pos, Scp085Registry.Scribble.get(), 0.85f, 1.1f + Random.nextFloat() * 0.2f);
            }
            case ReactSign -> {
                Sheet.start(Scp085Act.Sign, Now, Scp085Act.SignTicks, X, X, Detail);
                play(World, Pos, Scp085Registry.Page.get(), 0.4f, 1.3f);
            }
            default -> {
                Sheet.start(Scp085Act.Wave, Now, Scp085Act.WaveTicks, X, X, 1);
                play(World, Pos, Scp085Registry.Scribble.get(), 0.4f, 1.8f);
            }
        }
    }

    private static void ambience(ServerLevel World, BlockPos Pos, BlockState State, Scp085SheetBlockEntity Sheet, long Now) {
        long Elapsed = Now - Sheet.ActStart;
        if (Sheet.Act == Scp085Act.Note && (Elapsed == 18 || Elapsed == 30)) {
            play(World, Pos, Scp085Registry.Scribble.get(), 0.8f, 1.15f + World.getRandom().nextFloat() * 0.2f);
        }
        if (Sheet.Act == Scp085Act.Spot && Elapsed > 0 && Elapsed % 28 == 0) {
            Scp085Scene.Spot Current = spotAt(Sheet.scene(), Sheet.Param);
            if (Current != null && Current.Pose() == Scp085Pose.Write) {
                play(World, Pos, Scp085Registry.Scribble.get(), 0.5f, 1.0f + World.getRandom().nextFloat() * 0.3f);
            }
        }
    }

    public static boolean tryHop(ServerLevel World, BlockPos Pos, BlockState State, Scp085SheetBlockEntity Sheet, long Now) {
        Scp085Figure Figure = Scp085Figure.get(World);
        List<BlockPos> Options = nearbySheets(World, Pos, HopRange, Figure);
        if (Options.isEmpty()) {
            return false;
        }
        RandomSource Random = World.getRandom();
        double Total = 0.0;
        double[] Weights = new double[Options.size()];
        for (int Index = 0; Index < Options.size(); Index++) {
            BlockPos Option = Options.get(Index);
            double Weight = Option.equals(Sheet.CameFrom) ? 0.35 : 1.0;
            if (Figure.isHome(World, Option)) {
                Weight *= 1.5;
            }
            Weights[Index] = Weight;
            Total += Weight;
        }
        double Pick = Random.nextDouble() * Total;
        BlockPos Chosen = Options.get(Options.size() - 1);
        for (int Index = 0; Index < Options.size(); Index++) {
            Pick -= Weights[Index];
            if (Pick <= 0.0) {
                Chosen = Options.get(Index);
                break;
            }
        }
        startHop(World, Pos, Sheet, Now, Chosen);
        return true;
    }

    static void startHop(ServerLevel World, BlockPos Pos, Scp085SheetBlockEntity Sheet, long Now, BlockPos Target) {
        Direction Right = facingOf(Sheet.getBlockState()).getCounterClockWise();
        double Dot = (Target.getX() - Pos.getX()) * Right.getStepX() + (Target.getZ() - Pos.getZ()) * Right.getStepZ();
        int X = Mth.clamp(currentX(Sheet, Now), Sheet.scene().walkMin(), Sheet.scene().walkMax());
        Sheet.Facing = Dot > 0.01 ? 1 : Dot < -0.01 ? -1 : (X < 32 ? -1 : 1);
        Sheet.HopTarget = Target.immutable();
        Sheet.QueuedSpot = -1;
        Sheet.Form = Scp085Form.Woman;
        Sheet.start(Scp085Act.LeapOut, Now, Scp085Act.LeapTicks, X, X + Sheet.Facing * 10, 0);
        play(World, Pos, Scp085Registry.Hop.get(), 0.7f, 1.1f + World.getRandom().nextFloat() * 0.2f);
    }

    private static void transfer(ServerLevel World, BlockPos Pos, Scp085SheetBlockEntity Sheet) {
        BlockPos Target = Sheet.HopTarget;
        Sheet.HopTarget = null;
        Scp085Figure Figure = Scp085Figure.get(World);
        if (!Figure.isAt(World, Pos)) {
            leave(Sheet, World.getGameTime());
            return;
        }
        if (Target != null && World.isLoaded(Target) && World.getBlockEntity(Target) instanceof Scp085SheetBlockEntity Next && validTarget(World, Target, Figure)) {
            Figure.moveTo(World, Target);
            leave(Sheet, World.getGameTime());
            Sheet.syncNow();
            trail(World, Pos, Target);
            arrive(World, Target, Next, Pos);
            return;
        }
        arrive(World, Pos, Sheet, null);
    }

    private static void trail(ServerLevel World, BlockPos From, BlockPos To) {
        Vec3 Start = planeOf(World, From);
        Vec3 End = planeOf(World, To);
        double Rise = 0.6 + Start.distanceTo(End) * 0.12;
        for (int Index = 1; Index < 12; Index++) {
            double Progress = Index / 12.0;
            Vec3 Point = Start.lerp(End, Progress).add(0.0, Math.sin(Progress * Math.PI) * Rise, 0.0);
            World.sendParticles(Graphite, Point.x, Point.y, Point.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private static Vec3 planeOf(ServerLevel World, BlockPos Pos) {
        Direction Front = facingOf(World.getBlockState(Pos));
        return Vec3.atCenterOf(Pos).add(Front.getStepX() * -0.4, 0.0, Front.getStepZ() * -0.4);
    }

    public static List<BlockPos> nearbySheets(ServerLevel World, BlockPos Pos, double Range, Scp085Figure Figure) {
        List<BlockPos> Found = new ArrayList<>();
        int Reach = Mth.ceil(Range);
        double Limit = Range * Range;
        for (int ChunkX = (Pos.getX() - Reach) >> 4; ChunkX <= (Pos.getX() + Reach) >> 4; ChunkX++) {
            for (int ChunkZ = (Pos.getZ() - Reach) >> 4; ChunkZ <= (Pos.getZ() + Reach) >> 4; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    continue;
                }
                for (BlockEntity Entity : Chunk.getBlockEntities().values()) {
                    BlockPos Other = Entity.getBlockPos();
                    if (Entity instanceof Scp085SheetBlockEntity && !Entity.isRemoved() && !Other.equals(Pos) && Other.distSqr(Pos) <= Limit && validTarget(World, Other, Figure)) {
                        Found.add(Other.immutable());
                    }
                }
            }
        }
        return Found;
    }

    static boolean validTarget(ServerLevel World, BlockPos Pos, Scp085Figure Figure) {
        BlockState State = World.getBlockState(Pos);
        if (State.getBlock() instanceof Scp085DrawingBlock) {
            return true;
        }
        return State.getBlock() instanceof Scp085HomeSheetBlock && Figure.isHome(World, Pos);
    }

    static void sheetGone(ServerLevel World, BlockPos Pos, boolean HomeSheet) {
        Scp085Figure Figure = Scp085Figure.get(World);
        if (HomeSheet && Figure.isHome(World, Pos)) {
            Figure.homeRemoved(World, Pos);
            return;
        }
        if (!Figure.isAt(World, Pos)) {
            return;
        }
        Figure.sheetRemoved(World, Pos);
        GlobalPos Home = Figure.home();
        if (Home == null) {
            return;
        }
        ServerLevel HomeWorld = World.getServer().getLevel(Home.dimension());
        if (HomeWorld != null && HomeWorld.isLoaded(Home.pos()) && HomeWorld.getBlockEntity(Home.pos()) instanceof Scp085SheetBlockEntity Resting) {
            arrive(HomeWorld, Home.pos(), Resting, null);
        }
    }

    static void homePlaced(ServerLevel World, BlockPos Pos) {
        Scp085Figure Figure = Scp085Figure.get(World);
        GlobalPos Was = Figure.spot();
        if (!Figure.claimHome(World, Pos)) {
            return;
        }
        if (Was == null || Was.equals(Figure.home())) {
            return;
        }
        ServerLevel There = World.getServer().getLevel(Was.dimension());
        if (There == World && There.isLoaded(Was.pos()) && There.getBlockEntity(Was.pos()) instanceof Scp085SheetBlockEntity Away && Away.Here) {
            startHop(There, Was.pos(), Away, There.getGameTime(), Pos);
            return;
        }
        Figure.sendHome(World.getServer());
    }

    public static boolean force(ServerLevel World, BlockPos Pos, Scp085SheetBlockEntity Sheet, String Kind) {
        long Now = World.getGameTime();
        RandomSource Random = World.getRandom();
        int X = Mth.clamp(currentX(Sheet, Now), Sheet.scene().walkMin(), Sheet.scene().walkMax());
        Sheet.QueuedSpot = -1;
        switch (Kind) {
            case "wave" -> react(World, Pos, Sheet, Now, X, ReactWave * 16);
            case "note" -> react(World, Pos, Sheet, Now, X, ReactNote * 16 + Random.nextInt(NoteLines));
            case "sign" -> react(World, Pos, Sheet, Now, X, ReactSign * 16 + Random.nextInt(SignWords));
            case "cat", "bird", "rabbit" -> {
                Sheet.Form = Scp085Form.Woman;
                Sheet.start(Scp085Act.ShiftOut, Now, Scp085Act.ShiftTicks, X, X, Kind.equals("cat") ? 1 : Kind.equals("bird") ? 2 : 3);
                play(World, Pos, Scp085Registry.Shift.get(), 0.8f, 1.1f);
            }
            case "spot" -> {
                List<Scp085Scene.Spot> Spots = Sheet.scene().spots();
                if (Spots.isEmpty()) {
                    return false;
                }
                Sheet.Form = Scp085Form.Woman;
                goSpot(World, Pos, Sheet, Now, X, Random.nextInt(Spots.size()));
            }
            case "hop" -> {
                Sheet.Form = Scp085Form.Woman;
                return tryHop(World, Pos, Sheet.getBlockState(), Sheet, Now);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    public static int currentX(Scp085SheetBlockEntity Sheet, long Now) {
        if (Sheet.Act == Scp085Act.Walk || Sheet.Act == Scp085Act.AnimalMove) {
            float Progress = Mth.clamp((Now - Sheet.ActStart) / (float) Sheet.ActLength, 0.0f, 1.0f);
            return Math.round(Mth.lerp(Progress, Sheet.FromX, Sheet.ToX));
        }
        if (Sheet.Act == Scp085Act.LeapIn || Sheet.Act == Scp085Act.LeapOut) {
            return Sheet.FromX;
        }
        return Sheet.ToX;
    }

    private static int roam(Scp085Scene Scene, int X, RandomSource Random) {
        int Min = Scene.walkMin();
        int Max = Scene.walkMax();
        for (int Attempt = 0; Attempt < 6; Attempt++) {
            int To = Min + Random.nextInt(Max - Min + 1);
            if (Math.abs(To - X) >= 8) {
                return To;
            }
        }
        return X < (Min + Max) / 2 ? Max : Min;
    }

    private static int travel(int From, int To, Scp085Form Form) {
        return Math.max(10, Math.round(Math.abs(To - From) / Form.speed() * 20.0f));
    }

    @Nullable
    private static Scp085Scene.Spot spotAt(Scp085Scene Scene, int Index) {
        return Index >= 0 && Index < Scene.spots().size() ? Scene.spots().get(Index) : null;
    }

    static Direction facingOf(BlockState State) {
        return State.hasProperty(Scp085SheetBlock.Facing) ? State.getValue(Scp085SheetBlock.Facing) : Direction.NORTH;
    }

    private static void play(ServerLevel World, BlockPos Pos, SoundEvent Sound, float Volume, float Pitch) {
        Vec3 Where = planeOf(World, Pos);
        World.playSound(null, Where.x, Where.y, Where.z, Sound, SoundSource.BLOCKS, Volume, Pitch);
    }
}
