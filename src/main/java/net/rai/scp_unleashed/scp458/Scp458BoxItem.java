package net.rai.scp_unleashed.scp458;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
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

public class Scp458BoxItem extends BlockItem {
    public static final String Prefix = "block.scp_unleashed.scp458_box";

    public Scp458BoxItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp458BoxItem;
    }

    public static Scp458Contents contents(ItemStack Stack) {
        return Stack.getOrDefault(Scp458Registry.Contents.get(), Scp458Contents.Sealed);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (World.isClientSide) {
            return Scp458Box.canUse(User) ? InteractionResultHolder.success(Stack) : InteractionResultHolder.pass(Stack);
        }
        Scp458Box.Step Done = use((ServerLevel) World, User, Stack, User.isSecondaryUseActive());
        return Done.Result() == Scp458Box.Outcome.Refused ? InteractionResultHolder.pass(Stack) : InteractionResultHolder.consume(Stack);
    }

    public static Scp458Box.Step use(ServerLevel World, LivingEntity User, ItemStack Stack, boolean Closing) {
        Scp458Box.Step Done = Scp458Box.act(World, User, contents(Stack), Closing, Scp458Box.FrontAngle);
        if (Done.changed()) {
            Stack.set(Scp458Registry.Contents.get(), Done.After());
        }
        Scp458Box.give(User, Done.Given());
        Scp458Box.sound(World, User.getEyePosition().add(User.getLookAngle().scale(0.4)).subtract(0.0, 0.4, 0.0), Done.Result());
        switch (Done.Result()) {
            case Opened -> World.gameEvent(User, GameEvent.CONTAINER_OPEN, Vec3.atCenterOf(User.blockPosition()));
            case Closed -> World.gameEvent(User, GameEvent.CONTAINER_CLOSE, Vec3.atCenterOf(User.blockPosition()));
            case Took -> World.gameEvent(User, GameEvent.ITEM_INTERACT_FINISH, Vec3.atCenterOf(User.blockPosition()));
            default -> {
            }
        }
        return Done;
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
