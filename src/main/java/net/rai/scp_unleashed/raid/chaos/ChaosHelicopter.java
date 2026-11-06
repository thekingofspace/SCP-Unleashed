package net.rai.scp_unleashed.raid.chaos;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.mtf.MtfHelicopter;
import net.rai.scp_unleashed.mtf.MtfLandingZone;
import net.rai.scp_unleashed.mtf.MtfTier;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidWorld;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChaosHelicopter extends MtfHelicopter {
    private int RaidId = -1;
    private int Crew;
    private int Linger;
    private final int[] Ready = new int[2];
    private final List<List<UUID>> Riders = List.of(new ArrayList<>(), new ArrayList<>());

    public ChaosHelicopter(EntityType<? extends ChaosHelicopter> Type, Level World) {
        super(Type, World);
    }

    @Nullable
    public static ChaosHelicopter dispatch(ServerLevel World, Raid Current, MtfLandingZone.Zone Pad, int Crew, boolean Pickup) {
        ChaosHelicopter Made = HostileRegistry.HelicopterType.get().create(World);
        if (Made == null) {
            return null;
        }
        Vec3 Entry = MtfLandingZone.entry(World, Pad, World.getRandom());
        Made.setup(null, MtfTier.Faint, Pickup, Pad, Entry);
        Made.RaidId = Current.id();
        Made.Crew = Pickup ? 0 : Crew;
        if (!World.addFreshEntity(Made)) {
            return null;
        }
        return Made;
    }

    public int raidId() {
        return RaidId;
    }

    public int crew() {
        return Crew;
    }

    public void toPickup(MtfLandingZone.Zone Pad) {
        Crew = 0;
        Riders.get(0).clear();
        Riders.get(1).clear();
        Linger = 0;
        becomePickup(Pad);
    }

    @Nullable
    public Raid raid(ServerLevel World) {
        Raid Found = RaidId < 0 ? null : RaidWorld.get(World.getServer()).raid(RaidId);
        return Found != null && Found.live() ? Found : null;
    }

    @Override
    protected void serverTick(ServerLevel World) {
        Raid Current = raid(World);
        if (Current == null && phase() != PhaseDepart) {
            depart();
        }
        switch (phase()) {
            case PhaseApproach -> approach(World);
            case PhaseHover -> settle(World, null);
            case PhaseDeploy -> deployCrew(World, Current);
            case PhasePickup -> pickupCrew(World, Current);
            default -> leave(World);
        }
    }

    private List<ChaosInsurgent> riders(ServerLevel World, int Side, byte State) {
        List<ChaosInsurgent> Found = new ArrayList<>();
        List<UUID> Listed = Riders.get(Side);
        Listed.removeIf(Id -> !(World.getEntity(Id) instanceof ChaosInsurgent Rider) || !Rider.isAlive() || Rider.state() != State);
        for (UUID Id : Listed) {
            if (World.getEntity(Id) instanceof ChaosInsurgent Rider) {
                Found.add(Rider);
            }
        }
        return Found;
    }

    private void deployCrew(ServerLevel World, @Nullable Raid Current) {
        Vec3 Spot = hover();
        setPos(Spot.x, Spot.y, Spot.z);
        if (Current == null) {
            Crew = 0;
        }
        boolean Busy = false;
        for (int Side = 0; Side < 2; Side++) {
            List<ChaosInsurgent> Sliding = riders(World, Side, HostileRaider.StateRoping);
            Busy |= !Sliding.isEmpty();
            ChaosInsurgent Last = Sliding.isEmpty() ? null : Sliding.get(Sliding.size() - 1);
            boolean Free = Last == null || Last.ropeTop() - Last.getY() > NextRiderGap;
            if (Crew > 0 && Free && PhaseTicks >= Ready[Side] && Current != null) {
                ChaosInsurgent Rider = ChaosRaidStyle.rider(World, Current, this, Side);
                if (Rider != null) {
                    Riders.get(Side).add(Rider.getUUID());
                    Crew--;
                    Busy = true;
                } else {
                    Crew = 0;
                }
                Ready[Side] = PhaseTicks + RopeGap;
            }
        }
        if (Crew > 0 || Busy) {
            Linger = 0;
            return;
        }
        if (++Linger >= LingerTicks) {
            if (Current != null) {
                ChaosRaidStyle.deployed(World, Current);
            }
            depart();
        }
    }

    private void pickupCrew(ServerLevel World, @Nullable Raid Current) {
        Vec3 Spot = hover();
        setPos(Spot.x, Spot.y, Spot.z);
        boolean Busy = false;
        for (int Side = 0; Side < 2; Side++) {
            List<ChaosInsurgent> Climbing = riders(World, Side, HostileRaider.StateBoarding);
            Busy |= !Climbing.isEmpty();
            ChaosInsurgent Last = Climbing.isEmpty() ? null : Climbing.get(Climbing.size() - 1);
            boolean Free = Last == null || Last.getY() - groundBelow(World, Side) > NextRiderGap;
            if (Current == null || !Free || PhaseTicks < Ready[Side]) {
                continue;
            }
            ChaosInsurgent Next = waiting(World, Current, Side);
            if (Next != null) {
                Next.beginBoarding(this, Side);
                Riders.get(Side).add(Next.getUUID());
                Ready[Side] = PhaseTicks + RopeGap;
                Busy = true;
            }
        }
        boolean Expecting = Current != null && ChaosRaidStyle.expecting(World, Current);
        if (Busy || Expecting && PhaseTicks < PickupPatience) {
            Linger = 0;
            return;
        }
        if (++Linger >= LingerTicks) {
            depart();
        }
    }

    private double groundBelow(ServerLevel World, int Side) {
        Vec3 Anchor = MtfLandingZone.anchor(hover(), hoverYaw(), Side);
        return MtfLandingZone.surface(World, Anchor.x, Anchor.z);
    }

    @Nullable
    private ChaosInsurgent waiting(ServerLevel World, Raid Current, int Side) {
        Vec3 Stand = MtfLandingZone.standPoint(hover(), hoverYaw(), Side);
        ChaosInsurgent Best = null;
        double BestGap = (BoardReach + 1.0) * (BoardReach + 1.0);
        for (UUID Id : Current.raiders()) {
            if (!(World.getEntity(Id) instanceof ChaosInsurgent Candidate) || !Candidate.isAlive() || Candidate.state() != HostileRaider.StateWaiting) {
                continue;
            }
            double Gap = (Candidate.getX() - Stand.x) * (Candidate.getX() - Stand.x) + (Candidate.getZ() - Stand.z) * (Candidate.getZ() - Stand.z);
            if (Gap < BestGap && Math.abs(Candidate.getY() - groundBelow(World, Side)) < 2.5) {
                BestGap = Gap;
                Best = Candidate;
            }
        }
        return Best;
    }
}
