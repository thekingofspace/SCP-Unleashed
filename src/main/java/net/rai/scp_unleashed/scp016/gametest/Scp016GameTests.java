package net.rai.scp_unleashed.scp016.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp016.Scp016Affliction;
import net.rai.scp_unleashed.scp016.Scp016Brute;
import net.rai.scp_unleashed.scp016.Scp016Duelist;
import net.rai.scp_unleashed.scp016.Scp016Events;
import net.rai.scp_unleashed.scp016.Scp016Flyer;
import net.rai.scp_unleashed.scp016.Scp016Glutton;
import net.rai.scp_unleashed.scp016.Scp016Registry;
import net.rai.scp_unleashed.scp016.Scp016Skitter;
import net.rai.scp_unleashed.scp016.Scp016Slinger;
import net.rai.scp_unleashed.scp016.Scp016Spawn;
import net.rai.scp_unleashed.scp016.Scp016Swimmer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp016GameTests {
    public static final String Arena = "scp016_arena";
    public static final int Wide = 24;
    public static final double Near = 6.0;
    public static final int SpawnShield = 70;

    private Scp016GameTests() {
    }

    private static final class Silent extends ServerGamePacketListenerImpl {
        private Silent(MinecraftServer Server, ServerPlayer User) {
            super(Server, wired(), User, CommonListenerCookie.createInitial(User.getGameProfile(), false));
        }

        private static Connection wired() {
            Connection Link = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(Link);
            return Link;
        }

        @Override
        public void send(Packet<?> Sent) {
        }

        @Override
        public void send(Packet<?> Sent, @Nullable PacketSendListener Listener) {
        }

        @Override
        public void tick() {
        }

        @Override
        public void disconnect(Component Reason) {
        }
    }

    private static void enrol(MinecraftServer Server, ServerPlayer User, boolean Joining) {
        List<ServerPlayer> Players = ObfuscationReflectionHelper.getPrivateValue(PlayerList.class, Server.getPlayerList(), "players");
        Map<UUID, ServerPlayer> ByUuid = ObfuscationReflectionHelper.getPrivateValue(PlayerList.class, Server.getPlayerList(), "playersByUUID");
        if (Joining) {
            Players.add(User);
            ByUuid.put(User.getUUID(), User);
        } else {
            Players.remove(User);
            ByUuid.remove(User.getUUID());
        }
    }

    private static ServerPlayer survivor(GameTestHelper Helper, double X, double Y, double Z) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        ServerPlayer User = new ServerPlayer(Server, World, new GameProfile(UUID.randomUUID(), "scp016_test"), ClientInformation.createDefault()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        User.connection = new Silent(Server, User);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        enrol(Server, User, true);
        return User;
    }

    private static void dismiss(ServerPlayer User) {
        enrol(User.server, User, false);
        if (!User.isRemoved()) {
            User.serverLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static FakePlayer attacker(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp016_attacker"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void release(GameTestHelper Helper, ServerPlayer User) {
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    private static void floor(GameTestHelper Helper, int Level) {
        for (int X = 0; X < Wide; X++) {
            for (int Z = 0; Z < Wide; Z++) {
                Helper.setBlock(new BlockPos(X, Level, Z), Blocks.STONE.defaultBlockState());
            }
        }
    }

    private static Villager host(GameTestHelper Helper, double X, double Z) {
        Villager Host = Helper.spawn(EntityType.VILLAGER, new Vec3(X, 2.0, Z));
        Host.setNoAi(true);
        Helper.assertTrue(Scp016Affliction.afflict(Host), "the villager could not be afflicted");
        return Host;
    }

    private static DClass body(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        DClass Body = DClassRegistry.DClassType.get().create(World);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        Body.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Body.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.COMMAND, null);
        World.addFreshEntity(Body);
        return Body;
    }

    private static <T extends Scp016Spawn> T raise(GameTestHelper Helper, EntityType<T> Type, double X, double Y, double Z, boolean Emerging) {
        Scp016Spawn Risen = Scp016Events.raise(Helper.getLevel(), Type, Helper.absoluteVec(new Vec3(X, Y, Z)), 0, Emerging);
        Helper.assertTrue(Risen != null, "could not raise " + Type);
        return Type.tryCast(Risen);
    }

    private static List<Scp016Spawn> risenNear(GameTestHelper Helper, Vec3 At) {
        return Helper.getLevel().getEntitiesOfClass(Scp016Spawn.class, AABB.ofSize(At, Near * 2.0, Near, Near * 2.0), Found -> Found.isAlive());
    }

    private static void clear(GameTestHelper Helper) {
        AABB Area = Helper.getBounds().inflate(8.0);
        for (Scp016Spawn Found : Helper.getLevel().getEntitiesOfClass(Scp016Spawn.class, Area)) {
            Found.discard();
        }
    }

    private static void expectForm(GameTestHelper Helper, LivingEntity Host, DamageSource Source, Class<? extends Scp016Spawn> Wanted, String Case) {
        Vec3 At = Host.position();
        Host.hurt(Source, 1000.0f);
        Helper.assertTrue(Host.isDeadOrDying(), Case + ": the host did not die");
        List<Scp016Spawn> Found = risenNear(Helper, At);
        Helper.assertTrue(Found.size() == 1, Case + ": expected one SCP-016-1 instance, found " + Found.size());
        Helper.assertTrue(Wanted.isInstance(Found.get(0)), Case + ": expected " + Wanted.getSimpleName() + " but " + Found.get(0).getClass().getSimpleName() + " rose");
        Found.get(0).discard();
    }

    private static void expectDefault(GameTestHelper Helper, LivingEntity Host, Function<LivingEntity, Boolean> Kill, String Case) {
        Vec3 At = Host.position();
        Kill.apply(Host);
        List<Scp016Spawn> Found = risenNear(Helper, At);
        Helper.assertTrue(Found.size() == 1, Case + ": expected one SCP-016-1 instance, found " + Found.size());
        Scp016Spawn Risen = Found.get(0);
        Helper.assertTrue(Risen instanceof Scp016Brute || Risen instanceof Scp016Skitter || Risen instanceof Scp016Slinger,
                Case + ": expected a brute, skitter or slinger but " + Risen.getClass().getSimpleName() + " rose");
        Risen.discard();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp016_slain")
    public static void playerKillsGiveTheDuelist(GameTestHelper Helper) {
        floor(Helper, 1);
        ServerLevel World = Helper.getLevel();
        FakePlayer User = attacker(Helper);
        expectForm(Helper, host(Helper, 6.5, 6.5), World.damageSources().playerAttack(User), Scp016Duelist.class, "villager struck by a player");
        DClass Prisoner = body(Helper, 12.5, 6.5);
        Helper.assertTrue(Scp016Affliction.afflict(Prisoner), "the D-Class could not be afflicted");
        Arrow Shot = new Arrow(World, User, new ItemStack(Items.ARROW), null);
        expectForm(Helper, Prisoner, World.damageSources().arrow(Shot, User), Scp016Duelist.class, "D-Class shot by a player's arrow");
        Wolf Pet = Helper.spawn(EntityType.WOLF, new Vec3(18.5, 2.0, 18.5));
        Pet.setNoAi(true);
        Pet.tame(User);
        expectForm(Helper, host(Helper, 18.5, 6.5), World.damageSources().mobAttack(Pet), Scp016Duelist.class, "villager killed by a player's wolf");
        Villager Burnt = host(Helper, 6.5, 14.5);
        Burnt.hurt(World.damageSources().playerAttack(User), 0.5f);
        expectForm(Helper, Burnt, World.damageSources().onFire(), Scp016Duelist.class, "villager burnt after a player's hit");
        Pet.discard();
        release(Helper, User);
        clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp016_causes")
    public static void environmentalDeathsPickTheirForms(GameTestHelper Helper) {
        floor(Helper, 1);
        ServerLevel World = Helper.getLevel();
        expectForm(Helper, host(Helper, 5.5, 5.5), World.damageSources().starve(), Scp016Glutton.class, "starvation");
        expectForm(Helper, host(Helper, 12.5, 5.5), World.damageSources().drown(), Scp016Swimmer.class, "drowning");
        expectForm(Helper, host(Helper, 18.5, 5.5), World.damageSources().fall(), Scp016Flyer.class, "fall");
        FakePlayer User = attacker(Helper);
        Villager Pushed = host(Helper, 5.5, 14.5);
        Pushed.hurt(World.damageSources().playerAttack(User), 0.5f);
        expectForm(Helper, Pushed, World.damageSources().fall(), Scp016Flyer.class, "fall after a player's hit");
        release(Helper, User);
        clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp016_default")
    public static void otherDeathsUseTheDefaultPool(GameTestHelper Helper) {
        floor(Helper, 1);
        ServerLevel World = Helper.getLevel();
        expectDefault(Helper, host(Helper, 5.5, 5.5), Host -> Host.hurt(World.damageSources().onFire(), 1000.0f), "fire");
        expectDefault(Helper, host(Helper, 12.5, 5.5), Host -> {
            Scp016Affliction.finish(Host);
            return true;
        }, "the affliction running its course");
        Zombie Biter = Helper.spawn(EntityType.ZOMBIE, new Vec3(18.5, 2.0, 18.5));
        Biter.setNoAi(true);
        expectDefault(Helper, host(Helper, 18.5, 5.5), Host -> Host.hurt(World.damageSources().mobAttack(Biter), 1000.0f), "a zombie");
        Scp016Skitter Killer = raise(Helper, Scp016Registry.SkitterType.get(), 12.5, 2.0, 18.5, true);
        Villager Victim = Helper.spawn(EntityType.VILLAGER, new Vec3(5.5, 2.0, 14.5));
        Victim.setNoAi(true);
        Vec3 At = Victim.position();
        Victim.hurt(World.damageSources().mobAttack(Killer), 1000.0f);
        List<Scp016Spawn> Found = risenNear(Helper, At);
        Helper.assertTrue(Found.size() == 1 && (Found.get(0) instanceof Scp016Brute || Found.get(0) instanceof Scp016Skitter || Found.get(0) instanceof Scp016Slinger),
                "a kill by an SCP-016-1 instance did not raise one default form");
        Biter.discard();
        clear(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp016_glutton")
    public static void gluttonEatsHealsAndGrowsToItsCap(GameTestHelper Helper) {
        floor(Helper, 1);
        Scp016Glutton Eater = raise(Helper, Scp016Registry.GluttonType.get(), 8.5, 2.0, 8.5, false);
        Eater.setHealth(20.0f);
        float Before = Eater.getScale();
        Pig First = Helper.spawn(EntityType.PIG, new Vec3(11.5, 2.0, 8.5));
        First.setNoAi(true);
        List<Chicken> Second = new ArrayList<>();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(First.isRemoved(), "the glutton has not eaten the pig"))
                .thenExecute(() -> {
                    Helper.assertTrue(Eater.growth() == 1, "one meal left the glutton at growth " + Eater.growth());
                    Helper.assertTrue(Eater.getHealth() > 20.0f, "the meal did not heal the glutton: " + Eater.getHealth());
                    Helper.assertTrue(Eater.getScale() > Before + 0.04f, "the glutton did not grow: " + Before + " to " + Eater.getScale());
                    Eater.setGrowth(Scp016Glutton.GrowthCap);
                    Eater.setDigest(0);
                    Chicken Next = Helper.spawn(EntityType.CHICKEN, new Vec3(8.5, 2.0, 11.5));
                    Next.setNoAi(true);
                    Second.add(Next);
                })
                .thenWaitUntil(() -> Helper.assertTrue(!Second.isEmpty() && Second.get(0).isRemoved(), "the glutton has not eaten the chicken"))
                .thenExecute(() -> {
                    Helper.assertTrue(Eater.growth() == Scp016Glutton.GrowthCap, "growth went past the cap: " + Eater.growth());
                    double Capped = Scp016Glutton.BaseScale + Scp016Glutton.GrowthCap * Scp016Glutton.ScaleStep;
                    Helper.assertTrue(Math.abs(Eater.getScale() - Capped) < 1.0E-3, "the capped glutton has scale " + Eater.getScale() + " instead of " + Capped);
                    clear(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp016_swimmer")
    public static void swimmerSwimsAndLastsOnLand(GameTestHelper Helper) {
        floor(Helper, 3);
        for (int X = 2; X <= 17; X++) {
            for (int Z = 6; Z <= 12; Z++) {
                boolean Rim = X == 2 || X == 17 || Z == 6 || Z == 12;
                Helper.setBlock(new BlockPos(X, 0, Z), Blocks.STONE.defaultBlockState());
                for (int Y = 1; Y <= 3; Y++) {
                    if (!Rim) {
                        Helper.setBlock(new BlockPos(X, Y, Z), Blocks.WATER.defaultBlockState());
                    } else if (Y < 3) {
                        Helper.setBlock(new BlockPos(X, Y, Z), Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }
        Scp016Swimmer Diver = raise(Helper, Scp016Registry.SwimmerType.get(), 4.5, 1.5, 9.5, false);
        Scp016Swimmer Beached = raise(Helper, Scp016Registry.SwimmerType.get(), 9.5, 4.0, 18.5, false);
        Vec3 Shore = Helper.absoluteVec(new Vec3(9.5, 3.5, 11.5));
        double Away = Beached.position().distanceTo(Shore);
        List<Vec3> Trail = new ArrayList<>();
        double[] Swum = {0.0};
        Helper.startSequence().thenIdle(2).thenExecute(() -> {
            Vec3 Goal = Helper.absoluteVec(new Vec3(15.5, 2.0, 9.5));
            Diver.getNavigation().moveTo(Goal.x, Goal.y, Goal.z, 1.0);
            Trail.add(Diver.position());
        }).thenWaitUntil(() -> {
            Vec3 Now = Diver.position();
            Swum[0] += Now.subtract(Trail.get(Trail.size() - 1)).horizontalDistance();
            Trail.add(Now);
            Helper.assertTrue(Diver.isInWater(), "the swimmer left the water");
            Helper.assertTrue(Swum[0] >= 6.0, "the swimmer has only swum " + Swum[0] + " blocks");
        }).thenIdle(100).thenExecute(() -> {
            Helper.assertTrue(Beached.isAlive(), "the beached swimmer died");
            Helper.assertTrue(Beached.getHealth() >= Beached.getMaxHealth() - 0.01f, "the beached swimmer lost health within 11 s: " + Beached.getHealth());
            double Now = Beached.position().distanceTo(Shore);
            Helper.assertTrue(Beached.isInWater() || Now <= Away - 1.0, "the beached swimmer did not head for water: " + Away + " to " + Now);
            clear(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp016_flyer")
    public static void flyerTakesOffAndSwoops(GameTestHelper Helper) {
        floor(Helper, 1);
        Villager Prey = Helper.spawn(EntityType.VILLAGER, new Vec3(12.5, 2.0, 12.5));
        Prey.setNoAi(true);
        Scp016Flyer Hunter = raise(Helper, Scp016Registry.FlyerType.get(), 6.5, 2.0, 12.5, false);
        double Ground = Helper.absoluteVec(new Vec3(0.0, 2.0, 0.0)).y;
        Hunter.setTarget(Prey);
        List<Boolean> Flew = new ArrayList<>();
        Helper.startSequence()
                .thenWaitUntil(() -> {
                    if (Hunter.getY() > Ground + 3.0) {
                        Flew.add(true);
                    }
                    Helper.assertTrue(!Flew.isEmpty(), "the flyer never climbed three blocks");
                })
                .thenWaitUntil(() -> Helper.assertTrue(Hunter.swoops() >= 1, "the flyer has not struck from a dive"))
                .thenExecute(() -> {
                    Helper.assertTrue(Prey.getHealth() < Prey.getMaxHealth(), "the dive did not hurt its prey");
                    Helper.assertTrue(Scp016Affliction.isAfflicted(Prey), "the dive did not pass SCP-016 on");
                    Prey.discard();
                    clear(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 320, batch = "scp016_control")
    public static void controlledDrowningRaisesOneSwimmer(GameTestHelper Helper) {
        floor(Helper, 1);
        DClass Body = body(Helper, 12.5, 10.5);
        ServerPlayer User = survivor(Helper, 14.5, 2.0, 10.5);
        Helper.assertTrue(ControlManager.possess(User, Body), "the controller could not take the D-Class");
        Helper.assertTrue(Scp016Affliction.afflict(User), "the controlled D-Class could not be afflicted");
        List<Vec3> Spot = new ArrayList<>();
        Helper.startSequence().thenIdle(SpawnShield).thenExecute(() -> {
            Spot.add(User.position());
            Helper.assertTrue(User.hurt(User.damageSources().drown(), 1000.0f) || ControlManager.hasPendingDeath(User), "the drowning damage did not land on the controlled body");
            Helper.assertTrue(risenNear(Helper, Spot.get(0)).isEmpty(), "an instance rose before the deferred death was processed");
        }).thenWaitUntil(() -> Helper.assertTrue(!ControlSessions.isControlling(User), "the controller is still linked to the drowned body"))
                .thenIdle(3).thenExecute(() -> {
                    List<Scp016Spawn> Found = risenNear(Helper, Spot.get(0));
                    Helper.assertTrue(Found.size() == 1, "expected one instance from the drowned D-Class, found " + Found.size());
                    Helper.assertTrue(Found.get(0) instanceof Scp016Swimmer, "the drowned D-Class raised " + Found.get(0).getClass().getSimpleName() + " instead of a swimmer");
                    Helper.assertTrue(User.isAlive() && !User.isDeadOrDying(), "the controller died with the body");
                    Helper.assertTrue(!Scp016Affliction.isAfflicted(User), "the controller kept the body's affliction");
                    dismiss(User);
                    clear(Helper);
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp016_cap")
    public static void spawnCapStillHolds(GameTestHelper Helper) {
        floor(Helper, 1);
        ServerLevel World = Helper.getLevel();
        int Need = Scp016Events.SpawnCap - Scp016Events.countSpawn(World);
        for (int Index = 0; Index < Need; Index++) {
            raise(Helper, Scp016Registry.SkitterType.get(), 2.5 + (Index % 6) * 3.0, 2.0, 2.5 + (Index / 6) * 3.0, true);
        }
        Helper.assertTrue(Scp016Events.countSpawn(World) >= Scp016Events.SpawnCap, "could not fill the dimension to the cap");
        Villager Host = host(Helper, 20.5, 20.5);
        Vec3 At = Host.position();
        int Before = Scp016Events.countSpawn(World);
        Host.hurt(World.damageSources().drown(), 1000.0f);
        Helper.assertTrue(Host.isDeadOrDying(), "the host did not die");
        Helper.assertTrue(risenNear(Helper, At).isEmpty(), "an instance rose past the cap");
        Helper.assertTrue(Scp016Events.countSpawn(World) == Before, "the instance count changed at the cap: " + Before + " to " + Scp016Events.countSpawn(World));
        clear(Helper);
        Helper.succeed();
    }
}
