package net.rai.scp_unleashed.anomaly.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.scp008.Scp008Infection;
import net.rai.scp_unleashed.scp009.Scp009Doom;
import net.rai.scp_unleashed.scp009.Scp009Exposure;
import net.rai.scp_unleashed.scp020.Scp020Affliction;
import net.rai.scp_unleashed.scp020.Scp020Mold;
import net.rai.scp_unleashed.scp096.Scp096Registry;
import net.rai.scp_unleashed.scp173.Scp173Registry;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Leech;
import net.rai.scp_unleashed.scp409.Scp409Exposure;
import net.rai.scp_unleashed.scp409.Scp409Infection;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610Hosts;
import net.rai.scp_unleashed.scp610.Scp610Infection;
import net.rai.scp_unleashed.scp610.Scp610Registry;
import net.rai.scp_unleashed.scp682.Scp682;
import net.rai.scp_unleashed.scp682.Scp682Registry;
import net.rai.scp_unleashed.scp682.Scp682Signature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class SemiImmortalGameTests {
    public static final String Arena = "scp682_arena";
    public static final double Floor = 1.0;
    public static final int Watch = 200;
    public static final double Reach = 3.0;
    public static final TagKey<EntityType<?>> GateAvoiders = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gate_avoiders"));
    public static final List<String> Listed = List.of("scp682", "scp173", "scp096_shy_guy", "scp343", "scp106_old_man", "scp049", "scp076", "scp999_blob", "scarlet_king",
            "scp001", "scp001_gate_guardian", "scp1730_2", "scp1730_2_contained");

    private SemiImmortalGameTests() {
    }

    private static AABB area(GameTestHelper Helper) {
        return new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(32.0, 14.0, 32.0).inflate(4.0);
    }

    private static void sweep(GameTestHelper Helper) {
        for (Entity Each : Helper.getLevel().getEntitiesOfClass(Entity.class, area(Helper), Each -> !(Each instanceof ServerPlayer))) {
            Each.discard();
        }
    }

    private static String name(Entity Found) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(Found.getType()).toString();
    }

    private static Scp682 reptile(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        Scp682 Made = Scp682Registry.Scp682Type.get().create(World);
        Vec3 Where = Helper.absoluteVec(new Vec3(X, Floor, Z));
        Made.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        Made.setNoAi(true);
        Made.setHealth(Made.getMaxHealth());
        World.addFreshEntity(Made);
        return Made;
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

    private static <T extends Mob> T still(T Body) {
        Body.setNoAi(true);
        Body.setPersistenceRequired();
        return Body;
    }

    private static List<LivingEntity> spared(GameTestHelper Helper) {
        List<LivingEntity> Found = new ArrayList<>();
        Found.add(reptile(Helper, 16.5, 16.5));
        Found.add(Helper.spawnWithNoFreeWill(Scp173Registry.Statue.get(), new Vec3(5.5, Floor, 5.5)));
        Found.add(Helper.spawnWithNoFreeWill(Scp096Registry.ShyGuy.get(), new Vec3(27.5, Floor, 5.5)));
        return Found;
    }

    private static List<String> watch(GameTestHelper Helper, List<LivingEntity> Spared, String Agent, Predicate<LivingEntity> Caught) {
        List<String> Breaches = new ArrayList<>();
        Map<UUID, Float> Start = new HashMap<>();
        for (LivingEntity Each : Spared) {
            Start.put(Each.getUUID(), Each.getHealth());
        }
        Helper.onEachTick(() -> {
            for (LivingEntity Each : Spared) {
                if (Each.isRemoved() || !Each.isAlive()) {
                    Breaches.add(Agent + " took " + name(Each));
                } else if (Caught.test(Each)) {
                    Breaches.add(name(Each) + " caught " + Agent);
                } else if (Each.getHealth() < Start.get(Each.getUUID())) {
                    Breaches.add(name(Each) + " took damage while exposed to " + Agent);
                }
            }
        });
        return Breaches;
    }

    private static void clean(GameTestHelper Helper, List<String> Breaches) {
        Helper.assertTrue(Breaches.isEmpty(), Breaches.isEmpty() ? "" : Breaches.get(0));
    }

    private static boolean fleshNear(GameTestHelper Helper, Entity Around) {
        return !Helper.getLevel().getEntitiesOfClass(Scp610Creature.class, Around.getBoundingBox().inflate(2.0), Scp610Creature::isAlive).isEmpty();
    }

    @GameTest(template = Arena, timeoutTicks = Watch + 100, batch = "simmune_610")
    public static void fleshSparesImmortals(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        List<LivingEntity> Spared = spared(Helper);
        Zombie Shambler = still(Helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.5, Floor, 27.5)));
        Villager Farmer = still(Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(27.5, Floor, 27.5)));
        for (LivingEntity Each : Spared) {
            Helper.assertTrue(InfectionImmunity.isImmune(Each, Scp610Registry.Immune), name(Each) + " is not immune to SCP-610");
            Helper.assertFalse(Scp610Infection.infect(Each), "SCP-610 infected " + name(Each));
            Helper.assertFalse(Scp610Registry.organism().infect(Each), "SCP-610 passed on by contact to " + name(Each));
            Helper.assertFalse(Scp610Infection.exposable(Each), name(Each) + " is still picked as an SCP-610 target");
            Scp610Infection.exposeAround(World, Each.getEyePosition(), LabExposure.NothingIgnored, Reach);
            Helper.assertTrue(Scp610Hosts.convert(World, Each) == null && !Each.isRemoved(), "SCP-610 converted " + name(Each));
            Helper.assertFalse(Scp610Creature.isQuarry(Shambler, Each), name(Each) + " is hunted by SCP-610 as a host");
        }
        Helper.assertTrue(Scp610Infection.infect(Shambler), "SCP-610 no longer infects a zombie");
        Helper.assertTrue(Scp610Infection.infect(Farmer), "SCP-610 no longer infects a villager");
        Scp610Infection.setElapsed(Shambler, Scp610Infection.MobConvertTicks - 2);
        List<String> Breaches = watch(Helper, Spared, "SCP-610", Scp610Infection::isInfected);
        Helper.runAfterDelay(Watch, () -> {
            clean(Helper, Breaches);
            Helper.assertTrue(Shambler.isRemoved(), "an infected zombie was never taken by SCP-610");
            Helper.assertFalse(Helper.getLevel().getEntitiesOfClass(Scp610Creature.class, area(Helper), Scp610Creature::isAlive).isEmpty(), "the infected zombie did not rise as SCP-610");
            Helper.assertTrue(Scp610Infection.isInfected(Farmer), "the villager lost its SCP-610 infection");
            sweep(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "simmune_610_leftover")
    public static void leftoverFleshNeverConverts(GameTestHelper Helper) {
        sweep(Helper);
        List<LivingEntity> Spared = spared(Helper);
        for (LivingEntity Each : Spared) {
            Each.setData(Scp610Registry.Infection.get(), Scp610Infection.MobConvertTicks - 2);
        }
        Helper.assertTrue(Scp610Infection.isInfected(Spared.get(0)), "the leftover SCP-610 infection could not be planted");
        Helper.runAfterDelay(40, () -> {
            for (LivingEntity Each : Spared) {
                Helper.assertTrue(Each.isAlive() && !Each.isRemoved(), "a leftover SCP-610 infection took " + name(Each));
                Helper.assertFalse(Scp610Infection.isInfected(Each), "a leftover SCP-610 infection stayed on " + name(Each));
                Helper.assertFalse(fleshNear(Helper, Each), "SCP-610 flesh rose from " + name(Each));
            }
            sweep(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = Watch + 100, batch = "simmune_008")
    public static void plagueSparesImmortals(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        List<LivingEntity> Spared = spared(Helper);
        Scp682 Reptile = (Scp682) Spared.get(0);
        Villager Farmer = still(Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(27.5, Floor, 27.5)));
        for (LivingEntity Each : Spared) {
            Helper.assertFalse(Scp008Infection.canCatch(Each), name(Each) + " can still catch SCP-008");
            Helper.assertFalse(Scp008Infection.infect(Each), "SCP-008 infected " + name(Each));
            Scp008Infection.exposeAround(World, Each.getEyePosition(), LabExposure.NothingIgnored, Reach);
        }
        Helper.assertTrue(Reptile.lingers(Scp682Signature.Plague), "SCP-008 left SCP-682 nothing to adapt to");
        Helper.assertTrue(Scp008Infection.infect(Farmer), "SCP-008 no longer infects a villager");
        List<String> Breaches = watch(Helper, Spared, "SCP-008", Scp008Infection::isInfected);
        Helper.runAfterDelay(Watch, () -> {
            clean(Helper, Breaches);
            Helper.assertTrue(Reptile.adaptation().holds(Scp682Signature.Plague), "SCP-682 never took on the plague mark from SCP-008");
            Helper.assertFalse(Reptile.lingers(Scp682Signature.Plague), "SCP-008 still lingers on SCP-682 after the plague mark");
            Helper.assertTrue(Scp008Infection.isInfected(Farmer), "the villager lost its SCP-008 infection");
            Scp008Infection.cure(Farmer);
            sweep(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = Watch + 100, batch = "simmune_020")
    public static void moldSparesImmortals(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        List<LivingEntity> Spared = spared(Helper);
        Villager Farmer = still(Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(27.5, Floor, 27.5)));
        for (LivingEntity Each : Spared) {
            Helper.assertFalse(Scp020Affliction.canCatch(Each), name(Each) + " can still catch SCP-020");
            Helper.assertFalse(Scp020Affliction.afflict(Each), "SCP-020 afflicted " + name(Each));
            Scp020Affliction.exposeAround(World, Each.getEyePosition(), LabExposure.NothingIgnored, Reach);
            Scp020Mold.breathe(Each);
        }
        Scp020Affliction.exposeAround(World, Farmer.getEyePosition(), LabExposure.NothingIgnored, Reach);
        Helper.assertTrue(Scp020Affliction.isAfflicted(Farmer), "SCP-020 no longer reaches a villager");
        List<String> Breaches = watch(Helper, Spared, "SCP-020", Scp020Affliction::isAfflicted);
        Helper.runAfterDelay(Watch, () -> {
            clean(Helper, Breaches);
            Scp020Affliction.cure(Farmer);
            sweep(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = Watch + 100, batch = "simmune_crystal")
    public static void mineralsLingerWithoutSpreading(GameTestHelper Helper) {
        sweep(Helper);
        List<LivingEntity> Spared = spared(Helper);
        Scp682 Reptile = (Scp682) Spared.get(0);
        DClass Neighbour = prisoner(Helper, 18.3, 16.5);
        DClass Control = prisoner(Helper, 5.5, 27.5);
        for (LivingEntity Each : Spared) {
            Helper.assertFalse(Scp409Exposure.touchCrystal(Each), "touching SCP-409 infected " + name(Each));
            Helper.assertFalse(Scp009Doom.doom(Each), "SCP-009 took hold of " + name(Each));
        }
        Helper.assertTrue(Reptile.lingers(Scp682Signature.Crystal) && Reptile.lingers(Scp682Signature.RedIce), "SCP-409 and SCP-009 left SCP-682 nothing to adapt to");
        Helper.assertTrue(Scp009Exposure.isVulnerable(Reptile), "red ice no longer reaches SCP-682 to adapt to");
        Helper.assertFalse(Scp009Exposure.isVulnerable(Spared.get(1)) || Scp009Exposure.isVulnerable(Spared.get(2)), "red ice still picks SCP-173 or SCP-096");
        CompoundTag Saved = Reptile.saveWithoutId(new CompoundTag());
        Helper.assertTrue(Saved.getInt("Lingering") != 0, "the lingering exposure is not saved");
        Helper.assertTrue(Scp409Exposure.touchCrystal(Control), "touching SCP-409 no longer infects a D-Class");
        Helper.assertTrue(Scp009Doom.doom(Control) || Scp009Doom.isDoomed(Control), "SCP-009 no longer dooms a D-Class");
        boolean[] Spread = {false};
        Helper.onEachTick(() -> Spread[0] |= Scp409Infection.isInfected(Neighbour) || Scp009Doom.isDoomed(Neighbour));
        List<String> Breaches = watch(Helper, Spared, "SCP-409 or SCP-009", Each -> Scp409Infection.isInfected(Each) || Scp009Doom.isDoomed(Each));
        Helper.runAfterDelay(Watch, () -> {
            clean(Helper, Breaches);
            Helper.assertFalse(Spread[0], "SCP-682 passed a lingering exposure on to a D-Class beside it");
            Helper.assertTrue(Reptile.adaptation().holds(Scp682Signature.Crystal), "SCP-682 never took on the crystal mark");
            Helper.assertTrue(Reptile.adaptation().holds(Scp682Signature.RedIce), "SCP-682 never took on the red ice mark");
            Helper.assertFalse(Reptile.lingers(Scp682Signature.Crystal) || Reptile.lingers(Scp682Signature.RedIce), "an exposure still lingers after its mark");
            Helper.assertTrue(Scp409Infection.isInfected(Control), "the D-Class lost its SCP-409 infection");
            Scp409Infection.cure(Control);
            Scp009Doom.clear(Control);
            sweep(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "simmune_leech")
    public static void leechesNeverLatchOnImmortals(GameTestHelper Helper) {
        sweep(Helper);
        List<LivingEntity> Spared = spared(Helper);
        Zombie Shambler = still(Helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.5, Floor, 27.5)));
        for (LivingEntity Each : Spared) {
            Scp1730Leech Leech = Helper.spawnWithNoFreeWill(Scp1730Registry.LeechType.get(), Helper.relativeVec(Each.position().add(0.0, 0.0, 2.0)));
            Each.invulnerableTime = 0;
            Leech.doHurtTarget(Each);
            Helper.assertFalse(Leech.latched(), "an SCP-1730-3 leech latched onto " + name(Each));
        }
        Scp1730Leech Control = Helper.spawnWithNoFreeWill(Scp1730Registry.LeechType.get(), new Vec3(7.5, Floor, 27.5));
        Shambler.invulnerableTime = 0;
        Helper.assertTrue(Control.doHurtTarget(Shambler) && Control.latched(), "an SCP-1730-3 leech no longer latches onto a zombie");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "simmune_tags")
    public static void semiImmortalsAreListed(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (String Path : Listed) {
            Optional<EntityType<?>> Found = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
            Helper.assertTrue(Found.isPresent(), Path + " is not a registered entity");
            Helper.assertTrue(Found.get().is(InfectionImmunity.SemiImmortal), Path + " is missing from the semi immortal tag");
            Helper.assertTrue(Found.get().is(InfectionImmunity.Immune), Path + " is missing from the infection immune tag");
            Helper.assertTrue(Found.get().is(Scp610Registry.Immune) || Found.get().is(InfectionImmunity.Immune), Path + " can still be reached by SCP-610");
        }
        int Avoiders = 0;
        for (EntityType<?> Type : BuiltInRegistries.ENTITY_TYPE) {
            if (Type.is(GateAvoiders)) {
                Avoiders++;
                Helper.assertTrue(Type.is(InfectionImmunity.SemiImmortal) && Type.is(InfectionImmunity.Immune), BuiltInRegistries.ENTITY_TYPE.getKey(Type) + " avoids the Gate but can be infected");
            }
        }
        Helper.assertTrue(Avoiders > 0, "the gate avoiders tag is empty");
        for (EntityType<?> Type : List.of(EntityType.ZOMBIE, EntityType.VILLAGER, EntityType.COW, DClassRegistry.DClassType.get())) {
            Entity Made = Type.create(World);
            Helper.assertTrue(Made != null && !InfectionImmunity.isImmune(Made), BuiltInRegistries.ENTITY_TYPE.getKey(Type) + " became immune to infection");
            Helper.assertFalse(Type.is(InfectionImmunity.SemiImmortal), BuiltInRegistries.ENTITY_TYPE.getKey(Type) + " counts as semi immortal");
        }
        Helper.assertFalse(InfectionImmunity.isImmune(Helper.makeMockPlayer(GameType.SURVIVAL)), "a survival player became immune to infection");
        Helper.succeed();
    }
}
