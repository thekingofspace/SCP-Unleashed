package net.rai.scp_unleashed.fusion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WireTier;

public final class FusionBus {
    public static final int Unlimited = Integer.MAX_VALUE / 4;

    private FusionBus() {
    }

    public static Rating rate(Level World, BlockPos PortPos) {
        long Total = 0;
        int Outlets = 0;
        int Links = 0;
        for (Direction Side : Direction.values()) {
            BlockPos Near = PortPos.relative(Side);
            if (!World.isLoaded(Near)) {
                continue;
            }
            BlockState NearState = World.getBlockState(Near);
            if (!(NearState.getBlock() instanceof WireConnector Connector) || !Connector.hostPos(NearState, Near).equals(PortPos)
                    || !(World.getBlockEntity(Near) instanceof OutletBlockEntity Outlet)) {
                continue;
            }
            Outlets++;
            long Carried = 0;
            boolean Endless = false;
            for (WireLink Link : Outlet.getLinks()) {
                Links++;
                Endless |= Link.Tier() == WireTier.Creative;
                Carried += Link.Tier().getMaxDemand();
            }
            long Mount = Endless ? Unlimited : Connector.getConnectorTier().getMaxDemand();
            Total += Math.min(Mount, Carried);
        }
        return new Rating((int) Math.min(Unlimited, Total), Outlets, Links);
    }

    public record Rating(int Capacity, int Outlets, int Links) {
        public static final Rating Empty = new Rating(0, 0, 0);
    }
}
