package net.rai.scp_unleashed.scp140.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp140.Scp140BookBlockEntity;
import net.rai.scp_unleashed.scp140.Scp140Chronicle;
import net.rai.scp_unleashed.scp140.Scp140Mood;
import net.rai.scp_unleashed.scp140.api.Scp140Pages;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp140JadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp140_chronicle");
    private static final String DataKey = "Scp140Chronicle";
    private static final String Prefix = "jade.scp_unleashed.scp140_chronicle.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof Scp140BookBlockEntity) || !(Accessor.getLevel() instanceof ServerLevel World)) {
            return;
        }
        Scp140Chronicle Chronicle = Scp140Chronicle.get(World.getServer());
        CompoundTag Info = new CompoundTag();
        Info.putInt("Written", Chronicle.written());
        Info.putInt("Mood", Chronicle.mood(World.getGameTime()).ordinal());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        Tooltip.add(Component.translatable(Prefix + "pages", Info.getInt("Written"), Scp140Pages.Total).withStyle(ChatFormatting.GRAY));
        Tooltip.add(Scp140Mood.byId(Info.getInt("Mood")).describe());
    }
}
