package net.rai.scp_unleashed.scp207.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp207.Scp207CrateBlockEntity;
import net.rai.scp_unleashed.scp207.Scp207Registry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp207CrateJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = Scp207Registry.location("scp207_crate");
    private static final String DataKey = "Scp207Crate";
    private static final String Prefix = "jade.scp_unleashed.scp207_crate.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof Scp207CrateBlockEntity Crate)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putInt("Left", Crate.left());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        int Left = Data.getCompound(DataKey).getInt("Left");
        if (Left > 0) {
            Tooltip.add(Component.translatable(Prefix + "left", Left).withStyle(ChatFormatting.GOLD));
        } else {
            Tooltip.add(Component.translatable(Prefix + "empty").withStyle(ChatFormatting.GRAY));
        }
    }
}
