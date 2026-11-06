package net.rai.scp_unleashed.scp1730;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.block.Scp1730ControlBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730SealBlock;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Painter;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Fixture;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Spot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class Scp1730Director {
    public static final long EmptyGrace = 6000L;
    public static final int SealStep = 5;
    public static final int AmbienceMin = 200;
    public static final int AmbienceSpread = 260;
    public static final int JobPainter = 0;
    public static final int JobGuardian = 1;
    public static final int JobSetPieceBase = 8;
    private static final List<Pending> Seals = new ArrayList<>();

    private record Pending(ServerLevel World, BlockPos Pos, long At) {
    }

    private Scp1730Director() {
    }

    public static void prepare(ServerLevel Target) {
        Scp1730State State = Scp1730State.get(Target.getServer());
        if (!State.active()) {
            State.startRun(Target.getGameTime());
            if (Scp1730World.standingIn()) {
                Scp1730World.paintZone(Target, State.run(), 0);
            }
        }
    }

    public static List<ServerPlayer> inside(ServerLevel World) {
        List<ServerPlayer> Found = new ArrayList<>();
        for (ServerPlayer User : World.players()) {
            if (!User.isSpectator() && User.isAlive()) {
                Found.add(User);
            }
        }
        return Found;
    }

    public static void tick(MinecraftServer Server) {
        Scp1730Shockwaves.tick();
        tickSeals();
        ServerLevel World = Scp1730World.level(Server);
        if (World == null) {
            return;
        }
        Scp1730State State = Scp1730State.get(Server);
        if (!State.active()) {
            return;
        }
        int Run = State.run();
        List<ServerPlayer> Inside = inside(World);
        long Now = World.getGameTime();
        if (Inside.isEmpty()) {
            if (State.emptySince() < 0L) {
                State.setEmptySince(Now);
            }
            if (State.collapsing() || Now - State.emptySince() > EmptyGrace) {
                State.endRun();
            }
            return;
        }
        State.setEmptySince(-1L);
        Scp1730Plan Plan = Scp1730World.plan(Run);
        int[] ZoneOf = new int[Inside.size()];
        for (int Index = 0; Index < Inside.size(); Index++) {
            ServerPlayer User = Inside.get(Index);
            int Lx = Scp1730World.localX(Run, User.getX());
            int Lz = Scp1730World.localZ(User.getZ());
            int Ly = (int) Math.floor(User.getY());
            ZoneOf[Index] = Plan.zoneAt(Lx, Ly, Lz);
            if (Plan.inPit(Lx, Ly, Lz) && !User.isCreative()) {
                fall(World, User);
                continue;
            }
            if (ZoneOf[Index] >= 0) {
                populate(World, State, Run, Plan, ZoneOf[Index]);
            }
            Scp1730Cracks.tick(World, User, Math.max(0, ZoneOf[Index]));
            Scp1730Breach.tickPlayer(World, User, Run, ZoneOf[Index]);
            ambience(World, User, ZoneOf[Index]);
        }
        if (Now % 20L == 0L) {
            for (int Zone = 0; Zone < Scp1730State.ZoneCount; Zone++) {
                if (State.populated(Zone)) {
                    runJobs(World, State, Run, Plan, Zone);
                }
            }
        }
        Scp1730WingDoor.tick(World, State, Run);
        Scp1730Breach.tickEvents(World, State, Run, Inside, ZoneOf);
        Scp1730Breach.despawn(World);
        if (State.collapsing()) {
            Scp1730Collapse.tick(World, State);
        }
    }

    public static void fall(ServerLevel World, ServerPlayer User) {
        World.playSound(null, User.getX(), User.getY(), User.getZ(), Scp1730Registry.PitFall.get(), SoundSource.PLAYERS, 1.2f, 0.7f);
        Scp1730Symbols.release(User);
        User.hurt(new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp1730Registry.PitDamage)), Float.MAX_VALUE);
    }

    private static void ambience(ServerLevel World, ServerPlayer User, int Zone) {
        if ((World.getGameTime() + User.getId() * 31L) % (AmbienceMin + (User.getId() * 13L) % AmbienceSpread) != 0L) {
            return;
        }
        SoundEvent[] Choices = {Scp1730Registry.AmbienceAlarm.get(), Scp1730Registry.AmbiencePa.get(), Scp1730Registry.AmbienceDrip.get(),
                Scp1730Registry.AmbienceGroan.get(), Scp1730Registry.AmbienceVent.get()};
        SoundEvent Chosen = Choices[World.random.nextInt(Choices.length)];
        double Angle = World.random.nextDouble() * Math.PI * 2.0;
        double Reach = 8.0 + World.random.nextDouble() * 8.0;
        World.playSound(null, User.getX() + Math.cos(Angle) * Reach, User.getY() + 1.0, User.getZ() + Math.sin(Angle) * Reach, Chosen, SoundSource.AMBIENT,
                0.5f + Math.max(0, Zone) * 0.1f, 0.85f + World.random.nextFloat() * 0.2f);
    }

    public static void populate(ServerLevel World, Scp1730State State, int Run, Scp1730Plan Plan, int Zone) {
        if (State.populated(Zone)) {
            return;
        }
        State.markPopulated(Zone);
        runJobs(World, State, Run, Plan, Zone);
    }

    public static void runJobs(ServerLevel World, Scp1730State State, int Run, Scp1730Plan Plan, int Zone) {
        if (Zone < Scp1730State.DeepZone && !State.jobDone(Zone, JobPainter)) {
            for (Scp1730Fixture Each : Plan.fixtures("painter", Zone)) {
                BlockPos At = Scp1730World.world(Run, Each);
                if (World.isPositionEntityTicking(At)) {
                    Scp1730Painter Painter = Scp1730Registry.PainterType.get().create(World);
                    if (Painter != null) {
                        Painter.moveTo(At.getX() + 0.5, At.getY(), At.getZ() + 0.5, Scp1730Plan.yawOf(Each.Facing()), 0.0f);
                        Painter.finalizeSpawn(World, World.getCurrentDifficultyAt(At), MobSpawnType.EVENT, null);
                        World.addFreshEntity(Painter);
                    }
                    State.markJob(Zone, JobPainter);
                }
                break;
            }
            if (Plan.fixtures("painter", Zone).isEmpty()) {
                State.markJob(Zone, JobPainter);
            }
        }
        if (Zone == Scp1730State.DeepZone && !State.jobDone(Zone, JobGuardian) && !State.guardianDead()) {
            Scp1730Spot Spot = Plan.guardian();
            if (Spot != null) {
                Vec3 At = Scp1730World.world(Run, Spot);
                if (World.isPositionEntityTicking(BlockPos.containing(At))) {
                    Scp1730Guardian Guardian = Scp1730Registry.GuardianType.get().create(World);
                    if (Guardian != null) {
                        Guardian.moveTo(At.x, At.y + Scp1730Guardian.Hover, At.z, Spot.Yaw(), 0.0f);
                        Guardian.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(At)), MobSpawnType.EVENT, null);
                        Guardian.setHome(BlockPos.containing(At));
                        World.addFreshEntity(Guardian);
                    }
                    State.markJob(Zone, JobGuardian);
                }
            } else {
                State.markJob(Zone, JobGuardian);
            }
        }
        List<Scp1730Fixture> Pieces = Plan.fixtures("setpiece", Zone);
        for (int Index = 0; Index < Pieces.size() && Index < 48; Index++) {
            if (State.jobDone(Zone, JobSetPieceBase + Index)) {
                continue;
            }
            Scp1730Fixture Each = Pieces.get(Index);
            BlockPos At = Scp1730World.world(Run, Each);
            if (!World.isPositionEntityTicking(At)) {
                continue;
            }
            boolean Wing = Plan.inWing(Each.X(), Each.Y(), Each.Z());
            for (net.minecraft.world.entity.Mob Made : Scp1730Breach.spawnSetPiece(World, Scp1730Containment.setPiece(Zone, Each.Data(), Plan.seed(), Wing), Vec3.atBottomCenterOf(At),
                    Scp1730Plan.yawOf(Each.Facing()))) {
                if (Wing) {
                    Scp1730Containment.settle(Run, Plan.wingAt(Each.X(), Each.Y(), Each.Z()), Made);
                }
            }
            State.markJob(Zone, JobSetPieceBase + Index);
        }
    }

    public static void controlUsed(ServerLevel World, BlockPos Pos, Scp1730ControlBlock.Kind Kind, boolean Value, ServerPlayer User) {
        Scp1730State State = Scp1730State.get(World.getServer());
        int Run = State.run();
        int Zone = Scp1730World.zoneOf(Run, Pos);
        if (Zone < 0) {
            return;
        }
        switch (Kind) {
            case Reader, Release -> net.rai.scp_unleashed.scp1730.puzzle.Scp1730PuzzleDirector.objective(World, Run, Zone, Pos, Kind, Value, User);
            case Breaker -> {
                State.setPower(Zone, !Value);
                User.displayClientMessage(Component.translatable(Value ? "message.scp_unleashed.scp1730.power_cut" : "message.scp_unleashed.scp1730.power_on")
                        .withStyle(Value ? ChatFormatting.DARK_GRAY : ChatFormatting.YELLOW), true);
                net.rai.scp_unleashed.scp1730.puzzle.Scp1730PuzzleDirector.objective(World, Run, Zone, Pos, Kind, Value, User);
            }
        }
    }

    public static void openSeal(ServerLevel World, int Run, int Zone) {
        Scp1730State State = Scp1730State.get(World.getServer());
        if (State.open(Zone)) {
            return;
        }
        State.setOpen(Zone);
        List<Scp1730Fixture> Cells = new ArrayList<>(Scp1730World.plan(Run).fixtures("seal", Zone));
        if (Cells.isEmpty()) {
            return;
        }
        Cells.sort(Comparator.comparingInt(Scp1730Fixture::Y));
        long Now = World.getGameTime();
        int MinY = Cells.getFirst().Y();
        double Sx = 0.0;
        double Sy = 0.0;
        double Sz = 0.0;
        synchronized (Seals) {
            for (Scp1730Fixture Each : Cells) {
                BlockPos At = Scp1730World.world(Run, Each);
                Seals.add(new Pending(World, At, Now + (long) (Each.Y() - MinY) * SealStep + 4L));
                Sx += At.getX();
                Sy += At.getY();
                Sz += At.getZ();
            }
        }
        World.playSound(null, Sx / Cells.size(), Sy / Cells.size(), Sz / Cells.size(), Scp1730Registry.SealOpen.get(), SoundSource.BLOCKS, 2.0f, 0.8f);
    }

    private static void tickSeals() {
        synchronized (Seals) {
            if (Seals.isEmpty()) {
                return;
            }
            Iterator<Pending> Each = Seals.iterator();
            while (Each.hasNext()) {
                Pending Job = Each.next();
                if (Job.World().getGameTime() < Job.At()) {
                    continue;
                }
                BlockState Current = Job.World().getBlockState(Job.Pos());
                if (Current.getBlock() instanceof Scp1730SealBlock) {
                    Job.World().setBlock(Job.Pos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    Job.World().levelEvent(2001, Job.Pos(), Block.getId(Current));
                }
                Each.remove();
            }
        }
    }

    public static void clear() {
        synchronized (Seals) {
            Seals.clear();
        }
    }
}
