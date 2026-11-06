package net.rai.scp_unleashed.trinitite;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.rai.scp_unleashed.materials.MaterialTooltips;

import java.util.List;

public class TrinititeOreBlock extends DropExperienceBlock {
    public static final MapCodec<TrinititeOreBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            IntProvider.codec(0, 10).fieldOf("experience").forGetter(TrinititeOreBlock::getExperience),
            propertiesCodec()
    ).apply(Instance, TrinititeOreBlock::new));

    private final IntProvider Experience;

    public TrinititeOreBlock(IntProvider Experience, Properties Properties) {
        super(Experience, Properties);
        this.Experience = Experience;
    }

    public IntProvider getExperience() {
        return Experience;
    }

    @Override
    public MapCodec<TrinititeOreBlock> codec() {
        return BlockCodec;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        MaterialTooltips.append(getDescriptionId(), Lines);
    }
}
