package net.rai.scp_unleashed.scp143;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class Scp143Boat extends Boat {
    public Scp143Boat(EntityType<? extends Boat> Kind, Level World) {
        super(Kind, World);
    }

    public static Scp143Boat at(Level World, double X, double Y, double Z) {
        Scp143Boat Made = new Scp143Boat(Scp143Registry.BoatType.get(), World);
        Made.setPos(X, Y, Z);
        Made.xo = X;
        Made.yo = Y;
        Made.zo = Z;
        return Made;
    }

    @Override
    public Item getDropItem() {
        return Scp143Registry.BladewoodBoat.get();
    }

    @Override
    @Nullable
    public ItemEntity spawnAtLocation(ItemLike Dropped, int OffsetY) {
        return super.spawnAtLocation(Scp143Boat.swap(Dropped), OffsetY);
    }

    public static ItemLike swap(ItemLike Dropped) {
        return Dropped.asItem() == Boat.Type.OAK.getPlanks().asItem() ? Scp143Registry.PlanksItem.get() : Dropped;
    }
}
