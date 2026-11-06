package net.rai.scp_unleashed.props.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.props.PropsRegistry;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class PropSeatGameTests {
    public static final String Arena = "gear_arena";
    public static final int SettleTicks = 10;

    private PropSeatGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "seat_holds")
    public static void seatsHoldTheirRiders(GameTestHelper Helper) {
        List<Block> Seats = List.of(PropsRegistry.OfficeChair.get(), Scp4666Registry.Chair.get(), Scp4666Registry.Armchair.get(), Scp4666Registry.RockingChair.get());
        List<PropSeatEntity> Placed = Seats.stream().map(Chair -> seat(Helper, Chair, new BlockPos(2 + Seats.indexOf(Chair) * 2, 1, 2))).toList();
        PropSeatEntity Table = seat(Helper, Scp4666Registry.Table.get(), new BlockPos(2, 1, 4));
        Helper.runAfterDelay(SettleTicks, () -> {
            for (int Index = 0; Index < Placed.size(); Index++) {
                PropSeatEntity Seat = Placed.get(Index);
                Helper.assertTrue(Seat.isAlive() && Seat.isVehicle(), "the seat on " + Seats.get(Index).getDescriptionId() + " threw its rider off");
            }
            Helper.assertFalse(Table.isAlive(), "a seat on a table stayed");
            Helper.succeed();
        });
    }

    private static PropSeatEntity seat(GameTestHelper Helper, Block Chair, BlockPos Local) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Local);
        World.setBlockAndUpdate(Pos, Chair.defaultBlockState());
        PropSeatEntity Seat = new PropSeatEntity(PropsRegistry.Seat.get(), World);
        Seat.moveTo(Pos.getX() + 0.5, Pos.getY(), Pos.getZ() + 0.5, 0.0f, 0.0f);
        World.addFreshEntity(Seat);
        ArmorStand Rider = EntityType.ARMOR_STAND.create(World);
        Helper.assertTrue(Rider != null, "no armor stand");
        Rider.moveTo(Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, 0.0f, 0.0f);
        World.addFreshEntity(Rider);
        Helper.assertTrue(Rider.startRiding(Seat, true), "the rider could not sit on " + Chair.getDescriptionId());
        return Seat;
    }
}
