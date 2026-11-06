package net.rai.scp_unleashed.centrifuge.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.centrifuge.CentrifugeBlockEntity;
import net.rai.scp_unleashed.centrifuge.CentrifugeStatus;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CentrifugeJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "centrifuge");
    private static final String DataKey = "Centrifuge";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof CentrifugeBlockEntity Machine)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Machine.getStatus().getSerializedName());
        Info.putInt("Progress", Math.round(Machine.getProgressFraction() * 100.0f));
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        CentrifugeStatus Status = CentrifugeStatus.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable("jade.scp_unleashed.centrifuge." + Status.getSerializedName()).withStyle(Status.getColour()));
        if (Status != CentrifugeStatus.Idle) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.centrifuge.progress", Info.getInt("Progress")));
        }
    }
}
