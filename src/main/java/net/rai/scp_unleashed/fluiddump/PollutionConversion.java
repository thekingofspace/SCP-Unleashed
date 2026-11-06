package net.rai.scp_unleashed.fluiddump;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.fission.FissionRegistry;

import java.util.HashMap;
import java.util.Map;

public final class PollutionConversion {
    public static final int SurveyCap = 4096;
    public static final int SurveyBudget = 512;
    public static final int ConvertBudget = 48;
    public static final long RetryGap = 1200L;
    public static final long AfterConversion = 2400L;
    public static final int EffectEvery = 12;
    private static final Map<ResourceKey<Level>, PollutionConversion> Active = new HashMap<>();

    private final PollutionSource Source;
    private final BlockPos Centre;
    private final LongArrayFIFOQueue Queue = new LongArrayFIFOQueue();
    private final LongOpenHashSet Seen = new LongOpenHashSet();
    private final LongArrayList Body = new LongArrayList();
    private boolean Surveying = true;
    private int Cursor;
    private int Pass;
    private int Converted;

    private PollutionConversion(PollutionSource Source) {
        this.Source = Source;
        this.Centre = Source.getCentre();
    }

    public static boolean busy(ServerLevel World) {
        return Active.containsKey(World.dimension());
    }

    public static void forget() {
        Active.clear();
    }

    public static void start(ServerLevel World, PollutionSource Source) {
        Source.setNextSurvey(World.getGameTime() + RetryGap);
        if (PollutionRules.isProtected(World)) {
            return;
        }
        PollutionConversion Job = new PollutionConversion(Source);
        if (!Job.seed(World)) {
            return;
        }
        Active.put(World.dimension(), Job);
    }

    public static void tick(ServerLevel World) {
        PollutionConversion Job = Active.get(World.dimension());
        if (Job == null) {
            return;
        }
        PollutionField Field = PollutionField.get(World);
        if (!Field.getSources().contains(Job.Source)) {
            Active.remove(World.dimension());
            return;
        }
        boolean Finished = Job.Surveying ? Job.survey(World, Field) : Job.convert(World);
        if (Finished) {
            Active.remove(World.dimension());
        }
    }

    private boolean seed(ServerLevel World) {
        Seen.add(Centre.asLong());
        if (!World.isLoaded(Centre)) {
            return false;
        }
        if (PollutionRules.isWater(World.getBlockState(Centre).getFluidState())) {
            Queue.enqueue(Centre.asLong());
        }
        for (Direction Side : Direction.values()) {
            BlockPos Next = Centre.relative(Side);
            if (!World.isLoaded(Next)) {
                return false;
            }
            Seen.add(Next.asLong());
            if (PollutionRules.isWater(World.getBlockState(Next).getFluidState())) {
                Queue.enqueue(Next.asLong());
            }
        }
        return !Queue.isEmpty();
    }

    private boolean survey(ServerLevel World, PollutionField Field) {
        BlockPos.MutableBlockPos Next = new BlockPos.MutableBlockPos();
        for (int Step = 0; Step < SurveyBudget; Step++) {
            if (Queue.isEmpty()) {
                return begin(World);
            }
            long Packed = Queue.dequeueLong();
            BlockPos Pos = BlockPos.of(Packed);
            if (Field.densityAt(Pos) < PollutionMath.CoverDensity) {
                return true;
            }
            Body.add(Packed);
            for (Direction Side : Direction.values()) {
                Next.setWithOffset(Pos, Side);
                long Key = Next.asLong();
                if (!Seen.add(Key)) {
                    continue;
                }
                if (!World.isLoaded(Next)) {
                    return true;
                }
                if (PollutionRules.isWater(World.getBlockState(Next).getFluidState())) {
                    if (Body.size() + Queue.size() >= SurveyCap) {
                        return true;
                    }
                    Queue.enqueue(Key);
                }
            }
        }
        return false;
    }

    private boolean begin(ServerLevel World) {
        if (Body.isEmpty()) {
            return true;
        }
        long[] Order = Body.toLongArray();
        LongArrays.quickSort(Order, (First, Second) -> Long.compare(distance(First), distance(Second)));
        Body.clear();
        Body.addElements(0, Order);
        Surveying = false;
        Cursor = 0;
        Pass = 0;
        World.playSound(null, Centre, FluidDumpRegistry.DumpGurgle.get(), SoundSource.BLOCKS, 1.4f, 0.6f);
        return false;
    }

    private boolean convert(ServerLevel World) {
        BlockState Waste = FissionRegistry.WasteWater.get().defaultFluidState().createLegacyBlock();
        for (int Step = 0; Step < ConvertBudget; Step++) {
            if (Cursor >= Body.size()) {
                Pass++;
                Cursor = 0;
                if (Pass >= 2) {
                    Source.setNextSurvey(World.getGameTime() + AfterConversion);
                    return true;
                }
            }
            BlockPos Pos = BlockPos.of(Body.getLong(Cursor++));
            if (!World.isLoaded(Pos)) {
                continue;
            }
            BlockState State = World.getBlockState(Pos);
            if (!convertible(State)) {
                continue;
            }
            World.setBlock(Pos, Waste, Block.UPDATE_ALL);
            Converted++;
            if (Converted % EffectEvery == 0) {
                World.sendParticles(ParticleTypes.BUBBLE_POP, Pos.getX() + 0.5, Pos.getY() + 0.9, Pos.getZ() + 0.5, 4, 0.3, 0.05, 0.3, 0.01);
            }
        }
        return false;
    }

    private long distance(long Packed) {
        long Dx = BlockPos.getX(Packed) - Centre.getX();
        long Dy = BlockPos.getY(Packed) - Centre.getY();
        long Dz = BlockPos.getZ(Packed) - Centre.getZ();
        return Dx * Dx + Dy * Dy + Dz * Dz;
    }

    public static boolean convertible(BlockState State) {
        if (!State.getFluidState().isSourceOfType(Fluids.WATER)) {
            return false;
        }
        return State.is(Blocks.WATER) || State.is(Blocks.BUBBLE_COLUMN) || State.is(Blocks.KELP) || State.is(Blocks.KELP_PLANT)
                || State.is(Blocks.SEAGRASS) || State.is(Blocks.TALL_SEAGRASS);
    }
}
