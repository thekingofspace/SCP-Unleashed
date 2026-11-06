package net.rai.scp_unleashed.scp1730;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.scp1730.plan.Scp1730RoomInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp1730Containment {
    public static final String WingTag = "scp1730_wing";
    public static final double LeakRange = 16.0;

    private Scp1730Containment() {
    }

    public static boolean biological(String Id) {
        return Scp1730Breach.biological(Id);
    }

    public static boolean biological(List<Scp1730Breach.Threat> Group) {
        return Scp1730Breach.biological(Group);
    }

    public static List<Scp1730Breach.Threat> allowed(int Zone, boolean Wing) {
        if (!Wing) {
            return Scp1730Breach.halls(Zone);
        }
        List<Scp1730Breach.Threat> Found = new ArrayList<>();
        for (Scp1730Breach.Threat Each : Scp1730Breach.roster(Zone)) {
            if (biological(Each.Id()) == Wing) {
                Found.add(Each);
            }
        }
        return Found;
    }

    @Nullable
    public static Scp1730Breach.Threat pick(int Zone, boolean Wing, RandomSource Random) {
        List<Scp1730Breach.Threat> Table = allowed(Zone, Wing);
        int Total = 0;
        for (Scp1730Breach.Threat Each : Table) {
            Total += Each.Weight();
        }
        if (Total <= 0) {
            return null;
        }
        int Roll = Random.nextInt(Total);
        for (Scp1730Breach.Threat Each : Table) {
            Roll -= Each.Weight();
            if (Roll < 0) {
                return Each;
            }
        }
        return Table.getFirst();
    }

    public static List<Scp1730Breach.Threat> setPiece(int Zone, int Number, long Seed, boolean Wing) {
        List<Scp1730Breach.Threat> Picked = Scp1730Breach.setPiece(Zone, Number, Seed);
        return biological(Picked) == Wing ? Picked : List.of();
    }

    public static boolean inWing(Scp1730Plan Plan, int Run, Vec3 At) {
        return Plan.inWing(Scp1730World.localX(Run, At.x), (int) Math.floor(At.y), Scp1730World.localZ(At.z));
    }

    @Nullable
    public static Scp1730RoomInfo wingOf(Scp1730Plan Plan, int Run, Vec3 At) {
        return Plan.wingAt(Scp1730World.localX(Run, At.x), (int) Math.floor(At.y), Scp1730World.localZ(At.z));
    }

    public static void settle(int Run, Scp1730RoomInfo Wing, Mob Creature) {
        if (Wing == null) {
            return;
        }
        BlockPos Centre = Scp1730World.world(Run, (Wing.MinX() + Wing.MaxX()) / 2, Wing.Floor() + 1, (Wing.MinZ() + Wing.MaxZ()) / 2);
        int Radius = Math.max(Wing.MaxX() - Wing.MinX(), Wing.MaxZ() - Wing.MinZ()) / 2 - 1;
        Creature.restrictTo(Centre, Math.max(4, Radius));
        Creature.addTag(WingTag);
    }

    public static boolean leaked(ServerLevel World, Scp1730Plan Plan, int Run, Mob Creature) {
        if (!Creature.getTags().contains(WingTag) && !biological(idOf(Creature))) {
            return false;
        }
        if (inWing(Plan, Run, Creature.position())) {
            return false;
        }
        return World.getNearestPlayer(Creature, LeakRange) == null;
    }

    public static String idOf(Mob Creature) {
        var Key = EntityType.getKey(Creature.getType());
        return Key.getNamespace().equals(net.rai.scp_unleashed.Scp_unleashed.ModId) ? Key.getPath() : Key.toString();
    }
}
