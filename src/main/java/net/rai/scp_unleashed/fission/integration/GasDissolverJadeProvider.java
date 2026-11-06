package net.rai.scp_unleashed.fission.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fission.DissolverMode;
import net.rai.scp_unleashed.fission.GasDissolverBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum GasDissolverJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gas_dissolver");
    private static final String DataKey = "GasDissolver";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof GasDissolverBlockEntity Dissolver)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Status", Dissolver.getMode().getSerializedName());
        Info.putInt("Water", Dissolver.getTank().getFluidAmount());
        Info.putInt("Network", Dissolver.getNetworkRate());
        Info.putInt("Area", Dissolver.getAreaRate());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        DissolverMode Status = DissolverMode.byName(Info.getString("Status"));
        Tooltip.add(Component.translatable(Status.getTranslationKey()).withStyle(Status.getColour()));
        Tooltip.add(Component.translatable("jade.scp_unleashed.gas_dissolver.water", Info.getInt("Water"), GasDissolverBlockEntity.TankCapacity).withStyle(ChatFormatting.AQUA));
        int Network = Info.getInt("Network");
        int Area = Info.getInt("Area");
        if (Network + Area > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.gas_dissolver.rate", Network + Area).withStyle(ChatFormatting.GREEN));
        }
    }
}
