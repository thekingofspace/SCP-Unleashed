package net.rai.scp_unleashed.disperser.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.disperser.DisperserBlockEntity;
import net.rai.scp_unleashed.labware.LabOrganisms;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DisperserJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "disperser");
    private static final String DataKey = "Disperser";
    private static final String Prefix = "jade.scp_unleashed.disperser.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof DisperserBlockEntity Machine)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        ResourceLocation Culture = Machine.getCulture();
        if (Culture != null) {
            Info.putString("Culture", Culture.toString());
        }
        Info.putBoolean("Running", Machine.isRunning());
        Info.putBoolean("Signalled", Machine.isSignalled());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        if (!Info.contains("Culture")) {
            Tooltip.add(Component.translatable(Prefix + "empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        ResourceLocation Culture = ResourceLocation.tryParse(Info.getString("Culture"));
        Tooltip.add(Component.translatable(Prefix + "culture", LabOrganisms.styledNameOf(Culture)).withStyle(ChatFormatting.GRAY));
        if (Info.getBoolean("Running")) {
            Tooltip.add(Component.translatable(Prefix + "running").withStyle(ChatFormatting.RED));
        } else if (!Info.getBoolean("Signalled")) {
            Tooltip.add(Component.translatable(Prefix + "waiting").withStyle(ChatFormatting.YELLOW));
        } else {
            Tooltip.add(Component.translatable(Prefix + "unpowered").withStyle(ChatFormatting.RED));
        }
    }
}
