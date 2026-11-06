package net.rai.scp_unleashed.microhid.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.microhid.MicroChargingPlatformBlockEntity;
import net.rai.scp_unleashed.microhid.MicroChargingStatus;
import net.rai.scp_unleashed.microhid.MicroHidEnergy;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum MicroChargingPlatformJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "micro_charging_platform");
    private static final String DataKey = "ScpMicroChargingPlatform";
    private static final int BarColour = 0xFF5AA8FF;
    private static final int BarShadeColour = 0xFF1C3E78;

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof MicroChargingPlatformBlockEntity Platform)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putByte("Status", (byte) Platform.getStatus().ordinal());
        Info.putBoolean("Docked", !Platform.getDocked().isEmpty());
        Info.putFloat("Fill", Platform.getDocked().isEmpty() ? 0.0F : MicroHidEnergy.fraction(Platform.getDocked()));
        Info.putInt("Percent", Platform.getDocked().isEmpty() ? 0 : MicroHidEnergy.percent(Platform.getDocked()));
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        MicroChargingStatus Status = MicroChargingStatus.byIndex(Info.getByte("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        if (Info.getBoolean("Docked")) {
            IElementHelper Helper = IElementHelper.get();
            Tooltip.add(Helper.progress(Info.getFloat("Fill"), Component.translatable("jade.scp_unleashed.micro_charging_platform.charge", Info.getInt("Percent")),
                    Helper.progressStyle().color(BarColour, BarShadeColour).textColor(0xFFFFFFFF), BoxStyle.getNestedBox(), true));
        }
    }
}
