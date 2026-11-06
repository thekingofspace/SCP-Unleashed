package net.rai.scp_unleashed.scp513.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp513.Scp513Affliction;
import net.rai.scp_unleashed.scp513.Scp513Registry;
import net.rai.scp_unleashed.scp513.Scp513Ring;
import net.rai.scp_unleashed.scp513.Scp513Stage;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp513DClassGameTests {
    public static final String Arena = "scp513_arena";
    public static final int MobTimeline = Scp513Stage.DeathAt - Scp513Affliction.MobDreadStart;

    private Scp513DClassGameTests() {
    }

    private static Mob prisoner(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        Mob Body = DClassRegistry.DClassType.get().create(World);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        Body.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Body.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.SPAWN_EGG, null);
        World.addFreshEntity(Body);
        return Body;
    }

    private static JsonObject sounds(GameTestHelper Helper) {
        try (InputStream Stream = Scp513DClassGameTests.class.getResourceAsStream("/assets/scp_unleashed/sounds.json")) {
            Helper.assertTrue(Stream != null, "sounds.json is not on the classpath");
            return JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException Failure) {
            throw new IllegalStateException(Failure);
        }
    }

    private static void noDrowning(GameTestHelper Helper, JsonObject Table, String Prefix) {
        int Checked = 0;
        for (Map.Entry<String, JsonElement> Event : Table.entrySet()) {
            if (!Event.getKey().startsWith(Prefix)) {
                continue;
            }
            Checked++;
            for (JsonElement Sound : Event.getValue().getAsJsonObject().getAsJsonArray("sounds")) {
                String Name = Sound.isJsonPrimitive() ? Sound.getAsString() : Sound.getAsJsonObject().get("name").getAsString();
                Helper.assertFalse(Name.contains("drown"), Event.getKey() + " still plays the drowning recording " + Name);
            }
        }
        Helper.assertTrue(Checked > 0, "no " + Prefix + " sound events were found");
    }

    @GameTest(template = Arena, timeoutTicks = MobTimeline + 160, batch = "scp513_dclassDiesFromRing")
    public static void dclassDiesFromRing(GameTestHelper Helper) {
        Mob Subject = prisoner(Helper, 6.5, 6.5);
        int RingAt = 10;
        Helper.runAtTickTime(RingAt, () -> {
            Scp513Ring.ring(Helper.getLevel(), Helper.absoluteVec(new Vec3(9.5, 2.0, 6.5)), null);
            Helper.assertTrue(Scp513Affliction.isAfflicted(Subject), "the ring did not reach the D-Class");
            Helper.assertTrue(Scp513Affliction.state(Subject).Elapsed() == Scp513Affliction.MobDreadStart, "the D-Class did not start on the short dread");
        });
        Helper.runAtTickTime(RingAt + Scp513Affliction.PanicTicks + 200, () -> {
            Helper.assertTrue(Subject.isAlive(), "the D-Class died during the dread");
            Helper.assertTrue(Subject.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(Scp513Affliction.DreadSlow), "the frightened D-Class was not slowed");
            Helper.assertFalse(Scp513Affliction.isLocked(Subject), "the D-Class froze before the dread ended");
        });
        Helper.runAtTickTime(RingAt + MobTimeline - 40, () -> Helper.assertTrue(Subject.isAlive() && Scp513Affliction.isAfflicted(Subject), "the D-Class died before the timeline ended"));
        Helper.runAtTickTime(RingAt + MobTimeline + 20, () -> {
            Helper.assertTrue(Subject.isDeadOrDying() || Subject.isRemoved(), "the D-Class survived SCP-513 with its AI on");
            Helper.assertFalse(Subject.hasData(Scp513Registry.Affliction.get()), "the dead D-Class still carries the affliction");
            Helper.assertFalse(Subject.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(Scp513Affliction.DreadSlow), "the dread slowdown outlived the affliction");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp513_playersKeepLongDread")
    public static void playersKeepLongDread(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp513_long"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(4.5, 2.0, 6.5));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        try {
            Helper.assertTrue(Scp513Affliction.expose(User, Spot), "the player was not afflicted");
            Helper.assertTrue(Scp513Affliction.state(User).Elapsed() == 0, "the player's dread was shortened");
            Helper.assertTrue(Scp513Affliction.dreadStart(User) == 0 && Scp513Affliction.MobDreadStart > 0, "the dread starts are wrong");
            Helper.assertTrue(MobTimeline == 1200, "the mob timeline is not one minute: " + MobTimeline);
            Scp513Affliction.cure(User);
        } finally {
            World.removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp513_noDrownSounds")
    public static void noDrownSounds(GameTestHelper Helper) {
        JsonObject Table = sounds(Helper);
        noDrowning(Helper, Table, "scp513_");
        Helper.assertTrue(Table.has("scp513_gasp_catch") && Table.has("scp513_choke_rasp"), "the new gasp and choke layers have no sound events");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "dclass_noDrownSounds")
    public static void dclassNoDrownSounds(GameTestHelper Helper) {
        noDrowning(Helper, sounds(Helper), "dclass_");
        Helper.succeed();
    }
}
