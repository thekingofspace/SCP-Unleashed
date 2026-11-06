package net.rai.scp_unleashed.scp354.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.pressure.PressureNetwork;
import net.rai.scp_unleashed.scp354.Scp354Basin;
import net.rai.scp_unleashed.scp354.Scp354Beast;
import net.rai.scp_unleashed.scp354.Scp354Emergence;
import net.rai.scp_unleashed.scp354.Scp354Emergent;
import net.rai.scp_unleashed.scp354.Scp354Fluid;
import net.rai.scp_unleashed.scp354.Scp354Grab;
import net.rai.scp_unleashed.scp354.Scp354Grasp;
import net.rai.scp_unleashed.scp354.Scp354Pools;
import net.rai.scp_unleashed.scp354.Scp354Prey;
import net.rai.scp_unleashed.scp354.Scp354Registry;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntBinaryOperator;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp354GameTests {
    public static final String Arena = "scp354_arena";
    public static final int Size = 40;
    public static final int Middle = 20;
    public static final int Quiet = 1000000;
    public static final String DumpVariable = "SCP354_DUMP";
    private static final Map<UUID, Integer> Deaths = new ConcurrentHashMap<>();
    private static final Map<UUID, String> Causes = new ConcurrentHashMap<>();
    private static final Set<UUID> Tracked = ConcurrentHashMap.newKeySet();

    private Scp354GameTests() {
    }

    @EventBusSubscriber(modid = Scp_unleashed.ModId)
    public static final class Watch {
        private Watch() {
        }

        @SubscribeEvent
        public static void onDeath(LivingDeathEvent Event) {
            LivingEntity Fallen = Event.getEntity();
            if (Fallen.level().isClientSide || !Tracked.contains(Fallen.getUUID())) {
                return;
            }
            Deaths.merge(Fallen.getUUID(), 1, Integer::sum);
            Causes.put(Fallen.getUUID(), Event.getSource().typeHolder().unwrapKey().map(Key -> Key.location().toString()).orElse("?"));
        }
    }

    private static void terrain(GameTestHelper Helper, IntBinaryOperator Height) {
        for (int X = 0; X < Size; X++) {
            for (int Z = 0; Z < Size; Z++) {
                int Top = Height.applyAsInt(X, Z);
                for (int Y = 1; Y <= Top; Y++) {
                    BlockState State = Y == Top ? Blocks.GRASS_BLOCK.defaultBlockState() : Y > Top - 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
                    Helper.getLevel().setBlock(Helper.absolutePos(new BlockPos(X, Y, Z)), State, Block.UPDATE_CLIENTS);
                }
                if (Top < 18 && (X * 7 + Z * 3) % 11 == 0) {
                    Helper.getLevel().setBlock(Helper.absolutePos(new BlockPos(X, Top + 1, Z)), Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static Scp354Basin.Result pool(GameTestHelper Helper, BlockPos Centre, long Seed) {
        return Scp354Basin.build(Helper.getLevel(), Helper.absolutePos(Centre), RandomSource.create(Seed));
    }

    private static void dump(GameTestHelper Helper, String Name, Scp354Basin.Result Built) {
        String Folder = System.getenv(DumpVariable);
        if (Folder == null || Folder.isEmpty()) {
            return;
        }
        BlockPos Origin = Helper.absolutePos(BlockPos.ZERO);
        StringBuilder Text = new StringBuilder("{\"name\": \"").append(Name).append("\", \"centre\": [").append(Built.Shape().CentreX() - Origin.getX()).append(", ")
                .append(Built.Shape().Surface() - Origin.getY()).append(", ").append(Built.Shape().CentreZ() - Origin.getZ()).append("], \"exposed\": ")
                .append(Built.Exposed().size()).append(", \"blocks\": [");
        boolean First = true;
        for (int X = 0; X < Size; X++) {
            for (int Z = 0; Z < Size; Z++) {
                for (int Y = 0; Y < 22; Y++) {
                    BlockState State = Helper.getLevel().getBlockState(Origin.offset(X, Y, Z));
                    if (State.isAir()) {
                        continue;
                    }
                    if (!First) {
                        Text.append(", ");
                    }
                    First = false;
                    Text.append("[").append(X).append(", ").append(Y).append(", ").append(Z).append(", \"").append(BuiltInRegistries.BLOCK.getKey(State.getBlock())).append("\"]");
                }
            }
        }
        Text.append("]}\n");
        try {
            Path Target = Path.of(Folder, Name + ".json");
            Files.createDirectories(Target.getParent());
            Files.writeString(Target, Text.toString(), StandardCharsets.UTF_8);
        } catch (IOException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    private static void checkPool(GameTestHelper Helper, Scp354Basin.Result Built, String Label) {
        Helper.assertTrue(Built.Exposed().isEmpty(), Label + ": " + Built.Exposed().size() + " liquid blocks are exposed, first " + (Built.Exposed().isEmpty() ? "" : Built.Exposed().get(0)));
        Set<BlockPos> Found = Scp354Basin.scan(Helper.getLevel(), Built.Shape().centre(), Scp354Basin.Window + 1, Scp354Basin.MaxDepth + 2, 2);
        Helper.assertTrue(!Found.isEmpty() && Found.size() == Built.Liquid().size(), Label + ": world holds " + Found.size() + " liquid blocks, the build placed " + Built.Liquid().size());
        List<BlockPos> Exposed = Scp354Basin.exposed(Helper.getLevel(), Found);
        Helper.assertTrue(Exposed.isEmpty(), Label + ": the world check finds " + Exposed.size() + " exposed liquid blocks, first " + (Exposed.isEmpty() ? "" : Exposed.get(0)));
        int Wide = Built.Shape().width(true);
        int Deep = Built.Shape().width(false);
        Helper.assertTrue(Wide >= 5 && Wide <= 8 && Deep >= 5 && Deep <= 8, Label + ": basin is " + Wide + " by " + Deep);
        int Depth = Built.Shape().deepest();
        Helper.assertTrue(Depth >= 3 && Depth <= 4, Label + ": basin is " + Depth + " deep");
        for (BlockPos Cell : Found) {
            BlockState Above = Helper.getLevel().getBlockState(Cell.above());
            Helper.assertTrue(Scp354Fluid.isRed(Above.getFluidState()) || !Above.isSolid() || Above.isAir(), Label + ": liquid at " + Cell + " is roofed by " + Above);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp354_poolFlat")
    public static void poolFlat(GameTestHelper Helper) {
        terrain(Helper, (X, Z) -> 6);
        Scp354Basin.Result Built = pool(Helper, new BlockPos(Middle, 6, Middle), 354L);
        checkPool(Helper, Built, "flat");
        dump(Helper, "pool_flat", Built);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp354_poolSlope")
    public static void poolSlope(GameTestHelper Helper) {
        terrain(Helper, (X, Z) -> 2 + (int) Math.floor(X * 0.45 + Z * 0.12));
        int Ground = 2 + (int) Math.floor(Middle * 0.45 + Middle * 0.12);
        Scp354Basin.Result Built = pool(Helper, new BlockPos(Middle, Ground, Middle), 3541L);
        checkPool(Helper, Built, "slope");
        dump(Helper, "pool_slope", Built);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp354_poolCliff")
    public static void poolCliff(GameTestHelper Helper) {
        terrain(Helper, (X, Z) -> X < Middle ? 14 : 3);
        Scp354Basin.Result Built = pool(Helper, new BlockPos(Middle - 2, 14, Middle), 35400L);
        checkPool(Helper, Built, "cliff");
        dump(Helper, "pool_cliff", Built);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp354_liquidStatic")
    public static void liquidStatic(GameTestHelper Helper) {
        BlockPos Pillar = new BlockPos(Middle, 8, Middle);
        for (int Y = 1; Y <= 8; Y++) {
            Helper.setBlock(new BlockPos(Middle, Y, Middle), Blocks.STONE);
        }
        Helper.setBlock(Pillar.above(), Scp354Registry.RedLiquid.get().defaultBlockState());
        Helper.runAfterDelay(5, () -> {
            BlockPos Cell = Helper.absolutePos(Pillar.above());
            ServerLevel World = Helper.getLevel();
            for (int Round = 0; Round < 20; Round++) {
                World.getFluidState(Cell).tick(World, Cell);
                World.updateNeighborsAt(Cell, Blocks.STONE);
            }
        });
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Scp354Fluid.isRed(Helper.getLevel().getFluidState(Helper.absolutePos(Pillar.above()))), "the liquid vanished");
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                BlockPos Next = Pillar.above().relative(Side);
                Helper.assertTrue(Helper.getLevel().getBlockState(Helper.absolutePos(Next)).isAir(), "the liquid spread to " + Next);
                Helper.assertTrue(Helper.getLevel().getBlockState(Helper.absolutePos(Next.below())).isAir(), "the liquid spread down to " + Next.below());
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp354_bucketRefused")
    public static void bucketRefused(GameTestHelper Helper) {
        terrain(Helper, (X, Z) -> 6);
        Scp354Basin.Result Built = pool(Helper, new BlockPos(Middle, 6, Middle), 354L);
        ServerLevel World = Helper.getLevel();
        BlockPos Top = Built.Shape().centre();
        BlockState State = World.getBlockState(Top);
        Helper.assertTrue(State.getBlock() instanceof BucketPickup, "the liquid is not a liquid block");
        ItemStack Picked = ((BucketPickup) State.getBlock()).pickupBlock(null, World, Top, State);
        Helper.assertTrue(Picked.isEmpty(), "a bucket scooped up " + Picked);
        Helper.assertTrue(Scp354Fluid.isRed(World.getFluidState(Top)), "the liquid was removed by a pickup");
        Helper.assertFalse(State.canBeReplaced(Fluids.WATER), "water can replace the liquid");
        Helper.assertFalse(State.canBeReplaced(), "blocks can be placed into the liquid");
        Helper.assertTrue(World.getFluidState(Top).is(PressureNetwork.FinitePool), "pumps would treat the pool as endless");
        Helper.assertFalse(FluidUtil.tryPickUpFluid(new ItemStack(Items.BUCKET), null, World, Top, Direction.UP).isSuccess(), "a fluid handler drew from the pool");
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp354_bucket"));
        User.moveTo(Top.getX() + 0.5, Top.getY() + 2.0, Top.getZ() + 0.5, 0.0f, 90.0f);
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        User.getItemInHand(InteractionHand.MAIN_HAND).use(World, User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(User.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET), "using a bucket on the pool gave " + User.getItemInHand(InteractionHand.MAIN_HAND));
        Helper.assertTrue(Scp354Fluid.isRed(World.getFluidState(Top)), "using a bucket removed the liquid");
        Helper.assertTrue(State.getExplosionResistance(World, Top, null) >= Scp354Registry.Resistance, "the liquid is not explosion proof");
        Helper.assertTrue(State.getDestroySpeed(World, Top) < 0.0f, "the liquid can be mined");
        Helper.succeed();
    }

    private static List<BlockPos> shore(ServerLevel World, BlockPos Centre) {
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Cell : BlockPos.betweenClosed(Centre.offset(-14, -8, -14), Centre.offset(14, 4, 14))) {
            BlockState State = World.getBlockState(Cell);
            if (Scp354Basin.isOwn(State) && State.getFluidState().isEmpty()) {
                Found.add(Cell.immutable());
            }
        }
        return Found;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp354_shoreUnbreakable")
    public static void shoreUnbreakable(GameTestHelper Helper) {
        terrain(Helper, (X, Z) -> 6);
        Scp354Basin.Result Built = pool(Helper, new BlockPos(Middle, 6, Middle), 354L);
        ServerLevel World = Helper.getLevel();
        BlockPos Centre = Built.Shape().centre();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp354_digger"));
        for (Block Ground : List.of(Scp354Registry.StainedEarth.get(), Scp354Registry.StainedStone.get(), Scp354Registry.DeadGrass.get())) {
            BlockState State = Ground.defaultBlockState();
            String Name = BuiltInRegistries.BLOCK.getKey(Ground).getPath();
            for (ItemStack Tool : List.of(new ItemStack(Items.NETHERITE_PICKAXE), new ItemStack(Items.NETHERITE_SHOVEL), ItemStack.EMPTY)) {
                User.setItemInHand(InteractionHand.MAIN_HAND, Tool);
                Helper.assertTrue(State.getDestroyProgress(User, World, Centre) <= 0.0f, Name + " can be mined with " + Tool);
            }
            Helper.assertTrue(State.getDestroySpeed(World, Centre) < 0.0f, Name + " has a break time");
            Helper.assertTrue(Ground.getExplosionResistance() >= Scp354Registry.Resistance, Name + " is not explosion proof");
            Helper.assertTrue(State.getPistonPushReaction() == PushReaction.BLOCK, Name + " can be pushed by pistons");
            Helper.assertTrue(State.is(BlockTags.WITHER_IMMUNE) && State.is(BlockTags.DRAGON_IMMUNE), Name + " can be broken by bosses");
        }
        List<BlockPos> Before = shore(World, Centre);
        Helper.assertTrue(!Before.isEmpty(), "the pool was built without a shore");
        BlockPos Blast = Before.get(Before.size() / 2);
        World.explode(null, Blast.getX() + 0.5, Blast.getY() + 1.0, Blast.getZ() + 0.5, 6.0f, Level.ExplosionInteraction.TNT);
        List<BlockPos> After = shore(World, Centre);
        Helper.assertTrue(After.equals(Before), "an explosion broke " + (Before.size() - After.size()) + " shore blocks");
        Helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static ServerPlayer person(GameTestHelper Helper, GameType Mode, Vec3 Relative) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        CommonListenerCookie Cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "scp354_test"), false);
        ServerPlayer User = new ServerPlayer(Server, World, Cookie.gameProfile(), Cookie.clientInformation());
        Connection Link = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(Link);
        try {
            Field Registrations = NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
            Registrations.setAccessible(true);
            Map<ConnectionProtocol, Map<ResourceLocation, ?>> All = (Map<ConnectionProtocol, Map<ResourceLocation, ?>>) Registrations.get(null);
            ChannelAttributes.getOrCreateCommonChannels(Link, ConnectionProtocol.PLAY).addAll(All.getOrDefault(ConnectionProtocol.PLAY, Map.of()).keySet());
        } catch (ReflectiveOperationException Problem) {
            throw new IllegalStateException(Problem);
        }
        Server.getPlayerList().placeNewPlayer(Link, User, Cookie);
        Helper.onEachTick(() -> {
            if (!User.hasDisconnected()) {
                User.connection.chunkSender.onChunkBatchReceivedByClient(64.0f);
            }
        });
        User.setGameMode(Mode);
        Vec3 Spot = Helper.absoluteVec(Relative);
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.hasDisconnected()) {
            Helper.getLevel().getServer().getPlayerList().remove(User);
        }
    }

    private static Scp354Pools.Pool watched(GameTestHelper Helper, Scp354Basin.Result Built) {
        ServerLevel World = Helper.getLevel();
        return Scp354Pools.get(World.getServer()).add(new Scp354Pools.Pool(World.dimension(), Built.Shape().centre(), Built.Shape().radius(), Built.Shape().deepest(), Quiet,
                World.getGameTime(), 0));
    }

    private static void forget(GameTestHelper Helper, Scp354Pools.Pool Entry) {
        Scp354Pools.get(Helper.getLevel().getServer()).remove(Entry);
    }

    private static Vec3 into(GameTestHelper Helper, Scp354Basin.Result Built) {
        BlockPos Centre = Built.Shape().centre();
        BlockPos Origin = Helper.absolutePos(BlockPos.ZERO);
        return new Vec3(Centre.getX() - Origin.getX() + 0.5, Centre.getY() - Origin.getY() + 0.2, Centre.getZ() - Origin.getZ() + 0.5);
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp354_playerTaken")
    public static void playerTaken(GameTestHelper Helper) {
        terrain(Helper, (X, Z) -> 6);
        Scp354Basin.Result Built = pool(Helper, new BlockPos(Middle, 6, Middle), 354L);
        Scp354Pools.Pool Entry = watched(Helper, Built);
        ServerPlayer User = person(Helper, GameType.SURVIVAL, into(Helper, Built));
        Tracked.add(User.getUUID());
        long Start = Helper.getLevel().getGameTime();
        long[] Died = {-1L};
        Helper.onEachTick(() -> {
            if (Died[0] < 0 && User.isDeadOrDying()) {
                Died[0] = Helper.getLevel().getGameTime();
            }
        });
        Helper.runAfterDelay(80, () -> {
            try {
                Helper.assertTrue(Died[0] >= 0, "the player was still alive after 4 seconds in the pool");
                long Took = Died[0] - Start;
                Helper.assertTrue(Took <= 60, "the player took " + Took + " ticks to die");
                Helper.assertTrue("scp_unleashed:scp354_taken".equals(Causes.get(User.getUUID())), "the player died of " + Causes.get(User.getUUID()));
                Helper.assertTrue(Deaths.getOrDefault(User.getUUID(), 0) == 1, "the player died " + Deaths.get(User.getUUID()) + " times");
                Helper.succeed();
            } finally {
                dismiss(Helper, User);
                forget(Helper, Entry);
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp354_creativeImmune")
    public static void creativeImmune(GameTestHelper Helper) {
        terrain(Helper, (X, Z) -> 6);
        Scp354Basin.Result Built = pool(Helper, new BlockPos(Middle, 6, Middle), 354L);
        Scp354Pools.Pool Entry = watched(Helper, Built);
        ServerPlayer User = person(Helper, GameType.CREATIVE, into(Helper, Built));
        Helper.runAfterDelay(80, () -> {
            try {
                Helper.assertTrue(User.isAlive() && !User.isDeadOrDying(), "a creative player died in the pool");
                Helper.assertFalse(Scp354Grab.isSeized(User), "a creative player was seized");
                AABB Area = new AABB(Built.Shape().centre()).inflate(8.0);
                Helper.assertTrue(Helper.getLevel().getEntitiesOfClass(Scp354Grasp.class, Area).isEmpty(), "a tendril rose for a creative player");
                Helper.succeed();
            } finally {
                dismiss(Helper, User);
                forget(Helper, Entry);
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp354_pigTaken")
    public static void pigTaken(GameTestHelper Helper) {
        terrain(Helper, (X, Z) -> 6);
        Scp354Basin.Result Built = pool(Helper, new BlockPos(Middle, 6, Middle), 354L);
        BlockPos Centre = Built.Shape().centre();
        BlockPos Origin = Helper.absolutePos(BlockPos.ZERO);
        Pig Animal = Helper.spawn(EntityType.PIG, new Vec3(Centre.getX() - Origin.getX() + 0.5, 7.0, Centre.getZ() - Origin.getZ() + 6.5));
        UUID Id = Animal.getUUID();
        Tracked.add(Id);
        Animal.getNavigation().moveTo(Centre.getX() + 0.5, Centre.getY(), Centre.getZ() + 0.5, 1.2);
        int[] Pushes = {0};
        Helper.onEachTick(() -> {
            if (Animal.isAlive() && !Scp354Grab.isSeized(Animal) && Pushes[0]++ < 40) {
                Vec3 Toward = new Vec3(Centre.getX() + 0.5 - Animal.getX(), 0.0, Centre.getZ() + 0.5 - Animal.getZ()).normalize().scale(0.18);
                Animal.setDeltaMovement(Toward.x, Animal.getDeltaMovement().y, Toward.z);
            }
        });
        Helper.runAfterDelay(120, () -> {
            Helper.assertTrue(!Animal.isAlive() || Animal.isRemoved(), "the pig survived the pool at " + Animal.blockPosition());
            Helper.assertTrue("scp_unleashed:scp354_taken".equals(Causes.get(Id)), "the pig died of " + Causes.get(Id));
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp354_controlledTaken")
    public static void controlledTaken(GameTestHelper Helper) {
        terrain(Helper, (X, Z) -> 6);
        Scp354Basin.Result Built = pool(Helper, new BlockPos(Middle, 6, Middle), 354L);
        Scp354Pools.Pool Entry = watched(Helper, Built);
        DClass Body = Helper.spawn(DClassRegistry.DClassType.get(), new Vec3(4.5, 7.0, 4.5));
        UUID BodyId = Body.getUUID();
        ServerPlayer User = person(Helper, GameType.SURVIVAL, new Vec3(5.5, 7.0, 4.5));
        Tracked.add(BodyId);
        Tracked.add(User.getUUID());
        Helper.runAfterDelay(5, () -> {
            boolean Linked = ControlManager.possess(User, Body);
            Helper.assertTrue(Linked, "the test player could not take control of the D-Class");
            Helper.assertTrue(ControlSessions.isControlling(User), "the control session did not start");
            Vec3 Spot = Helper.absoluteVec(into(Helper, Built));
            User.teleportTo(Helper.getLevel(), Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        });
        Helper.runAfterDelay(110, () -> {
            try {
                Helper.assertTrue(Deaths.getOrDefault(BodyId, 0) == 1, "the controlled D-Class body died " + Deaths.getOrDefault(BodyId, 0) + " times");
                Helper.assertTrue("scp_unleashed:scp354_taken".equals(Causes.get(BodyId)), "the body died of " + Causes.get(BodyId));
                Helper.assertFalse(ControlSessions.isControlling(User), "the controller is still linked to a dead body");
                Helper.assertTrue(User.isAlive() && !User.isDeadOrDying() && User.getHealth() > 0.0f, "the controller died with the body");
                Helper.assertTrue(Deaths.getOrDefault(User.getUUID(), 0) <= 1, "the controller died again after going home");
                Helper.assertFalse(ControlManager.hasPendingDeath(User), "a death is still pending");
                Helper.succeed();
            } finally {
                dismiss(Helper, User);
                forget(Helper, Entry);
            }
        });
    }

    private static void clearKin(GameTestHelper Helper) {
        for (LivingEntity Found : Helper.getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(120.0), Scp354Prey::isKin)) {
            Found.discard();
        }
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp354_emergeCapped")
    public static void emergeCapped(GameTestHelper Helper) {
        terrain(Helper, (X, Z) -> 6);
        clearKin(Helper);
        Scp354Basin.Result Built = pool(Helper, new BlockPos(Middle, 6, Middle), 354L);
        Scp354Pools.Pool Entry = watched(Helper, Built);
        ServerLevel World = Helper.getLevel();
        try {
            List<Entity> Made = new ArrayList<>();
            for (int Index = 0; Index < Scp354Emergence.Cap; Index++) {
                Optional<Entity> Risen = Scp354Emergence.emerge(World, Entry, null);
                Helper.assertTrue(Risen.isPresent(), "nothing emerged on try " + Index);
                Entity Found = Risen.get();
                Helper.assertTrue(Found.getType().is(Scp354Registry.Spawns), Found.getType() + " is not in scp354_spawns");
                Helper.assertTrue(Scp354Emergence.isPoolBorn(Found), Found + " is not marked pool-born");
                Helper.assertTrue(Found instanceof Mob Creature && Creature.isPersistenceRequired(), Found + " would despawn");
                Helper.assertTrue(!(Found instanceof Scp354Emergent Emergent) || Emergent.isEmerging(), Found + " did not begin emerging");
                Helper.assertTrue(Scp354Fluid.isRed(World.getFluidState(BlockPos.containing(Found.getX(), Found.getY() + 0.1, Found.getZ()))) || Found.getY() <= Entry.Centre.getY() + 1,
                        Found + " was not placed in the pool");
                Made.add(Found);
            }
            Helper.assertTrue(Scp354Emergence.alive(World, Entry) >= Scp354Emergence.Cap, "only " + Scp354Emergence.alive(World, Entry) + " creatures are counted");
            Helper.assertFalse(Scp354Emergence.hasRoom(World, Entry), "the pool still has room past the cap");
            Entry.Timer = 1;
            Scp354Emergence.tickPool(World, Scp354Pools.get(World.getServer()), Entry, World.getGameTime());
            Helper.assertTrue(Scp354Emergence.alive(World, Entry) == Scp354Emergence.Cap, "the pool grew past its cap to " + Scp354Emergence.alive(World, Entry));
            Helper.assertTrue(Entry.Timer >= Scp354Emergence.IntervalMin, "the timer did not reset after a capped emergence");
            Helper.succeed();
        } finally {
            clearKin(Helper);
            forget(Helper, Entry);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp354_catchUp")
    public static void catchUp(GameTestHelper Helper) {
        terrain(Helper, (X, Z) -> 6);
        clearKin(Helper);
        Scp354Basin.Result Built = pool(Helper, new BlockPos(Middle, 6, Middle), 354L);
        Scp354Pools.Pool Entry = watched(Helper, Built);
        ServerLevel World = Helper.getLevel();
        try {
            long Now = World.getGameTime();
            Entry.LastSeen = Now - Scp354Emergence.CatchUpDouble - 10L;
            Scp354Emergence.tickPool(World, Scp354Pools.get(World.getServer()), Entry, Now);
            Helper.assertTrue(Scp354Emergence.alive(World, Entry) == 2, "a long absence raised " + Scp354Emergence.alive(World, Entry) + " creatures, not 2");
            Helper.assertTrue(Entry.LastSeen == Now, "the pool did not record the visit");
            clearKin(Helper);
            Entry.LastSeen = Now - Scp354Emergence.CatchUpAfter - 10L;
            Scp354Emergence.tickPool(World, Scp354Pools.get(World.getServer()), Entry, Now);
            Helper.assertTrue(Scp354Emergence.alive(World, Entry) == 1, "a shorter absence raised " + Scp354Emergence.alive(World, Entry) + " creatures, not 1");
            clearKin(Helper);
            Entry.LastSeen = Now - 200L;
            Scp354Emergence.tickPool(World, Scp354Pools.get(World.getServer()), Entry, Now);
            Helper.assertTrue(Scp354Emergence.alive(World, Entry) == 0, "a brief absence raised creatures");
            Helper.succeed();
        } finally {
            clearKin(Helper);
            forget(Helper, Entry);
        }
    }

    private static void hunts(GameTestHelper Helper, EntityType<? extends Scp354Beast> Type) {
        terrain(Helper, (X, Z) -> 1);
        clearKin(Helper);
        Scp354Beast Hunter = Helper.spawn(Type, new Vec3(10.5, 2.0, Middle + 0.5));
        Hunter.setYRot(-90.0f);
        DClass Near = Helper.spawn(DClassRegistry.DClassType.get(), new Vec3(15.5, 2.0, Middle + 0.5));
        Villager Far = Helper.spawn(EntityType.VILLAGER, new Vec3(22.5, 2.0, Middle + 0.5));
        Scp354Beast Kin = Helper.spawn(Scp354Registry.Lizard.get(), new Vec3(7.5, 2.0, Middle + 3.5));
        Near.setNoAi(true);
        Far.setNoAi(true);
        Kin.setNoAi(true);
        LivingEntity[] First = {null};
        Helper.onEachTick(() -> {
            LivingEntity Target = Hunter.getTarget();
            Helper.assertTrue(Target == null || !Scp354Prey.isKin(Target), Hunter + " turned on its own kind");
            if (First[0] == null && Target != null) {
                First[0] = Target;
            }
        });
        Helper.runAfterDelay(60, () -> {
            try {
                Helper.assertTrue(First[0] == Near, Hunter.getType() + " first hunted " + First[0] + " instead of the closer D-Class");
                Helper.assertTrue(Kin.getTarget() == null, "the idle lizard picked a target");
                Helper.succeed();
            } finally {
                clearKin(Helper);
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp354_tentacleHunts")
    public static void tentacleHunts(GameTestHelper Helper) {
        hunts(Helper, Scp354Registry.Tentacle.get());
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp354_lionHunts")
    public static void lionHunts(GameTestHelper Helper) {
        hunts(Helper, Scp354Registry.Lion.get());
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp354_lizardHunts")
    public static void lizardHunts(GameTestHelper Helper) {
        hunts(Helper, Scp354Registry.Lizard.get());
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "scp354_switchesCloser")
    public static void switchesCloser(GameTestHelper Helper) {
        terrain(Helper, (X, Z) -> 1);
        clearKin(Helper);
        Scp354Beast Hunter = Helper.spawn(Scp354Registry.Lizard.get(), new Vec3(10.5, 2.0, Middle + 0.5));
        Villager Far = Helper.spawn(EntityType.VILLAGER, new Vec3(24.5, 2.0, Middle + 0.5));
        Far.setNoAi(true);
        Hunter.setNoAi(false);
        Villager[] Near = {null};
        int[] Ticks = {0, 0};
        Helper.onEachTick(() -> {
            Ticks[0]++;
            if (Near[0] == null) {
                Helper.assertTrue(Ticks[0] <= 120, "the lizard did not hunt the only villager, it hunts " + Hunter.getTarget() + " from " + Hunter.position()
                        + " while the villager stands at " + Far.position());
                if (Hunter.getTarget() == Far) {
                    Near[0] = Helper.spawn(EntityType.VILLAGER, new Vec3(13.5, 2.0, Middle + 0.5));
                    Near[0].setNoAi(true);
                    Ticks[1] = Ticks[0];
                }
                return;
            }
            if (Hunter.getTarget() == Near[0]) {
                clearKin(Helper);
                Helper.succeed();
                return;
            }
            Helper.assertTrue(Ticks[0] - Ticks[1] <= 60, "the lizard kept chasing the far villager");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp354_persistence")
    public static void persistence(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp354Pools.Pool Entry = Scp354Pools.get(World.getServer()).add(new Scp354Pools.Pool(World.dimension(), Helper.absolutePos(new BlockPos(Middle, 6, Middle)), 4, 3,
                4321, 98765L, 7));
        try {
            CompoundTag Saved = Scp354Pools.snapshot(World.getServer());
            Scp354Pools Loaded = Scp354Pools.load(Saved, World.registryAccess());
            Optional<Scp354Pools.Pool> Found = Loaded.nearest(World.dimension(), Entry.Centre, 1.0);
            Helper.assertTrue(Found.isPresent(), "the pool did not survive a save");
            Scp354Pools.Pool Copy = Found.get();
            Helper.assertTrue(Copy.Centre.equals(Entry.Centre) && Copy.Radius == 4 && Copy.Depth == 3 && Copy.Timer == 4321 && Copy.LastSeen == 98765L && Copy.Born == 7,
                    "the pool came back changed: " + Copy.save());
            Scp354Beast Creature = Helper.spawn(Scp354Registry.Lion.get(), new Vec3(4.5, 2.0, 4.5));
            Creature.setData(Scp354Registry.PoolBorn.get(), true);
            CompoundTag Body = Creature.saveWithoutId(new CompoundTag());
            Entity Again = Scp354Registry.Lion.get().create(World);
            Helper.assertTrue(Again != null, "could not create a lion");
            Again.load(Body);
            Helper.assertTrue(Scp354Emergence.isPoolBorn(Again), "the pool-born mark did not survive a save");
            Creature.discard();
            Helper.succeed();
        } finally {
            forget(Helper, Entry);
        }
    }
}
