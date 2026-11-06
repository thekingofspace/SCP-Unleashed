package net.rai.scp_unleashed.scp098.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp098.Scp098HiveBlock;
import net.rai.scp_unleashed.scp098.Scp098HiveBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp098HiveJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp098_hive");
    private static final String Prefix = "jade.scp_unleashed.scp098_hive.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        if (!(Accessor.getBlockEntity() instanceof Scp098HiveBlockEntity Hive)) {
            return;
        }
        int Stage = Accessor.getBlockState().getValue(Scp098HiveBlock.Stage);
        Tooltip.add(Component.translatable(Prefix + "stage", Stage, Scp098HiveBlock.MaxStage).withStyle(Stage >= Scp098HiveBlock.MaxStage ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        Tooltip.add(Component.translatable(Prefix + "occupants", Hive.occupants(), Scp098HiveBlockEntity.Capacity).withStyle(ChatFormatting.GRAY));
        int Fullness = Hive.fullnessPercent();
        Tooltip.add(Component.translatable(Prefix + "fullness", Fullness).withStyle(Fullness >= 100 ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
    }
}
