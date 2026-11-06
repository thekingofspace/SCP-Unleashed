package net.rai.scp_unleashed.npc.doors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.rai.scp_unleashed.door.DoorButtonBlock;
import net.rai.scp_unleashed.door.DoorLeverBlock;
import net.rai.scp_unleashed.door.KeycardItem;
import net.rai.scp_unleashed.door.KeycardScannerBlock;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.door.ScanStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class NpcDoors {
    public static final int Search = 2;

    private NpcDoors() {
    }

    public static boolean blocks(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return isDoor(State) && !isOpen(State);
    }

    public static List<BlockPos> extent(Level World, BlockPos DoorPos) {
        BlockState State = World.getBlockState(DoorPos);
        if (State.getBlock() instanceof PanelDoorBlock Panel) {
            return List.copyOf(Panel.cells(Panel.originOf(DoorPos, State), State.getValue(PanelDoorBlock.Facing)));
        }
        if (State.getBlock() instanceof DoorBlock) {
            BlockPos Lower = State.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? DoorPos.below() : DoorPos.immutable();
            List<BlockPos> Cells = new ArrayList<>(List.of(Lower, Lower.above()));
            Direction Front = State.getValue(DoorBlock.FACING);
            for (Direction Side : new Direction[]{Front.getClockWise(), Front.getCounterClockWise()}) {
                BlockPos Next = Lower.relative(Side);
                BlockState Other = World.getBlockState(Next);
                if (Other.getBlock() instanceof DoorBlock && Other.getValue(DoorBlock.FACING) == Front && Other.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                    Cells.add(Next);
                    Cells.add(Next.above());
                }
            }
            return List.copyOf(Cells);
        }
        if (State.getBlock() instanceof TrapDoorBlock || State.getBlock() instanceof FenceGateBlock) {
            return List.of(DoorPos.immutable());
        }
        return List.of();
    }

    public static Optional<BlockPos> findOpener(Level World, BlockPos DoorPos) {
        List<BlockPos> Cells = extent(World, DoorPos);
        return around(World, Cells).stream().filter(Pos -> isOpener(World.getBlockState(Pos))).min(nearest(Cells));
    }

    public static boolean useOpener(LivingEntity User, BlockPos Opener) {
        if (!(User.level() instanceof ServerLevel World)) {
            return false;
        }
        BlockState State = World.getBlockState(Opener);
        if (!isOpener(State) || State.getBlock() instanceof ButtonBlock && isPowered(State) || State.getBlock() instanceof DoorButtonBlock && isPowered(State)) {
            return false;
        }
        if (!servesClosedDoor(World, Opener)) {
            return false;
        }
        InteractionResult Result = State.useWithoutItem(World, hand(World, User, Opener), hit(User, Opener));
        if (!Result.consumesAction()) {
            return false;
        }
        User.swing(InteractionHand.MAIN_HAND, true);
        if (User instanceof Mob Walker) {
            Walker.getLookControl().setLookAt(Vec3.atCenterOf(Opener));
        }
        return true;
    }

    public static Optional<BlockPos> findScanner(Level World, BlockPos DoorPos) {
        List<BlockPos> Cells = extent(World, DoorPos);
        return around(World, Cells).stream().filter(Pos -> World.getBlockState(Pos).getBlock() instanceof KeycardScannerBlock).min(nearest(Cells));
    }

    public static Optional<ItemStack> pickCard(Level World, BlockPos Scanner, Iterable<ItemStack> Carried) {
        BlockState State = World.getBlockState(Scanner);
        if (!(State.getBlock() instanceof KeycardScannerBlock)) {
            return Optional.empty();
        }
        int Required = State.getValue(KeycardScannerBlock.Clearance);
        ItemStack Best = null;
        int BestRank = Integer.MAX_VALUE;
        for (ItemStack Stack : Carried) {
            if (Stack.isEmpty() || !(Stack.getItem() instanceof KeycardItem Card) || !Card.opens(Required)) {
                continue;
            }
            int Rank = Card.isOmni() ? KeycardItem.OmniClearance : Card.getClearance();
            if (Rank < BestRank) {
                Best = Stack;
                BestRank = Rank;
            }
        }
        return Optional.ofNullable(Best);
    }

    public static boolean swipe(LivingEntity User, ItemStack Card, BlockPos Scanner) {
        if (!(User.level() instanceof ServerLevel World) || !(Card.getItem() instanceof KeycardItem)) {
            return false;
        }
        BlockState State = World.getBlockState(Scanner);
        if (!(State.getBlock() instanceof KeycardScannerBlock) || State.getValue(KeycardScannerBlock.Status) != ScanStatus.Idle) {
            return false;
        }
        FakePlayer Hand = hand(World, User, Scanner);
        Hand.setItemSlot(EquipmentSlot.MAINHAND, Card.copyWithCount(1));
        try {
            State.useItemOn(Hand.getMainHandItem(), World, Hand, InteractionHand.MAIN_HAND, hit(User, Scanner));
        } finally {
            Hand.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        User.swing(InteractionHand.MAIN_HAND, true);
        if (User instanceof Mob Walker) {
            Walker.getLookControl().setLookAt(Vec3.atCenterOf(Scanner));
        }
        BlockState After = World.getBlockState(Scanner);
        return After.getBlock() instanceof KeycardScannerBlock && After.getValue(KeycardScannerBlock.Status) == ScanStatus.Granted;
    }

    private static boolean isDoor(BlockState State) {
        return State.getBlock() instanceof DoorBlock || State.getBlock() instanceof PanelDoorBlock || State.getBlock() instanceof TrapDoorBlock
                || State.getBlock() instanceof FenceGateBlock;
    }

    private static boolean isOpen(BlockState State) {
        return State.hasProperty(BlockStateProperties.OPEN) && State.getValue(BlockStateProperties.OPEN);
    }

    private static boolean isPowered(BlockState State) {
        return State.hasProperty(BlockStateProperties.POWERED) && State.getValue(BlockStateProperties.POWERED);
    }

    private static boolean isOpener(BlockState State) {
        return State.getBlock() instanceof ButtonBlock || State.getBlock() instanceof DoorButtonBlock || State.getBlock() instanceof LeverBlock
                || State.getBlock() instanceof DoorLeverBlock;
    }

    private static boolean servesClosedDoor(Level World, BlockPos Opener) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Search; Dx <= Search; Dx++) {
            for (int Dy = -Search; Dy <= Search; Dy++) {
                for (int Dz = -Search; Dz <= Search; Dz++) {
                    Cursor.set(Opener.getX() + Dx, Opener.getY() + Dy, Opener.getZ() + Dz);
                    BlockState State = World.getBlockState(Cursor);
                    if (isDoor(State) && !isOpen(State)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static List<BlockPos> around(Level World, List<BlockPos> Cells) {
        if (Cells.isEmpty()) {
            return List.of();
        }
        Set<BlockPos> Inside = new LinkedHashSet<>(Cells);
        Set<BlockPos> Seen = new LinkedHashSet<>();
        for (BlockPos Cell : Cells) {
            for (int Dx = -Search; Dx <= Search; Dx++) {
                for (int Dy = -Search; Dy <= Search; Dy++) {
                    for (int Dz = -Search; Dz <= Search; Dz++) {
                        BlockPos Pos = Cell.offset(Dx, Dy, Dz);
                        if (!Inside.contains(Pos)) {
                            Seen.add(Pos);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(Seen);
    }

    private static Comparator<BlockPos> nearest(List<BlockPos> Cells) {
        double X = Cells.stream().mapToDouble(Pos -> Pos.getX() + 0.5).average().orElse(0.0);
        double Y = Cells.stream().mapToDouble(Pos -> Pos.getY() + 0.5).average().orElse(0.0);
        double Z = Cells.stream().mapToDouble(Pos -> Pos.getZ() + 0.5).average().orElse(0.0);
        Vec3 Middle = new Vec3(X, Y, Z);
        return Comparator.comparingDouble(Pos -> Vec3.atCenterOf(Pos).distanceToSqr(Middle));
    }

    private static FakePlayer hand(ServerLevel World, LivingEntity User, BlockPos Pos) {
        FakePlayer Hand = FakePlayerFactory.getMinecraft(World);
        Vec3 Eye = User.getEyePosition();
        Vec3 Centre = Vec3.atCenterOf(Pos);
        float Yaw = (float) (Mth.atan2(Centre.z - Eye.z, Centre.x - Eye.x) * Mth.RAD_TO_DEG) - 90.0f;
        Hand.moveTo(Eye.x, Eye.y - Hand.getEyeHeight(), Eye.z, Yaw, 0.0f);
        return Hand;
    }

    private static BlockHitResult hit(LivingEntity User, BlockPos Pos) {
        Vec3 Centre = Vec3.atCenterOf(Pos);
        Vec3 Toward = User.getEyePosition().subtract(Centre);
        return new BlockHitResult(Centre, Direction.getNearest(Toward.x, Toward.y, Toward.z), Pos, false);
    }
}
