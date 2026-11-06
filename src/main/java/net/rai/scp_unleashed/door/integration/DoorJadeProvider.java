package net.rai.scp_unleashed.door.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.door.DevicePulse;
import net.rai.scp_unleashed.door.DoorLeverBlock;
import net.rai.scp_unleashed.door.KeycardItem;
import net.rai.scp_unleashed.door.KeycardScannerBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DoorJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "door_devices");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState State = Accessor.getBlockState();
        if (State.getBlock() instanceof KeycardScannerBlock) {
            int Required = State.getValue(KeycardScannerBlock.Clearance);
            Tooltip.add(Component.translatable("jade.scp_unleashed.door_clearance", Required).withStyle(KeycardItem.colourOf(Required)));
        }
        if (State.hasProperty(DevicePulse.Pulse)) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.door_pulse", DevicePulse.describe(State)).withStyle(ChatFormatting.AQUA));
        }
        if (State.getBlock() instanceof DoorLeverBlock) {
            boolean On = State.getValue(DoorLeverBlock.Powered);
            Tooltip.add(Component.translatable(On ? "jade.scp_unleashed.door_lever_on" : "jade.scp_unleashed.door_lever_off").withStyle(On ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
    }
}
