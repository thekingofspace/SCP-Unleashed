package net.rai.scp_unleashed.scp323.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.scp323.network.Scp323WatchPayload;

public final class Scp323ClientWatch {
    public static final double FrozenSensitivity = -1.0 / 3.0;
    public static final float FadeRate = 0.05f;
    private static int CreatureId = Scp323WatchPayload.Nobody;
    private static float Fade;
    private static float FadeO;
    private static int Age;
    private static boolean HandHidden;

    private Scp323ClientWatch() {
    }

    public static void set(int Id) {
        CreatureId = Id;
        if (Id == Scp323WatchPayload.Nobody) {
            restoreCamera();
        } else {
            Age = 0;
        }
    }

    public static boolean watching() {
        return CreatureId != Scp323WatchPayload.Nobody;
    }

    public static float fade(float Partial) {
        return Mth.lerp(Partial, FadeO, Fade);
    }

    public static int age() {
        return Age;
    }

    public static void hideHand(Minecraft Client) {
        if (watching()) {
            Client.gameRenderer.setRenderHand(false);
            HandHidden = true;
        } else if (HandHidden) {
            Client.gameRenderer.setRenderHand(true);
            HandHidden = false;
        }
    }

    public static void reset() {
        if (HandHidden) {
            Minecraft.getInstance().gameRenderer.setRenderHand(true);
            HandHidden = false;
        }
        CreatureId = Scp323WatchPayload.Nobody;
        Fade = 0.0f;
        FadeO = 0.0f;
        Age = 0;
    }

    private static void restoreCamera() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player != null && Client.getCameraEntity() != Client.player) {
            Client.setCameraEntity(Client.player);
        }
    }

    private static void drain(KeyMapping Key) {
        while (Key.consumeClick()) {
        }
    }

    public static void tick(Minecraft Client) {
        FadeO = Fade;
        Fade = Mth.clamp(Fade + (watching() ? FadeRate : -FadeRate * 2.0f), 0.0f, 1.0f);
        LocalPlayer User = Client.player;
        if (!watching() || User == null || Client.level == null) {
            return;
        }
        Age++;
        drain(Client.options.keyInventory);
        drain(Client.options.keyDrop);
        drain(Client.options.keySwapOffhand);
        drain(Client.options.keyAttack);
        drain(Client.options.keyUse);
        drain(Client.options.keyPickItem);
        drain(Client.options.keyShift);
        drain(Client.options.keyJump);
        drain(Client.options.keySprint);
        for (KeyMapping Slot : Client.options.keyHotbarSlots) {
            drain(Slot);
        }
        Client.options.keyShift.setDown(false);
        Client.options.keySprint.setDown(false);
        Client.options.keyAttack.setDown(false);
        Client.options.keyUse.setDown(false);
        if (Client.screen instanceof AbstractContainerScreen<?>) {
            User.closeContainer();
        }
        Entity Creature = Client.level.getEntity(CreatureId);
        if (Creature != null && Client.getCameraEntity() != Creature) {
            Client.setCameraEntity(Creature);
        }
    }

    public static void freeze(Input Held) {
        if (!watching()) {
            return;
        }
        Held.forwardImpulse = 0.0f;
        Held.leftImpulse = 0.0f;
        Held.up = false;
        Held.down = false;
        Held.left = false;
        Held.right = false;
        Held.jumping = false;
        Held.shiftKeyDown = false;
    }
}
