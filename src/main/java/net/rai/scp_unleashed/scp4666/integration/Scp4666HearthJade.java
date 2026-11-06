package net.rai.scp_unleashed.scp4666.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp4666.block.Scp4666FireplaceBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666HeaterBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp4666HearthJade implements IBlockComponentProvider {
    Fireplace("scp4666_fireplace", new String[]{"out", "embers", "low", "burning", "roaring"}),
    Boiler("scp4666_boiler", new String[]{"cold", "warm", "hot", "roaring"});

    private final ResourceLocation Uid;
    private final String Prefix;
    private final String[] Levels;

    Scp4666HearthJade(String Name, String[] Levels) {
        this.Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
        this.Prefix = "jade.scp_unleashed." + Name + ".";
        this.Levels = Levels;
    }

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        BlockState Found = Accessor.getBlockState();
        int Level;
        if (Found.getBlock() instanceof Scp4666FireplaceBlock) {
            Level = Found.getValue(Scp4666FireplaceBlock.Fire);
        } else if (Found.getBlock() instanceof Scp4666HeaterBlock) {
            Level = Found.getValue(Scp4666HeaterBlock.Heat);
        } else {
            return;
        }
        Level = Math.max(0, Math.min(Levels.length - 1, Level));
        Tooltip.add(Component.translatable(Prefix + "label", Component.translatable(Prefix + Levels[Level]))
                .withStyle(Level == 0 ? ChatFormatting.DARK_GRAY : Level == 1 ? ChatFormatting.GRAY : ChatFormatting.GOLD));
    }
}
