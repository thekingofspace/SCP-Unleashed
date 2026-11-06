package net.rai.scp_unleashed.microhid.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.client.GunView;
import net.rai.scp_unleashed.microhid.MicroHidBeam;
import net.rai.scp_unleashed.microhid.MicroHidItem;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class MicroHidInput {
    private MicroHidInput() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void tickStart(ClientTickEvent.Pre Event) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (User == null || !MicroHidItem.isMicroHid(User.getMainHandItem()) || Game.screen != null || Game.getOverlay() != null) {
            return;
        }
        int Clicks = 0;
        while (Game.options.keyAttack.consumeClick()) {
            Clicks++;
        }
        if (Clicks > 0 && !User.isUsingItem()) {
            User.displayClientMessage(Component.translatable(MicroHidBeam.MessagePrefix + "hold", Component.keybind("key.use")).withStyle(ChatFormatting.AQUA), true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void movement(MovementInputUpdateEvent Event) {
        if (!(Event.getEntity() instanceof LocalPlayer User) || User.isPassenger() || !User.isUsingItem() || !MicroHidItem.isMicroHid(User.getUseItem())) {
            return;
        }
        float Scale = MicroHidBeam.moveFactor(User.getTicksUsingItem()) / GunView.of(GunKind.MicroHid).AimSpeed();
        Event.getInput().forwardImpulse *= Scale;
        Event.getInput().leftImpulse *= Scale;
        if (User.isSprinting()) {
            User.setSprinting(false);
        }
    }
}
