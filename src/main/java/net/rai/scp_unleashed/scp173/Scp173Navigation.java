package net.rai.scp_unleashed.scp173;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Scp173Navigation extends GroundPathNavigation {
    public Scp173Navigation(Mob Walker, Level World) {
        super(Walker, World);
    }

    @Override
    protected PathFinder createPathFinder(int MaxVisited) {
        nodeEvaluator = new Nodes();
        nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(nodeEvaluator, MaxVisited);
    }

    public static final class Nodes extends WalkNodeEvaluator {
        private static final Vec3 Nowhere = new Vec3(Double.NaN, Double.NaN, Double.NaN);
        private final Long2ObjectOpenHashMap<Vec3> Stands = new Long2ObjectOpenHashMap<>();
        @Nullable
        private Scp173Passage Way;
        @Nullable
        private Node Origin;

        @Override
        public void prepare(PathNavigationRegion Region, Mob Walker) {
            super.prepare(Region, Walker);
            Way = Scp173Passage.remembering(Walker.level());
            Stands.clear();
            Origin = null;
        }

        @Override
        public void done() {
            Way = null;
            Origin = null;
            Stands.clear();
            super.done();
        }

        @Override
        public Node getStart() {
            Node Found = super.getStart();
            Origin = Found;
            return Found;
        }

        @Nullable
        private Vec3 stand(Node At) {
            if (Way == null) {
                return null;
            }
            long Key = BlockPos.asLong(At.x, At.y, At.z);
            Vec3 Found = Stands.get(Key);
            if (Found == null) {
                Vec3 Measured = Scp173Climb.stand(Way, new BlockPos(At.x, At.y, At.z));
                Found = Measured == null ? Nowhere : Measured;
                Stands.put(Key, Found);
            }
            return Found == Nowhere ? null : Found;
        }

        @Override
        public int getNeighbors(Node[] Out, Node From) {
            int Count = super.getNeighbors(Out, From);
            if (Way == null) {
                return 0;
            }
            Vec3 Leave = From == Origin ? mob.position() : stand(From);
            if (Leave == null) {
                return 0;
            }
            int Kept = 0;
            for (int Index = 0; Index < Count; Index++) {
                Node Next = Out[Index];
                Vec3 Arrive = stand(Next);
                if (Arrive != null && Way.passes(Leave, Arrive)) {
                    Out[Kept++] = Next;
                }
            }
            return Kept;
        }
    }
}
