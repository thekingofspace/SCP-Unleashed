package net.rai.scp_unleashed.sulfur.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.sulfur.tank.IndustrialTankBlockEntity;
import net.rai.scp_unleashed.sulfur.tank.TankStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum IndustrialTankJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "industrial_tank");
    private static final String Prefix = "jade.scp_unleashed.industrial_tank.";
    private static final String DataKey = "IndustrialTank";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (Accessor.getBlockEntity() instanceof IndustrialTankBlockEntity Machine) {
            CompoundTag Status = new CompoundTag();
            Status.putInt("Status", Machine.status().ordinal());
            Status.putFloat("Progress", Machine.progress());
            Status.putInt("Made", Machine.made());
            Data.put(DataKey, Status);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Status = Accessor.getServerData().getCompound(DataKey);
        TankStatus Shown = TankStatus.byIndex(Status.getInt("Status"));
        Tooltip.add(Component.translatable(Shown.getTranslationKey()).withStyle(Shown.getColour()));
        if (Shown.isWorking()) {
            Tooltip.add(Component.translatable(Prefix + "progress", Math.round(Status.getFloat("Progress") * 100.0f)).withStyle(ChatFormatting.GRAY));
        }
        Tooltip.add(Component.translatable(Prefix + "demand", IndustrialTankBlockEntity.WorkDraw).withStyle(ChatFormatting.GRAY));
    }
}
