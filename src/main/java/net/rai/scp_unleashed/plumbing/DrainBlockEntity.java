package net.rai.scp_unleashed.plumbing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.fission.FissionTags;

import java.util.List;
import java.util.function.Consumer;

public class DrainBlockEntity extends BlockEntity {
    public static final int Capacity = 250;
    public static final int FlowPerTick = 25;
    public static final int MaxReach = 4;
    public static final double HalfWidth = 0.6;
    public static final int WashInterval = 10;
    public static final int FlowHold = 20;
    public static final int RateWindow = 20;
    public static final float WasteDamage = 1.0f;

    private static Consumer<DrainBlockEntity> ClientEffects = Drain -> {
    };

    private final FluidTank Buffer = new FluidTank(Capacity) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private Fluid Pouring = Fluids.EMPTY;
    private long LastPoured = Long.MIN_VALUE / 2;
    private int Window;
    private int Rate;

    public DrainBlockEntity(BlockPos Pos, BlockState State) {
        super(PlumbingRegistry.DrainEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<DrainBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public FluidTank getBuffer() {
        return Buffer;
    }

    public Fluid getPouring() {
        return Pouring;
    }

    public int getRate() {
        return Rate;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, DrainBlockEntity Drain) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof DrainBlock) {
            Drain.tickServer(ServerWorld, Pos, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, DrainBlockEntity Drain) {
        ClientEffects.accept(Drain);
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        if (!Buffer.isEmpty()) {
            FluidStack Out = Buffer.drain(FlowPerTick, IFluidHandler.FluidAction.EXECUTE);
            if (!Out.isEmpty()) {
                LastPoured = Now;
                Window += Out.getAmount();
                if (!Out.getFluid().isSame(Pouring)) {
                    Pouring = Out.getFluid();
                    World.sendBlockUpdated(Pos, State, State, Block.UPDATE_CLIENTS);
                }
                if (Math.floorMod(Now + Pos.asLong(), WashInterval) == 0) {
                    affect(World, Pos, Out);
                }
            }
        }
        if (Now % RateWindow == 0) {
            Rate = Math.round(Window * 20.0f / RateWindow);
            Window = 0;
        }
        boolean Flowing = Now - LastPoured <= FlowHold;
        if (State.getValue(DrainBlock.Flowing) != Flowing) {
            World.setBlock(Pos, State.setValue(DrainBlock.Flowing, Flowing), Block.UPDATE_CLIENTS);
        }
    }

    public static int reach(Level World, BlockPos Pos) {
        for (int Depth = 1; Depth <= MaxReach; Depth++) {
            BlockPos Below = Pos.below(Depth);
            if (!World.isLoaded(Below) || World.getBlockState(Below).blocksMotion()) {
                return Depth - 1;
            }
        }
        return MaxReach;
    }

    public static AABB area(BlockPos Pos, int Reach) {
        double CentreX = Pos.getX() + 0.5;
        double CentreZ = Pos.getZ() + 0.5;
        return new AABB(CentreX - HalfWidth, Pos.getY() - Reach, CentreZ - HalfWidth, CentreX + HalfWidth, Pos.getY(), CentreZ + HalfWidth);
    }

    private void affect(ServerLevel World, BlockPos Pos, FluidStack Out) {
        int Reach = reach(World, Pos);
        if (Reach <= 0) {
            return;
        }
        List<Entity> Targets = World.getEntities((Entity) null, area(Pos, Reach), Target -> Target.isAlive() && !Target.isSpectator());
        if (Targets.isEmpty()) {
            return;
        }
        FluidStack Sample = Out.copyWithAmount(Out.getAmount() * WashInterval);
        for (Entity Target : Targets) {
            if (Out.is(FissionTags.WasteWater)) {
                if (Target instanceof LivingEntity Living) {
                    DamageSource Source = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(PlumbingRegistry.WasteWaterDamage));
                    Living.hurt(Source, WasteDamage);
                }
            } else if (Out.is(FluidTags.LAVA)) {
                Target.lavaHurt();
            } else if (Out.is(FluidTags.WATER)) {
                EntityWashedEvent.wash(Target, World, Pos, Sample, 1);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Buffer", Buffer.writeToNBT(Registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Pouring")) {
            ResourceLocation Id = ResourceLocation.tryParse(Tag.getString("Pouring"));
            Pouring = Id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(Id);
            return;
        }
        if (Tag.contains("Buffer")) {
            Buffer.readFromNBT(Registries, Tag.getCompound("Buffer"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putString("Pouring", BuiltInRegistries.FLUID.getKey(Pouring).toString());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
