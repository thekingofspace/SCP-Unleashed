package net.rai.scp_unleashed.scp079;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.entity.BreakerBlockEntity;
import net.rai.scp_unleashed.block.entity.NetworkRouterBlockEntity;
import net.rai.scp_unleashed.block.entity.RedstoneReceiverBlockEntity;
import net.rai.scp_unleashed.speaker.SoundConsoleBlockEntity;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.WireDevice;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Scp079Grid {
    public static final int MaxNetworks = 12;
    public static final int MaxDevices = 256;

    private Scp079Grid() {
    }

    public static Reach reach(ServerLevel World, Scp079BlockEntity Computer) {
        WireNetworkManager Manager = WireNetworkManager.get(World);
        BlockPos Origin = Computer.getBlockPos();
        Set<WireNetwork> Networks = new LinkedHashSet<>();
        Map<BlockPos, WireDevice> Devices = new LinkedHashMap<>();
        Set<BlockPos> Breakers = new LinkedHashSet<>();
        Set<BlockPos> Visited = new HashSet<>();
        ArrayDeque<BlockPos> Hosts = new ArrayDeque<>();
        Hosts.add(Origin);
        Visited.add(Origin);
        while (!Hosts.isEmpty() && Networks.size() < MaxNetworks) {
            BlockPos Host = Hosts.poll();
            List<BlockPos> Cells = Host.equals(Origin) ? List.of(Origin, Scp079Block.cellPos(Origin, Computer.facing(), Scp079Block.ScreenPart)) : List.of(Host);
            for (BlockPos OutletPos : outletsOn(World, Manager, Host, Cells)) {
                WireNetwork Network = Manager.networkAt(World, OutletPos);
                if (Network == null || !Networks.add(Network)) {
                    continue;
                }
                Network.findDevices(World).forEach((Pos, Device) -> {
                    if (Devices.size() < MaxDevices && !Pos.equals(Origin)) {
                        Devices.putIfAbsent(Pos, Device);
                    }
                    if (Device instanceof NetworkRouterBlockEntity && Visited.add(Pos)) {
                        Hosts.add(Pos);
                    }
                });
                for (WireNetwork.Bridge Link : Network.getBridges()) {
                    Breakers.add(Link.Switch());
                }
            }
        }
        return new Reach(Networks, Devices, Breakers);
    }

    private static List<BlockPos> outletsOn(ServerLevel World, WireNetworkManager Manager, BlockPos Host, List<BlockPos> Cells) {
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Cell : Cells) {
            for (Direction Side : Direction.values()) {
                BlockPos Next = Cell.relative(Side);
                if (!World.isLoaded(Next)) {
                    continue;
                }
                BlockState State = World.getBlockState(Next);
                if (State.getBlock() instanceof WireConnector Connector && Manager.canonicalHost(World, Connector.hostPos(State, Next)).equals(Host)) {
                    Found.add(Next.immutable());
                }
            }
        }
        return Found;
    }

    public record Reach(Set<WireNetwork> Networks, Map<BlockPos, WireDevice> Devices, Set<BlockPos> Breakers) {
        public boolean isEmpty() {
            return Devices.isEmpty() && Breakers.isEmpty();
        }

        public int deviceCount() {
            return Devices.size();
        }

        public List<SpeakerBlockEntity> speakers() {
            List<SpeakerBlockEntity> Found = new ArrayList<>();
            for (WireDevice Device : Devices.values()) {
                if (Device instanceof SpeakerBlockEntity Speaker && !Speaker.isRemoved()) {
                    Found.add(Speaker);
                }
            }
            return Found;
        }

        public List<RedstoneReceiverBlockEntity> receivers() {
            List<RedstoneReceiverBlockEntity> Found = new ArrayList<>();
            for (WireDevice Device : Devices.values()) {
                if (Device instanceof RedstoneReceiverBlockEntity Receiver && !Receiver.isRemoved()) {
                    Found.add(Receiver);
                }
            }
            return Found;
        }

        public List<SoundConsoleBlockEntity> consoles() {
            List<SoundConsoleBlockEntity> Found = new ArrayList<>();
            for (WireDevice Device : Devices.values()) {
                if (Device instanceof SoundConsoleBlockEntity Console && !Console.isRemoved()) {
                    Found.add(Console);
                }
            }
            return Found;
        }

        public List<BreakerBlockEntity> breakers(ServerLevel World) {
            List<BreakerBlockEntity> Found = new ArrayList<>();
            for (BlockPos Pos : Breakers) {
                if (World.isLoaded(Pos) && World.getBlockEntity(Pos) instanceof BreakerBlockEntity Breaker) {
                    Found.add(Breaker);
                }
            }
            return Found;
        }
    }
}
