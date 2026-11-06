package net.rai.scp_unleashed.scp001scarlet.cult;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionRules;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionType;
import net.rai.scp_unleashed.raid.RaidStyle;
import net.rai.scp_unleashed.scp001scarlet.Scp001ScarletRegistry;
import net.rai.scp_unleashed.scp001scarlet.ScarletConfig;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;

public final class ScarletCultFaction extends FactionType {
    public static final ScarletCultFaction Instance = new ScarletCultFaction();
    public static final long NightStart = 13000L;
    public static final long NightEnd = 23000L;
    private boolean Ready;
    private static boolean IgnoreNight;

    private ScarletCultFaction() {
        super(Scp001ScarletRegistry.location("scarlet_cultists"), 0x9E2A2B, new FactionRules(0.0, 0.0, 0.0, 0, 0, 0L, 0L, 0, 0, 0L));
    }

    public static boolean allowed(MinecraftServer Server) {
        ScarletState.Phase Phase = ScarletState.get(Server).phase();
        return ScarletConfig.cultists() && (Phase == ScarletState.Phase.Stirring || Phase == ScarletState.Phase.Xk2);
    }

    public static boolean night(ServerLevel World) {
        if (IgnoreNight) {
            return true;
        }
        long Time = World.getDayTime() % 24000L;
        return Time >= NightStart && Time <= NightEnd;
    }

    public static void ignoreNightForTests(boolean Value) {
        IgnoreNight = Value;
    }

    @Override
    public RaidStyle raidStyle() {
        return ScarletRitualStyle.Instance;
    }

    @Override
    public boolean canRaid(FactionState State) {
        return Ready && State.phase().active();
    }

    public boolean ready() {
        return Ready;
    }

    @Override
    public void tick(MinecraftServer Server, FactionState State) {
        ScarletState.Phase Phase = ScarletState.get(Server).phase();
        ServerLevel Overworld = Server.overworld();
        if ((Phase == ScarletState.Phase.Stirring || Phase == ScarletState.Phase.Xk2) && State.phase() == FactionPhase.Dormant) {
            FactionControl.awaken(Overworld, this, Overworld.getSharedSpawnPos(), null);
        } else if (Phase == ScarletState.Phase.Freed && State.phase() != FactionPhase.Defeated) {
            FactionControl.defeat(Server, this, null);
        }
        Ready = State.phase().active() && allowed(Server) && night(Overworld);
    }

    @Override
    public boolean idleMember(net.minecraft.world.entity.Mob Member) {
        return false;
    }
}
