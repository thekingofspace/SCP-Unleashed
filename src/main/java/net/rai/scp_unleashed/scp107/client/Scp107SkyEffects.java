package net.rai.scp_unleashed.scp107.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.rai.scp_unleashed.scp107.Scp107RainKind;

import java.util.Optional;

public class Scp107SkyEffects extends DimensionSpecialEffects.OverworldEffects {
    @Override
    public boolean renderSnowAndRain(ClientLevel World, int Ticks, float Partial, LightTexture Lights, double CameraX, double CameraY, double CameraZ) {
        Optional<Scp107RainKind> Kind = Scp107ClientRain.shownKind(World);
        if (Kind.isEmpty()) {
            return false;
        }
        Scp107RainRenderer.render(World, Kind.get(), Ticks, Partial, Lights, CameraX, CameraY, CameraZ);
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel World, int Ticks, Camera View) {
        Optional<Scp107RainKind> Kind = Scp107ClientRain.shownKind(World);
        if (Kind.isEmpty()) {
            return false;
        }
        Scp107RainRenderer.tick(World, Kind.get(), Ticks, View);
        return true;
    }
}
