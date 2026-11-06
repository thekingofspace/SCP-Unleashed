package net.rai.scp_unleashed.scp444;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp444BookItem extends Item {
    public static final String Prefix = "item.scp_unleashed.scp444_book";
    public static final int Details = 3;

    public Scp444BookItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.getItem() instanceof Scp444BookItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (!World.isClientSide && User instanceof ServerPlayer Reader) {
            Scp444Reading.open(Reader, Hand);
        }
        return InteractionResultHolder.sidedSuccess(Held, World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            for (int Index = 0; Index < Details; Index++) {
                Lines.add(Component.translatable(Prefix + ".detail." + Index).withStyle(Index == 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            }
        }
    }
}
