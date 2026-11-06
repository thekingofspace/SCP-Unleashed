package net.rai.scp_unleashed.anomaly.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.scp008.Scp008Infection;
import net.rai.scp_unleashed.scp009.Scp009Doom;
import net.rai.scp_unleashed.scp009.Scp009Exposure;
import net.rai.scp_unleashed.scp009.Scp009Registry;
import net.rai.scp_unleashed.scp016.Scp016Affliction;
import net.rai.scp_unleashed.scp035.Scp035Consume;
import net.rai.scp_unleashed.scp049.Scp049Persons;
import net.rai.scp_unleashed.scp049.Scp049Registry;
import net.rai.scp_unleashed.scp073.Scp073Registry;
import net.rai.scp_unleashed.scp076.Scp076Registry;
import net.rai.scp_unleashed.scp096.Scp096Registry;
import net.rai.scp_unleashed.scp098.Scp098Host;
import net.rai.scp_unleashed.scp106.Scp106OldMan;
import net.rai.scp_unleashed.scp106.Scp106Registry;
import net.rai.scp_unleashed.scp129.Scp129Infection;
import net.rai.scp_unleashed.scp129.Scp129Strain;
import net.rai.scp_unleashed.scp149.Scp149Fly;
import net.rai.scp_unleashed.scp149.Scp149Host;
import net.rai.scp_unleashed.scp173.Scp173Registry;
import net.rai.scp_unleashed.scp217.Scp217Clockwork;
import net.rai.scp_unleashed.scp253.Scp253Infection;
import net.rai.scp_unleashed.scp253.Scp253Registry;
import net.rai.scp_unleashed.scp253.Scp253State;
import net.rai.scp_unleashed.scp323.Scp323Hosts;
import net.rai.scp_unleashed.scp343.Scp343Registry;
import net.rai.scp_unleashed.scp407.Scp407Hearing;
import net.rai.scp_unleashed.scp409.Scp409Exposure;
import net.rai.scp_unleashed.scp409.Scp409Infection;
import net.rai.scp_unleashed.scp409.Scp409Registry;
import net.rai.scp_unleashed.scp427.Scp427Exposure;
import net.rai.scp_unleashed.scp427.Scp427Registry;
import net.rai.scp_unleashed.scp439.Scp439Colonies;
import net.rai.scp_unleashed.scp439.Scp439Infection;
import net.rai.scp_unleashed.scp439.Scp439Registry;
import net.rai.scp_unleashed.scp444.Scp444Spread;
import net.rai.scp_unleashed.scp457.Scp457Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class InfectionImmunityGameTests {
    public static final String Arena = "infection_immunity_arena";
    public static final String Batch = "infection_immunity";
    public static final double Floor = 2.0;

    private InfectionImmunityGameTests() {
    }

    private record Exposure(String Name, Predicate<LivingEntity> Reaches) {
    }

    private static DClass prisoner(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        DClass Body = DClassRegistry.DClassType.get().create(World);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        Body.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Body.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.COMMAND, null);
        Body.setNoAi(true);
        for (EquipmentSlot Slot : EquipmentSlot.values()) {
            Body.setItemSlot(Slot, ItemStack.EMPTY);
        }
        World.addFreshEntity(Body);
        return Body;
    }

    private static DClass shielded(GameTestHelper Helper, double X, double Z) {
        DClass Body = prisoner(Helper, X, Z);
        Body.setInvulnerable(true);
        return Body;
    }

    private static List<Mob> anomalies(GameTestHelper Helper) {
        return List.of(Helper.spawnWithNoFreeWill(Scp173Registry.Statue.get(), new Vec3(4.5, Floor, 12.5)),
                Helper.spawnWithNoFreeWill(Scp096Registry.ShyGuy.get(), new Vec3(8.5, Floor, 12.5)),
                Helper.spawnWithNoFreeWill(Scp106Registry.OldMan.get(), new Vec3(12.5, Floor, 12.5)));
    }

    private static String name(Entity Found) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(Found.getType()).toString();
    }

    private static void tidy(List<? extends Entity> Spawned) {
        for (Entity Each : Spawned) {
            if (Each.isRemoved()) {
                continue;
            }
            if (Each instanceof Scp106OldMan Man) {
                Man.erase();
            } else {
                Each.discard();
            }
        }
    }

    private static void expose(GameTestHelper Helper, Consumer<LivingEntity> Undo, Exposure... Exposures) {
        DClass Prisoner = prisoner(Helper, 4.5, 4.5);
        DClass Shielded = shielded(Helper, 12.5, 4.5);
        List<Mob> Spared = anomalies(Helper);
        for (Exposure Each : Exposures) {
            for (Mob Anomaly : Spared) {
                Helper.assertFalse(Each.Reaches().test(Anomaly), Each.Name() + " reached " + name(Anomaly));
            }
            Helper.assertFalse(Each.Reaches().test(Shielded), Each.Name() + " reached an invulnerable D-Class");
            Helper.assertTrue(Each.Reaches().test(Prisoner), Each.Name() + " no longer reaches an ordinary D-Class");
        }
        Undo.accept(Prisoner);
        tidy(List.of(Prisoner, Shielded));
        tidy(Spared);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = Batch)
    public static void immortalsCatchNothing(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        List<LivingEntity> Immortals = new ArrayList<>();
        Immortals.add(Helper.spawn(Scp343Registry.God.get(), new Vec3(4.5, Floor, 4.5)));
        Immortals.add(Helper.spawn(Scp073Registry.Cain.get(), new Vec3(10.5, Floor, 4.5)));
        Immortals.add(Helper.spawn(Scp076Registry.Abel.get(), new Vec3(16.5, Floor, 4.5)));
        for (LivingEntity Body : Immortals) {
            Helper.assertTrue(InfectionImmunity.isImmune(Body), name(Body) + " is not immune to infection");
            Helper.assertFalse(Scp009Doom.doom(Body), name(Body) + " accepted an SCP-009 doom");
            for (LabOrganism Organism : LabOrganisms.all()) {
                Organism.expose(World, Body.getEyePosition(), Pos -> false, 4.0);
            }
        }
        Helper.runAfterDelay(20, () -> {
            for (LivingEntity Body : Immortals) {
                Helper.assertFalse(Scp009Doom.isDoomed(Body), name(Body) + " was doomed by SCP-009");
                for (LabOrganism Organism : LabOrganisms.all()) {
                    Helper.assertFalse(Organism.isPresentOn(Body), name(Body) + " caught " + Organism.getId());
                }
            }
            tidy(Immortals);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void contractCoversAnomaliesNotPeople(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        List<EntityType<?>> Immune = List.of(Scp173Registry.Statue.get(), Scp096Registry.ShyGuy.get(), Scp106Registry.OldMan.get(), Scp049Registry.Doctor.get(),
                Scp343Registry.God.get(), Scp427Registry.Flesh.get(), Scp457Registry.Burning.get(), Scp073Registry.Cain.get(), Scp076Registry.Abel.get());
        for (EntityType<?> Type : Immune) {
            Entity Made = Type.create(World);
            Helper.assertTrue(Made != null && InfectionImmunity.isImmune(Made), BuiltInRegistries.ENTITY_TYPE.getKey(Type) + " is not immune to infection");
            Helper.assertTrue(Type.is(InfectionImmunity.Immune), BuiltInRegistries.ENTITY_TYPE.getKey(Type) + " is missing from the infection immune tag");
            Helper.assertTrue(Type.is(Scp409Registry.Immune), BuiltInRegistries.ENTITY_TYPE.getKey(Type) + " is missing from the SCP-409 immune tag");
        }
        List<EntityType<?>> Mortal = List.of(DClassRegistry.DClassType.get(), EntityType.VILLAGER, EntityType.ZOMBIE, EntityType.COW);
        for (EntityType<?> Type : Mortal) {
            Entity Made = Type.create(World);
            Helper.assertTrue(Made != null && !InfectionImmunity.isImmune(Made), BuiltInRegistries.ENTITY_TYPE.getKey(Type) + " became immune to infection");
        }
        Helper.assertFalse(InfectionImmunity.isImmune(Helper.makeMockPlayer(GameType.SURVIVAL)), "a survival player became immune to infection");
        Helper.assertTrue(InfectionImmunity.isImmune(Helper.makeMockPlayer(GameType.CREATIVE)), "a creative player can still be infected");
        Helper.assertTrue(InfectionImmunity.isImmune(Helper.makeMockPlayer(GameType.SPECTATOR)), "a spectator can still be infected");
        Helper.assertTrue(InfectionImmunity.isImmune(EntityType.ARMOR_STAND.create(World)), "an armour stand can be infected");
        Helper.assertTrue(InfectionImmunity.isImmune(EntityType.ARROW.create(World)), "something that is not alive can be infected");
        Helper.assertTrue(InfectionImmunity.isImmune(null), "nothing at all can be infected");
        DClass Shielded = DClassRegistry.DClassType.get().create(World);
        Shielded.setInvulnerable(true);
        Helper.assertTrue(InfectionImmunity.isImmune(Shielded), "an invulnerable D-Class can be infected");
        Helper.assertTrue(InfectionImmunity.isImmune(EntityType.ENDER_DRAGON.create(World), Scp409Registry.Immune), "a type in its own immune tag was not immune");
        Helper.assertFalse(InfectionImmunity.isImmune(EntityType.ENDER_DRAGON.create(World)), "a vanilla boss became immune to every infection");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "infection_immunity_scp009")
    public static void redIceSparesImmuneAnomalies(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        DClass Prisoner = prisoner(Helper, 4.5, 4.5);
        DClass Shielded = shielded(Helper, 12.5, 4.5);
        List<Mob> Spared = anomalies(Helper);
        Scp009Exposure.expose(World, Helper.absoluteVec(new Vec3(8.5, 3.0, 8.5)), Pos -> false);
        Helper.assertTrue(Scp009Doom.isDoomed(Prisoner), "a D-Class in sight of red ice was not doomed");
        Helper.assertFalse(Scp009Doom.isDoomed(Shielded), "an invulnerable D-Class was doomed by red ice");
        for (Mob Anomaly : Spared) {
            Helper.assertFalse(Scp009Doom.isDoomed(Anomaly), name(Anomaly) + " was doomed by red ice in plain sight");
            Helper.assertFalse(Scp009Doom.doom(Anomaly), name(Anomaly) + " accepted a direct SCP-009 doom");
            Helper.assertFalse(Scp009Exposure.isVulnerable(Anomaly), name(Anomaly) + " is still picked as a red ice target");
        }
        Mob OldMan = Spared.get(2);
        Helper.assertTrue(OldMan.addEffect(new MobEffectInstance(Scp009Registry.Doomed, Scp009Doom.TotalTicks, 0, false, false, true)), "the doom could not be forced onto SCP-106");
        Helper.runAfterDelay(3, () -> {
            Helper.assertFalse(Scp009Doom.isDoomed(OldMan), "a forced SCP-009 doom stayed on SCP-106");
            Scp009Doom.clear(Prisoner);
            tidy(List.of(Prisoner, Shielded));
            tidy(Spared);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "infection_immunity_scp409")
    public static void crystalSparesImmuneAnomalies(GameTestHelper Helper) {
        DClass Toucher = prisoner(Helper, 4.5, 4.5);
        DClass Neighbour = prisoner(Helper, 12.5, 4.5);
        DClass Shielded = shielded(Helper, 8.5, 4.5);
        List<Mob> Spared = anomalies(Helper);
        Helper.assertTrue(Scp409Exposure.touchCrystal(Toucher), "touching SCP-409 did not infect a D-Class");
        Helper.assertFalse(Scp409Exposure.touchCrystal(Shielded), "touching SCP-409 infected an invulnerable D-Class");
        for (Mob Anomaly : Spared) {
            Helper.assertFalse(Scp409Exposure.touchCrystal(Anomaly), "touching SCP-409 infected " + name(Anomaly));
            Helper.assertFalse(Scp409Exposure.pass(Toucher, Anomaly), "an infected D-Class passed SCP-409 to " + name(Anomaly));
            Helper.assertFalse(Scp409Infection.isInfected(Anomaly), name(Anomaly) + " carries SCP-409");
        }
        Helper.assertTrue(Scp409Exposure.pass(Toucher, Neighbour), "an infected D-Class no longer passes SCP-409 to another D-Class");
        Scp409Infection.cure(Toucher);
        Scp409Infection.cure(Neighbour);
        tidy(List.of(Toucher, Neighbour, Shielded));
        tidy(Spared);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "infection_immunity_leftovers")
    public static void leftoverInfectionsFallAway(GameTestHelper Helper) {
        List<Mob> Spared = anomalies(Helper);
        Mob ShyGuy = Spared.get(1);
        Mob OldMan = Spared.get(2);
        ShyGuy.setData(Scp409Registry.Crystal.get(), 0);
        OldMan.setData(Scp253Registry.Infection.get(), new Scp253State(0, 1L));
        Helper.assertTrue(Scp409Infection.isInfected(ShyGuy) && Scp253Infection.isInfected(OldMan), "the leftover infections could not be planted");
        Helper.runAfterDelay(3, () -> {
            Helper.assertFalse(Scp409Infection.isInfected(ShyGuy), "a leftover SCP-409 infection stayed on SCP-096");
            Helper.assertFalse(Scp253Infection.isInfected(OldMan), "a leftover SCP-253 infection stayed on SCP-106");
            tidy(Spared);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void plagueSparesImmuneAnomalies(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        DClass Prisoner = prisoner(Helper, 4.5, 4.5);
        DClass Shielded = shielded(Helper, 12.5, 4.5);
        List<Mob> Spared = anomalies(Helper);
        Scp008Infection.exposeAround(World, Helper.absoluteVec(new Vec3(8.5, 3.0, 8.5)), Pos -> false, 10.0);
        Helper.assertTrue(Scp008Infection.isInfected(Prisoner), "a D-Class beside an SCP-008 leak was not infected");
        Helper.assertFalse(Scp008Infection.isInfected(Shielded), "an invulnerable D-Class caught SCP-008");
        for (Mob Anomaly : Spared) {
            Helper.assertFalse(Scp008Infection.infect(Anomaly), "an SCP-008-1 bite infected " + name(Anomaly));
            Helper.assertFalse(Scp008Infection.isInfected(Anomaly), name(Anomaly) + " carries SCP-008");
        }
        Scp008Infection.cure(Prisoner);
        tidy(List.of(Prisoner, Shielded));
        tidy(Spared);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void bloodBlowSparesImmuneAnomalies(GameTestHelper Helper) {
        expose(Helper, Scp253Infection::cure, new Exposure("SCP-253", Scp253Infection::infect));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void bloodborneSparesImmuneAnomalies(GameTestHelper Helper) {
        expose(Helper, Scp016Affliction::cure, new Exposure("SCP-016", Scp016Affliction::afflict));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void strainsSpareImmuneAnomalies(GameTestHelper Helper) {
        expose(Helper, Scp129Infection::cure, new Exposure("SCP-129", Target -> Scp129Infection.infect(Target, Scp129Strain.One)));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void clockworkSparesImmuneAnomalies(GameTestHelper Helper) {
        expose(Helper, Scp217Clockwork::cure, new Exposure("SCP-217", Scp217Clockwork::infect));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void sporesSpareImmuneAnomalies(GameTestHelper Helper) {
        BlockPos Hive = Helper.absolutePos(new BlockPos(8, 2, 8));
        expose(Helper, Scp098Host::cure, new Exposure("SCP-098", Target -> Scp098Host.infect(Target, Helper.getLevel(), Hive)));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void fliesSpareImmuneAnomalies(GameTestHelper Helper) {
        expose(Helper, Scp149Host::cure, new Exposure("SCP-149 targeting", Scp149Fly::isPerson), new Exposure("SCP-149", Scp149Host::infect));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void hiveSparesImmuneAnomalies(GameTestHelper Helper) {
        expose(Helper, Target -> {
            Target.removeData(Scp439Registry.Infection.get());
            Scp439Infection.clear(Target);
            Scp439Colonies.get(Helper.getLevel()).removeHost(Target.getUUID());
        }, new Exposure("SCP-439", Target -> Scp439Infection.infect(Target, null)));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void songSparesImmuneAnomalies(GameTestHelper Helper) {
        expose(Helper, Target -> {
        }, new Exposure("SCP-407", Scp407Hearing::counts));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void doctorSparesImmuneAnomalies(GameTestHelper Helper) {
        expose(Helper, Target -> {
        }, new Exposure("SCP-049", Target -> Scp049Persons.isPerson(Target, false)));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void skullSparesImmuneAnomalies(GameTestHelper Helper) {
        expose(Helper, Target -> {
        }, new Exposure("SCP-323", Scp323Hosts::canHost));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void maskSparesImmuneAnomalies(GameTestHelper Helper) {
        expose(Helper, Target -> {
        }, new Exposure("SCP-035", Scp035Consume::canConsume));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void locketSparesImmuneAnomalies(GameTestHelper Helper) {
        expose(Helper, Target -> {
        }, new Exposure("SCP-427", Target -> !Scp427Exposure.isExempt(Target)));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = Batch)
    public static void silenceSparesImmuneAnomalies(GameTestHelper Helper) {
        expose(Helper, Target -> {
        }, new Exposure("SCP-444", Target -> !Scp444Spread.isImmune(Target)));
    }
}
