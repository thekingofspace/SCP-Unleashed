package net.rai.scp_unleashed.raid.objective;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidMark;
import net.rai.scp_unleashed.raid.RaidRegistry;
import net.rai.scp_unleashed.raid.RaidStage;
import net.rai.scp_unleashed.raid.RaidWorld;
import org.jetbrains.annotations.Nullable;

public abstract class RaidGoal extends Goal {
    protected final Mob Owner;
    private final ResourceLocation Objective;

    protected RaidGoal(Mob Owner, ResourceLocation Objective) {
        this.Owner = Owner;
        this.Objective = Objective;
    }

    public ResourceLocation objective() {
        return Objective;
    }

    @Nullable
    protected ServerLevel world() {
        return Owner.level() instanceof ServerLevel World ? World : null;
    }

    @Nullable
    protected Raid raid() {
        ServerLevel World = world();
        if (World == null || !Owner.hasData(RaidRegistry.Mark)) {
            return null;
        }
        RaidMark Mark = Owner.getData(RaidRegistry.Mark);
        if (!Mark.marked()) {
            return null;
        }
        Raid Found = RaidWorld.get(World.getServer()).raid(Mark.Raid());
        return Found != null && Found.live() ? Found : null;
    }

    @Nullable
    protected Raid activeRaid() {
        Raid Found = raid();
        return Found != null && Found.stage() == RaidStage.Active ? Found : null;
    }

    public static boolean attached(Mob Raider, ResourceLocation Objective) {
        return Raider.goalSelector.getAvailableGoals().stream().anyMatch(Wrapped -> Wrapped.getGoal() instanceof RaidGoal Found && Found.objective().equals(Objective));
    }
}
