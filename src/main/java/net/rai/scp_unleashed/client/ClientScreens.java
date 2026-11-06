package net.rai.scp_unleashed.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.rai.scp_unleashed.block.entity.PortHolder;

public final class ClientScreens {
    private ClientScreens() {
    }

    public static void openPortPanel(BlockPos Pos) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level != null && Game.level.getBlockEntity(Pos) instanceof PortHolder Device) {
            int[] Values = new int[Device.getSlotCount()];
            for (int Slot = 0; Slot < Values.length; Slot++) {
                Values[Slot] = Device.getSlot(Slot);
            }
            Game.setScreen(new PortPanelScreen(Pos.immutable(), PortPanelScreen.Style.of(Device.getKindName()), Game.level.getBlockState(Pos).getBlock().getName(), Values));
        }
    }
}
