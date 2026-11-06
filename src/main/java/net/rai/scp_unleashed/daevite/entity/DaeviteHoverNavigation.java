package net.rai.scp_unleashed.daevite.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;

public class DaeviteHoverNavigation extends GroundPathNavigation {
    public DaeviteHoverNavigation(Mob Owner, Level World) {
        super(Owner, World);
        setCanFloat(true);
        setCanOpenDoors(true);
        setCanPassDoors(true);
    }

    @Override
    protected boolean canUpdatePath() {
        return true;
    }
}
