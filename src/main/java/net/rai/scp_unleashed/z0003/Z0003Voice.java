package net.rai.scp_unleashed.z0003;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public final class Z0003Voice {
    public static final double OwnerReach = 64.0;
    public static final String ArmedKey = "message.scp_unleashed.z0003.armed";
    public static final String RadiusClampedKey = "message.scp_unleashed.z0003.armed.radius";
    public static final String DelayClampedKey = "message.scp_unleashed.z0003.armed.delay";
    public static final String BothClampedKey = "message.scp_unleashed.z0003.armed.both";
    public static final float ClickVolume = 0.35f;

    private Z0003Voice() {
    }

    public static int heard(ServerPlayer Speaker, String Raw) {
        Optional<Z0003Phrase.Setting> Parsed = Z0003Phrase.parse(Raw);
        return Parsed.map(Setting -> apply(Speaker, Setting)).orElse(0);
    }

    public static int apply(ServerPlayer Speaker, Z0003Phrase.Setting Setting) {
        if (Z0003Petrify.isStatue(Speaker) || Speaker.isSpectator()) {
            return 0;
        }
        ServerLevel World = Speaker.serverLevel();
        Z0003Arming Armed = Z0003Arming.of(Setting, World.getGameTime());
        int Count = 0;
        for (InteractionHand Hand : InteractionHand.values()) {
            ItemStack Held = Speaker.getItemInHand(Hand);
            if (Z0003DeviceItem.is(Held)) {
                Z0003DeviceItem.arm(Held, Armed);
                Count++;
            }
        }
        if (Count > 0) {
            Z0003Registry.play(World, Speaker.position(), Z0003Registry.ArmLayers, SoundSource.PLAYERS, ClickVolume, 1.0f);
        } else {
            AABB Box = Speaker.getBoundingBox().inflate(OwnerReach);
            for (Z0003Device Found : World.getEntitiesOfClass(Z0003Device.class, Box, Candidate -> owns(Speaker, Candidate.getOwner()))) {
                if (Found.arm(Armed)) {
                    Z0003Registry.play(World, Found.position(), Z0003Registry.ArmLayers, SoundSource.NEUTRAL, ClickVolume, 1.0f);
                    Count++;
                }
            }
            for (ItemEntity Found : World.getEntitiesOfClass(ItemEntity.class, Box, Candidate -> Z0003DeviceItem.is(Candidate.getItem()) && owns(Speaker,
                    Candidate.getOwner()))) {
                ItemStack Stack = Found.getItem().copy();
                Z0003DeviceItem.arm(Stack, Armed);
                Found.setItem(Stack);
                Z0003Registry.play(World, Found.position(), Z0003Registry.ArmLayers, SoundSource.NEUTRAL, ClickVolume, 1.0f);
                Count++;
            }
        }
        if (Count > 0) {
            Speaker.displayClientMessage(armedMessage(Setting), true);
        }
        return Count;
    }

    public static Component armedMessage(Z0003Phrase.Setting Setting) {
        String Radius = number(Setting.Radius());
        String Seconds = number(Setting.seconds());
        if (Setting.RadiusClamped() && Setting.DelayClamped()) {
            return Component.translatable(BothClampedKey, Radius, Seconds);
        }
        if (Setting.RadiusClamped()) {
            return Component.translatable(RadiusClampedKey, Radius, Seconds, Radius);
        }
        if (Setting.DelayClamped()) {
            return Component.translatable(DelayClampedKey, Radius, Seconds, Seconds);
        }
        return Component.translatable(ArmedKey, Radius, Seconds);
    }

    private static boolean owns(ServerPlayer Speaker, @Nullable Entity Owner) {
        return Owner != null && Owner.getUUID().equals(Speaker.getUUID());
    }

    public static String number(float Value) {
        float Rounded = Math.round(Value * 10.0f) / 10.0f;
        if (Rounded == Math.rint(Rounded)) {
            return Integer.toString((int) Rounded);
        }
        return String.format(Locale.ROOT, "%.1f", Rounded);
    }
}
