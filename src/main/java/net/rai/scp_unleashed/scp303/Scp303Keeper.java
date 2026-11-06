package net.rai.scp_unleashed.scp303;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Scp303Keeper {
    public static final int Interval = 20;
    public static final int CellGrace = 100;
    public static final int MissingGrace = 200;
    public static final int RetryTicks = 100;
    public static final double ReturnMin = 16.0;
    public static final double ReturnMax = 28.0;
    public static final double SignRange = 8.0;
    public static final float SignChance = 0.3f;
    public static final DustParticleOptions Sign = new DustParticleOptions(new Vector3f(0.24f, 0.72f, 0.64f), 0.45f);
    private static int CellTicks;
    private static int MissingTicks;
    private static boolean Announced;

    private Scp303Keeper() {
    }

    public static void tick(MinecraftServer Server) {
        ServerLevel World = Server.overworld();
        if (World.getGameTime() % Interval != 0) {
            return;
        }
        Scp303State State = Scp303State.get(Server);
        announce(Server, State.isActive());
        if (!Scp303Arrival.realm(World)) {
            return;
        }
        if (!State.isActive()) {
            CellTicks = 0;
            MissingTicks = 0;
            return;
        }
        Scp303Doorman Present = Scp303Arrival.loaded(World);
        Optional<Scp303State.Binding> Bound = State.bound();
        if (Bound.isPresent()) {
            MissingTicks = 0;
            keepCell(World, State, Bound.get(), Present);
            return;
        }
        CellTicks = 0;
        if (Present != null) {
            MissingTicks = 0;
            return;
        }
        if (!State.isAway()) {
            MissingTicks += Interval;
            if (MissingTicks >= MissingGrace) {
                MissingTicks = 0;
                State.leave(World.getGameTime());
            }
            return;
        }
        if (World.getGameTime() < State.returnAt()) {
            return;
        }
        if (!comeBack(World)) {
            State.delay(World.getGameTime() + RetryTicks);
        }
    }

    private static void announce(MinecraftServer Server, boolean Active) {
        if (Active == Announced) {
            return;
        }
        Announced = Active;
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            Scp303Unlock.sync(User);
        }
    }

    public static boolean comeBack(ServerLevel World) {
        List<LivingEntity> Choices = new ArrayList<>();
        for (ServerPlayer Candidate : World.players()) {
            if (Scp303Doorman.quarry(Candidate, World)) {
                Choices.add(Candidate);
            }
        }
        if (Choices.isEmpty()) {
            Choices.addAll(Scp303Doorman.people(World));
        }
        if (Choices.isEmpty()) {
            return false;
        }
        LivingEntity Target = Choices.get(World.getRandom().nextInt(Choices.size()));
        Optional<Vec3> Spot = Scp303Spots.hidden(World, Target, ReturnMin, ReturnMax, World.getRandom(), null, true, false);
        if (Spot.isEmpty()) {
            return false;
        }
        Vec3 Toward = Target.position().subtract(Spot.get());
        float Yaw = (float) (Math.atan2(Toward.z, Toward.x) * 180.0 / Math.PI) - 90.0f;
        return Scp303Arrival.place(World, Spot.get(), Yaw, Target.getUUID()).isPresent();
    }

    private static void keepCell(ServerLevel World, Scp303State State, Scp303State.Binding Binding, Scp303Doorman Present) {
        ServerLevel Cell = World.getServer().getLevel(Binding.Dimension());
        BlockPos Door = Binding.Door();
        if (Cell == null || !Cell.isLoaded(Door)) {
            CellTicks = 0;
            return;
        }
        if (!Scp303Doors.usable(Cell, Door)) {
            State.unbind();
            if (Present != null) {
                Present.freed(World, null);
            }
            return;
        }
        sign(Cell, Door);
        if (Present != null) {
            CellTicks = 0;
            return;
        }
        CellTicks += Interval;
        if (CellTicks < CellGrace) {
            return;
        }
        Optional<Vec3> Spot = Scp303Doors.cellIdle(Cell, Door, Binding.CellSide());
        if (Spot.isEmpty()) {
            Spot = Scp303Doors.hold(Cell, Door, Binding.CellSide());
        }
        if (Spot.isEmpty() || Scp303Sight.watched(Cell, Spot.get(), Scp303Doorman.Height)) {
            return;
        }
        CellTicks = 0;
        BlockState DoorState = Cell.getBlockState(Door);
        Scp303Arrival.place(Cell, Spot.get(), Scp303Doors.facingYaw(DoorState, Binding.CellSide()), null);
    }

    private static void sign(ServerLevel World, BlockPos Door) {
        if (World.getRandom().nextFloat() > SignChance || World.getNearestPlayer(Door.getX() + 0.5, Door.getY() + 1.0, Door.getZ() + 0.5, SignRange, false) == null) {
            return;
        }
        Vec3 Handle = Scp303Doors.handle(Door, World.getBlockState(Door));
        World.sendParticles(Sign, Handle.x, Handle.y + 0.05, Handle.z, 1, 0.05, 0.08, 0.05, 0.0);
    }
}
