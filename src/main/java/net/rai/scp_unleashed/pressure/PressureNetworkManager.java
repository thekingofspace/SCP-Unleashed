package net.rai.scp_unleashed.pressure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.rai.scp_unleashed.block.DuctBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class PressureNetworkManager {
    private static final Map<ServerLevel, PressureNetworkManager> Managers = new WeakHashMap<>();
    private static final int RescanTicks = 200;
    private static final int SaveTicks = 40;
    private static final int SeedsPerTick = 256;
    private static final int PruneTicks = 100;

    private final Map<BlockPos, PressureNetwork> NetworkByMember = new HashMap<>();
    private final Set<PressureNetwork> Networks = new LinkedHashSet<>();
    private final Set<BlockPos> Seeds = new LinkedHashSet<>();
    private final Map<BlockPos, FluidTank> IntakeBuffers = new HashMap<>();

    public static PressureNetworkManager get(ServerLevel World) {
        return Managers.computeIfAbsent(World, Key -> new PressureNetworkManager());
    }

    public static void blockChanged(ServerLevel World, BlockPos Pos) {
        PressureNetworkManager Manager = Managers.get(World);
        if (Manager != null) {
            Manager.invalidateAround(World, Pos);
        }
    }

    public static void tickLevel(ServerLevel World) {
        PressureNetworkManager Manager = Managers.get(World);
        if (Manager != null) {
            Manager.tick(World);
        }
    }

    public static void saveAll(ServerLevel World) {
        PressureNetworkManager Manager = Managers.get(World);
        if (Manager != null) {
            for (PressureNetwork Network : Manager.Networks) {
                Network.writeBack(World, true);
            }
        }
    }

    public static void forget(ServerLevel World) {
        saveAll(World);
        Managers.remove(World);
    }

    private void invalidateAround(ServerLevel World, BlockPos Pos) {
        if (NetworkByMember.isEmpty()) {
            return;
        }
        discard(World, NetworkByMember.get(Pos));
        for (Direction Side : Direction.values()) {
            discard(World, NetworkByMember.get(Pos.relative(Side)));
        }
    }

    private void discard(ServerLevel World, @Nullable PressureNetwork Network) {
        if (Network == null || !Networks.remove(Network)) {
            return;
        }
        Network.writeBack(World, true);
        for (BlockPos Member : Network.getMembers()) {
            NetworkByMember.remove(Member, Network);
        }
        if (Network.getPressure() > 0.5) {
            Seeds.addAll(Network.getMembers());
        }
    }

    public PressureNetwork networkAt(ServerLevel World, BlockPos Pos, DuctKind Kind) {
        PressureNetwork Network = NetworkByMember.get(Pos);
        if (Network != null && Network.getKind() == Kind) {
            return Network;
        }
        discard(World, Network);
        Network = PressureNetwork.scan(World, Pos, Kind);
        for (BlockPos Member : Network.getMembers()) {
            PressureNetwork Previous = NetworkByMember.put(Member, Network);
            if (Previous != null && Previous != Network && Networks.remove(Previous)) {
                for (BlockPos Stale : Previous.getMembers()) {
                    NetworkByMember.remove(Stale, Previous);
                }
            }
        }
        Networks.add(Network);
        return Network;
    }

    private void tick(ServerLevel World) {
        long Now = World.getGameTime();
        if (Now % PruneTicks == 0) {
            IntakeBuffers.values().removeIf(FluidTank::isEmpty);
        }
        if (!Seeds.isEmpty()) {
            List<BlockPos> Batch = new ArrayList<>();
            Iterator<BlockPos> Pending = Seeds.iterator();
            while (Pending.hasNext() && Batch.size() < SeedsPerTick) {
                Batch.add(Pending.next());
                Pending.remove();
            }
            for (BlockPos Seed : Batch) {
                if (!NetworkByMember.containsKey(Seed) && World.isLoaded(Seed) && World.getBlockState(Seed).getBlock() instanceof DuctBlock Duct) {
                    networkAt(World, Seed, Duct.getKind());
                }
            }
        }
        for (PressureNetwork Network : List.copyOf(Networks)) {
            if (!Networks.contains(Network)) {
                continue;
            }
            if (Now - Network.getScannedAt() > RescanTicks) {
                discard(World, Network);
                continue;
            }
            Network.simulate(World, IntakeBuffers);
            if (Math.floorMod(Now + System.identityHashCode(Network), SaveTicks) == 0) {
                Network.writeBack(World, false);
            }
            Network.emitParticles(World);
        }
    }
}
