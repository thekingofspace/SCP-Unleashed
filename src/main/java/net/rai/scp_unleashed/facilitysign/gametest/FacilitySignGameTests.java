package net.rai.scp_unleashed.facilitysign.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.facilitysign.FacilityHeaderBlock;
import net.rai.scp_unleashed.facilitysign.FacilityHeaderPart;
import net.rai.scp_unleashed.facilitysign.FacilityPlaqueBlock;
import net.rai.scp_unleashed.facilitysign.FacilitySignArrow;
import net.rai.scp_unleashed.facilitysign.FacilitySignBlock;
import net.rai.scp_unleashed.facilitysign.FacilitySignDesign;
import net.rai.scp_unleashed.facilitysign.FacilitySignMount;
import net.rai.scp_unleashed.facilitysign.FacilitySignPart;
import net.rai.scp_unleashed.facilitysign.FacilitySignRegistry;
import net.rai.scp_unleashed.facilitysign.FacilitySignSupport;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class FacilitySignGameTests {
    public static final String Arena = "facility_sign_arena";
    private static final int WallZ = 4;

    private FacilitySignGameTests() {
    }

    private static void buildWall(GameTestHelper Helper) {
        for (int X = 0; X < 12; X++) {
            for (int Y = 2; Y <= 3; Y++) {
                Helper.setBlock(new BlockPos(X, Y, WallZ), Blocks.STONE.defaultBlockState());
            }
        }
    }

    private static FacilitySignTestPlayer player(GameTestHelper Helper, Vec3 Relative, float Yaw, String Name) {
        ServerLevel World = Helper.getLevel();
        FacilitySignTestPlayer User = new FacilitySignTestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(Relative);
        User.moveTo(Where.x, Where.y, Where.z, Yaw, 0.0f);
        User.setYHeadRot(Yaw);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, FacilitySignTestPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static InteractionResult click(GameTestHelper Helper, FacilitySignTestPlayer User, BlockPos Relative, Direction Face, double Offset) {
        BlockPos Absolute = Helper.absolutePos(Relative);
        Vec3 Centre = Vec3.atCenterOf(Absolute);
        Vec3 Location = Centre.add(Face.getStepX() * 0.5 + (Face.getAxis() == Direction.Axis.Z ? Offset : 0.0), Face.getStepY() * 0.5, Face.getStepZ() * 0.5 + (Face.getAxis() == Direction.Axis.X ? Offset : 0.0));
        return User.gameMode.useItemOn(User, Helper.getLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Location, Face, Absolute, false));
    }

    private static BlockPos local(GameTestHelper Helper, BlockPos Absolute) {
        return Absolute.subtract(Helper.absolutePos(BlockPos.ZERO));
    }

    private static String row(GameTestHelper Helper, int Y) {
        StringBuilder Text = new StringBuilder();
        for (int X = 0; X < 12; X++) {
            BlockState State = at(Helper, new BlockPos(X, Y, WallZ - 1));
            Text.append(X).append('=').append(State.isAir() ? "air" : State.toString().replace("Block{scp_unleashed:", "").replace("Block{minecraft:", "")).append(' ');
        }
        return Text.toString();
    }

    private static BlockState at(GameTestHelper Helper, BlockPos Relative) {
        return Helper.getBlockState(Relative);
    }

    private static int dropped(GameTestHelper Helper, Item Wanted) {
        int Total = 0;
        for (ItemEntity Drop : Helper.getEntities(EntityType.ITEM)) {
            if (Drop.getItem().is(Wanted)) {
                Total += Drop.getItem().getCount();
            }
        }
        return Total;
    }

    private static BlockPos placeSign(GameTestHelper Helper, FacilitySignDesign Design, BlockPos Relative, Direction Facing, FacilitySignMount Hung) {
        BlockState Left = FacilitySignRegistry.sign(Design).defaultBlockState().setValue(FacilitySignBlock.FACING, Facing)
                .setValue(FacilitySignBlock.Mount, Hung).setValue(FacilitySignBlock.Part, FacilitySignPart.Left);
        BlockPos Other = Relative.relative(FacilitySignSupport.rightOf(Facing));
        Helper.setBlock(Relative, Left);
        Helper.setBlock(Other, Left.setValue(FacilitySignBlock.Part, FacilitySignPart.Right));
        return Other;
    }

    private static List<BlockPos> placeHeader(GameTestHelper Helper, FacilitySignDesign Design, BlockPos Centre, Direction Facing) {
        BlockState Middle = FacilitySignRegistry.header(Design).defaultBlockState().setValue(FacilityHeaderBlock.FACING, Facing)
                .setValue(FacilityHeaderBlock.Part, FacilityHeaderPart.Centre).setValue(FacilityHeaderBlock.Mount, FacilitySignMount.Wall);
        Direction Right = FacilitySignSupport.rightOf(Facing);
        Helper.setBlock(Centre, Middle);
        Helper.setBlock(Centre.relative(Right.getOpposite()), Middle.setValue(FacilityHeaderBlock.Part, FacilityHeaderPart.Left));
        Helper.setBlock(Centre.relative(Right), Middle.setValue(FacilityHeaderBlock.Part, FacilityHeaderPart.Right));
        return List.of(Centre.relative(Right.getOpposite()), Centre, Centre.relative(Right));
    }

    private static int countSigns(GameTestHelper Helper, BlockState Kind, int Y, int Z) {
        int Found = 0;
        for (int X = 0; X < 12; X++) {
            if (at(Helper, new BlockPos(X, Y, Z)).is(Kind.getBlock())) {
                Found++;
            }
        }
        return Found;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "facility_sign_place")
    public static void largeSignFillsTwoCellsOrRefuses(GameTestHelper Helper) {
        buildWall(Helper);
        FacilitySignBlock Sign = FacilitySignRegistry.sign(FacilitySignDesign.GateA);
        FacilitySignTestPlayer User = player(Helper, new Vec3(5.5, 2.0, 1.5), 0.0f, "sign_placer");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FacilitySignRegistry.SignItems.get(FacilitySignDesign.GateA).get(), 3));
        click(Helper, User, new BlockPos(5, 3, WallZ), Direction.NORTH, 0.2);
        BlockPos Cell = new BlockPos(5, 3, WallZ - 1);
        BlockState Main = at(Helper, Cell);
        Helper.assertTrue(Main.is(Sign), "clicking the wall did not place a Gate A sign, found " + Main);
        Helper.assertTrue(Main.getValue(FacilitySignBlock.FACING) == Direction.NORTH && Main.getValue(FacilitySignBlock.Mount) == FacilitySignMount.Wall
                && Main.getValue(FacilitySignBlock.Arrow) == FacilitySignArrow.Right, "the placed sign has the wrong facing, mount or arrow: " + Main);
        BlockPos Partner = local(Helper, FacilitySignBlock.partnerPos(Main, Helper.absolutePos(Cell)));
        BlockState Second = at(Helper, Partner);
        Helper.assertTrue(Second.is(Sign) && Second.getValue(FacilitySignBlock.Part) != Main.getValue(FacilitySignBlock.Part)
                && Second.getValue(FacilitySignBlock.FACING) == Direction.NORTH, "the second cell at " + Partner + " was not filled: " + Second);
        Helper.assertTrue(Partner.getY() == 3 && Partner.getZ() == WallZ - 1 && Math.abs(Partner.getX() - 5) == 1, "the second half is not beside the first: " + Partner);
        Helper.assertTrue(countSigns(Helper, Main, 3, WallZ - 1) == 2, "a large sign should fill exactly two cells");
        Helper.assertTrue(User.getMainHandItem().getCount() == 2, "placing should use one sign");
        Helper.setBlock(new BlockPos(7, 2, WallZ - 1), Blocks.GLASS.defaultBlockState());
        Helper.setBlock(new BlockPos(9, 2, WallZ - 1), Blocks.GLASS.defaultBlockState());
        click(Helper, User, new BlockPos(8, 2, WallZ), Direction.NORTH, 0.0);
        Helper.assertTrue(at(Helper, new BlockPos(8, 2, WallZ - 1)).isAir(), "a sign was placed although both neighbours were blocked");
        Helper.assertTrue(User.getMainHandItem().getCount() == 2, "a refused placement used up a sign");
        Helper.setBlock(new BlockPos(2, 2, WallZ - 1), Blocks.GLASS.defaultBlockState());
        click(Helper, User, new BlockPos(3, 2, WallZ), Direction.NORTH, -0.3);
        Helper.assertTrue(at(Helper, new BlockPos(3, 2, WallZ - 1)).is(Sign) && at(Helper, new BlockPos(4, 2, WallZ - 1)).is(Sign),
                "with one side blocked the sign should take the free side");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "facility_sign_break")
    public static void breakingEitherHalfRemovesBothAndDropsOne(GameTestHelper Helper) {
        buildWall(Helper);
        Item SignItem = FacilitySignRegistry.SignItems.get(FacilitySignDesign.RestroomMen).get();
        FacilitySignTestPlayer User = player(Helper, new Vec3(5.5, 2.0, 1.5), 0.0f, "sign_breaker");
        BlockPos LeftFirst = new BlockPos(2, 2, WallZ - 1);
        BlockPos RightFirst = placeSign(Helper, FacilitySignDesign.RestroomMen, LeftFirst, Direction.NORTH, FacilitySignMount.Wall);
        User.gameMode.destroyBlock(Helper.absolutePos(LeftFirst));
        Helper.assertTrue(at(Helper, LeftFirst).isAir() && at(Helper, RightFirst).isAir(), "breaking the left half left a piece behind");
        Helper.assertTrue(dropped(Helper, SignItem) == 1, "breaking the left half dropped " + dropped(Helper, SignItem) + " signs");
        BlockPos LeftSecond = new BlockPos(7, 2, WallZ - 1);
        BlockPos RightSecond = placeSign(Helper, FacilitySignDesign.RestroomMen, LeftSecond, Direction.NORTH, FacilitySignMount.Wall);
        User.gameMode.destroyBlock(Helper.absolutePos(RightSecond));
        Helper.assertTrue(at(Helper, LeftSecond).isAir() && at(Helper, RightSecond).isAir(), "breaking the right half left a piece behind");
        Helper.assertTrue(dropped(Helper, SignItem) == 2, "breaking the right half should drop exactly one more sign, total " + dropped(Helper, SignItem));
        BlockPos LeftThird = new BlockPos(2, 3, WallZ - 1);
        BlockPos RightThird = placeSign(Helper, FacilitySignDesign.RestroomMen, LeftThird, Direction.NORTH, FacilitySignMount.Wall);
        User.setGameMode(GameType.CREATIVE);
        User.gameMode.destroyBlock(Helper.absolutePos(RightThird));
        Helper.assertTrue(at(Helper, LeftThird).isAir() && at(Helper, RightThird).isAir(), "a creative break left a piece behind");
        Helper.assertTrue(dropped(Helper, SignItem) == 2, "a creative break dropped a sign");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "facility_sign_arrow")
    public static void arrowCyclesWithAnEmptyHand(GameTestHelper Helper) {
        buildWall(Helper);
        BlockPos Left = new BlockPos(5, 3, WallZ - 1);
        BlockPos Right = placeSign(Helper, FacilitySignDesign.GateB, Left, Direction.NORTH, FacilitySignMount.Wall);
        FacilitySignTestPlayer User = player(Helper, new Vec3(5.5, 2.0, 1.5), 0.0f, "arrow_turner");
        FacilitySignArrow[] Expected = {FacilitySignArrow.Down, FacilitySignArrow.Left, FacilitySignArrow.Up, FacilitySignArrow.None, FacilitySignArrow.Right};
        Helper.assertTrue(at(Helper, Left).getValue(FacilitySignBlock.Arrow) == FacilitySignArrow.Right, "a new sign should point right");
        for (int Step = 0; Step < Expected.length; Step++) {
            click(Helper, User, Step % 2 == 0 ? Left : Right, Direction.NORTH, 0.0);
            FacilitySignArrow Now = at(Helper, Left).getValue(FacilitySignBlock.Arrow);
            Helper.assertTrue(Now == Expected[Step], "step " + Step + ": expected " + Expected[Step] + " but the arrow is " + Now);
            Helper.assertTrue(at(Helper, Right).getValue(FacilitySignBlock.Arrow) == Now, "the two halves disagree on the arrow at step " + Step);
        }
        User.setShiftKeyDown(true);
        click(Helper, User, Left, Direction.NORTH, 0.0);
        Helper.assertTrue(at(Helper, Left).getValue(FacilitySignBlock.Arrow) == FacilitySignArrow.None, "sneaking should turn the arrow back");
        User.setShiftKeyDown(false);
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        click(Helper, User, Left, Direction.NORTH, 0.0);
        Helper.assertTrue(at(Helper, Left).getValue(FacilitySignBlock.Arrow) == FacilitySignArrow.None, "using the sign with an item in hand turned the arrow");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "facility_sign_support")
    public static void wallAndCeilingSignsNeedTheirSupport(GameTestHelper Helper) {
        buildWall(Helper);
        for (int X = 1; X <= 10; X++) {
            Helper.setBlock(new BlockPos(X, 4, 1), Blocks.STONE.defaultBlockState());
        }
        FacilitySignBlock Sign = FacilitySignRegistry.sign(FacilitySignDesign.Stairs);
        Item SignItem = FacilitySignRegistry.SignItems.get(FacilitySignDesign.Stairs).get();
        Item PlaqueItem = FacilitySignRegistry.PlaqueItems.get(FacilitySignDesign.Stairs).get();
        FacilitySignTestPlayer Facing = player(Helper, new Vec3(5.5, 2.0, 2.5), 0.0f, "wall_hanger");
        Facing.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SignItem, 4));
        click(Helper, Facing, new BlockPos(5, 3, WallZ), Direction.NORTH, 0.2);
        BlockPos WallCell = new BlockPos(5, 3, WallZ - 1);
        Helper.assertTrue(at(Helper, WallCell).is(Sign), "the wall sign was not placed");
        BlockPos WallPartner = local(Helper, FacilitySignBlock.partnerPos(at(Helper, WallCell), Helper.absolutePos(WallCell)));
        FacilitySignTestPlayer Below = player(Helper, new Vec3(6.5, 2.0, 3.5), 180.0f, "ceiling_hanger");
        Below.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SignItem, 4));
        click(Helper, Below, new BlockPos(6, 4, 1), Direction.DOWN, 0.0);
        BlockPos HungCell = new BlockPos(6, 3, 1);
        BlockState Hung = at(Helper, HungCell);
        Helper.assertTrue(Hung.is(Sign) && Hung.getValue(FacilitySignBlock.Mount) == FacilitySignMount.Ceiling, "clicking the ceiling did not hang a sign: " + Hung);
        Helper.assertTrue(Hung.getValue(FacilitySignBlock.FACING) == Direction.SOUTH, "the hung sign should face the player: " + Hung.getValue(FacilitySignBlock.FACING));
        BlockPos HungPartner = local(Helper, FacilitySignBlock.partnerPos(Hung, Helper.absolutePos(HungCell)));
        Helper.assertTrue(at(Helper, HungPartner).is(Sign) && at(Helper, HungPartner).getValue(FacilitySignBlock.Mount) == FacilitySignMount.Ceiling, "the hung sign has no second half");
        Facing.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PlaqueItem, 2));
        click(Helper, Facing, new BlockPos(9, 2, WallZ), Direction.NORTH, 0.0);
        BlockPos PlaqueCell = new BlockPos(9, 2, WallZ - 1);
        Helper.assertTrue(at(Helper, PlaqueCell).getBlock() instanceof FacilityPlaqueBlock, "the plaque was not placed");
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(at(Helper, WallCell).is(Sign) && at(Helper, WallPartner).is(Sign), "the wall sign did not survive on its wall");
            Helper.assertTrue(at(Helper, HungCell).is(Sign) && at(Helper, HungPartner).is(Sign), "the hung sign did not survive under its ceiling");
            Helper.assertTrue(at(Helper, PlaqueCell).getBlock() instanceof FacilityPlaqueBlock, "the plaque did not survive on its wall");
            Helper.setBlock(WallPartner.relative(Direction.SOUTH), Blocks.AIR.defaultBlockState());
            Helper.assertTrue(at(Helper, WallCell).isAir() && at(Helper, WallPartner).isAir(), "the wall sign stayed after its wall was removed");
            Helper.assertTrue(dropped(Helper, SignItem) == 1, "the wall sign should drop once, dropped " + dropped(Helper, SignItem));
            Helper.setBlock(HungCell.above(), Blocks.AIR.defaultBlockState());
            Helper.assertTrue(at(Helper, HungCell).isAir() && at(Helper, HungPartner).isAir(), "the hung sign stayed after its ceiling was removed");
            Helper.assertTrue(dropped(Helper, SignItem) == 2, "the hung sign should drop once, total " + dropped(Helper, SignItem));
            Helper.setBlock(PlaqueCell.relative(Direction.SOUTH), Blocks.AIR.defaultBlockState());
            Helper.assertTrue(at(Helper, PlaqueCell).isAir(), "the plaque stayed after its wall was removed");
            Helper.assertTrue(dropped(Helper, PlaqueItem) == 1, "the plaque should drop once");
            dismiss(Helper, Facing);
            dismiss(Helper, Below);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "facility_sign_glow")
    public static void exitSignEmitsLight(GameTestHelper Helper) {
        buildWall(Helper);
        BlockPos Left = new BlockPos(3, 3, WallZ - 1);
        placeSign(Helper, FacilitySignDesign.EmergencyExit, Left, Direction.NORTH, FacilitySignMount.Wall);
        BlockPos Plaque = new BlockPos(8, 2, WallZ - 1);
        Helper.setBlock(Plaque, FacilitySignRegistry.plaque(FacilitySignDesign.EmergencyExit).defaultBlockState().setValue(FacilityPlaqueBlock.FACING, Direction.NORTH));
        BlockPos Plain = new BlockPos(10, 2, WallZ - 1);
        Helper.setBlock(Plain, FacilitySignRegistry.plaque(FacilitySignDesign.GateC).defaultBlockState().setValue(FacilityPlaqueBlock.FACING, Direction.NORTH));
        Helper.assertTrue(at(Helper, Left).getLightEmission() == FacilitySignRegistry.GlowLight, "the exit sign does not emit light");
        Helper.assertTrue(at(Helper, Plaque).getLightEmission() == FacilitySignRegistry.GlowLight, "the small exit sign does not emit light");
        Helper.assertTrue(at(Helper, Plain).getLightEmission() == 0, "a gate plaque should not emit light");
        Helper.succeedWhen(() -> {
            int Brightness = Helper.getLevel().getBrightness(LightLayer.BLOCK, Helper.absolutePos(Left));
            Helper.assertTrue(Brightness == FacilitySignRegistry.GlowLight, "block light at the exit sign is " + Brightness);
            Helper.assertTrue(Helper.getLevel().getBrightness(LightLayer.BLOCK, Helper.absolutePos(Plaque)) == FacilitySignRegistry.GlowLight, "no block light at the exit plaque");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "facility_sign_header_place")
    public static void headerFillsThreeCellsAroundTheClick(GameTestHelper Helper) {
        buildWall(Helper);
        FacilityHeaderBlock Header = FacilitySignRegistry.header(FacilitySignDesign.GateA);
        FacilitySignTestPlayer User = player(Helper, new Vec3(5.5, 2.0, 1.5), 0.0f, "header_placer");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FacilitySignRegistry.HeaderItems.get(FacilitySignDesign.GateA).get(), 4));
        click(Helper, User, new BlockPos(5, 3, WallZ), Direction.NORTH, 0.3);
        BlockPos Centre = new BlockPos(5, 3, WallZ - 1);
        BlockState Middle = at(Helper, Centre);
        Helper.assertTrue(Middle.is(Header) && Middle.getValue(FacilityHeaderBlock.Part) == FacilityHeaderPart.Centre, "the clicked cell should hold the header centre: " + Middle);
        BlockState East = at(Helper, Centre.east());
        BlockState West = at(Helper, Centre.west());
        Helper.assertTrue(East.is(Header) && East.getValue(FacilityHeaderBlock.Part) == FacilityHeaderPart.Left, "the left part should be on the viewer's left: " + East);
        Helper.assertTrue(West.is(Header) && West.getValue(FacilityHeaderBlock.Part) == FacilityHeaderPart.Right, "the right part should be on the viewer's right: " + West);
        Helper.assertTrue(countSigns(Helper, Middle, 3, WallZ - 1) == 3, "a header should fill exactly three cells");
        Helper.assertTrue(User.getMainHandItem().getCount() == 3, "placing should use one header");
        Helper.setBlock(new BlockPos(9, 2, WallZ - 1), Blocks.GLASS.defaultBlockState());
        click(Helper, User, new BlockPos(8, 2, WallZ), Direction.NORTH, 0.0);
        Helper.assertTrue(at(Helper, new BlockPos(8, 2, WallZ - 1)).isAir() && at(Helper, new BlockPos(7, 2, WallZ - 1)).isAir(), "a header was placed with its left side blocked: " + row(Helper, 2));
        Helper.setBlock(new BlockPos(9, 2, WallZ - 1), Blocks.AIR.defaultBlockState());
        Helper.setBlock(new BlockPos(7, 2, WallZ - 1), Blocks.GLASS.defaultBlockState());
        click(Helper, User, new BlockPos(8, 2, WallZ), Direction.NORTH, 0.0);
        Helper.assertTrue(at(Helper, new BlockPos(8, 2, WallZ - 1)).isAir() && at(Helper, new BlockPos(9, 2, WallZ - 1)).isAir(), "a header was placed with its right side blocked: " + row(Helper, 2));
        Helper.assertTrue(User.getMainHandItem().getCount() == 3, "refused placements used up a header");
        Helper.setBlock(new BlockPos(7, 2, WallZ - 1), Blocks.AIR.defaultBlockState());
        click(Helper, User, new BlockPos(8, 2, WallZ), Direction.NORTH, 0.0);
        Helper.assertTrue(at(Helper, new BlockPos(7, 2, WallZ - 1)).is(Header) && at(Helper, new BlockPos(8, 2, WallZ - 1)).is(Header)
                && at(Helper, new BlockPos(9, 2, WallZ - 1)).is(Header), "the header was not placed once the space was clear");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "facility_sign_header_break")
    public static void breakingAnyHeaderPartRemovesAllAndDropsOne(GameTestHelper Helper) {
        buildWall(Helper);
        Item HeaderItem = FacilitySignRegistry.HeaderItems.get(FacilitySignDesign.GateC).get();
        FacilitySignTestPlayer User = player(Helper, new Vec3(5.5, 2.0, 1.5), 0.0f, "header_breaker");
        int Expected = 0;
        BlockPos[] Centres = {new BlockPos(2, 2, WallZ - 1), new BlockPos(6, 2, WallZ - 1), new BlockPos(10, 2, WallZ - 1)};
        for (int Index = 0; Index < 3; Index++) {
            List<BlockPos> Cells = placeHeader(Helper, FacilitySignDesign.GateC, Centres[Index], Direction.NORTH);
            User.gameMode.destroyBlock(Helper.absolutePos(Cells.get(Index)));
            Expected++;
            for (BlockPos Cell : Cells) {
                Helper.assertTrue(at(Helper, Cell).isAir(), "breaking header part " + Index + " left a piece at " + Cell);
            }
            Helper.assertTrue(dropped(Helper, HeaderItem) == Expected, "breaking header part " + Index + " should drop one header, total " + dropped(Helper, HeaderItem));
        }
        List<BlockPos> Cells = placeHeader(Helper, FacilitySignDesign.GateC, new BlockPos(6, 3, WallZ - 1), Direction.NORTH);
        Helper.setBlock(new BlockPos(5, 3, WallZ), Blocks.AIR.defaultBlockState());
        for (BlockPos Cell : Cells) {
            Helper.assertTrue(at(Helper, Cell).isAir(), "removing the wall behind one part left a piece at " + Cell);
        }
        Helper.assertTrue(dropped(Helper, HeaderItem) == Expected + 1, "losing the wall should drop one header");
        List<BlockPos> Creative = placeHeader(Helper, FacilitySignDesign.GateC, new BlockPos(2, 3, WallZ - 1), Direction.NORTH);
        User.setGameMode(GameType.CREATIVE);
        User.gameMode.destroyBlock(Helper.absolutePos(Creative.get(0)));
        for (BlockPos Cell : Creative) {
            Helper.assertTrue(at(Helper, Cell).isAir(), "a creative break left a header piece at " + Cell);
        }
        Helper.assertTrue(dropped(Helper, HeaderItem) == Expected + 1, "a creative break dropped a header");
        dismiss(Helper, User);
        Helper.succeed();
    }
}
