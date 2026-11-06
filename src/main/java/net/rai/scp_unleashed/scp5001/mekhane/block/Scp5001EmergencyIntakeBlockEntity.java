package net.rai.scp_unleashed.scp5001.mekhane.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneIntake;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneValues;
import net.rai.scp_unleashed.scp5001.mekhane.Scp5001MekhaneRegistry;

public class Scp5001EmergencyIntakeBlockEntity extends MekhaneStatusBlockEntity {
    private final IItemHandler Handler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int Slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int Slot, ItemStack Stack, boolean Simulate) {
            MinecraftServer Server = level == null ? null : level.getServer();
            if (Server == null || Stack.isEmpty()) {
                return Stack;
            }
            int Taken = MekhaneIntake.accept(Server, Stack, Simulate);
            if (Taken <= 0) {
                return Stack;
            }
            if (!Simulate) {
                fed();
            }
            return Stack.copyWithCount(Stack.getCount() - Taken);
        }

        @Override
        public ItemStack extractItem(int Slot, int Amount, boolean Simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int Slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return MekhaneValues.worth(Stack) != null;
        }
    };
    private long LastSound;

    public Scp5001EmergencyIntakeBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp5001MekhaneRegistry.IntakeEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp5001EmergencyIntakeBlockEntity Intake) {
        Intake.refresh(Scp5001EmergencyIntakeBlock.Awake);
    }

    public IItemHandler handler() {
        return Handler;
    }

    public void fed() {
        if (level == null || level.isClientSide) {
            return;
        }
        long Now = level.getGameTime();
        if (Now - LastSound >= 6) {
            LastSound = Now;
            level.playSound(null, worldPosition, Scp5001MekhaneRegistry.IntakeFeed.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.7f, 0.8f + level.random.nextFloat() * 0.2f);
        }
    }
}
