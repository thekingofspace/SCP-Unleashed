package net.rai.scp_unleashed.scp055;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp055CaseItem extends BlockItem {
    public static final String Prefix = "block.scp_unleashed.scp055_case.";

    public Scp055CaseItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp055CaseItem;
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Player User = Context.getPlayer();
        if (User == null || User.isSecondaryUseActive()) {
            return super.useOn(Context);
        }
        return open(Context.getLevel(), User);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (User.isSecondaryUseActive()) {
            return InteractionResultHolder.pass(Stack);
        }
        InteractionResult Result = open(World, User);
        return new InteractionResultHolder<>(Result, Stack);
    }

    private static InteractionResult open(Level World, Player User) {
        if (!Scp055Episodes.canBegin(User)) {
            return InteractionResult.CONSUME;
        }
        if (User instanceof ServerPlayer Viewer) {
            Scp055Episodes.begin(Viewer);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "details").withStyle(ChatFormatting.GOLD));
        }
    }
}
