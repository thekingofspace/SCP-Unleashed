package net.rai.scp_unleashed.scp105.photo096;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp105.photo096.client.Scp105Photo096ClientHooks;
import net.rai.scp_unleashed.senses.Senses;

import java.util.List;

public class Scp105Photo096Item extends Item {
    public static final int ViewCooldown = 60;
    public static final String Key = "item.scp_unleashed.scp105_photograph_096.";

    public Scp105Photo096Item(Properties Settings) {
        super(Settings);
    }

    public static ItemStack create() {
        return new ItemStack(Scp105Photo096Registry.Photograph.get());
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp105Photo096Item;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (Senses.refuseSight(User)) {
            return InteractionResultHolder.fail(Stack);
        }
        User.getCooldowns().addCooldown(this, ViewCooldown);
        if (World.isClientSide) {
            Scp105Photo096ClientHooks.open();
        } else if (User instanceof ServerPlayer Viewer) {
            Scp105Photo096Sight.view(Viewer);
            User.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "back").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "cold").withStyle(ChatFormatting.GOLD));
        }
    }
}
