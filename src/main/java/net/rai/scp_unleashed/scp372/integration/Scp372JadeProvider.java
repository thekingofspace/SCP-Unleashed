package net.rai.scp_unleashed.scp372.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp372.Scp372AnchorBlock;
import net.rai.scp_unleashed.scp372.Scp372AnchorBlockEntity;
import net.rai.scp_unleashed.scp372.Scp372AnchorLight;
import net.rai.scp_unleashed.scp372.Scp372Registry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp372JadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = Scp372Registry.location(Scp372Registry.AnchorId);
    private static final String Prefix = "jade.scp_unleashed." + Scp372Registry.AnchorId + ".";
    private static final String DataKey = "Scp372Anchor";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (Accessor.getBlockEntity() instanceof Scp372AnchorBlockEntity Anchor) {
            CompoundTag Status = new CompoundTag();
            Status.putInt("Received", Anchor.getLastReceived());
            Status.putString("Stage", Anchor.stage().name());
            Data.put(DataKey, Status);
        }
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        Scp372AnchorLight Lit = Accessor.getBlockState().getValue(Scp372AnchorBlock.Light);
        CompoundTag Status = Accessor.getServerData().getCompound(DataKey);
        int Received = Status.getInt("Received");
        String Stage = Status.getString("Stage");
        if (Lit == Scp372AnchorLight.Holding) {
            Tooltip.add(Component.translatable(Prefix + "holding").withStyle(ChatFormatting.GREEN));
        } else if (Lit == Scp372AnchorLight.Powered) {
            Tooltip.add(Component.translatable(Scp372AnchorBlockEntity.Stage.Charging.name().equals(Stage) ? Prefix + "charging" : Prefix + "powered").withStyle(ChatFormatting.GOLD));
        } else if (Received > 0) {
            Tooltip.add(Component.translatable(Prefix + "underpowered").withStyle(ChatFormatting.RED));
        } else {
            Tooltip.add(Component.translatable(Prefix + "unpowered").withStyle(ChatFormatting.RED));
        }
        Tooltip.add(Component.translatable(Prefix + "demand", Scp372AnchorBlockEntity.PowerDraw).withStyle(ChatFormatting.GRAY));
    }
}
