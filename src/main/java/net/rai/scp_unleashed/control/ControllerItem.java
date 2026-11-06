package net.rai.scp_unleashed.control;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class ControllerItem extends Item {
    public static final String ReleaseKey = "key.scp_unleashed.release_control";

    public ControllerItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack Stack, Player User, LivingEntity Target, InteractionHand Hand) {
        if (Target instanceof Controllable) {
            if (User instanceof ServerPlayer Server) {
                ControlManager.possess(Server, Target);
            }
            return InteractionResult.sidedSuccess(User.level().isClientSide);
        }
        if (User instanceof ServerPlayer Server) {
            Component Refusal = Target instanceof ControlRefusing Refusing ? Refusing.controlRefusal()
                    : Component.translatable("message.scp_unleashed.control.no_interface", Target.getDisplayName());
            Server.displayClientMessage(Refusal.copy().withStyle(ChatFormatting.RED), true);
            Server.playNotifySound(ControlRegistry.Refuse.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
        }
        return InteractionResult.sidedSuccess(User.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.controller.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.controller.home").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.controller.release", Component.keybind(ReleaseKey).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.controller.death").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.controller.chain").withStyle(ChatFormatting.GOLD));
        }
    }
}
