package net.rai.scp_unleashed.scp860.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp860.Scp860DoorBlock;
import net.rai.scp_unleashed.scp860.Scp860DoorBlockEntity;
import net.rai.scp_unleashed.scp860.Scp860Registry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp860DoorJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = Scp860Registry.location("scp860_door");
    private static final String Prefix = "jade.scp_unleashed.scp860_door.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        Scp860DoorBlockEntity Frame = Scp860DoorBlock.frame(Accessor.getLevel(), Accessor.getPosition(), Accessor.getBlockState());
        if (Frame == null) {
            return;
        }
        if (Frame.far()) {
            Tooltip.add(Component.translatable(Prefix + (Frame.opened() ? "ajar" : "shut")).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!Frame.keyed()) {
            Tooltip.add(Component.translatable(Prefix + "locked").withStyle(ChatFormatting.GRAY));
        } else if (Frame.opened()) {
            Tooltip.add(Component.translatable(Prefix + "open").withStyle(ChatFormatting.AQUA));
        } else {
            Tooltip.add(Component.translatable(Prefix + "keyed").withStyle(ChatFormatting.BLUE));
        }
        if (Frame.sealed()) {
            Tooltip.add(Component.translatable(Prefix + "held").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
