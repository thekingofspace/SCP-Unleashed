package net.rai.scp_unleashed.scp179;

import net.minecraft.server.MinecraftServer;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteNetwork;

import java.util.ArrayList;
import java.util.List;

public final class SatelliteLink {
    public static final int MaxShown = 8;
    public static final int MaxName = 24;

    private SatelliteLink() {
    }

    public static boolean anyDeployed(MinecraftServer Server) {
        return SatelliteNetwork.get(Server).anyDeployed();
    }

    public static List<String> names(MinecraftServer Server) {
        List<String> Found = new ArrayList<>();
        for (SatelliteNetwork.Satellite Entry : SatelliteNetwork.get(Server).all()) {
            if (Found.size() >= MaxShown) {
                break;
            }
            String Name = Entry.Name() == null || Entry.Name().isBlank() ? "SAT-" + (Found.size() + 1) : Entry.Name();
            Found.add(Name.length() > MaxName ? Name.substring(0, MaxName) : Name);
        }
        return Found;
    }

    public static int count(MinecraftServer Server) {
        return SatelliteNetwork.get(Server).all().size();
    }
}
