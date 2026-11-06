package net.rai.scp_unleashed.scp999.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp999.PinkDogBowlBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum PinkDogBowlJadeProvider implements IBlockComponentProvider {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "pink_dog_bowl");
    private static final String Prefix = "jade.scp_unleashed.pink_dog_bowl.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        int Count = PinkDogBowlBlock.candy(Accessor.getBlockState());
        if (Count > 0) {
            Tooltip.add(Component.translatable(Prefix + "candy", Count, PinkDogBowlBlock.Capacity).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            Tooltip.add(Component.translatable(Prefix + "empty").withStyle(ChatFormatting.GRAY));
        }
    }
}
