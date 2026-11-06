package net.rai.scp_unleashed.scp3008.econ.block;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.block.Block;

public class Scp3008WorkbenchMenu extends CraftingMenu {
    private final ContainerLevelAccess Access;
    private final Block Bench;

    public Scp3008WorkbenchMenu(int Id, Inventory Holder, ContainerLevelAccess Access, Block Bench) {
        super(Id, Holder, Access);
        this.Access = Access;
        this.Bench = Bench;
    }

    @Override
    public boolean stillValid(Player User) {
        return stillValid(Access, User, Bench);
    }
}
