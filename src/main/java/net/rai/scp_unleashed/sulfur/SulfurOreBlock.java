package net.rai.scp_unleashed.sulfur;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.rai.scp_unleashed.materials.MaterialTooltips;

import java.util.List;

public class SulfurOreBlock extends DropExperienceBlock {
    public static final MapCodec<SulfurOreBlock> BlockCodec = simpleCodec(SulfurOreBlock::new);
    public static final int ExperienceMin = 1;
    public static final int ExperienceMax = 3;

    public SulfurOreBlock(Properties Settings) {
        super(UniformInt.of(ExperienceMin, ExperienceMax), Settings);
    }

    @Override
    public MapCodec<? extends DropExperienceBlock> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        MaterialTooltips.append(getDescriptionId(), Lines);
    }
}
