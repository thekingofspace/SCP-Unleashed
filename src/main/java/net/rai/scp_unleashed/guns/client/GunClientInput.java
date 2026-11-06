package net.rai.scp_unleashed.guns.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunAmmo;
import net.rai.scp_unleashed.guns.GunHeat;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.network.GunFirePayload;
import net.rai.scp_unleashed.guns.network.GunReloadPayload;
import net.rai.scp_unleashed.space.SpaceWeapons;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class GunClientInput {
    public static final int ClickMemory = 4;
    public static final int DryFireDelay = 6;
    public static final float UseSlowdown = 0.2F;
    private static final long[] NextShot = new long[GunKind.values().length];
    private static int Clicks;
    private static long ClickTick;
    private static boolean ReloadQueued;
    private static boolean DryClicked;

    private GunClientInput() {
    }

    @SubscribeEvent
    public static void tickStart(ClientTickEvent.Pre Event) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (User == null) {
            return;
        }
        boolean Holding = GunItem.kindOf(User.getMainHandItem()) != null;
        if (Holding && Game.screen == null && Game.getOverlay() == null) {
            while (Game.options.keyAttack.consumeClick()) {
                Clicks++;
                ClickTick = GunClientState.ticks();
            }
        }
        while (GunsClientEvents.ReloadKey.consumeClick()) {
            ReloadQueued = Holding;
        }
    }

    @SubscribeEvent
    public static void tickEnd(ClientTickEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        GunClientState.tick(Game);
        GunEffects.tick(Game);
        GunClientSounds.tick();
        LocalPlayer User = Game.player;
        if (User == null || Game.level == null) {
            Clicks = 0;
            ReloadQueued = false;
            return;
        }
        ItemStack Stack = User.getMainHandItem();
        GunKind Kind = GunItem.kindOf(Stack);
        if (Kind == null || Game.screen != null || User.isSpectator() || !User.isAlive()) {
            Clicks = 0;
            ReloadQueued = false;
            DryClicked = false;
            return;
        }
        long Now = GunClientState.ticks();
        if (Now - ClickTick > ClickMemory) {
            Clicks = 0;
        }
        if (ReloadQueued) {
            ReloadQueued = false;
            requestReload(User, Stack, Kind, true);
        }
        GunStats Stats = Kind.getStats();
        boolean Held = Game.options.keyAttack.isDown();
        if (!Held) {
            DryClicked = false;
        }
        boolean Trigger = Clicks > 0 || Stats.isAutomatic() && Held;
        if (!Trigger || Now < NextShot[Kind.ordinal()] || GunItem.isBusy(Stack) || GunHeat.venting(Stack, Game.level.getGameTime())) {
            return;
        }
        if (Stack.getItem() instanceof GunItem Gun && Gun.blocksTrigger(User, Stack)) {
            Clicks = 0;
            return;
        }
        Clicks = 0;
        if (SpaceWeapons.jammed(User, Stack)) {
            if (!DryClicked || !Stats.isAutomatic()) {
                GunClientSounds.play(GunSounds.dryFire(), 1.0F, GunSounds.PitchJitter);
                User.displayClientMessage(SpaceWeapons.hint(), true);
                DryClicked = true;
            }
            NextShot[Kind.ordinal()] = Now + DryFireDelay;
            return;
        }
        if (Stats.usesCharges()) {
            if (GunItem.getCharges(Stack) > 0) {
                PacketDistributor.sendToServer(new GunFirePayload(Kind.ordinal(), GunClientState.fireSequence(false)));
                NextShot[Kind.ordinal()] = Now + Stats.getInterval();
            }
            return;
        }
        if (GunClientState.predictedAmmo(User, Stack) <= 0) {
            if (!DryClicked || !Stats.isAutomatic()) {
                GunClientSounds.play(GunSounds.dryFire(Kind), 1.0F, GunSounds.PitchJitter);
                DryClicked = true;
                requestReload(User, Stack, Kind, false);
            }
            NextShot[Kind.ordinal()] = Now + DryFireDelay;
            return;
        }
        PacketDistributor.sendToServer(new GunFirePayload(Kind.ordinal(), GunClientState.fireSequence(!User.getAbilities().instabuild)));
        NextShot[Kind.ordinal()] = Now + Stats.getInterval();
        GunClientState.onLocalShot(User, Kind);
    }

    private static void requestReload(LocalPlayer User, ItemStack Stack, GunKind Kind, boolean Explicit) {
        GunStats Stats = Kind.getStats();
        if (Stats.usesCharges() || !Kind.usesAmmo() || GunItem.isBusy(Stack) || GunItem.getAmmo(Stack) >= Stats.getCapacity()) {
            return;
        }
        if (!User.getAbilities().instabuild && GunAmmo.count(User, Kind) <= 0) {
            if (Explicit) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.guns.no_ammo", Kind.getAmmo().getDescription()), true);
            }
            return;
        }
        PacketDistributor.sendToServer(GunReloadPayload.Instance);
    }

    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered Event) {
        LocalPlayer User = Minecraft.getInstance().player;
        if (Event.isAttack() && User != null && GunItem.kindOf(User.getMainHandItem()) != null) {
            Event.setCanceled(true);
            Event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void movement(MovementInputUpdateEvent Event) {
        if (!(Event.getEntity() instanceof LocalPlayer User) || !User.isUsingItem() || User.isPassenger()) {
            return;
        }
        GunKind Kind = GunItem.kindOf(User.getUseItem());
        if (Kind == null) {
            return;
        }
        float Boost = GunView.of(Kind).AimSpeed() / UseSlowdown;
        Event.getInput().forwardImpulse *= Boost;
        Event.getInput().leftImpulse *= Boost;
    }

    @SubscribeEvent
    public static void turn(CalculatePlayerTurnEvent Event) {
        float Zoom = GunClientState.zoom(1.0F);
        if (Zoom >= 0.999F) {
            return;
        }
        double Base = Event.getMouseSensitivity() * 0.6 + 0.2;
        double Scaled = Math.cbrt(Zoom) * Base;
        Event.setMouseSensitivity((Scaled - 0.2) / 0.6);
    }

    @SubscribeEvent
    public static void fieldOfView(ViewportEvent.ComputeFov Event) {
        if (!Event.usedConfiguredFov()) {
            return;
        }
        float Zoom = GunClientState.zoom((float) Event.getPartialTick());
        if (Zoom < 0.999F) {
            Event.setFOV(Event.getFOV() * Zoom);
        }
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre Event) {
        Minecraft Game = Minecraft.getInstance();
        GunClientState.applyRecoil(Game.isPaused() ? null : Game.player);
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        GunClientState.reset();
        GunEffects.clear();
        GunClientSounds.clear();
        GunVoices.clear();
        Clicks = 0;
        ReloadQueued = false;
    }
}
