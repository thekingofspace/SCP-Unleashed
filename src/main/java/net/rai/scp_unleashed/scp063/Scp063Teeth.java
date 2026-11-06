package net.rai.scp_unleashed.scp063;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.WeakHashMap;

public final class Scp063Teeth {
    public static final long RefusalCooldown = 15L;
    private static final Map<Player, Long> LastRefusal = new WeakHashMap<>();

    private Scp063Teeth() {
    }

    public static boolean isToothless(Player User) {
        return User.getExistingData(Scp063Registry.Toothless).orElse(Boolean.FALSE);
    }

    public static boolean isChewing(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.getUseAnimation() == UseAnim.EAT;
    }

    public static boolean isCake(BlockState State) {
        return State.getBlock() instanceof CakeBlock || State.getBlock() instanceof CandleCakeBlock;
    }

    public static boolean lose(ServerPlayer User) {
        if (isToothless(User)) {
            return false;
        }
        User.setData(Scp063Registry.Toothless, Boolean.TRUE);
        Scp063Brushing.tell(User, "toothless", ChatFormatting.DARK_RED);
        Scp063Brushing.play(User, Scp063Registry.TeethGone.get(), 1.0f, 1.0f);
        Scp063Brushing.play(User, Scp063Registry.TeethClatter.get(), 0.8f, 1.0f);
        return true;
    }

    public static boolean restore(ServerPlayer User) {
        boolean Had = isToothless(User);
        User.removeData(Scp063Registry.Toothless);
        Scp063Brushing.setBrushTime(User, 0);
        if (Had) {
            Scp063Brushing.tell(User, "restored", ChatFormatting.AQUA);
            Scp063Brushing.play(User, Scp063Registry.TeethBack.get(), 0.8f, 1.0f);
        }
        return Had;
    }

    public static void refuse(ServerPlayer User) {
        Scp063Brushing.tell(User, "refuse", ChatFormatting.GRAY);
        long Now = User.level().getGameTime();
        Long Last = LastRefusal.get(User);
        if (Last == null || Now - Last >= RefusalCooldown || Now < Last) {
            LastRefusal.put(User, Now);
            Scp063Brushing.play(User, Scp063Registry.Gum.get(), 0.8f, 1.0f);
        }
    }
}
