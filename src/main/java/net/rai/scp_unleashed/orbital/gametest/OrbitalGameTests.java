package net.rai.scp_unleashed.orbital.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.orbital.OrbitalArray;
import net.rai.scp_unleashed.orbital.OrbitalFiring;
import net.rai.scp_unleashed.orbital.OrbitalLaunch;
import net.rai.scp_unleashed.orbital.OrbitalLink;
import net.rai.scp_unleashed.orbital.OrbitalPart;
import net.rai.scp_unleashed.orbital.OrbitalRegistry;
import net.rai.scp_unleashed.orbital.OrbitalStrike;
import net.rai.scp_unleashed.orbital.OrbitalStrikeEntity;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteBoosterEntity;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class OrbitalGameTests {
    public static final String Arena = "orbital_arena";
    public static final int Floor = 4;
    public static final int Middle = 13;
    public static final int Charge = 20;
    public static final OrbitalStrike.Settings Plain = new OrbitalStrike.Settings(3000.0F, 4.5F, 0.5, 10.0, 40.0F, false, 5);
    public static final OrbitalStrike.Settings Carving = new OrbitalStrike.Settings(3000.0F, 4.5F, 0.5, 10.0, 40.0F, true, 5);

    private OrbitalGameTests() {
    }

    private static FakePlayer person(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0F, 0.0F);
        World.addNewPlayer(User);
        GatewayGameTests.whenDone(Helper, () -> {
            OrbitalArray.get(World.getServer()).clear(User.getUUID());
            if (!User.isRemoved()) {
                World.removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
            }
        });
        return User;
    }

    private static void grantAll(MinecraftServer Server, UUID Owner) {
        OrbitalArray Array = OrbitalArray.get(Server);
        for (OrbitalPart Part : OrbitalPart.values()) {
            Array.record(Owner, Part, 0L);
        }
    }

    private static ItemStack linkedTo(Player Owner) {
        ItemStack Stack = new ItemStack(OrbitalRegistry.Designator.get());
        Stack.set(OrbitalRegistry.Link.get(), new OrbitalLink(Owner.getUUID(), Owner.getGameProfile().getName()));
        return Stack;
    }

    private static Vec3 centre(GameTestHelper Helper) {
        return Helper.absoluteVec(new Vec3(Middle + 0.5, Floor, Middle + 0.5));
    }

    private static OrbitalStrikeEntity strike(GameTestHelper Helper, OrbitalStrike.Settings Values) {
        ServerLevel World = Helper.getLevel();
        OrbitalStrikeEntity Strike = OrbitalStrikeEntity.create(World, centre(Helper), null, Values, Charge);
        World.addFreshEntity(Strike);
        return Strike;
    }

    private static Mob still(GameTestHelper Helper, EntityType<? extends Mob> Type, double X, double Z) {
        Mob Made = Helper.spawnWithNoFreeWill(Type, new Vec3(X, Floor, Z));
        Made.setPersistenceRequired();
        return Made;
    }

    private static void griefing(GameTestHelper Helper, boolean Allowed) {
        MinecraftServer Server = Helper.getLevel().getServer();
        GameRules.BooleanValue Rule = Server.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean Before = Rule.get();
        Rule.set(Allowed, Server);
        GatewayGameTests.whenDone(Helper, () -> Rule.set(Before, Server));
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "orbital_launch")
    public static void launchRecordsForLauncherOnly(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = Helper.getLevel();
            MinecraftServer Server = World.getServer();
            FakePlayer First = person(Helper, 3.5, 3.5, "orbital_first");
            FakePlayer Second = person(Helper, 6.5, 3.5, "orbital_second");
            OrbitalArray Array = OrbitalArray.get(Server);
            Vec3 Pad = centre(Helper);
            for (OrbitalPart Part : OrbitalPart.values()) {
                ItemStack Payload = new ItemStack(OrbitalRegistry.part(Part));
                Helper.assertTrue(OrbitalLaunch.mayLaunch(World, First, Payload, Pad), "the first launch of " + Part + " was refused");
                SatelliteBoosterEntity Booster = SatelliteBoosterEntity.launching(World, Pad, 0.0F, First.getUUID(), null).withPayload(OrbitalLaunch.payloadId(Payload));
                Booster.deploy(World);
                Helper.assertTrue(Array.has(First.getUUID(), Part), Part + " was not recorded for its launcher");
                Helper.assertTrue(!Array.has(Second.getUUID(), Part), Part + " was recorded for a player who did not launch it");
                Helper.assertTrue(!OrbitalLaunch.mayLaunch(World, First, Payload, Pad), "a second " + Part + " for the same player was not refused");
                Helper.assertTrue(OrbitalLaunch.mayLaunch(World, Second, Payload, Pad), "another player's " + Part + " was refused");
            }
            Helper.assertTrue(Array.complete(First.getUUID()), "six launches did not complete the array");
            Helper.assertTrue(Array.count(Second.getUUID()) == 0 && !Array.complete(Second.getUUID()), "the other player has parts they never launched");
            int Before = SatelliteNetwork.get(Server).count();
            SatelliteBoosterEntity Plain = SatelliteBoosterEntity.launching(World, Pad, 0.0F, First.getUUID(), "orbital_test_plain")
                    .withPayload(OrbitalLaunch.payloadId(new ItemStack(RocketryRegistry.Satellite.get())));
            Plain.deploy(World);
            Helper.assertTrue(SatelliteNetwork.get(Server).count() == Before + 1, "a plain satellite no longer joins the satellite network");
            SatelliteNetwork.get(Server).removeNamed("orbital_test_plain");
            SatelliteBoosterEntity Legacy = SatelliteBoosterEntity.launching(World, Pad, 0.0F, First.getUUID(), "orbital_test_legacy");
            Legacy.deploy(World);
            Helper.assertTrue(SatelliteNetwork.get(Server).count() == Before + 1, "a booster without a payload id did not deploy a satellite");
            SatelliteNetwork.get(Server).removeNamed("orbital_test_legacy");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "orbital_trigger")
    public static void designatorRefusesStrangersAndIncompleteArrays(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            MinecraftServer Server = Helper.getLevel().getServer();
            FakePlayer Owner = person(Helper, 3.5, 3.5, "orbital_owner");
            FakePlayer Stranger = person(Helper, 6.5, 3.5, "orbital_stranger");
            BlockPos Marked = Helper.absolutePos(new BlockPos(Middle, Floor - 1, Middle));
            ItemStack Fresh = new ItemStack(OrbitalRegistry.Designator.get());
            OrbitalFiring.Outcome Unready = OrbitalFiring.use(Stranger, Fresh, Marked);
            Helper.assertTrue(Unready == OrbitalFiring.Outcome.NoArray, "an unlinked designator in empty hands gave " + Unready);
            Helper.assertTrue(Fresh.get(OrbitalRegistry.Link.get()) == null, "the designator linked to a player without an array");
            OrbitalArray Array = OrbitalArray.get(Server);
            for (OrbitalPart Part : OrbitalPart.values()) {
                if (Part != OrbitalPart.EmitterBarrel) {
                    Array.record(Stranger.getUUID(), Part, 0L);
                }
            }
            OrbitalFiring.Outcome Partial = OrbitalFiring.use(Stranger, Fresh, Marked);
            Helper.assertTrue(Partial == OrbitalFiring.Outcome.NoArray, "five parts linked a designator: " + Partial);
            grantAll(Server, Owner.getUUID());
            ItemStack Owned = new ItemStack(OrbitalRegistry.Designator.get());
            OrbitalFiring.Outcome Linked = OrbitalFiring.use(Owner, Owned, Marked);
            Helper.assertTrue(Linked == OrbitalFiring.Outcome.Linked, "a complete array did not link the designator: " + Linked);
            OrbitalLink Link = Owned.get(OrbitalRegistry.Link.get());
            Helper.assertTrue(Link != null && Link.ownedBy(Owner.getUUID()), "the designator linked to the wrong player");
            OrbitalFiring.Outcome Foreign = OrbitalFiring.use(Stranger, Owned, Marked);
            Helper.assertTrue(Foreign == OrbitalFiring.Outcome.Foreign, "a stranger used the owner's designator: " + Foreign);
            Array.record(Stranger.getUUID(), OrbitalPart.EmitterBarrel, 0L);
            OrbitalFiring.Outcome StillForeign = OrbitalFiring.use(Stranger, Owned, Marked);
            Helper.assertTrue(StillForeign == OrbitalFiring.Outcome.Foreign, "a stranger with their own array used the owner's designator: " + StillForeign);
            Array.clear(Owner.getUUID());
            OrbitalFiring.Outcome Broken = OrbitalFiring.use(Owner, Owned, Marked);
            Helper.assertTrue(Broken == OrbitalFiring.Outcome.Incomplete, "a linked designator fired without a complete array: " + Broken);
            Helper.assertTrue(Helper.getLevel().getEntitiesOfClass(OrbitalStrikeEntity.class, Helper.getBounds().inflate(4.0)).isEmpty(), "a refused use still called a strike");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "orbital_damage")
    public static void strikeLandsAfterWarningWithFalloff(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Map<UUID, Float> Taken = new HashMap<>();
            OrbitalStrike.observe((Target, Amount) -> Taken.merge(Target.getUUID(), Amount, Float::sum));
            GatewayGameTests.whenDone(Helper, () -> OrbitalStrike.observe(null));
            Mob Centre = still(Helper, EntityType.ZOMBIE, Middle + 0.5, Middle + 0.5);
            Mob Edge = still(Helper, EntityType.ZOMBIE, Middle + 4.5, Middle + 0.5);
            Mob Splash = still(Helper, EntityType.IRON_GOLEM, Middle + 0.5, Middle + 8.5);
            Mob Far = still(Helper, EntityType.ZOMBIE, 1.5, 1.5);
            OrbitalStrikeEntity Strike = strike(Helper, Plain);
            float Expected = 3000.0F;
            float EdgeDistance = (float) Math.max(0.0, 4.0 - Edge.getBbWidth() / 2.0);
            float EdgeExpected = OrbitalStrike.beamDamage(Plain, EdgeDistance);
            Helper.runAtTickTime(Charge - 2, () -> {
                Helper.assertTrue(Centre.isAlive() && Taken.isEmpty(), "the beam landed before its warning finished");
                Helper.assertTrue(!Strike.hasStruck(), "the strike reports landing early");
            });
            Helper.runAtTickTime(Charge + 4, () -> {
                Float AtCentre = Taken.get(Centre.getUUID());
                Float AtEdge = Taken.get(Edge.getUUID());
                Float AtSplash = Taken.get(Splash.getUUID());
                Helper.assertTrue(AtCentre != null && Math.abs(AtCentre - Expected) < 1.0F, "the centre took " + AtCentre + " instead of " + Expected);
                Helper.assertTrue(AtEdge != null && AtEdge < AtCentre && Math.abs(AtEdge - EdgeExpected) < 1.0F && AtEdge >= Expected * 0.5F,
                        "the edge took " + AtEdge + ", expected about " + EdgeExpected);
                Helper.assertTrue(AtSplash != null && AtSplash > 0.0F && AtSplash < Plain.SplashDamage(), "the splash target took " + AtSplash);
                Helper.assertTrue(!Taken.containsKey(Far.getUUID()), "a zombie far outside the blast was hurt");
                Helper.assertTrue(!Centre.isAlive() && !Edge.isAlive(), "zombies in the beam survived");
                Helper.assertTrue(Splash.isAlive(), "the iron golem at the blast edge died");
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "orbital_no_griefing")
    public static void noCraterWithoutMobGriefing(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            griefing(Helper, false);
            BlockPos Ground = new BlockPos(Middle, Floor - 1, Middle);
            strike(Helper, Carving);
            Helper.runAtTickTime(Charge + 3, () -> {
                Helper.assertTrue(!Helper.getBlockState(Ground).isAir(), "the beam carved a crater with mobGriefing off");
                Helper.assertTrue(!Helper.getBlockState(Ground.below()).isAir(), "the beam dug under the centre with mobGriefing off");
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "orbital_crater")
    public static void craterSparesUnbreakableBlocks(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            griefing(Helper, true);
            BlockPos Ground = new BlockPos(Middle, Floor - 1, Middle);
            BlockPos Bedrock = new BlockPos(Middle + 1, Floor - 1, Middle);
            BlockPos Shell = new BlockPos(Middle - 1, Floor - 1, Middle);
            BlockPos Chest = new BlockPos(Middle, Floor, Middle + 2);
            BlockPos Obsidian = new BlockPos(Middle, Floor - 2, Middle + 1);
            Block ShellBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp5001_shell"));
            Helper.setBlock(Bedrock, Blocks.BEDROCK);
            Helper.setBlock(Shell, ShellBlock);
            Helper.setBlock(Chest, Blocks.CHEST);
            Helper.setBlock(Obsidian, Blocks.OBSIDIAN);
            BlockState ShellState = Helper.getBlockState(Shell);
            strike(Helper, Carving);
            Helper.runAtTickTime(Charge + 3, () -> {
                Helper.assertTrue(Helper.getBlockState(Ground).isAir() || Helper.getBlockState(Ground).is(Blocks.FIRE), "the beam left the ground under it intact");
                Helper.assertTrue(Helper.getBlockState(Bedrock).is(Blocks.BEDROCK), "the beam broke bedrock");
                Helper.assertTrue(Helper.getBlockState(Shell).equals(ShellState), "the beam broke an SCP-5001 shell");
                Helper.assertTrue(Helper.getBlockState(Chest).is(Blocks.CHEST), "the beam destroyed a chest");
                Helper.assertTrue(Helper.getBlockState(Obsidian).is(Blocks.OBSIDIAN), "the beam broke obsidian");
                Helper.assertTrue(!Helper.getBlockState(new BlockPos(1, Floor - 1, 1)).isAir(), "the crater reached the arena corner");
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "orbital_cooldown")
    public static void cooldownBlocksRefire(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            griefing(Helper, false);
            MinecraftServer Server = Helper.getLevel().getServer();
            FakePlayer Owner = person(Helper, 2.5, 2.5, "orbital_gunner");
            grantAll(Server, Owner.getUUID());
            ItemStack Stack = linkedTo(Owner);
            BlockPos Marked = Helper.absolutePos(new BlockPos(Middle, Floor - 1, Middle));
            OrbitalFiring.Outcome First = OrbitalFiring.use(Owner, Stack, Marked);
            Helper.assertTrue(First == OrbitalFiring.Outcome.Fired, "a ready array did not fire: " + First);
            Helper.assertTrue(Stack.get(OrbitalRegistry.Recharge.get()) != null, "the designator does not show the recharge");
            OrbitalFiring.Outcome Second = OrbitalFiring.use(Owner, Stack, Marked);
            Helper.assertTrue(Second == OrbitalFiring.Outcome.Recharging, "the array fired again straight away: " + Second);
            Helper.assertTrue(OrbitalArray.get(Server).readyAt(Owner.getUUID()) > Server.overworld().getGameTime(), "the array has no recharge time");
            Helper.assertTrue(Helper.getLevel().getEntitiesOfClass(OrbitalStrikeEntity.class, Helper.getBounds().inflate(4.0)).size() == 1, "the refused shot still called a strike");
            OrbitalArray.get(Server).recharge(Owner.getUUID());
            ServerLevel World = Helper.getLevel();
            int Top = World.getHeight(Heightmap.Types.MOTION_BLOCKING, Marked.getX(), Marked.getZ());
            Helper.assertTrue(OrbitalFiring.impact(World, Marked).map(Found -> Found.getY() == Math.max(Top, Marked.getY() + 1)).orElse(false),
                    "the impact point is not the highest surface over the mark: marked " + Marked.getY() + ", surface " + Top + ", impact " + OrbitalFiring.impact(World, Marked));
            Helper.assertTrue(OrbitalFiring.impact(World, Marked.below(40)).isEmpty(), "a mark buried 40 blocks deep was accepted");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "orbital_save")
    public static void saveAndLoadKeepTheArray(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = Helper.getLevel();
            HolderLookup.Provider Registries = World.registryAccess();
            OrbitalArray Original = new OrbitalArray();
            UUID Full = UUID.randomUUID();
            UUID Half = UUID.randomUUID();
            for (OrbitalPart Part : OrbitalPart.values()) {
                Original.record(Full, Part, 100L + Part.ordinal());
            }
            Original.record(Half, OrbitalPart.PowerCore, 5L);
            Original.record(Half, OrbitalPart.HeatSink, 6L);
            Original.fired(Full, 123456L);
            CompoundTag Saved = Original.save(new CompoundTag(), Registries);
            OrbitalArray Loaded = OrbitalArray.load(Saved, Registries);
            Helper.assertTrue(Loaded.complete(Full) && Loaded.count(Full) == OrbitalPart.Count, "the full array did not survive a save");
            Helper.assertTrue(Loaded.count(Half) == 2 && Loaded.has(Half, OrbitalPart.PowerCore) && Loaded.has(Half, OrbitalPart.HeatSink), "the partial array did not survive a save");
            Helper.assertTrue(Loaded.readyAt(Full) == 123456L && Loaded.shots(Full) == 1, "the recharge did not survive a save");
            Helper.assertTrue(Loaded.all().get(Full).launchedAt(OrbitalPart.EmitterBarrel) == 100L + OrbitalPart.EmitterBarrel.ordinal(), "launch times did not survive a save");
            OrbitalArray Empty = OrbitalArray.load(new CompoundTag(), Registries);
            Helper.assertTrue(Empty.all().isEmpty(), "an empty save produced owners");
            SatelliteBoosterEntity Booster = SatelliteBoosterEntity.launching(World, centre(Helper), 0.0F, Full, null)
                    .withPayload(OrbitalLaunch.payloadId(new ItemStack(OrbitalRegistry.part(OrbitalPart.FocusingArray))));
            CompoundTag Flight = new CompoundTag();
            Booster.saveWithoutId(Flight);
            SatelliteBoosterEntity Reloaded = new SatelliteBoosterEntity(RocketryRegistry.SatelliteBoosterType.get(), World);
            Reloaded.load(Flight);
            Helper.assertTrue(Booster.getPayload() != null && Booster.getPayload().equals(Reloaded.getPayload()), "a booster in flight lost its payload on save");
            Helper.succeed();
        });
    }
}
