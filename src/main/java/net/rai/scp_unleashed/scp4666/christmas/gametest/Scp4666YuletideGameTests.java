package net.rai.scp_unleashed.scp4666.christmas.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666DoorNote;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666NoteEvent;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666Sighting;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666Sightings;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666Visit;
import net.rai.scp_unleashed.seasonal.Season;
import net.rai.scp_unleashed.seasonal.SeasonMode;
import net.rai.scp_unleashed.seasonal.Seasons;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp4666YuletideGameTests {
    public static final String Arena = "scp458_arena";

    private Scp4666YuletideGameTests() {
    }

    private static FakePlayer player(GameTestHelper Helper, BlockPos Spot) {
        FakePlayer User = FakePlayerFactory.get(Helper.getLevel(), new GameProfile(UUID.randomUUID(), "yule_" + Helper.getTick()));
        User.moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, 0.0f, 0.0f);
        return User;
    }

    private static void floor(GameTestHelper Helper) {
        for (int X = 0; X < 16; X++) {
            for (int Z = 0; Z < 16; Z++) {
                Helper.setBlock(new BlockPos(X, 0, Z), Blocks.STONE);
                for (int Y = 1; Y < 5; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.AIR);
                }
            }
        }
    }

    private static BlockState door(Direction Facing, boolean Open) {
        return Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Facing).setValue(DoorBlock.OPEN, Open).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
    }

    private static void placeDoor(ServerLevel World, BlockPos Lower, Direction Facing, boolean Open) {
        BlockState State = door(Facing, Open);
        World.setBlock(Lower, State.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 18);
        World.setBlock(Lower.above(), State.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 18);
    }

    private static List<Scp4666DoorNote> notes(ServerLevel World, BlockPos Around) {
        return World.getEntitiesOfClass(Scp4666DoorNote.class, new AABB(Around).inflate(4.0));
    }

    private static boolean holds(FakePlayer User) {
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            if (User.getInventory().getItem(Slot).is(Scp4666Registry.SaintNote.get())) {
                return true;
            }
        }
        return false;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp4666_note_schedule")
    public static void scp4666NoteOnlyAfterSurviving(GameTestHelper Helper) {
        floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Door = Helper.absolutePos(new BlockPos(8, 1, 8));
        placeDoor(World, Door, Direction.NORTH, false);
        FakePlayer User = player(Helper, Door.south(3));
        Scp4666Visit Visit = Scp4666Visit.of(User);
        Helper.assertFalse(Visit.pending(), "a fresh player already has a note waiting");
        Scp4666NoteEvent.tick(User, Visit);
        Helper.assertTrue(notes(World, Door).isEmpty(), "a note appeared for a player who never survived SCP-4666");
        long Now = World.getServer().overworld().getGameTime();
        Scp4666NoteEvent.survived(User);
        Helper.assertTrue(Visit.Survived && Visit.Read, "surviving was not remembered");
        Helper.assertTrue(Visit.pending(), "surviving did not schedule the note");
        Helper.assertTrue(Visit.NoteAt >= Now + Scp4666NoteEvent.DelayMin && Visit.NoteAt <= Now + Scp4666NoteEvent.DelayMin + Scp4666NoteEvent.DelaySpread,
                "the note is due outside one to three days: " + (Visit.NoteAt - Now));
        long First = Visit.NoteAt;
        Scp4666NoteEvent.survived(User);
        Helper.assertTrue(Visit.NoteAt == First, "surviving twice moved the note");
        Scp4666NoteEvent.tick(User, Visit);
        Helper.assertTrue(notes(World, Door).isEmpty(), "the note appeared before it was due");
        Scp4666Visit Copy = Scp4666Visit.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, Scp4666Visit.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, Visit).getOrThrow()).getOrThrow();
        Helper.assertTrue(Copy.NoteAt == Visit.NoteAt && Copy.Survived && !Copy.NoteDone, "the visit did not survive a save");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_note_door")
    public static void scp4666NoteSitsOnDoorAndFollowsIt(GameTestHelper Helper) {
        floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Door = Helper.absolutePos(new BlockPos(8, 1, 8));
        placeDoor(World, Door, Direction.NORTH, false);
        FakePlayer User = player(Helper, Door.south(3));
        Scp4666Visit Visit = Scp4666Visit.of(User);
        Scp4666NoteEvent.survived(User);
        Visit.rememberDoor(Door);
        Optional<Scp4666NoteEvent.Choice> Picked = Scp4666NoteEvent.choose(World, Door.south(6), Door.south(3), Visit);
        Helper.assertTrue(Picked.isPresent() && Picked.get().Door().equals(Door) && Picked.get().Home(), "the remembered door near home was not chosen");
        Helper.assertTrue(Picked.get().Side().getAxis() == Direction.Axis.Z, "the note side is not a face of the door");
        Optional<Scp4666DoorNote> Placed = Scp4666NoteEvent.place(User, Visit, Picked.get());
        Helper.assertTrue(Placed.isPresent() && Visit.NoteDone, "the note was not placed");
        Scp4666DoorNote Note = Placed.get();
        Vec3 Centre = Vec3.atBottomCenterOf(Door);
        Helper.assertTrue(Math.abs(Note.getY() - (Door.getY() + Scp4666DoorNote.NoteLift)) < 1.0E-3, "the note is not at eye height on the door");
        Helper.assertTrue(Math.abs(Note.getX() - Centre.x) < 0.01, "the note is not centred on the closed door");
        double Face = Picked.get().Side() == Direction.SOUTH ? Door.getZ() + 1.0 + Scp4666DoorNote.FaceGap : Door.getZ() + 13.0 / 16.0 - Scp4666DoorNote.FaceGap;
        Helper.assertTrue(Math.abs(Note.getZ() - Face) < 1.0E-3, "the note is not on the door face: " + Note.getZ() + " wanted " + Face);
        Direction Closed = Note.placement().Normal();
        placeDoor(World, Door, Direction.NORTH, true);
        Helper.startSequence().thenExecuteAfter(2, () -> {
            Scp4666DoorNote.Placement Open = Note.placement();
            Helper.assertTrue(Open != null && Open.Normal() != Closed && Open.Normal().getAxis() == Direction.Axis.X, "the note did not turn with the opened door");
            Helper.assertTrue(Note.position().distanceTo(Open.Spot()) < 1.0E-3, "the note did not follow the opened door");
            Helper.assertTrue(Note.position().distanceTo(Vec3.atCenterOf(Door)) < 1.0, "the note left the door");
            Helper.assertTrue(Note.isAlive(), "opening the door removed the note");
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp4666_note_once")
    public static void scp4666NoteOncePerPlayerAndTaken(GameTestHelper Helper) {
        floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Door = Helper.absolutePos(new BlockPos(8, 1, 8));
        placeDoor(World, Door, Direction.EAST, false);
        FakePlayer User = player(Helper, Door.west(2));
        Scp4666Visit Visit = Scp4666Visit.of(User);
        Scp4666NoteEvent.survived(User);
        Optional<Scp4666NoteEvent.Choice> Picked = Scp4666NoteEvent.choose(World, Door.above(100), Door.west(2), Visit);
        Helper.assertTrue(Picked.isPresent() && Picked.get().Door().equals(Door) && !Picked.get().Home(), "with no door at home the nearest door to the player was not used");
        Helper.assertTrue(Scp4666NoteEvent.place(User, Visit, Picked.get()).isPresent(), "the note was not placed");
        Visit.NoteAt = 1L;
        Helper.assertFalse(Scp4666NoteEvent.due(User, Visit), "a second note is due after the first");
        Scp4666NoteEvent.tick(User, Visit);
        Scp4666NoteEvent.survived(User);
        Helper.assertFalse(Visit.pending(), "surviving again scheduled another note");
        List<Scp4666DoorNote> Found = notes(World, Door);
        Helper.assertTrue(Found.size() == 1, "expected one note, found " + Found.size());
        Found.get(0).interact(User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(holds(User), "taking the note did not give the note item");
        Helper.assertTrue(Found.get(0).isRemoved(), "the note stayed on the door after it was taken");
        ItemStack Stack = new ItemStack(Scp4666Registry.SaintNote.get());
        Helper.assertTrue(Stack.getMaxStackSize() == 1, "the note item stacks");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp4666_note_broken")
    public static void scp4666NoteDropsWhenDoorBreaks(GameTestHelper Helper) {
        floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Door = Helper.absolutePos(new BlockPos(8, 1, 8));
        BlockPos Other = Helper.absolutePos(new BlockPos(3, 1, 8));
        BlockPos Remembered = Helper.absolutePos(new BlockPos(13, 1, 8));
        placeDoor(World, Door, Direction.SOUTH, false);
        placeDoor(World, Other, Direction.SOUTH, false);
        placeDoor(World, Remembered, Direction.SOUTH, false);
        FakePlayer User = player(Helper, Door.north(2));
        Scp4666Visit Visit = Scp4666Visit.of(User);
        Visit.rememberDoor(Remembered);
        Optional<Scp4666NoteEvent.Choice> Picked = Scp4666NoteEvent.choose(World, Other, Door.north(2), Visit);
        Helper.assertTrue(Picked.isPresent() && Picked.get().Door().equals(Remembered), "a door the player placed did not win over a closer one");
        Helper.assertTrue(Scp4666NoteEvent.nearest(World, Door.north(), 3, 2) != null && Scp4666NoteEvent.nearest(World, Door.north(), 3, 2).equals(Door),
                "the door scan missed the nearest door");
        Scp4666DoorNote Note = Scp4666DoorNote.attach(World, Door, Direction.NORTH, User.getUUID());
        Helper.assertTrue(Note != null, "the note could not be attached");
        World.setBlock(Door.above(), Blocks.AIR.defaultBlockState(), 18);
        World.setBlock(Door, Blocks.AIR.defaultBlockState(), 18);
        Helper.startSequence().thenExecuteAfter(3, () -> {
            Helper.assertTrue(Note.isRemoved(), "the note floats where the door was");
            boolean Dropped = World.getEntitiesOfClass(ItemEntity.class, new AABB(Door).inflate(2.0)).stream().anyMatch(Item -> Item.getItem().is(Scp4666Registry.SaintNote.get()));
            Helper.assertTrue(Dropped, "the note did not drop as an item");
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp4666_sighting_rules")
    public static void scp4666SightingNeedsSeasonFileAndCooldown(GameTestHelper Helper) {
        LocalDate Today = LocalDate.of(2026, 12, 24);
        long Now = 500000L;
        Scp4666Visit Visit = new Scp4666Visit();
        Helper.assertTrue(Scp4666Sightings.decide(false, Visit, true, true, true, Now, Today) == Scp4666Sightings.Refusal.Season, "the sighting ignores the season");
        Helper.assertTrue(Scp4666Sightings.decide(true, Visit, true, true, true, Now, Today) == Scp4666Sightings.Refusal.Unread, "the sighting ignores the unread file");
        Visit.Read = true;
        Helper.assertTrue(Scp4666Sightings.decide(true, Visit, true, true, true, Now, Today) == Scp4666Sightings.Refusal.None, "a reader outdoors at night in season was refused");
        Helper.assertTrue(Scp4666Sightings.decide(true, Visit, true, false, true, Now, Today) == Scp4666Sightings.Refusal.Indoors, "the sighting appears indoors");
        Helper.assertTrue(Scp4666Sightings.decide(true, Visit, true, true, false, Now, Today) == Scp4666Sightings.Refusal.Daytime, "the sighting appears by day");
        Visit.SightingTick = Now - 100L;
        Helper.assertTrue(Scp4666Sightings.decide(true, Visit, true, true, true, Now, Today) == Scp4666Sightings.Refusal.Cooldown, "the cooldown is ignored");
        Visit.SightingTick = Now - Scp4666Sightings.CooldownTicks;
        Visit.SightingDay = Today.toEpochDay();
        Helper.assertTrue(Scp4666Sightings.decide(true, Visit, true, true, true, Now, Today) == Scp4666Sightings.Refusal.SameDay, "two sightings fit in one real day");
        Helper.assertTrue(Scp4666Sightings.decide(true, Visit, true, true, true, Now, Today.plusDays(1)) == Scp4666Sightings.Refusal.None, "the next day is still refused");
        Helper.assertTrue(Scp4666Sightings.Chance > 0.0f && Scp4666Sightings.Chance <= 0.02f, "the sighting is too common");
        FakePlayer User = player(Helper, Helper.absolutePos(new BlockPos(8, 1, 8)));
        Scp4666Visit Fresh = Scp4666Visit.of(User);
        Scp4666Sightings.noticeRead(User, Fresh);
        Helper.assertFalse(Fresh.Read, "a player who never read the file counts as a reader");
        ItemStack File = new ItemStack(ArchiveRegistry.OpenedDocument.get());
        File.set(ArchiveRegistry.StampComponent.get(), new DocumentStamp(Scp4666Registry.DocumentId, DocumentClass.Keter));
        User.getInventory().add(File);
        Scp4666Sightings.noticeRead(User, Fresh);
        Helper.assertTrue(Fresh.Read, "holding the opened SCP-4666 file did not count as reading it");
        Seasons.force(Season.Christmas, SeasonMode.Off);
        Seasons.refresh();
        Helper.assertTrue(Scp4666Sightings.refusal(User, Fresh, Today) == Scp4666Sightings.Refusal.Season, "the sighting runs with Christmas forced off");
        Seasons.force(Season.Christmas, SeasonMode.On);
        Seasons.refresh();
        Helper.assertTrue(Scp4666Sightings.refusal(User, Fresh, Today) != Scp4666Sightings.Refusal.Season, "the sighting is refused with Christmas forced on");
        Seasons.force(Season.Christmas, SeasonMode.Auto);
        Seasons.refresh();
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp4666_sighting_far")
    public static void scp4666SightingStaysFarAndInView(GameTestHelper Helper) {
        RandomSource Random = RandomSource.create(4666L);
        Vec3 Origin = new Vec3(10.0, 70.0, -20.0);
        for (int Draw = 0; Draw < 400; Draw++) {
            float Facing = Random.nextFloat() * 360.0f - 180.0f;
            Vec3 Aim = Scp4666Sightings.aim(Origin, Facing, Random, Scp4666Sightings.FarDistance);
            double Distance = Math.hypot(Aim.x - Origin.x, Aim.z - Origin.z);
            Helper.assertTrue(Distance >= Scp4666Sightings.NearDistance - 1.0E-6 && Distance <= Scp4666Sightings.FarDistance + 1.0E-6, "a sighting was placed " + Distance + " blocks away");
            float Toward = (float) (Mth.atan2(Aim.z - Origin.z, Aim.x - Origin.x) * Mth.RAD_TO_DEG) - 90.0f;
            Helper.assertTrue(Math.abs(Mth.wrapDegrees(Toward - Facing)) <= Scp4666Sightings.ViewCone + 0.01f, "a sighting was placed out of view");
        }
        FakePlayer User = player(Helper, Helper.absolutePos(new BlockPos(8, 1, 8)));
        double Far = Scp4666Sightings.farLimit(User);
        Helper.assertTrue(Far >= Scp4666Sightings.NearDistance && Far <= Scp4666Sightings.FarDistance, "the far limit left 60 to 90 blocks: " + Far);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 220, batch = "scp4666_sighting_wave")
    public static void scp4666SightingWavesThenDivesAndDespawns(GameTestHelper Helper) {
        floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Spot = Helper.absolutePos(new BlockPos(8, 1, 8));
        FakePlayer User = player(Helper, Spot.south(70));
        User.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(Spot.south(140)));
        Scp4666Sighting Figure = Scp4666Sighting.spawn(World, Vec3.atBottomCenterOf(Spot), User);
        FakePlayer Stranger = FakePlayerFactory.get(World, new GameProfile(UUID.randomUUID(), "yule_other"));
        Helper.assertTrue(Figure.broadcastToPlayer(User), "the witness cannot see the sighting");
        Helper.assertFalse(Figure.broadcastToPlayer(Stranger), "another player can see the sighting");
        Helper.assertFalse(Figure.isPickable(), "the sighting can be hit");
        Helper.assertFalse(Scp4666Registry.Sighting.get().canSerialize(), "the sighting would be saved with the world");
        Helper.startSequence()
                .thenExecuteAfter(Scp4666Sighting.WaveTicks - 10, () -> Helper.assertFalse(Figure.diving(), "the sighting dived before its wave was done"))
                .thenWaitUntil(() -> Helper.assertTrue(Figure.diving(), "the sighting never dived"))
                .thenWaitUntil(() -> Helper.assertTrue(Figure.isRemoved(), "the sighting stayed after its dive"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp4666_sighting_stare")
    public static void scp4666SightingDivesWhenWatched(GameTestHelper Helper) {
        floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Spot = Helper.absolutePos(new BlockPos(8, 1, 8));
        FakePlayer User = player(Helper, Spot.south(70));
        Scp4666Sighting Figure = Scp4666Sighting.spawn(World, Vec3.atBottomCenterOf(Spot), User);
        User.lookAt(EntityAnchorArgument.Anchor.EYES, Figure.position().add(0.0, Figure.getBbHeight() * 0.8, 0.0));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Figure.diving(), "staring at the sighting did not make it dive"))
                .thenExecute(() -> Helper.assertTrue(Figure.age() < Scp4666Sighting.WaveTicks, "the stare did not cut the wave short"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp4666_sighting_near")
    public static void scp4666SightingDivesWhenApproached(GameTestHelper Helper) {
        floor(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Spot = Helper.absolutePos(new BlockPos(8, 1, 8));
        FakePlayer User = player(Helper, Spot.south(5));
        User.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(Spot.south(40)));
        Scp4666Sighting Figure = Scp4666Sighting.spawn(World, Vec3.atBottomCenterOf(Spot), User);
        Helper.startSequence()
                .thenExecuteAfter(2, () -> Helper.assertTrue(Figure.diving(), "a close witness did not make the sighting dive"))
                .thenWaitUntil(() -> Helper.assertTrue(Figure.isRemoved(), "the sighting stayed after diving"))
                .thenSucceed();
    }
}
