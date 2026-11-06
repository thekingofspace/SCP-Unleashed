package net.rai.scp_unleashed.solar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.wire.WireDevice;

public class SolarBlockEntity extends BlockEntity implements WireDevice {
    public static final int RefreshTicks = 20;
    public static final float GeneratingThreshold = 0.05f;
    public static final float Ease = 0.12f;
    public static final float MaxStep = 0.9f;
    public static final float SnapStep = 0.02f;

    private final SolarTier Tier;
    private int Stored;
    private float Carry;
    private float Rate;
    private float Sun;
    private float Weather = 1.0f;
    private float Exposure;
    private boolean Day;
    private long NextRefresh = Long.MIN_VALUE;
    private boolean Primed;
    private float Tilt;
    private float PreviousTilt;

    public SolarBlockEntity(SolarTier Tier, BlockPos Pos, BlockState State) {
        super(Tier.entityType(), Pos, State);
        this.Tier = Tier;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, SolarBlockEntity Collector) {
        if (World instanceof ServerLevel ServerWorld) {
            Collector.tickServer(ServerWorld, Pos);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, SolarBlockEntity Collector) {
        Collector.tickClient(World);
    }

    private void tickServer(ServerLevel World, BlockPos Pos) {
        long Now = World.getGameTime();
        if (Now >= NextRefresh) {
            NextRefresh = Now + RefreshTicks;
            refresh(World, Pos);
        }
        if (Rate > 0.0f && Stored < Tier.getCapacity()) {
            Carry += Rate;
            int Whole = (int) Carry;
            Carry -= Whole;
            if (Whole > 0) {
                Stored = Math.min(Tier.getCapacity(), Stored + Whole);
                setChanged();
            }
        }
    }

    private void refresh(ServerLevel World, BlockPos Pos) {
        Day = SolarSun.isDay(World);
        Sun = SolarSun.sunStrength(World);
        Weather = SolarSun.weather(World, Pos.above());
        int Open = 0;
        for (BlockPos Offset : Tier.getCells()) {
            if (World.canSeeSky(Pos.offset(Offset).above())) {
                Open++;
            }
        }
        Exposure = (float) Open / Tier.getCells().size();
        Rate = Tier.getPeakOutput() * Sun * Weather * Exposure;
        boolean Generating = Rate >= GeneratingThreshold;
        for (BlockPos Offset : Tier.getCells()) {
            BlockPos Cell = Pos.offset(Offset);
            BlockState CellState = World.getBlockState(Cell);
            BooleanProperty Flag = generatingProperty(CellState);
            if (Flag != null && CellState.getValue(Flag) != Generating) {
                World.setBlock(Cell, CellState.setValue(Flag, Generating), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static BooleanProperty generatingProperty(BlockState State) {
        if (State.getBlock() instanceof SolarPanelBlock) {
            return SolarPanelBlock.Generating;
        }
        if (State.getBlock() instanceof SolarArrayBlock) {
            return SolarArrayBlock.Generating;
        }
        return null;
    }

    private void tickClient(Level World) {
        float Target = SolarSun.trackingTilt(World, 1.0f);
        if (!Primed) {
            Primed = true;
            Tilt = Target;
            PreviousTilt = Target;
            return;
        }
        PreviousTilt = Tilt;
        float Step = Mth.clamp((Target - Tilt) * Ease, -MaxStep, MaxStep);
        if (Math.abs(Step) < SnapStep) {
            Tilt = Target;
        } else {
            Tilt += Step;
        }
    }

    public float getRenderTilt(float PartialTick) {
        return Mth.lerp(PartialTick, PreviousTilt, Tilt);
    }

    public SolarTier getTier() {
        return Tier;
    }

    public float getRate() {
        return Rate;
    }

    public float getEfficiency() {
        return Sun * Weather * Exposure;
    }

    public float getSun() {
        return Sun;
    }

    public float getWeather() {
        return Weather;
    }

    public float getExposure() {
        return Exposure;
    }

    public boolean isDay() {
        return Day;
    }

    public AABB getRenderBounds() {
        return Tier == SolarTier.Array ? new AABB(worldPosition).expandTowards(1.0, 0.0, 1.0) : new AABB(worldPosition);
    }

    @Override
    public int getPowerOutput() {
        return Math.min(Stored, Tier.getPeakOutput());
    }

    @Override
    public int getReserveOutput() {
        return WireDevice.bufferedReserve(this);
    }

    @Override
    public boolean isGenerator() {
        return true;
    }

    @Override
    public int getGenerationRate() {
        return Math.round(Rate);
    }

    @Override
    public void extractPower(int Amount) {
        Stored = Math.max(0, Stored - Amount);
        setChanged();
    }

    @Override
    public int getStoredPower() {
        return Stored;
    }

    @Override
    public int getPowerCapacity() {
        return Tier.getCapacity();
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Stored", Stored);
        Tag.putFloat("Carry", Carry);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Stored = Mth.clamp(Tag.getInt("Stored"), 0, Tier.getCapacity());
        Carry = Mth.clamp(Tag.getFloat("Carry"), 0.0f, 1.0f);
        if (Tag.contains("Rate")) {
            Rate = Math.max(0.0f, Tag.getFloat("Rate"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putFloat("Rate", Rate);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
