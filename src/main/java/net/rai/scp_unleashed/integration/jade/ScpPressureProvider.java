package net.rai.scp_unleashed.integration.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.DuctBlock;
import net.rai.scp_unleashed.block.PressurizerBlock;
import net.rai.scp_unleashed.block.entity.PressurizerBlockEntity;
import net.rai.scp_unleashed.pressure.DuctKind;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum ScpPressureProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "pressure");
    private static final String DataKey = "ScpPressure";
    private static final int PipeBarColour = 0xFF5C8FD6;
    private static final int PipeBarShadeColour = 0xFF2C4A78;
    private static final int VentBarColour = 0xFFB8C2CC;
    private static final int VentBarShadeColour = 0xFF5E666E;

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getLevel() instanceof ServerLevel World)) {
            return;
        }
        CompoundTag Pressure = new CompoundTag();
        if (Accessor.getBlock() instanceof DuctBlock Duct) {
            Pressure.putString("Kind", Duct.getKind().getSerializedName());
            Pressure.putDouble("Pressure", PressureNetworkManager.get(World).networkAt(World, Accessor.getPosition(), Duct.getKind()).getPressure());
            Pressure.putInt("Rating", Duct.getKind().getRating());
        } else if (Accessor.getBlock() instanceof PressurizerBlock Pump && Accessor.getBlockEntity() instanceof PressurizerBlockEntity Machine) {
            Pressure.putString("Kind", Pump.getKind().getSerializedName());
            Pressure.putBoolean("Pump", true);
            Pressure.putDouble("Pressure", Machine.getLastPressure());
            Pressure.putInt("Rating", Pump.getKind().getTargetPressure());
            Pressure.putBoolean("Active", Accessor.getBlockState().getValue(PressurizerBlock.Active));
        } else {
            return;
        }
        Data.put(DataKey, Pressure);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Pressure = Data.getCompound(DataKey);
        boolean IsVent = DuctKind.Vent.getSerializedName().equals(Pressure.getString("Kind"));
        if (Pressure.getBoolean("Pump")) {
            boolean Active = Pressure.getBoolean("Active");
            Tooltip.add(Component.translatable(Active ? "jade.scp_unleashed.pump_active" : "jade.scp_unleashed.pump_idle")
                    .withStyle(Active ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
        int Rating = Math.max(1, Pressure.getInt("Rating"));
        double Current = Pressure.getDouble("Pressure");
        IElementHelper Helper = IElementHelper.get();
        Tooltip.add(Helper.progress((float) Math.min(1.0, Current / Rating), Component.translatable("jade.scp_unleashed.pressure", Math.round(Current), Rating),
                Helper.progressStyle().color(IsVent ? VentBarColour : PipeBarColour, IsVent ? VentBarShadeColour : PipeBarShadeColour).textColor(0xFFFFFFFF),
                BoxStyle.getNestedBox(), true));
    }
}
