package net.rai.scp_unleashed.space.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.space.AirInfuserBlockEntity;
import net.rai.scp_unleashed.space.AirInfuserStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum AirInfuserJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "air_infuser");
    private static final String DataKey = "ScpAirInfuser";
    private static final int BarColour = 0xFF5FC8F0;
    private static final int BarShadeColour = 0xFF1E5A78;

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof AirInfuserBlockEntity Machine)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putByte("Status", (byte) Machine.getStatus().ordinal());
        Info.putFloat("Fill", Machine.getFill());
        Info.putBoolean("Loaded", !Machine.getStorage().getStackInSlot(AirInfuserBlockEntity.ChamberSlot).isEmpty());
        Info.putByte("Upgrades", (byte) Machine.getSpeedUpgrades());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        AirInfuserStatus Status = AirInfuserStatus.byIndex(Info.getByte("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        if (Info.getBoolean("Loaded")) {
            float Fill = Info.getFloat("Fill");
            IElementHelper Helper = IElementHelper.get();
            Tooltip.add(Helper.progress(Fill, Component.translatable("jade.scp_unleashed.air_infuser.fill", Math.round(Fill * 100.0F)),
                    Helper.progressStyle().color(BarColour, BarShadeColour).textColor(0xFFFFFFFF), BoxStyle.getNestedBox(), true));
        }
        int Upgrades = Info.getByte("Upgrades");
        if (Upgrades > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.air_infuser.upgrades", Upgrades));
        }
    }
}
