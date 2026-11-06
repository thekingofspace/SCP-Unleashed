package net.rai.scp_unleashed.scp327;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;

public class Scp327LookControl extends LookControl {
    public Scp327LookControl(Mob Owner) {
        super(Owner);
    }

    @Override
    protected boolean resetXRotOnTick() {
        return false;
    }
}
