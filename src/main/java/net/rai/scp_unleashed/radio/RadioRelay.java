package net.rai.scp_unleashed.radio;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class RadioRelay {
    private RadioRelay() {
    }

    public record Hearer(ServerPlayer Listener, double Strength) {
    }

    public static Vec3 mouth(ServerPlayer Speaker) {
        return Speaker.getEyePosition();
    }

    public static List<Hearer> hearers(ServerPlayer Sender, int Channel) {
        ServerLevel World = Sender.serverLevel();
        RadioNetwork.Ranges Limits = VoiceConfig.ranges();
        return hearers(World, Sender, mouth(Sender), Channel, Limits, RadioTowerData.nodes(World, Limits));
    }

    public static List<Hearer> hearers(ServerLevel World, @Nullable ServerPlayer Sender, Vec3 From, int Channel, RadioNetwork.Ranges Limits, List<RadioNetwork.Node> Nodes) {
        return collect(World, Sender, Channel, RadioNetwork.from(Limits, Nodes, From));
    }

    public static List<Hearer> fromTower(ServerLevel World, RadioTowerData.Tower Tower, RadioNetwork.Ranges Limits, List<RadioNetwork.Node> Nodes) {
        List<RadioNetwork.Node> Including = new ArrayList<>(Nodes);
        if (Including.stream().noneMatch(Each -> Each.Pos().equals(Tower.Pos()))) {
            Including.add(RadioNetwork.node(Limits, Tower.Pos(), Math.max(1, Tower.Masts())));
        }
        return collect(World, null, Tower.Channel(), RadioNetwork.fromTower(Limits, Including, Tower.Pos()));
    }

    private static List<Hearer> collect(ServerLevel World, @Nullable ServerPlayer Sender, int Channel, RadioNetwork.Reach Reach) {
        List<Hearer> Found = new ArrayList<>();
        for (ServerPlayer Each : World.players()) {
            if (Each == Sender || !RadioCarry.hears(Each, Channel)) {
                continue;
            }
            double Strength = Reach.to(mouth(Each));
            if (Strength >= 0.0) {
                Found.add(new Hearer(Each, Strength));
            }
        }
        return Found;
    }
}
