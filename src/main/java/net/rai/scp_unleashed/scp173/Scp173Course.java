package net.rai.scp_unleashed.scp173;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp173Course {
    public static final byte Walk = 0;
    public static final byte Climb = 1;
    public static final byte Drop = 2;
    private final List<Step> Steps;
    private final BlockPos Goal;
    private final boolean Climbing;
    private int Index;

    public record Step(Vec3 Where, BlockPos Cell, byte Kind, @Nullable Direction Wall) {
    }

    public Scp173Course(List<Step> Steps, BlockPos Goal, boolean Climbing) {
        this.Steps = List.copyOf(Steps);
        this.Goal = Goal;
        this.Climbing = Climbing;
    }

    public static Scp173Course walk(ServerLevel World, Path Route) {
        List<Step> Found = new ArrayList<>();
        Scp173Passage Way = Scp173Passage.remembering(World);
        for (int Slot = Math.max(1, Route.getNextNodeIndex()); Slot < Route.getNodeCount(); Slot++) {
            Node Next = Route.getNode(Slot);
            BlockPos Cell = Next.asBlockPos();
            Vec3 Where = Scp173Climb.stand(Way, Cell);
            Found.add(new Step(Where != null ? Where : new Vec3(Next.x + 0.5, WalkNodeEvaluator.getFloorLevel(World, Cell), Next.z + 0.5), Cell, Walk, null));
        }
        Node End = Route.getEndNode();
        return new Scp173Course(Found, End != null ? End.asBlockPos() : Route.getTarget(), false);
    }

    public boolean isDone() {
        return Index >= Steps.size();
    }

    public Step next() {
        return Steps.get(Index);
    }

    public void advance() {
        Index++;
    }

    public BlockPos goal() {
        return Goal;
    }

    public boolean climbing() {
        return Climbing;
    }

    public int remaining() {
        return Steps.size() - Index;
    }
}
