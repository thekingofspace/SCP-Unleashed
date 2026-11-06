package net.rai.scp_unleashed.archive.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.client.DatabaseRecords;
import net.rai.scp_unleashed.archive.database.DatabaseTerminalBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum DatabaseJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "archive_database");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        int Count = DatabaseRecords.count();
        Tooltip.add(Component.translatable(Count == 1 ? "jade.scp_unleashed.archive_database.one" : "jade.scp_unleashed.archive_database.count", Count)
                .withStyle(Count == 0 ? ChatFormatting.GRAY : ChatFormatting.AQUA));
        if (Accessor.getBlock() instanceof DatabaseTerminalBlock) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.archive_database.read_only").withStyle(ChatFormatting.GOLD));
        }
    }
}
