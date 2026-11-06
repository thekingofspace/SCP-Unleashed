package net.rai.scp_unleashed.extraction.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.extraction.ExtractionRegistry;
import net.rai.scp_unleashed.extraction.RiftDetectorItem;
import net.rai.scp_unleashed.goggles.Goggles;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class DetectorTracker {
    public static final int FadeTicks = 4;
    public static final int FlashTicks = 2;
    public static final int PruneAge = 40;
    private static final RandomSource Noise = RandomSource.create();
    private static final Map<Integer, Cached> Cache = new HashMap<>();
    @Nullable
    private static Reading Current;
    private static float Alpha;
    private static float AlphaO;
    private static long LastClick = Long.MIN_VALUE / 2;
    private static long Now;

    private DetectorTracker() {
    }

    public record Reading(int RiftId, FragmentRarity Grade, float Strength) {
    }

    private record Cached(long Tick, @Nullable Reading Found) {
    }

    public static boolean isHolding(LivingEntity User) {
        return User.getMainHandItem().getItem() instanceof RiftDetectorItem || User.getOffhandItem().getItem() instanceof RiftDetectorItem
                || Goggles.isWearing(User, Goggles.Kind.RiftDetector);
    }

    @Nullable
    public static Reading current() {
        return Current;
    }

    public static float alpha(float PartialTick) {
        return Mth.lerp(PartialTick, AlphaO, Alpha);
    }

    public static boolean flashing() {
        return Now - LastClick < FlashTicks;
    }

    @Nullable
    public static Reading readingFor(LivingEntity Holder) {
        long Tick = Holder.level().getGameTime();
        Cached Entry = Cache.get(Holder.getId());
        if (Entry != null && Entry.Tick() == Tick) {
            return Entry.Found();
        }
        Reading Found = scan(Holder);
        Cache.put(Holder.getId(), new Cached(Tick, Found));
        return Found;
    }

    @Nullable
    public static Reading scan(LivingEntity Holder) {
        Vec3 Eye = Holder.getEyePosition();
        RiftEntity Best = null;
        double Nearest = Double.MAX_VALUE;
        for (RiftEntity Rift : Holder.level().getEntities(RiftRegistry.Rift.get(), new AABB(Eye, Eye).inflate(RiftDetectorItem.maxRange()), Candidate -> Candidate.isAlive() && !Candidate.isCollapsing())) {
            double Distance = Rift.getCenter(1.0F).distanceTo(Eye);
            if (Distance <= RiftDetectorItem.rangeFor(Rift.getRarity()) && Distance < Nearest) {
                Best = Rift;
                Nearest = Distance;
            }
        }
        if (Best == null) {
            return null;
        }
        float Strength = (float) Mth.clamp(1.0 - Nearest / RiftDetectorItem.rangeFor(Best.getRarity()), 0.0, 1.0);
        return new Reading(Best.getId(), Best.getRarity(), Strength);
    }

    public static void tick(Minecraft Game) {
        ClientLevel World = Game.level;
        LocalPlayer User = Game.player;
        if (World == null || User == null) {
            reset();
            return;
        }
        Now = World.getGameTime();
        AlphaO = Alpha;
        boolean Holding = isHolding(User);
        Reading Found = Holding ? readingFor(User) : null;
        if (Found != null) {
            if (Current == null || Current.RiftId() != Found.RiftId()) {
                play(Game, ExtractionRegistry.DetectorPing.get(), 0.5F, 1.15F - 0.06F * Found.Grade().getTier());
            }
            float Rate = 0.8F + 16.0F * (float) Math.pow(Found.Strength(), 1.7);
            if (Noise.nextFloat() < 1.0F - (float) Math.exp(-Rate / 20.0F)) {
                LastClick = Now;
                play(Game, ExtractionRegistry.DetectorClick.get(), 0.2F + 0.3F * Found.Strength(), 0.92F + Noise.nextFloat() * 0.16F);
            }
        }
        Current = Found;
        Alpha = Mth.clamp(Alpha + (Holding ? 1.0F : -1.0F) / FadeTicks, 0.0F, 1.0F);
        if (Now % 20L == 0L) {
            Cache.entrySet().removeIf(Entry -> Now - Entry.getValue().Tick() > PruneAge);
        }
    }

    private static void play(Minecraft Game, SoundEvent Sound, float Volume, float Pitch) {
        Game.getSoundManager().play(new SimpleSoundInstance(Sound.getLocation(), SoundSource.PLAYERS, Volume, Pitch, SoundInstance.createUnseededRandom(),
                false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
    }

    private static void reset() {
        Cache.clear();
        Current = null;
        Alpha = 0.0F;
        AlphaO = 0.0F;
    }
}
