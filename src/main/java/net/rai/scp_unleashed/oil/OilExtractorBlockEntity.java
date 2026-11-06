package net.rai.scp_unleashed.oil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.pressure.PortRole;

public class OilExtractorBlockEntity extends OilMachineBlockEntity {
    public static final int CrudeTank = 0;
    public static final int WorkDraw = 64;
    public static final int StrokeTicks = 60;
    public static final int OilPerStroke = 240;
    public static final float PhaseStep = 1.0f / StrokeTicks;

    private final FluidTank Crude = addTank(Stack -> OilRegistry.Oil.is(Stack), PortRole.Output);
    private float Progress;
    private float Phase;
    private float PreviousPhase;

    public OilExtractorBlockEntity(BlockPos Pos, BlockState State) {
        super(OilRegistry.OilExtractorEntity.get(), Pos, State, OilMachineKind.Extractor);
    }

    public static boolean onBedrock(BlockGetter World, BlockPos WellPos) {
        return World.getBlockState(WellPos.below()).is(Blocks.BEDROCK);
    }

    public BlockPos getWellPos() {
        return OilExtractorBlock.Layout.cellPos(worldPosition, getFront(), OilExtractorBlock.WellPart);
    }

    public FluidTank getCrude() {
        return Crude;
    }

    @Override
    public float getProgress() {
        return Progress;
    }

    @Override
    protected float work(ServerLevel World, BlockPos Pos, BlockState State, long Now, int Delivered, boolean WasDemanding) {
        if (!onBedrock(World, getWellPos())) {
            Status = OilStatus.NeedsBedrock;
            return 0.0f;
        }
        if (Crude.getSpace() < OilPerStroke) {
            Status = OilStatus.OutputFull;
            return 0.0f;
        }
        float Share = consume(Now, WorkDraw, Delivered, WasDemanding);
        if (Share <= 0.0f) {
            return 0.0f;
        }
        Progress += Share / StrokeTicks;
        if (Progress >= 1.0f) {
            Progress -= 1.0f;
            Crude.fill(OilRegistry.Oil.stack(OilPerStroke), IFluidHandler.FluidAction.EXECUTE);
        }
        return Share;
    }

    @Override
    protected void tickClient() {
        super.tickClient();
        PreviousPhase = Phase;
        Phase += getChurn() * PhaseStep;
        if (Phase >= 1.0f) {
            Phase -= 1.0f;
            PreviousPhase -= 1.0f;
        }
    }

    public float getPhase(float PartialTick) {
        return PreviousPhase + (Phase - PreviousPhase) * PartialTick;
    }

    public float getPreviousPhase() {
        return PreviousPhase;
    }

    public float getCurrentPhase() {
        return Phase;
    }

    @Override
    protected void saveExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putFloat("Progress", Progress);
    }

    @Override
    protected void loadExtra(CompoundTag Tag, HolderLookup.Provider Registries) {
        Progress = Math.max(0.0f, Tag.getFloat("Progress"));
    }
}
