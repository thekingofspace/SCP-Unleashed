package net.rai.scp_unleashed.raid.hostile.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.door.DoorKind;
import net.rai.scp_unleashed.door.DoorRegistry;
import net.rai.scp_unleashed.door.KeycardItem;
import net.rai.scp_unleashed.door.KeycardScannerBlock;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionWorld;
import net.rai.scp_unleashed.guards.GuardDuty;
import net.rai.scp_unleashed.guards.GuardRules;
import net.rai.scp_unleashed.guards.GuardThreats;
import net.rai.scp_unleashed.guards.PatrolPoint;
import net.rai.scp_unleashed.guards.PatrolRoute;
import net.rai.scp_unleashed.mtf.MtfFriends;
import net.rai.scp_unleashed.mtf.MtfLandingZone;
import net.rai.scp_unleashed.mtf.MtfOperative;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidMark;
import net.rai.scp_unleashed.raid.RaidOutcome;
import net.rai.scp_unleashed.raid.base.RaidBases;
import net.rai.scp_unleashed.raid.RaidStage;
import net.rai.scp_unleashed.raid.RaidWorld;
import net.rai.scp_unleashed.raid.chaos.ChaosArsenal;
import net.rai.scp_unleashed.raid.chaos.ChaosFaction;
import net.rai.scp_unleashed.raid.chaos.ChaosHelicopter;
import net.rai.scp_unleashed.raid.chaos.ChaosInsurgent;
import net.rai.scp_unleashed.raid.chaos.ChaosRaidStyle;
import net.rai.scp_unleashed.archive.investigation.Investigations;
import net.rai.scp_unleashed.raid.hostile.HostileFaction;
import net.rai.scp_unleashed.raid.hostile.HostileGate;
import net.rai.scp_unleashed.raid.hostile.HostileHauls;
import net.rai.scp_unleashed.raid.hostile.HostileLedger;
import net.rai.scp_unleashed.raid.hostile.HostileLevels;
import net.rai.scp_unleashed.raid.hostile.HostileRaidStyle;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import net.rai.scp_unleashed.raid.hostile.HostileTargets;
import net.rai.scp_unleashed.raid.hostile.HostileTrophy;
import net.rai.scp_unleashed.raid.serpentshand.SerpentCaptureGoal;
import net.rai.scp_unleashed.raid.serpentshand.SerpentPodItem;
import net.rai.scp_unleashed.raid.serpentshand.SerpentPods;
import net.rai.scp_unleashed.raid.serpentshand.SerpentSeal;
import net.rai.scp_unleashed.raid.serpentshand.SerpentsHandAgent;
import net.rai.scp_unleashed.raid.serpentshand.SerpentsHandFaction;
import net.rai.scp_unleashed.raid.serpentshand.SerpentsHandRaidStyle;
import net.rai.scp_unleashed.scp005.Scp005Registry;
import net.rai.scp_unleashed.scp127.Scp127Item;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import net.rai.scp_unleashed.scp131.Scp131Registry;
import net.rai.scp_unleashed.security.TurretCategory;
import net.rai.scp_unleashed.security.TurretTargeting;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageRegistry;
import net.rai.scp_unleashed.z0003.Z0003Device;
import net.rai.scp_unleashed.z0003.Z0003DeviceItem;
import net.rai.scp_unleashed.z0003.Z0003Petrify;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.imageio.ImageIO;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class HostileRaidGameTests {
    public static final String Arena = "faction_raids_arena";
    public static final String DoorArena = "guards_arena";
    public static final int WallZ = 12;
    public static final int DoorX = 6;
    public static final int StartZ = 5;
    public static final int GoalZ = 18;

    private HostileRaidGameTests() {
    }

    private static int floor(GameTestHelper Helper) {
        for (int Y = 0; Y < 5; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static int doorFloor(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(23, Y, 23)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(23, Y + 1, 23)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static RaidContext open(GameTestHelper Helper, HostileFaction Faction, BlockPos TargetRel, BlockPos ExitRel, int Level) {
        RaidContext Context = staged(Helper, Faction, TargetRel, 4.0, 0.0);
        long Now = Context.Now();
        Raid Current = Context.Current();
        Current.extra().putLong(RaidDirector.ExitKey, Helper.absolutePos(ExitRel).asLong());
        Current.extra().putInt(HostileRaidStyle.LevelKey, Level);
        Current.setStage(RaidStage.Active, Now);
        Current.setDeadline(Now + 24000L);
        return Context;
    }

    private static RaidContext staged(GameTestHelper Helper, HostileFaction Faction, BlockPos TargetRel, double Footprint, double Heading) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState State = FactionWorld.state(Server, Faction);
        RaidWorld Data = RaidWorld.get(Server);
        BlockPos Target = Helper.absolutePos(TargetRel);
        long Now = Server.overworld().getGameTime();
        Raid Current = new Raid(Data.nextId(), Faction.id(), State.era(), World.dimension(), "gametest", Target, 4.0, Footprint, 1.0, Heading, true, Now, Target);
        Data.add(Current);
        return new RaidContext(Server, World, Current, Faction, State, Faction.hostileStyle(), Data.ledger(Faction.id(), State.era()), Now);
    }

    private static void beginSoon(GameTestHelper Helper, RaidContext Context) {
        Raid Current = Context.Current();
        Current.setStage(RaidStage.Warning, Context.Now() - Context.Style().warningTicks() - 1L);
    }

    private static RaidContext fresh(GameTestHelper Helper, RaidContext Old) {
        MinecraftServer Server = Helper.getLevel().getServer();
        RaidContext Found = RaidDirector.context(Server, Old.Current(), Server.overworld().getGameTime());
        return Found != null ? Found : Old;
    }

    private static void close(GameTestHelper Helper, RaidContext Context) {
        Raid Current = Context.Current();
        HostileHauls.clear(Current);
        if (Current.live()) {
            RaidDirector.end(fresh(Helper, Context), RaidOutcome.Stopped);
        }
        RaidWorld.get(Helper.getLevel().getServer()).remove(Current);
    }

    private static <T extends HostileRaider> T raider(GameTestHelper Helper, EntityType<T> Type, RaidContext Context, double X, int Y, double Z, int Level, byte Role) {
        ServerLevel World = Helper.getLevel();
        T Made = Type.create(World);
        if (Made == null) {
            throw new IllegalStateException("could not create " + Type);
        }
        Made.configure(Level, Role, 0, World.random);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        if (Context != null) {
            Context.enlist(Made, RaidMark.Raider);
        }
        World.addFreshEntity(Made);
        return Made;
    }

    private static Scp131Pod specimen(GameTestHelper Helper, double X, int Y, double Z, String Name) {
        Scp131Pod Pod = Helper.spawnWithNoFreeWill(Scp131Registry.Pod.get(), new Vec3(X, Y, Z));
        Pod.setCustomName(Component.literal(Name));
        return Pod;
    }

    private static CompoundTag backup(FactionState State) {
        CompoundTag Saved = new CompoundTag();
        if (State.extra().contains(HostileLedger.LedgerKey)) {
            Saved.put(HostileLedger.LedgerKey, State.extra().get(HostileLedger.LedgerKey).copy());
        }
        Saved.putInt(HostileLedger.NextKey, State.extra().getInt(HostileLedger.NextKey));
        Saved.putDouble("Xp", State.power());
        return Saved;
    }

    private static void restore(HostileFaction Faction, FactionState State, CompoundTag Saved) {
        if (Saved.contains(HostileLedger.LedgerKey)) {
            State.extra().put(HostileLedger.LedgerKey, Saved.get(HostileLedger.LedgerKey).copy());
        } else {
            State.extra().remove(HostileLedger.LedgerKey);
        }
        State.extra().putInt(HostileLedger.NextKey, Math.max(State.extra().getInt(HostileLedger.NextKey), Saved.getInt(HostileLedger.NextKey)));
        Faction.setXp(State, Saved.getDouble("Xp"));
        State.changed();
    }

    private static void sweep(GameTestHelper Helper) {
        AABB Area = Helper.getBounds().inflate(12.0);
        for (Entity Found : Helper.getLevel().getEntities((Entity) null, Area, Candidate -> !(Candidate instanceof Player))) {
            if (Found instanceof LivingEntity Living && Z0003Petrify.isStatue(Living)) {
                Z0003Petrify.free(Living);
            }
            Found.discard();
        }
    }

    private static List<ItemEntity> drops(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(ItemEntity.class, Helper.getBounds().inflate(4.0), Found -> Found.isAlive());
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "hostile_unattended")
    public static void unattendedRaidsAreCalledOff(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        BlockPos Target = Helper.absolutePos(new BlockPos(6, 1, 6));
        long Now = Server.overworld().getGameTime();
        String Group = RaidBases.TeamPrefix + "gametest_nobody";
        Raid Planned = new Raid(-1, ChaosFaction.Id, 0, World.dimension(), Group, Target, 4.0, 8.0, 1.0, 0.0, false, Now, Target);
        Raid Ordered = new Raid(-2, ChaosFaction.Id, 0, World.dimension(), Group, Target, 4.0, 8.0, 1.0, 0.0, true, Now, Target);
        Helper.assertFalse(RaidDirector.groupOnline(Server, Group), "a group with nobody in it counted as online");
        Helper.assertTrue(RaidDirector.unattended(Server, Planned), "a raid on a group with nobody online was allowed to go on");
        Helper.assertFalse(RaidDirector.unattended(Server, Ordered), "a raid started by an operator was called off");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "hostile_capture")
    public static void serpentSealsSafeAnomalyInPod(GameTestHelper Helper) {
        int F = floor(Helper);
        RaidContext Context = open(Helper, SerpentsHandFaction.Instance, new BlockPos(16, F, 16), new BlockPos(30, F, 30), 2);
        SerpentsHandAgent Agent = raider(Helper, HostileRegistry.AgentType.get(), Context, 8.5, F, 8.5, 2, HostileRaider.RoleSeeker);
        Scp131Pod Pod = specimen(Helper, 12.5, F, 12.5, "Specimen Alpha");
        Helper.assertTrue(HostileTargets.capturable(Pod), "a Safe class SCP-131 pod is not capturable");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(HostileHauls.has(Context.Current(), Agent.getUUID()), "the agent never sealed the SCP-131 pod"))
                .thenExecute(() -> {
                    Helper.assertTrue(Pod.isRemoved(), "the captured pod is still in the world");
                    HostileTrophy Haul = HostileHauls.of(Context.Current(), Agent.getUUID());
                    Helper.assertTrue(Haul != null && Haul.creature() && Haul.Type().equals(Scp131Registry.Pod.getId()), "the haul is not the SCP-131 pod: " + Haul);
                    Helper.assertTrue(Haul.Data().getString("CustomName").contains("Specimen Alpha"), "the saved entity data lost its name");
                    Helper.assertTrue(SerpentPodItem.filled(Agent.getMainHandItem()), "the agent is not carrying a filled stasis pod");
                    Helper.assertTrue(Agent.hauling(), "the agent does not know it carries a pod");
                })
                .thenExecute(() -> {
                    close(Helper, Context);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "hostile_pod_release")
    public static void carrierDropsPodThatReleasesAnomaly(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        RaidContext Context = open(Helper, SerpentsHandFaction.Instance, new BlockPos(16, F, 16), new BlockPos(30, F, 30), 2);
        SerpentsHandAgent Agent = raider(Helper, HostileRegistry.AgentType.get(), Context, 8.5, F, 8.5, 2, HostileRaider.RoleSeeker);
        Scp131Pod Pod = specimen(Helper, 10.5, F, 8.5, "Specimen Beta");
        Pod.setHealth(Math.max(1.0f, Pod.getMaxHealth() - 3.0f));
        float Health = Pod.getHealth();
        HostileTrophy Trophy = SerpentPods.capture(World, Pod, Agent);
        Helper.assertTrue(Trophy != null, "the pod could not be captured");
        HostileHauls.put(Context.Current(), Agent.getUUID(), Trophy);
        Agent.carry(SerpentPodItem.filled(Trophy, World.registryAccess()));
        Agent.kill();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(drops(Helper).stream().anyMatch(Found -> SerpentPodItem.filled(Found.getItem())), "no filled pod dropped"))
                .thenExecute(() -> {
                    Helper.assertFalse(HostileHauls.has(Context.Current(), Agent.getUUID()), "the haul was not taken off the fallen carrier");
                    ItemEntity Dropped = drops(Helper).stream().filter(Found -> SerpentPodItem.filled(Found.getItem())).findFirst().orElseThrow();
                    Helper.assertTrue(drops(Helper).stream().filter(Found -> SerpentPodItem.filled(Found.getItem())).count() == 1, "more than one pod dropped");
                    ItemStack Stack = Dropped.getItem().copy();
                    Dropped.discard();
                    FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "pod_opener"));
                    User.setItemInHand(InteractionHand.MAIN_HAND, Stack);
                    BlockPos Ground = Helper.absolutePos(new BlockPos(14, F - 1, 14));
                    UseOnContext Opening = new UseOnContext(User, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(Ground).add(0.0, 0.5, 0.0), Direction.UP, Ground, false));
                    InteractionResult Result = Stack.useOn(Opening);
                    Helper.assertTrue(Result.consumesAction(), "releasing the pod failed: " + Result);
                    Helper.assertTrue(Stack.isEmpty(), "the pod was not used up by the release");
                    List<Scp131Pod> Back = World.getEntitiesOfClass(Scp131Pod.class, Helper.getBounds().inflate(4.0),
                            Found -> Found.isAlive() && Found.hasCustomName() && "Specimen Beta".equals(Found.getCustomName().getString()));
                    Helper.assertTrue(Back.size() == 1, "the released pod did not come back with its name, found " + Back.size());
                    Helper.assertTrue(Math.abs(Back.get(0).getHealth() - Health) < 0.01f, "the released pod lost its health: " + Back.get(0).getHealth() + " vs " + Health);
                })
                .thenExecute(() -> {
                    close(Helper, Context);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "hostile_escape")
    public static void escapeFillsLedgerAndLevelsUp(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState Serpents = FactionWorld.state(Server, SerpentsHandFaction.Instance);
        FactionState Chaos = FactionWorld.state(Server, ChaosFaction.Instance);
        CompoundTag SerpentsBefore = backup(Serpents);
        CompoundTag ChaosBefore = backup(Chaos);
        SerpentsHandFaction.Instance.setXp(Serpents, 50.0);
        int Before = HostileLedger.size(Serpents);
        RaidContext Context = open(Helper, SerpentsHandFaction.Instance, new BlockPos(16, F, 16), new BlockPos(30, F, 30), 1);
        SerpentsHandAgent Agent = raider(Helper, HostileRegistry.AgentType.get(), Context, 8.5, F, 8.5, 1, HostileRaider.RoleSeeker);
        Scp131Pod Pod = specimen(Helper, 10.5, F, 8.5, "Specimen Delta");
        HostileTrophy Trophy = SerpentPods.capture(World, Pod, Agent);
        Helper.assertTrue(Trophy != null, "the pod could not be captured");
        HostileHauls.put(Context.Current(), Agent.getUUID(), Trophy);
        Agent.carry(SerpentPodItem.filled(Trophy, World.registryAccess()));
        Helper.assertTrue(HostileFaction.level(Serpents) == 1, "the Serpent's Hand did not start at level 1");
        HostileRaidStyle.departed(Agent);
        Helper.assertTrue(Agent.isRemoved(), "the escaped agent was not removed");
        Helper.assertFalse(Context.Current().raiders().contains(Agent.getUUID()), "the escaped agent is still listed in the raid");
        Helper.assertTrue(HostileLedger.size(Serpents) == Before + 1, "the ledger did not grow");
        HostileLedger.Entry Last = HostileLedger.entries(Serpents).get(HostileLedger.size(Serpents) - 1);
        Helper.assertTrue(Last.Trophy().creature() && Last.Trophy().Type().equals(Scp131Registry.Pod.getId()), "the ledger entry is not the pod");
        Helper.assertTrue(Last.Trophy().Data().getString("CustomName").contains("Specimen Delta"), "the ledger lost the entity data");
        Helper.assertTrue(Last.Level() == 1 && Last.Time() > 0L && Last.Day() >= 1L, "the ledger entry lacks its time or level");
        Helper.assertTrue(Math.abs(HostileFaction.xp(Serpents) - (50.0 + HostileFaction.CaptureXp)) < 0.01, "the capture did not add xp: " + HostileFaction.xp(Serpents));
        Helper.assertTrue(HostileFaction.level(Serpents) == 2, "the Serpent's Hand did not reach level 2 at " + HostileFaction.xp(Serpents));
        ChaosFaction.Instance.setXp(Chaos, 260.0);
        int ChaosLevel = HostileFaction.level(Chaos);
        RaidContext ChaosRaid = open(Helper, ChaosFaction.Instance, new BlockPos(16, F, 16), new BlockPos(30, F, 30), 3);
        ChaosInsurgent Thief = raider(Helper, HostileRegistry.InsurgentType.get(), ChaosRaid, 6.5, F, 6.5, 3, HostileRaider.RoleSeeker);
        HostileTrophy Key = HostileTrophy.of(new ItemStack(Scp005Registry.Key.get()), World.registryAccess());
        HostileHauls.put(ChaosRaid.Current(), Thief.getUUID(), Key);
        HostileRaidStyle.departed(Thief);
        Helper.assertTrue(HostileFaction.level(Chaos) == ChaosLevel + 1, "securing an object did not raise the Chaos level: " + HostileFaction.level(Chaos));
        Helper.assertTrue(HostileLedger.entries(Chaos).stream().anyMatch(One -> !One.Trophy().creature() && One.Trophy().Type().equals(Scp005Registry.Key.getId())),
                "the Chaos ledger does not hold the key");
        close(Helper, Context);
        close(Helper, ChaosRaid);
        restore(SerpentsHandFaction.Instance, Serpents, SerpentsBefore);
        restore(ChaosFaction.Instance, Chaos, ChaosBefore);
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "hostile_ledger_persist")
    public static void ledgerSurvivesSaveAndLoad(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState Serpents = FactionWorld.state(Server, SerpentsHandFaction.Instance);
        FactionState Chaos = FactionWorld.state(Server, ChaosFaction.Instance);
        CompoundTag SerpentsBefore = backup(Serpents);
        CompoundTag ChaosBefore = backup(Chaos);
        Scp131Pod Pod = specimen(Helper, 10.5, F, 8.5, "Specimen Epsilon");
        HostileTrophy Creature = HostileTrophy.of(Pod);
        Helper.assertTrue(Creature != null, "the pod could not be saved");
        ItemStack Gun = Scp127Item.create();
        HostileTrophy Thing = HostileTrophy.of(Gun, World.registryAccess());
        SerpentsHandFaction.Instance.setXp(Serpents, 333.0);
        HostileLedger.Entry First = HostileLedger.add(Serpents, Creature, 1234L, 2L, 3);
        HostileLedger.Entry Second = HostileLedger.add(Chaos, Thing, 5678L, 4L, 5);
        CompoundTag Saved = FactionWorld.get(Server).save(new CompoundTag(), Server.registryAccess());
        FactionWorld Loaded = FactionWorld.load(Saved, Server.registryAccess());
        FactionState SerpentsAgain = Loaded.state(SerpentsHandFaction.Id);
        FactionState ChaosAgain = Loaded.state(ChaosFaction.Id);
        HostileLedger.Entry FirstAgain = HostileLedger.find(SerpentsAgain, First.Id());
        HostileLedger.Entry SecondAgain = HostileLedger.find(ChaosAgain, Second.Id());
        Helper.assertTrue(FirstAgain != null && SecondAgain != null, "ledger entries were lost on save and load");
        Helper.assertTrue(FirstAgain.Time() == 1234L && FirstAgain.Day() == 2L && FirstAgain.Level() == 3, "the creature entry lost its time or level");
        Helper.assertTrue(FirstAgain.Trophy().Data().getString("CustomName").contains("Specimen Epsilon"), "the creature entry lost its entity data");
        Helper.assertTrue(FirstAgain.Trophy().Data().equals(Creature.Data()), "the creature entity data changed");
        ItemStack GunAgain = SecondAgain.Trophy().stack(Server.registryAccess());
        Helper.assertTrue(ItemStack.isSameItemSameComponents(GunAgain, Gun), "the object entry changed: " + GunAgain);
        Helper.assertTrue(Math.abs(SerpentsAgain.power() - 333.0) < 0.01, "the xp was not saved");
        Helper.assertTrue(HostileFaction.level(SerpentsAgain) == HostileLevels.level(333.0), "the level does not follow the saved xp");
        restore(SerpentsHandFaction.Instance, Serpents, SerpentsBefore);
        restore(ChaosFaction.Instance, Chaos, ChaosBefore);
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "hostile_ledger_drop")
    public static void laterRaidersDropLedgerAnomalies(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState Serpents = FactionWorld.state(Server, SerpentsHandFaction.Instance);
        FactionState Chaos = FactionWorld.state(Server, ChaosFaction.Instance);
        CompoundTag SerpentsBefore = backup(Serpents);
        CompoundTag ChaosBefore = backup(Chaos);
        Serpents.extra().remove(HostileLedger.LedgerKey);
        Chaos.extra().remove(HostileLedger.LedgerKey);
        Scp131Pod Pod = specimen(Helper, 20.5, F, 20.5, "Specimen Gamma");
        HostileTrophy Creature = HostileTrophy.of(Pod);
        Pod.discard();
        HostileLedger.Entry Held = HostileLedger.add(Serpents, Creature, World.getGameTime(), 1L, 2);
        HostileLedger.Entry Key = HostileLedger.add(Chaos, HostileTrophy.of(new ItemStack(Scp005Registry.Key.get()), World.registryAccess()), World.getGameTime(), 1L, 2);
        RaidContext SerpentRaid = open(Helper, SerpentsHandFaction.Instance, new BlockPos(16, F, 16), new BlockPos(30, F, 30), 2);
        RaidContext ChaosRaid = open(Helper, ChaosFaction.Instance, new BlockPos(16, F, 16), new BlockPos(30, F, 30), 2);
        SerpentsHandAgent Agent = raider(Helper, HostileRegistry.AgentType.get(), SerpentRaid, 6.5, F, 6.5, 2, HostileRaider.RoleFighter);
        ChaosInsurgent Insurgent = raider(Helper, HostileRegistry.InsurgentType.get(), ChaosRaid, 6.5, F, 20.5, 2, HostileRaider.RoleFighter);
        Helper.assertTrue(SerpentsHandRaidStyle.issuePod(SerpentRaid, Agent), "the agent was not given a ledger pod");
        Helper.assertTrue(ChaosArsenal.stow(ChaosRaid, Insurgent), "the insurgent was not given a ledger object");
        Helper.assertTrue(Agent.issued() == Held.Id() && Insurgent.issued() == Key.Id(), "the wrong ledger entries were issued");
        Agent.kill();
        Insurgent.kill();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(drops(Helper).size() >= 2, "the fallen raiders did not drop their ledger anomalies"))
                .thenExecute(() -> {
                    Helper.assertTrue(drops(Helper).stream().anyMatch(Found -> {
                        HostileTrophy Inside = SerpentPodItem.trophy(Found.getItem());
                        return Inside != null && Inside.Data().getString("CustomName").contains("Specimen Gamma");
                    }), "the agent did not drop a pod holding the ledger creature");
                    Helper.assertTrue(drops(Helper).stream().anyMatch(Found -> Found.getItem().is(Scp005Registry.Key.get())), "the insurgent did not drop the key");
                    Helper.assertTrue(HostileLedger.find(Serpents, Held.Id()) == null, "the dropped creature is still in the ledger");
                    Helper.assertTrue(HostileLedger.find(Chaos, Key.Id()) == null, "the dropped key is still in the ledger");
                })
                .thenExecute(() -> {
                    close(Helper, SerpentRaid);
                    close(Helper, ChaosRaid);
                    restore(SerpentsHandFaction.Instance, Serpents, SerpentsBefore);
                    restore(ChaosFaction.Instance, Chaos, ChaosBefore);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "hostile_chaos_locker")
    public static void chaosStealsFromStorageLocker(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos LockerAt = new BlockPos(14, F, 12);
        Helper.setBlock(LockerAt, StorageRegistry.SmallContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Direction.WEST));
        StorageContainerBlockEntity Locker = (StorageContainerBlockEntity) World.getBlockEntity(Helper.absolutePos(LockerAt));
        Helper.assertTrue(Locker != null, "the storage locker has no block entity");
        Locker.setContents(new ItemStack(Scp005Registry.Key.get()));
        RaidContext Context = open(Helper, ChaosFaction.Instance, new BlockPos(14, F, 12), new BlockPos(30, F, 30), 3);
        ChaosInsurgent Thief = raider(Helper, HostileRegistry.InsurgentType.get(), Context, 6.5, F, 12.5, 3, HostileRaider.RoleSeeker);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(HostileHauls.has(Context.Current(), Thief.getUUID()), "the insurgent never took the anomaly from the locker"))
                .thenExecute(() -> {
                    HostileTrophy Haul = HostileHauls.of(Context.Current(), Thief.getUUID());
                    Helper.assertTrue(Haul != null && !Haul.creature() && Haul.Type().equals(Scp005Registry.Key.getId()), "the haul is not the key: " + Haul);
                    Helper.assertTrue(Locker.peekContents().isEmpty(), "the locker still holds the key");
                    Helper.assertTrue(Thief.getMainHandItem().is(Scp005Registry.Key.get()), "the thief is not carrying the key");
                    Helper.assertTrue(Context.Current().stolen() == 1, "the theft was not counted");
                })
                .thenExecute(() -> {
                    close(Helper, Context);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "hostile_chaos_chest")
    public static void chaosStealsAnomalyFromChest(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos ChestAt = new BlockPos(14, F, 12);
        Helper.setBlock(ChestAt, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity Chest = (ChestBlockEntity) World.getBlockEntity(Helper.absolutePos(ChestAt));
        Helper.assertTrue(Chest != null, "the chest has no block entity");
        Chest.setItem(0, new ItemStack(Items.BREAD, 5));
        Chest.setItem(4, new ItemStack(Scp005Registry.Key.get()));
        Chest.setItem(9, new ItemStack(Items.IRON_INGOT, 3));
        RaidContext Context = open(Helper, ChaosFaction.Instance, new BlockPos(14, F, 12), new BlockPos(30, F, 30), 3);
        ChaosInsurgent Thief = raider(Helper, HostileRegistry.InsurgentType.get(), Context, 6.5, F, 12.5, 3, HostileRaider.RoleSeeker);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(HostileHauls.has(Context.Current(), Thief.getUUID()), "the insurgent never took the anomaly from the chest"))
                .thenExecute(() -> {
                    HostileTrophy Haul = HostileHauls.of(Context.Current(), Thief.getUUID());
                    Helper.assertTrue(Haul != null && !Haul.creature() && Haul.Type().equals(Scp005Registry.Key.getId()), "the haul is not the key: " + Haul);
                    Helper.assertTrue(Chest.getItem(4).isEmpty(), "the chest still holds the key");
                    Helper.assertTrue(Chest.getItem(0).is(Items.BREAD) && Chest.getItem(0).getCount() == 5, "the insurgent took the bread");
                    Helper.assertTrue(Chest.getItem(9).is(Items.IRON_INGOT) && Chest.getItem(9).getCount() == 3, "the insurgent took the iron");
                    Helper.assertTrue(Thief.getMainHandItem().is(Scp005Registry.Key.get()), "the thief is not carrying the key");
                    Helper.assertTrue(Context.Current().stolen() == 1, "the theft was not counted");
                })
                .thenExecute(() -> {
                    close(Helper, Context);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "hostile_chaos_arsenal")
    public static void chaosTurnsCapturedObjectsOnDefenders(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState Chaos = FactionWorld.state(Server, ChaosFaction.Instance);
        CompoundTag ChaosBefore = backup(Chaos);
        Chaos.extra().remove(HostileLedger.LedgerKey);
        HostileLedger.Entry Gun = HostileLedger.add(Chaos, HostileTrophy.of(Scp127Item.create(), World.registryAccess()), World.getGameTime(), 1L, 3);
        HostileLedger.Entry Device = HostileLedger.add(Chaos, HostileTrophy.of(Z0003DeviceItem.create(), World.registryAccess()), World.getGameTime(), 1L, 3);
        RaidContext Context = open(Helper, ChaosFaction.Instance, new BlockPos(16, F, 16), new BlockPos(30, F, 30), 3);
        ChaosInsurgent Gunner = raider(Helper, HostileRegistry.InsurgentType.get(), Context, 4.5, F, 4.5, 3, HostileRaider.RoleFighter);
        ChaosArsenal.give(Context, Gunner, Gun);
        Helper.assertTrue(Scp127Item.is(Gunner.getMainHandItem()), "the insurgent is not wielding SCP-127");
        DClass Target = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(4.5, F, 16.5));
        float Start = Target.getHealth();
        ChaosInsurgent Thrower = raider(Helper, HostileRegistry.InsurgentType.get(), Context, 26.5, F, 4.5, 3, HostileRaider.RoleFighter);
        Thrower.setNoAi(true);
        ChaosArsenal.give(Context, Thrower, Device);
        Helper.assertTrue(Thrower.thrower() && Z0003DeviceItem.is(Thrower.hidden()), "the insurgent was not handed Z-0003");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Target.getHealth() < Start || !Target.isAlive(), "the insurgent never fired SCP-127 at the defender"))
                .thenExecute(() -> {
                    Helper.assertTrue(Scp127Item.is(Gunner.getMainHandItem()), "the insurgent dropped SCP-127");
                    DClass Far = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(26.5, F, 16.5));
                    ChaosArsenal.throwAt(World, Thrower, Far);
                    Helper.assertTrue(Thrower.hidden().isEmpty() && !Thrower.thrower(), "the thrower still holds Z-0003");
                    Helper.assertTrue(HostileLedger.find(Chaos, Device.Id()) == null, "a thrown Z-0003 is still in the ledger");
                    Helper.assertTrue(HostileLedger.find(Chaos, Gun.Id()) != null, "SCP-127 left the ledger while still held");
                    Helper.assertTrue(!World.getEntitiesOfClass(Z0003Device.class, Helper.getBounds().inflate(8.0), Found -> Found.isAlive()).isEmpty(),
                            "no Z-0003 device was thrown");
                })
                .thenExecute(() -> {
                    close(Helper, Context);
                    restore(ChaosFaction.Instance, Chaos, ChaosBefore);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    private static void doorLayout(GameTestHelper Helper, int F, int Clearance) {
        for (int Y = F; Y < F + 4; Y++) {
            for (int X = 2; X <= 21; X++) {
                Helper.setBlock(new BlockPos(X, Y, 2), Blocks.STONE);
                Helper.setBlock(new BlockPos(X, Y, 21), Blocks.STONE);
                Helper.setBlock(new BlockPos(X, Y, WallZ), Blocks.STONE);
            }
            for (int Z = 2; Z <= 21; Z++) {
                Helper.setBlock(new BlockPos(2, Y, Z), Blocks.STONE);
                Helper.setBlock(new BlockPos(21, Y, Z), Blocks.STONE);
            }
        }
        PanelDoorBlock Frame = DoorRegistry.SingleSlidingDoor.get();
        ServerLevel World = Helper.getLevel();
        BlockPos Base = Helper.absolutePos(new BlockPos(DoorX, F, WallZ));
        DoorKind Kind = Frame.getKind();
        BlockState State = Frame.defaultBlockState().setValue(PanelDoorBlock.Facing, Direction.NORTH);
        for (int Part = 0; Part < Kind.getCellCount(); Part++) {
            BlockPos Cell = PanelDoorBlock.cellPos(Base, Direction.NORTH, Kind.columnOf(Part), Kind.rowOf(Part));
            World.setBlock(Cell, State.setValue(Frame.partProperty(), Part), Block.UPDATE_ALL);
        }
        Helper.setBlock(new BlockPos(DoorX + 1, F + 1, WallZ - 1), DoorRegistry.KeycardScanner.get().defaultBlockState()
                .setValue(KeycardScannerBlock.FACING, Direction.NORTH).setValue(KeycardScannerBlock.Clearance, Clearance));
    }

    private static void drive(GameTestHelper Helper, HostileRaider Raider, int F) {
        Vec3 Goal = Helper.absoluteVec(new Vec3(DoorX + 0.5, F, GoalZ + 0.5));
        Helper.onEachTick(() -> {
            if (Raider.tickCount % 20 == 0 && Raider.getNavigation().isDone()) {
                Raider.getNavigation().moveTo(Goal.x, Goal.y, Goal.z, 1.0);
            }
        });
    }

    private static double flat(GameTestHelper Helper, Entity Body, double X, double Z) {
        Vec3 Here = Helper.relativeVec(Body.position());
        return Math.sqrt(Math.pow(Here.x - X, 2) + Math.pow(Here.z - Z, 2));
    }

    @GameTest(template = DoorArena, timeoutTicks = 1200, batch = "hostile_keycard_pass")
    public static void higherLevelRaidersOpenKeycardDoors(GameTestHelper Helper) {
        int Previous = -1;
        for (int Level = 1; Level <= HostileLevels.MaxLevel; Level++) {
            int Tier = HostileLevels.keycardTier(Level);
            Helper.assertTrue(Tier >= Previous, "the keycard tier fell at level " + Level);
            Previous = Tier;
        }
        Helper.assertTrue(HostileLevels.keycardTier(1) == 0 && HostileLevels.keycardTier(HostileLevels.MaxLevel) > HostileLevels.keycardTier(2), "keycards do not rise with level");
        int F = doorFloor(Helper);
        doorLayout(Helper, F, 3);
        SerpentsHandAgent Agent = raider(Helper, HostileRegistry.AgentType.get(), null, DoorX + 0.5, F, StartZ + 0.5, 4, HostileRaider.RoleFighter);
        Helper.assertTrue(Agent.getOffhandItem().getItem() instanceof KeycardItem Card && Card.getClearance() == 3, "a level 4 agent does not carry a level 3 card");
        drive(Helper, Agent, F);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(flat(Helper, Agent, DoorX + 0.5, GoalZ + 0.5) < 3.0, "the level 4 agent never got through the level 3 door, at "
                        + Helper.relativeVec(Agent.position())))
                .thenExecute(() -> sweep(Helper))
                .thenSucceed();
    }

    @GameTest(template = DoorArena, timeoutTicks = 500, batch = "hostile_keycard_block")
    public static void lowLevelRaidersAreStoppedByKeycardDoors(GameTestHelper Helper) {
        int F = doorFloor(Helper);
        doorLayout(Helper, F, 3);
        ChaosInsurgent Insurgent = raider(Helper, HostileRegistry.InsurgentType.get(), null, DoorX + 0.5, F, StartZ + 0.5, 3, HostileRaider.RoleFighter);
        Helper.assertTrue(Insurgent.getOffhandItem().getItem() instanceof KeycardItem Card && Card.getClearance() == 2, "a level 3 insurgent does not carry a level 2 card");
        drive(Helper, Insurgent, F);
        Helper.runAfterDelay(400, () -> {
            Helper.assertTrue(Helper.relativeVec(Insurgent.position()).z < WallZ, "a level 3 insurgent passed a level 3 door");
            sweep(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "hostile_exit_seal")
    public static void serpentSealTakesCarriersAway(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState Serpents = FactionWorld.state(Server, SerpentsHandFaction.Instance);
        CompoundTag SerpentsBefore = backup(Serpents);
        int Before = HostileLedger.size(Serpents);
        RaidContext Context = open(Helper, SerpentsHandFaction.Instance, new BlockPos(16, F, 16), new BlockPos(24, F, 24), 2);
        SerpentSeal Seal = SerpentsHandRaidStyle.openAt(Context, Helper.absolutePos(new BlockPos(24, F, 24)));
        SerpentsHandAgent Carrier = raider(Helper, HostileRegistry.AgentType.get(), Context, 14.5, F, 14.5, 2, HostileRaider.RoleSeeker);
        SerpentsHandAgent Guard = raider(Helper, HostileRegistry.AgentType.get(), Context, 16.5, F, 12.5, 2, HostileRaider.RoleFighter);
        Scp131Pod Pod = specimen(Helper, 10.5, F, 8.5, "Specimen Zeta");
        HostileTrophy Trophy = SerpentPods.capture(World, Pod, Carrier);
        HostileHauls.put(Context.Current(), Carrier.getUUID(), Trophy);
        Carrier.carry(SerpentPodItem.filled(Trophy, World.registryAccess()));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Carrier.isRemoved(), "the carrier never left through the seal, at " + Helper.relativeVec(Carrier.position())
                        + " state " + Carrier.state()))
                .thenExecute(() -> {
                    Helper.assertTrue(HostileLedger.size(Serpents) == Before + 1, "the carried pod did not reach the ledger");
                    Helper.assertTrue(Seal.isAlive() && !Seal.closing(), "the seal closed while the raid was still on");
                    Guard.beginSink();
                })
                .thenWaitUntil(() -> Helper.assertTrue(Guard.isRemoved(), "the agent did not sink into the seal"))
                .thenExecute(() -> {
                    Helper.assertTrue(Context.Current().remaining() == 0 || !Context.Current().live(), "sunken agents are still counted in the raid");
                    close(Helper, Context);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Seal.isRemoved(), "the seal never closed after the raid"))
                .thenExecute(() -> {
                    restore(SerpentsHandFaction.Instance, Serpents, SerpentsBefore);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1600, batch = "hostile_exit_heli")
    public static void chaosHelicopterPicksUpInsurgents(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState Chaos = FactionWorld.state(Server, ChaosFaction.Instance);
        CompoundTag ChaosBefore = backup(Chaos);
        int Before = HostileLedger.size(Chaos);
        BlockPos Ground = Helper.absolutePos(new BlockPos(16, F, 16));
        MtfLandingZone.Zone Pad = new MtfLandingZone.Zone(Ground, Ground.getY() + MtfLandingZone.DefaultHover, 0.0f);
        RaidContext Context = open(Helper, ChaosFaction.Instance, new BlockPos(16, F, 16), new BlockPos(16, F, 16), 3);
        Context.data().putLong(ChaosRaidStyle.ZoneKey, Ground.asLong());
        Context.data().putDouble(ChaosRaidStyle.HoverKey, Pad.HoverY());
        Context.data().putFloat(ChaosRaidStyle.YawKey, Pad.Yaw());
        Vec3 Stand = ChaosRaidStyle.standPoint(Context.Current(), 0);
        Vec3 Relative = Helper.relativeVec(Stand);
        ChaosInsurgent Insurgent = raider(Helper, HostileRegistry.InsurgentType.get(), Context, Relative.x + 3.0, F, Relative.z + 3.0, 3, HostileRaider.RoleSeeker);
        HostileHauls.put(Context.Current(), Insurgent.getUUID(), HostileTrophy.of(new ItemStack(Scp005Registry.Key.get()), World.registryAccess()));
        Insurgent.carry(new ItemStack(Scp005Registry.Key.get()));
        ChaosHelicopter[] Ride = new ChaosHelicopter[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Insurgent.state() == HostileRaider.StateWaiting, "the carrier never reached the landing zone, at "
                        + Helper.relativeVec(Insurgent.position())))
                .thenExecute(() -> {
                    ChaosRaidStyle.Instance.tick(fresh(Helper, Context));
                    Ride[0] = ChaosRaidStyle.helicopter(fresh(Helper, Context));
                    Helper.assertTrue(Ride[0] != null && Ride[0].isPickup(), "no pickup helicopter was sent for the waiting carrier");
                    Vec3 Hover = Pad.hover();
                    Ride[0].moveTo(Hover.x, Hover.y, Hover.z, Pad.Yaw(), 0.0f);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Insurgent.isRemoved(), "the carrier never climbed aboard, state " + Insurgent.state()))
                .thenExecute(() -> {
                    Helper.assertTrue(HostileLedger.size(Chaos) == Before + 1, "the extracted object did not reach the Chaos ledger");
                    Helper.assertFalse(Context.Current().raiders().contains(Insurgent.getUUID()), "the extracted insurgent is still listed");
                    close(Helper, Context);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Ride[0].isRemoved(), "the helicopter never flew off and despawned"))
                .thenExecute(() -> {
                    restore(ChaosFaction.Instance, Chaos, ChaosBefore);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "hostile_fast_rope")
    public static void chaosFastRopesIn(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Ground = Helper.absolutePos(new BlockPos(16, F, 16));
        MtfLandingZone.Zone Pad = new MtfLandingZone.Zone(Ground, Ground.getY() + MtfLandingZone.DefaultHover, 0.0f);
        RaidContext Context = open(Helper, ChaosFaction.Instance, new BlockPos(10, F, 10), new BlockPos(16, F, 16), 2);
        Context.data().putInt(HostileRaidStyle.PendingKey, 2);
        Context.data().putInt(HostileRaidStyle.SeekersKey, 1);
        Context.data().putLong(ChaosRaidStyle.ZoneKey, Ground.asLong());
        Context.data().putDouble(ChaosRaidStyle.HoverKey, Pad.HoverY());
        Context.data().putFloat(ChaosRaidStyle.YawKey, Pad.Yaw());
        ChaosHelicopter Ride = ChaosHelicopter.dispatch(World, Context.Current(), Pad, 2, false);
        Helper.assertTrue(Ride != null, "the deploy helicopter was not created");
        Context.data().putString(ChaosRaidStyle.HeliKey, Ride.getUUID().toString());
        Context.data().putLong(ChaosRaidStyle.SentKey, World.getGameTime());
        Vec3 Hover = Pad.hover();
        Ride.moveTo(Hover.x, Hover.y, Hover.z, Pad.Yaw(), 0.0f);
        Helper.startSequence()
                .thenWaitUntil(() -> {
                    List<ChaosInsurgent> Down = World.getEntitiesOfClass(ChaosInsurgent.class, Helper.getBounds().inflate(8.0),
                            Found -> Found.isAlive() && Found.active() && Found.onGround());
                    Helper.assertTrue(Down.size() == 2, "the insurgents did not fast rope down, " + Down.size() + " landed");
                })
                .thenExecute(() -> {
                    Helper.assertTrue(Context.Current().remaining() == 2, "the roped insurgents were not enlisted in the raid");
                    Helper.assertTrue(HostileRaidStyle.active(Context), "the raid ended during the drop");
                })
                .thenWaitUntil(() -> Helper.assertTrue(Ride.isRemoved(), "the deploy helicopter never left"))
                .thenExecute(() -> {
                    close(Helper, Context);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "hostile_schedule")
    public static void raidSchedulingRespectsCooldownAndRule(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        SerpentsHandFaction Faction = SerpentsHandFaction.Instance;
        FactionState State = FactionWorld.state(Server, Faction);
        FactionState Rival = FactionWorld.state(Server, ChaosFaction.Instance);
        FactionPhase Phase = State.phase();
        long Next = State.timer(HostileFaction.NextRaidKey);
        long Ended = State.timer(HostileFaction.LastEndKey);
        long RivalEnded = Rival.timer(HostileFaction.LastEndKey);
        GameRules.BooleanValue Rule = Server.getGameRules().getRule(HostileRegistry.SerpentRule);
        boolean Was = Rule.get();
        long Now = Server.overworld().getGameTime();
        Investigations Cases = Investigations.get(Server);
        int Documents = Cases.opened(Server);
        try {
            if (!Phase.active()) {
                State.setPhase(FactionPhase.Awakened);
            }
            Rule.set(true, Server);
            Rival.setTimer(HostileFaction.LastEndKey, 0L);
            Cases.setOpened(HostileGate.DocumentsNeeded - 1);
            State.setTimer(HostileFaction.NextRaidKey, Now - 1L);
            Faction.tick(Server, State);
            Helper.assertFalse(Faction.canRaid(State), "the Serpent's Hand may raid before enough documents were opened");
            Cases.setOpened(HostileGate.DocumentsNeeded);
            State.setTimer(HostileFaction.NextRaidKey, Now + 10000L);
            Faction.tick(Server, State);
            Helper.assertFalse(Faction.canRaid(State), "the Serpent's Hand may raid during their cooldown");
            State.setTimer(HostileFaction.NextRaidKey, Now - 1L);
            Faction.tick(Server, State);
            Helper.assertTrue(Faction.canRaid(State) || RaidWorld.get(Server).activeFor(ChaosFaction.Id) != null, "the Serpent's Hand may not raid after their cooldown");
            Rule.set(false, Server);
            Faction.tick(Server, State);
            Helper.assertFalse(Faction.canRaid(State), "the Serpent's Hand may raid with their game rule off");
            Rule.set(true, Server);
            Rival.setTimer(HostileFaction.LastEndKey, Now - 1000L);
            Faction.tick(Server, State);
            Helper.assertFalse(Faction.canRaid(State), "the Serpent's Hand may raid right after a Chaos raid");
            Rival.setTimer(HostileFaction.LastEndKey, 0L);
            Faction.schedule(Server, State, RaidOutcome.TimedOut, true, World.random);
            long Wait = State.timer(HostileFaction.NextRaidKey) - Now;
            long Least = (long) (Faction.minDays() * 0.6 * HostileFaction.DayTicks);
            long Most = (long) (Faction.startDays() * HostileFaction.JitterMax * 1.5 * HostileFaction.DayTicks) + 40L;
            Helper.assertTrue(Wait >= Least && Wait <= Most, "the next raid was scheduled " + Wait + " ticks away");
            Helper.assertTrue(Faction.baseDays(1) == Faction.startDays(), "the first level cooldown is not the start cooldown");
            double Daily = Faction.accrual(State, 0) * HostileFaction.DayTicks;
            Helper.assertTrue(Math.abs(Daily - HostileFaction.DailyXp) < 1.0E-6, "the slow daily xp gain is " + Daily);
            double Previous = Double.MAX_VALUE;
            for (int Level = 1; Level <= HostileLevels.MaxLevel; Level++) {
                double Days = Faction.baseDays(Level);
                Helper.assertTrue(Days <= Previous && Days >= Faction.minDays(), "the cooldown at level " + Level + " is " + Days);
                Previous = Days;
            }
            Faction.schedule(Server, State, RaidOutcome.Stopped, false, World.random);
            Helper.assertTrue(State.timer(HostileFaction.NextRaidKey) - Now <= 2400L + 40L, "a raid that never began was not retried soon");
        } finally {
            Cases.setOpened(Documents);
            Rule.set(Was, Server);
            State.setTimer(HostileFaction.NextRaidKey, Next);
            State.setTimer(HostileFaction.LastEndKey, Ended);
            Rival.setTimer(HostileFaction.LastEndKey, RivalEnded);
            State.setPhase(Phase);
            Faction.tick(Server, State);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "hostile_statues")
    public static void statuesAreIgnored(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        RaidContext Context = open(Helper, SerpentsHandFaction.Instance, new BlockPos(16, F, 16), new BlockPos(30, F, 30), 2);
        SerpentsHandAgent Agent = raider(Helper, HostileRegistry.AgentType.get(), Context, 8.5, F, 8.5, 2, HostileRaider.RoleSeeker);
        Scp131Pod Pod = specimen(Helper, 12.5, F, 12.5, "Specimen Statue");
        DClass Person = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(12.5, F, 4.5));
        Helper.assertTrue(HostileTargets.capturable(Pod), "the pod should be capturable before it is a statue");
        Helper.assertTrue(HostileTargets.hostileTo(Agent, Person), "a D-Class is not a target before it is a statue");
        Helper.assertTrue(Z0003Petrify.petrify(Pod, true) && Z0003Petrify.petrify(Person, true), "the statues could not be made");
        Helper.assertFalse(HostileTargets.capturable(Pod), "a statue can be captured");
        Helper.assertTrue(SerpentCaptureGoal.find(World, Context.Current(), Agent, null) == null, "the capture search picked a statue");
        Helper.assertFalse(HostileTargets.hostileTo(Agent, Person), "a statue is targeted");
        close(Helper, Context);
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "hostile_security")
    public static void defencesTargetRaiders(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        SerpentsHandAgent Agent = raider(Helper, HostileRegistry.AgentType.get(), null, 8.5, F, 8.5, 2, HostileRaider.RoleFighter);
        ChaosInsurgent Insurgent = raider(Helper, HostileRegistry.InsurgentType.get(), null, 10.5, F, 8.5, 2, HostileRaider.RoleFighter);
        SerpentsHandAgent Other = raider(Helper, HostileRegistry.AgentType.get(), null, 8.5, F, 10.5, 2, HostileRaider.RoleFighter);
        Agent.setNoAi(true);
        Insurgent.setNoAi(true);
        Other.setNoAi(true);
        for (HostileRaider Raider : List.of(Agent, Insurgent)) {
            Helper.assertTrue(TurretTargeting.categoryOf(Raider) == TurretCategory.Hostile, "turrets do not see a raider as hostile: " + TurretTargeting.categoryOf(Raider));
            Helper.assertTrue(TurretTargeting.isTarget(GuardRules.Default.toTurret(), null, Raider), "a default turret will not shoot a raider");
            UUID Caller = UUID.randomUUID();
            Helper.assertFalse(MtfFriends.isFriend(Caller, Raider), "the MTF treat a raider as a friend");
            Helper.assertTrue(MtfFriends.isHostile(Caller, Raider), "the MTF do not engage a raider");
        }
        DClass Guard = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(8.5, F, 14.5));
        BlockPos Post = Helper.absolutePos(new BlockPos(8, F, 14));
        PatrolRoute Route = new PatrolRoute(UUID.randomUUID(), World.dimension(), Post, List.of(new PatrolPoint(Post, 20, Direction.NORTH)), false, GuardRules.Default,
                Optional.empty(), "");
        GuardDuty.assign(Guard, Route);
        Helper.assertTrue(GuardThreats.isThreat(Guard, Route, GuardRules.Default.toTurret(), Agent), "guards do not see a Serpent's Hand agent as a threat");
        Helper.assertTrue(GuardThreats.isThreat(Guard, Route, GuardRules.Default.toTurret(), Insurgent), "guards do not see a Chaos insurgent as a threat");
        Helper.assertTrue(GuardThreats.find(Guard, Route, GuardRules.Default.toTurret()) instanceof HostileRaider, "a guard does not pick a raider to fight");
        MtfOperative Operative = Helper.spawnWithNoFreeWill(MtfRegistry.OperativeType.get(), new Vec3(4.5, F, 4.5));
        Helper.assertTrue(HostileTargets.hostileTo(Agent, Guard) && HostileTargets.hostileTo(Agent, Operative), "raiders ignore guards or the MTF");
        Helper.assertTrue(HostileTargets.hostileTo(Agent, Insurgent) && HostileTargets.hostileTo(Insurgent, Agent), "the two factions do not fight each other");
        Helper.assertFalse(HostileTargets.hostileTo(Agent, Other), "Serpent's Hand agents target each other");
        Helper.assertTrue(Agent.isAlliedTo(Other) && !Agent.isAlliedTo(Insurgent), "faction alliances are wrong");
        sweep(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "hostile_seal_arrival")
    public static void serpentsArriveThroughSeal(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        RaidContext Context = open(Helper, SerpentsHandFaction.Instance, new BlockPos(16, F, 16), new BlockPos(24, F, 24), 2);
        Context.data().putInt(HostileRaidStyle.PendingKey, 2);
        Context.data().putInt(HostileRaidStyle.SquadKey, 2);
        Context.data().putInt(HostileRaidStyle.SeekersKey, 1);
        SerpentsHandRaidStyle.openAt(Context, Helper.absolutePos(new BlockPos(24, F, 24)));
        Helper.startSequence()
                .thenWaitUntil(() -> {
                    List<SerpentsHandAgent> Up = World.getEntitiesOfClass(SerpentsHandAgent.class, Helper.getBounds().inflate(8.0), Found -> Found.isAlive() && Found.active());
                    Helper.assertTrue(Up.size() == 2, "the agents did not rise from the seal, " + Up.size() + " are up");
                })
                .thenExecute(() -> {
                    Helper.assertTrue(Context.Current().remaining() == 2, "the risen agents were not enlisted");
                    Helper.assertTrue(World.getEntitiesOfClass(SerpentsHandAgent.class, Helper.getBounds().inflate(8.0), HostileRaider::seeker).size() == 1,
                            "the squad has the wrong number of seekers");
                    close(Helper, Context);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1400, batch = "hostile_flow_serpents")
    public static void serpentRaidRunsStartToFinish(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState Serpents = FactionWorld.state(Server, SerpentsHandFaction.Instance);
        CompoundTag SerpentsBefore = backup(Serpents);
        long Next = Serpents.timer(HostileFaction.NextRaidKey);
        long Ended = Serpents.timer(HostileFaction.LastEndKey);
        RaidContext Context = staged(Helper, SerpentsHandFaction.Instance, new BlockPos(16, F, 16), -6.0, Math.PI * 0.25);
        beginSoon(Helper, Context);
        Raid Current = Context.Current();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Current.stage() == RaidStage.Active, "the raid never began, stage " + Current.stage().serialName()))
                .thenExecute(() -> Helper.assertTrue(SerpentsHandRaidStyle.seal(fresh(Helper, Context)) != null, "no seal opened for the raid"))
                .thenWaitUntil(() -> Helper.assertTrue(Current.spawned() >= 2 && HostileRaidStyle.raiders(fresh(Helper, Context)).stream().allMatch(HostileRaider::active),
                        "the squad did not arrive, " + Current.spawned() + " spawned"))
                .thenExecute(() -> RaidDirector.retreat(fresh(Helper, Context), RaidOutcome.TimedOut))
                .thenWaitUntil(() -> Helper.assertTrue(!Current.live(), "the raid never finished, " + Current.remaining() + " raiders left, stage " + Current.stage().serialName()))
                .thenExecute(() -> {
                    Helper.assertTrue(World.getEntitiesOfClass(SerpentsHandAgent.class, Helper.getBounds().inflate(24.0), Found -> Found.isAlive()).isEmpty(),
                            "agents were left behind after the raid");
                    Helper.assertTrue(RaidWorld.get(Server).raid(Current.id()) == null, "the finished raid is still stored");
                    Helper.assertTrue(Serpents.timer(HostileFaction.NextRaidKey) > Server.overworld().getGameTime(), "no next raid was scheduled");
                })
                .thenWaitUntil(() -> Helper.assertTrue(World.getEntitiesOfClass(SerpentSeal.class, Helper.getBounds().inflate(24.0), Found -> Found.isAlive()).isEmpty(),
                        "the seal stayed open after the raid"))
                .thenExecute(() -> {
                    Serpents.setTimer(HostileFaction.NextRaidKey, Next);
                    Serpents.setTimer(HostileFaction.LastEndKey, Ended);
                    restore(SerpentsHandFaction.Instance, Serpents, SerpentsBefore);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 2400, batch = "hostile_flow_chaos")
    public static void chaosRaidRunsStartToFinish(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState Chaos = FactionWorld.state(Server, ChaosFaction.Instance);
        CompoundTag ChaosBefore = backup(Chaos);
        long Next = Chaos.timer(HostileFaction.NextRaidKey);
        long Ended = Chaos.timer(HostileFaction.LastEndKey);
        RaidContext Context = staged(Helper, ChaosFaction.Instance, new BlockPos(16, F, 16), -6.0, Math.PI * 1.25);
        beginSoon(Helper, Context);
        Raid Current = Context.Current();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Current.stage() == RaidStage.Active, "the raid never began, stage " + Current.stage().serialName()))
                .thenExecute(() -> {
                    ChaosHelicopter Ride = ChaosRaidStyle.helicopter(fresh(Helper, Context));
                    Helper.assertTrue(Ride != null, "the raid began without a helicopter on open ground");
                    MtfLandingZone.Zone Pad = ChaosRaidStyle.zone(Current);
                    if (Ride != null && Pad != null) {
                        Vec3 Hover = Pad.hover();
                        Ride.moveTo(Hover.x, Hover.y, Hover.z, Pad.Yaw(), 0.0f);
                    }
                })
                .thenWaitUntil(() -> Helper.assertTrue(Current.spawned() >= 3 && HostileRaidStyle.raiders(fresh(Helper, Context)).stream().allMatch(HostileRaider::active)
                        && ChaosRaidStyle.Instance.pending(fresh(Helper, Context)) == 0, "the squad did not land, " + Current.spawned() + " spawned"))
                .thenExecute(() -> RaidDirector.retreat(fresh(Helper, Context), RaidOutcome.TimedOut))
                .thenWaitUntil(() -> {
                    ChaosHelicopter Ride = ChaosRaidStyle.helicopter(fresh(Helper, Context));
                    MtfLandingZone.Zone Pad = ChaosRaidStyle.zone(Current);
                    if (Ride != null && Pad != null && Ride.isPickup() && Ride.phase() == ChaosHelicopter.PhaseApproach) {
                        Vec3 Hover = Pad.hover();
                        Ride.moveTo(Hover.x, Hover.y, Hover.z, Pad.Yaw(), 0.0f);
                    }
                    Helper.assertTrue(!Current.live(), "the raid never finished, " + Current.remaining() + " raiders left, stage " + Current.stage().serialName());
                })
                .thenExecute(() -> {
                    Helper.assertTrue(World.getEntitiesOfClass(ChaosInsurgent.class, Helper.getBounds().inflate(24.0), Found -> Found.isAlive()).isEmpty(),
                            "insurgents were left behind after the raid");
                    Helper.assertTrue(Chaos.timer(HostileFaction.NextRaidKey) > Server.overworld().getGameTime(), "no next raid was scheduled");
                })
                .thenExecute(() -> {
                    Chaos.setTimer(HostileFaction.NextRaidKey, Next);
                    Chaos.setTimer(HostileFaction.LastEndKey, Ended);
                    restore(ChaosFaction.Instance, Chaos, ChaosBefore);
                    sweep(Helper);
                })
                .thenSucceed();
    }

    private static double[] centroid(BufferedImage Picture, Predicate<int[]> Wanted, double InsideRadius, double OutsideRadius) {
        double SumX = 0.0;
        double SumY = 0.0;
        int Count = 0;
        double Middle = Picture.getWidth() / 2.0;
        for (int Y = 0; Y < Picture.getHeight(); Y++) {
            for (int X = 0; X < Picture.getWidth(); X++) {
                int Argb = Picture.getRGB(X, Y);
                int[] Pixel = {(Argb >> 16) & 255, (Argb >> 8) & 255, Argb & 255, (Argb >>> 24) & 255};
                double Distance = Math.hypot(X + 0.5 - Middle, Y + 0.5 - Middle);
                if (Distance >= InsideRadius && Distance <= OutsideRadius && Wanted.test(Pixel)) {
                    SumX += X + 0.5;
                    SumY += Y + 0.5;
                    Count++;
                }
            }
        }
        return Count == 0 ? new double[]{Double.NaN, Double.NaN, 0.0} : new double[]{SumX / Count - Middle, SumY / Count - Middle, Count};
    }

    private static BufferedImage art(String Name) throws IOException {
        try (InputStream Input = HostileRaidGameTests.class.getResourceAsStream("/assets/scp_unleashed/textures/entity/serpents_hand/" + Name)) {
            if (Input == null) {
                throw new IOException("missing " + Name);
            }
            return ImageIO.read(Input);
        }
    }

    private static void centred(GameTestHelper Helper, double[] Found, double Limit, String Name) {
        Helper.assertTrue(Found[2] > 0.0, "no " + Name + " pixels were found");
        Helper.assertTrue(Math.hypot(Found[0], Found[1]) <= Limit, Name + " sits " + String.format(Locale.ROOT, "%.2f %.2f", Found[0], Found[1]) + " pixels off the seal centre");
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "hostile_seal_art")
    public static void sealArtIsCentred(GameTestHelper Helper) {
        Predicate<int[]> Gold = Pixel -> Pixel[3] > 200 && Pixel[0] > 150 && Pixel[1] > 100 && Pixel[2] < 130;
        Predicate<int[]> Teal = Pixel -> Pixel[3] > 200 && Pixel[1] > 120 && Pixel[2] > 120 && Pixel[0] < 200;
        Predicate<int[]> Eye = Pixel -> Pixel[3] > 200 && Pixel[1] > 200 && Pixel[2] > 180;
        Predicate<int[]> Solid = Pixel -> Pixel[3] > 200;
        try {
            BufferedImage Ring = art("seal_ring.png");
            BufferedImage Sigil = art("seal_sigil.png");
            Helper.assertTrue(Ring.getWidth() == Ring.getHeight() && Sigil.getWidth() == Sigil.getHeight(), "the seal textures are not square");
            centred(Helper, centroid(Ring, Solid, 28.5, 32.0), 0.5, "the outer rim");
            centred(Helper, centroid(Ring, Solid, 18.5, 21.0), 0.5, "the inner rim");
            centred(Helper, centroid(Ring, Teal, 21.0, 29.0), 1.6, "the serpent");
            centred(Helper, centroid(Sigil, Solid, 13.5, 16.0), 0.5, "the sigil circle");
            centred(Helper, centroid(Sigil, Eye, 0.0, 13.5), 0.6, "the eye in the palm");
            centred(Helper, centroid(Sigil, Gold, 0.0, 13.5), 1.5, "the hand");
        } catch (IOException Problem) {
            Helper.fail("the seal textures could not be read: " + Problem.getMessage());
            return;
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "hostile_seal_centre")
    public static void sealSitsOnItsSpotAndTakesRaidersAtItsCentre(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        FactionState Serpents = FactionWorld.state(Server, SerpentsHandFaction.Instance);
        CompoundTag SerpentsBefore = backup(Serpents);
        BlockPos Spot = Helper.absolutePos(new BlockPos(20, F, 20));
        RaidContext Context = open(Helper, SerpentsHandFaction.Instance, new BlockPos(10, F, 10), new BlockPos(20, F, 20), 2);
        SerpentSeal First = SerpentsHandRaidStyle.openAt(Context, Spot);
        Helper.assertTrue(Math.abs(First.getX() - (Spot.getX() + 0.5)) < 1.0E-9 && Math.abs(First.getZ() - (Spot.getZ() + 0.5)) < 1.0E-9,
                "the seal is not on the centre of its block: " + First.position() + " for " + Spot);
        Helper.assertTrue(RaidDirector.exitOf(Context.Current()).equals(Spot), "the exit is not the seal's block");
        Helper.assertTrue(SerpentsHandRaidStyle.centre(fresh(Helper, Context)).distanceTo(First.position()) < 1.0E-9, "the sink centre is not the seal");
        First.discard();
        Context.data().putInt(HostileRaidStyle.PendingKey, 3);
        Context.data().putInt(HostileRaidStyle.SquadKey, 3);
        Context.data().putInt(HostileRaidStyle.SpawnedKey, 0);
        SerpentsHandRaidStyle.Instance.tick(fresh(Helper, Context));
        SerpentSeal Again = SerpentsHandRaidStyle.seal(fresh(Helper, Context));
        Helper.assertTrue(Again != null && Again != First, "the missing seal was not opened again");
        Helper.assertTrue(Math.abs(Again.getX() - (Spot.getX() + 0.5)) < 1.0E-9 && Math.abs(Again.getZ() - (Spot.getZ() + 0.5)) < 1.0E-9,
                "the reopened seal moved off its block centre: " + Again.position());
        Vec3 Centre = Again.position();
        SerpentsHandAgent Leaver = raider(Helper, HostileRegistry.AgentType.get(), Context, 20.5 + 1.6, F, 20.5 + 0.7, 2, HostileRaider.RoleFighter);
        Leaver.setNoAi(true);
        SerpentsHandRaidStyle.Instance.arrive(fresh(Helper, Context), Leaver);
        Vec3[] Last = new Vec3[1];
        Helper.onEachTick(() -> {
            if (!Leaver.isRemoved()) {
                Last[0] = Leaver.position();
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> {
                    List<SerpentsHandAgent> Risen = World.getEntitiesOfClass(SerpentsHandAgent.class, Helper.getBounds().inflate(8.0), Found -> Found != Leaver && Found.anchor() != null);
                    Helper.assertTrue(Risen.size() >= 3, "only " + Risen.size() + " agents rose from the seal");
                    for (SerpentsHandAgent Agent : Risen) {
                        Vec3 Rose = Agent.anchor();
                        double Flat = Math.hypot(Rose.x - Centre.x, Rose.z - Centre.z);
                        Helper.assertTrue(Flat <= SerpentsHandRaidStyle.EmergeSpread + 1.0E-6, "an agent rose " + Flat + " blocks from the seal centre");
                    }
                })
                .thenWaitUntil(() -> Helper.assertTrue(Leaver.isRemoved(), "the agent never sank away"))
                .thenExecute(() -> {
                    double Flat = Math.hypot(Last[0].x - Centre.x, Last[0].z - Centre.z);
                    Helper.assertTrue(Flat < 0.05, "the agent sank " + Flat + " blocks from the seal centre");
                    close(Helper, Context);
                    restore(SerpentsHandFaction.Instance, Serpents, SerpentsBefore);
                    sweep(Helper);
                })
                .thenSucceed();
    }
}
