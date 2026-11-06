package net.rai.scp_unleashed.particleemitter.client;

import net.minecraft.client.Minecraft;
import net.rai.scp_unleashed.particleemitter.ParticleEmitterBlockEntity;

public final class EmitterEffects {
    private EmitterEffects() {
    }

    public static void tick(ParticleEmitterBlockEntity Emitter) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null || Emitter.getLevel() != Game.level) {
            return;
        }
        switch (Emitter.getMode()) {
            case ColdFloor -> ColdFloorEffect.tick(Game, Emitter);
        }
    }
}
