package net.rai.scp_unleashed.ultralaser.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunAmmo;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.client.GunClientSounds;
import net.rai.scp_unleashed.guns.network.GunReloadPayload;
import net.rai.scp_unleashed.ultralaser.UltralaserItem;
import net.rai.scp_unleashed.ultralaser.UltralaserPriming;
import net.rai.scp_unleashed.ultralaser.network.UltralaserTriggerPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class UltralaserInput {
    public static final float PrimeSlowdown = 0.45F;
    private static boolean Pressed;

    private UltralaserInput() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void tickStart(ClientTickEvent.Pre Event) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (User == null) {
            Pressed = false;
            return;
        }
        ItemStack Stack = User.getMainHandItem();
        if (!UltralaserItem.isUltralaser(Stack)) {
            letGo();
            return;
        }
        boolean Free = Game.screen == null && Game.getOverlay() == null;
        int Clicks = 0;
        if (Free) {
            while (Game.options.keyAttack.consumeClick()) {
                Clicks++;
            }
        }
        boolean Held = Free && Game.options.keyAttack.isDown() && User.isAlive() && !User.isSpectator();
        if (Pressed) {
            if (!Held) {
                letGo();
            } else if (GunItem.getCharging(Stack) != null) {
                User.setSprinting(false);
            }
            return;
        }
        if (Clicks <= 0) {
            return;
        }
        if (GunItem.getReload(Stack) != null) {
            return;
        }
        if (GunItem.getAmmo(Stack) <= 0) {
            dryFire(User, Stack);
            return;
        }
        if (User.getCooldowns().isOnCooldown(Stack.getItem())) {
            User.displayClientMessage(Component.translatable(UltralaserPriming.MessagePrefix + "venting").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            GunClientSounds.play(GunSounds.dryFire(), 0.6F, GunSounds.PitchJitter);
            return;
        }
        if (!Held) {
            User.displayClientMessage(Component.translatable(UltralaserPriming.MessagePrefix + "hold", Component.keybind("key.attack")).withStyle(ChatFormatting.AQUA), true);
            return;
        }
        Pressed = true;
        PacketDistributor.sendToServer(new UltralaserTriggerPayload(true));
    }

    private static void letGo() {
        if (Pressed) {
            Pressed = false;
            PacketDistributor.sendToServer(new UltralaserTriggerPayload(false));
        }
    }

    private static void dryFire(LocalPlayer User, ItemStack Stack) {
        GunClientSounds.play(GunSounds.dryFire(), 1.0F, GunSounds.PitchJitter);
        if (GunItem.isBusy(Stack)) {
            return;
        }
        if (!User.getAbilities().instabuild && GunAmmo.count(User, GunKind.Ultralaser) <= 0) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.guns.no_ammo", GunKind.Ultralaser.getAmmo().getDescription()), true);
            return;
        }
        PacketDistributor.sendToServer(GunReloadPayload.Instance);
    }

    @SubscribeEvent
    public static void movement(MovementInputUpdateEvent Event) {
        if (Event.getEntity() instanceof LocalPlayer User && !User.isPassenger() && UltralaserEffects.isCharging(User)) {
            Event.getInput().forwardImpulse *= PrimeSlowdown;
            Event.getInput().leftImpulse *= PrimeSlowdown;
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Pressed = false;
    }
}
