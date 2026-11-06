package net.rai.scp_unleashed.scp2317.note;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.scp2317.note.client.Scp2317RiteNoteClientHooks;

import java.util.List;

public class Scp2317RiteNoteItem extends Item {
    public static final String TooltipKey = "item.scp_unleashed.scp001_scarlet_rite_note.tooltip";

    public Scp2317RiteNoteItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (World.isClientSide) {
            Scp2317RiteNoteClientHooks.read();
        } else {
            World.playSound(null, User.getX(), User.getEyeY(), User.getZ(), Scp2317RiteNoteRegistry.Read.get(), SoundSource.PLAYERS, Scp2317RiteNote.ReadVolume, Scp2317RiteNote.ReadPitch);
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(TooltipKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
