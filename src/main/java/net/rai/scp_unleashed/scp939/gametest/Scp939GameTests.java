package net.rai.scp_unleashed.scp939.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.antimemetic.AntiMemeticPillsItem;
import net.rai.scp_unleashed.antimemetic.AntiMemeticRegistry;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.gear.GearRegistry;
import net.rai.scp_unleashed.scp465.Scp465Room;
import net.rai.scp_unleashed.scp538.gametest.Scp538TestPlayer;
import net.rai.scp_unleashed.scp939.Scp939;
import net.rai.scp_unleashed.scp939.Scp939Breath;
import net.rai.scp_unleashed.scp939.Scp939Brood;
import net.rai.scp_unleashed.scp939.Scp939Cave;
import net.rai.scp_unleashed.scp939.Scp939Registry;
import net.rai.scp_unleashed.scp939.Scp939Rooms;
import net.rai.scp_unleashed.scp939.Scp939Senses;
import net.rai.scp_unleashed.scp939.compressor.AmnesticCanisterItem;
import net.rai.scp_unleashed.scp939.compressor.Amnestics;
import net.rai.scp_unleashed.scp939.compressor.CompressorStatus;
import net.rai.scp_unleashed.scp939.compressor.GasCompressorBlockEntity;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp939GameTests {
    public static final String Arena = "scp939_arena";
    public static final String Cave = "scp939_cave";
    public static final BlockPos CaveCentre = new BlockPos(8, 2, 8);
    public static final BlockPos CaveLow = new BlockPos(3, 2, 3);
    public static final BlockPos CaveHigh = new BlockPos(13, 5, 13);
    public static final BlockState Wall = Blocks.STONE_BRICKS.defaultBlockState();
    public static final BlockState Floor = Blocks.SMOOTH_STONE.defaultBlockState();

    private Scp939GameTests() {
    }

    private record Room(BlockPos Low, int SizeX, int Height, int SizeZ) {
        BlockPos centre() {
            return Low.offset(SizeX / 2, 0, SizeZ / 2);
        }
    }

    private static Room room(GameTestHelper Helper, BlockPos Low, int SizeX, int Height, int SizeZ) {
        for (int X = Low.getX() - 1; X <= Low.getX() + SizeX; X++) {
            for (int Z = Low.getZ() - 1; Z <= Low.getZ() + SizeZ; Z++) {
                boolean Edge = X < Low.getX() || X >= Low.getX() + SizeX || Z < Low.getZ() || Z >= Low.getZ() + SizeZ;
                for (int Y = Low.getY(); Y < Low.getY() + Height; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Edge ? Wall : Blocks.AIR.defaultBlockState());
                }
                Helper.setBlock(new BlockPos(X, Low.getY() + Height, Z), Wall);
                Helper.setBlock(new BlockPos(X, Low.getY() - 1, Z), Floor);
            }
        }
        Scp939Rooms.forgetAll();
        return new Room(Low, SizeX, Height, SizeZ);
    }

    private static Scp939 hunter(GameTestHelper Helper, BlockPos Relative, boolean Still) {
        ServerLevel World = Helper.getLevel();
        Scp939 Made = Scp939Registry.Scp939Type.get().create(World);
        Vec3 Where = Helper.absoluteVec(Vec3.atBottomCenterOf(Relative));
        Made.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        Made.setPersistenceRequired();
        Made.setNoAi(Still);
        World.addFreshEntity(Made);
        return Made;
    }

    private static Scp538TestPlayer player(GameTestHelper Helper, BlockPos Relative, String Name, GameType Mode) {
        ServerLevel World = Helper.getLevel();
        Scp538TestPlayer User = new Scp538TestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(Vec3.atBottomCenterOf(Relative));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(Mode);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static AABB area(GameTestHelper Helper) {
        return new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(40.0, 16.0, 40.0).inflate(2.0);
    }

    private static void sweep(GameTestHelper Helper) {
        for (Entity Each : Helper.getLevel().getEntitiesOfClass(Entity.class, area(Helper), Each -> !(Each instanceof ServerPlayer))) {
            Each.discard();
        }
        Scp939Rooms.forgetAll();
    }

    private static Scp465Room.Verdict verdict(GameTestHelper Helper, BlockPos Relative) {
        return Scp939Rooms.scan(Helper.getLevel(), Helper.absolutePos(Relative)).Result();
    }

    @GameTest(template = Cave, timeoutTicks = 400, batch = "scp939_caveArrival")
    public static void caveArrival(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer Reader = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp939_reader"));
        for (BlockPos Each : BlockPos.betweenClosed(CaveLow.offset(-1, -1, -1), new BlockPos(CaveHigh.getX() + 1, 13, CaveHigh.getZ() + 1))) {
            boolean Hollow = Each.getX() >= CaveLow.getX() && Each.getX() <= CaveHigh.getX() && Each.getY() >= CaveLow.getY() && Each.getY() <= CaveHigh.getY()
                    && Each.getZ() >= CaveLow.getZ() && Each.getZ() <= CaveHigh.getZ();
            Helper.setBlock(Each, Hollow ? Blocks.CAVE_AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
        }
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(World.getBrightness(LightLayer.SKY, Helper.absolutePos(CaveCentre)) == 0, "the sealed cave is still sky lit"))
                .thenExecute(() -> {
            AnomalyDocument Document = AnomalyDocuments.server().get(Scp939Registry.DocumentId);
            Helper.assertTrue(Document != null, "the scp_939 document did not load");
            Helper.assertTrue(Scp939Cave.problem(World, Helper.absolutePos(new BlockPos(1, 8, 1))) != null, "solid rock passed as a cave");
            Optional<BlockPos> Found = Scp939Cave.find(World, Helper.absolutePos(CaveCentre), 6);
            Helper.assertTrue(Found.isPresent(), "the cave search found nothing, centre says: " + Scp939Cave.problem(World, Helper.absolutePos(CaveCentre)));
            AABB Hollow = AABB.encapsulatingFullBlocks(Helper.absolutePos(CaveLow), Helper.absolutePos(CaveHigh));
            Helper.assertTrue(Hollow.contains(Vec3.atCenterOf(Found.get())), "the cave spot " + Found.get() + " is outside the hollow");
            boolean Placed = Scp939Cave.manifest(new AnomalySpawners.Discovery(World, Found.get(), Reader, Scp939Registry.DocumentId, Document, false));
            Helper.assertTrue(Placed, "the arrival placed nothing");
            List<Scp939> Pack = World.getEntitiesOfClass(Scp939.class, Hollow.inflate(1.0), Entity::isAlive);
            Helper.assertTrue(Pack.size() >= Scp939Cave.GroupMin && Pack.size() <= Scp939Cave.GroupMax, "the arrival placed " + Pack.size() + " instances");
            for (Scp939 Each : Pack) {
                Helper.assertTrue(!Each.isBaby(), "an arriving instance is young");
                Helper.assertTrue(Each.isPersistenceRequired(), "an arriving instance could despawn");
                Helper.assertTrue(Hollow.inflate(0.5).contains(Each.position()), "an instance arrived outside the cave");
            }
        }).thenIdle(20).thenExecute(() -> sweep(Helper)).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp939_hunting")
    public static void hunting(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp939 Hunter = hunter(Helper, new BlockPos(10, 1, 10), false);
        Cow Prey = Helper.spawn(EntityType.COW, new BlockPos(15, 1, 10));
        Scp538TestPlayer Creative = player(Helper, new BlockPos(11, 1, 12), "scp939_creative", GameType.CREATIVE);
        Scp538TestPlayer Sneak = player(Helper, new BlockPos(10, 1, 24), "scp939_sneak", GameType.SURVIVAL);
        Villager Brood = Helper.spawn(EntityType.VILLAGER, new BlockPos(8, 1, 12));
        Scp939Brood.mark(Brood, 100000);
        Helper.assertFalse(Scp939Senses.isPrey(Hunter, Creative), "a creative player counts as prey");
        Helper.assertFalse(Scp939Senses.isPrey(Hunter, Brood), "SCP-939-1 counts as prey");
        Helper.assertTrue(Scp939Senses.isPrey(Hunter, Sneak), "a survival player is not prey");
        Helper.assertTrue(Scp939Senses.isPrey(Hunter, Prey), "a cow is not prey");
        Sneak.setShiftKeyDown(true);
        Helper.assertTrue(Scp939Senses.reach(Sneak) == Scp939Senses.SmellRange, "sneaking does not muffle a player");
        Sneak.setShiftKeyDown(false);
        Sneak.setSprinting(true);
        Helper.assertTrue(Scp939Senses.reach(Sneak) == Scp939Senses.HearRun, "sprinting is not heard from further away");
        Sneak.setSprinting(false);
        Helper.assertFalse(Scp939Senses.senses(World, Hunter, Sneak), "a still player 14 blocks away was sensed");
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Hunter.getTarget() == Prey || !Prey.isAlive(), "the hunter has not gone after the cow, target " + Hunter.getTarget()))
                .thenExecute(() -> {
                    Helper.assertTrue(Hunter.getTarget() != Creative && Hunter.getTarget() != Brood, "the hunter picked a forbidden target");
                    dismiss(Helper, Creative);
                    dismiss(Helper, Sneak);
                    sweep(Helper);
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp939_roomSize")
    public static void roomSize(GameTestHelper Helper) {
        Room Largest = room(Helper, new BlockPos(3, 1, 3), 32, 4, 32);
        Helper.assertTrue(Scp939Rooms.accepted(verdict(Helper, Largest.centre())), "a sealed 32x32 room was refused: " + verdict(Helper, Largest.centre()));
        Helper.assertTrue(Scp939Rooms.roomAt(Helper.getLevel(), Helper.absolutePos(Largest.centre())).isPresent(), "the room cache refused a 32x32 room");
        Helper.assertTrue(Scp465Room.scan(Helper.getLevel(), Helper.absolutePos(Largest.centre())).Result() == Scp465Room.Verdict.TooBig,
                "the SCP-465 scan accepted a 32x32 room");
        Helper.setBlock(new BlockPos(3 + 32, 2, 10), Blocks.AIR);
        Scp939Rooms.forgetAll();
        Helper.assertFalse(Scp939Rooms.accepted(verdict(Helper, Largest.centre())), "a 32x32 room with a hole in the wall was accepted");
        Helper.assertTrue(Scp939Rooms.roomAt(Helper.getLevel(), Helper.absolutePos(Largest.centre())).isEmpty(), "an unsealed room counts as a cell");
        Room TooWide = room(Helper, new BlockPos(3, 1, 3), 33, 4, 20);
        Helper.assertTrue(verdict(Helper, TooWide.centre()) == Scp465Room.Verdict.TooBig, "a 33 block wide room was not too big: " + verdict(Helper, TooWide.centre()));
        Room Tall = room(Helper, new BlockPos(3, 1, 3), 10, Scp939Rooms.SpanUp + 1, 10);
        Helper.assertTrue(verdict(Helper, Tall.centre()) == Scp465Room.Verdict.TooBig, "a room taller than the limit was accepted: " + verdict(Helper, Tall.centre()));
        Room Small = room(Helper, new BlockPos(3, 1, 3), 3, 3, 3);
        Helper.assertTrue(Scp939Rooms.accepted(verdict(Helper, Small.centre())), "a small sealed cell was refused: " + verdict(Helper, Small.centre()));
        Room Standard = room(Helper, new BlockPos(3, 1, 3), 20, 4, 20);
        Helper.assertTrue(Scp465Room.scan(Helper.getLevel(), Helper.absolutePos(Standard.centre())).Result() == Scp465Room.Verdict.Fits,
                "the SCP-465 scan no longer accepts a 20x20 room");
        Scp939Rooms.forgetAll();
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp939_feeding")
    public static void feeding(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Cell = room(Helper, new BlockPos(5, 1, 5), 9, 4, 9);
        Scp939 Hunter = hunter(Helper, Cell.centre(), false);
        Hunter.scan(World);
        Helper.assertTrue(Hunter.isContained(), "the hunter in a sealed cell is not contained");
        Cow Prey = Helper.spawn(EntityType.COW, Cell.centre().offset(3, 0, 3));
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Hunter.fed() > 0, "the contained hunter has not eaten, target " + Hunter.getTarget()))
                .thenExecute(() -> {
                    Helper.assertFalse(Prey.isAlive(), "the cow is still alive");
                    Helper.assertTrue(Hunter.meals() >= 1, "the meal was not counted");
                    Helper.assertTrue(World.getEntitiesOfClass(ItemEntity.class, area(Helper)).isEmpty(), "the eaten cow dropped items");
                    Helper.assertTrue(Hunter.voices().contains("cow"), "the hunter did not learn the cow's voice");
                    sweep(Helper);
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp939_brood")
    public static void brood(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Cell = room(Helper, new BlockPos(5, 1, 5), 9, 4, 9);
        Scp939 Parent = hunter(Helper, Cell.centre(), true);
        Parent.scan(World);
        Helper.assertTrue(Parent.isContained(), "the parent is not contained");
        Helper.assertFalse(Parent.canBrood(World), "an unfed instance can produce SCP-939-1");
        Parent.setMeals(Scp939.MealsForBrood);
        Helper.assertTrue(Parent.canBrood(World), "a fed contained instance cannot produce SCP-939-1");
        Parent.setBroodWait(100);
        Helper.assertFalse(Parent.canBrood(World), "the brood cooldown is ignored");
        Parent.setBroodWait(0);
        Parent.heave(World);
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(!World.getEntitiesOfClass(Villager.class, area(Helper), Scp939Brood::isBrood).isEmpty(),
                "no SCP-939-1 appeared")).thenExecute(() -> {
            Villager Child = World.getEntitiesOfClass(Villager.class, area(Helper), Scp939Brood::isBrood).get(0);
            Helper.assertTrue(Child.getType() == EntityType.VILLAGER, "SCP-939-1 is not a villager");
            Helper.assertTrue("minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(Child.getType()).getNamespace()), "SCP-939-1 has a modded entity id");
            Helper.assertTrue("entity.minecraft.villager".equals(Child.getType().getDescriptionId()), "SCP-939-1 has the wrong name key");
            Helper.assertTrue(Child.isBaby(), "SCP-939-1 is not a baby");
            Helper.assertTrue(!Child.hasCustomName(), "SCP-939-1 has a custom name");
            Helper.assertTrue(Parent.meals() == 0 && Parent.broodWait() == Scp939.BroodCooldown, "the parent did not reset after producing");
            Helper.assertFalse(Parent.canBrood(World), "the parent can produce again at once");
            sweep(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp939_burst")
    public static void burst(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Villager Child = Helper.spawn(EntityType.VILLAGER, new BlockPos(10, 1, 10));
        Scp939Brood.mark(Child, 2);
        float[] Health = {0.0f};
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Scp939Brood.isShaking(Child), "SCP-939-1 has not begun to shake"))
                .thenExecute(() -> Health[0] = Child.getHealth())
                .thenIdle(60).thenExecute(() -> {
                    Helper.assertTrue(Child.isFullyFrozen(), "the shaking SCP-939-1 does not shake on screen");
                    Helper.assertTrue(Child.getHealth() >= Health[0], "shaking hurt SCP-939-1: " + Child.getHealth() + " of " + Health[0]);
                    Helper.assertTrue(Child.isBaby(), "SCP-939-1 grew up");
                }).thenWaitUntil(() -> Helper.assertTrue(Child.isRemoved(), "SCP-939-1 has not burst")).thenExecute(() -> {
                    List<Scp939> Young = World.getEntitiesOfClass(Scp939.class, area(Helper), Entity::isAlive);
                    Helper.assertTrue(Young.size() == 1, "the burst released " + Young.size() + " instances");
                    Helper.assertTrue(Young.get(0).isBaby(), "the released instance is not young");
                    Helper.assertTrue(Young.get(0).position().distanceTo(Child.position()) < 1.5, "the young instance appeared away from SCP-939-1");
                    sweep(Helper);
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp939_growth")
    public static void growth(GameTestHelper Helper) {
        Scp939 Young = hunter(Helper, new BlockPos(10, 1, 10), true);
        Young.setAge(-60);
        Helper.assertTrue(Young.isBaby(), "the young instance is not young");
        Helper.assertTrue(Young.getBbWidth() < Scp939.Width * 0.75, "the young instance is full size");
        Scp939 Fed = hunter(Helper, new BlockPos(20, 1, 20), true);
        Fed.setAge(-Scp939.GrowTicks);
        Fed.feed(Helper.getLevel(), Helper.spawn(EntityType.CHICKEN, new BlockPos(22, 1, 22)));
        Helper.assertTrue(Fed.getAge() > -Scp939.GrowTicks + 20, "feeding did not speed up growth: " + Fed.getAge());
        Helper.startSequence().thenWaitUntil(() -> Helper.assertFalse(Young.isBaby(), "the young instance has not grown")).thenExecute(() -> {
            Helper.assertTrue(Young.getBbWidth() >= Scp939.Width - 0.01, "the grown instance is still small");
            sweep(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp939_exposure")
    public static void exposure(GameTestHelper Helper) {
        Scp939 Hunter = hunter(Helper, new BlockPos(10, 1, 10), true);
        Scp538TestPlayer Near = player(Helper, new BlockPos(12, 1, 10), "scp939_near", GameType.SURVIVAL);
        Scp538TestPlayer Masked = player(Helper, new BlockPos(10, 1, 12), "scp939_masked", GameType.SURVIVAL);
        Scp538TestPlayer Far = player(Helper, new BlockPos(30, 1, 30), "scp939_far", GameType.SURVIVAL);
        Masked.setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.GasMask.get()));
        try {
            for (int Step = 0; Step < 20; Step++) {
                Scp939Breath.breathe(Near, Scp939Breath.TickEvery);
                Scp939Breath.breathe(Masked, Scp939Breath.TickEvery);
                Scp939Breath.breathe(Far, Scp939Breath.TickEvery);
            }
            float Gained = Scp939Breath.exposure(Near);
            Helper.assertTrue(Gained > 0.5f, "standing by an instance gave no exposure: " + Gained);
            Helper.assertTrue(Scp939Breath.exposure(Masked) == 0.0f, "a gas mask did not stop the breath");
            Helper.assertTrue(Scp939Breath.masked(Masked) && !Scp939Breath.masked(Near), "the gas mask check is wrong");
            Helper.assertTrue(Scp939Breath.breath(Helper.getLevel(), Masked) == 0.0f, "the masked player still sees the breath");
            Helper.assertTrue(Scp939Breath.exposure(Far) == 0.0f, "a far player gained exposure");
            Scp939Breath.expose(Near, 50.0f);
            float Before = Scp939Breath.exposure(Near);
            Scp939Breath.breathe(Near, Scp939Breath.TickEvery);
            Helper.assertTrue(Scp939Breath.exposure(Near) >= Before, "exposure fell while still near");
            Hunter.discard();
            for (int Step = 0; Step < Scp939Breath.DecayTicks / Scp939Breath.TickEvery; Step++) {
                Scp939Breath.breathe(Near, Scp939Breath.TickEvery);
            }
            Helper.assertTrue(Scp939Breath.exposure(Near) < Before && Scp939Breath.exposure(Near) >= Before - 1.5f, "exposure did not decay slowly: " + Scp939Breath.exposure(Near));
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(Near, Near.damageSources().genericKill()));
            Helper.assertTrue(Scp939Breath.exposure(Near) == 0.0f, "dying did not reset exposure");
        } finally {
            dismiss(Helper, Near);
            dismiss(Helper, Masked);
            dismiss(Helper, Far);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp939_haze")
    public static void haze(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Cell = room(Helper, new BlockPos(5, 1, 5), 9, 4, 9);
        Scp939 Hunter = hunter(Helper, Cell.centre(), true);
        Scp538TestPlayer Inside = player(Helper, Cell.centre().offset(3, 0, 3), "scp939_inside", GameType.SURVIVAL);
        Scp538TestPlayer Masked = player(Helper, Cell.centre().offset(-3, 0, 3), "scp939_inside_masked", GameType.SURVIVAL);
        Masked.setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.GasMask.get()));
        Hunter.setHaze(0.9f);
        try {
            Hunter.scan(World);
            Helper.assertTrue(Hunter.haze() > 0.9f, "the haze did not build in a sealed cell");
            Helper.assertTrue(Hunter.isHazed(), "a thick haze does not hide the instance");
            Helper.assertTrue(Scp939Breath.breath(World, Inside) >= 0.85f, "the haze does not reach the far side of the cell");
            Helper.assertTrue(Scp939Breath.breath(World, Masked) == 0.0f, "the mask does not clear the haze");
            Helper.setBlock(new BlockPos(5 + 9, 2, 8), Blocks.AIR);
            Helper.setBlock(new BlockPos(5 + 9, 3, 8), Blocks.AIR);
            Scp939Rooms.forgetAll();
            for (int Step = 0; Step < 10; Step++) {
                Hunter.scan(World);
            }
            Helper.assertFalse(Hunter.isContained(), "an opened cell still counts as sealed");
            Helper.assertTrue(Hunter.haze() < 0.5f, "the haze did not vent from an open cell: " + Hunter.haze());
        } finally {
            dismiss(Helper, Inside);
            dismiss(Helper, Masked);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp939_footsteps")
    public static void footsteps(GameTestHelper Helper) {
        Helper.assertTrue(Scp939.contactsCrossed(0.2, 0.3, Scp939.WalkContacts) == 1, "a walk step at 0.25 was missed");
        Helper.assertTrue(Scp939.contactsCrossed(0.3, 0.45, Scp939.WalkContacts) == 0, "a walk step played between foot plants");
        Helper.assertTrue(Scp939.contactsCrossed(0.95, 1.05, Scp939.WalkContacts) == 1, "the step at the cycle start was missed");
        Helper.assertTrue(Scp939.contactsCrossed(0.0, 1.0, Scp939.WalkContacts) == 4, "a whole walk cycle does not give four steps");
        Helper.assertTrue(Scp939.contactsCrossed(0.05, 0.12, Scp939.SprintContacts) == 1, "a sprint step at 0.1 was missed");
        Helper.assertTrue(Scp939.contactsCrossed(0.5, 0.5, Scp939.SprintContacts) == 0, "standing still played a step");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp939_lure")
    public static void lure(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp939 Hunter = hunter(Helper, new BlockPos(10, 1, 10), false);
        Scp538TestPlayer Prey = player(Helper, new BlockPos(17, 1, 10), "scp939_lured", GameType.SURVIVAL);
        Helper.assertTrue(Scp939Senses.lureable(World, Hunter, Prey), "a sensed player beyond lunging range cannot be lured");
        Vec3 Far = Helper.absoluteVec(new Vec3(30.5, 1.0, 10.5));
        Prey.teleportTo(Far.x, Far.y, Far.z);
        Hunter.beginLure(Prey);
        Helper.startSequence().thenIdle(60).thenExecute(() -> {
            Helper.assertTrue(Hunter.isLuring(), "the hunt mode ended early");
            Helper.assertTrue(Hunter.getTarget() == null, "the hunter attacked while luring");
            Helper.assertTrue(Hunter.isProwling(), "the hunter does not stalk while luring");
            Helper.assertTrue(Hunter.calls() >= 1, "the hunter made no villager calls");
            Helper.assertTrue(Hunter.calls() <= 2, "the hunter spammed calls: " + Hunter.calls());
            Vec3 Close = Hunter.position().add(3.0, 0.0, 0.0);
            Prey.teleportTo(Close.x, Close.y, Close.z);
        }).thenWaitUntil(() -> Helper.assertTrue(!Hunter.isLuring() && Hunter.getTarget() == Prey, "the hunt mode did not end in an attack")).thenExecute(() -> {
            Helper.assertTrue(Hunter.lureWait() > 0, "the hunt mode has no cooldown");
            Hunter.setTarget(null);
            Hunter.setLureWait(0);
            Prey.teleportTo(Hunter.getX() + 8.0, Hunter.getY(), Hunter.getZ());
            Hunter.beginLure(Prey);
            Hunter.setLureLeft(3);
        }).thenIdle(10).thenExecute(() -> {
            Helper.assertFalse(Hunter.isLuring(), "the hunt mode never timed out");
            Helper.assertTrue(Hunter.lureWait() > 0, "a timed out hunt mode has no cooldown");
            dismiss(Helper, Prey);
            sweep(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp939_lureContained")
    public static void lureContained(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Cell = room(Helper, new BlockPos(5, 1, 5), 9, 4, 9);
        Scp939 Hunter = hunter(Helper, Cell.centre(), true);
        Hunter.scan(World);
        Scp538TestPlayer Outside = player(Helper, Cell.centre().offset(7, 0, 0), "scp939_outside", GameType.SURVIVAL);
        try {
            Helper.assertTrue(Hunter.isContained(), "the hunter is not contained");
            Helper.assertFalse(Scp939Senses.senses(World, Hunter, Outside), "a contained hunter can attack a player outside its cell");
            Helper.assertTrue(Scp939Senses.lureable(World, Hunter, Outside), "a contained hunter cannot call to a player outside its cell");
            Helper.assertTrue(Scp939Senses.lureCandidate(World, Hunter) == Outside, "the outside player is not picked for a call");
            Hunter.beginLure(Outside);
            Hunter.lureCall(World);
            Helper.assertTrue(Hunter.calls() == 1, "the contained hunter did not call");
            Helper.assertTrue(Hunter.getTarget() == null, "calling set an attack target");
        } finally {
            dismiss(Helper, Outside);
            sweep(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp939_stasis")
    public static void stasis(GameTestHelper Helper) {
        Scp939 Hunter = hunter(Helper, new BlockPos(10, 1, 10), true);
        Helper.assertFalse(Hunter.getType().is(StasisRegistry.Immune), "SCP-939 cannot be moved in a stasis pod");
        Helper.assertTrue(Hunter.getType().canSerialize(), "SCP-939 cannot be saved into a pod");
        sweep(Helper);
        Helper.succeed();
    }

    private static GasCompressorBlockEntity compressor(GameTestHelper Helper, BlockPos Relative, int Canisters) {
        Helper.setBlock(Relative, Scp939Registry.Compressor.get().defaultBlockState());
        GasCompressorBlockEntity Device = (GasCompressorBlockEntity) Helper.getLevel().getBlockEntity(Helper.absolutePos(Relative));
        Device.getStorage().setStackInSlot(GasCompressorBlockEntity.InputSlot, new ItemStack(Scp939Registry.EmptyCanister.get(), Canisters));
        return Device;
    }

    @GameTest(template = Arena, timeoutTicks = 1100, batch = "gas_compressor_room")
    public static void compressorNeedsRoomPowerAndSource(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        GasCompressorBlockEntity Open = compressor(Helper, new BlockPos(30, 1, 30), 4);
        Room Cell = room(Helper, new BlockPos(5, 1, 5), 12, 4, 12);
        GasCompressorBlockEntity Inside = compressor(Helper, Cell.centre(), 4);
        boolean[] Powered = {false};
        Helper.onEachTick(() -> {
            Open.insertPower(Open.getPowerDemand() > 0 ? Open.getPowerDemand() : GasCompressorBlockEntity.WorkDraw);
            if (Powered[0]) {
                Inside.insertPower(Inside.getPowerDemand());
            }
        });
        Helper.startSequence().thenIdle(60).thenExecute(() -> {
            Helper.assertTrue(Open.status() == CompressorStatus.Thin, "the compressor in the open is " + Open.status());
            Helper.assertTrue(Inside.status() == CompressorStatus.Thin, "the compressor in an empty cell is " + Inside.status());
            for (int Index = 0; Index < 4; Index++) {
                hunter(Helper, Cell.centre().offset(-4 + Index * 2, 0, 3), true);
            }
            Inside.recheck();
        }).thenIdle(60).thenExecute(() -> {
            Helper.assertTrue(Inside.sources() >= 4.0f, "the compressor counts " + Inside.sources() + " sources");
            Helper.assertTrue(Inside.status() == CompressorStatus.Unpowered, "the unpowered compressor is " + Inside.status());
            Helper.assertTrue(Inside.getStorage().getStackInSlot(GasCompressorBlockEntity.OutputSlot).isEmpty(), "the unpowered compressor made a canister");
            Powered[0] = true;
        }).thenWaitUntil(() -> Helper.assertTrue(Inside.made() >= 1, "the powered compressor has not filled a canister, status " + Inside.status())).thenExecute(() -> {
            ItemStack Output = Inside.getStorage().getStackInSlot(GasCompressorBlockEntity.OutputSlot);
            Helper.assertTrue(Output.is(Scp939Registry.Canister.get()), "the output is not an amnestic canister");
            Helper.assertTrue(Inside.getStorage().getStackInSlot(GasCompressorBlockEntity.InputSlot).getCount() == 3, "an empty canister was not used up");
            Helper.assertTrue(Open.made() == 0, "the compressor in the open made a canister");
            Helper.assertTrue(GasCompressorBlockEntity.ticksFor(1.0f) > GasCompressorBlockEntity.ticksFor(4.0f), "more sources do not fill faster");
            sweep(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "canister_use")
    public static void canisterUse(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp538TestPlayer User = player(Helper, new BlockPos(10, 1, 10), "scp939_canister", GameType.SURVIVAL);
        Scp538TestPlayer Masked = player(Helper, new BlockPos(14, 1, 10), "scp939_canister_masked", GameType.SURVIVAL);
        Masked.setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.GasMask.get()));
        try {
            User.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, 100, 0, false, true, true));
            ItemStack Stack = new ItemStack(Scp939Registry.Canister.get(), 3);
            User.setItemInHand(InteractionHand.MAIN_HAND, Stack);
            ItemStack Left = Scp939Registry.Canister.get().finishUsingItem(Stack, World, User);
            MobEffectInstance Shield = User.getEffect(AntiMemeticRegistry.Shielded);
            Helper.assertTrue(Shield != null && Shield.getDuration() >= AntiMemeticPillsItem.EffectTicks - 1, "the anti-memetic effect was not refreshed");
            MobEffectInstance Haze = User.getEffect(Scp939Registry.Amnestic);
            Helper.assertTrue(Haze != null && Haze.getAmplifier() == 0, "the first dose did not give the mild amnestic effect");
            Helper.assertTrue(Scp939Breath.exposure(User) >= Amnestics.ExposurePerUse, "the dose did not raise exposure");
            Helper.assertTrue(Left.getCount() == 2, "the canister was not used up");
            Helper.assertTrue(User.getInventory().countItem(Scp939Registry.EmptyCanister.get()) == 1, "no empty canister came back");
            Helper.assertTrue(User.getCooldowns().isOnCooldown(Scp939Registry.Canister.get()), "the canister has no cooldown");
            Scp939Registry.Canister.get().finishUsingItem(Left, World, User);
            Scp939Registry.Canister.get().finishUsingItem(Left, World, User);
            MobEffectInstance Strong = User.getEffect(Scp939Registry.Amnestic);
            Helper.assertTrue(Strong != null && Strong.getAmplifier() >= 1, "overuse did not strengthen the effect");
            Helper.assertTrue(Strong.getDuration() >= Amnestics.amnesticTicks(Strong.getAmplifier()) - 1, "the amnestic effect was not set to its full length");
            Helper.assertTrue(User.getEffect(Scp939Registry.Amnestic).getEffect().is(Scp939Registry.Amnestics), "the amnestic effect is not refreshable");
            ItemStack MaskedStack = new ItemStack(Scp939Registry.Canister.get());
            Masked.setItemInHand(InteractionHand.MAIN_HAND, MaskedStack);
            Helper.assertFalse(Scp939Registry.Canister.get().use(World, Masked, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                    "the canister could be used through a mask");
            Helper.assertTrue(AmnesticCanisterItem.UseTicks > 0, "the canister has no use time");
        } finally {
            dismiss(Helper, User);
            dismiss(Helper, Masked);
        }
        Helper.succeed();
    }
}
