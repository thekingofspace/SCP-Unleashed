package net.rai.scp_unleashed.scp902;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp902BoxItem extends BlockItem {
    public static final String Prefix = "block.scp_unleashed.scp902_box";

    public Scp902BoxItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp902BoxItem;
    }

    public static Scp902Latch latch(ItemStack Stack) {
        return Stack.getOrDefault(Scp902Registry.LatchComponent.get(), Scp902Latch.Shut);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (World.isClientSide) {
            return Scp902Box.canUse(User) ? InteractionResultHolder.success(Stack) : InteractionResultHolder.pass(Stack);
        }
        if (!Scp902Box.canUse(User)) {
            return InteractionResultHolder.pass(Stack);
        }
        if (User.isSecondaryUseActive() && User instanceof ServerPlayer Visitor && Scp902Trip.begin(Visitor)) {
            return InteractionResultHolder.consume(Stack);
        }
        toggle((ServerLevel) World, User, Stack);
        return InteractionResultHolder.consume(Stack);
    }

    public static boolean toggle(ServerLevel World, Player User, ItemStack Stack) {
        long Now = World.getGameTime();
        Scp902Latch Current = latch(Stack);
        if (Current.settling(Now)) {
            return false;
        }
        Scp902Latch Next = Current.toggled(Now);
        Stack.set(Scp902Registry.LatchComponent.get(), Next);
        Vec3 At = User.getEyePosition().add(User.getLookAngle().scale(0.4)).subtract(0.0, 0.4, 0.0);
        Scp902Box.lid(World, At, Next.Open());
        World.gameEvent(User, Next.Open() ? GameEvent.CONTAINER_OPEN : GameEvent.CONTAINER_CLOSE, At);
        if (Next.Open()) {
            Scp902Box.empty(User, World);
        }
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack Before, ItemStack After, boolean SlotChanged) {
        return SlotChanged || Before.getItem() != After.getItem();
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack Before, ItemStack After) {
        return Before.getItem() != After.getItem();
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + ".detail.0").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + ".detail.1").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Prefix + ".detail.2").withStyle(ChatFormatting.GRAY));
        }
    }
}
