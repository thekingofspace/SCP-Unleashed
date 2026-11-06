package net.rai.scp_unleashed.scp4666.christmas;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class Scp4666NoteItem extends Item {
    public static final String TextKey = "item.scp_unleashed.scp4666_saint_note.text";
    public static final String SignedKey = "item.scp_unleashed.scp4666_saint_note.signed";

    public Scp4666NoteItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (World.isClientSide) {
            Scp4666DoorNote.Reader.run();
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(TextKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        Lines.add(Component.translatable(SignedKey).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
