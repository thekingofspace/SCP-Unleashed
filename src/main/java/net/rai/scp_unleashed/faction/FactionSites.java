package net.rai.scp_unleashed.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class FactionSites {
    private FactionSites() {
    }

    public static boolean raze(ServerLevel World, FactionType Type, FactionSite Site, @Nullable BlockPos Where) {
        FactionState State = FactionWorld.state(World.getServer(), Type);
        if (!State.sites().remove(Site)) {
            return false;
        }
        for (FactionMember Member : List.copyOf(State.roster())) {
            if (Member.Site() == Site.id()) {
                State.dismiss(Member.Id());
            }
        }
        State.changed();
        Type.siteRazed(World, State, Site, Where);
        if (Type.fallsWithLastSite() && State.sites().isEmpty() && State.phase() != FactionPhase.Defeated) {
            FactionControl.defeat(World.getServer(), Type, Where);
        }
        return true;
    }

    public static int remaining(FactionState State, String Kind) {
        return (int) State.countSites(Kind);
    }
}
