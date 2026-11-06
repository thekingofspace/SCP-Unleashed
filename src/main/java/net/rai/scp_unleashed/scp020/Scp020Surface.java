package net.rai.scp_unleashed.scp020;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.labware.LabSurfaces;

import java.util.List;

public final class Scp020Surface implements LabSurfaces.Surface {
    public static final int SwabReach = 1;

    @Override
    public void sample(ServerLevel World, BlockPos Pos, Direction Side, List<ResourceLocation> Found) {
        if (!Scp020Infection.anyNear(World, Pos, SwabReach)) {
            return;
        }
        Found.add(Scp020Organism.Id);
        Scp020Mold.disturb(World, Pos);
    }

    @Override
    public int clean(ServerLevel World, BlockPos Centre, int Radius) {
        return Scp020Infection.clearIn(World, Centre, Radius, (Host, Side) -> {
        });
    }
}
