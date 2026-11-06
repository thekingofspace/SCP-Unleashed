package net.rai.scp_unleashed.fluiddump;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class FluidDumpBlockEntity extends BlockEntity {
    public static final int Capacity = 4000;
    public static final int FlowPerTick = 50;
    public static final int Bucket = 1000;
    public static final int PourInterval = 10;
    public static final int HissGap = 10;
    public static final int HoldTicks = 30;
    public static final int RateWindow = 20;
    public static final double ForeignWeight = 0.25;

    private static Consumer<FluidDumpBlockEntity> ClientEffects = Dump -> {
    };

    private final FluidTank Buffer = new FluidTank(Capacity) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private DumpMode Mode = DumpMode.Idle;
    private Fluid Handling = Fluids.EMPTY;
    private long LastActive = Long.MIN_VALUE / 2;
    private long LastHiss = Long.MIN_VALUE / 2;
    private int Window;
    private int Rate;

    public FluidDumpBlockEntity(BlockPos Pos, BlockState State) {
        super(FluidDumpRegistry.FluidDumpEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<FluidDumpBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public FluidTank getBuffer() {
        return Buffer;
    }

    public DumpMode getMode() {
        return Mode;
    }

    public Fluid getHandling() {
        return Handling;
    }

    public int getRate() {
        return Rate;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, FluidDumpBlockEntity Dump) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof FluidDumpBlock) {
            Dump.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, FluidDumpBlockEntity Dump) {
        ClientEffects.accept(Dump);
    }

    public static int waterSides(Level World, BlockPos Pos) {
        int Count = 0;
        for (Direction Side : Direction.values()) {
            BlockPos Next = Pos.relative(Side);
            if (World.isLoaded(Next) && PollutionRules.isOpenWater(World.getBlockState(Next))) {
                Count++;
            }
        }
        return Count;
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        Fluid Liquid = Handling;
        DumpMode Next = null;
        if (!Buffer.isEmpty()) {
            FluidStack Held = Buffer.getFluid();
            Liquid = Held.getFluid();
            Next = waterSides(World, Pos) > 0 ? discharge(World, Pos, Now) : pour(World, Pos, Held, Now);
        }
        if (Next == null) {
            boolean Recent = Now - LastActive <= HoldTicks;
            if (Buffer.isEmpty()) {
                Next = Recent && Mode.isWorking() ? Mode : DumpMode.Idle;
            } else {
                Next = Recent || Mode == DumpMode.Blocked ? Mode : DumpMode.Idle;
            }
        } else if (Next.isWorking()) {
            LastActive = Now;
        }
        if (Now % RateWindow == 0) {
            Rate = Math.round(Window * 20.0f / RateWindow);
            Window = 0;
        }
        if (Next != Mode || !Liquid.isSame(Handling)) {
            Mode = Next;
            Handling = Liquid;
            setChanged();
            World.sendBlockUpdated(Pos, State, State, Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    private DumpMode discharge(ServerLevel World, BlockPos Pos, long Now) {
        if (Buffer.getFluid().is(FluidTags.LAVA)) {
            Window += Buffer.drain(Capacity, IFluidHandler.FluidAction.EXECUTE).getAmount();
            quench(World, Pos, Now);
            return DumpMode.Quenching;
        }
        FluidStack Out = Buffer.drain(FlowPerTick, IFluidHandler.FluidAction.EXECUTE);
        if (Out.isEmpty()) {
            return null;
        }
        Window += Out.getAmount();
        if (Out.is(FluidTags.WATER)) {
            return DumpMode.Absorbing;
        }
        double Weight = Out.is(FluidDumpTags.Pollutant) ? 1.0 : ForeignWeight;
        PollutionSource Fed = PollutionField.get(World).feed(World, Pos, Out.getAmount() * Weight / Bucket);
        return Fed == null ? DumpMode.Absorbing : DumpMode.Polluting;
    }

    private void quench(ServerLevel World, BlockPos Pos, long Now) {
        RandomSource Random = World.getRandom();
        int Formed = 0;
        for (Direction Side : Direction.values()) {
            BlockPos Next = Pos.relative(Side);
            if (!World.isLoaded(Next)) {
                continue;
            }
            BlockState Old = World.getBlockState(Next);
            if (!PollutionRules.isOpenWater(Old)) {
                continue;
            }
            BlockState Stone = EventHooks.fireFluidPlaceBlockEvent(World, Next, Pos, Blocks.COBBLESTONE.defaultBlockState());
            if (Stone == Old) {
                continue;
            }
            World.setBlockAndUpdate(Next, Stone);
            World.levelEvent(LevelEvent.LAVA_FIZZ, Next, 0);
            World.sendParticles(ParticleTypes.CLOUD, Next.getX() + 0.5, Next.getY() + 0.9, Next.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0.02);
            Formed++;
        }
        if (Formed > 0 && Now - LastHiss >= HissGap) {
            LastHiss = Now;
            World.playSound(null, Pos, FluidDumpRegistry.DumpHiss.get(), SoundSource.BLOCKS, 1.0f, 0.85f + Random.nextFloat() * 0.2f);
        }
    }

    @Nullable
    private DumpMode pour(ServerLevel World, BlockPos Pos, FluidStack Held, long Now) {
        Fluid Still = DryPour.still(Held.getFluid());
        BlockState Placed = Still.defaultFluidState().createLegacyBlock();
        if (Placed.isAir() || PollutionRules.isProtected(World)) {
            return DumpMode.Blocked;
        }
        if (Held.getAmount() < Bucket || Math.floorMod(Now + Pos.asLong(), PourInterval) != 0) {
            return null;
        }
        BlockPos Target = DryPour.find(World, Pos, Still);
        if (Target == null) {
            return DumpMode.Blocked;
        }
        FluidStack Poured = Buffer.drain(Bucket, IFluidHandler.FluidAction.EXECUTE);
        Window += Poured.getAmount();
        FluidType Kind = Still.getFluidType();
        if (Kind.isVaporizedOnPlacement(World, Target, Poured)) {
            Kind.onVaporize(null, World, Target, Poured);
            return DumpMode.Pouring;
        }
        BlockState Old = World.getBlockState(Target);
        if (!Old.isAir() && Old.getFluidState().isEmpty()) {
            World.destroyBlock(Target, true);
        }
        World.setBlock(Target, Placed, Block.UPDATE_ALL_IMMEDIATE);
        World.gameEvent(null, GameEvent.FLUID_PLACE, Target);
        SoundEvent Splash = Kind.getSound(SoundActions.BUCKET_EMPTY);
        RandomSource Random = World.getRandom();
        World.playSound(null, Target, Splash == null ? SoundEvents.BUCKET_EMPTY : Splash, SoundSource.BLOCKS, 0.7f, 0.85f + Random.nextFloat() * 0.15f);
        World.playSound(null, Pos, FluidDumpRegistry.DumpPour.get(), SoundSource.BLOCKS, 0.6f, 0.9f + Random.nextFloat() * 0.15f);
        return DumpMode.Pouring;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Buffer", Buffer.writeToNBT(Registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Mode")) {
            Mode = DumpMode.byIndex(Tag.getByte("Mode"));
            ResourceLocation Id = ResourceLocation.tryParse(Tag.getString("Handling"));
            Handling = Id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(Id);
            return;
        }
        if (Tag.contains("Buffer")) {
            Buffer.readFromNBT(Registries, Tag.getCompound("Buffer"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Mode", (byte) Mode.ordinal());
        Tag.putString("Handling", BuiltInRegistries.FLUID.getKey(Handling).toString());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
