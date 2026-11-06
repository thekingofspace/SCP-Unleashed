package net.rai.scp_unleashed.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.reactor.network.ReactorSnapshotPayload;
import org.jetbrains.annotations.Nullable;

public class ReactorControllerMenu extends AbstractContainerMenu {
    public static final int SnapshotTicks = 10;

    @Nullable
    private final ReactorControllerBlockEntity Controller;
    private final Player Viewer;
    private int Ticks;
    private boolean Dirty = true;

    public ReactorControllerMenu(int ContainerId, Inventory PlayerInventory) {
        this(ContainerId, PlayerInventory, null);
    }

    public ReactorControllerMenu(int ContainerId, Inventory PlayerInventory, @Nullable ReactorControllerBlockEntity Controller) {
        super(ReactorRegistry.ControllerMenu.get(), ContainerId);
        this.Controller = Controller;
        this.Viewer = PlayerInventory.player;
    }

    @Override
    public boolean clickMenuButton(Player User, int Id) {
        if (Controller == null || !(Controller.getLevel() instanceof ServerLevel World)) {
            return false;
        }
        ReactorGrid Grid = Controller.resolve(World);
        if (Grid == null) {
            return false;
        }
        ReactorControl.Command Command = ReactorControl.decode(Id);
        BlockPos Origin = Controller.getBlockPos();
        switch (Command.Action()) {
            case ReactorControl.SetTarget -> {
                GridTower Tower = Grid.towerAt(Origin.offset(Command.Dx(), Command.Dy(), Command.Dz()));
                if (Tower == null) {
                    return false;
                }
                Grid.setTarget(Tower, Command.Value());
            }
            case ReactorControl.SetAll -> Grid.setAllTargets(Command.Value());
            case ReactorControl.Scram -> Grid.scram(World);
            default -> {
                return false;
            }
        }
        Grid.writeBack();
        World.playSound(null, Origin, ReactorRegistry.RodDrive.get(), SoundSource.BLOCKS, 0.35f, 1.3f);
        Dirty = true;
        return true;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (Controller == null || !(Controller.getLevel() instanceof ServerLevel World) || !(Viewer instanceof ServerPlayer Watcher)) {
            return;
        }
        Ticks++;
        if (Dirty || Ticks % SnapshotTicks == 0) {
            Dirty = false;
            PacketDistributor.sendToPlayer(Watcher, new ReactorSnapshotPayload(containerId, ReactorSnapshot.build(World, Controller)));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player User, int Index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player User) {
        return Controller == null || Controller.isUsableBy(User);
    }
}
