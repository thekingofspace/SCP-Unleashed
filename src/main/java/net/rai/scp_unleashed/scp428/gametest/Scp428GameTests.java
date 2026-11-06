package net.rai.scp_unleashed.scp428.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.rai.scp_unleashed.anomaly.AnomalyDrops;
import net.rai.scp_unleashed.door.DoorRegistry;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.guns.npc.NpcGunner;
import net.rai.scp_unleashed.mtf.MtfMission;
import net.rai.scp_unleashed.mtf.MtfMissions;
import net.rai.scp_unleashed.mtf.MtfOperative;
import net.rai.scp_unleashed.mtf.MtfTier;
import net.rai.scp_unleashed.mtf.gametest.MtfTestPlayer;
import net.rai.scp_unleashed.door.KeycardScannerBlock;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.door.ScanStatus;
import net.rai.scp_unleashed.scp428.Scp428Carry;
import net.rai.scp_unleashed.scp428.Scp428Doorwork;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp428.Scp428Cords;
import net.rai.scp_unleashed.scp428.Scp428Core;
import net.rai.scp_unleashed.scp428.Scp428Goals;
import net.rai.scp_unleashed.scp428.Scp428Grip;
import net.rai.scp_unleashed.scp428.Scp428Hold;
import net.rai.scp_unleashed.scp428.Scp428Holds;
import net.rai.scp_unleashed.scp428.Scp428Instance;
import net.rai.scp_unleashed.scp428.Scp428Looks;
import net.rai.scp_unleashed.scp428.Scp428Record;
import net.rai.scp_unleashed.scp428.Scp428Registry;
import net.rai.scp_unleashed.scp428.Scp428Size;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp428GameTests {
    public static final String Arena = "scp428_arena";
    public static final String SkinValue = "c2NwNDI4X3Rlc3Rfc2tpbg==";
    public static final String SkinSignature = "c2lnbmVk";

    private Scp428GameTests() {
    }

    private static int stand(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static Scp428Core core(GameTestHelper Helper, double X, double Z) {
        Scp428Core Core = Helper.spawn(Scp428Registry.Core.get(), new Vec3(X, stand(Helper), Z));
        Core.setYRot(0.0f);
        return Core;
    }

    private static List<Scp428Instance> bodies(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(Scp428Instance.class, Helper.getBounds().inflate(2.0), Scp428Instance::isAlive);
    }

    private static List<Scp428Core> cores(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(Scp428Core.class, Helper.getBounds().inflate(2.0), Scp428Core::isAlive);
    }

    private static Victim victim(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        GameProfile Profile = new GameProfile(UUID.randomUUID(), Name);
        Profile.getProperties().put("textures", new Property("textures", SkinValue, SkinSignature));
        Victim User = new Victim(World, Profile);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, stand(Helper), Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static FakePlayer watcher(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp428_watcher"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, stand(Helper), Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (User != null && !User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void tidy(GameTestHelper Helper) {
        for (Scp428Core Core : cores(Helper)) {
            Core.discard();
        }
        Helper.killAllEntitiesOfClass(Scp428Instance.class);
        Helper.killAllEntitiesOfClass(DClass.class);
    }

    private static String state(Scp428Core Core) {
        return " [count " + Core.count() + ", girth " + String.format("%.2f", Core.girth()) + ", target " + (Core.getTarget() == null ? "none" : Core.getTarget().getName().getString())
                + ", gripping " + Core.isGripping() + ", absorbed " + Core.absorbed() + ", at " + Core.position() + ", device " + Core.agent().device() + ", used " + Core.agent().lastUsed()
                + ", bodies " + Core.bodies((ServerLevel) Core.level()).size() + "]";
    }

    private static double floorY(GameTestHelper Helper) {
        return Helper.absoluteVec(new Vec3(0.0, stand(Helper), 0.0)).y;
    }

    private static void room(GameTestHelper Helper, int Height) {
        int Base = stand(Helper);
        for (int X = 5; X <= 11; X++) {
            for (int Z = 13; Z <= 19; Z++) {
                if (X != 5 && X != 11 && Z != 13 && Z != 19) {
                    continue;
                }
                for (int Y = Base; Y < Base + Height; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.STONE_BRICKS);
                }
            }
        }
    }

    private static AABB inside(GameTestHelper Helper) {
        int Base = stand(Helper);
        return new AABB(Vec3.atLowerCornerOf(Helper.absolutePos(new BlockPos(6, Base, 14))), Vec3.atLowerCornerOf(Helper.absolutePos(new BlockPos(12, Base + 3, 19))));
    }

    private static void door(GameTestHelper Helper, Block Kind) {
        int Base = stand(Helper);
        BlockState Door = Kind.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST).setValue(DoorBlock.OPEN, false);
        Helper.setBlock(new BlockPos(11, Base, 16), Door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        Helper.setBlock(new BlockPos(11, Base + 1, 16), Door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp428_spawnsWithOneInstance")
    public static void spawnsWithOneInstance(GameTestHelper Helper) {
        Scp428Core Core = core(Helper, 8.5, 16.5);
        double Floor = floorY(Helper);
        Helper.runAfterDelay(60, () -> {
            Helper.assertTrue(Core.count() == 1, "SCP-428 did not start with one instance" + state(Core));
            List<Scp428Instance> Found = bodies(Helper);
            Helper.assertTrue(Found.size() == 1, "expected one instance in the world, found " + Found.size() + state(Core));
            Scp428Instance Body = Found.get(0);
            Helper.assertTrue(Core.claims(Body.recordId(), Body.getUUID()), "the instance is not linked to its core");
            Helper.assertTrue(!Body.isPlayerLook() && Body.formerName().startsWith("D-"), "the first instance is not a D-Class look: " + Body.formerName());
            Helper.assertTrue(Math.abs(Core.getBbWidth() - Scp428Size.SingleDiameter) < 0.02f, "a single-instance core is " + Core.getBbWidth() + " wide");
            double Gap = Core.getBoundingBox().minY - Floor;
            Helper.assertTrue(Gap > 0.15 && Gap < 0.6, "SCP-428 does not hover at the right height: gap " + Gap + state(Core));
            Helper.assertTrue(!Core.onGround() && Core.isNoGravity(), "SCP-428 is resting on the ground");
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp428_absorbsDClass")
    public static void absorbsDClass(GameTestHelper Helper) {
        Scp428Core Core = core(Helper, 8.5, 16.5);
        DClass Person = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(11.0, stand(Helper), 16.5));
        int Look = DClassAppearance.pack(4, DClassAppearance.Curly, 3, 2, 1, 2, 1, DClassAppearance.Jumpsuit);
        Person.setAppearance(Look);
        UUID PersonId = Person.getUUID();
        float[] Before = new float[1];
        boolean[] Gripped = new boolean[1];
        Helper.onEachTick(() -> Gripped[0] |= Core.isGripping());
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 1 && Core.girth() >= 0.99f, "SCP-428 never settled at one instance" + state(Core)))
                .thenExecute(() -> Before[0] = Core.getBbWidth())
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 2, "SCP-428 did not take the D-Class in" + state(Core)))
                .thenWaitUntil(() -> Helper.assertTrue(Core.girth() >= 1.99f, "SCP-428 did not grow" + state(Core)))
                .thenExecute(() -> {
                    Helper.assertTrue(Gripped[0], "SCP-428 never held the D-Class before taking it");
                    Helper.assertTrue(Core.getBbWidth() > Before[0] + 0.05f, "the core did not get wider: " + Before[0] + " to " + Core.getBbWidth());
                    Scp428Record Taken = Core.records().get(1);
                    Helper.assertTrue(Taken.appearance() == Look && !Taken.isPlayer(), "the record did not keep the D-Class look: " + Taken.appearance());
                    Helper.assertTrue(Helper.getLevel().getEntity(PersonId) == null, "the D-Class body was not replaced by an instance");
                    Scp428Instance Body = bodies(Helper).stream().filter(Found -> Found.recordId() != null && Found.recordId().equals(Taken.id())).findFirst().orElse(null);
                    Helper.assertTrue(Body != null, "no instance stands in for the D-Class");
                    Helper.assertTrue(Body.appearance() == Look, "the instance does not wear the D-Class look");
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp428_absorbsPlayer")
    public static void absorbsPlayer(GameTestHelper Helper) {
        Scp428Core Core = core(Helper, 8.5, 16.5);
        Victim User = victim(Helper, 10.0, 16.5, "scp428_test");
        UUID UserId = User.getUUID();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 2, "SCP-428 did not take the player in" + state(Core) + " deaths " + User.Deaths))
                .thenExecute(() -> {
                    Helper.assertTrue(User.Deaths == 1, "the player died " + User.Deaths + " times");
                    Scp428Record Taken = Core.records().get(1);
                    Helper.assertTrue(Taken.isPlayer() && Taken.name().equals("scp428_test"), "the record did not keep the player: " + Taken.name());
                    ResolvableProfile Profile = Taken.profile();
                    Helper.assertTrue(Profile != null && Profile.id().isPresent() && Profile.id().get().equals(UserId), "the stored profile lost the player id");
                    Property Skin = Profile.properties().get("textures").stream().findFirst().orElse(null);
                    Helper.assertTrue(Skin != null && SkinValue.equals(Skin.value()) && SkinSignature.equals(Skin.signature()), "the stored profile lost the skin textures");
                    Scp428Instance Body = bodies(Helper).stream().filter(Found -> Found.recordId() != null && Found.recordId().equals(Taken.id())).findFirst().orElse(null);
                    Helper.assertTrue(Body != null && Body.isPlayerLook() && Body.profile() != null && "scp428_test".equals(Body.formerName()),
                            "the instance does not carry the player's profile");
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp428_neverOverCapacity")
    public static void neverOverCapacity(GameTestHelper Helper) {
        Scp428Core Core = core(Helper, 16.5, 16.5);
        Helper.runAfterDelay(5, () -> {
            for (int Index = 0; Index < 30; Index++) {
                Core.adopt(Scp428Looks.stray(Helper.getLevel().getRandom()), Core.position().add(Index % 6 - 2.5, 0.0, Index / 6 - 2.5), 0.0f);
            }
        });
        Helper.startSequence()
                .thenExecuteAfter(30, () -> {
                    Helper.assertTrue(Core.count() == Scp428Size.Capacity, "the count is " + Core.count() + state(Core));
                    Helper.assertTrue(Core.records().size() == Scp428Size.Capacity, "more records than the cap");
                    Helper.assertTrue(bodies(Helper).size() == Scp428Size.Capacity, "there are " + bodies(Helper).size() + " instances");
                    DClass Extra = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(18.5, stand(Helper), 16.5));
                    Core.strike(Helper.getLevel(), Extra);
                })
                .thenExecuteAfter(10, () -> {
                    Helper.assertTrue(Core.count() == Scp428Size.Capacity, "a full SCP-428 grew past the cap: " + Core.count());
                    Helper.assertTrue(bodies(Helper).size() == Scp428Size.Capacity, "a full SCP-428 made another instance");
                    Helper.assertTrue(Math.abs(Core.getMaxHealth() - Scp428Size.maxHealth(Scp428Size.Capacity)) < 0.01, "max health " + Core.getMaxHealth());
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp428_losingInstanceShrinks")
    public static void losingInstanceShrinks(GameTestHelper Helper) {
        Scp428Core Core = core(Helper, 16.5, 16.5);
        float[] Wide = new float[1];
        Helper.runAfterDelay(5, () -> {
            for (int Index = 0; Index < 4; Index++) {
                Core.adopt(Scp428Looks.stray(Helper.getLevel().getRandom()), Core.position().add(Index * 1.2 - 2.0, 0.0, 2.0), 0.0f);
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 5 && Core.girth() >= 4.99f && bodies(Helper).size() == 5, "SCP-428 did not reach five" + state(Core)))
                .thenExecute(() -> {
                    Wide[0] = Core.getBbWidth();
                    Scp428Instance Body = bodies(Helper).get(2);
                    Body.hurt(Helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 4 && Core.girth() <= 4.01f, "losing an instance did not shrink the count" + state(Core)))
                .thenExecute(() -> {
                    Helper.assertTrue(Core.getBbWidth() < Wide[0] - 0.02f, "the core did not shrink: " + Wide[0] + " to " + Core.getBbWidth());
                    Helper.assertTrue(Math.abs(Core.getMaxHealth() - Scp428Size.maxHealth(4)) < 0.01, "max health did not drop: " + Core.getMaxHealth());
                })
                .thenExecuteAfter(Scp428Core.MissingGrace + 30, () -> {
                    Helper.assertTrue(Core.count() == 4 && bodies(Helper).size() == 4, "a lost instance came back" + state(Core));
                    tidy(Helper);
                })
                .thenSucceed();
    }

    private static void escape(GameTestHelper Helper, Block Leaf, Block Switch, String Name) {
        int Base = stand(Helper);
        room(Helper, 3);
        door(Helper, Leaf);
        if (Switch == Blocks.STONE_BUTTON) {
            Helper.setBlock(new BlockPos(10, Base + 1, 15), Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACE, AttachFace.WALL)
                    .setValue(ButtonBlock.FACING, Direction.WEST));
        } else if (Switch == Blocks.LEVER) {
            Helper.setBlock(new BlockPos(10, Base + 1, 15), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                    .setValue(LeverBlock.FACING, Direction.WEST));
        }
        Scp428Core Core = core(Helper, 8.5, 16.5);
        FakePlayer Bait = watcher(Helper, 16.5, 16.5);
        AABB Room = inside(Helper);
        boolean[] Opened = new boolean[1];
        boolean[] Switched = new boolean[1];
        Helper.onEachTick(() -> {
            Opened[0] |= Helper.getBlockState(new BlockPos(11, Base, 16)).getValue(DoorBlock.OPEN);
            if (Switch != Blocks.AIR) {
                BlockState Device = Helper.getBlockState(new BlockPos(10, Base + 1, 15));
                Switched[0] |= Device.hasProperty(ButtonBlock.POWERED) && Device.getValue(ButtonBlock.POWERED);
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!Room.contains(Core.position()), Name + ": SCP-428 never left the room" + state(Core) + " opened " + Opened[0] + " switched "
                        + Switched[0]))
                .thenExecute(() -> {
                    Helper.assertTrue(Opened[0], Name + ": the door was never opened");
                    Helper.assertTrue(Switch == Blocks.AIR || Switched[0], Name + ": the switch was never used");
                    dismiss(Helper, Bait);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp428_opensWoodenDoor")
    public static void opensWoodenDoor(GameTestHelper Helper) {
        escape(Helper, Blocks.OAK_DOOR, Blocks.AIR, "wooden door");
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp428_pressesButtonForIronDoor")
    public static void pressesButtonForIronDoor(GameTestHelper Helper) {
        escape(Helper, Blocks.IRON_DOOR, Blocks.STONE_BUTTON, "button and iron door");
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp428_pullsLeverForIronDoor")
    public static void pullsLeverForIronDoor(GameTestHelper Helper) {
        escape(Helper, Blocks.IRON_DOOR, Blocks.LEVER, "lever and iron door");
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp428_squeezesThroughDoorway")
    public static void squeezesThroughDoorway(GameTestHelper Helper) {
        int Base = stand(Helper);
        room(Helper, 3);
        Scp428Core Core = core(Helper, 8.5, 16.5);
        Helper.runAfterDelay(3, () -> {
            for (int Index = 0; Index < 11; Index++) {
                Core.adopt(Scp428Looks.stray(Helper.getLevel().getRandom()), Core.position().add(Index % 3 - 1.0, 0.0, Index / 3 - 1.5), 0.0f);
            }
        });
        AABB Room = inside(Helper);
        FakePlayer[] Bait = new FakePlayer[1];
        float[] Squeezed = new float[1];
        Helper.onEachTick(() -> Squeezed[0] = Math.max(Squeezed[0], Core.squeeze()));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 12 && Core.girth() >= 11.99f, "SCP-428 never grew to twelve" + state(Core)))
                .thenExecute(() -> {
                    Helper.assertTrue(Scp428Size.diameter(Core.girth()) > 1.2f, "a twelve-instance core is only " + Scp428Size.diameter(Core.girth()) + " across");
                    Helper.assertTrue(Room.contains(Core.position()), "SCP-428 left the sealed room while growing" + state(Core));
                    Squeezed[0] = 0.0f;
                    Helper.setBlock(new BlockPos(11, Base, 16), Blocks.AIR);
                    Helper.setBlock(new BlockPos(11, Base + 1, 16), Blocks.AIR);
                    Bait[0] = watcher(Helper, 17.5, 16.5);
                })
                .thenWaitUntil(() -> Helper.assertTrue(!Room.contains(Core.position()), "a large SCP-428 never got through the doorway" + state(Core) + " squeeze " + Core.squeeze()))
                .thenExecute(() -> {
                    Helper.assertTrue(Squeezed[0] > 0.9f, "SCP-428 never squeezed: " + Squeezed[0]);
                    Helper.assertTrue(Scp428Size.width(Scp428Size.Capacity, 1.0f) <= 0.75f
                            && Scp428Size.tall(Scp428Size.Capacity, 1.0f) + Scp428Size.clearance(Scp428Size.Capacity, 1.0f) <= 1.85f, "a full core cannot squeeze through a doorway");
                    dismiss(Helper, Bait[0]);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1400, batch = "scp428_sealedRoomHolds")
    public static void sealedRoomHolds(GameTestHelper Helper) {
        room(Helper, 3);
        Scp428Core Core = core(Helper, 8.5, 16.5);
        FakePlayer Bait = watcher(Helper, 15.5, 16.5);
        AABB Room = inside(Helper);
        Helper.onEachTick(() -> {
            Helper.assertTrue(Room.contains(Core.position()), "SCP-428 left a sealed room" + state(Core));
            for (Scp428Instance Body : bodies(Helper)) {
                Helper.assertTrue(Room.contains(Body.position()), "an instance left a sealed room at " + Body.position());
            }
        });
        Helper.runAfterDelay(1300, () -> {
            Helper.assertTrue(Core.getTarget() == Bait, "SCP-428 never noticed the player outside" + state(Core));
            Helper.assertBlockPresent(Blocks.STONE_BRICKS, new BlockPos(11, stand(Helper) + 1, 16));
            Helper.assertTrue(Core.agent().operations() == 0, "SCP-428 operated something in a bare room");
            dismiss(Helper, Bait);
            tidy(Helper);
            Helper.succeed();
        });
    }

    private static Map<String, Integer> looks(List<Scp428Instance> Found) {
        Map<String, Integer> Result = new HashMap<>();
        for (Scp428Instance Body : Found) {
            Result.merge(Body.formerName() + "/" + Body.appearance() + "/" + Body.isPlayerLook(), 1, Integer::sum);
        }
        return Result;
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp428_stasisRoundTrip")
    public static void stasisRoundTrip(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp428Core Core = core(Helper, 8.5, 16.5);
        FakePlayer User = watcher(Helper, 3.5, 3.5);
        List<Map<String, Integer>> Before = new ArrayList<>();
        Helper.runAfterDelay(3, () -> {
            Core.adopt(new Scp428Looks.Look(DClassAppearance.pack(2, 3, 1, 1, 1, 0, 0, 0), new CompoundTag(), "D-1111"), Core.position().add(1.5, 0.0, 0.0), 0.0f);
            Core.adopt(new Scp428Looks.Look(DClassAppearance.pack(5, 6, 2, 3, 2, 1, 2, 0), new CompoundTag(), "D-2222"), Core.position().add(-1.5, 0.0, 0.0), 0.0f);
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 3 && bodies(Helper).size() == 3, "SCP-428 did not reach three" + state(Core)))
                .thenExecute(() -> {
                    Before.add(looks(bodies(Helper)));
                    ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
                    Helper.assertTrue(StasisPods.refusal(User, Pod, Core) == null, "a stasis pod refuses SCP-428");
                    Helper.assertTrue(StasisPods.refusal(User, Pod, bodies(Helper).get(0)) != null, "a stasis pod accepts a lone instance");
                    Helper.assertTrue(StasisPods.capture(World, User, Pod, Core), "the pod failed to capture SCP-428");
                    Helper.assertTrue(Core.isRemoved() && StasisPodItem.isOccupied(Pod), "the pod does not hold SCP-428");
                    Helper.assertTrue(bodies(Helper).isEmpty(), "instances stayed behind after capture: " + bodies(Helper).size());
                    Helper.assertTrue(StasisPods.breakOut(World, Pod, Helper.absoluteVec(new Vec3(16.5, stand(Helper), 16.5)), null), "SCP-428 could not be released");
                })
                .thenExecuteAfter(3, () -> {
                    List<Scp428Core> Released = cores(Helper);
                    Helper.assertTrue(Released.size() == 1, "expected one released SCP-428, found " + Released.size());
                    Scp428Core Back = Released.get(0);
                    Helper.assertTrue(Back.count() == 3, "SCP-428 came back holding " + Back.count());
                    Helper.assertTrue(bodies(Helper).size() == 3, "expected three instances back, found " + bodies(Helper).size());
                    Helper.assertTrue(looks(bodies(Helper)).equals(Before.get(0)), "the instances came back looking different: " + looks(bodies(Helper)) + " vs " + Before.get(0));
                    for (Scp428Instance Body : bodies(Helper)) {
                        Helper.assertTrue(Back.claims(Body.recordId(), Body.getUUID()) && Body.distanceTo(Back) < 6.0, "a released instance is not tethered");
                    }
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp428_survivesReload")
    public static void survivesReload(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp428Core Core = core(Helper, 8.5, 16.5);
        List<Map<String, Integer>> Before = new ArrayList<>();
        Scp428Core[] Current = {Core};
        Helper.runAfterDelay(3, () -> {
            Core.adopt(Scp428Looks.stray(World.getRandom()), Core.position().add(1.5, 0.0, 0.0), 0.0f);
            Core.adopt(Scp428Looks.stray(World.getRandom()), Core.position().add(-1.5, 0.0, 0.0), 0.0f);
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 3 && bodies(Helper).size() == 3, "SCP-428 did not reach three" + state(Core)))
                .thenExecute(() -> {
                    Before.add(looks(bodies(Helper)));
                    List<CompoundTag> Saved = new ArrayList<>();
                    for (Scp428Instance Body : bodies(Helper)) {
                        CompoundTag Tag = new CompoundTag();
                        Body.save(Tag);
                        Saved.add(Tag);
                        Body.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                    }
                    CompoundTag CoreTag = new CompoundTag();
                    Core.save(CoreTag);
                    Core.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                    Scp428Core Loaded = (Scp428Core) EntityType.create(CoreTag, World).orElseThrow();
                    Helper.assertTrue(World.addFreshEntity(Loaded), "the reloaded core was rejected");
                    for (CompoundTag Tag : Saved) {
                        Helper.assertTrue(World.addFreshEntity(EntityType.create(Tag, World).orElseThrow()), "a reloaded instance was rejected");
                    }
                    Current[0] = Loaded;
                })
                .thenExecuteAfter(Scp428Core.MissingGrace + 40, () -> {
                    Helper.assertTrue(Current[0].count() == 3, "the reloaded core holds " + Current[0].count());
                    Helper.assertTrue(bodies(Helper).size() == 3, "after a full reload there are " + bodies(Helper).size() + " instances");
                    Helper.assertTrue(looks(bodies(Helper)).equals(Before.get(0)), "looks changed across a reload");
                    for (Scp428Instance Body : bodies(Helper)) {
                        Helper.assertTrue(Current[0].claims(Body.recordId(), Body.getUUID()), "an instance did not re-link to its core");
                    }
                    CompoundTag CoreTag = new CompoundTag();
                    Current[0].save(CoreTag);
                    Current[0].remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                    for (Scp428Instance Body : bodies(Helper)) {
                        Body.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                    }
                    Scp428Core Loaded = (Scp428Core) EntityType.create(CoreTag, World).orElseThrow();
                    Helper.assertTrue(World.addFreshEntity(Loaded), "the reloaded core was rejected");
                    Current[0] = Loaded;
                })
                .thenExecuteAfter(Scp428Core.MissingGrace + 40, () -> {
                    Helper.assertTrue(Current[0].count() == 3, "the core lost records when its instances stayed unloaded");
                    Helper.assertTrue(bodies(Helper).size() == 3, "missing instances were not rebuilt: " + bodies(Helper).size());
                    Helper.assertTrue(looks(bodies(Helper)).equals(Before.get(0)), "rebuilt instances look different");
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp428_controlledDeathOnce")
    public static void controlledDeathOnce(GameTestHelper Helper) {
        Scp428Core Core = core(Helper, 8.5, 16.5);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(10.0, stand(Helper), 16.5));
        int Look = DClassAppearance.pack(3, DClassAppearance.Mohawk, 2, 4, 0, 3, 2, DClassAppearance.Jumpsuit);
        Body.setAppearance(Look);
        Victim User = victim(Helper, 11.0, 16.5, "scp428_controller");
        Helper.assertTrue(ControlManager.possess(User, Body), "the test player could not take control of the D-Class");
        Helper.assertTrue(ControlSessions.isControlling(User), "no control session started");
        boolean[] Pending = new boolean[1];
        Helper.onEachTick(() -> {
            if (ControlManager.hasPendingDeath(User)) {
                Pending[0] = true;
                Helper.assertTrue(Core.count() == 1, "the controlled death was counted before the deferred tick" + state(Core));
                ControlManager.processDeath(User);
            }
            Helper.assertTrue(Core.count() <= 2, "the controlled D-Class was taken twice" + state(Core));
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 2, "the controlled D-Class was never taken" + state(Core) + " [pending " + Pending[0] + ", deaths "
                        + User.Deaths + "]"))
                .thenExecute(() -> {
                    Helper.assertTrue(Pending[0], "the death did not go through the deferred control path");
                    Helper.assertTrue(!ControlSessions.isControlling(User), "the player still controls a dead body");
                    Helper.assertTrue(User.Deaths == 0, "the controller died for real");
                    Scp428Record Taken = Core.records().get(1);
                    Helper.assertTrue(!Taken.isPlayer() && Taken.appearance() == Look, "the instance took the controller's skin instead of the D-Class look");
                })
                .thenExecuteAfter(40, () -> {
                    Helper.assertTrue(Core.count() == 2 && Core.absorbed() == 1, "the count changed after the death" + state(Core));
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    private static Scp428Core still(GameTestHelper Helper, double X, double Z, int Extra) {
        Scp428Core Core = core(Helper, X, Z);
        Core.setNoAi(true);
        Helper.runAfterDelay(2, () -> {
            for (int Index = 0; Index < Extra; Index++) {
                Core.adopt(Scp428Looks.stray(Helper.getLevel().getRandom()), Core.position().add(Index - 0.5, 0.0, 1.5), 0.0f);
            }
        });
        return Core;
    }

    private static List<Scp428Instance> holders(GameTestHelper Helper, UUID Held) {
        return bodies(Helper).stream().filter(Body -> Body.isHolding(Held)).toList();
    }

    private static void struggleFree(GameTestHelper Helper, int Holders, int Need) {
        Scp428Core Core = still(Helper, 8.5, 16.5, Holders - 1);
        Victim User = victim(Helper, 12.5, 16.5, "scp428_struggler");
        UUID Id = User.getUUID();
        int[] Pressed = {0};
        int[] Phase = {0};
        Helper.onEachTick(() -> {
            if (Phase[0] == 0 && Core.holdCount(Id) == Holders) {
                Phase[0] = 1;
            } else if (Phase[0] == 1 && Pressed[0] < Need - 1) {
                Helper.assertTrue(Scp428Holds.press(User), "a press was refused with " + Core.holdCount(Id) + " holders" + state(Core));
                Pressed[0]++;
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Pressed[0] == Need - 1, Holders + " holders never formed or presses stalled: holders " + Core.holdCount(Id) + ", pressed "
                        + Pressed[0] + state(Core)))
                .thenExecute(() -> {
                    Helper.assertTrue(Core.isHolding(Id) && Core.holdCount(Id) == Holders, "the player slipped free early after " + Pressed[0] + " presses");
                    Helper.assertTrue(Scp428Grip.isLocked(User), "a held player can still walk");
                    Helper.assertTrue(Core.holdPresses(Id) == Need - 1, "the hold counted " + Core.holdPresses(Id) + " presses");
                    Phase[0] = 2;
                })
                .thenExecuteAfter(1, () -> Helper.assertTrue(Scp428Holds.press(User), "the last press was refused"))
                .thenExecute(() -> {
                    Helper.assertTrue(!Core.isHolding(Id) && !Scp428Holds.isHeld(User), Need + " presses did not free the player from " + Holders + " holders");
                    Helper.assertTrue(!Scp428Grip.isLocked(User), "the freed player is still locked in place");
                    Helper.assertTrue(holders(Helper, Id).isEmpty(), "instances are still holding after the escape");
                })
                .thenExecuteAfter(Scp428Core.RegrabCooldown - 20, () -> {
                    Helper.assertTrue(!Core.isHolding(Id), "the player was grabbed again during the escape window");
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp428_struggleOneHolder")
    public static void struggleOneHolder(GameTestHelper Helper) {
        struggleFree(Helper, 1, Scp428Hold.OneHolder);
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp428_struggleTwoHolders")
    public static void struggleTwoHolders(GameTestHelper Helper) {
        struggleFree(Helper, 2, Scp428Hold.TwoHolders);
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp428_threeHoldersNeverLetGo")
    public static void threeHoldersNeverLetGo(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 2);
        Victim User = victim(Helper, 12.5, 16.5, "scp428_pinned");
        UUID Id = User.getUUID();
        int[] Tries = {0};
        Helper.onEachTick(() -> {
            if (Core.holdCount(Id) >= 3 && Tries[0] < 40) {
                Helper.assertTrue(!Scp428Holds.press(User), "a press counted against three holders");
                Tries[0]++;
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Tries[0] >= 40, "three holders never formed: " + Core.holdCount(Id) + state(Core)))
                .thenExecute(() -> {
                    Helper.assertTrue(Core.isHolding(Id) && Core.holdCount(Id) == 3, "three holders let go after struggling: " + Core.holdCount(Id));
                    Helper.assertTrue(Core.holdPresses(Id) == 0, "presses were kept against three holders: " + Core.holdPresses(Id));
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp428_holderDeathLowersNeed")
    public static void holderDeathLowersNeed(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 1);
        Victim User = victim(Helper, 12.5, 16.5, "scp428_slipping");
        UUID Id = User.getUUID();
        int[] Pressed = {0};
        Helper.onEachTick(() -> {
            if (Core.holdCount(Id) == 2 && Pressed[0] < 9) {
                Helper.assertTrue(Scp428Holds.press(User), "a press was refused with two holders");
                Pressed[0]++;
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Pressed[0] == 9, "two holders never formed: " + Core.holdCount(Id) + state(Core)))
                .thenExecute(() -> {
                    Helper.assertTrue(Core.isHolding(Id) && Core.holdPresses(Id) == 9, "nine presses freed the player from two holders");
                    holders(Helper, Id).get(0).hurt(Helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                })
                .thenExecuteAfter(3, () -> {
                    Helper.assertTrue(!Core.isHolding(Id) && !Scp428Grip.isLocked(User), "losing a holder did not lower the need below the nine presses already made");
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp428_helpFreesHolder")
    public static void helpFreesHolder(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 1);
        Victim User = victim(Helper, 12.5, 16.5, "scp428_helped");
        FakePlayer Friend = watcher(Helper, 13.5, 19.5);
        UUID Id = User.getUUID();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.holdCount(Id) == 2, "two holders never formed: " + Core.holdCount(Id) + state(Core)))
                .thenExecute(() -> {
                    Scp428Instance Holder = holders(Helper, Id).get(0);
                    float Health = Holder.getHealth();
                    Holder.invulnerableTime = 0;
                    Helper.assertTrue(!Holder.hurt(Helper.getLevel().damageSources().playerAttack(User), 3.0f), "the held player's own hit landed on a holder");
                    Helper.assertTrue(Holder.isHolding(Id) && Holder.getHealth() == Health, "the held player's own hit broke the hold");
                    Holder.invulnerableTime = 0;
                    Helper.assertTrue(Holder.hurt(Helper.getLevel().damageSources().playerAttack(Friend), 3.0f), "the friend's hit did not land");
                    Helper.assertTrue(!Holder.isHolding(Id), "a holder hit by a friend kept holding");
                    Helper.assertTrue(Core.holdCount(Id) == 1, "the hold still counts " + Core.holdCount(Id) + " holders");
                    Helper.assertTrue(!Core.canClutch(Holder, User) || !Holder.canGrab(), "a holder knocked off can grab again at once");
                })
                .thenExecuteAfter(5, () -> {
                    Helper.assertTrue(Core.holdCount(Id) == 1, "the knocked off holder grabbed straight back: " + Core.holdCount(Id));
                    dismiss(Helper, Friend);
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp428_creativeIgnored")
    public static void creativeIgnored(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 1);
        FakePlayer Builder = watcher(Helper, 10.5, 16.5);
        Builder.setGameMode(GameType.CREATIVE);
        Helper.onEachTick(() -> {
            Helper.assertTrue(!Core.isHolding(Builder.getUUID()) && !Scp428Holds.isHeld(Builder), "a creative player was grabbed");
            Helper.assertTrue(Core.getTarget() != Builder, "SCP-428 targeted a creative player");
        });
        Helper.runAfterDelay(240, () -> {
            dismiss(Helper, Builder);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp428_absorbsHeldVictim")
    public static void absorbsHeldVictim(GameTestHelper Helper) {
        Scp428Core Core = core(Helper, 8.5, 16.5);
        Helper.runAfterDelay(2, () -> Core.adopt(Scp428Looks.stray(Helper.getLevel().getRandom()), Core.position().add(0.5, 0.0, 1.5), 0.0f));
        DClass Person = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(14.5, stand(Helper), 16.5));
        UUID Id = Person.getUUID();
        boolean[] HeldFirst = new boolean[1];
        Helper.onEachTick(() -> {
            if (!Core.isGripping() && !holders(Helper, Id).isEmpty()) {
                HeldFirst[0] = true;
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 3, "SCP-428 never took in the held D-Class" + state(Core)))
                .thenExecute(() -> {
                    Helper.assertTrue(HeldFirst[0], "the figures never held the D-Class before the mass reached it");
                    Helper.assertTrue(Helper.getLevel().getEntity(Id) == null && !Core.isHolding(Id), "the hold outlived the D-Class");
                    Helper.assertTrue(bodies(Helper).stream().noneMatch(Scp428Instance::isHolding), "a figure is still holding after the take");
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp428_npcStruggles")
    public static void npcStruggles(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 0);
        DClass Person = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(12.5, stand(Helper), 16.5));
        UUID Id = Person.getUUID();
        long[] Held = {-1L};
        Helper.onEachTick(() -> {
            if (Held[0] < 0 && Core.holdCount(Id) == 1) {
                Held[0] = Helper.getTick();
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Held[0] >= 0, "the figure never held the D-Class" + state(Core)))
                .thenWaitUntil(() -> Helper.assertTrue(!Core.isHolding(Id), "the D-Class never struggled free of one holder" + state(Core) + " presses " + Core.holdPresses(Id)))
                .thenExecute(() -> {
                    long Took = Helper.getTick() - Held[0];
                    Helper.assertTrue(Took >= Scp428Hold.OneHolder * Scp428Core.AutoShortest && Took <= Scp428Hold.OneHolder * (Scp428Core.AutoShortest + Scp428Core.AutoSpread) + 10,
                            "the D-Class broke free in " + Took + " ticks");
                    Helper.assertTrue(Person.isAlive() && !Scp428Grip.isLocked(Person), "the freed D-Class is still locked");
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp428_friendsBreakHold")
    public static void friendsBreakHold(GameTestHelper Helper) {
        Scp428Core Core = core(Helper, 8.5, 16.5);
        DClass Person = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(10.5, stand(Helper), 16.5));
        FakePlayer Friend = watcher(Helper, 8.5, 19.5);
        UUID Id = Person.getUUID();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.isGripping() && Core.holdCount(Id) >= 1, "SCP-428 never took hold of the D-Class" + state(Core)))
                .thenExecute(() -> {
                    Core.invulnerableTime = 0;
                    Helper.assertTrue(Core.hurt(Helper.getLevel().damageSources().playerAttack(Friend), 2.0f), "the friend's hit on the mass did not land");
                    Helper.assertTrue(!Core.isGripping(), "one hit from a friend did not make the mass let go" + state(Core));
                    for (Scp428Instance Holder : holders(Helper, Id)) {
                        Holder.invulnerableTime = 0;
                        Holder.hurt(Helper.getLevel().damageSources().playerAttack(Friend), 2.0f);
                    }
                })
                .thenExecuteAfter(2, () -> {
                    Helper.assertTrue(!Core.isHolding(Id) && !Scp428Grip.isLocked(Person), "the D-Class is still held after its friend hit every holder" + state(Core));
                })
                .thenExecuteAfter(Scp428Core.ReachTicks + 10, () -> {
                    Helper.assertTrue(Person.isAlive() && Core.count() == 1, "the freed D-Class was still taken" + state(Core));
                    dismiss(Helper, Friend);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    private static double flat(Entity Body, Scp428Core Core) {
        double Dx = Body.getX() - Core.getX();
        double Dz = Body.getZ() - Core.getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    private static void settled(GameTestHelper Helper, Scp428Core Core, int Count, double Slack) {
        List<Scp428Instance> Found = bodies(Helper);
        Helper.assertTrue(Found.size() == Count, "expected " + Count + " figures, found " + Found.size() + state(Core));
        Helper.assertTrue(Math.abs(Core.girth() - Count) < 0.01f, "the mass is still growing" + state(Core));
        for (Scp428Instance Body : Found) {
            double Gap = flat(Body, Core);
            Helper.assertTrue(Body.huddle() == Scp428Instance.Huddled, "a figure is not huddled: mode " + Body.huddle() + state(Core));
            Vec3 Spot = Core.huddleSpot(Helper.getLevel(), Body);
            Helper.assertTrue(Math.abs(Gap - Core.huddleRadius()) < Slack, "a figure stands " + String.format("%.2f", Gap) + " from the mass, the ring is "
                    + String.format("%.2f", Core.huddleRadius()) + state(Core));
            Helper.assertTrue(Spot != null && Math.sqrt(Body.distanceToSqr(Spot.x, Body.getY(), Spot.z)) < Slack, "a figure has not reached its place in the ring" + state(Core));
        }
    }

    private static double facingError(Scp428Instance Body, double Dx, double Dz) {
        float Want = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        return Math.abs(Mth.wrapDegrees(Body.yBodyRot - Want));
    }

    @GameTest(template = Arena, timeoutTicks = 1600, batch = "scp428_huddleSettles")
    public static void huddleSettles(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 5);
        Helper.startSequence()
                .thenWaitUntil(() -> settled(Helper, Core, 6, 0.35))
                .thenExecuteAfter(30, () -> {
                    settled(Helper, Core, 6, 0.5);
                    List<Scp428Instance> Found = bodies(Helper);
                    for (Scp428Instance Body : Found) {
                        double Off = facingError(Body, Body.getX() - Core.getX(), Body.getZ() - Core.getZ());
                        Helper.assertTrue(Off < 50.0, "a huddled figure does not face outward: off by " + String.format("%.0f", Off) + " degrees");
                        for (Scp428Instance Other : Found) {
                            Helper.assertTrue(Other == Body || Other.distanceTo(Body) > 0.55, "two huddled figures overlap: " + String.format("%.2f", Other.distanceTo(Body)));
                        }
                    }
                    Helper.assertTrue(Core.getTarget() == null && Core.isResting(), "the mass stopped resting" + state(Core));
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1800, batch = "scp428_huddleFollows")
    public static void huddleFollows(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 3);
        int[] Phase = {0};
        int[] Moved = {0};
        Map<UUID, Vec3> Last = new HashMap<>();
        double[] Furthest = {0.0};
        Helper.onEachTick(() -> {
            if (Phase[0] == 1) {
                Core.setPos(Core.getX() + 0.05, Core.getY(), Core.getZ());
                if (++Moved[0] >= 70) {
                    Phase[0] = 2;
                }
            }
            for (Scp428Instance Body : bodies(Helper)) {
                Vec3 Before = Last.put(Body.getUUID(), Body.position());
                if (Phase[0] >= 1 && Before != null) {
                    Furthest[0] = Math.max(Furthest[0], Before.distanceTo(Body.position()));
                }
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> settled(Helper, Core, 4, 0.35))
                .thenExecute(() -> Phase[0] = 1)
                .thenWaitUntil(() -> Helper.assertTrue(Phase[0] == 2, "the mass did not finish moving"))
                .thenWaitUntil(() -> settled(Helper, Core, 4, Scp428Goals.HuddleDrift + 0.05))
                .thenExecute(() -> {
                    Helper.assertTrue(Furthest[0] < 0.9, "a figure jumped " + String.format("%.2f", Furthest[0]) + " blocks in one tick instead of walking");
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1800, batch = "scp428_guardsStepBetween")
    public static void guardsStepBetween(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 5);
        FakePlayer[] Visitor = new FakePlayer[1];
        Helper.startSequence()
                .thenWaitUntil(() -> settled(Helper, Core, 6, 0.35))
                .thenExecute(() -> {
                    Visitor[0] = watcher(Helper, 13.5, 16.5);
                    Visitor[0].setGameMode(GameType.CREATIVE);
                })
                .thenWaitUntil(() -> {
                    FakePlayer Near = Visitor[0];
                    Vec3 Toward = new Vec3(Near.getX() - Core.getX(), 0.0, Near.getZ() - Core.getZ());
                    double Distance = Toward.length();
                    Vec3 Heading = Toward.scale(1.0 / Distance);
                    List<Scp428Instance> Guards = bodies(Helper).stream().filter(Body -> Body.huddle() == Scp428Instance.Guarding).toList();
                    Helper.assertTrue(Guards.size() == 3, "expected three guards, found " + Guards.size() + state(Core));
                    for (Scp428Instance Body : Guards) {
                        Vec3 Offset = Body.position().subtract(Core.position());
                        double Along = Offset.x * Heading.x + Offset.z * Heading.z;
                        double Across = Math.abs(Offset.x * -Heading.z + Offset.z * Heading.x);
                        Helper.assertTrue(Along > Core.radius() && Along < Distance - 0.6, "a guard is not between the visitor and the mass: along " + String.format("%.2f", Along));
                        Helper.assertTrue(Across < 1.3, "a guard stands too far to the side: " + String.format("%.2f", Across));
                        double Off = facingError(Body, Near.getX() - Body.getX(), Near.getZ() - Body.getZ());
                        Helper.assertTrue(Off < 45.0, "a guard does not face the visitor: off by " + String.format("%.0f", Off) + " degrees");
                    }
                })
                .thenExecute(() -> {
                    Helper.assertTrue(Core.getTarget() == null, "the mass hunted a creative visitor" + state(Core));
                    Helper.assertTrue(bodies(Helper).stream().noneMatch(Scp428Instance::isHolding), "a guard grabbed the visitor");
                    long Huddled = bodies(Helper).stream().filter(Body -> Body.huddle() == Scp428Instance.Huddled).count();
                    Helper.assertTrue(Huddled == 3, "the other figures left the huddle: " + Huddled + " still huddled");
                    dismiss(Helper, Visitor[0]);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1600, batch = "scp428_huddleBreaksOnTarget")
    public static void huddleBreaksOnTarget(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 3);
        FakePlayer[] Bait = new FakePlayer[1];
        Helper.startSequence()
                .thenWaitUntil(() -> settled(Helper, Core, 4, 0.35))
                .thenExecute(() -> Bait[0] = watcher(Helper, 16.5, 16.5))
                .thenWaitUntil(() -> Helper.assertTrue(Core.getTarget() == Bait[0], "the mass never noticed the player" + state(Core)))
                .thenWaitUntil(() -> Helper.assertTrue(bodies(Helper).stream().allMatch(Body -> Body.huddle() == Scp428Instance.Loose), "figures kept huddling during the hunt"))
                .thenExecuteAfter(10, () -> {
                    Helper.assertTrue(bodies(Helper).stream().allMatch(Body -> Body.huddle() == Scp428Instance.Loose), "figures went back to huddling while the mass hunts");
                    Helper.assertTrue(bodies(Helper).stream().anyMatch(Body -> flat(Body, Core) > Core.huddleRadius() + 0.8), "no figure left the huddle to hunt" + state(Core));
                    dismiss(Helper, Bait[0]);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1600, batch = "scp428_bracesWhenHurt")
    public static void bracesWhenHurt(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 3);
        FakePlayer[] Builder = new FakePlayer[1];
        float[] Health = new float[1];
        Helper.startSequence()
                .thenWaitUntil(() -> settled(Helper, Core, 4, 0.35))
                .thenExecute(() -> {
                    Builder[0] = watcher(Helper, 8.5, 22.5);
                    Builder[0].setGameMode(GameType.CREATIVE);
                    Health[0] = Core.getHealth();
                    Core.invulnerableTime = 0;
                    Helper.assertTrue(Core.hurt(Helper.getLevel().damageSources().playerAttack(Builder[0]), 2.0f), "the hit on the resting mass did not land");
                    Core.setDeltaMovement(Vec3.ZERO);
                    Helper.assertTrue(Core.getHealth() < Health[0], "the resting mass took no damage");
                    Helper.assertTrue(Core.bracing(), "the figures did not brace after the hit");
                })
                .thenWaitUntil(() -> {
                    for (Scp428Instance Body : bodies(Helper)) {
                        Helper.assertTrue(Body.huddle() == Scp428Instance.Bracing, "a figure did not close in: mode " + Body.huddle());
                        Helper.assertTrue(flat(Body, Core) < Core.radius() + Scp428Core.BraceGap + 0.35, "a figure did not close in: " + String.format("%.2f", flat(Body, Core)));
                    }
                })
                .thenExecuteAfter(Scp428Core.BraceTicks + 5, () -> {
                    Helper.assertTrue(!Core.bracing() && Core.isResting(), "the brace never ended or the mass hunted a creative player" + state(Core));
                    dismiss(Helper, Builder[0]);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp428_cordFramesHold")
    public static void cordFramesHold(GameTestHelper Helper) {
        RandomSource Dice = RandomSource.create(4280L);
        double Worst = 0.0;
        double WorstTurn = 0.0;
        for (int Trial = 0; Trial < 4000; Trial++) {
            Vec3 From = new Vec3(Dice.nextDouble() * 4.0 - 2.0, Dice.nextDouble() * 3.0, Dice.nextDouble() * 4.0 - 2.0);
            double Reach = Trial % 4 == 0 ? 0.3 + Dice.nextDouble() * 1.2 : 0.5 + Dice.nextDouble() * 14.0;
            double Tilt = Trial % 5 == 0 ? Math.PI * 0.5 * (Dice.nextBoolean() ? 1.0 : -1.0) * (0.9 + Dice.nextDouble() * 0.1) : (Dice.nextDouble() - 0.5) * Math.PI;
            double Heading = Dice.nextDouble() * Math.PI * 2.0;
            Vec3 To = From.add(Math.cos(Heading) * Math.cos(Tilt) * Reach, Math.sin(Tilt) * Reach, Math.sin(Heading) * Math.cos(Tilt) * Reach);
            double Turn = Dice.nextDouble() * Math.PI * 2.0;
            Vec3 Back = new Vec3(Math.cos(Turn), 0.0, Math.sin(Turn));
            Vec3[] Points = new Vec3[22];
            for (int Index = 0; Index <= 20; Index++) {
                Points[Index] = Scp428Cords.tetherPoint(From, To, Back, Scp428Cords.sample(Index, 20), Trial * 3.7, Trial * 0.41);
            }
            Points[21] = To.subtract(Back.scale(Scp428Cords.Sink));
            Vec3[][] Frames = Scp428Cords.frames(Points);
            Vec3[] Tangents = new Vec3[Points.length];
            Vec3 Last = new Vec3(1.0, 0.0, 0.0);
            for (int Index = 0; Index < Points.length; Index++) {
                Vec3 Along = Points[Math.min(Points.length - 1, Index + 1)].subtract(Points[Math.max(0, Index - 1)]);
                Last = Along.lengthSqr() < 1.0e-12 ? Last : Along.normalize();
                Tangents[Index] = Last;
            }
            for (int Index = 1; Index < Points.length; Index++) {
                Vec3 Expected = Scp428Cords.turn(Frames[0][Index - 1], Tangents[Index - 1], Tangents[Index]).normalize();
                double Twist = Math.toDegrees(Math.acos(Mth.clamp(Expected.dot(Frames[0][Index]), -1.0, 1.0)));
                Worst = Math.max(Worst, Twist);
                Helper.assertTrue(Math.abs(Frames[0][Index].dot(Tangents[Index])) < 1.0e-3 && Math.abs(Frames[1][Index].dot(Tangents[Index])) < 1.0e-3,
                        "a cord frame is not square to the cord");
                if (Index >= 2 && Index < Points.length - 1) {
                    Vec3 Before = Points[Index - 1].subtract(Points[Index - 2]);
                    Vec3 After = Points[Index].subtract(Points[Index - 1]);
                    if (Before.lengthSqr() > 1.0e-8 && After.lengthSqr() > 1.0e-8) {
                        WorstTurn = Math.max(WorstTurn, Math.toDegrees(Math.acos(Mth.clamp(Before.normalize().dot(After.normalize()), -1.0, 1.0))));
                    }
                }
            }
        }
        Helper.assertTrue(Worst < 1.0, "a cord twists by " + String.format("%.2f", Worst) + " degrees between two segments");
        Helper.assertTrue(WorstTurn < 150.0, "a cord folds back on itself by " + String.format("%.1f", WorstTurn) + " degrees");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp428_cordEndsLand")
    public static void cordEndsLand(GameTestHelper Helper) {
        RandomSource Dice = RandomSource.create(428L);
        double Limit = 0.5 / 16.0;
        double Near = 1.0e-4;
        for (int Trial = 0; Trial < 400; Trial++) {
            Vec3 From = new Vec3(Dice.nextDouble() * 6.0 - 3.0, Dice.nextDouble() * 3.0, Dice.nextDouble() * 6.0 - 3.0);
            double Heading = Dice.nextDouble() * Math.PI * 2.0;
            double Reach = 1.5 + Dice.nextDouble() * 18.0;
            Vec3 To = From.add(Math.cos(Heading) * Reach, Dice.nextDouble() * 4.0 - 2.0, Math.sin(Heading) * Reach);
            double Turn = Dice.nextDouble() * Math.PI * 2.0;
            Vec3 Back = new Vec3(Math.cos(Turn), 0.0, Math.sin(Turn));
            double Time = Dice.nextDouble() * 24000.0;
            double Phase = Dice.nextDouble() * Math.PI * 2.0;
            double Span = From.distanceTo(To);
            Helper.assertTrue(Scp428Cords.tetherPoint(From, To, Back, Near, Time, Phase).distanceTo(From) < Limit, "a tether leaves the mass with a gap");
            Helper.assertTrue(Scp428Cords.tetherPoint(From, To, Back, 1.0 - Near, Time, Phase).distanceTo(To) < Limit, "a tether misses the head by "
                    + Scp428Cords.tetherPoint(From, To, Back, 1.0 - Near, Time, Phase).distanceTo(To) * 16.0 + " px over " + Span);
            Helper.assertTrue(Scp428Cords.reachPoint(From, To, 1.0 - Near, Time, Phase).distanceTo(To) < Limit, "a reaching tentacle misses its target");
            Helper.assertTrue(Scp428Cords.reachPoint(From, To, Near, Time, Phase).distanceTo(From) < Limit, "a reaching tentacle leaves the mass with a gap");
            Helper.assertTrue(Scp428Cords.gripPoint(From, To, 1.0 - Near, Time, Phase).distanceTo(To) < Limit, "a gripping tentacle misses the body by "
                    + Scp428Cords.gripPoint(From, To, 1.0 - Near, Time, Phase).distanceTo(To) * 16.0 + " px");
            Helper.assertTrue(Scp428Cords.gripPoint(From, To, Near, Time, Phase).distanceTo(From) < Limit, "a gripping tentacle leaves the mass with a gap");
            Helper.assertTrue(Scp428Cords.reachEnd(To, Back, 1.0).distanceTo(To) < 1.0e-9, "a finished reach does not touch the head");
            float Girth = Dice.nextFloat() * Scp428Size.Capacity;
            float Squeeze = Dice.nextFloat();
            double Width = Scp428Size.width(Girth, Squeeze);
            double Tall = Scp428Size.tall(Girth, Squeeze);
            Vec3 Root = Scp428Cords.root(From, Width, Tall, To).subtract(From);
            double Rx = Math.sqrt(Root.x * Root.x + Root.z * Root.z) / (Width * 0.5);
            double Ry = (Root.y - Tall * 0.5) / (Tall * 0.5);
            Helper.assertTrue(Rx * Rx + Ry * Ry < 1.0, "a cord root sits outside the mass");
            Vec3 Top = Scp428Cords.reachRoot(From, Tall).subtract(From);
            Helper.assertTrue(Top.y < Tall && Top.y > Tall * 0.5, "the reaching tentacle does not start inside the top of the mass");
            for (int Arm = 0; Arm < 3; Arm++) {
                Vec3 Grip = Scp428Cords.gripRoot(From, To, Width, Tall, Arm).subtract(From);
                double Gx = Math.sqrt(Grip.x * Grip.x + Grip.z * Grip.z) / (Width * 0.5);
                double Gy = (Grip.y - Tall * 0.5) / (Tall * 0.5);
                Helper.assertTrue(Gx * Gx + Gy * Gy < 1.0, "a gripping tentacle starts outside the mass");
            }
        }
        Vec3 Neck = new Vec3(1.0, 1.5, -2.0);
        Vec3 Straight = Scp428Cords.headPoint(Neck, 90.0, 0.0, 1.0);
        Helper.assertTrue(Straight.distanceTo(new Vec3(1.0 + Scp428Cords.HeadBack, 1.5 + Scp428Cords.HeadUp, -2.0)) < 1.0e-9, "the head anchor is not behind a level head: " + Straight);
        Vec3 Down = Scp428Cords.headPoint(Neck, 0.0, 90.0, 1.0);
        Helper.assertTrue(Down.distanceTo(new Vec3(1.0, 1.5 + Scp428Cords.HeadBack, -2.0 + Scp428Cords.HeadUp)) < 1.0e-9, "the head anchor does not follow a bowed head: " + Down);
        Helper.succeed();
    }

    private static ItemStack stack(String Id, int Count) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(Id)), Count);
    }

    private static List<ItemEntity> loose(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(ItemEntity.class, Helper.getBounds().inflate(4.0), ItemEntity::isAlive);
    }

    private static int looseCount(GameTestHelper Helper, Item Kind) {
        return loose(Helper).stream().filter(Found -> Found.getItem().is(Kind)).mapToInt(Found -> Found.getItem().getCount()).sum();
    }

    private static void clearLoose(GameTestHelper Helper) {
        for (ItemEntity Found : loose(Helper)) {
            Found.discard();
        }
    }

    private static void wideDoor(GameTestHelper Helper) {
        int Base = stand(Helper);
        PanelDoorBlock Door = DoorRegistry.RoomDoor.get();
        BlockPos Origin = new BlockPos(11, Base, 17);
        for (int Part = 0; Part < Door.getKind().getCellCount(); Part++) {
            BlockPos Cell = PanelDoorBlock.cellPos(Origin, Direction.EAST, Door.getKind().columnOf(Part), Door.getKind().rowOf(Part));
            Helper.getLevel().setBlock(Helper.absolutePos(Cell), Door.defaultBlockState().setValue(PanelDoorBlock.Facing, Direction.EAST).setValue(PanelDoorBlock.Open, false)
                    .setValue(Door.partProperty(), Part), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean wideDoorOpen(GameTestHelper Helper) {
        return Scp428Doorwork.isOpen(Helper.getBlockState(new BlockPos(11, stand(Helper), 17)));
    }

    @GameTest(template = Arena, timeoutTicks = 520, batch = "scp428_noLeverFlicking")
    public static void noLeverFlicking(GameTestHelper Helper) {
        int Base = stand(Helper);
        BlockPos[] Levers = {new BlockPos(6, Base, 10), new BlockPos(14, Base, 12), new BlockPos(10, Base, 21), new BlockPos(18, Base, 18), new BlockPos(4, Base, 18)};
        for (BlockPos Spot : Levers) {
            Helper.setBlock(Spot, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR));
        }
        Scp428Core Core = core(Helper, 10.5, 16.5);
        Helper.onEachTick(() -> {
            for (BlockPos Spot : Levers) {
                Helper.assertTrue(!Helper.getBlockState(Spot).getValue(LeverBlock.POWERED), "SCP-428 flicked a lever while roaming" + state(Core));
            }
        });
        Helper.runAfterDelay(500, () -> {
            Helper.assertTrue(Core.agent().operations() == 0, "SCP-428 worked " + Core.agent().operations() + " devices with nothing to reach" + state(Core));
            for (Scp428Instance Body : bodies(Helper)) {
                Helper.assertTrue(Body.agent().operations() == 0, "a figure worked a device with nothing to reach");
            }
            tidy(Helper);
            Helper.succeed();
        });
    }

    private static void leverDoor(GameTestHelper Helper, BlockPos LeverAt, BlockState Lever, String Name) {
        room(Helper, 3);
        wideDoor(Helper);
        Helper.setBlock(LeverAt, Lever);
        Scp428Core Core = core(Helper, 7.5, 15.5);
        FakePlayer Bait = watcher(Helper, 16.5, 16.5);
        AABB Room = inside(Helper);
        int[] Flips = {0};
        boolean[] Was = {false};
        Helper.onEachTick(() -> {
            BlockState Now = Helper.getBlockState(LeverAt);
            boolean On = Now.hasProperty(LeverBlock.POWERED) && Now.getValue(LeverBlock.POWERED);
            if (On != Was[0]) {
                Flips[0]++;
                Was[0] = On;
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!Room.contains(Core.position()), Name + ": SCP-428 never got through the wide door" + state(Core) + " flips " + Flips[0]
                        + " open " + wideDoorOpen(Helper)))
                .thenExecuteAfter(40, () -> {
                    Helper.assertTrue(Flips[0] == 1, Name + ": the lever was flipped " + Flips[0] + " times");
                    Helper.assertTrue(Was[0] && wideDoorOpen(Helper), Name + ": the lever was put back and the door closed");
                    dismiss(Helper, Bait);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp428_leverBesideWideDoor")
    public static void leverBesideWideDoor(GameTestHelper Helper) {
        int Base = stand(Helper);
        leverDoor(Helper, new BlockPos(10, Base + 1, 15), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.WEST),
                "beside");
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp428_leverAboveWideDoor")
    public static void leverAboveWideDoor(GameTestHelper Helper) {
        int Base = stand(Helper);
        leverDoor(Helper, new BlockPos(10, Base + 2, 16), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, Direction.WEST),
                "above");
    }

    @GameTest(template = Arena, timeoutTicks = 900, batch = "scp428_leverBelowWideDoor")
    public static void leverBelowWideDoor(GameTestHelper Helper) {
        int Base = stand(Helper);
        leverDoor(Helper, new BlockPos(10, Base, 17), Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.WEST),
                "below");
    }

    private static void scannerDoor(GameTestHelper Helper) {
        int Base = stand(Helper);
        room(Helper, 3);
        door(Helper, Blocks.IRON_DOOR);
        Helper.setBlock(new BlockPos(10, Base + 1, 15), DoorRegistry.KeycardScanner.get().defaultBlockState().setValue(KeycardScannerBlock.FACING, Direction.WEST)
                .setValue(KeycardScannerBlock.Clearance, 2));
    }

    private static Scp428Carry.Haul carrying(boolean Owned, ItemStack... Stacks) {
        return new Scp428Carry.Haul(new ArrayList<>(List.of(Stacks)), Owned, false);
    }

    @GameTest(template = Arena, timeoutTicks = 1000, batch = "scp428_keycardDoorOpens")
    public static void keycardDoorOpens(GameTestHelper Helper) {
        int Base = stand(Helper);
        scannerDoor(Helper);
        Scp428Core Core = core(Helper, 7.5, 15.5);
        Helper.runAfterDelay(2, () -> Core.adopt(Scp428Looks.stray(Helper.getLevel().getRandom()), Core.position().add(0.0, 0.0, 2.0), 0.0f,
                carrying(true, new ItemStack(DoorRegistry.KeycardLevel3.get()))));
        FakePlayer[] Bait = new FakePlayer[1];
        AABB Room = inside(Helper);
        boolean[] Granted = new boolean[1];
        boolean[] Swiped = new boolean[1];
        Helper.onEachTick(() -> {
            BlockState Scanner = Helper.getBlockState(new BlockPos(10, Base + 1, 15));
            Granted[0] |= Scanner.getValue(KeycardScannerBlock.Status) == ScanStatus.Granted;
            Swiped[0] |= bodies(Helper).stream().anyMatch(Body -> Body.swipeStart() >= 0);
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(bodies(Helper).size() == 2, "the card carrier never formed" + state(Core)))
                .thenExecuteAfter(30, () -> Bait[0] = watcher(Helper, 16.5, 16.5))
                .thenWaitUntil(() -> Helper.assertTrue(!Room.contains(Core.position()), "SCP-428 never got through the keycard door" + state(Core) + " granted " + Granted[0]))
                .thenExecute(() -> {
                    Helper.assertTrue(Granted[0], "the scanner never accepted a card");
                    Helper.assertTrue(Swiped[0], "no figure swiped the card");
                    dismiss(Helper, Bait[0]);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp428_keycardDoorHolds")
    public static void keycardDoorHolds(GameTestHelper Helper) {
        int Base = stand(Helper);
        scannerDoor(Helper);
        Scp428Core Core = core(Helper, 7.5, 15.5);
        Helper.runAfterDelay(2, () -> Core.adopt(Scp428Looks.stray(Helper.getLevel().getRandom()), Core.position().add(0.0, 0.0, 2.0), 0.0f,
                carrying(true, new ItemStack(DoorRegistry.KeycardLevel1.get()))));
        FakePlayer[] Bait = new FakePlayer[1];
        AABB Room = inside(Helper);
        Helper.runAfterDelay(40, () -> Bait[0] = watcher(Helper, 16.5, 16.5));
        Helper.onEachTick(() -> {
            BlockState Scanner = Helper.getBlockState(new BlockPos(10, Base + 1, 15));
            Helper.assertTrue(Scanner.getValue(KeycardScannerBlock.Status) != ScanStatus.Granted, "a figure without a valid card opened the scanner");
            Helper.assertTrue(Room.contains(Core.position()), "SCP-428 left through a keycard door with no valid card" + state(Core));
            Helper.assertTrue(!Helper.getBlockState(new BlockPos(11, Base, 16)).getValue(DoorBlock.OPEN), "the keycard door opened");
        });
        Helper.runAfterDelay(600, () -> {
            Helper.assertTrue(Bait[0] != null && Core.getTarget() == Bait[0], "SCP-428 never wanted the player behind the door" + state(Core));
            dismiss(Helper, Bait[0]);
            tidy(Helper);
            Helper.succeed();
        });
    }

    private static Arrow shoot(GameTestHelper Helper, Scp428Core Core, Player Shooter) {
        Arrow Shot = new Arrow(EntityType.ARROW, Helper.getLevel());
        Shot.setOwner(Shooter);
        Shot.setPos(Core.getX(), Core.getY() + 0.5, Core.getZ());
        Core.invulnerableTime = 0;
        Helper.assertTrue(Core.hurt(Helper.getLevel().damageSources().arrow(Shot, Shooter), 1.0f), "the arrow did not hurt the mass");
        return Shot;
    }

    @GameTest(template = Arena, timeoutTicks = 1600, batch = "scp428_angerAfterShot")
    public static void angerAfterShot(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 3);
        Victim[] Shooter = new Victim[1];
        Helper.startSequence()
                .thenWaitUntil(() -> settled(Helper, Core, 4, 0.35))
                .thenExecute(() -> {
                    Shooter[0] = victim(Helper, 17.5, 16.5, "scp428_shooter");
                    FakePlayer Builder = watcher(Helper, 17.5, 12.5);
                    Builder.setGameMode(GameType.CREATIVE);
                    shoot(Helper, Core, Builder);
                    Helper.assertTrue(!Core.angry(), "a creative shooter angered the figures");
                    dismiss(Helper, Builder);
                    shoot(Helper, Core, Shooter[0]);
                    Helper.assertTrue(Core.angry() && Core.angerTarget() == Shooter[0], "a shot from a player did not anger SCP-428");
                    Helper.assertTrue(Core.pace() > 1.0 && Core.clutchRange() > Scp428Core.ClutchRange, "angry figures are not faster or keener");
                })
                .thenWaitUntil(() -> Helper.assertTrue(bodies(Helper).stream().allMatch(Body -> Body.huddle() == Scp428Instance.Loose), "angry figures kept huddling"))
                .thenWaitUntil(() -> Helper.assertTrue(bodies(Helper).stream().anyMatch(Body -> Body.distanceTo(Shooter[0]) < 7.0), "no figure went after the shooter"))
                .thenExecute(() -> dismiss(Helper, Shooter[0]))
                .thenExecuteAfter(3, () -> {
                    Helper.assertTrue(!Core.angry() && Core.getTarget() == null, "SCP-428 stayed angry after the shooter left" + state(Core));
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp428_figureShootsBack")
    public static void figureShootsBack(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 0);
        Helper.runAfterDelay(2, () -> Core.adopt(Scp428Looks.stray(Helper.getLevel().getRandom()), Core.position().add(1.5, 0.0, 0.0), 0.0f,
                carrying(true, new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 16))));
        Victim[] Shooter = new Victim[1];
        boolean[] Fired = new boolean[1];
        Helper.onEachTick(() -> {
            for (Arrow Shot : Helper.getLevel().getEntitiesOfClass(Arrow.class, Helper.getBounds().inflate(6.0), Arrow::isAlive)) {
                Fired[0] |= Shot.getOwner() instanceof Scp428Instance;
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(bodies(Helper).size() == 2 && bodies(Helper).stream().allMatch(Body -> Body.riseProgress(0.0f) >= 1.0f),
                        "the archer never formed" + state(Core)))
                .thenExecute(() -> {
                    Scp428Instance Archer = bodies(Helper).stream().filter(Body -> Body.weapon() != null).findFirst().orElse(null);
                    Helper.assertTrue(Archer != null && Archer.getMainHandItem().is(Items.BOW), "the archer does not hold its bow");
                    for (Scp428Instance Body : bodies(Helper)) {
                        if (Body != Archer) {
                            Body.hurt(Helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                        }
                    }
                    Shooter[0] = victim(Helper, 18.5, 16.5, "scp428_target");
                    shoot(Helper, Core, Shooter[0]);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Fired[0], "the figure with a bow never shot back" + state(Core)))
                .thenExecute(() -> {
                    Scp428Record Entry = Core.records().stream().filter(Found -> Found.carried().stream().anyMatch(Piece -> Piece.is(Items.BOW))).findFirst().orElse(null);
                    Helper.assertTrue(Entry != null && Entry.carried().stream().filter(Found -> Found.is(Items.ARROW)).mapToInt(ItemStack::getCount).sum() < 16,
                            "the figure shot without using its own arrows");
                    dismiss(Helper, Shooter[0]);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp428_figureFiresGun")
    public static void figureFiresGun(GameTestHelper Helper) {
        Scp428Core Core = still(Helper, 8.5, 16.5, 0);
        Helper.runAfterDelay(2, () -> Core.adopt(Scp428Looks.stray(Helper.getLevel().getRandom()), Core.position().add(1.5, 0.0, 0.0), 0.0f,
                carrying(true, new ItemStack(GunsRegistry.Pistol.get()), new ItemStack(GunsRegistry.HandgunRounds.get(), 24))));
        Victim[] Shooter = new Victim[1];
        float[] Health = new float[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(bodies(Helper).size() == 2 && bodies(Helper).stream().allMatch(Body -> Body.riseProgress(0.0f) >= 1.0f),
                        "the gunner never formed" + state(Core)))
                .thenExecute(() -> {
                    Scp428Instance Gunner = bodies(Helper).stream().filter(Body -> Body.weapon() != null).findFirst().orElse(null);
                    Helper.assertTrue(Gunner != null && Gunner.getMainHandItem().is(GunsRegistry.Pistol.get()), "the gunner does not hold its pistol");
                    for (Scp428Instance Body : bodies(Helper)) {
                        if (Body != Gunner) {
                            Body.hurt(Helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                        }
                    }
                    Shooter[0] = victim(Helper, 17.5, 16.5, "scp428_gun_target");
                    Health[0] = Shooter[0].getHealth();
                    shoot(Helper, Core, Shooter[0]);
                })
                .thenWaitUntil(() -> {
                    int Rounds = Core.records().stream().flatMap(Found -> Found.carried().stream()).filter(Found -> Found.is(GunsRegistry.HandgunRounds.get()))
                            .mapToInt(ItemStack::getCount).sum();
                    Helper.assertTrue(Rounds < 24, "the gunner never loaded its own rounds" + state(Core));
                    Helper.assertTrue(Shooter[0].getHealth() < Health[0], "the gunner never hit the shooter: health " + Shooter[0].getHealth());
                })
                .thenExecute(() -> {
                    dismiss(Helper, Shooter[0]);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp428_captureRules")
    public static void captureRules(GameTestHelper Helper) {
        Item Card = DoorRegistry.KeycardLevel2.get();
        Item Odd = BuiltInRegistries.ITEM.get(ResourceLocation.parse("scp_unleashed:scp018_ball"));
        Helper.assertTrue(AnomalyDrops.isAnomalous(new ItemStack(Odd)), "the test item is not covered by the anomaly drop rule");
        for (boolean Keep : new boolean[]{false, true}) {
            for (boolean Drop : new boolean[]{false, true}) {
                String Case = " (keepInventory " + Keep + ", scpDropAnomalies " + Drop + ")";
                clearLoose(Helper);
                Victim User = victim(Helper, 4.5, 4.5, "scp428_rules");
                User.getInventory().add(new ItemStack(Card));
                User.getInventory().add(new ItemStack(Items.BOW));
                User.getInventory().add(new ItemStack(Items.DIAMOND, 3));
                User.getInventory().add(new ItemStack(Odd));
                Scp428Carry.Haul Taken = Scp428Carry.capture(User, Keep, Drop);
                int Left = 0;
                for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
                    Left += User.getInventory().getItem(Slot).getCount();
                }
                Helper.assertTrue(Taken.FromPlayer(), "a player capture was not marked as a player" + Case);
                if (Keep) {
                    Helper.assertTrue(!Taken.Owned() && Left == 6, "keepInventory did not leave the player their items" + Case);
                    Helper.assertTrue(Taken.Stacks().stream().noneMatch(Found -> Found.is(Items.DIAMOND) || Found.is(Odd)), "the reference copy holds items it does not need" + Case);
                    Helper.assertTrue(Taken.Stacks().stream().anyMatch(Found -> Found.is(Card)) && Taken.Stacks().stream().anyMatch(Found -> Found.is(Items.BOW)),
                            "the reference copy lost the card or the bow" + Case);
                    Helper.assertTrue(looseCount(Helper, Odd) == 0, "capture dropped items with keepInventory on" + Case);
                    Helper.assertTrue(!Scp428Carry.dropsOnDeath(Scp428Record.of(Scp428Looks.stray(Helper.getLevel().getRandom()), Taken), false),
                            "a reference copy would drop" + Case);
                } else {
                    Helper.assertTrue(Taken.Owned() && Left == 0, "the player kept items the figure took" + Case);
                    int Carried = Taken.Stacks().stream().mapToInt(ItemStack::getCount).sum();
                    Helper.assertTrue(Carried == (Drop ? 5 : 6), "the figure carries " + Carried + " items" + Case);
                    Helper.assertTrue(looseCount(Helper, Odd) == (Drop ? 1 : 0), "the anomalous item " + (Drop ? "did not drop" : "dropped") + Case);
                    Helper.assertTrue(looseCount(Helper, Items.DIAMOND) == 0, "ordinary items dropped at capture" + Case);
                    Scp428Record Entry = Scp428Record.of(Scp428Looks.stray(Helper.getLevel().getRandom()), Taken);
                    Helper.assertTrue(Scp428Carry.dropsOnDeath(Entry, false) && !Scp428Carry.dropsOnDeath(Entry, true), "a player figure ignores keepInventory on death" + Case);
                }
                dismiss(Helper, User);
                DClass Person = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(6.5, stand(Helper), 4.5));
                Person.main().set(0, new ItemStack(Card));
                Person.main().set(1, new ItemStack(Items.DIAMOND, 3));
                Person.main().set(2, new ItemStack(Odd));
                Scp428Carry.Haul Held = Scp428Carry.capture(Person, Keep, Drop);
                Helper.assertTrue(Held.Owned() && !Held.FromPlayer(), "a D-Class capture was not owned" + Case);
                Helper.assertTrue(Person.pack().isEmpty(), "the D-Class kept items the figure took" + Case);
                int Hauled = Held.Stacks().stream().mapToInt(ItemStack::getCount).sum();
                Helper.assertTrue(Hauled == (Drop ? 4 : 5), "the D-Class figure carries " + Hauled + " items" + Case);
                Scp428Record Entry = Scp428Record.of(Scp428Looks.stray(Helper.getLevel().getRandom()), Held);
                Helper.assertTrue(Scp428Carry.dropsOnDeath(Entry, false) && Scp428Carry.dropsOnDeath(Entry, true), "a D-Class figure would not drop on death" + Case);
                Person.discard();
            }
        }
        clearLoose(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp428_capturedItemsDrop")
    public static void capturedItemsDrop(GameTestHelper Helper) {
        Scp428Core Core = core(Helper, 8.5, 16.5);
        DClass Person = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(11.0, stand(Helper), 16.5));
        Person.main().set(0, new ItemStack(Items.DIAMOND, 4));
        Person.main().set(1, new ItemStack(DoorRegistry.KeycardLevel2.get()));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 2 && bodies(Helper).size() == 2, "SCP-428 never took the D-Class in" + state(Core)))
                .thenExecute(() -> {
                    Helper.assertTrue(looseCount(Helper, Items.DIAMOND) == 0, "the capture dropped the D-Class items");
                    Scp428Record Entry = Core.records().get(1);
                    Helper.assertTrue(Entry.owned() && Entry.carried().stream().filter(Found -> Found.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum() == 4,
                            "the figure does not carry the D-Class items");
                    Scp428Instance Body = bodies(Helper).stream().filter(Found -> Entry.id().equals(Found.recordId())).findFirst().orElseThrow();
                    Body.hurt(Helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                })
                .thenExecuteAfter(3, () -> {
                    Helper.assertTrue(looseCount(Helper, Items.DIAMOND) == 4, "the dead D-Class figure dropped " + looseCount(Helper, Items.DIAMOND) + " diamonds");
                    Helper.assertTrue(looseCount(Helper, DoorRegistry.KeycardLevel2.get()) == 1, "the dead D-Class figure did not drop its card");
                    clearLoose(Helper);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp428_stasisKeepsItems")
    public static void stasisKeepsItems(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp428Core Core = core(Helper, 8.5, 16.5);
        FakePlayer User = watcher(Helper, 3.5, 3.5);
        Helper.runAfterDelay(3, () -> Core.adopt(Scp428Looks.stray(World.getRandom()), Core.position().add(1.5, 0.0, 0.0), 0.0f,
                carrying(true, new ItemStack(Items.EMERALD, 7), new ItemStack(DoorRegistry.KeycardLevel4.get()))));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 2 && bodies(Helper).size() == 2, "SCP-428 did not reach two" + state(Core)))
                .thenExecute(() -> {
                    ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
                    Helper.assertTrue(StasisPods.capture(World, User, Pod, Core), "the pod failed to capture SCP-428");
                    Helper.assertTrue(looseCount(Helper, Items.EMERALD) == 0, "capturing SCP-428 dropped carried items");
                    Helper.assertTrue(StasisPods.breakOut(World, Pod, Helper.absoluteVec(new Vec3(16.5, stand(Helper), 16.5)), null), "SCP-428 could not be released");
                })
                .thenExecuteAfter(3, () -> {
                    Scp428Core Back = cores(Helper).get(0);
                    Helper.assertTrue(looseCount(Helper, Items.EMERALD) == 0, "releasing SCP-428 dropped carried items");
                    int Kept = Back.records().stream().flatMap(Found -> Found.carried().stream()).filter(Found -> Found.is(Items.EMERALD)).mapToInt(ItemStack::getCount).sum();
                    Helper.assertTrue(Kept == 7, "the released records carry " + Kept + " emeralds");
                    Scp428Record Entry = Back.records().stream().filter(Found -> !Found.carried().isEmpty()).findFirst().orElseThrow();
                    Scp428Instance Body = bodies(Helper).stream().filter(Found -> Entry.id().equals(Found.recordId())).findFirst().orElseThrow();
                    Body.hurt(World.damageSources().genericKill(), Float.MAX_VALUE);
                })
                .thenExecuteAfter(3, () -> {
                    Helper.assertTrue(looseCount(Helper, Items.EMERALD) == 7, "after stasis the figure dropped " + looseCount(Helper, Items.EMERALD) + " emeralds");
                    Helper.assertTrue(cores(Helper).get(0).records().stream().allMatch(Found -> Found.carried().stream().noneMatch(Piece -> Piece.is(Items.EMERALD))),
                            "the emeralds are both dropped and still carried");
                    clearLoose(Helper);
                    dismiss(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    private static MtfTestPlayer squadCaller(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MtfTestPlayer User = new MtfTestPlayer(World, "scp428_mtf_caller");
        Vec3 Spot = Helper.absoluteVec(new Vec3(1.5, stand(Helper), 1.5));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.enlist(true);
        User.setGameMode(GameType.CREATIVE);
        return User;
    }

    private static void recall(GameTestHelper Helper, MtfTestPlayer User) {
        MtfMissions.cancel(Helper.getLevel().getServer(), User.getUUID());
        User.enlist(false);
        dismiss(Helper, User);
        Helper.killAllEntitiesOfClass(MtfOperative.class);
    }

    private static MtfOperative operative(GameTestHelper Helper, MtfTestPlayer User, double X, double Z) {
        Helper.assertTrue(MtfMissions.deployNow(User, MtfTier.Faint) == MtfMissions.Outcome.Called, "the fireteam was not deployed");
        MtfMission Mission = MtfMissions.get(User.getUUID());
        Helper.assertTrue(Mission != null, "the fireteam has no mission");
        MtfOperative Kept = null;
        for (UUID Id : List.copyOf(Mission.operatives())) {
            if (Kept == null && Helper.getLevel().getEntity(Id) instanceof MtfOperative Member && NpcGunner.isGun(Member.getMainHandItem())) {
                Kept = Member;
                continue;
            }
            if (Helper.getLevel().getEntity(Id) instanceof MtfOperative Spare) {
                Spare.discard();
            }
            Mission.operatives().remove(Id);
        }
        Helper.assertTrue(Kept != null, "the fireteam has no armed operative");
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, stand(Helper), Z));
        Kept.setNoAi(true);
        Kept.setTarget(null);
        Kept.moveTo(Spot.x, Spot.y, Spot.z, 90.0f, 0.0f);
        return Kept;
    }

    private static boolean holds(GameTestHelper Helper, Scp428Core Core, UUID Held) {
        return Held.equals(Core.gripVictim()) || Scp428Holds.coreOf(Held) != null || bodies(Helper).stream().anyMatch(Body -> Body.isHolding(Held));
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp428_absorbsOperative")
    public static void absorbsOperative(GameTestHelper Helper) {
        Scp428Core Core = core(Helper, 8.5, 16.5);
        MtfTestPlayer User = squadCaller(Helper);
        MtfOperative Member = operative(Helper, User, 11.0, 16.5);
        UUID MemberId = Member.getUUID();
        int Look = Member.appearance();
        Item Gun = Member.getMainHandItem().getItem();
        Map<EquipmentSlot, Item> Kit = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot Slot : Scp428Looks.Worn) {
            Kit.put(Slot, Member.getItemBySlot(Slot).getItem());
        }
        Helper.assertTrue(Kit.values().stream().noneMatch(Worn -> Worn == Items.AIR), "the operative is missing part of its kit");
        boolean[] Gripped = new boolean[1];
        Helper.onEachTick(() -> Gripped[0] |= holds(Helper, Core, MemberId));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Core.count() == 2, "SCP-428 did not take the operative in" + state(Core)))
                .thenExecute(() -> {
                    Helper.assertTrue(Gripped[0], "SCP-428 never held the operative before taking it");
                    Helper.assertTrue(Helper.getLevel().getEntity(MemberId) == null, "the operative's body was not replaced by a figure");
                    MtfMission Mission = MtfMissions.get(User.getUUID());
                    Helper.assertTrue(Mission == null || !Mission.operatives().contains(MemberId), "the squad still counts the taken operative");
                    Scp428Record Taken = Core.records().get(1);
                    Helper.assertTrue(Taken.appearance() == Look && !Taken.isPlayer(), "the record did not keep the operative's look");
                    Scp428Instance Body = bodies(Helper).stream().filter(Found -> Found.recordId() != null && Found.recordId().equals(Taken.id())).findFirst().orElse(null);
                    Helper.assertTrue(Body != null && Body.appearance() == Look, "no figure wears the operative's look");
                    for (EquipmentSlot Slot : Scp428Looks.Worn) {
                        Helper.assertTrue(Body.getItemBySlot(Slot).is(Kit.get(Slot)), "the figure is not wearing the operative's " + Slot.getName() + " gear");
                    }
                    ItemStack Weapon = Scp428Carry.weapon(Taken);
                    Helper.assertTrue(Weapon != null && Weapon.is(Gun), "the figure does not carry the operative's weapon");
                    Helper.assertTrue(!Scp428Carry.dropsOnDeath(Taken, false) && !Scp428Carry.dropsOnDeath(Taken, true), "the operative's issued gear would drop");
                    MtfMissions.cancel(Helper.getLevel().getServer(), User.getUUID());
                })
                .thenExecuteAfter(20, () -> {
                    Helper.assertTrue(Core.count() == 2 && Core.records().size() == 2 && bodies(Helper).size() == 2, "recalling the squad changed SCP-428" + state(Core));
                    Helper.assertTrue(!holds(Helper, Core, MemberId), "SCP-428 still holds the taken operative");
                    recall(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp428_recalledOperativeLetsGo")
    public static void recalledOperativeLetsGo(GameTestHelper Helper) {
        Scp428Core Core = core(Helper, 8.5, 16.5);
        MtfTestPlayer User = squadCaller(Helper);
        MtfOperative Member = operative(Helper, User, 11.0, 16.5);
        UUID MemberId = Member.getUUID();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(holds(Helper, Core, MemberId), "SCP-428 never took hold of the operative" + state(Core)))
                .thenExecute(() -> MtfMissions.cancel(Helper.getLevel().getServer(), User.getUUID()))
                .thenExecuteAfter(2, () -> Helper.assertTrue(Helper.getLevel().getEntity(MemberId) == null, "the recall left the operative behind"))
                .thenWaitUntil(() -> Helper.assertTrue(!holds(Helper, Core, MemberId), "SCP-428 still holds a recalled operative" + state(Core)))
                .thenExecuteAfter(80, () -> {
                    Helper.assertTrue(Core.count() == 1 && Core.records().size() == 1, "SCP-428 took in an operative that had been recalled" + state(Core));
                    Helper.assertTrue(bodies(Helper).stream().noneMatch(Body -> Body.isHolding(MemberId)), "a figure still holds the recalled operative");
                    recall(Helper, User);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    public static final class Victim extends FakePlayer {
        public int Deaths;

        public Victim(ServerLevel World, GameProfile Profile) {
            super(World, Profile);
            ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, this, 0, "spawnInvulnerableTime");
        }

        @Override
        public boolean isInvulnerableTo(DamageSource Source) {
            return isRemoved() || getAbilities().invulnerable && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        }

        @Override
        public void die(DamageSource Source) {
            if (CommonHooks.onLivingDeath(this, Source)) {
                return;
            }
            Deaths++;
        }
    }
}
