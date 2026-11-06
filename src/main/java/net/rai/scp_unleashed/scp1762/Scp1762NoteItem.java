package net.rai.scp_unleashed.scp1762;

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
import net.rai.scp_unleashed.scp1762.client.Scp1762ClientHooks;

import java.util.List;

public class Scp1762NoteItem extends Item {
    public static final String Prefix = "item.scp_unleashed.scp1762_note";
    public static final String TextPrefix = "gui.scp_unleashed.scp1762_note.";
    public static final int Notes = 2;
    public static final int[] Rows = {5, 4};

    public Scp1762NoteItem(Properties Settings) {
        super(Settings);
    }

    public static int line(ItemStack Stack) {
        return Math.floorMod(Stack.getOrDefault(Scp1762Registry.NoteLine.get(), 0), Notes);
    }

    public static int rows(int Note) {
        return Rows[Math.floorMod(Note, Notes)];
    }

    public static String key(int Note, int Row) {
        return TextPrefix + Note + "." + Row;
    }

    public static String sign(int Note) {
        return TextPrefix + Note + ".sign";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (World.isClientSide) {
            Scp1762ClientHooks.read(line(Stack));
        } else {
            World.playSound(null, User.getX(), User.getEyeY(), User.getZ(), Scp1762Registry.NoteRead.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public Component getName(ItemStack Stack) {
        return Component.translatable(Prefix + "." + line(Stack));
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        int Note = line(Stack);
        for (int Row = 0; Row < rows(Note); Row++) {
            Lines.add(Component.translatable(key(Note, Row)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        Lines.add(Component.translatable(sign(Note)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
