package net.rai.scp_unleashed.anomaly.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp529.Scp529Registry;
import net.rai.scp_unleashed.scp999.Scp999Registry;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class VehicleRefusalGameTests {
    public static final String Arena = "gear_arena";

    private VehicleRefusalGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "anomaly_vehicles")
    public static void anomaliesStayOutOfBoatsAndCarts(GameTestHelper Helper) {
        List<EntityType<?>> Vehicles = List.of(EntityType.BOAT, EntityType.CHEST_BOAT, EntityType.MINECART);
        int Column = 1;
        for (EntityType<?> VehicleType : Vehicles) {
            Entity Josie = Helper.spawn(Scp529Registry.Josie.get(), new BlockPos(Column, 1, 1));
            Entity Blob = Helper.spawn(Scp999Registry.Blob.get(), new BlockPos(Column, 1, 3));
            Mob Zombie = Helper.spawn(EntityType.ZOMBIE, new BlockPos(Column, 1, 5));
            Entity First = Helper.spawn(VehicleType, new BlockPos(Column, 1, 1));
            Entity Second = Helper.spawn(VehicleType, new BlockPos(Column, 1, 3));
            Entity Third = Helper.spawn(VehicleType, new BlockPos(Column, 1, 5));
            Helper.assertFalse(Josie.startRiding(First, true), "SCP-529 got into a " + VehicleType.getDescriptionId());
            Helper.assertFalse(Blob.startRiding(Second, true), "SCP-999 got into a " + VehicleType.getDescriptionId());
            Helper.assertTrue(Zombie.startRiding(Third, true), "a zombie could not get into a " + VehicleType.getDescriptionId());
            Column += 3;
        }
        Helper.succeed();
    }
}
