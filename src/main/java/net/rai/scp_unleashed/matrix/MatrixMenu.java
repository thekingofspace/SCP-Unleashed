package net.rai.scp_unleashed.matrix;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import org.jetbrains.annotations.Nullable;

public class MatrixMenu extends AbstractContainerMenu {
    public static final int FirstTierButton = 0;
    public static final int ToggleButton = 5;

    @Nullable
    private final InstabilityMatrixBlockEntity Matrix;
    private final ContainerData Data;

    public MatrixMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new SimpleContainerData(InstabilityMatrixBlockEntity.DataCount));
    }

    public MatrixMenu(int ContainerId, Inventory PlayerInventory, @Nullable InstabilityMatrixBlockEntity Matrix, ContainerData Data) {
        super(MatrixRegistry.MatrixMenuType.get(), ContainerId);
        checkContainerDataCount(Data, InstabilityMatrixBlockEntity.DataCount);
        this.Matrix = Matrix;
        this.Data = Data;
        addDataSlots(Data);
    }

    public FragmentRarity getTier() {
        return FragmentRarity.byTier(Data.get(InstabilityMatrixBlockEntity.DataTier));
    }

    public boolean isEnabled() {
        return Data.get(InstabilityMatrixBlockEntity.DataEnabled) != 0;
    }

    public MatrixStatus getStatus() {
        return MatrixStatus.byIndex(Data.get(InstabilityMatrixBlockEntity.DataStatus));
    }

    public int getReceived() {
        return Math.max(0, Data.get(InstabilityMatrixBlockEntity.DataReceived));
    }

    public int getDemand() {
        return Math.max(0, Data.get(InstabilityMatrixBlockEntity.DataDemand));
    }

    public float getResonance() {
        return Math.min(1.0f, Math.max(0, Data.get(InstabilityMatrixBlockEntity.DataResonance)) / (float) InstabilityMatrixBlockEntity.Scale);
    }

    public float getFlow() {
        return Math.min(1.0f, Math.max(0, Data.get(InstabilityMatrixBlockEntity.DataFlow)) / (float) InstabilityMatrixBlockEntity.Scale);
    }

    public int getNearby() {
        return Math.max(0, Data.get(InstabilityMatrixBlockEntity.DataNearby));
    }

    public int getSecondsSinceFormed() {
        return Data.get(InstabilityMatrixBlockEntity.DataSince);
    }

    public int getFormed() {
        return Math.max(0, Data.get(InstabilityMatrixBlockEntity.DataFormed));
    }

    public int getBlipOffset(int Index) {
        return Data.get(InstabilityMatrixBlockEntity.DataBlips + Index * 2);
    }

    public int getBlipTier(int Index) {
        return Data.get(InstabilityMatrixBlockEntity.DataBlips + Index * 2 + 1);
    }

    public float getLiveRatio() {
        MatrixStatus Status = getStatus();
        if (!Status.isRunning() && Status != MatrixStatus.Unpowered) {
            return 0.0f;
        }
        int Draw = MatrixTuning.drawFor(getTier());
        return Draw <= 0 ? 0.0f : Math.min(1.0f, getReceived() / (float) Draw);
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        if (Matrix == null) {
            return false;
        }
        if (Id >= FirstTierButton && Id < FirstTierButton + FragmentRarity.values().length) {
            Matrix.selectTier(FragmentRarity.byTier(Id - FirstTierButton + 1));
            return true;
        }
        if (Id == ToggleButton) {
            Matrix.toggle();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player User) {
        return Matrix == null || Matrix.isUsableBy(User);
    }
}
