package net.rai.scp_unleashed.gas.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gas.GasStack;
import net.rai.scp_unleashed.gas.GasTankBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum GasTankJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gas_tank");
    private static final String DataKey = "GasTank";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (Accessor.getBlockEntity() instanceof GasTankBlockEntity Tank) {
            Data.put(DataKey, Tank.getTank().getGas().save());
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        GasStack Held = GasStack.load(Data.getCompound(DataKey));
        if (Held.isEmpty()) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.gas_tank.empty", GasTankBlockEntity.Capacity).withStyle(ChatFormatting.GRAY));
        } else {
            int Colour = 0xFF000000 | Held.getGas().getColour();
            int Shade = 0xFF000000 | ((Held.getGas().getColour() >> 1) & 0x7F7F7F);
            IElementHelper Helper = IElementHelper.get();
            Tooltip.add(Helper.progress(Held.getAmount() / (float) GasTankBlockEntity.Capacity,
                    Component.translatable("jade.scp_unleashed.gas_tank.contents", Held.getGas().getName(), Held.getAmount(), GasTankBlockEntity.Capacity),
                    Helper.progressStyle().color(Colour, Shade).textColor(0xFFFFFFFF), BoxStyle.getNestedBox(), true));
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.gas_tank.ports").withStyle(ChatFormatting.DARK_GRAY));
    }
}
