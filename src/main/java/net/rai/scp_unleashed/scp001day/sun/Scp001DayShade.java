package net.rai.scp_unleashed.scp001day.sun;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp001DayShade {
    public static final TagKey<Block> Passes = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp001_day_sun_passes"));
    public static final TagKey<Block> Filters = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp001_day_sun_filters"));
    public static final int LeafPass = 191;
    public static final int Samples = 4;
    private static final int Unknown = -1;
    private static volatile int[] Table = new int[0];

    private Scp001DayShade() {
    }

    public static int cell(BlockState State) {
        int Id = Block.BLOCK_STATE_REGISTRY.getId(State);
        if (Id < 0) {
            return compute(State);
        }
        int[] Known = Table;
        if (Id >= Known.length) {
            Known = grow(Id + 1);
        }
        int Value = Known[Id];
        if (Value == Unknown) {
            Value = compute(State);
            Known[Id] = Value;
        }
        return Value;
    }

    public static synchronized void reset() {
        Table = new int[0];
    }

    private static synchronized int[] grow(int Needed) {
        int[] Known = Table;
        if (Known.length >= Needed) {
            return Known;
        }
        int Size = Math.max(Needed, Block.BLOCK_STATE_REGISTRY.size());
        int[] Next = Arrays.copyOf(Known, Size);
        Arrays.fill(Next, Known.length, Size, Unknown);
        Table = Next;
        return Next;
    }

    public static int compute(BlockState State) {
        if (State.isAir()) {
            return Scp001DayRay.Clear;
        }
        if (State.is(Passes) || (State.is(Tags.Blocks.GLASS_BLOCKS) && !State.is(Tags.Blocks.GLASS_BLOCKS_TINTED))) {
            return Scp001DayRay.Clear;
        }
        if (State.is(Filters) || State.is(BlockTags.LEAVES)) {
            return Scp001DayRay.cell(0, LeafPass);
        }
        VoxelShape Shape;
        try {
            Shape = State.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (RuntimeException Problem) {
            return State.canOcclude() ? Scp001DayRay.Solid : Scp001DayRay.Clear;
        }
        if (Shape.isEmpty()) {
            return Scp001DayRay.Clear;
        }
        if (Block.isShapeFullBlock(Shape)) {
            return Scp001DayRay.Solid;
        }
        return Scp001DayRay.cell(octants(Shape.toAabbs()), 255);
    }

    public static int octants(List<AABB> Boxes) {
        int Mask = 0;
        for (int Bit = 0; Bit < 8; Bit++) {
            double[] Low = {(Bit & 1) * 0.5, ((Bit >> 1) & 1) * 0.5, ((Bit >> 2) & 1) * 0.5};
            for (int Axis = 0; Axis < 3; Axis++) {
                if (screens(Boxes, Low, Axis)) {
                    Mask |= 1 << Bit;
                    break;
                }
            }
        }
        return Mask;
    }

    private static boolean screens(List<AABB> Boxes, double[] Low, int Axis) {
        int First = (Axis + 1) % 3;
        int Second = (Axis + 2) % 3;
        for (int I = 0; I < Samples; I++) {
            for (int J = 0; J < Samples; J++) {
                double U = Low[First] + (I + 0.5) * 0.5 / Samples;
                double V = Low[Second] + (J + 0.5) * 0.5 / Samples;
                boolean Hit = false;
                for (AABB Box : Boxes) {
                    if (within(Box, First, U) && within(Box, Second, V) && Box.max(axisOf(Axis)) > Low[Axis] + 1.0e-4 && Box.min(axisOf(Axis)) < Low[Axis] + 0.5 - 1.0e-4) {
                        Hit = true;
                        break;
                    }
                }
                if (!Hit) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean within(AABB Box, int Axis, double Value) {
        return Box.min(axisOf(Axis)) <= Value && Value <= Box.max(axisOf(Axis));
    }

    private static Direction.Axis axisOf(int Axis) {
        return Axis == 0 ? Direction.Axis.X : (Axis == 1 ? Direction.Axis.Y : Direction.Axis.Z);
    }
}
