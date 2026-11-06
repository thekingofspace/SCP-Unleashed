package net.rai.scp_unleashed.scp513;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp513BellItem extends Item {
    public static final String Prefix = "item.scp_unleashed.scp513_bell";
    public static final int UseCooldown = 40;

    public Scp513BellItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp513BellItem;
    }

    public static boolean isTaped(ItemStack Stack) {
        return Stack.has(Scp513Registry.Tape.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        User.getCooldowns().addCooldown(this, UseCooldown);
        if (World instanceof ServerLevel Server) {
            Scp513Chimes.strike(Server, User, Stack);
            User.awardStat(Stats.ITEM_USED.get(this));
            User.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack Stack, ItemEntity Dropped) {
        if (Dropped.level() instanceof ServerLevel Server && (Dropped.tickCount + Dropped.getId()) % Scp513Chimes.CheckEvery == 0) {
            Scp513Chimes.tickLoose(Server, Dropped);
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (isTaped(Stack)) {
            Lines.add(Component.translatable(Prefix + ".taped").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + ".detail.0").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + ".detail.1").withStyle(ChatFormatting.GOLD));
        }
    }
}
