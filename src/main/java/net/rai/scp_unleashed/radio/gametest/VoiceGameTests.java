package net.rai.scp_unleashed.radio.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.integration.voicechat.VoiceGate;
import net.rai.scp_unleashed.radio.RadioVoice;
import net.rai.scp_unleashed.radio.RadioVoiceFilter;
import net.rai.scp_unleashed.scp939.Scp939VoiceLure;
import net.rai.scp_unleashed.voice.VoiceArchive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class VoiceGameTests {
    public static final String Arena = "radio_arena";
    public static final String AllowedPackage = "net/rai/scp_unleashed/integration/voicechat/";
    public static final String ApiProbe = "de.maxhenkel.voicechat.api.VoicechatApi";

    private VoiceGameTests() {
    }

    private static boolean contains(byte[] Haystack, byte[] Needle) {
        Outer:
        for (int Start = 0; Start <= Haystack.length - Needle.length; Start++) {
            for (int Index = 0; Index < Needle.length; Index++) {
                if (Haystack[Start + Index] != Needle[Index]) {
                    continue Outer;
                }
            }
            return true;
        }
        return false;
    }

    public static List<String> apiUsers(List<String> Scanned) throws IOException {
        Path Root = ModList.get().getModFileById(Scp_unleashed.ModId).getFile().findResource("net", "rai", "scp_unleashed");
        byte[] Needle = String.join("/", "de", "maxhenkel").getBytes(StandardCharsets.UTF_8);
        List<String> Users = new ArrayList<>();
        try (Stream<Path> Walk = Files.walk(Root)) {
            for (Path Each : (Iterable<Path>) Walk.filter(File -> File.toString().endsWith(".class"))::iterator) {
                String Name = "net/rai/scp_unleashed/" + Root.relativize(Each).toString().replace('\\', '/');
                Scanned.add(Name);
                if (contains(Files.readAllBytes(Each), Needle)) {
                    Users.add(Name);
                }
            }
        }
        return Users;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "voice_classGate")
    public static void classGate(GameTestHelper Helper) {
        boolean Present = VoiceGate.isLoaded();
        if (!Present) {
            boolean Found;
            try {
                Class.forName(ApiProbe, false, VoiceGameTests.class.getClassLoader());
                Found = true;
            } catch (ClassNotFoundException Expected) {
                Found = false;
            }
            Helper.assertFalse(Found, "The voice chat API is on the runtime classpath even though Simple Voice Chat is not loaded");
            Helper.assertTrue(VoiceArchive.playback() == null, "A voice playback backend exists without Simple Voice Chat");
            Helper.assertFalse(Scp939VoiceLure.enabled(), "Voice lures report enabled without Simple Voice Chat");
        }
        List<String> Scanned = new ArrayList<>();
        List<String> Users;
        try {
            Users = apiUsers(Scanned);
        } catch (IOException Problem) {
            throw new IllegalStateException(Problem);
        }
        Helper.assertTrue(Scanned.size() > 500, "Only " + Scanned.size() + " mod classes were scanned");
        List<String> Outside = new ArrayList<>();
        for (String Name : Users) {
            if (!Name.startsWith(AllowedPackage)) {
                Outside.add(Name);
            }
        }
        Helper.assertTrue(Outside.isEmpty(), "Classes outside the voice chat package touch the voice chat API: " + Outside);
        Helper.assertTrue(Users.contains(AllowedPackage + "ScpVoicechatPlugin.class") && Users.contains(AllowedPackage + "SvcRadio.class")
                && Users.contains(AllowedPackage + "SvcLurePlayback.class") && Users.contains(AllowedPackage + "SvcCapture.class"), "The voice chat bridge classes were not found: " + Users);
        Helper.assertFalse(Users.contains(AllowedPackage + "VoiceGate.class"), "VoiceGate must stay free of voice chat types");
        Helper.assertTrue(RadioVoice.route(UUID.randomUUID()) == null, "An unknown speaker has a radio route");
        Scp939VoiceLure.tick(Helper.getLevel().getServer());
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "voice_dsp")
    public static void dsp(GameTestHelper Helper) {
        RadioVoiceFilter Filter = new RadioVoiceFilter();
        short[] Flat = new short[960];
        for (int Round = 0; Round < 20; Round++) {
            java.util.Arrays.fill(Flat, (short) 10000);
            Filter.process(Flat);
        }
        int Drift = 0;
        for (short Sample : Flat) {
            Drift = Math.max(Drift, Math.abs(Sample));
        }
        Helper.assertTrue(Drift < 300, "The radio filter lets a steady offset through: " + Drift);
        Filter.reset();
        short[] Loud = new short[960];
        int Max = 0;
        for (int Round = 0; Round < 5; Round++) {
            for (int Sample = 0; Sample < Loud.length; Sample++) {
                Loud[Sample] = (short) Math.round(32000.0 * Math.sin(2.0 * Math.PI * 1000.0 * (Round * Loud.length + Sample) / RadioVoiceFilter.SampleRate));
            }
            Filter.process(Loud);
            for (short Sample : Loud) {
                Max = Math.max(Max, Math.abs(Sample));
            }
        }
        Helper.assertTrue(Max > 4000 && Max <= RadioVoiceFilter.Ceiling, "A loud voice through the radio filter peaked at " + Max);
        RadioVoice.Target Clear = new RadioVoice.Target(UUID.randomUUID(), 0.9f, false, false);
        RadioVoice.Target Weak = new RadioVoice.Target(UUID.randomUUID(), 0.02f, false, false);
        RadioVoice.Target Corrupt = new RadioVoice.Target(UUID.randomUUID(), 0.9f, false, true);
        Helper.assertFalse(RadioVoice.drops(Clear, 0.01f), "A strong clear signal dropped a frame");
        Helper.assertTrue(RadioVoice.drops(Weak, 0.2f), "A signal at the edge of range never drops frames");
        Helper.assertTrue(RadioVoice.drops(Corrupt, 0.99f), "A corrupted channel let voice through");
        Helper.succeed();
    }
}
