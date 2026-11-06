package net.rai.scp_unleashed.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class FactionControl {
    private FactionControl() {
    }

    public static FactionState state(MinecraftServer Server, FactionType Type) {
        return FactionWorld.state(Server, Type);
    }

    public static boolean awaken(ServerLevel World, FactionType Type, BlockPos Near, @Nullable Entity Cause) {
        FactionState State = state(World.getServer(), Type);
        if (State.phase() != FactionPhase.Dormant) {
            return false;
        }
        long Now = World.getServer().overworld().getGameTime();
        State.setPhase(FactionPhase.Awakened);
        State.setAwakenedAt(Now);
        State.setLastAccrual(Now);
        State.setLastExpansion(Now);
        State.setLastPatrol(Now);
        State.setLastRaid(Now);
        Type.awaken(World, State, Near, Cause);
        return true;
    }

    public static boolean defeat(MinecraftServer Server, FactionType Type, @Nullable BlockPos Where) {
        FactionState State = state(Server, Type);
        if (State.phase() == FactionPhase.Defeated) {
            return false;
        }
        State.setPhase(FactionPhase.Defeated);
        State.nextEra();
        Type.defeat(Server, State, Where);
        for (FactionMember Member : List.copyOf(State.roster())) {
            State.dismiss(Member.Id());
        }
        return true;
    }

    public static void reset(MinecraftServer Server, FactionType Type) {
        FactionState State = state(Server, Type);
        Type.reset(Server, State);
        State.nextEra();
        State.clearAll();
    }

    public static void enlist(FactionType Type, FactionState State, Mob Member, int Site, String Role) {
        Member.setData(FactionRegistry.Bond, new FactionBond(Type.id().toString(), State.era(), Site, Role));
        Member.setPersistenceRequired();
        if (Site >= 0 && State.roster().size() < Type.rules().RosterCap()) {
            State.enroll(new FactionMember(Member.getUUID(), Site, Role, Member.level().dimension(), Member.blockPosition()));
        }
    }

    public static void stamp(FactionType Type, FactionState State, Entity Body) {
        Body.setData(FactionRegistry.Bond, new FactionBond(Type.id().toString(), State.era(), -1, ""));
    }

    public static void addPower(MinecraftServer Server, FactionType Type, double Amount) {
        FactionState State = state(Server, Type);
        if (State.phase().active()) {
            State.setPower(State.power() + Amount, Type.rules().MaxPower());
        }
    }

    public static boolean current(FactionState State, FactionBond Bond) {
        return Bond.Era() == State.era() && State.phase() != FactionPhase.Defeated;
    }
}
