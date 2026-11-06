package net.rai.scp_unleashed.rifttech;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import org.jetbrains.annotations.Nullable;

public class PylonMenu extends AbstractContainerMenu {
    public static final int FirstTierButton = 0;
    public static final int ToggleButton = 5;
    public static final int ApocalypticButton = 6;

    @Nullable
    private final InstabilityPylonBlockEntity Pylon;
    private final ContainerData Data;

    public PylonMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null, new SimpleContainerData(InstabilityPylonBlockEntity.DataCount));
    }

    public PylonMenu(int ContainerId, Inventory PlayerInventory, @Nullable InstabilityPylonBlockEntity Pylon, ContainerData Data) {
        super(RiftTechRegistry.PylonMenuType.get(), ContainerId);
        checkContainerDataCount(Data, InstabilityPylonBlockEntity.DataCount);
        this.Pylon = Pylon;
        this.Data = Data;
        addDataSlots(Data);
    }

    private int get(int Index) {
        return Data.get(Index);
    }

    public GridStatus getStatus() {
        return GridStatus.byIndex(get(InstabilityPylonBlockEntity.DataStatus));
    }

    public PylonGrid.Problem getIssue() {
        return PylonGrid.Problem.byIndex(get(InstabilityPylonBlockEntity.DataIssue));
    }

    @Nullable
    public BlockPos getIssueOffset() {
        int X = (short) get(InstabilityPylonBlockEntity.DataIssueX);
        if (X == Short.MIN_VALUE) {
            return null;
        }
        return new BlockPos(X, (short) get(InstabilityPylonBlockEntity.DataIssueY), (short) get(InstabilityPylonBlockEntity.DataIssueZ));
    }

    public FragmentRarity getTier() {
        return FragmentRarity.byTier(get(InstabilityPylonBlockEntity.DataTier));
    }

    public boolean isEnabled() {
        return get(InstabilityPylonBlockEntity.DataEnabled) != 0;
    }

    public boolean isApocalyptic() {
        return get(InstabilityPylonBlockEntity.DataApocalyptic) != 0;
    }

    public float getCharge() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataCharge)) / (float) GridTuning.Scale;
    }

    public float getApocalypticCharge() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataApocalypticCharge)) / (float) GridTuning.Scale;
    }

    public int getIntake() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataIntake));
    }

    public int getGridDemand() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataGridDemand));
    }

    public int getSizeX() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataSizeX));
    }

    public int getSizeZ() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataSizeZ));
    }

    public int getRise() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataRise));
    }

    public int getCorners() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataCorners));
    }

    public int getRifts() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataRifts));
    }

    public int getCap() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataCap));
    }

    public int getEta() {
        return get(InstabilityPylonBlockEntity.DataEta);
    }

    public int getOwnReceived() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataOwnReceived));
    }

    public int getOwnDemand() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataOwnDemand));
    }

    public int getMask() {
        return get(InstabilityPylonBlockEntity.DataMask) & 0xFF;
    }

    public int getCornerIndex() {
        return get(InstabilityPylonBlockEntity.DataCornerIndex);
    }

    public int getFormed() {
        return Math.max(0, get(InstabilityPylonBlockEntity.DataFormed));
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        if (Pylon == null) {
            return false;
        }
        if (Id >= FirstTierButton && Id < FirstTierButton + FragmentRarity.values().length) {
            Pylon.selectTier(FragmentRarity.byTier(Id - FirstTierButton + 1));
            return true;
        }
        if (Id == ToggleButton) {
            Pylon.toggleEnabled();
            return true;
        }
        if (Id == ApocalypticButton) {
            Pylon.toggleApocalyptic();
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
        return Pylon == null || Pylon.isUsableBy(User);
    }
}
