package net.rai.scp_unleashed.guards;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class GuardSounds {
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Bind = List.of(GuardsRegistry.LinkLock, GuardsRegistry.LinkTone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Linked = List.of(GuardsRegistry.LinkClick, GuardsRegistry.LinkTone, GuardsRegistry.LinkLock);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Unlinked = List.of(GuardsRegistry.LinkClick, GuardsRegistry.UnlinkTone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Refused = List.of(GuardsRegistry.LinkClick, GuardsRegistry.UnlinkTone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Written = List.of(GuardsRegistry.OrdersWrite, GuardsRegistry.OrdersStamp);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Assigned = List.of(GuardsRegistry.OrdersHand, GuardsRegistry.AlertKey, GuardsRegistry.LinkTone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Cleared = List.of(GuardsRegistry.OrdersClear, GuardsRegistry.UnlinkTone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Alert = List.of(GuardsRegistry.AlertKey, GuardsRegistry.AlertTone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Boot = List.of(GuardsRegistry.TerminalBoot, GuardsRegistry.LinkTone);

    private GuardSounds() {
    }

    public static void play(Level World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source, float Volume, float Pitch) {
        if (World.isClientSide) {
            return;
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Part : Layers) {
            World.playSound(null, At.x, At.y, At.z, Part.get(), Source, Volume, Pitch * (0.97f + World.getRandom().nextFloat() * 0.06f));
        }
    }
}
