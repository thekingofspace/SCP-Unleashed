package net.rai.scp_unleashed.radio;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public enum RadioBleedKind implements StringRepresentable {
    Clank("clank", () -> MobEffects.DARKNESS, 60, false, () -> List.of(RadioRegistry.BleedClank, RadioRegistry.BleedClankShiver, RadioRegistry.Hiss)),
    Pulse("pulse", () -> MobEffects.HUNGER, 160, false, () -> List.of(RadioRegistry.BleedPulse, RadioRegistry.BleedPulseBody, RadioRegistry.BleedWhisper)),
    Refrain("refrain", () -> MobEffects.CONFUSION, 100, false, () -> List.of(RadioRegistry.BleedRefrain, RadioRegistry.BleedRefrainBell, RadioRegistry.Hiss)),
    Corrupt("corrupt", () -> MobEffects.MOVEMENT_SLOWDOWN, 80, true, () -> List.of(RadioRegistry.Corrupt, RadioRegistry.CorruptCrackle, RadioRegistry.CorruptBlip)),
    Glitch("glitch", null, 0, true, () -> List.of(RadioRegistry.Glitch, RadioRegistry.CorruptBlip, RadioRegistry.Hiss));

    private final String Name;
    @Nullable
    private final Supplier<Holder<MobEffect>> Effect;
    private final int Ticks;
    private final boolean Machine;
    private final Supplier<List<DeferredHolder<SoundEvent, SoundEvent>>> Layers;

    RadioBleedKind(String Name, @Nullable Supplier<Holder<MobEffect>> Effect, int Ticks, boolean Machine, Supplier<List<DeferredHolder<SoundEvent, SoundEvent>>> Layers) {
        this.Name = Name;
        this.Effect = Effect;
        this.Ticks = Ticks;
        this.Machine = Machine;
        this.Layers = Layers;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }

    @Nullable
    public Holder<MobEffect> effect() {
        return Effect == null ? null : Effect.get();
    }

    public int ticks() {
        return Ticks;
    }

    public boolean isMachine() {
        return Machine;
    }

    public List<DeferredHolder<SoundEvent, SoundEvent>> layers() {
        return Layers.get();
    }

    @Nullable
    public static RadioBleedKind byName(String Text) {
        for (RadioBleedKind Each : values()) {
            if (Each.Name.equals(Text)) {
                return Each;
            }
        }
        return null;
    }
}
