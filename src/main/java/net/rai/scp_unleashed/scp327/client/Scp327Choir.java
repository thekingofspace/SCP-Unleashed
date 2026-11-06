package net.rai.scp_unleashed.scp327.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp327.Scp327Registry;
import net.rai.scp_unleashed.scp327.Scp327Siren;
import net.rai.scp_unleashed.scp327.song.Scp327Song;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class Scp327Choir {
    public static final SoundSource Channel = SoundSource.HOSTILE;
    public static final float Level = 1.0f;
    public static final float TeenLevel = 0.5f;
    public static final int FadeIn = 50;
    public static final int FadeOut = 40;
    public static final double HearRange = 64.0;
    public static final float Spread = 0.06f;
    private static final RandomSource Dice = RandomSource.create();
    private static final Map<Scp327Song.Voice, Supplier<SoundEvent>> Voices = new EnumMap<>(Scp327Song.Voice.class);
    private static final Map<Integer, Singer> Singers = new HashMap<>();
    private static List<List<Scp327Song.Note>> Adult;
    private static List<List<Scp327Song.Note>> Teen;

    static {
        Voices.put(Scp327Song.Voice.Hum, Scp327Registry.SongHum::get);
        Voices.put(Scp327Song.Voice.Breath, Scp327Registry.SongBreath::get);
        Voices.put(Scp327Song.Voice.Flute, Scp327Registry.SongFlute::get);
        Voices.put(Scp327Song.Voice.Chime, Scp327Registry.SongChime::get);
        Voices.put(Scp327Song.Voice.Bell, Scp327Registry.SongBell::get);
        Voices.put(Scp327Song.Voice.Harp, Scp327Registry.SongHarp::get);
        Voices.put(Scp327Song.Voice.Shimmer, Scp327Registry.SongShimmer::get);
        Voices.put(Scp327Song.Voice.Bed, Scp327Registry.SongBed::get);
    }

    private Scp327Choir() {
    }

    private static List<List<Scp327Song.Note>> timeline(List<Scp327Song.Note> Notes) {
        List<List<Scp327Song.Note>> Built = new ArrayList<>();
        for (int Tick = 0; Tick < Scp327Song.Length; Tick++) {
            Built.add(new ArrayList<>());
        }
        for (Scp327Song.Note Played : Notes) {
            Built.get(Played.Tick()).add(Played);
        }
        return Built;
    }

    private static List<List<Scp327Song.Note>> adult() {
        if (Adult == null) {
            Adult = timeline(Scp327Song.notes());
        }
        return Adult;
    }

    private static List<List<Scp327Song.Note>> teen() {
        if (Teen == null) {
            Teen = timeline(Scp327Song.hum());
        }
        return Teen;
    }

    public static float factor(Scp327Siren Creature) {
        float Base = 1.0f - Spread * 0.5f + Spread * ((Creature.getUUID().hashCode() & 0xFF) / 255.0f);
        return Creature.stage() == Scp327Siren.StageTeen ? Base * Scp327Song.TeenFactor : Base;
    }

    public static boolean wants(Scp327Siren Creature) {
        return Creature.isAlive() && Creature.isSinging() && Creature.stage() != Scp327Siren.StageBaby;
    }

    public static void tick(Minecraft Client) {
        ClientLevel World = Client.level;
        LocalPlayer User = Client.player;
        if (World == null || User == null) {
            reset(Client);
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        for (Scp327Siren Creature : World.getEntitiesOfClass(Scp327Siren.class, User.getBoundingBox().inflate(HearRange), Candidate -> wants(Candidate) && Candidate.heardBy(User))) {
            Singer Chorister = Singers.computeIfAbsent(Creature.getId(), Key -> new Singer(Creature.getId(), 0));
            Chorister.Target = 1.0f;
            Chorister.Factor = factor(Creature);
            Chorister.Teen = Creature.stage() == Scp327Siren.StageTeen;
        }
        Iterator<Map.Entry<Integer, Singer>> Entries = Singers.entrySet().iterator();
        while (Entries.hasNext()) {
            Singer Chorister = Entries.next().getValue();
            Entity Found = World.getEntity(Chorister.Id);
            if (!(Found instanceof Scp327Siren Creature) || !wants(Creature) || Creature.distanceTo(User) > HearRange + 8.0 || !Creature.heardBy(User)) {
                Chorister.Target = 0.0f;
            }
            if (Chorister.Volume < Chorister.Target) {
                Chorister.Volume = Math.min(Chorister.Target, Chorister.Volume + 1.0f / FadeIn);
            } else if (Chorister.Volume > Chorister.Target) {
                Chorister.Volume = Math.max(Chorister.Target, Chorister.Volume - 1.0f / FadeOut);
            }
            if (Chorister.Volume <= 0.0f && Chorister.Target <= 0.0f) {
                Chorister.stop(Client);
                Entries.remove();
                continue;
            }
            if (Found instanceof Scp327Siren Creature) {
                Chorister.play(Client, Creature);
            }
        }
    }

    public static boolean singing(int EntityId) {
        Singer Chorister = Singers.get(EntityId);
        return Chorister != null && Chorister.Volume > 0.0f;
    }

    public static float loudness(int EntityId) {
        Singer Chorister = Singers.get(EntityId);
        return Chorister == null ? 0.0f : Chorister.Volume;
    }

    public static void reset(Minecraft Client) {
        for (Singer Chorister : Singers.values()) {
            Chorister.stop(Client);
        }
        Singers.clear();
    }

    private static final class Singer {
        private final int Id;
        private int Position;
        private float Volume;
        private float Target;
        private float Factor = 1.0f;
        private boolean Teen;
        private Scp327BedSound Bed;

        private Singer(int Id, int Start) {
            this.Id = Id;
            this.Position = Start;
        }

        private void play(Minecraft Client, Scp327Siren Creature) {
            List<List<Scp327Song.Note>> Table = Teen ? teen() : adult();
            Vec3 Mouth = Creature.songPoint();
            float Gain = Volume * (Teen ? TeenLevel : Level);
            for (Scp327Song.Note Played : Table.get(Position)) {
                if (Played.Kind() == Scp327Song.Voice.Bed) {
                    if (Bed != null) {
                        Bed.finish();
                    }
                    Bed = new Scp327BedSound(Creature, Voices.get(Played.Kind()).get(), Played.Volume() * (Teen ? TeenLevel : Level), Played.Pitch(), this::fade);
                    Client.getSoundManager().play(Bed);
                    continue;
                }
                float Pitch = Math.max(Scp327Song.MinPitch, Math.min(Scp327Song.MaxPitch, Played.Pitch() * Factor));
                Client.getSoundManager().play(new SimpleSoundInstance(Voices.get(Played.Kind()).get().getLocation(), Channel, Played.Volume() * Gain, Pitch, Dice, false, 0,
                        SoundInstance.Attenuation.LINEAR, Mouth.x, Mouth.y, Mouth.z, false));
            }
            Position = (Position + 1) % Scp327Song.Length;
        }

        private float fade() {
            return Volume;
        }

        private void stop(Minecraft Client) {
            if (Bed != null) {
                Bed.finish();
                Bed = null;
            }
        }
    }
}
