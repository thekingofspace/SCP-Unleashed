package net.rai.scp_unleashed.scp513.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp513.Scp513BellItem;
import net.rai.scp_unleashed.scp513.Scp513Contained;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp513JadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp513_bell");
    private static final String DataKey = "Scp513Bell";
    private static final String Prefix = "jade.scp_unleashed.scp513_bell.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        StorageContainerBlockEntity Locker = StorageContainerBlock.containerAt(Accessor.getLevel(), Accessor.getPosition());
        if (Locker == null || !Scp513BellItem.is(Locker.peekContents())) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putBoolean("Still", Scp513Contained.isSealed(Locker));
        Info.putBoolean("Bound", Scp513BellItem.isTaped(Locker.peekContents()));
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        String Key = Info.getBoolean("Still") ? "still" : Info.getBoolean("Bound") ? "bound" : "restless";
        Tooltip.add(Component.translatable(Prefix + Key).withStyle(Info.getBoolean("Still") ? ChatFormatting.GRAY : ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
