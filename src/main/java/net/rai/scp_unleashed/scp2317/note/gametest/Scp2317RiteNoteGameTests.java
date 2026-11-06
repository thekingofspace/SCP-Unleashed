package net.rai.scp_unleashed.scp2317.note.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletPhrase;
import net.rai.scp_unleashed.scp2317.Scp2317KingBlockEntity;
import net.rai.scp_unleashed.scp2317.Scp2317Registry;
import net.rai.scp_unleashed.scp2317.note.Scp2317RiteNote;
import net.rai.scp_unleashed.scp2317.note.Scp2317RiteNoteRegistry;
import net.rai.scp_unleashed.scp2317.note.Scp2317RiteNoteState;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp2317RiteNoteGameTests {
    public static final String Arena = GatewayGameTests.Arena;
    public static final String StateName = "scp_unleashed_scarlet";
    public static final BlockPos KingSpot = new BlockPos(6, 2, 12);
    public static final int Reach = 9;

    private Scp2317RiteNoteGameTests() {
    }

    private static ScarletState swap(GameTestHelper Helper, ScarletState.Phase Phase, float Manifestation) {
        MinecraftServer Server = Helper.getLevel().getServer();
        ScarletState Before = ScarletState.get(Server);
        ScarletState Fresh = new ScarletState();
        Fresh.markRead();
        Fresh.phase(Phase, Server.overworld().getGameTime());
        Fresh.manifestation(Manifestation);
        Server.overworld().getDataStorage().set(StateName, Fresh);
        GatewayGameTests.whenDone(Helper, () -> Server.overworld().getDataStorage().set(StateName, Before));
        return Fresh;
    }

    private static Scp2317KingBlockEntity king(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(KingSpot);
        World.setBlock(Pos, Scp2317Registry.King.get().defaultBlockState(), Block.UPDATE_ALL);
        Helper.assertTrue(World.getBlockEntity(Pos) instanceof Scp2317KingBlockEntity, "the sleeper has no block entity");
        GatewayGameTests.whenDone(Helper, () -> {
            for (BlockPos Found : notes(World, Pos)) {
                World.setBlock(Found, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            Scp2317RiteNoteState.get(World).forget(Pos);
        });
        return (Scp2317KingBlockEntity) World.getBlockEntity(Pos);
    }

    private static List<BlockPos> notes(ServerLevel World, BlockPos King) {
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos At : BlockPos.betweenClosed(King.offset(-Reach, -2, -Reach), King.offset(Reach, 2, Reach))) {
            if (World.getBlockState(At).is(Scp2317RiteNoteRegistry.FloorNote.get())) {
                Found.add(At.immutable());
            }
        }
        return Found;
    }

    private static int copies(Player User) {
        int Count = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            ItemStack Stack = User.getInventory().getItem(Slot);
            if (Stack.is(Scp2317RiteNoteRegistry.Note.get())) {
                Count += Stack.getCount();
            }
        }
        return Count;
    }

    private static InteractionResult press(ServerLevel World, BlockPos Pos, Player User) {
        BlockState State = World.getBlockState(Pos);
        return State.useWithoutItem(World, User, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false));
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "sritenote_phrase")
    public static void phraseOnTheNoteSpeaksTheRite(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            Language Words = Language.getInstance();
            for (String Key : Scp2317RiteNote.Body) {
                Helper.assertTrue(Words.has(Key), "the note is missing the line " + Key);
            }
            for (String Key : List.of(Scp2317RiteNote.SignKey, Scp2317RiteNote.LabelKey, "item.scp_unleashed.scp001_scarlet_rite_note", "block.scp_unleashed.scp2317_rite_note")) {
                Helper.assertTrue(Words.has(Key), "the note is missing " + Key);
            }
            String Phrase = Words.getOrDefault(Scp2317RiteNote.WordsKey);
            Helper.assertTrue(ScarletPhrase.matches(Phrase), "the words on the note do not speak the rite: " + Phrase);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "sritenote_floor")
    public static void noteWaitsForTheLastChainAndAppearsOnce(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            swap(Helper, ScarletState.Phase.Stirring, 0.0f);
            ServerLevel World = Helper.getLevel();
            Scp2317KingBlockEntity King = king(Helper);
            BlockPos Pos = King.getBlockPos();
            for (int Broken = 0; Broken < ScarletState.Chains; Broken++) {
                King.sync(Broken);
                Scp2317RiteNote.tick(World, King);
                Scp2317RiteNote.tick(World, King);
                Helper.assertTrue(notes(World, Pos).isEmpty(), "a note appeared with only " + Broken + " chains broken");
                Helper.assertTrue(!Scp2317RiteNoteState.get(World).placed(Pos), "the note was marked placed at " + Broken + " chains");
            }
            King.sync(ScarletState.Chains);
            for (int Tick = 0; Tick < 6; Tick++) {
                Scp2317RiteNote.tick(World, King);
            }
            List<BlockPos> Found = notes(World, Pos);
            Helper.assertTrue(Found.size() == 1, "expected one note at seven chains, found " + Found.size());
            Helper.assertTrue(Found.get(0).equals(Pos.offset(Scp2317RiteNote.Spots[0][0], 0, Scp2317RiteNote.Spots[0][1])), "the note is not at the King's feet: " + Found.get(0));
            CompoundTag Saved = Scp2317RiteNoteState.get(World).save(new CompoundTag(), World.registryAccess());
            boolean Stored = false;
            for (long Each : Saved.getLongArray("Placed")) {
                Stored |= Each == Pos.asLong();
            }
            Helper.assertTrue(Stored, "the placed note is not saved");
            World.setBlock(Found.get(0), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            for (int Tick = 0; Tick < 4; Tick++) {
                Scp2317RiteNote.tick(World, King);
            }
            Helper.assertTrue(notes(World, Pos).isEmpty(), "a removed note came back");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "sritenote_existing")
    public static void worldAlreadyInXk2GetsItsNote(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            swap(Helper, ScarletState.Phase.Xk2, 1.0f);
            Scp2317KingBlockEntity King = king(Helper);
            BlockPos Pos = King.getBlockPos();
            ServerLevel World = Helper.getLevel();
            Helper.assertTrue(King.broken() == 0 && notes(World, Pos).isEmpty(), "the sleeper started with a note");
            Helper.succeedWhen(() -> {
                Helper.assertTrue(King.broken() == ScarletState.Chains, "the sleeper has not caught up with the chains");
                Helper.assertTrue(notes(World, Pos).size() == 1, "the sleeper's tick did not leave a note");
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "sritenote_take")
    public static void takingGivesOneCopyOnly(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ServerLevel World = Helper.getLevel();
            BlockPos Pos = Helper.absolutePos(new BlockPos(4, 2, 4));
            World.setBlock(Pos, Scp2317RiteNoteRegistry.FloorNote.get().defaultBlockState(), Block.UPDATE_ALL);
            GatewayGameTests.whenDone(Helper, () -> World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
            FakePlayer First = GatewayGameTests.person(Helper, 4.5, 6.5, "sritenote_first");
            FakePlayer Second = GatewayGameTests.person(Helper, 5.5, 6.5, "sritenote_second");
            First.getInventory().clearContent();
            Second.getInventory().clearContent();
            Helper.assertTrue(press(World, Pos, First).consumesAction(), "the note did not answer a right click");
            Helper.assertTrue(copies(First) == 1, "the first reader got " + copies(First) + " copies");
            press(World, Pos, First);
            press(World, Pos, First);
            Helper.assertTrue(copies(First) == 1, "a reader who holds a copy got another: " + copies(First));
            Helper.assertTrue(World.getBlockState(Pos).is(Scp2317RiteNoteRegistry.FloorNote.get()), "the floor note was taken away");
            press(World, Pos, Second);
            Helper.assertTrue(copies(Second) == 1, "the second reader got " + copies(Second) + " copies");
            Helper.assertTrue(!Scp2317RiteNote.take(First, Pos), "take handed out a second copy");
            Helper.succeed();
        });
    }
}
