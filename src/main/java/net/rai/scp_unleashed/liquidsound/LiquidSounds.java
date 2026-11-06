package net.rai.scp_unleashed.liquidsound;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.oil.OilLiquid;
import net.rai.scp_unleashed.oil.OilRegistry;
import net.rai.scp_unleashed.scp006.Scp006Registry;
import net.rai.scp_unleashed.scp354.Scp354Registry;
import net.rai.scp_unleashed.sulfur.SulfurRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class LiquidSounds {
    public static final int FlowChance = 64;
    public static final int BubbleChance = 180;
    public static final float HeavySplashVolume = 0.25f;

    private static volatile Map<FluidType, LiquidSoundSet> Sets;

    private LiquidSounds() {
    }

    private static Map<FluidType, LiquidSoundSet> sets() {
        Map<FluidType, LiquidSoundSet> Known = Sets;
        if (Known == null) {
            Known = new IdentityHashMap<>();
            Known.put(Scp006Registry.WaterType.get(), LiquidSoundRegistry.Spring);
            Known.put(Scp354Registry.RedType.get(), LiquidSoundRegistry.Red);
            Known.put(FissionRegistry.WasteWaterType.get(), LiquidSoundRegistry.Waste);
            Known.put(SulfurRegistry.AcidType.get(), SulfurRegistry.AcidSounds);
            for (OilLiquid Grade : OilRegistry.Liquids) {
                Known.put(Grade.getType().get(), LiquidSoundRegistry.Oil);
            }
            Sets = Known;
        }
        return Known;
    }

    @Nullable
    public static LiquidSoundSet of(FluidType Type) {
        return sets().get(Type);
    }

    @Nullable
    public static LiquidSoundSet setFor(Entity Body) {
        if (!Body.isInFluidType()) {
            return null;
        }
        for (Map.Entry<FluidType, LiquidSoundSet> Entry : sets().entrySet()) {
            if (Body.isInFluidType(Entry.getKey())) {
                return Entry.getValue();
            }
        }
        return null;
    }

    private static float motionVolume(Entity Body, float Alone, float Ridden) {
        Entity Rider = Objects.requireNonNullElse(Body.getControllingPassenger(), Body);
        Vec3 Motion = Rider.getDeltaMovement();
        float Speed = (float) Math.sqrt(Motion.x * Motion.x * 0.2 + Motion.y * Motion.y + Motion.z * Motion.z * 0.2);
        return Math.min(1.0f, Speed * (Rider == Body ? Alone : Ridden));
    }

    private static void play(Entity Body, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume) {
        float Pitch = 1.0f + (Body.getRandom().nextFloat() - Body.getRandom().nextFloat()) * 0.4f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            Body.playSound(Layer.get(), Volume, Pitch);
        }
    }

    public static void swim(Entity Body, LiquidSoundSet Sounds) {
        play(Body, Sounds.Swim(), motionVolume(Body, 0.35f, 0.4f));
    }

    public static void splash(Entity Body, LiquidSoundSet Sounds) {
        float Volume = motionVolume(Body, 0.2f, 0.9f);
        play(Body, Volume < HeavySplashVolume ? Sounds.Splash() : Sounds.HeavySplash(), Volume);
    }

    public static void ambience(Level World, BlockPos Pos, FluidState State, RandomSource Random, LiquidSoundSet Sounds) {
        if (!State.isSource() && !State.getValue(BaseFlowingFluid.FALLING)) {
            if (!Sounds.Flow().isEmpty() && Random.nextInt(FlowChance) == 0) {
                float Volume = Random.nextFloat() * 0.25f + 0.75f;
                float Pitch = Random.nextFloat() + 0.5f;
                for (DeferredHolder<SoundEvent, SoundEvent> Layer : Sounds.Flow()) {
                    World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Layer.get(), SoundSource.BLOCKS, Volume, Pitch, false);
                }
            }
            return;
        }
        if (State.isSource() && !Sounds.Bubble().isEmpty() && Random.nextInt(BubbleChance) == 0 && World.getFluidState(Pos.above()).isEmpty()) {
            float Volume = Random.nextFloat() * 0.3f + 0.6f;
            float Pitch = Random.nextFloat() * 0.4f + 0.8f;
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Sounds.Bubble()) {
                World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.9, Pos.getZ() + 0.5, Layer.get(), SoundSource.BLOCKS, Volume, Pitch, false);
            }
        }
    }
}
