package net.rai.scp_unleashed.liquidsound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class LiquidSoundRegistry {
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<SoundEvent, SoundEvent> SpringSwim = sound("scp006_swim");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpringSwimRipple = sound("scp006_swim_ripple");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpringSplash = sound("scp006_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpringSplashRipple = sound("scp006_splash_ripple");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpringSplashHeavy = sound("scp006_splash_heavy");

    public static final DeferredHolder<SoundEvent, SoundEvent> OilSwim = sound("oil_swim");
    public static final DeferredHolder<SoundEvent, SoundEvent> OilSwimGloop = sound("oil_swim_gloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> OilSplash = sound("oil_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> OilSplashGloop = sound("oil_splash_gloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> OilSplashHeavy = sound("oil_splash_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> OilFlow = sound("oil_flow");
    public static final DeferredHolder<SoundEvent, SoundEvent> OilFlowGloop = sound("oil_flow_gloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> OilBubble = sound("oil_bubble");

    public static final DeferredHolder<SoundEvent, SoundEvent> WasteSwim = sound("waste_water_swim");
    public static final DeferredHolder<SoundEvent, SoundEvent> WasteSwimMuck = sound("waste_water_swim_muck");
    public static final DeferredHolder<SoundEvent, SoundEvent> WasteSplash = sound("waste_water_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> WasteSplashMuck = sound("waste_water_splash_muck");
    public static final DeferredHolder<SoundEvent, SoundEvent> WasteSplashHeavy = sound("waste_water_splash_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> WasteFlow = sound("waste_water_flow");
    public static final DeferredHolder<SoundEvent, SoundEvent> WasteBubble = sound("waste_water_bubble");

    public static final DeferredHolder<SoundEvent, SoundEvent> RedSwim = sound("scp354_red_swim");
    public static final DeferredHolder<SoundEvent, SoundEvent> RedSwimDrag = sound("scp354_red_swim_drag");
    public static final DeferredHolder<SoundEvent, SoundEvent> RedSplash = sound("scp354_red_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> RedSplashGloop = sound("scp354_red_splash_gloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> RedSplashHeavy = sound("scp354_red_splash_heavy");

    public static final LiquidSoundSet Spring = new LiquidSoundSet(List.of(SpringSwim, SpringSwimRipple), List.of(SpringSplash, SpringSplashRipple),
            List.of(SpringSplashHeavy, SpringSplashRipple), List.of(), List.of());
    public static final LiquidSoundSet Oil = new LiquidSoundSet(List.of(OilSwim, OilSwimGloop), List.of(OilSplash, OilSplashGloop),
            List.of(OilSplashHeavy, OilSplashGloop), List.of(OilFlow, OilFlowGloop), List.of(OilBubble));
    public static final LiquidSoundSet Waste = new LiquidSoundSet(List.of(WasteSwim, WasteSwimMuck), List.of(WasteSplash, WasteSplashMuck),
            List.of(WasteSplashHeavy, WasteSplashMuck), List.of(WasteFlow), List.of(WasteBubble));
    public static final LiquidSoundSet Red = new LiquidSoundSet(List.of(RedSwim, RedSwimDrag), List.of(RedSplash, RedSplashGloop),
            List.of(RedSplashHeavy, RedSplashGloop), List.of(), List.of());

    private LiquidSoundRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        SoundRegister.register(ModEventBus);
    }
}
