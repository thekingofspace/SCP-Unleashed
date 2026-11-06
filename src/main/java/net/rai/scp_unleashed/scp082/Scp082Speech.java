package net.rai.scp_unleashed.scp082;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.senses.Senses;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Scp082Speech {
    public static final int JovialLines = 52;
    public static final int IrritableLines = 28;
    public static final int RavenousLines = 22;
    public static final int StarvingLines = 16;
    public static final int GrabLines = 12;
    public static final int MealLines = 12;
    public static final int SnackLines = 10;
    public static final int RageLines = 10;
    public static final int EscapeLines = 8;
    public static final double SpeechRange = 16.0;
    public static final int RambleMin = 160;
    public static final int RambleMax = 380;
    public static final int Remembered = 6;
    public static final int TalkTicks = 46;
    public static final int VoiceGap = 11;
    public static final String Prefix = "message.scp_unleashed.scp082.";

    public enum Pool {
        Jovial("jovial", JovialLines),
        Irritable("irritable", IrritableLines),
        Ravenous("ravenous", RavenousLines),
        Starving("starving", StarvingLines),
        Grab("grab", GrabLines),
        Meal("meal", MealLines),
        Snack("snack", SnackLines),
        Rage("rage", RageLines),
        Escape("escape", EscapeLines);

        public final String Key;
        public final int Count;

        Pool(String Key, int Count) {
            this.Key = Key;
            this.Count = Count;
        }

        public static Pool of(Scp082Mood Mood) {
            return switch (Mood) {
                case Jovial -> Jovial;
                case Irritable -> Irritable;
                case Ravenous -> Ravenous;
                case Starving -> Starving;
            };
        }
    }

    private final Deque<String> Recent = new ArrayDeque<>();

    public String pick(Pool Source, RandomSource Random) {
        String Chosen = Source.Key + "." + Random.nextInt(Source.Count);
        for (int Attempt = 0; Attempt < 8 && Recent.contains(Chosen); Attempt++) {
            Chosen = Source.Key + "." + Random.nextInt(Source.Count);
        }
        Recent.addLast(Chosen);
        while (Recent.size() > Remembered) {
            Recent.removeFirst();
        }
        return Chosen;
    }

    public static int say(ServerLevel World, Scp082Fernand Speaker, String Line, Scp082Mood Mood) {
        Component Spoken = Component.translatable(Prefix + "ramble." + Line);
        Component Shown = Component.translatable(Prefix + "speech", Speaker.getDisplayName(), Spoken).withStyle(Mood.Colour, ChatFormatting.ITALIC);
        int Heard = 0;
        for (ServerPlayer Listener : World.players()) {
            if (Listener.distanceToSqr(Speaker) <= SpeechRange * SpeechRange && !Listener.isSpectator()) {
                Listener.displayClientMessage(Senses.heard(Listener, Shown), true);
                Heard++;
            }
        }
        return Heard;
    }

    public static boolean anyoneNear(ServerLevel World, Scp082Fernand Speaker) {
        for (ServerPlayer Listener : World.players()) {
            if (Listener.distanceToSqr(Speaker) <= SpeechRange * SpeechRange && !Listener.isSpectator()) {
                return true;
            }
        }
        return false;
    }
}
