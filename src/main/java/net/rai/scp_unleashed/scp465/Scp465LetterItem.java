package net.rai.scp_unleashed.scp465;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp465.network.Scp465LetterPayload;

import java.util.List;

public class Scp465LetterItem extends Item {
    public static final String Prefix = "item.scp_unleashed.scp465_letter";
    public static final String NotePrefix = "gui.scp_unleashed.scp465_letter.note.";
    public static final int Notes = 24;
    public static final int NamedFrom = 14;

    public Scp465LetterItem(Properties Settings) {
        super(Settings);
    }

    public static String noteKey(int Line) {
        return NotePrefix + Math.floorMod(Line, Notes);
    }

    public static boolean named(int Line) {
        return Math.floorMod(Line, Notes) >= NamedFrom;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (World.isClientSide) {
            return InteractionResultHolder.success(Stack);
        }
        if (User instanceof ServerPlayer Reader) {
            int Line = World.getRandom().nextInt(Notes);
            World.playSound(null, Reader.getX(), Reader.getEyeY(), Reader.getZ(), Scp465Registry.LetterOpen.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            Scp465Celebration.useLetter(Reader, Stack);
            PacketDistributor.sendToPlayer(Reader, new Scp465LetterPayload(Line, Reader.getGameProfile().getName()));
        }
        return InteractionResultHolder.consume(Stack);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
