package net.rai.scp_unleashed.scp5001.event.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.event.Scp5001Arena;
import net.rai.scp_unleashed.scp5001.event.Scp5001Event;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventCommands;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventConfig;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventData;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;
import net.rai.scp_unleashed.scp5001.event.Scp5001Exposure;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001LaserEmitterBlock;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001RodEmitterBlock;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Mimic;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Vessel;
import net.rai.scp_unleashed.scp610.Scp610Creature;

import java.lang.reflect.Method;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp5001EventGameTests {
    public static final String Arena = "scp5001event_arena";
    public static final BlockPos Middle = new BlockPos(16, 1, 16);
    public static final int Radius = 9;

    private Scp5001EventGameTests() {
    }

    private static Scp5001Arena.Layout setup(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp5001EventConfig.clearTestOverrides();
        Scp5001EventConfig.overrideForTests("alarmSeconds", 0);
        Scp5001EventConfig.overrideForTests("rodLatchSeconds", 0);
        Scp5001EventConfig.overrideForTests("waveSeconds", 600);
        Scp5001EventConfig.overrideForTests("siteRadius", 40);
        Scp5001EventConfig.overrideForTests("siteHeight", 40);
        Scp5001EventConfig.overrideForTests("burstRadius", 0);
        purge(World);
        Scp5001EventCommands.clearAll(World);
        return Scp5001Arena.build(World, Helper.absolutePos(Middle), Radius);
    }

    private static void purge(ServerLevel World) {
        Scp5001EventData Data = Scp5001EventData.get(World.getServer());
        for (BlockPos Each : Data.allDevices()) {
            if (World.isLoaded(Each)) {
                World.setBlock(Each, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
        }
        Data.clearDevices();
    }

    private static void finish(GameTestHelper Helper, Scp5001Arena.Layout Made) {
        ServerLevel World = Helper.getLevel();
        Scp5001EventCommands.clearAll(World);
        Scp5001Arena.clear(World, Made);
        purge(World);
        Scp5001EventConfig.clearTestOverrides();
        AABB Area = new AABB(Made.Core()).inflate(30.0);
        for (Entity Each : World.getEntitiesOfClass(Entity.class, Area, Each -> Each instanceof Scp5001Vessel || Each instanceof Scp5001Mimic || Each instanceof Scp610Creature
                || Each instanceof Villager)) {
            Each.discard();
        }
        Scp5001Event.forgetCache();
        Helper.succeed();
    }

    private static Scp5001State state(GameTestHelper Helper) {
        return Scp5001State.get(Helper.getLevel().getServer());
    }

    private static Scp5001EventData data(GameTestHelper Helper) {
        return Scp5001EventData.get(Helper.getLevel().getServer());
    }

    private static long now(GameTestHelper Helper) {
        return Helper.getLevel().getGameTime();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001event_locks")
    public static void locksStayGone(GameTestHelper Helper) {
        Scp5001Arena.Layout Made = setup(Helper);
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Dormant, "the facility should start dormant");
        Helper.assertTrue(Scp5001Event.unlock(World, now(Helper)), "the first toggle should disengage the interlocks");
        Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Unlocked, "the phase should be Unlocked");
        Helper.assertFalse(Scp5001Event.unlock(World, now(Helper)), "a second toggle must not do anything");
        Helper.runAfterDelay(10, () -> {
            Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Rising, "the alarm should give way to Rising");
            Helper.assertFalse(Scp5001Event.unlock(World, now(Helper)), "the interlocks cannot be re-engaged while rising");
            state(Helper).phase(Scp5001State.Phase.Dormant, now(Helper));
        });
        Helper.runAfterDelay(14, () -> {
            Helper.assertTrue(data(Helper).cooling(now(Helper)), "returning to Dormant should start the cooldown");
            Helper.assertFalse(Scp5001Event.unlock(World, now(Helper)), "the interlocks stay down during the cooldown");
            data(Helper).cooldownEnd(now(Helper));
        });
        Helper.runAfterDelay(18, () -> {
            Helper.assertFalse(data(Helper).cooling(now(Helper)), "the cooldown should be over");
            Helper.assertFalse(data(Helper).rodsSpent(), "the pillars re-arm when the cooldown ends");
            Helper.assertTrue(Scp5001Event.unlock(World, now(Helper)), "a new incident is possible after the cooldown");
            finish(Helper, Made);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001event_rise")
    public static void liftRisesAndRodsSlowIt(GameTestHelper Helper) {
        Scp5001Arena.Layout Made = setup(Helper);
        Scp5001EventConfig.overrideForTests("riseMinutes", 0.5);
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(Scp5001Event.unlock(World, now(Helper)), "unlock");
        float[] Marks = new float[3];
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Rising, "rising");
            Helper.assertTrue(Scp5001Event.latched(World, state(Helper), data(Helper), now(Helper)), "the pillars should be latched");
            Marks[0] = state(Helper).lift();
        });
        Helper.runAfterDelay(25, () -> {
            Marks[1] = state(Helper).lift();
            data(Helper).rodsSpent(true);
        });
        Helper.runAfterDelay(45, () -> {
            Marks[2] = state(Helper).lift();
            float Latched = Marks[1] - Marks[0];
            float Free = Marks[2] - Marks[1];
            Helper.assertTrue(Latched > 0.0f, "the lift should rise over time");
            Helper.assertTrue(Free > Latched * 1.2f, "without the pillars the vessel should rise faster: " + Latched + " against " + Free);
            finish(Helper, Made);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp5001event_overdrive")
    public static void overdriveSpawnsSwarm(GameTestHelper Helper) {
        Scp5001Arena.Layout Made = setup(Helper);
        Scp5001EventConfig.overrideForTests("riseMinutes", 4.0);
        Scp5001EventConfig.overrideForTests("swarmWaves", 1);
        Scp5001EventConfig.overrideForTests("swarmBase", 3);
        Scp5001EventConfig.overrideForTests("swarmPerPlayer", 0);
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(Scp5001Event.unlock(World, now(Helper)), "unlock");
        Helper.runAfterDelay(4, () -> {
            float Before = state(Helper).lift();
            for (BlockPos Generator : Made.Generators()) {
                Helper.assertTrue(Scp5001Event.overdriveGenerator(World, Generator, now(Helper)), "every generator should accept overdrive");
            }
            Helper.assertFalse(Scp5001Event.overdriveGenerator(World, Made.Generators().get(0), now(Helper)), "a generator only overdrives once");
            Helper.assertTrue(state(Helper).overdriven() == 4, "four generators overdriven");
            Helper.assertTrue(state(Helper).lift() <= Before, "overdrive should push the vessel back");
        });
        Helper.runAfterDelay(8, () -> Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Overdrive, "all four generators should pin the vessel"));
        Helper.runAfterDelay(30, () -> {
            boolean Firing = Helper.getLevel().getBlockState(Made.Lasers().get(0)).getValue(Scp5001LaserEmitterBlock.Firing);
            Helper.assertTrue(Firing, "the lasers should fire while the vessel is pinned");
        });
        Helper.runAfterDelay(32, () -> Helper.succeedWhen(() -> {
            Helper.assertTrue(data(Helper).swarm().size() >= 3, "the pinned vessel should release SCP-610");
            for (UUID Id : data(Helper).swarm()) {
                Helper.assertTrue(World.getEntity(Id) instanceof Scp610Creature, "tracked spawns should be SCP-610");
            }
            finish(Helper, Made);
        }));
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp5001event_purge")
    public static void killingTheSwarmPurges(GameTestHelper Helper) {
        Scp5001Arena.Layout Made = setup(Helper);
        Scp5001EventConfig.overrideForTests("riseMinutes", 4.0);
        Scp5001EventConfig.overrideForTests("swarmWaves", 1);
        Scp5001EventConfig.overrideForTests("swarmBase", 2);
        Scp5001EventConfig.overrideForTests("swarmPerPlayer", 0);
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(Scp5001Event.unlock(World, now(Helper)), "unlock");
        Helper.runAfterDelay(4, () -> {
            for (BlockPos Generator : Made.Generators()) {
                Scp5001Event.overdriveGenerator(World, Generator, now(Helper));
            }
        });
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Overdrive, "pinned");
            Helper.assertTrue(!data(Helper).swarm().isEmpty(), "the swarm should be out");
            for (UUID Id : data(Helper).swarm().toArray(UUID[]::new)) {
                Entity Found = World.getEntity(Id);
                if (Found != null) {
                    Found.kill();
                }
            }
        });
        Helper.runAfterDelay(84, () -> Helper.succeedWhen(() -> {
            Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Purging, "killing every spawned instance should enter Purging");
            finish(Helper, Made);
        }));
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp5001event_reset")
    public static void resetVaporizesRods(GameTestHelper Helper) {
        Scp5001Arena.Layout Made = setup(Helper);
        Scp5001EventConfig.overrideForTests("riseMinutes", 4.0);
        Scp5001EventConfig.overrideForTests("resetSeconds", 1);
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(Scp5001Event.unlock(World, now(Helper)), "unlock");
        Helper.runAfterDelay(4, () -> {
            Helper.assertFalse(Scp5001Event.forceReset(World, now(Helper)), "reset is refused before the purge");
            state(Helper).lift(0.5f);
            data(Helper).lastSeen(Scp5001State.Phase.Purging);
            state(Helper).phase(Scp5001State.Phase.Purging, now(Helper));
        });
        Helper.runAfterDelay(6, () -> {
            Helper.assertTrue(Scp5001Event.forceReset(World, now(Helper)), "reset is accepted while purging");
            Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Resetting, "resetting");
            Helper.assertTrue(data(Helper).rodsSpent(), "the pillars should be spent");
            for (BlockPos Rod : Made.Rods()) {
                Helper.assertTrue(World.getBlockState(Rod).getValue(Scp5001RodEmitterBlock.Spent), "every pillar emitter should be vaporized");
            }
        });
        Helper.runAfterDelay(10, () -> Helper.succeedWhen(() -> {
            Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Dormant, "the vessel should settle back to Dormant");
            Helper.assertTrue(state(Helper).lift() <= 0.0f, "the vessel should be back on the core");
            Helper.assertTrue(data(Helper).cooling(now(Helper)), "a cooldown should follow the incident");
            Helper.assertFalse(Scp5001Event.unlock(World, now(Helper)), "the interlocks stay down until the cooldown ends");
            finish(Helper, Made);
        }));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001event_failure")
    public static void fullLiftBreaches(GameTestHelper Helper) {
        Scp5001Arena.Layout Made = setup(Helper);
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(Scp5001Event.unlock(World, now(Helper)), "unlock");
        Helper.runAfterDelay(4, () -> state(Helper).lift(0.9999f));
        Helper.runAfterDelay(6, () -> Helper.succeedWhen(() -> {
            Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Xk1, "a full lift should breach into XK-1");
            Helper.assertTrue(data(Helper).rodsSpent(), "the pillars snap in the burst");
            finish(Helper, Made);
        }));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001event_timeout")
    public static void abandonedRiseBreaches(GameTestHelper Helper) {
        Scp5001Arena.Layout Made = setup(Helper);
        Scp5001EventConfig.overrideForTests("riseMinutes", 0.05);
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(Scp5001Event.unlock(World, now(Helper)), "unlock");
        Helper.runAfterDelay(4, () -> Helper.succeedWhen(() -> {
            Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Xk1, "with nobody stopping it the timer runs out into XK-1");
            finish(Helper, Made);
        }));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001event_exposure")
    public static void exposureKillsAndLeavesMimic(GameTestHelper Helper) {
        Scp5001Arena.Layout Made = setup(Helper);
        Scp5001EventConfig.overrideForTests("riseMinutes", 4.0);
        Scp5001EventConfig.overrideForTests("exposureMax", 10);
        Scp5001EventConfig.overrideForTests("exposureGainFar", 3);
        Scp5001EventConfig.overrideForTests("exposureGainNear", 3);
        ServerLevel World = Helper.getLevel();
        Villager Victim = Helper.spawn(EntityType.VILLAGER, new BlockPos(12, 1, 20));
        Victim.setNoAi(true);
        Scp5001Exposure.set(Victim, 8);
        Player Donor = Helper.makeMockPlayer(GameType.SURVIVAL);
        Donor.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        Scp5001Mimic Copy = Scp5001EventRegistry.MimicType.get().create(World);
        Helper.assertTrue(Copy != null, "a mimic can be made");
        Copy.copy(Donor);
        Helper.assertTrue(Copy.profile() != null && Copy.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "a player mimic copies the skin and the armour look");
        Helper.assertFalse(Copy.mimicsMob(), "a player mimic is not a mob mimic");
        Helper.assertTrue(Scp5001Event.unlock(World, now(Helper)), "unlock");
        Helper.runAfterDelay(4, () -> Helper.succeedWhen(() -> {
            Helper.assertTrue(!Victim.isAlive(), "full exposure should kill");
            boolean Found = false;
            for (Scp5001Mimic Each : World.getEntitiesOfClass(Scp5001Mimic.class, new AABB(Made.Core()).inflate(20.0))) {
                Found |= "minecraft:villager".equals(Each.source());
            }
            Helper.assertTrue(Found, "the victim should leave a mimic behind");
            finish(Helper, Made);
        }));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001event_persist")
    public static void survivesSaveAndLoad(GameTestHelper Helper) {
        Scp5001Arena.Layout Made = setup(Helper);
        Scp5001EventConfig.overrideForTests("riseMinutes", 4.0);
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(Scp5001Event.unlock(World, now(Helper)), "unlock");
        Helper.runAfterDelay(4, () -> {
            state(Helper).lift(0.4f);
            Scp5001Event.overdriveGenerator(World, Made.Generators().get(0), now(Helper));
            Scp5001Event.overdriveGenerator(World, Made.Generators().get(1), now(Helper));
            HolderLookup.Provider Registries = World.registryAccess();
            CompoundTag DataTag = data(Helper).save(new CompoundTag(), Registries);
            CompoundTag StateTag = state(Helper).save(new CompoundTag(), Registries);
            try {
                Method Load = Scp5001State.class.getDeclaredMethod("load", CompoundTag.class, HolderLookup.Provider.class);
                Load.setAccessible(true);
                Scp5001State Loaded = (Scp5001State) Load.invoke(null, StateTag, Registries);
                World.getServer().overworld().getDataStorage().set("scp_unleashed_scp5001", Loaded);
            } catch (ReflectiveOperationException Problem) {
                throw new IllegalStateException(Problem);
            }
            World.getServer().overworld().getDataStorage().set(Scp5001EventData.Name, Scp5001EventData.load(DataTag, Registries));
        });
        float[] Mark = new float[1];
        Helper.runAfterDelay(5, () -> {
            Mark[0] = state(Helper).lift();
            Helper.assertTrue(Mark[0] > 0.3f, "the lift survives a reload");
            Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Rising, "the phase survives a reload");
            Helper.assertTrue(state(Helper).overdriven() == 2, "the overdriven count survives a reload");
            Helper.assertTrue(data(Helper).overdriven().size() == 2, "the overdriven generators survive a reload");
            Helper.assertTrue(Scp5001Event.generators(World, state(Helper), data(Helper)).size() == 4, "the registered generators survive a reload");
        });
        Helper.runAfterDelay(25, () -> {
            Helper.assertTrue(state(Helper).phase() == Scp5001State.Phase.Rising, "the event resumes after a reload");
            Helper.assertTrue(state(Helper).lift() > Mark[0], "the lift resumes from where it was");
            finish(Helper, Made);
        });
    }
}
