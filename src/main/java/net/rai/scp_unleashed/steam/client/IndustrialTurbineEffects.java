package net.rai.scp_unleashed.steam.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.steam.IndustrialTurbineBlockEntity;
import net.rai.scp_unleashed.steam.SteamRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class IndustrialTurbineEffects {
    public static final float SoundStart = 0.02f;

    private static final Map<IndustrialTurbineBlockEntity, List<IndustrialTurbineSound>> Loops = new WeakHashMap<>();

    private IndustrialTurbineEffects() {
    }

    private static List<TurbineLoopSound.Layer> layers() {
        return List.of(
                new TurbineLoopSound.Layer(SteamRegistry.IndustrialTurbineRumble.get(), 0.6f, 0.0f, 0.85f, 0.5f),
                new TurbineLoopSound.Layer(SteamRegistry.IndustrialTurbineDrone.get(), 0.45f, 0.05f, 0.8f, 0.45f),
                new TurbineLoopSound.Layer(SteamRegistry.IndustrialTurbineRush.get(), 0.5f, 0.12f, 0.75f, 0.55f),
                new TurbineLoopSound.Layer(SteamRegistry.IndustrialTurbineHum.get(), 0.4f, 0.25f, 0.75f, 0.6f));
    }

    public static void tick(IndustrialTurbineBlockEntity Machine) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Machine.getLevel();
        if (World == null || Game.level != World) {
            return;
        }
        if (Machine.getSpin() <= SoundStart) {
            return;
        }
        SoundManager Sounds = Game.getSoundManager();
        List<IndustrialTurbineSound> Playing = Loops.get(Machine);
        boolean Alive = Playing != null && !Playing.isEmpty();
        if (Alive) {
            for (IndustrialTurbineSound Loop : Playing) {
                Alive &= Sounds.isActive(Loop);
            }
        }
        if (Alive) {
            return;
        }
        if (Playing != null) {
            for (IndustrialTurbineSound Loop : Playing) {
                Sounds.stop(Loop);
            }
        }
        List<IndustrialTurbineSound> Started = new ArrayList<>();
        for (TurbineLoopSound.Layer Kind : layers()) {
            IndustrialTurbineSound Loop = new IndustrialTurbineSound(Machine, Kind);
            Started.add(Loop);
            Sounds.play(Loop);
        }
        Loops.put(Machine, Started);
    }
}
