package net.rai.scp_unleashed.raid.objective;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.rai.scp_unleashed.raid.Raid;

public interface RaidObjective {
    ResourceLocation id();

    void attach(Raid Current, Mob Raider);
}
