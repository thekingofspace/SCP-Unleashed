package net.rai.scp_unleashed.scp343.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.assembler.AssemblingInput;
import net.rai.scp_unleashed.assembler.AssemblingRecipe;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlRoster;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.furnishing.FurnishingSeat;
import net.rai.scp_unleashed.scp343.Scp343Away;
import net.rai.scp_unleashed.scp343.Scp343Blessings;
import net.rai.scp_unleashed.scp343.Scp343Data;
import net.rai.scp_unleashed.scp343.Scp343God;
import net.rai.scp_unleashed.scp343.Scp343Judgement;
import net.rai.scp_unleashed.scp343.Scp343Registry;
import net.rai.scp_unleashed.scp343.Scp343ThroneBlock;
import net.rai.scp_unleashed.scp343.Scp343Walls;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp343GameTests {
    public static final String Arena = "scp343_arena";
    public static final long ContainedSeed = 343L;

    private Scp343GameTests() {
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
        ServerPlayer User = new ServerPlayer(Server, World, new GameProfile(UUID.randomUUID(), "scp343_test"), ClientInformation.createDefault()) {
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
        MinecraftServer Server = User.server;
        enrol(Server, User, false);
        if (!User.isRemoved()) {
            User.serverLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static Scp343God god(GameTestHelper Helper, double X, double Y, double Z) {
        ServerLevel World = Helper.getLevel();
        Scp343God God = Scp343Registry.God.get().create(World);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        God.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        God.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.COMMAND, null);
        World.addFreshEntity(God);
        return God;
    }

    private static void room(GameTestHelper Helper, int MinX, int MinZ, int MaxX, int MaxZ, int Top) {
        for (int X = MinX; X <= MaxX; X++) {
            for (int Z = MinZ; Z <= MaxZ; Z++) {
                boolean Rim = X == MinX || X == MaxX || Z == MinZ || Z == MaxZ;
                for (int Y = 2; Y <= Top; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Rim || Y == Top ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void pillars(GameTestHelper Helper, int Low, int High, int SkipX, int SkipZ) {
        for (int X = Low; X <= High; X += 3) {
            for (int Z = Low; Z <= High; Z += 3) {
                if (Math.abs(X - SkipX) <= 3 && Math.abs(Z - SkipZ) <= 3) {
                    continue;
                }
                Helper.setBlock(new BlockPos(X, 2, Z), Blocks.STONE.defaultBlockState());
                Helper.setBlock(new BlockPos(X, 3, Z), Blocks.STONE.defaultBlockState());
            }
        }
    }

    private static int giftsOn(Player User) {
        return Scp343Blessings.giftsOn(User);
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp343_talk")
    public static void talkBlessesOnceThenOnlySpeaks(GameTestHelper Helper) {
        Scp343God God = god(Helper, 10.5, 2.0, 10.5);
        ServerPlayer User = survivor(Helper, 12.5, 2.0, 10.5);
        User.removeAllEffects();
        God.listen(User);
        int First = giftsOn(User);
        Helper.assertTrue(First >= Scp343Blessings.Fewest && First <= Scp343Blessings.Most, "talking to SCP-343 gave " + First + " gifts instead of 2 or 3");
        Helper.assertTrue(!Scp343Blessings.ready(User), "the blessing cooldown did not start");
        User.removeAllEffects();
        Helper.startSequence().thenExecuteAfter(50, () -> {
            God.listen(User);
            Helper.assertTrue(giftsOn(User) == 0, "a second talk during the cooldown gave " + giftsOn(User) + " more gifts");
        }).thenExecute(() -> {
            dismiss(User);
            God.discard();
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp343_immune")
    public static void takesNoDamageButKill(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp343God God = god(Helper, 10.5, 2.0, 10.5);
        Zombie Attacker = Helper.spawn(EntityType.ZOMBIE, new Vec3(12.5, 2.0, 10.5));
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        DamageSources Sources = World.damageSources();
        List<DamageSource> Tried = List.of(Sources.generic(), Sources.mobAttack(Attacker), Sources.playerAttack(User), Sources.onFire(), Sources.inFire(), Sources.lava(),
                Sources.fall(), Sources.drown(), Sources.explosion(null, null), Sources.magic(), Sources.wither(), Sources.freeze(), Sources.inWall(), Sources.cactus(),
                Sources.sonicBoom(Attacker), Sources.starve(), Sources.lightningBolt(), Sources.thorns(Attacker), Sources.dragonBreath(), Sources.hotFloor());
        float Full = God.getHealth();
        for (DamageSource Source : Tried) {
            boolean Hit = God.hurt(Source, 1000.0f);
            Helper.assertTrue(!Hit, "SCP-343 took " + Source.getMsgId() + " damage");
            Helper.assertTrue(God.getHealth() == Full && God.isAlive(), "SCP-343 lost health to " + Source.getMsgId());
        }
        Helper.assertTrue(God.getTarget() == null, "SCP-343 picked a target");
        Attacker.setTarget(God);
        Helper.assertTrue(Attacker.getTarget() != God, "a zombie was allowed to target SCP-343");
        Attacker.discard();
        God.kill();
        Helper.succeedWhen(() -> Helper.assertTrue(God.isDeadOrDying() || God.isRemoved(), "the kill command did not kill SCP-343"));
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp343_stasis")
    public static void stasisPodRefuses(GameTestHelper Helper) {
        Scp343God God = god(Helper, 10.5, 2.0, 10.5);
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Component Refusal = StasisPods.refusal(User, Pod, God);
        Helper.assertTrue(Refusal != null, "a stasis pod would take SCP-343");
        Helper.assertTrue(Refusal.getContents() instanceof TranslatableContents Text && Text.getKey().equals("message.scp_unleashed.stasis.immune"),
                "the pod refusal was not the normal immune message: " + Refusal.getString());
        Helper.assertTrue(!StasisPods.capture(Helper.getLevel(), User, Pod, God), "the pod captured SCP-343");
        Helper.assertTrue(God.isAlive() && !God.isRemoved(), "SCP-343 vanished into the pod");
        God.discard();
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp343_contained")
    public static void containedWalksIntoWallAndReturnsElsewhere(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        AABB Bounds = Helper.getBounds();
        Predicate<BlockPos> Inside = Pos -> Bounds.contains(Vec3.atCenterOf(Pos));
        RandomSource Seeded = RandomSource.create(ContainedSeed);
        pillars(Helper, 14, 39, 32, 32);
        room(Helper, 7, 7, 11, 11, 5);
        Scp343God God = god(Helper, 9.5, 2.0, 9.5);
        UUID Id = God.getUUID();
        ServerPlayer User = survivor(Helper, 32.5, 2.0, 32.5);
        Vec3 Room = Helper.absoluteVec(new Vec3(9.5, 2.0, 9.5));
        Scp343God[] Back = new Scp343God[1];
        Helper.startSequence().thenExecuteAfter(2, () -> {
            Helper.assertTrue(God.checkContainmentNow(), "SCP-343 did not notice he was sealed in a room");
            Helper.assertTrue(God.isLeaving(), "SCP-343 is not heading for a wall");
        }).thenWaitUntil(() -> Helper.assertTrue(God.isRemoved(), "SCP-343 has not stepped into the wall yet")).thenExecute(() -> {
            Scp343Data Data = Scp343Data.get(World.getServer());
            Scp343Data.Away Record = Data.away(Id);
            Helper.assertTrue(Record != null, "no away record was kept for SCP-343");
            Helper.assertTrue(Record.ReturnAt() - World.getServer().overworld().getGameTime() >= Scp343Away.ShortestAway - 40, "SCP-343 is due back too soon");
            long Now = World.getServer().overworld().getGameTime();
            Helper.assertTrue(Scp343Away.bringBack(World.getServer(), Data, Record, Now, Seeded, Inside), "SCP-343 found nowhere to come back");
            Helper.assertTrue(Data.away(Id) == null, "the away record was not cleared on return");
            Entity Found = World.getEntity(Id);
            Helper.assertTrue(Found instanceof Scp343God, "SCP-343 did not reappear in this level");
            Back[0] = (Scp343God) Found;
            Helper.assertTrue(Back[0].position().distanceTo(Room) > 3.0, "SCP-343 reappeared inside the sealed room");
            Helper.assertTrue(Back[0].isEmerging() && Scp343Walls.solid(World, Back[0].blockPosition()), "SCP-343 did not step out of a wall when he came back");
            Helper.assertTrue(Inside.test(Back[0].blockPosition()) && World.isPositionEntityTicking(Back[0].blockPosition()), "SCP-343 came back somewhere that does not tick");
        }).thenWaitUntil(() -> Helper.assertTrue(Back[0] != null && !Back[0].isPhasing(), "SCP-343 is still stepping out")).thenExecute(() -> {
            dismiss(User);
            Back[0].discard();
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp343_throne")
    public static void throneKeepsHimAndHeSits(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        room(Helper, 4, 4, 12, 12, 6);
        BlockState Lower = Scp343Registry.Throne.get().defaultBlockState().setValue(Scp343ThroneBlock.Facing, Direction.NORTH).setValue(Scp343ThroneBlock.Half, DoubleBlockHalf.LOWER);
        Helper.setBlock(new BlockPos(8, 2, 10), Lower);
        Helper.setBlock(new BlockPos(8, 3, 10), Lower.setValue(Scp343ThroneBlock.Half, DoubleBlockHalf.UPPER));
        BlockPos Throne = Helper.absolutePos(new BlockPos(8, 2, 10));
        Scp343God God = god(Helper, 8.5, 2.0, 6.5);
        Helper.startSequence().thenExecuteAfter(2, () -> {
            Helper.assertTrue(Scp343Data.get(World.getServer()).thrones(World.dimension()).contains(Throne), "the throne was not recorded");
        }).thenWaitUntil(() -> Helper.assertTrue(God.thronePos() != null, "SCP-343 has not noticed the throne")).thenExecute(() -> {
            Helper.assertTrue(!God.checkContainmentNow(), "SCP-343 still wants to leave a sealed room with a throne in it");
            Helper.assertTrue(!God.isLeaving() && !God.isRemoved(), "SCP-343 is leaving despite the throne");
        }).thenWaitUntil(() -> Helper.assertTrue(God.isSeated(), "SCP-343 has not sat on the throne")).thenExecute(() -> {
            Helper.assertTrue(God.getVehicle() instanceof FurnishingSeat Seat && Seat.blockPosition().equals(Throne), "SCP-343 sat somewhere other than the throne");
            Helper.assertTrue(!God.isRemoved() && Scp343Data.get(World.getServer()).away(God.getUUID()) == null, "SCP-343 vanished");
            God.stopRiding();
            God.discard();
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp343_murder")
    public static void killingVillagerNearHimIsJudged(GameTestHelper Helper) {
        Scp343God God = god(Helper, 10.5, 2.0, 10.5);
        Villager Victim = Helper.spawn(EntityType.VILLAGER, new Vec3(14.5, 2.0, 10.5));
        ServerPlayer User = survivor(Helper, 13.5, 2.0, 12.5);
        Victim.hurt(User.damageSources().playerAttack(User), 1000.0f);
        Helper.assertTrue(Scp343Judgement.pending(User.getUUID()), "the murder was not noticed");
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(User.isDeadOrDying(), "the murderer is still alive")).thenExecute(() -> {
            Helper.assertTrue(User.getLastDamageSource() != null && User.getLastDamageSource().is(Scp343Registry.Judged), "the murderer died of something else");
            dismiss(User);
            God.discard();
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp343_zombie")
    public static void killingZombieIsNotMurder(GameTestHelper Helper) {
        Scp343God God = god(Helper, 10.5, 2.0, 10.5);
        Zombie Target = Helper.spawn(EntityType.ZOMBIE, new Vec3(14.5, 2.0, 10.5));
        ServerPlayer User = survivor(Helper, 13.5, 2.0, 12.5);
        Target.hurt(User.damageSources().playerAttack(User), 1000.0f);
        Helper.assertTrue(!Scp343Judgement.pending(User.getUUID()), "killing a zombie was judged");
        Helper.startSequence().thenExecuteAfter(Scp343Judgement.FadeTicks + 20, () -> {
            Helper.assertTrue(User.isAlive() && !User.isDeadOrDying(), "the zombie slayer died");
            dismiss(User);
            God.discard();
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "scp343_control")
    public static void controlledMurdererBodyDiesControllerLives(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp343God God = god(Helper, 8.5, 2.0, 10.5);
        DClass Body = DClassRegistry.DClassType.get().create(World);
        Vec3 BodySpot = Helper.absoluteVec(new Vec3(12.5, 2.0, 10.5));
        Body.moveTo(BodySpot.x, BodySpot.y, BodySpot.z, 0.0f, 0.0f);
        Body.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(BodySpot)), MobSpawnType.COMMAND, null);
        World.addFreshEntity(Body);
        ServerPlayer User = survivor(Helper, 14.5, 2.0, 10.5);
        Helper.assertTrue(ControlManager.possess(User, Body), "the controller could not take the D-Class");
        UUID BodyId = ControlSessions.session(User).BodyId();
        Villager Victim = Helper.spawn(EntityType.VILLAGER, new Vec3(13.5, 2.0, 12.5));
        Victim.hurt(User.damageSources().playerAttack(User), 1000.0f);
        Helper.assertTrue(Scp343Judgement.pending(User.getUUID()), "the controlled murder was not noticed");
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(!ControlSessions.isControlling(User), "the controller is still linked to the murderer's body"))
                .thenIdle(5).thenExecute(() -> {
                    Helper.assertTrue(User.isAlive() && !User.isDeadOrDying() && User.getHealth() > 0.0f, "the controller died with the body");
                    Helper.assertTrue(ControlRoster.get(World.getServer()).status(BodyId) == ControlRoster.BodyStatus.Deceased, "the D-Class body did not die");
                    Entity Left = World.getEntity(BodyId);
                    Helper.assertTrue(Left == null || !Left.isAlive(), "the D-Class body is still standing");
                    dismiss(User);
                    God.discard();
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp343_recipe")
    public static void assemblerMakesTheThrone(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "assembling/scp343_throne");
        RecipeHolder<?> Holder = World.getRecipeManager().byKey(Id).orElse(null);
        Helper.assertTrue(Holder != null, "the throne recipe is missing");
        Helper.assertTrue(Holder.value() instanceof AssemblingRecipe, "the throne recipe is not an assembler recipe");
        AssemblingRecipe Recipe = (AssemblingRecipe) Holder.value();
        Helper.assertTrue(Recipe.getResult().is(Scp343Registry.ThroneItem.get()), "the recipe makes " + Recipe.getResult() + " instead of the throne");
        List<ItemStack> Inputs = List.of(new ItemStack(Items.QUARTZ_BLOCK, 24), new ItemStack(Items.GOLD_BLOCK, 6), new ItemStack(Items.AMETHYST_SHARD, 12),
                new ItemStack(Items.WHITE_WOOL, 6), new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gold_plate")), 8),
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "energum_alloy_ingot")), 4),
                new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "unstable_fragment_severe")), 1));
        AssemblingInput Input = new AssemblingInput(Inputs);
        Helper.assertTrue(Recipe.matches(Input, World), "the listed ingredients do not satisfy the throne recipe");
        Helper.assertTrue(Recipe.assemble(Input, World.registryAccess()).is(Scp343Registry.ThroneItem.get()), "assembling did not produce the throne");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp343_away")
    public static void awayStateSurvivesSaveAndReload(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Scp343God God = god(Helper, 10.5, 2.0, 10.5);
        UUID Id = God.getUUID();
        Scp343Away.depart(God);
        Helper.assertTrue(God.isRemoved(), "SCP-343 did not leave the world");
        Scp343Data Data = Scp343Data.get(Server);
        Scp343Data.Away Record = Data.away(Id);
        Helper.assertTrue(Record != null, "no away record");
        CompoundTag Saved = Data.save(new CompoundTag(), World.registryAccess());
        Scp343Data Loaded = Scp343Data.load(Saved, World.registryAccess());
        Scp343Data.Away Again = Loaded.away(Id);
        Helper.assertTrue(Again != null, "the away record was lost in a save and reload");
        Helper.assertTrue(Again.From().equals(Record.From()) && Again.FromPos().equals(Record.FromPos()) && Again.ReturnAt() == Record.ReturnAt(), "the away record changed in a save and reload");
        Helper.assertTrue(Again.Body().getString("id").equals("scp_unleashed:scp343") && Again.Body().getUUID("UUID").equals(Id), "the stored body lost its identity");
        BlockPos Stand = Helper.absolutePos(new BlockPos(20, 2, 20));
        Scp343Away.Plan Target = new Scp343Away.Plan(World, new Scp343Walls.Emergence(Stand, Stand), null);
        Helper.assertTrue(Scp343Away.emerge(Server, Loaded, Again, Target), "SCP-343 could not come back from the reloaded record");
        Data.returned(Id);
        Entity Found = World.getEntity(Id);
        Helper.assertTrue(Found instanceof Scp343God, "the returned SCP-343 is not the same entity");
        Helper.startSequence().thenIdle(40).thenExecute(() -> {
            Helper.assertTrue(Found.isAlive() && !((Scp343God) Found).isPhasing(), "the returned SCP-343 did not finish stepping out");
            Found.discard();
        }).thenSucceed();
    }
}
