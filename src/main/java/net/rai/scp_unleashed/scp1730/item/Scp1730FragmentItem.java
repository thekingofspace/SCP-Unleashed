package net.rai.scp_unleashed.scp1730.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730Symbols;
import net.rai.scp_unleashed.scp1730.Scp1730World;

import java.util.List;

public class Scp1730FragmentItem extends Item {
    public Scp1730FragmentItem(Properties Props) {
        super(Props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (World.isClientSide) {
            return InteractionResultHolder.success(Stack);
        }
        if (!Scp1730World.inside(World) || !(User instanceof ServerPlayer Traveller)) {
            Stack.shrink(1);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.fragment_inert").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResultHolder.consume(Stack);
        }
        World.playSound(null, User.getX(), User.getY(), User.getZ(), Scp1730Registry.FragmentUse.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        Stack.shrink(1);
        Scp1730Symbols.release(Traveller);
        GatewayTravel.leave(Traveller);
        return InteractionResultHolder.consume(Stack);
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (World.isClientSide || Scp1730World.inside(World) || !(Holder instanceof Player User) || User.isCreative()) {
            return;
        }
        Stack.setCount(0);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp1730.fragment_crumbles").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @Override
    public boolean isFoil(ItemStack Stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(getDescriptionId() + ".detail").withStyle(ChatFormatting.GOLD));
        }
    }
}
