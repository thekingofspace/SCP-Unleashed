package net.rai.scp_unleashed.scp407;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class Scp407Song {
    public static final String Alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";
    public static final int NoteWidth = 6;
    public static final int Length = Scp407Score.Length;
    public static final SoundSource Source = SoundSource.BLOCKS;
    private static final int[] Starts = new int[Length + 1];
    private static final byte[] VoiceOf;
    private static final float[] PitchOf;
    private static final float[] VolumeOf;

    static {
        StringBuilder Joined = new StringBuilder();
        for (String Part : Scp407Score.Data) {
            Joined.append(Part);
        }
        int Count = Joined.length() / NoteWidth;
        int[] Ticks = new int[Count];
        VoiceOf = new byte[Count];
        PitchOf = new float[Count];
        VolumeOf = new float[Count];
        int[] Counts = new int[Length];
        for (int Index = 0; Index < Count; Index++) {
            int Base = Index * NoteWidth;
            int Tick = digit(Joined.charAt(Base)) * 4096 + digit(Joined.charAt(Base + 1)) * 64 + digit(Joined.charAt(Base + 2));
            Ticks[Index] = Math.min(Length - 1, Tick);
            VoiceOf[Index] = (byte) digit(Joined.charAt(Base + 3));
            PitchOf[Index] = (float) Math.pow(2.0, (digit(Joined.charAt(Base + 4)) - Scp407Score.PitchCentre) / 12.0);
            VolumeOf[Index] = digit(Joined.charAt(Base + 5)) / 63.0f;
            Counts[Ticks[Index]]++;
        }
        for (int Tick = 0; Tick < Length; Tick++) {
            Starts[Tick + 1] = Starts[Tick] + Counts[Tick];
        }
        int[] Cursor = new int[Length];
        byte[] Voices = VoiceOf.clone();
        float[] Pitches = PitchOf.clone();
        float[] Volumes = VolumeOf.clone();
        for (int Index = 0; Index < Count; Index++) {
            int Slot = Starts[Ticks[Index]] + Cursor[Ticks[Index]]++;
            VoiceOf[Slot] = Voices[Index];
            PitchOf[Slot] = Pitches[Index];
            VolumeOf[Slot] = Volumes[Index];
        }
    }

    private Scp407Song() {
    }

    private static int digit(char Character) {
        return Alphabet.indexOf(Character);
    }

    public static int noteCount() {
        return VoiceOf.length;
    }

    public static int notesAt(int Tick) {
        int Position = Math.floorMod(Tick, Length);
        return Starts[Position + 1] - Starts[Position];
    }

    public static void play(ServerLevel World, Vec3 At, long Tick, float Volume) {
        int Position = (int) Math.floorMod(Tick, (long) Length);
        for (int Index = Starts[Position]; Index < Starts[Position + 1]; Index++) {
            float Loudness = VolumeOf[Index] * Volume;
            if (Loudness <= 0.0f) {
                continue;
            }
            World.playSeededSound(null, At.x, At.y, At.z, Scp407Registry.Voices.get(VoiceOf[Index]), Source, Loudness, PitchOf[Index], World.getRandom().nextLong());
        }
    }
}
