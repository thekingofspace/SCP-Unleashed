package net.rai.scp_unleashed.scp407;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.archive.UniqueGifts;
import net.rai.scp_unleashed.speaker.cassette.CassetteItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp407CassetteItem extends CassetteItem {
    public Scp407CassetteItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public boolean outlastsInsert(@Nullable Player User) {
        return UniqueGifts.keeps(User);
    }

    @Override
    public Component getName(ItemStack Stack) {
        return Component.translatable(getDescriptionId(Stack));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp407_cassette.tooltip").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        super.appendHoverText(Stack, Context, Lines, Flag);
    }
}
