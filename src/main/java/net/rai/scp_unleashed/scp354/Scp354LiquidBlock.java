package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Scp354LiquidBlock extends LiquidBlock {
    public static final String SinkKey = "Scp354Sinking";
    public static final int SinkTicks = 36;

    public Scp354LiquidBlock(FlowingFluid Liquid, Properties Settings) {
        super(Liquid, Settings);
    }

    @Override
    public ItemStack pickupBlock(@Nullable Player User, LevelAccessor World, BlockPos Pos, BlockState State) {
        return ItemStack.EMPTY;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.empty();
    }

    @Override
    protected boolean canBeReplaced(BlockState State, Fluid Incoming) {
        return false;
    }

    @Override
    protected void entityInside(BlockState State, Level World, BlockPos Pos, Entity Target) {
        super.entityInside(State, World, Pos, Target);
        if (!(World instanceof ServerLevel Server) || Target.isRemoved()) {
            return;
        }
        if (Target instanceof ItemEntity Dropped) {
            sink(Server, Dropped);
        } else if (Target instanceof LivingEntity Living) {
            Scp354Grab.touch(Server, Living, Pos);
        }
    }

    public static void sink(ServerLevel World, ItemEntity Dropped) {
        CompoundTag Data = Dropped.getPersistentData();
        long Now = World.getGameTime();
        if (Data.getLong(SinkKey + "At") == Now) {
            return;
        }
        Data.putLong(SinkKey + "At", Now);
        int Ticks = Data.getInt(SinkKey) + 1;
        Data.putInt(SinkKey, Ticks);
        Dropped.setPickUpDelay(10);
        if (Ticks % 8 == 0) {
            World.sendParticles(Scp354Registry.Bubble.get(), Dropped.getX(), Dropped.getY() + 0.1, Dropped.getZ(), 2, 0.1, 0.05, 0.1, 0.0);
        }
        if (Ticks >= SinkTicks) {
            World.sendParticles(Scp354Registry.Bubble.get(), Dropped.getX(), Dropped.getY() + 0.2, Dropped.getZ(), 4, 0.15, 0.1, 0.15, 0.0);
            Dropped.discard();
        }
    }
}
