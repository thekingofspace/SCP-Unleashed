package net.rai.scp_unleashed.scp001scarlet.king.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKing;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingData;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletRite;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletAnchorBlock;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThrone;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneEmitter;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneRise;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneSite;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneSpill;
import net.rai.scp_unleashed.scp999.Scp999Blob;
import net.rai.scp_unleashed.scp999.Scp999Registry;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ScarletBuildGameTests {
    public static final String Arena = "scarletking_arena";
    public static final String Phrase = "Seven thrones, Seven brides, Seven hearts for the scarlet king";
    public static final int PadY = -54;
    public static final long[] Seeds = {1L, 42L, 0x5CA41E7L, -9L, 123456789L, 77L, 0xDEADBEEFL};

    private ScarletBuildGameTests() {
    }

    static void freshState(MinecraftServer Server) {
        ScarletState Made = new ScarletState();
        Made.setDirty();
        Server.overworld().getDataStorage().set("scp_unleashed_scarlet", Made);
        ScarletThroneSite.get(Server).forget();
        ScarletThroneEmitter.clear();
        ScarletThroneRise.forget();
        ScarletThroneRise.SpawnOverride = -1;
        ScarletRite.forget();
        ScarletKingData Data = ScarletKingData.get(Server);
        Data.endRite();
        Data.king(null);
        Data.consumed(false);
    }

    static void hold(ServerLevel World, BlockPos Centre, boolean Forced) {
        int Reach = ScarletThrone.RimRadius + ScarletThroneSpill.Margin + 2;
        for (int Cx = (Centre.getX() - Reach) >> 4; Cx <= (Centre.getX() + Reach) >> 4; Cx++) {
            for (int Cz = (Centre.getZ() - Reach) >> 4; Cz <= (Centre.getZ() + Reach) >> 4; Cz++) {
                World.setChunkForced(Cx, Cz, Forced);
            }
        }
    }

    static boolean loaded(ServerLevel World, BlockPos Centre) {
        int Reach = ScarletThrone.RimRadius + ScarletThroneSpill.Margin + 2;
        for (int Cx = (Centre.getX() - Reach) >> 4; Cx <= (Centre.getX() + Reach) >> 4; Cx++) {
            for (int Cz = (Centre.getZ() - Reach) >> 4; Cz <= (Centre.getZ() + Reach) >> 4; Cz++) {
                if (!World.areEntitiesLoaded(ChunkPos.asLong(Cx, Cz))) {
                    return false;
                }
            }
        }
        return true;
    }

    static void clean(ServerLevel World, BlockPos Centre) {
        AABB Area = new AABB(Centre).inflate(ScarletThrone.RimRadius + 12.0, ScarletThrone.ClearAbove + 8.0, ScarletThrone.RimRadius + 12.0);
        for (Entity Each : World.getEntitiesOfClass(Entity.class, Area, Each -> !(Each instanceof Player))) {
            if (Each instanceof ScarletKing King) {
                King.clearBar();
            }
            Each.discard();
        }
        hold(World, Centre, false);
        freshState(World.getServer());
    }

    private static BlockPos pad(ServerLevel World, BlockPos Middle) {
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                for (int Y = World.getMinBuildHeight() + 1; Y <= PadY; Y++) {
                    World.setBlock(new BlockPos(Middle.getX() + Dx, Y, Middle.getZ() + Dz), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        return new BlockPos(Middle.getX(), PadY + 1, Middle.getZ());
    }

    static FakePlayer speaker(ServerLevel World, Vec3 At, String Name) {
        FakePlayer Made = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Made.moveTo(At.x, At.y, At.z, 0.0f, 0.0f);
        return Made;
    }

    static Scp999Blob blob(ServerLevel World, BlockPos Feet) {
        Scp999Blob Made = Scp999Registry.Blob.get().spawn(World, Feet, MobSpawnType.COMMAND);
        if (Made != null) {
            Made.setNoAi(true);
            Made.moveTo(Feet.getX() + 0.5, Feet.getY(), Feet.getZ() + 0.5, 0.0f, 0.0f);
        }
        return Made;
    }

    @GameTest(template = Arena, timeoutTicks = 1600, batch = "scarletking_build")
    public static void phraseBuildsTheArenaUnderNinehundredThenStartsTheRite(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        freshState(Server);
        BlockPos Middle = new BlockPos(48016, PadY, 48016);
        hold(World, Middle, true);
        BlockPos Feet = pad(World, Middle);
        ScarletState State = ScarletState.get(Server);
        State.markRead();
        State.phase(ScarletState.Phase.Xk2, World.getGameTime());
        long XkTick = State.phaseTick();
        Scp999Blob Blob = blob(World, Feet);
        Helper.assertTrue(Blob != null, "999 did not spawn");
        FakePlayer User = speaker(World, Vec3.atBottomCenterOf(Feet).add(2.0, 0.0, 0.0), "scarlet_build");
        ScarletRite.heard(User, Phrase);
        int Nonce = ScarletRite.pendingNonce(User.getUUID());
        Helper.assertTrue(Nonce >= 0, "the phrase did not ask for confirmation");
        Helper.assertFalse(State.thronePlaced(), "the Throne appeared before the answer");
        ScarletRite.answer(User, Nonce, true);
        BlockPos Throne = new BlockPos(Feet.getX(), Feet.getY() - ScarletThrone.DaisHeight, Feet.getZ());
        ScarletThroneSite Site = ScarletThroneSite.get(Server);
        Helper.assertTrue(Site.rising(), "confirming did not begin the build");
        Helper.assertTrue(State.thronePlaced() && State.throne().equals(Throne), "the Throne is not centred under 999: " + State.throne().toShortString());
        Helper.assertTrue(State.phase() == ScarletState.Phase.Rite, "the world-end clock was not paused while building");
        UUID BlobId = Blob.getUUID();
        Helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    ScarletKingData Data = ScarletKingData.get(Server);
                    Helper.assertTrue(Site.rising(), "the build finished implausibly fast");
                    Helper.assertTrue(Data.riteStart() < 0L, "the rite began before the arena was built");
                    Helper.assertTrue(World.getEntitiesOfClass(ScarletKing.class, new AABB(Throne).inflate(64.0)).isEmpty(), "the King came before the arena");
                })
                .thenWaitUntil(() -> {
                    Helper.assertFalse(Site.rising(), "the arena is still building, stage " + Site.stage());
                })
                .thenExecute(() -> {
                    ScarletKingData Data = ScarletKingData.get(Server);
                    Helper.assertTrue(Data.riteStart() >= 0L, "the rite did not begin after the build");
                    Helper.assertTrue(Data.riteThrone().equals(Throne), "the rite began somewhere else");
                    Helper.assertTrue(Data.xkTick() == XkTick, "the rite lost the XK-2 clock");
                    Helper.assertTrue(BlobId.equals(Data.blob()), "the rite did not take the offered 999");
                    Helper.assertTrue(World.getBlockState(Throne.above()).is(net.rai.scp_unleashed.scp001scarlet.Scp001ScarletRegistry.GlyphStone.get()), "the dais is not under 999");
                    Helper.assertTrue(!World.getBlockState(Throne.below()).isAir(), "the arena has no floor");
                    long Seed = Site.seed();
                    for (int Index = 0; Index < ScarletThrone.Pillars; Index++) {
                        int[] Offset = ScarletThrone.pillarOffset(Index);
                        Helper.assertFalse(World.getBlockState(Throne.offset(Offset[0], ScarletThrone.PillarHeight / 2, Offset[1])).isAir(), "pillar " + Index + " did not rise");
                        ScarletThrone.Anchor Mount = ScarletThrone.anchor(Index, Seed);
                        BlockState Ring = World.getBlockState(Throne.offset(Mount.Arm()));
                        Helper.assertTrue(Ring.getBlock() instanceof ScarletAnchorBlock, "pillar " + Index + " has no chain anchor");
                        Helper.assertTrue(World.getBlockState(Throne.offset(Mount.Arm()).below()).is(Blocks.CHAIN), "pillar " + Index + " has no chain under its anchor");
                        Helper.assertFalse(World.getBlockState(Throne.offset(Mount.Bracket())).isAir(), "pillar " + Index + " anchor has no bracket");
                    }
                    Entity Held = World.getEntity(BlobId);
                    Helper.assertTrue(Held instanceof Scp999Blob, "999 vanished during the build");
                    clean(World, Throne);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scarletking_refuse")
    public static void phraseRefusesWithoutNinehundredOutsideXk2AndAtBadSpots(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        freshState(Server);
        BlockPos Middle = new BlockPos(-48016, PadY, 48016);
        hold(World, Middle, true);
        BlockPos Feet = pad(World, Middle);
        ScarletState State = ScarletState.get(Server);
        State.markRead();
        State.phase(ScarletState.Phase.Xk2, World.getGameTime());
        Vec3 Near = Vec3.atBottomCenterOf(Feet).add(2.0, 0.0, 0.0);
        FakePlayer User = speaker(World, Near, "scarlet_refuse");
        Helper.assertTrue(ScarletRite.outcome(World, Near, ScarletState.Phase.Xk2, false, BlockPos.ZERO) == ScarletRite.Outcome.Unbound, "the phrase answered without 999");
        ScarletRite.heard(User, Phrase);
        Helper.assertTrue(ScarletRite.pendingNonce(User.getUUID()) < 0, "the phrase asked to confirm without 999");
        Scp999Blob Blob = blob(World, Feet);
        Helper.assertTrue(Blob != null, "999 did not spawn");
        Helper.assertTrue(ScarletThroneRise.survey(World, Blob.position()).fine(), "the test pad should be a fine spot: " + ScarletThroneRise.survey(World, Blob.position()).Reason());
        Helper.assertTrue(ScarletRite.outcome(World, Near, ScarletState.Phase.Stirring, false, BlockPos.ZERO) == ScarletRite.Outcome.Stirring, "the phrase answered before XK-2");
        Helper.assertTrue(ScarletRite.outcome(World, Near, ScarletState.Phase.Asleep, false, BlockPos.ZERO) == ScarletRite.Outcome.Silent, "the phrase answered while asleep");
        State.phase(ScarletState.Phase.Stirring, World.getGameTime());
        ScarletRite.heard(User, Phrase);
        Helper.assertTrue(ScarletRite.pendingNonce(User.getUUID()) < 0, "the phrase asked to confirm before XK-2");
        State.phase(ScarletState.Phase.Xk2, World.getGameTime());
        ScarletThroneRise.SpawnOverride = 1000000;
        Helper.assertTrue(ScarletThroneRise.survey(World, Blob.position()).Reason() == ScarletThroneRise.Refusal.Spawn, "spawn protection did not refuse");
        Helper.assertTrue(ScarletRite.outcome(World, Near, ScarletState.Phase.Xk2, false, BlockPos.ZERO) == ScarletRite.Outcome.Unsuitable, "the rite was ready inside spawn protection");
        ScarletRite.heard(User, Phrase);
        Helper.assertTrue(ScarletRite.pendingNonce(User.getUUID()) < 0, "the phrase asked to confirm inside spawn protection");
        ScarletThroneRise.SpawnOverride = 0;
        World.setBlock(Feet, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        Helper.assertTrue(ScarletThroneRise.survey(World, Vec3.atBottomCenterOf(Feet)).Reason() == ScarletThroneRise.Refusal.Liquid, "standing in water did not refuse");
        World.setBlock(Feet, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockState Foreign = null;
        for (Block Each : BuiltInRegistries.BLOCK) {
            BlockState Candidate = Each.defaultBlockState();
            if (ScarletThroneRise.foreign(Candidate) && !Candidate.hasBlockEntity() && Candidate.isCollisionShapeFullBlock(World, Feet)) {
                Foreign = Candidate;
                break;
            }
        }
        Helper.assertTrue(Foreign != null, "no other mod block was found to claim the ground");
        BlockPos Claim = Feet.offset(20, 3, 0);
        World.setBlock(Claim, Foreign, Block.UPDATE_CLIENTS);
        Helper.assertTrue(ScarletThroneRise.survey(World, Blob.position()).Reason() == ScarletThroneRise.Refusal.Claimed, "another feature's block did not claim the ground");
        World.setBlock(Claim, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockPos Low = new BlockPos(Middle.getX() + 30, World.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, Middle.getX() + 30, Middle.getZ()), Middle.getZ());
        Helper.assertTrue(ScarletThroneRise.survey(World, Vec3.atBottomCenterOf(Low)).Reason() == ScarletThroneRise.Refusal.Limit, "the floor of the world did not refuse, got "
                + ScarletThroneRise.survey(World, Vec3.atBottomCenterOf(Low)).Reason());
        Helper.assertFalse(State.thronePlaced(), "a refusal placed a Throne");
        Helper.assertFalse(ScarletThroneSite.get(Server).rising(), "a refusal began a build");
        clean(World, Feet);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 2400, batch = "scarletking_spill")
    public static void containersInTheFootprintDropTheirItems(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        freshState(Server);
        BlockPos Throne = new BlockPos(48016, -59, -48016);
        BlockPos Box = Throne.offset(18, 0, 3);
        hold(World, Throne, true);
        int[] Before = new int[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(loaded(World, Throne), "the footprint's entities are still loading"))
                .thenExecute(() -> {
                    for (ItemEntity Stale : World.getEntitiesOfClass(ItemEntity.class, new AABB(Throne).inflate(ScarletThrone.RimRadius + 12.0, 64.0, ScarletThrone.RimRadius + 12.0))) {
                        Stale.discard();
                    }
                    World.setBlock(Box, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
                    Helper.assertTrue(World.getBlockEntity(Box) instanceof ChestBlockEntity, "the chest did not place");
                    ChestBlockEntity Chest = (ChestBlockEntity) World.getBlockEntity(Box);
                    Chest.setItem(0, new ItemStack(Items.DIAMOND, 7));
                    Chest.setItem(5, new ItemStack(Items.GOLDEN_APPLE, 2));
                    Before[0] = ScarletThroneSpill.spilled();
                    ScarletThroneSite.placeAt(Server, Throne);
                    ScarletThroneEmitter.emitAll(World, ScarletThroneSite.get(Server));
                    Helper.assertFalse(World.getBlockState(Box).is(Blocks.CHEST), "the chest survived the build");
                    Helper.assertTrue(ScarletThroneSpill.spilled() >= Before[0] + 3, "the chest spilled " + (ScarletThroneSpill.spilled() - Before[0]) + " stacks");
                })
                .thenExecuteAfter(10, () -> {
                    BlockPos Spot = ScarletThroneSpill.spot(World, Throne, Box);
                    List<ItemEntity> Drops = World.getEntitiesOfClass(ItemEntity.class, new AABB(Spot).inflate(3.0, 4.0, 3.0));
                    int Diamonds = Drops.stream().filter(Each -> Each.getItem().is(Items.DIAMOND)).mapToInt(Each -> Each.getItem().getCount()).sum();
                    Helper.assertTrue(Diamonds == 7, "the chest's diamonds were lost, found " + Diamonds + " near " + Spot.toShortString());
                    Helper.assertTrue(Drops.stream().anyMatch(Each -> Each.getItem().is(Items.GOLDEN_APPLE)), "the chest's apples were lost");
                    Helper.assertTrue(Drops.stream().anyMatch(Each -> Each.getItem().is(Items.CHEST)), "the chest itself was not dropped");
                    double Distance = Math.sqrt(Spot.distSqr(new BlockPos(Throne.getX(), Spot.getY(), Throne.getZ())));
                    Helper.assertTrue(Distance > ScarletThrone.RimRadius, "the drops landed inside the arena");
                    clean(World, Throne);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scarlet_bracket")
    public static void pillarBracketsHoldTheirChains(GameTestHelper Helper) {
        for (long Seed : Seeds) {
            ScarletThrone.Plan Made = ScarletThrone.plan(Seed);
            for (int Index = 0; Index < ScarletThrone.Pillars; Index++) {
                ScarletThrone.Anchor Mount = ScarletThrone.anchor(Index, Seed);
                BlockPos Bracket = Mount.Bracket();
                BlockPos Arm = Mount.Arm();
                String Where = "seed " + Seed + " pillar " + Index;
                int Top = ScarletThrone.columnTop(Index, Bracket.getX(), Bracket.getZ(), Seed);
                Helper.assertTrue(Top >= 0, Where + ": the bracket is not on the pillar");
                Helper.assertTrue(Bracket.getY() <= Top - 2, Where + ": the bracket sits too close to the broken top");
                Helper.assertTrue(ScarletThrone.columnTop(Index, Arm.getX(), Arm.getZ(), Seed) < 0, Where + ": the arm is inside the pillar");
                Helper.assertTrue(Arm.distManhattan(Bracket) == 1 && Arm.getY() == Bracket.getY(), Where + ": the arm is not face-adjacent to the bracket");
                int[] Centre = ScarletThrone.pillarOffset(Index);
                Helper.assertTrue(Mount.Facing().getStepX() * -Centre[0] + Mount.Facing().getStepZ() * -Centre[1] > 0, Where + ": the anchor does not face the dais");
                BlockState AtArm = null;
                BlockState Under = null;
                BlockState AtBracket = null;
                int Lowest = Integer.MAX_VALUE;
                for (ScarletThrone.Piece Each : Made.All()) {
                    if (Each.X() == Arm.getX() && Each.Z() == Arm.getZ()) {
                        if (Each.Y() == Arm.getY()) {
                            AtArm = Each.State();
                        }
                        if (Each.Y() == Arm.getY() - 1) {
                            Under = Each.State();
                        }
                        if (Each.State().is(Blocks.CHAIN)) {
                            Lowest = Math.min(Lowest, Each.Y());
                            Helper.assertTrue(Each.Y() < Arm.getY(), Where + ": a chain hangs above its anchor");
                        }
                    }
                    if (Each.X() == Bracket.getX() && Each.Y() == Bracket.getY() && Each.Z() == Bracket.getZ()) {
                        AtBracket = Each.State();
                    }
                }
                Helper.assertTrue(AtArm != null && AtArm.getBlock() instanceof ScarletAnchorBlock, Where + ": no anchor on the arm");
                Helper.assertTrue(Under != null && Under.is(Blocks.CHAIN), Where + ": the top chain is not directly under the anchor");
                Helper.assertTrue(AtBracket != null && !AtBracket.isAir(), Where + ": the bracket is not a pillar block");
                Helper.assertTrue(Lowest >= ScarletThrone.ChainClearance - 1, Where + ": the chain hangs too low, to " + Lowest);
            }
        }
        Helper.succeed();
    }
}
