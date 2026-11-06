package net.rai.scp_unleashed.inkwire;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InvisibleInkItem extends Item {
    public static final int Uses = 32;

    public InvisibleInkItem(Properties Properties) {
        super(Properties);
    }

    public static boolean isHeldBy(Player User) {
        return User.getMainHandItem().getItem() instanceof InvisibleInkItem || User.getOffhandItem().getItem() instanceof InvisibleInkItem;
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        if (!InkMarks.canHide(World.getBlockState(Pos))) {
            return InteractionResult.PASS;
        }
        if (World instanceof ServerLevel Server && InkMarks.toggleBlock(Server, Pos) == InkMarks.Toggle.Hidden) {
            spend(Server, Context.getItemInHand(), Context.getPlayer(), Context.getHand());
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public static InkMarks.Toggle inkWire(ServerPlayer User, InteractionHand Hand, BlockPos Owner, BlockPos Target) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (!(Stack.getItem() instanceof InvisibleInkItem) || !User.mayBuild() || User.isSpectator()) {
            return InkMarks.Toggle.Missed;
        }
        ServerLevel World = User.serverLevel();
        InkMarks.Toggle Result = InkMarks.toggleWire(World, User, Owner, Target);
        if (Result == InkMarks.Toggle.Hidden) {
            spend(World, Stack, User, Hand);
        }
        return Result;
    }

    public static void spend(ServerLevel World, ItemStack Stack, @Nullable Player User, InteractionHand Hand) {
        Stack.hurtAndBreak(1, World, User, Gone -> {
        });
        if (Stack.isEmpty() && User != null) {
            User.setItemInHand(Hand, new ItemStack(Items.GLASS_BOTTLE));
            World.playSound(null, User.getX(), User.getY(), User.getZ(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.7f, 1.25f);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.invisible_ink.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.invisible_ink.use").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.invisible_ink.ghosts").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.invisible_ink.cost").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.invisible_ink.dabs", Stack.getMaxDamage() - Stack.getDamageValue(), Stack.getMaxDamage()).withStyle(ChatFormatting.AQUA));
        }
    }
}
