package net.rai.scp_unleashed.scp682.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.scp096.Scp096Persons;
import net.rai.scp_unleashed.scp096.Scp096Registry;
import net.rai.scp_unleashed.scp106.Scp106Registry;
import net.rai.scp_unleashed.scp106.Scp106Targeting;
import net.rai.scp_unleashed.scp008.Scp008Infection;
import net.rai.scp_unleashed.scp009.Scp009Doom;
import net.rai.scp_unleashed.scp009.Scp009Registry;
import net.rai.scp_unleashed.scp173.Scp173Registry;
import net.rai.scp_unleashed.scp173.Scp173Sight;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import net.rai.scp_unleashed.scp409.Scp409Exposure;
import net.rai.scp_unleashed.scp409.Scp409Infection;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457Registry;
import net.rai.scp_unleashed.scp409.Scp409Registry;
import net.rai.scp_unleashed.scp538.gametest.Scp538TestPlayer;
import net.rai.scp_unleashed.scp548.Scp548Registry;
import net.rai.scp_unleashed.scp682.Scp682;
import net.rai.scp_unleashed.scp682.Scp682Adaptation;
import net.rai.scp_unleashed.scp682.Scp682Lake;
import net.rai.scp_unleashed.scp682.Scp682Registry;
import net.rai.scp_unleashed.scp682.Scp682RemoverItem;
import net.rai.scp_unleashed.scp682.Scp682Rivals;
import net.rai.scp_unleashed.scp682.Scp682Signature;
import net.rai.scp_unleashed.scp682.Scp682Size;
import net.rai.scp_unleashed.scp999.Scp999Registry;
import net.rai.scp_unleashed.stasis.StasisContents;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp682GameTests {
    public static final String Arena = "scp682_arena";
    public static final String Pool = "scp682_pool";
    public static final BlockPos Middle = new BlockPos(16, 1, 16);
    public static final BlockPos PoolMiddle = new BlockPos(12, 2, 12);

    private Scp682GameTests() {
    }

    private static Scp682 reptile(GameTestHelper Helper, BlockPos Relative, boolean Still) {
        ServerLevel World = Helper.getLevel();
        Scp682 Made = Scp682Registry.Scp682Type.get().create(World);
        Vec3 Where = Helper.absoluteVec(Vec3.atBottomCenterOf(Relative));
        Made.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        Made.setNoAi(Still);
        Made.setHealth(Made.getMaxHealth());
        World.addFreshEntity(Made);
        return Made;
    }

    private static <T extends Entity> T spawn(GameTestHelper Helper, EntityType<T> Type, BlockPos Relative, boolean Still) {
        ServerLevel World = Helper.getLevel();
        T Made = Type.create(World);
        Vec3 Where = Helper.absoluteVec(Vec3.atBottomCenterOf(Relative));
        Made.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        if (Made instanceof Mob Body) {
            Body.setNoAi(Still);
            Body.setPersistenceRequired();
        }
        World.addFreshEntity(Made);
        return Made;
    }

    private static Scp538TestPlayer player(GameTestHelper Helper, BlockPos Relative, String Name, GameType Mode) {
        ServerLevel World = Helper.getLevel();
        Scp538TestPlayer User = new Scp538TestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(Vec3.atBottomCenterOf(Relative));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.enlist(true);
        User.setGameMode(Mode);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, Scp538TestPlayer User) {
        User.enlist(false);
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static AABB area(GameTestHelper Helper) {
        return new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(32.0, 14.0, 32.0).inflate(4.0);
    }

    private static void sweep(GameTestHelper Helper) {
        for (Entity Each : Helper.getLevel().getEntitiesOfClass(Entity.class, area(Helper), Each -> !(Each instanceof ServerPlayer))) {
            Each.discard();
        }
    }

    private static boolean hit(Scp682 Reptile, DamageSource Source, float Amount) {
        Reptile.invulnerableTime = 0;
        return Reptile.hurt(Source, Amount);
    }

    private static ResourceLocation kind(DamageSource Source) {
        return Scp682Adaptation.kindOf(Source);
    }

    private static List<DamageSource> sources(ServerLevel World) {
        return List.of(World.damageSources().magic(), World.damageSources().cactus(), World.damageSources().sweetBerryBush(), World.damageSources().wither(),
                World.damageSources().inWall(), World.damageSources().dragonBreath(), World.damageSources().generic(), World.damageSources().starve());
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_adaptation")
    public static void adaptation(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        DamageSource Magic = World.damageSources().magic();
        for (int Index = 0; Index < Scp682Adaptation.HitsToAdapt - 1; Index++) {
            Helper.assertTrue(hit(Reptile, Magic, 2.0f), "an early hit was ignored");
        }
        Helper.assertFalse(Reptile.adaptation().isImmune(kind(Magic)), "SCP-682 adapted too early");
        Helper.assertTrue(hit(Reptile, Magic, 2.0f), "the adapting hit was ignored");
        Helper.assertTrue(Reptile.adaptation().isImmune(kind(Magic)), "SCP-682 did not adapt after enough hits");
        float Before = Reptile.getHealth();
        Helper.assertFalse(hit(Reptile, Magic, 5.0f), "an adapted damage type still hurt");
        Helper.assertTrue(Reptile.getHealth() == Before, "health dropped from an adapted damage type");
        Helper.assertTrue(Reptile.hide() != 0 && Reptile.adaptedCount() == 1, "the adaptation is not shown on the hide");
        CompoundTag Saved = Reptile.adaptation().save();
        Scp682Adaptation Loaded = new Scp682Adaptation();
        Loaded.load(Saved);
        Helper.assertTrue(Loaded.isImmune(kind(Magic)), "the immunity did not survive saving");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_immunityCap")
    public static void immunityCap(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        List<DamageSource> Kinds = sources(World).subList(0, Scp682Adaptation.MaxImmunities + 1);
        for (DamageSource Source : Kinds) {
            for (int Index = 0; Index < Scp682Adaptation.HitsToAdapt; Index++) {
                hit(Reptile, Source, 1.0f);
            }
        }
        List<ResourceLocation> Held = Reptile.adaptation().immunities();
        Helper.assertTrue(Held.size() == Scp682Adaptation.MaxImmunities, "SCP-682 holds " + Held.size() + " immunities");
        Helper.assertFalse(Reptile.adaptation().isImmune(kind(Kinds.get(0))), "the oldest immunity was not pushed out");
        for (int Index = 1; Index < Kinds.size(); Index++) {
            Helper.assertTrue(Held.get(Index - 1).equals(kind(Kinds.get(Index))), "immunities are not kept oldest first");
        }
        Helper.assertTrue(Reptile.size() == Scp682Size.Full, "small hits shrank SCP-682");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_acidNever")
    public static void acidNever(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        DamageSource Acid = World.damageSources().source(Scp682Registry.AcidDamage);
        float Start = Reptile.getHealth();
        for (int Index = 0; Index < Scp682Adaptation.HitsToAdapt * 4; Index++) {
            Helper.assertTrue(hit(Reptile, Acid, 1.0f), "acid stopped hurting SCP-682 after " + Index + " hits");
        }
        Helper.assertFalse(Reptile.adaptation().isImmune(Scp682Registry.AcidDamage.location()), "SCP-682 adapted to sulfuric acid");
        Helper.assertTrue(Reptile.getHealth() < Start, "acid did no damage");
        Helper.assertTrue(Reptile.adaptation().adapt(Scp682Registry.AcidDamage.location(), Scp682Adaptation.Category.Other) == null
                && !Reptile.adaptation().isImmune(Scp682Registry.AcidDamage.location()), "acid could be adapted to directly");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp682_unkillable")
    public static void unkillable(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        hit(Reptile, World.damageSources().generic(), Float.MAX_VALUE);
        Helper.assertTrue(Reptile.isAlive() && Reptile.getHealth() >= 1.0f, "SCP-682 died to a huge hit");
        Helper.assertTrue(Reptile.isCollapsed(), "SCP-682 did not collapse at 1 HP");
        Reptile.kill();
        Helper.assertTrue(Reptile.isAlive() && !Reptile.isRemoved(), "kill() removed SCP-682");
        Reptile.die(World.damageSources().generic());
        Helper.assertTrue(Reptile.isAlive() && !Reptile.isRemoved(), "die() removed SCP-682");
        Reptile.setHealth(0.0f);
        Helper.assertTrue(Reptile.isAlive() && Reptile.getHealth() >= 1.0f, "zero health killed SCP-682");
        Helper.assertFalse(hit(Reptile, World.damageSources().fellOutOfWorld(), Float.MAX_VALUE), "the void hurt SCP-682");
        Helper.assertTrue(Reptile.isAlive(), "the void killed SCP-682");
        Reptile.teleportTo(Reptile.getX(), World.getMinBuildHeight() - 80.0, Reptile.getZ());
        Helper.runAfterDelay(5, () -> Helper.assertTrue(Reptile.isAlive() && Reptile.getY() > World.getMinBuildHeight(), "SCP-682 was lost to the void"));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Reptile.isAlive(), "SCP-682 died");
            Helper.assertFalse(Reptile.isCollapsed() || Reptile.isRising(), "SCP-682 has not risen yet");
            Helper.assertTrue(Reptile.getHealth() >= Reptile.getMaxHealth() * Scp682.RiseShare - 0.01f, "SCP-682 rose without regenerating");
            sweep(Helper);
        });
    }

    public static final List<String> RivalIds = List.of("scp939", "scp049_2", "scp035_host", "scp008_zombie", "scp016_brute", "scp016_duelist", "scp354_brute", "scp354_charger",
            "scp354_crawler", "scp407_titan", "scp427_1", "scp323", "scp019_brute", "scp439_brute");
    public static final List<String> HookedIds = List.of("scp076", "scp457");
    public static final int RivalTicks = 600;
    public static final BlockPos Ground = Middle.above();

    private static boolean aimsAt(Mob Hunter, LivingEntity Prey) {
        if (Hunter.getTarget() == Prey) {
            return true;
        }
        return Hunter.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && Hunter.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == Prey;
    }

    @GameTestGenerator
    public static Collection<TestFunction> rivals() {
        List<TestFunction> Tests = new ArrayList<>();
        for (String Id : RivalIds) {
            String Name = "scp682_rival_" + Id;
            Tests.add(new TestFunction(Name, Name, Scp_unleashed.ModId + ":" + Arena, RivalTicks, 0L, true, Helper -> rival(Helper, Id, true)));
        }
        for (String Id : HookedIds) {
            String Name = "scp682_rival_" + Id;
            Tests.add(new TestFunction(Name, Name, Scp_unleashed.ModId + ":" + Arena, RivalTicks, 0L, true, Helper -> rival(Helper, Id, false)));
        }
        return Tests;
    }

    private static void rival(GameTestHelper Helper, String Id, boolean Tagged) {
        sweep(Helper);
        EntityType<?> Type = BuiltInRegistries.ENTITY_TYPE.get(Scp682Registry.location(Id));
        Helper.assertTrue(Type.is(Scp682Registry.Rivals) == Tagged, Id + (Tagged ? " is not in the scp682_rivals tag" : " is in the scp682_rivals tag but is hooked instead"));
        Scp682 Reptile = reptile(Helper, Ground, true);
        Entity Made = spawn(Helper, Type, Ground.offset(8, 0, 0), false);
        Helper.assertTrue(Made instanceof Mob, Id + " did not spawn as a mob");
        Mob Hunter = (Mob) Made;
        boolean[] Acquired = {false};
        boolean[] Landed = {false};
        float Full = Reptile.getHealth();
        Helper.onEachTick(() -> {
            if (Hunter.isAlive() && aimsAt(Hunter, Reptile)) {
                Acquired[0] = true;
            }
            if (Reptile.getLastHurtByMob() == Hunter || Reptile.getHealth() < Full && Reptile.getLastHurtByMob() != null && Reptile.getLastHurtByMob().getType() == Type) {
                Landed[0] = true;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Acquired[0], Id + " never targeted SCP-682");
            Helper.assertTrue(Landed[0], Id + " targeted SCP-682 but never landed a hit");
            sweep(Helper);
        });
    }

    private static Scp457 burning(GameTestHelper Helper, BlockPos Where, boolean Still) {
        return spawn(Helper, Scp457Registry.Burning.get(), Where, Still);
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp682_burnedBy457")
    public static void burnedBy457(GameTestHelper Helper) {
        sweep(Helper);
        Scp682 Reptile = reptile(Helper, Ground, true);
        Scp457 Burning = burning(Helper, Ground.offset(7, 0, 0), false);
        boolean[] Acquired = {false};
        boolean[] Burned = {false};
        int[] Since = {-1};
        int[] Clock = {0};
        Helper.onEachTick(() -> {
            Clock[0]++;
            if (Burning.isAlive() && Burning.getTarget() == Reptile) {
                Acquired[0] = true;
            }
            if (Reptile.getLastHurtByMob() == Burning) {
                Burned[0] = true;
            }
            if (Since[0] < 0 && Reptile.adaptation().holds(Scp682Signature.Cinder)) {
                Since[0] = Clock[0];
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Burning.isAlive(), "SCP-457 went out during the test");
            Helper.assertTrue(Acquired[0], "SCP-457 never went after SCP-682");
            Helper.assertTrue(Burned[0], "SCP-457 never landed a burn on SCP-682");
            Helper.assertTrue(Since[0] >= 0, "SCP-682 never took on the cinder mark from SCP-457's fire");
            Helper.assertTrue(Reptile.fireImmune() && !Reptile.isOnFire(), "SCP-682 still burns after the cinder mark");
            Helper.assertTrue(Clock[0] - Since[0] > 40, "waiting to see whether SCP-457 gives up");
            Helper.assertFalse(Burning.getTarget() == Reptile, "SCP-457 kept hunting a fireproof SCP-682");
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp682_fightsBack457")
    public static void fightsBack457(GameTestHelper Helper) {
        sweep(Helper);
        Scp682 Reptile = reptile(Helper, Ground, false);
        Scp457 Burning = burning(Helper, Ground.offset(7, 0, 0), false);
        boolean[] Struck = {false};
        Helper.onEachTick(() -> {
            if (Burning.getLastHurtByMob() == Reptile || !Burning.isAlive() && Burning.getKillCredit() == Reptile) {
                Struck[0] = true;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Reptile.getLastHurtByMob() == Burning || Reptile.getTarget() == Burning || Struck[0], "SCP-457 never engaged SCP-682");
            Helper.assertTrue(Struck[0], "SCP-682 never fought back against SCP-457");
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp682_hunts457")
    public static void hunts457(GameTestHelper Helper) {
        sweep(Helper);
        Scp682 Reptile = reptile(Helper, Ground, false);
        for (int Index = 0; Index < Scp682Signature.Cinder.threshold(); Index++) {
            Reptile.adaptation().expose(Scp682Signature.Cinder);
        }
        Reptile.syncAdaptation();
        Scp457 Burning = burning(Helper, Ground.offset(7, 0, 0), true);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Reptile.getTarget() == Burning || Burning.getLastHurtByMob() == Reptile || !Burning.isAlive(), "a fireproof SCP-682 ignored SCP-457");
            sweep(Helper);
        });
    }

    private static DamageSource typed(ServerLevel World, String Path) {
        return World.damageSources().source(ResourceKey.create(Registries.DAMAGE_TYPE, Scp682Registry.location(Path)));
    }

    private static Scp682 marked(GameTestHelper Helper) {
        Scp682 Reptile = reptile(Helper, Middle, true);
        for (DamageSource Source : sources(Helper.getLevel()).subList(0, Scp682Adaptation.MaxImmunities)) {
            Reptile.adaptation().adapt(kind(Source), Scp682Adaptation.Category.Other);
        }
        Reptile.syncAdaptation();
        return Reptile;
    }

    private static void pound(GameTestHelper Helper, Scp682 Reptile, Scp682Signature Signature, List<DamageSource> Blows) {
        for (int Index = 0; Index < Signature.threshold(); Index++) {
            Helper.assertFalse(Reptile.adaptation().holds(Signature), "SCP-682 took on the " + Signature.key() + " mark after only " + Index + " hits");
            Helper.assertTrue(hit(Reptile, Blows.get(Index % Blows.size()), 2.0f), "hit " + Index + " towards the " + Signature.key() + " mark was ignored");
        }
    }

    private static void gained(GameTestHelper Helper, Scp682 Reptile, Scp682Signature Signature) {
        Scp682Adaptation Held = Reptile.adaptation();
        Helper.assertTrue(Held.holds(Signature), "SCP-682 did not take on the " + Signature.key() + " mark");
        Helper.assertTrue((Reptile.signatures() & Signature.bit()) != 0, "the " + Signature.key() + " mark is not shown on the hide");
        List<ResourceLocation> Order = Held.immunities();
        Helper.assertTrue(Order.size() == Scp682Adaptation.MaxImmunities, "SCP-682 holds " + Order.size() + " adaptations with the " + Signature.key() + " mark");
        Helper.assertTrue(Order.get(Order.size() - 1).equals(Signature.id()), "the " + Signature.key() + " mark did not take the newest slot");
        Helper.assertFalse(Order.contains(kind(sources(Helper.getLevel()).get(0))), "the " + Signature.key() + " mark did not push out the oldest adaptation");
        Scp682Adaptation Loaded = new Scp682Adaptation();
        Loaded.load(Held.save());
        Helper.assertTrue(Loaded.holds(Signature), "the " + Signature.key() + " mark did not survive saving");
    }

    private static Entity foe(GameTestHelper Helper, String Path) {
        EntityType<?> Type = BuiltInRegistries.ENTITY_TYPE.get(Scp682Registry.location(Path));
        return spawn(Helper, Type, Middle.offset(7, 0, 0), true);
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_sigCinder")
    public static void sigCinder(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = marked(Helper);
        pound(Helper, Reptile, Scp682Signature.Cinder, List.of(World.damageSources().inFire(), World.damageSources().lava(), typed(World, "scp457_burn")));
        gained(Helper, Reptile, Scp682Signature.Cinder);
        Helper.assertTrue(Reptile.fireImmune(), "SCP-682 still burns after the cinder mark");
        Reptile.setRemainingFireTicks(100);
        Helper.assertFalse(Reptile.isOnFire(), "SCP-682 caught fire after the cinder mark");
        Helper.assertFalse(hit(Reptile, World.damageSources().hotFloor(), 4.0f) || hit(Reptile, typed(World, "scp457_burn"), 4.0f), "fire still hurts after the cinder mark");
        Helper.assertTrue(hit(Reptile, World.damageSources().source(Scp682Registry.AcidDamage), 2.0f), "the cinder mark kept out the acid");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_sigRime")
    public static void sigRime(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = marked(Helper);
        pound(Helper, Reptile, Scp682Signature.Rime, List.of(World.damageSources().freeze()));
        gained(Helper, Reptile, Scp682Signature.Rime);
        Helper.assertFalse(Reptile.canFreeze(), "SCP-682 can still freeze after the rime mark");
        Helper.assertFalse(Reptile.addEffect(new MobEffectInstance(Scp548Registry.Frostbite, 100)), "SCP-548's frostbite took hold after the rime mark");
        Helper.assertFalse(hit(Reptile, World.damageSources().freeze(), 4.0f), "frost still hurts after the rime mark");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp682_sigCrystal")
    public static void sigCrystal(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = marked(Helper);
        Helper.assertFalse(Scp409Exposure.touchCrystal(Reptile), "SCP-409 took hold of SCP-682");
        Helper.assertTrue(Reptile.lingers(Scp682Signature.Crystal), "touching SCP-409 left SCP-682 nothing to adapt to");
        Helper.succeedWhen(() -> {
            Helper.assertFalse(Scp409Infection.isInfected(Reptile), "SCP-682 caught SCP-409 while it adapted");
            Helper.assertTrue(Reptile.adaptation().holds(Scp682Signature.Crystal), "SCP-682 has not taken on the crystal mark yet");
            gained(Helper, Reptile, Scp682Signature.Crystal);
            Helper.assertFalse(Reptile.lingers(Scp682Signature.Crystal), "SCP-409 still lingers on SCP-682 after the crystal mark");
            Helper.assertTrue(InfectionImmunity.isImmune(Reptile, Scp409Registry.Immune), "SCP-409 can still reach SCP-682 after the crystal mark");
            Helper.assertFalse(Scp409Infection.infect(Reptile), "SCP-409 infected SCP-682 after the crystal mark");
            Helper.assertFalse(Reptile.lingers(Scp682Signature.Crystal), "SCP-409 lingered again after the crystal mark");
            Helper.assertFalse(hit(Reptile, typed(World, "scp409_overgrown"), 4.0f), "SCP-409's growth still hurts after the crystal mark");
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp682_sigRedIce")
    public static void sigRedIce(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = marked(Helper);
        Helper.assertFalse(Scp009Doom.doom(Reptile), "SCP-009 took hold of SCP-682");
        Helper.assertTrue(Reptile.lingers(Scp682Signature.RedIce), "SCP-009 left SCP-682 nothing to adapt to");
        Helper.succeedWhen(() -> {
            Helper.assertFalse(Scp009Doom.isDoomed(Reptile), "SCP-009 took hold of SCP-682 while it adapted");
            Helper.assertTrue(Reptile.adaptation().holds(Scp682Signature.RedIce), "SCP-682 has not taken on the red ice mark yet");
            gained(Helper, Reptile, Scp682Signature.RedIce);
            Helper.assertFalse(Reptile.lingers(Scp682Signature.RedIce), "SCP-009 still lingers on SCP-682 after the red ice mark");
            Helper.assertFalse(Reptile.addEffect(new MobEffectInstance(Scp009Registry.Doomed, 200)), "SCP-009 took hold again after the red ice mark");
            Helper.assertFalse(hit(Reptile, typed(World, "scp009_crystallized"), 4.0f), "red ice still hurts after the red ice mark");
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp682_sigPlague")
    public static void sigPlague(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = marked(Helper);
        Entity Carrier = foe(Helper, "scp008_zombie");
        Helper.assertTrue(Carrier instanceof LivingEntity, "no SCP-008 instance to test with");
        pound(Helper, Reptile, Scp682Signature.Plague, List.of(World.damageSources().mobAttack((LivingEntity) Carrier)));
        gained(Helper, Reptile, Scp682Signature.Plague);
        Helper.assertFalse(hit(Reptile, World.damageSources().mobAttack((LivingEntity) Carrier), 4.0f), "plague carriers still hurt after the plague mark");
        Helper.assertFalse(hit(Reptile, typed(World, "scp049_cure"), 4.0f), "SCP-049's cure still hurts after the plague mark");
        Helper.assertFalse(Scp008Infection.infect(Reptile), "SCP-008 took hold of SCP-682");
        Helper.succeedWhen(() -> {
            Helper.assertFalse(Scp008Infection.isInfected(Reptile), "SCP-008 still lingers after the plague mark");
            Helper.assertFalse(Reptile.lingers(Scp682Signature.Plague), "the plague exposure did not settle into the plague mark");
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_sigInsulated")
    public static void sigInsulated(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = marked(Helper);
        pound(Helper, Reptile, Scp682Signature.Insulated, List.of(typed(World, "tesla")));
        gained(Helper, Reptile, Scp682Signature.Insulated);
        Helper.assertFalse(hit(Reptile, typed(World, "tesla"), 4.0f) || hit(Reptile, World.damageSources().lightningBolt(), 4.0f), "electricity still hurts after the insulated mark");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_sigPlated")
    public static void sigPlated(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = marked(Helper);
        pound(Helper, Reptile, Scp682Signature.Plated, List.of(typed(World, "gunshot"), typed(World, "sentry_turret")));
        gained(Helper, Reptile, Scp682Signature.Plated);
        Helper.assertFalse(hit(Reptile, typed(World, "sentry_turret"), 4.0f) || hit(Reptile, typed(World, "gunshot"), 4.0f), "gunfire still hurts after the plated mark");
        Helper.assertTrue(hit(Reptile, typed(World, "micro_hid"), 4.0f), "the plated mark also stopped the MICRO-HID");
        Helper.assertTrue(hit(Reptile, World.damageSources().source(Scp682Registry.AcidDamage), 2.0f), "the plated mark kept out the acid");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_sigScarred")
    public static void sigScarred(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = marked(Helper);
        Entity Hunter = foe(Helper, "scp939");
        Helper.assertTrue(Hunter instanceof LivingEntity, "no SCP-939 instance to test with");
        pound(Helper, Reptile, Scp682Signature.Scarred, List.of(World.damageSources().mobAttack((LivingEntity) Hunter), typed(World, "scp427_maul")));
        gained(Helper, Reptile, Scp682Signature.Scarred);
        Helper.assertFalse(hit(Reptile, World.damageSources().mobAttack((LivingEntity) Hunter), 4.0f) || hit(Reptile, typed(World, "scp323_claw"), 4.0f),
                "other anomalies' claws still hurt after the scarred mark");
        Helper.assertTrue(hit(Reptile, World.damageSources().magic(), 2.0f), "the scarred mark blocked unrelated damage");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_sigStack")
    public static void sigStack(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        List<Scp682Signature> Order = List.of(Scp682Signature.Cinder, Scp682Signature.Rime, Scp682Signature.Insulated, Scp682Signature.Plated, Scp682Signature.Crystal,
                Scp682Signature.RedIce);
        for (Scp682Signature Each : Order) {
            for (int Index = 0; Index < Each.threshold(); Index++) {
                Reptile.adaptation().expose(Each);
            }
            Reptile.syncAdaptation();
        }
        Helper.assertTrue(Reptile.adaptation().count() == Scp682Adaptation.MaxImmunities, "more than five marks were kept");
        Helper.assertFalse(Reptile.adaptation().holds(Scp682Signature.Cinder) || Reptile.fireImmune(), "the oldest mark was not pushed out by the sixth");
        Helper.assertTrue(Integer.bitCount(Reptile.signatures()) == Scp682Adaptation.MaxImmunities, "the hide shows " + Integer.bitCount(Reptile.signatures()) + " marks");
        Helper.assertTrue(hit(Reptile, World.damageSources().source(Scp682Registry.AcidDamage), 2.0f), "five marks kept out the acid");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp682_collapseHold")
    public static void collapseHold(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682Size[] Sizes = Scp682Size.values();
        Scp682[] Downed = new Scp682[Sizes.length];
        int[] Rising = new int[Sizes.length];
        boolean[] Stood = new boolean[Sizes.length];
        for (int Index = 0; Index < Sizes.length; Index++) {
            Downed[Index] = reptile(Helper, Middle.offset(-9 + Index * 9, 0, 0), true);
            Downed[Index].setSize(Sizes[Index]);
            hit(Downed[Index], World.damageSources().generic(), Float.MAX_VALUE);
            Helper.assertTrue(Downed[Index].isCollapsed() && !Downed[Index].isRising(), Sizes[Index].key() + " SCP-682 did not collapse at 1 HP");
        }
        DamageSource Acid = World.damageSources().source(Scp682Registry.AcidDamage);
        List<DamageSource> Kinds = sources(World);
        int[] Clock = {0};
        Helper.onEachTick(() -> {
            Clock[0]++;
            for (int Index = 0; Index < Downed.length; Index++) {
                Scp682 Reptile = Downed[Index];
                String Label = Sizes[Index].key();
                if (Stood[Index]) {
                    Helper.assertFalse(Reptile.isCollapsed() || Reptile.isRising(), Label + " SCP-682 went down again after standing");
                    continue;
                }
                if (Reptile.collapseLeft() > 0) {
                    Helper.assertTrue(Reptile.isCollapsed() && !Reptile.isRising() && Rising[Index] == 0,
                            Label + " SCP-682 left the collapse with " + Reptile.collapseLeft() + " ticks to go");
                    if (Clock[0] % 5 == 0) {
                        hit(Reptile, Kinds.get(Clock[0] / 5 % Kinds.size()), 60.0f);
                        hit(Reptile, Acid, 2.0f);
                        Helper.assertTrue(Reptile.isCollapsed() && Reptile.isAlive(), Label + " SCP-682 was knocked out of its collapse by a hit");
                    }
                    continue;
                }
                Helper.assertFalse(Reptile.isCollapsed(), Label + " SCP-682 is collapsed with no collapse time left");
                if (Reptile.isRising()) {
                    Rising[Index]++;
                    continue;
                }
                Helper.assertTrue(Rising[Index] >= Scp682.RiseTicks - 1, Label + " SCP-682 rose in " + Rising[Index] + " ticks, not " + Scp682.RiseTicks);
                Stood[Index] = true;
            }
        });
        Helper.succeedWhen(() -> {
            for (int Index = 0; Index < Downed.length; Index++) {
                Helper.assertTrue(Stood[Index], Sizes[Index].key() + " SCP-682 has not stood up yet");
            }
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp682_collapseAcid")
    public static void collapseAcid(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        BlockState Acid = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("scp_unleashed", "sulfuric_acid")).defaultBlockState();
        for (int X = Middle.getX() - 3; X <= Middle.getX() + 3; X++) {
            for (int Z = Middle.getZ() - 3; Z <= Middle.getZ() + 3; Z++) {
                Helper.setBlock(new BlockPos(X, Middle.getY() - 1, Z), Acid);
            }
        }
        Scp682 Reptile = reptile(Helper, Middle.below(), true);
        Reptile.setSize(Scp682Size.Reduced);
        hit(Reptile, World.damageSources().generic(), Float.MAX_VALUE);
        Helper.assertTrue(Reptile.isCollapsed(), "SCP-682 did not collapse at 1 HP");
        int[] Clock = {0};
        int[] Downs = {1};
        boolean[] Was = {true};
        Helper.onEachTick(() -> {
            Clock[0]++;
            boolean Down = Reptile.isCollapsed();
            if (Down && !Was[0]) {
                Downs[0]++;
            }
            Was[0] = Down;
            Helper.assertTrue(Downs[0] == 1, "SCP-682 got up and fell straight back down in the acid");
            if (Reptile.size() != Scp682Size.Shrunk) {
                Helper.assertTrue(Down && !Reptile.isRising(), "SCP-682 tried to rise in acid at " + Reptile.size().key() + " size after " + Clock[0] + " ticks");
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Clock[0] > Scp682.CollapseTicks, "the collapse has not run its course");
            Helper.assertTrue(Reptile.size() == Scp682Size.Shrunk, "SCP-682 has not shrunk in the acid yet");
            Helper.assertFalse(Reptile.isCollapsed() || Reptile.isRising(), "shrunk SCP-682 has not stood up in the acid yet");
            Helper.assertTrue(Reptile.isAlive() && Reptile.getHealth() >= 1.0f, "SCP-682 did not survive the acid");
            for (int X = Middle.getX() - 3; X <= Middle.getX() + 3; X++) {
                for (int Z = Middle.getZ() - 3; Z <= Middle.getZ() + 3; Z++) {
                    Helper.setBlock(new BlockPos(X, Middle.getY() - 1, Z), Blocks.SMOOTH_STONE.defaultBlockState());
                }
            }
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp682_killCreative")
    public static void killCreative(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        Pig Bystander = spawn(Helper, EntityType.PIG, Middle.offset(-3, 0, 3), true);
        Scp538TestPlayer User = player(Helper, Middle.offset(-6, 0, 0), "scp682_creative", GameType.CREATIVE);
        World.getServer().getCommands().performPrefixedCommand(User.createCommandSourceStack().withPermission(4), "kill @e[distance=..10,type=!minecraft:player]");
        Helper.assertFalse(Bystander.isAlive(), "the other target survived the kill command");
        Helper.assertTrue(Reptile.isAlive() && Reptile.isCommanding() && Reptile.isCommandBusy(), "the command block did not rise");
        Helper.runAfterDelay(Scp682.CommandVerdict + 4, () -> {
            Helper.assertTrue(User.isDeadOrDying(), "the creative player survived");
            Helper.assertTrue(Reptile.isAlive(), "SCP-682 died to the kill command");
        });
        Helper.succeedWhen(() -> {
            Helper.assertFalse(Reptile.isCommandBusy() || Reptile.isCommanding(), "the command block did not withdraw");
            Helper.assertTrue(Reptile.isAlive(), "SCP-682 died");
            dismiss(Helper, User);
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp682_killSurvival")
    public static void killSurvival(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        Scp538TestPlayer User = player(Helper, Middle.offset(-12, 0, -12), "scp682_survival", GameType.SURVIVAL);
        Vec3 Start = User.position();
        World.getServer().getCommands().performPrefixedCommand(User.createCommandSourceStack().withPermission(4), "kill @e[type=scp_unleashed:scp682,distance=..30]");
        Helper.assertTrue(Reptile.isCommandBusy(), "the kill command was not answered");
        Helper.runAfterDelay(Scp682.CommandVerdict + 1, () -> Helper.assertTrue(User.position().distanceTo(Start) > 4.0 && User.distanceTo(Reptile) < Reptile.reach() + 2.0,
                "the player was not pulled to SCP-682"));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(User.isDeadOrDying(), "the survival player was not killed");
            Helper.assertTrue(Reptile.isAlive(), "SCP-682 died");
            dismiss(Helper, User);
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp682_killConsole")
    public static void killConsole(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        Scp538TestPlayer User = player(Helper, Middle.offset(-8, 0, 0), "scp682_watcher", GameType.CREATIVE);
        Vec3 At = Reptile.position();
        World.getServer().getCommands().performPrefixedCommand(World.getServer().createCommandSourceStack().withPosition(At).withPermission(4),
                "kill @e[type=scp_unleashed:scp682,distance=..4]");
        Helper.assertTrue(Reptile.isCommanding() && Reptile.commandVictim() == null, "the console kill did not play the visual alone");
        Helper.succeedWhen(() -> {
            Helper.assertFalse(Reptile.isCommandBusy(), "the visual did not finish");
            Helper.assertTrue(Reptile.isAlive() && User.isAlive(), "the console kill hurt someone");
            dismiss(Helper, User);
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_remover")
    public static void remover(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        FakePlayer Survivor = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp682_remover_survival"));
        Survivor.setGameMode(GameType.SURVIVAL);
        Helper.assertFalse(Scp682RemoverItem.remove(Survivor, Reptile), "the remover worked outside creative");
        Helper.assertTrue(Reptile.isAlive() && !Reptile.isRemoved(), "the remover removed SCP-682 outside creative");
        FakePlayer Builder = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp682_remover_creative"));
        Builder.setGameMode(GameType.CREATIVE);
        Helper.assertTrue(Scp682RemoverItem.remove(Builder, Reptile), "the remover failed in creative");
        Helper.assertTrue(Reptile.isRemoved(), "SCP-682 is still in the world");
        Helper.assertTrue(new ItemStack(Scp682Registry.Remover.get()).is(TagKey.<Item>create(Registries.ITEM, Scp682Registry.location("acid_proof"))),
                "the remover is not acid proof");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_shrink")
    public static void shrink(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        Helper.assertTrue(Reptile.getBbHeight() >= 3.0f && Reptile.getBbHeight() <= 4.0f, "full size SCP-682 is " + Reptile.getBbHeight() + " blocks tall, not about 4");
        List<DamageSource> Kinds = sources(World);
        for (int Index = 0; Index < Scp682Adaptation.UniqueToShrink - 1; Index++) {
            hit(Reptile, Kinds.get(Index), Scp682Adaptation.UniqueDamage + 1.0f);
        }
        Helper.assertTrue(Reptile.size() == Scp682Size.Full, "SCP-682 shrank before enough unique damage");
        hit(Reptile, Kinds.get(Scp682Adaptation.UniqueToShrink - 1), Scp682Adaptation.UniqueDamage + 1.0f);
        Helper.assertTrue(Reptile.size() == Scp682Size.Reduced, "enough unique damage did not shrink SCP-682");
        float Wide = Reptile.getBbWidth();
        Helper.assertTrue(Wide < Scp682.Width * 0.7f, "the hitbox did not shrink");
        Helper.assertTrue(Reptile.getBbHeight() >= 1.4f && Reptile.getBbHeight() <= 2.0f, "reduced SCP-682 is " + Reptile.getBbHeight() + " blocks tall, not about 2");
        for (int Index = 0; Index < Scp682Adaptation.UniqueToShrink; Index++) {
            hit(Reptile, Kinds.get(Index + 4), Scp682Adaptation.UniqueDamage + 1.0f);
        }
        Helper.assertTrue(Reptile.size() == Scp682Size.Shrunk, "a second round did not shrink SCP-682 to its smallest size");
        Helper.assertTrue(Reptile.getBbHeight() <= 1.0f && Reptile.getBbWidth() < 1.0f, "shrunk SCP-682 is not pet sized");
        Reptile.setRegrowWait(1);
        Reptile.acidTick(World, 10);
        Helper.assertTrue(Reptile.size() == Scp682Size.Reduced, "SCP-682 did not regrow out of acid");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_acidRegrow")
    public static void acidRegrow(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        for (int X = Middle.getX() - 3; X <= Middle.getX() + 3; X++) {
            for (int Z = Middle.getZ() - 3; Z <= Middle.getZ() + 3; Z++) {
                Helper.setBlock(new BlockPos(X, Middle.getY() - 1, Z), BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("scp_unleashed", "sulfuric_acid")).defaultBlockState());
            }
        }
        Scp682 Reptile = reptile(Helper, Middle.below(), true);
        Reptile.setSize(Scp682Size.Reduced);
        Helper.assertTrue(Reptile.acidAt(), "SCP-682 does not notice the acid around it");
        Reptile.setRegrowWait(1);
        for (int Step = 0; Step < Scp682.AcidShrinkTicks / 10; Step++) {
            Reptile.acidTick(World, 10);
        }
        Helper.assertTrue(Reptile.size() == Scp682Size.Shrunk, "SCP-682 regrew or failed to shrink in acid");
        Helper.assertTrue(Reptile.isSoaked() && Reptile.regrowWait() == Scp682.RegrowTicks, "acid did not hold back regrowth");
        for (int Step = 0; Step < 20; Step++) {
            Reptile.acidTick(World, 10);
        }
        Helper.assertTrue(Reptile.size() == Scp682Size.Shrunk, "SCP-682 grew while soaked");
        DamageSource Acid = World.damageSources().source(Scp682Registry.AcidDamage);
        float Before = Reptile.getHealth();
        Helper.assertFalse(hit(Reptile, Acid, 2.0f), "acid still hurts shrunk SCP-682");
        Helper.assertTrue(Reptile.getHealth() == Before && !Reptile.isCollapsed(), "acid changed shrunk SCP-682's health");
        Reptile.setSize(Scp682Size.Reduced);
        Helper.assertTrue(hit(Reptile, Acid, 2.0f), "acid stopped hurting reduced SCP-682");
        Reptile.setSize(Scp682Size.Shrunk);
        Helper.assertFalse(Reptile.roarReady(), "soaked SCP-682 is ready to roar");
        for (int X = Middle.getX() - 3; X <= Middle.getX() + 3; X++) {
            for (int Z = Middle.getZ() - 3; Z <= Middle.getZ() + 3; Z++) {
                Helper.setBlock(new BlockPos(X, Middle.getY() - 1, Z), Blocks.SMOOTH_STONE.defaultBlockState());
            }
        }
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_stasis")
    public static void stasis(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp682_stasis"));
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Helper.assertTrue(StasisPods.refusal(User, Pod, Reptile) != null, "a stasis pod accepts full size SCP-682");
        Reptile.setSize(Scp682Size.Reduced);
        Helper.assertTrue(StasisPods.refusal(User, Pod, Reptile) != null, "a stasis pod accepts reduced SCP-682");
        Reptile.setSize(Scp682Size.Shrunk);
        Helper.assertTrue(StasisPods.refusal(User, Pod, Reptile) == null, "a stasis pod refuses shrunk SCP-682");
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Reptile), "a stasis pod failed to capture shrunk SCP-682");
        Helper.assertTrue(Reptile.isRemoved(), "the captured SCP-682 is still in the world");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_blocks")
    public static void blocks(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        BlockState Hard = BuiltInRegistries.BLOCK.get(Scp682Registry.location("reinforced_containment_wall")).defaultBlockState();
        for (int Z = -1; Z <= 1; Z++) {
            for (int Y = 0; Y <= 1; Y++) {
                BlockPos Pos = Middle.offset(2, Y, Z);
                Helper.setBlock(Pos, Z == 1 ? Hard : Z == -1 ? Blocks.BEDROCK.defaultBlockState() : Blocks.STONE.defaultBlockState());
            }
        }
        Scp682 Reptile = reptile(Helper, Middle, true);
        Reptile.setSize(Scp682Size.Shrunk);
        Vec3 East = new Vec3(1.0, 0.0, 0.0);
        for (int Tick = 0; Tick < 80; Tick++) {
            Helper.assertFalse(Reptile.smasher().tear(World, Reptile, East), "shrunk SCP-682 tried to break a block");
        }
        Helper.assertBlockPresent(Blocks.STONE, Middle.offset(2, 1, 0));
        Reptile.setSize(Scp682Size.Full);
        for (int Tick = 0; Tick < 120; Tick++) {
            Reptile.smasher().tear(World, Reptile, East);
        }
        Helper.assertBlockNotPresent(Blocks.STONE, Middle.offset(2, 1, 0));
        Helper.assertBlockPresent(Blocks.BEDROCK, Middle.offset(2, 1, -1));
        Helper.assertTrue(!Hard.isAir() && Helper.getBlockState(Middle.offset(2, 1, 1)).is(Hard.getBlock()), "full size SCP-682 broke a containment wall");
        for (int Z = -1; Z <= 1; Z++) {
            for (int Y = 0; Y <= 1; Y++) {
                Helper.setBlock(Middle.offset(2, Y, Z), Blocks.AIR.defaultBlockState());
            }
        }
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp682_eyes173")
    public static void eyes173(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        Scp173Statue Statue = spawn(Helper, Scp173Registry.Statue.get(), Middle.offset(7, 0, 0), true);
        Helper.assertTrue(Statue.hunt().isPrey(World, Reptile), "SCP-173 does not treat SCP-682 as prey");
        Helper.runAfterDelay(30, () -> {
            Helper.assertFalse(Scp173Sight.isObserved(World, Statue), "SCP-173 is watched before SCP-682 grows eyes");
            DamageSource Snap = World.damageSources().source(Scp173Registry.Snap, Statue);
            for (int Index = 0; Index < Scp682Adaptation.HitsToAdapt; Index++) {
                hit(Reptile, Snap, Float.MAX_VALUE);
            }
            Helper.assertTrue(Reptile.isAlive(), "SCP-173 killed SCP-682");
            Helper.assertTrue(Reptile.eyes() == Scp682.EyeMany, "SCP-682 did not grow eyes after adapting to SCP-173");
            Helper.assertTrue(Scp173Sight.isObserved(World, Statue), "SCP-682's eyes do not hold SCP-173");
        });
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(Scp173Sight.isObserved(World, Statue), "SCP-173 is not kept frozen");
            sweep(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp682_eyeless096")
    public static void eyeless096(GameTestHelper Helper) {
        sweep(Helper);
        Scp682 Reptile = reptile(Helper, Middle, true);
        LivingEntity Guy = spawn(Helper, Scp096Registry.ShyGuy.get(), Middle.offset(8, 0, 0), true);
        Helper.assertFalse(Scp096Persons.isWitness(Reptile), "SCP-096 counts SCP-682 as a witness");
        Helper.assertFalse(Reptile.canAttack(Guy), "SCP-682 may attack SCP-096");
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Reptile.eyes() == Scp682.EyeSealed, "SCP-682's eyes did not seal near SCP-096");
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_ignore106")
    public static void ignore106(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        LivingEntity OldMan = spawn(Helper, Scp106Registry.OldMan.get(), Middle.offset(6, 0, 0), true);
        Helper.assertFalse(Scp106Targeting.valid(World, Reptile), "SCP-106 would target SCP-682");
        Helper.assertTrue(Scp106Targeting.choose(World, OldMan.position(), 24.0, 24.0, 0.0, null, 1.0, false, false) != Reptile, "SCP-106 chose SCP-682");
        Helper.assertFalse(Reptile.canAttack(OldMan), "SCP-682 may attack SCP-106");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp682_calm999")
    public static void calm999(GameTestHelper Helper) {
        sweep(Helper);
        Scp682 Reptile = reptile(Helper, Middle, true);
        LivingEntity Blob = spawn(Helper, Scp999Registry.Blob.get(), Middle.offset(5, 0, 2), true);
        Pig Bait = spawn(Helper, EntityType.PIG, Middle.offset(-4, 0, 0), true);
        Helper.assertFalse(Reptile.canAttack(Blob), "SCP-682 may attack SCP-999");
        Helper.runAfterDelay(25, () -> {
            Helper.assertTrue(Reptile.isCalm(), "SCP-682 is not calm beside SCP-999");
            Reptile.setNoAi(false);
        });
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Reptile.isCalm(), "SCP-682 is not calm beside SCP-999");
            Helper.assertTrue(Reptile.getTarget() == null && Bait.isAlive() && Blob.isAlive(), "calm SCP-682 still hunts");
            Helper.assertFalse(Reptile.canAttack(Bait), "calm SCP-682 may attack");
            sweep(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Pool, timeoutTicks = 300, batch = "scp682_swim")
    public static void swim(GameTestHelper Helper) {
        sweep(Helper);
        Scp682 Reptile = reptile(Helper, new BlockPos(5, 2, 12), false);
        Reptile.setSize(Scp682Size.Reduced);
        Vec3 Start = Reptile.position();
        Vec3 Goal = Helper.absoluteVec(new Vec3(19.0, 2.0, 12.0));
        Helper.assertTrue(Reptile.getNavigation() instanceof AmphibiousPathNavigation, "SCP-682 cannot path through water");
        Helper.runAfterDelay(2, () -> Reptile.getNavigation().moveTo(Goal.x, Goal.y, Goal.z, Scp682.HuntModifier));
        Helper.runAfterDelay(80, () -> Reptile.getNavigation().moveTo(Goal.x, Goal.y, Goal.z, Scp682.HuntModifier));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Reptile.isInWater(), "SCP-682 left the water");
            Helper.assertTrue(Reptile.position().distanceTo(Start) > 6.0, "SCP-682 did not swim across the pool");
            Helper.assertTrue(Reptile.getAirSupply() == Reptile.getMaxAirSupply(), "SCP-682 is running out of air");
            Helper.assertTrue(Reptile.isAlive() && Reptile.getHealth() >= Reptile.getMaxHealth() - 0.01f, "SCP-682 took damage in water");
            sweep(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp682_rivals")
    public static void rivals(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        List<Mob> Hunters = new ArrayList<>();
        int Slot = 0;
        for (EntityType<?> Type : BuiltInRegistries.ENTITY_TYPE) {
            if (!Type.is(Scp682Registry.Rivals)) {
                continue;
            }
            int Column = Slot % 5;
            int Row = Slot / 5;
            Slot++;
            Entity Made = Type.create(World);
            if (!(Made instanceof Mob Body)) {
                Helper.fail(BuiltInRegistries.ENTITY_TYPE.getKey(Type) + " is not a mob");
                return;
            }
            Vec3 Where = Helper.absoluteVec(Vec3.atBottomCenterOf(Middle.offset(-12 + Column * 6, 0, -12 + (Row % 2) * 24 + (Row / 2) * 3)));
            Body.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
            Body.setPersistenceRequired();
            World.addFreshEntity(Body);
            Hunters.add(Body);
        }
        Helper.assertTrue(!Hunters.isEmpty(), "no rivals are tagged");
        for (EntityType<?> Type : BuiltInRegistries.ENTITY_TYPE) {
            if (!Type.is(Scp682Registry.Wary)) {
                continue;
            }
            Entity Made = spawn(Helper, Type, Middle.offset(0, 0, 10), true);
            Helper.assertTrue(!(Made instanceof PathfinderMob Prey) || Prey.goalSelector.getAvailableGoals().stream().anyMatch(Each -> Each.getGoal() instanceof Scp682Rivals.Wariness),
                    BuiltInRegistries.ENTITY_TYPE.getKey(Type) + " does not shy away from SCP-682");
            Made.discard();
        }
        Set<Mob> Seen = new HashSet<>();
        Helper.succeedWhen(() -> {
            List<String> Idle = new ArrayList<>();
            for (Mob Hunter : Hunters) {
                if (Hunter.getTarget() == Reptile) {
                    Seen.add(Hunter);
                }
                if (!Seen.contains(Hunter)) {
                    Idle.add(BuiltInRegistries.ENTITY_TYPE.getKey(Hunter.getType()).getPath() + (Hunter.isAlive() ? "" : " (gone)"));
                }
            }
            Helper.assertTrue(Idle.isEmpty(), "rivals not attacking SCP-682: " + Idle);
            sweep(Helper);
        });
    }

    @GameTest(template = Pool, timeoutTicks = 100, batch = "scp682_arrival")
    public static void arrival(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        FakePlayer Reader = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp682_reader"));
        Optional<BlockPos> Found = Scp682Lake.find(World, Helper.absolutePos(PoolMiddle), 16);
        Helper.assertTrue(Found.isPresent(), "no deep water was found in the pool");
        Helper.assertTrue(Scp682Lake.water(World, Found.get()), "the arrival spot is not underwater");
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp682Registry.DocumentId);
        Helper.assertTrue(Document != null, "the SCP-682 document is missing");
        boolean Placed = Scp682Lake.manifest(new AnomalySpawners.Discovery(World, Found.get(), Reader, Scp682Registry.DocumentId, Document, false));
        Helper.assertTrue(Placed, "SCP-682 did not emerge");
        List<Scp682> Arrived = World.getEntitiesOfClass(Scp682.class, area(Helper));
        Helper.assertTrue(Arrived.size() == 1, "SCP-682 did not arrive");
        Helper.runAfterDelay(3, () -> {
            Helper.assertTrue(Arrived.get(0).isInWater(), "SCP-682 did not arrive in the water");
            sweep(Helper);
            Helper.succeed();
        });
    }

    private static Scp682 adaptedTo(GameTestHelper Helper, List<DamageSource> Sources) {
        Scp682 Reptile = reptile(Helper, Middle, true);
        for (DamageSource Source : Sources) {
            for (int Index = 0; Index < Scp682Adaptation.HitsToAdapt; Index++) {
                hit(Reptile, Source, 1.0f);
            }
            Helper.assertTrue(Reptile.adaptation().isImmune(kind(Source)), "SCP-682 did not adapt to " + kind(Source));
        }
        return Reptile;
    }

    private static void spam(Scp682 Reptile, DamageSource Source, int Hits) {
        for (int Index = 0; Index < Hits; Index++) {
            hit(Reptile, Source, 1.0f);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_decay_lost")
    public static void decayLost(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        DamageSource Magic = World.damageSources().magic();
        Scp682 Reptile = adaptedTo(Helper, List.of(Magic));
        Scp682Adaptation Held = Reptile.adaptation();
        Helper.assertTrue(Held.remaining(kind(Magic)) == Scp682Adaptation.Lifetime, "a new immunity did not start with a full lifetime");
        Helper.assertTrue(Reptile.fadingHide() == 0, "a new immunity already looks faded");
        Reptile.decayTick(World, Scp682Adaptation.Lifetime - Scp682Adaptation.FadingTicks);
        Helper.assertTrue(Held.isImmune(kind(Magic)) && Reptile.fadingHide() != 0, "an ageing immunity is not shown as fading");
        Helper.assertFalse(hit(Reptile, Magic, 2.0f), "a fading immunity stopped protecting SCP-682");
        Helper.assertTrue(Reptile.fadingHide() == 0, "a refreshed immunity still looks faded");
        Reptile.decayTick(World, Held.remaining(kind(Magic)) - 1);
        Helper.assertTrue(Held.isImmune(kind(Magic)), "the immunity wore off before its lifetime ran out");
        Reptile.decayTick(World, 1);
        Helper.assertFalse(Held.isImmune(kind(Magic)), "the immunity did not wear off after its lifetime");
        Helper.assertTrue(Reptile.hide() == 0 && Reptile.adaptedCount() == 0 && Reptile.fadingHide() == 0, "the lost immunity is still shown on the hide");
        Helper.assertTrue(Held.lossWait() == Scp682Adaptation.LossGap, "losing an immunity did not start the decay window");
        float Before = Reptile.getHealth();
        Helper.assertTrue(hit(Reptile, Magic, 2.0f) && Reptile.getHealth() < Before, "the lost immunity still blocks damage");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_decay_oldest")
    public static void decayOldest(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        List<DamageSource> Kinds = sources(World).subList(0, 3);
        Scp682 Reptile = adaptedTo(Helper, Kinds);
        Scp682Adaptation Held = Reptile.adaptation();
        Reptile.decayTick(World, Scp682Adaptation.Lifetime);
        Helper.assertFalse(Held.isImmune(kind(Kinds.get(0))), "the oldest immunity was not lost first");
        Helper.assertTrue(Held.isImmune(kind(Kinds.get(1))) && Held.isImmune(kind(Kinds.get(2))), "more than one immunity was lost in one window");
        Helper.assertTrue(Held.count() == 2 && Reptile.adaptedCount() == 2, "the hide does not match the remaining immunities");
        Reptile.decayTick(World, Scp682Adaptation.LossGap - 1);
        Helper.assertTrue(Held.count() == 2, "an immunity was lost before the decay window closed");
        Reptile.decayTick(World, 1);
        Helper.assertFalse(Held.isImmune(kind(Kinds.get(1))), "the next oldest immunity was not lost after the window");
        Helper.assertTrue(Held.isImmune(kind(Kinds.get(2))), "the newest immunity was lost out of order");
        Reptile.decayTick(World, Scp682Adaptation.LossGap);
        Helper.assertTrue(Held.count() == 0, "the last expired immunity was never lost");
        Scp682 Later = adaptedTo(Helper, Kinds.subList(0, 2));
        Later.decayTick(World, Scp682Adaptation.Lifetime / 2);
        spam(Later, Kinds.get(0), Scp682Adaptation.RefreshCap / Scp682Adaptation.RefreshTicks + 4);
        Later.decayTick(World, Later.adaptation().remaining(kind(Kinds.get(1))));
        Helper.assertTrue(Later.adaptation().isImmune(kind(Kinds.get(0))) && !Later.adaptation().isImmune(kind(Kinds.get(1))),
                "an expired newer immunity waited behind a refreshed older one");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_decay_refresh")
    public static void decayRefresh(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        DamageSource Magic = World.damageSources().magic();
        Scp682 Reptile = adaptedTo(Helper, List.of(Magic));
        Scp682Adaptation Held = Reptile.adaptation();
        ResourceLocation Kind = kind(Magic);
        int Life = Scp682Adaptation.Lifetime;
        int Aged = Scp682Adaptation.RefreshCap / 3;
        spam(Reptile, Magic, 5);
        Helper.assertTrue(Held.remaining(Kind) == Life && Held.refreshed(Kind) == 0, "a fresh immunity was pushed past its lifetime");
        Reptile.decayTick(World, Aged);
        Helper.assertTrue(Held.remaining(Kind) == Life - Aged, "the immunity did not age");
        Helper.assertFalse(hit(Reptile, Magic, 2.0f), "an immune hit hurt SCP-682");
        Helper.assertTrue(Held.remaining(Kind) == Life - Aged + Math.min(Scp682Adaptation.RefreshTicks, Aged), "an immune hit did not partly refresh the immunity");
        spam(Reptile, Magic, Aged / Scp682Adaptation.RefreshTicks + 4);
        Helper.assertTrue(Held.remaining(Kind) == Life && Held.refreshed(Kind) == Aged, "refreshing went past the lifetime");
        Reptile.decayTick(World, Scp682Adaptation.RefreshCap);
        spam(Reptile, Magic, Scp682Adaptation.RefreshCap / Scp682Adaptation.RefreshTicks + 4);
        Helper.assertTrue(Held.refreshed(Kind) == Scp682Adaptation.RefreshCap, "refreshing was not capped");
        Helper.assertTrue(Held.remaining(Kind) == Life - Aged, "the refresh cap did not hold, " + Held.remaining(Kind) + " left");
        Reptile.decayTick(World, Scp682Adaptation.RefreshTicks);
        spam(Reptile, Magic, 10);
        Helper.assertTrue(Held.remaining(Kind) == Life - Aged - Scp682Adaptation.RefreshTicks, "a spent immunity kept refreshing");
        Scp682 Marked = reptile(Helper, Middle.offset(0, 0, 6), true);
        for (int Index = 0; Index < Scp682Signature.Cinder.threshold(); Index++) {
            Marked.adaptation().expose(Scp682Signature.Cinder);
        }
        Marked.syncAdaptation();
        Helper.assertTrue(Marked.adaptation().holds(Scp682Signature.Cinder), "SCP-682 did not take on the cinder mark");
        Marked.decayTick(World, Aged);
        Helper.assertFalse(hit(Marked, World.damageSources().inFire(), 2.0f), "fire hurt the cinder mark");
        Helper.assertTrue(Marked.adaptation().remaining(Scp682Signature.Cinder.id()) == Life - Aged + Scp682Adaptation.RefreshTicks, "a covered hit did not refresh the mark");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_decay_saved")
    public static void decaySaved(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        List<DamageSource> Kinds = sources(World).subList(0, 3);
        Scp682 Reptile = adaptedTo(Helper, Kinds);
        Reptile.decayTick(World, Scp682Adaptation.Lifetime - Scp682Adaptation.FadingTicks);
        spam(Reptile, Kinds.get(1), 3);
        Reptile.decayTick(World, Scp682Adaptation.FadingTicks);
        Scp682Adaptation Held = Reptile.adaptation();
        Helper.assertTrue(Held.count() == 2 && Held.lossWait() > 0 && Held.refreshed(kind(Kinds.get(1))) > 0,
                "the setup did not leave two ageing immunities and an open window");
        CompoundTag Saved = new CompoundTag();
        Reptile.saveWithoutId(Saved);
        Scp682 Copy = Scp682Registry.Scp682Type.get().create(World);
        Copy.load(Saved);
        Scp682Adaptation Loaded = Copy.adaptation();
        Helper.assertTrue(Loaded.immunities().equals(Held.immunities()), "the immunity order changed after loading");
        for (ResourceLocation Each : Held.immunities()) {
            Helper.assertTrue(Loaded.remaining(Each) == Held.remaining(Each), "the remaining time of " + Each + " changed after loading");
            Helper.assertTrue(Loaded.refreshed(Each) == Held.refreshed(Each), "the refresh spent on " + Each + " changed after loading");
        }
        Helper.assertTrue(Loaded.lossWait() == Held.lossWait(), "the decay window changed after loading");
        Helper.assertTrue(Copy.fadingHide() == Reptile.fadingHide() && Copy.fadingHide() != 0, "the fading hide was not restored after loading");
        CompoundTag Old = Held.save();
        ListTag Entries = Old.getList("Immune", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Entries.size(); Index++) {
            Entries.getCompound(Index).remove("Remaining");
            Entries.getCompound(Index).remove("Refreshed");
        }
        Old.remove("LossWait");
        Scp682Adaptation Legacy = new Scp682Adaptation();
        Legacy.load(Old);
        for (ResourceLocation Each : Held.immunities()) {
            Helper.assertTrue(Legacy.remaining(Each) == Scp682Adaptation.Lifetime && Legacy.refreshed(Each) == 0, "an older save did not start a fresh lifetime");
        }
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp682_decay_stasis")
    public static void decayStasis(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        DamageSource Magic = World.damageSources().magic();
        ResourceLocation Kind = kind(Magic);
        Scp682 Podded = adaptedTo(Helper, List.of(Magic));
        Podded.setSize(Scp682Size.Shrunk);
        Scp682 Shelved = adaptedTo(Helper, List.of(Magic));
        Scp682 Awake = adaptedTo(Helper, List.of(Magic));
        int Start = Podded.adaptation().remaining(Kind);
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp682_decay"));
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Podded), "a stasis pod failed to capture shrunk SCP-682");
        CompoundTag Shelf = new CompoundTag();
        Shelved.saveWithoutId(Shelf);
        Shelved.discard();
        Helper.runAfterDelay(120, () -> {
            Helper.assertTrue(Awake.adaptation().remaining(Kind) <= Start - 100, "decay did not tick while SCP-682 was loaded");
            StasisContents Held = StasisPodItem.contents(Pod);
            Helper.assertTrue(Held != null, "the pod lost SCP-682");
            Optional<Entity> Made = StasisPods.create(World, Held);
            Helper.assertTrue(Made.isPresent() && Made.get() instanceof Scp682, "SCP-682 could not be rebuilt from the pod");
            Helper.assertTrue(((Scp682) Made.get()).adaptation().remaining(Kind) == Start, "decay ticked while SCP-682 was in stasis");
            Scp682 Unloaded = Scp682Registry.Scp682Type.get().create(World);
            Unloaded.load(Shelf);
            Helper.assertTrue(Unloaded.adaptation().remaining(Kind) == Start, "decay ticked while SCP-682 was unloaded");
            sweep(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp682_decay_rivals")
    public static void decayRivals(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp682 Reptile = reptile(Helper, Middle, true);
        Reptile.adaptation().adapt(Scp682Adaptation.Snap, Scp682Adaptation.Category.Anomalous);
        Reptile.adapted(World, Scp682Adaptation.Snap);
        Helper.assertTrue(Reptile.eyes() == Scp682.EyeMany, "SCP-682 did not grow eyes for SCP-173");
        for (int Index = 0; Index < Scp682Signature.Cinder.threshold(); Index++) {
            Reptile.adaptation().expose(Scp682Signature.Cinder);
        }
        Reptile.syncAdaptation();
        Helper.assertTrue(Reptile.holds(Scp682Signature.Cinder) && Reptile.fireImmune(), "SCP-682 did not take on the cinder mark");
        Reptile.decayTick(World, Scp682Adaptation.Lifetime);
        Helper.assertFalse(Reptile.adaptation().isImmune(Scp682Adaptation.Snap), "the SCP-173 adaptation did not wear off");
        Helper.assertTrue(Reptile.eyes() == Scp682.EyeNormal, "the extra eyes did not recede");
        Helper.assertTrue(Reptile.holds(Scp682Signature.Cinder), "the cinder mark went in the same window");
        Reptile.decayTick(World, Scp682Adaptation.LossGap);
        Helper.assertFalse(Reptile.holds(Scp682Signature.Cinder) || Reptile.fireImmune(), "the cinder mark did not wear off");
        Helper.assertTrue(Reptile.signatures() == 0 && Reptile.adaptedCount() == 0, "a lost mark is still shown on the hide");
        sweep(Helper);
        Helper.succeed();
    }
}
