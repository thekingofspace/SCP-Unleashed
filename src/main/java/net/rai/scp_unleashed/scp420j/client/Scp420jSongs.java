package net.rai.scp_unleashed.scp420j.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp420j.Scp420jFloat;
import net.rai.scp_unleashed.scp420j.Scp420jRegistry;
import net.rai.scp_unleashed.scp420j.network.Scp420jSongPayload;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Scp420jSongs {
    public static final int StartGrace = 10;
    public static final int Overrun = 60;
    public static final int NoteEvery = 7;
    public static final int RingEvery = 34;
    private static final Map<Integer, Playing> Active = new HashMap<>();

    private Scp420jSongs() {
    }

    private static final class Playing {
        private final EntityBoundSoundInstance Sound;
        private final Entity Source;
        private int Age;

        private Playing(EntityBoundSoundInstance Sound, Entity Source) {
            this.Sound = Sound;
            this.Source = Source;
        }
    }

    public static void apply(Scp420jSongPayload Payload) {
        if (Payload.Playing()) {
            start(Payload.EntityId());
        } else {
            stop(Payload.EntityId());
        }
    }

    public static boolean isPlaying(Entity Source) {
        Playing Found = Active.get(Source.getId());
        return Found != null && Found.Source == Source;
    }

    private static void start(int EntityId) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null) {
            return;
        }
        Entity Source = World.getEntity(EntityId);
        if (Source == null) {
            return;
        }
        stop(EntityId);
        EntityBoundSoundInstance Sound = new EntityBoundSoundInstance(Scp420jRegistry.Mandeville.get(), SoundSource.RECORDS, 1.0f, 1.0f, Source, World.getRandom().nextLong());
        Client.getSoundManager().play(Sound);
        Active.put(EntityId, new Playing(Sound, Source));
        if (Source == Client.player) {
            Client.getMusicManager().stopPlaying();
        }
    }

    private static void stop(int EntityId) {
        Playing Found = Active.remove(EntityId);
        if (Found != null) {
            Minecraft.getInstance().getSoundManager().stop(Found.Sound);
        }
    }

    public static void tick(Minecraft Client) {
        if (Client.level == null) {
            reset();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        Iterator<Map.Entry<Integer, Playing>> Entries = Active.entrySet().iterator();
        while (Entries.hasNext()) {
            Playing Found = Entries.next().getValue();
            Found.Age++;
            boolean Finished = Found.Age > StartGrace && !Client.getSoundManager().isActive(Found.Sound);
            if (Found.Source.isRemoved() || Found.Source.level() != Client.level || Finished || Found.Age > Scp420jFloat.TrackTicks + Overrun) {
                Client.getSoundManager().stop(Found.Sound);
                Entries.remove();
                continue;
            }
            fx(Client, Found);
        }
    }

    private static void fx(Minecraft Client, Playing Found) {
        Entity Source = Found.Source;
        ClientLevel World = Client.level;
        RandomSource Random = World.getRandom();
        boolean Self = Source == Client.player && Client.options.getCameraType().isFirstPerson();
        if (Found.Age % NoteEvery == 0) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = Self ? 0.9 + Random.nextDouble() * 0.5 : 0.45 + Random.nextDouble() * 0.35;
            double X = Source.getX() + Math.cos(Angle) * Radius;
            double Y = Source.getY() + Source.getBbHeight() * (0.45 + Random.nextDouble() * 0.5);
            double Z = Source.getZ() + Math.sin(Angle) * Radius;
            World.addParticle(Scp420jRegistry.Note.get(), X, Y, Z, Math.cos(Angle) * 0.012, 0.012 + Random.nextDouble() * 0.01, Math.sin(Angle) * 0.012);
        }
        if (Found.Age % RingEvery == RingEvery / 2) {
            Vec3 Look = Source.getLookAngle();
            Vec3 Mouth = Self ? Source.getEyePosition().add(Look.scale(1.1)).add(0.0, -0.05, 0.0) : Source.getEyePosition().add(Look.scale(0.35)).add(0.0, -0.1, 0.0);
            World.addParticle(Scp420jRegistry.Ring.get(), Mouth.x, Mouth.y, Mouth.z, Look.x * 0.025, 0.012, Look.z * 0.025);
        }
    }

    public static void reset() {
        Minecraft Client = Minecraft.getInstance();
        for (Playing Found : Active.values()) {
            Client.getSoundManager().stop(Found.Sound);
        }
        Active.clear();
    }
}
