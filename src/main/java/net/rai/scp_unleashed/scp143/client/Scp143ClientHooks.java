package net.rai.scp_unleashed.scp143.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleTypes;
import net.rai.scp_unleashed.scp143.Scp143PetalPath;
import net.rai.scp_unleashed.scp143.network.Scp143CutPayload;
import net.rai.scp_unleashed.scp143.network.Scp143PetalPayload;
import net.rai.scp_unleashed.scp143.network.Scp143SitePayload;

public final class Scp143ClientHooks {
    private Scp143ClientHooks() {
    }

    public static void petals(Scp143PetalPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        SpriteSet Sprites = Scp143ClientState.sprites();
        if (Client.level == null || Sprites == null) {
            return;
        }
        for (Scp143PetalPath.Spawn Petal : Payload.Petals()) {
            if (Scp143ClientState.has(Petal.Id()) || Client.level.getGameTime() - Petal.Born() > Scp143PetalPath.Life) {
                continue;
            }
            Scp143PetalParticle Made = new Scp143PetalParticle(Client.level, Petal, Sprites, true);
            Client.particleEngine.add(Made);
            Scp143ClientState.track(Petal.Id(), Made);
        }
    }

    public static void cut(Scp143CutPayload Payload) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null) {
            return;
        }
        Scp143PetalParticle Found = Scp143ClientState.find(Payload.Id());
        if (Found != null && Found.isAlive()) {
            Found.cut();
            return;
        }
        for (int Index = 0; Index < 3; Index++) {
            Client.level.addParticle(ParticleTypes.CRIT, Payload.X(), Payload.Y(), Payload.Z(), (Client.level.random.nextDouble() - 0.5) * 0.3,
                    Client.level.random.nextDouble() * 0.2, (Client.level.random.nextDouble() - 0.5) * 0.3);
        }
    }

    public static void site(Scp143SitePayload Payload) {
        Scp143ClientState.site(Payload.Known(), Payload.Base());
    }
}
