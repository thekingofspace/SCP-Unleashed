package net.rai.scp_unleashed.refinery.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.refinery.OrePurifierBlock;
import net.rai.scp_unleashed.refinery.OrePurifierBlockEntity;
import net.rai.scp_unleashed.refinery.PurifierStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum OrePurifierJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "ore_purifier");
    private static final String DataKey = "OrePurifier";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        OrePurifierBlockEntity Machine = OrePurifierBlock.masterEntity(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Machine == null) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Machine.getStatus().getSerializedName());
        Info.putInt("Progress", Math.round(Machine.getProgressFraction() * 100.0f));
        Info.putInt("Received", Machine.getLastReceived());
        Info.putInt("Demand", Machine.getDemand());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        PurifierStatus Status = PurifierStatus.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        if (Status != PurifierStatus.Idle) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.ore_purifier.progress", Info.getInt("Progress")));
        }
        if (Info.getInt("Demand") > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.ore_purifier.power", Info.getInt("Received"), Info.getInt("Demand")).withStyle(ChatFormatting.GRAY));
        }
    }
}
