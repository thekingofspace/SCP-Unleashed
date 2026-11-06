package net.rai.scp_unleashed.z0003.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassTrait;
import net.rai.scp_unleashed.z0003.Z0003Arming;
import net.rai.scp_unleashed.z0003.Z0003Device;
import net.rai.scp_unleashed.z0003.Z0003DeviceItem;
import net.rai.scp_unleashed.z0003.Z0003Petrify;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Z0003MobHeldGameTests {
    public static final String Arena = Z0003GameTests.Arena;
    public static final double Search = 40.0;
    public static final double CentreSlack = 0.75;
    public static final int Settle = 60;
    private static final Logger Log = LogUtils.getLogger();

    private Z0003MobHeldGameTests() {
    }

    private static int stand(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static Vec3 at(GameTestHelper Helper, double X, double Z) {
        return Helper.absoluteVec(new Vec3(X, stand(Helper), Z));
    }

    private static ItemStack armed(ServerLevel World, float Radius, int Ticks) {
        ItemStack Stack = Z0003DeviceItem.create();
        Z0003DeviceItem.arm(Stack, new Z0003Arming(Radius, World.getGameTime() + Ticks));
        return Stack;
    }

    private static DClass finder(GameTestHelper Helper, double X, double Z) {
        DClass Made = Helper.spawn(DClassRegistry.DClassType.get(), new Vec3(X, stand(Helper), Z));
        Made.identify(DClassTrait.Curious, DClassCrime.random(Made.getRandom()), 0);
        Made.setCanPickUpLoot(true);
        return Made;
    }

    private static Z0003TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer User = new Z0003TestPlayer(World, Name);
        Vec3 Spot = at(Helper, X, Z);
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static <T extends Entity> List<T> around(GameTestHelper Helper, Class<T> Kind) {
        Vec3 Centre = at(Helper, 16.0, 16.0);
        return Helper.getLevel().getEntitiesOfClass(Kind, new AABB(Centre, Centre).inflate(Search), Entity::isAlive);
    }

    private static boolean carries(DClass Holder) {
        for (ItemStack Stack : Holder.main()) {
            if (Z0003DeviceItem.is(Stack)) {
                return true;
            }
        }
        return Z0003DeviceItem.is(Holder.getItemBySlot(EquipmentSlot.OFFHAND));
    }

    private static void clean(GameTestHelper Helper) {
        for (Z0003Device Found : around(Helper, Z0003Device.class)) {
            Found.discard();
        }
        for (ItemEntity Found : around(Helper, ItemEntity.class)) {
            Found.discard();
        }
        for (LivingEntity Found : around(Helper, LivingEntity.class)) {
            if (!(Found instanceof Z0003TestPlayer)) {
                Found.discard();
            }
        }
    }

    private static final class Watch {
        private final Map<Integer, Vec3> Fired = new HashMap<>();
        private long Ticked;

        private Watch(GameTestHelper Helper) {
            Helper.onEachTick(() -> {
                Ticked++;
                for (Z0003Device Found : around(Helper, Z0003Device.class)) {
                    if (Found.isActive()) {
                        Fired.putIfAbsent(Found.getId(), Found.fieldCentre());
                    }
                }
            });
        }

        private boolean fired() {
            return !Fired.isEmpty();
        }

        private Vec3 centre() {
            return Fired.values().iterator().next();
        }
    }

    private static void centredOn(GameTestHelper Helper, Watch Seen, LivingEntity Holder) {
        double Gap = Z0003Device.distanceTo(Seen.centre(), Holder.getBoundingBox());
        Helper.assertTrue(Gap <= CentreSlack, "the field was not centred on its holder, " + Gap + " blocks off");
    }

    private static void settled(GameTestHelper Helper, Watch Seen, String Who) {
        Log.info("Z-0003 held by {} fired {} time(s); the world ticked {} times after", Who, Seen.Fired.size(), Seen.Ticked);
        Helper.assertTrue(Seen.Ticked >= Settle, "the world stopped ticking after the device went off in the hand of " + Who);
        Helper.assertTrue(Seen.Fired.size() == 1, "the device in the hand of " + Who + " went off " + Seen.Fired.size() + " times");
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "z0003_mob_held_pickup")
    public static void dclassPickupFiresOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        DClass Holder = finder(Helper, 16.5, 16.5);
        Vec3 Spot = at(Helper, 17.5, 16.5);
        ItemEntity Dropped = new ItemEntity(World, Spot.x, Spot.y + 0.2, Spot.z, armed(World, 3.0f, 80));
        Dropped.setPickUpDelay(0);
        World.addFreshEntity(Dropped);
        Watch Seen = new Watch(Helper);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(carries(Holder), "the D-Class never picked up the armed device"))
                .thenExecute(() -> Helper.assertFalse(Seen.fired(), "the device went off before it was picked up"))
                .thenWaitUntil(() -> Helper.assertTrue(Seen.fired(), "the device never went off in the D-Class's hand"))
                .thenWaitUntil(() -> Helper.assertTrue(Z0003Petrify.isStatue(Holder), "the D-Class holding the device was not caught"))
                .thenExecute(() -> {
                    centredOn(Helper, Seen, Holder);
                    Helper.assertFalse(carries(Holder), "the D-Class still carries the device after it went off");
                    Helper.assertTrue(Holder.getMainHandItem().isEmpty(), "the device is still in the D-Class's hand");
                    Seen.Ticked = 0;
                })
                .thenIdle(Settle)
                .thenExecute(() -> {
                    settled(Helper, Seen, "a D-Class");
                    clean(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 800, batch = "z0003_mob_held_spoken")
    public static void spokenDropPickedUpFiresOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer User = player(Helper, 8.5, 16.5, "z0003_dropper");
        User.setItemInHand(InteractionHand.MAIN_HAND, Z0003DeviceItem.create());
        DClass Holder = DClassRegistry.DClassType.get().spawn(World, Helper.absolutePos(new BlockPos(11, stand(Helper), 16)), MobSpawnType.SPAWN_EGG);
        Helper.assertTrue(Holder != null, "the D-Class could not be spawned");
        Holder.identify(DClassTrait.Curious, DClassCrime.random(Holder.getRandom()), 0);
        String Line = "effect 2 meters in 6 seconds";
        CommonHooks.onServerChatSubmittedEvent(User, Line, Component.literal(Line));
        Helper.assertTrue(Z0003DeviceItem.isArmed(User.getMainHandItem()), "the spoken setting did not arm the device");
        ItemStack Stack = User.getMainHandItem().copy();
        User.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Helper.assertTrue(User.drop(Stack, false, true) != null, "the player could not drop the device");
        Watch Seen = new Watch(Helper);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(carries(Holder), "the D-Class never picked up the dropped device"))
                .thenWaitUntil(() -> Helper.assertTrue(Seen.fired(), "the dropped device never went off in the D-Class's hand"))
                .thenWaitUntil(() -> Helper.assertTrue(Z0003Petrify.isStatue(Holder), "the D-Class holding the device was not caught"))
                .thenExecute(() -> {
                    centredOn(Helper, Seen, Holder);
                    Seen.Ticked = 0;
                })
                .thenIdle(Settle)
                .thenExecute(() -> {
                    settled(Helper, Seen, "a D-Class that picked it up");
                    clean(Helper);
                    World.removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "z0003_mob_held_offhand")
    public static void dclassOffhandFiresOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        DClass Holder = finder(Helper, 16.5, 16.5);
        Holder.setItemSlot(EquipmentSlot.OFFHAND, armed(World, 3.0f, 20));
        Watch Seen = new Watch(Helper);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Seen.fired(), "the device in the off hand never went off"))
                .thenWaitUntil(() -> Helper.assertTrue(Z0003Petrify.isStatue(Holder), "the D-Class was not caught"))
                .thenExecute(() -> {
                    centredOn(Helper, Seen, Holder);
                    Helper.assertTrue(Holder.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty(), "the device stayed in the off hand");
                    Seen.Ticked = 0;
                })
                .thenIdle(Settle)
                .thenExecute(() -> {
                    settled(Helper, Seen, "a D-Class's off hand");
                    clean(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "z0003_mob_held_zombie")
    public static void otherMobFiresOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Zombie Holder = Helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(16.5, stand(Helper), 16.5));
        Holder.setItemInHand(InteractionHand.MAIN_HAND, armed(World, 2.0f, 20));
        Watch Seen = new Watch(Helper);
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Seen.fired(), "the device in the zombie's hand never went off"))
                .thenExecute(() -> {
                    centredOn(Helper, Seen, Holder);
                    Helper.assertTrue(Holder.getMainHandItem().isEmpty(), "the device stayed in the zombie's hand");
                    Seen.Ticked = 0;
                })
                .thenIdle(Settle)
                .thenExecute(() -> {
                    settled(Helper, Seen, "a zombie");
                    clean(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "z0003_mob_held_stored")
    public static void framedAndChestedStayInert(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int Floor = stand(Helper);
        BlockPos Chest = new BlockPos(12, Floor, 12);
        Helper.setBlock(Chest, Blocks.CHEST);
        ChestBlockEntity Box = (ChestBlockEntity) World.getBlockEntity(Helper.absolutePos(Chest));
        Helper.assertTrue(Box != null, "the chest has no block entity");
        Box.setItem(0, armed(World, 2.0f, 5));
        BlockPos Wall = new BlockPos(20, Floor, 20);
        Helper.setBlock(Wall, Blocks.STONE);
        ItemFrame Frame = new ItemFrame(World, Helper.absolutePos(Wall.north()), Direction.NORTH);
        Frame.setItem(armed(World, 2.0f, 5), false);
        World.addFreshEntity(Frame);
        Watch Seen = new Watch(Helper);
        Helper.startSequence()
                .thenIdle(80)
                .thenExecute(() -> {
                    Helper.assertFalse(Seen.fired(), "a framed or chested device went off");
                    Helper.assertTrue(Z0003DeviceItem.isArmed(Box.getItem(0)), "the chested device lost its setting");
                    Helper.assertTrue(Z0003DeviceItem.isArmed(Frame.getItem()), "the framed device lost its setting");
                    Helper.assertTrue(Seen.Ticked >= 80, "the world stopped ticking");
                    Frame.discard();
                    Box.clearContent();
                    Helper.setBlock(Chest, Blocks.AIR);
                    Helper.setBlock(Wall, Blocks.AIR);
                    clean(Helper);
                })
                .thenSucceed();
    }
}
