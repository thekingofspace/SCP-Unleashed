package net.rai.scp_unleashed.scp143;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class Scp143ChestBoat extends ChestBoat {
    public Scp143ChestBoat(EntityType<? extends Boat> Kind, Level World) {
        super(Kind, World);
    }

    public static Scp143ChestBoat at(Level World, double X, double Y, double Z) {
        Scp143ChestBoat Made = new Scp143ChestBoat(Scp143Registry.ChestBoatType.get(), World);
        Made.setPos(X, Y, Z);
        Made.xo = X;
        Made.yo = Y;
        Made.zo = Z;
        return Made;
    }

    @Override
    public Item getDropItem() {
        return Scp143Registry.BladewoodChestBoat.get();
    }

    @Override
    @Nullable
    public ItemEntity spawnAtLocation(ItemLike Dropped, int OffsetY) {
        return super.spawnAtLocation(Scp143Boat.swap(Dropped), OffsetY);
    }
}
