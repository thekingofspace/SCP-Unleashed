package net.rai.scp_unleashed.mining.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.mining.MiningRegistry;
import net.rai.scp_unleashed.mining.ServerAmbience;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class ServerAmbienceClient {
    public static final int RefreshTicks = 10;
    public static final double Range = 16.0;
    public static final double ClusterRadius = 6.0;
    public static final double KeepRadius = 8.0;
    public static final int MaxClusters = 2;
    public static final int MaxLoops = 3;
    public static final int PhaseOffset = 48;
    public static final float CrowdStep = 0.06f;
    public static final int CrowdCap = 5;
    public static final float IdleVolume = 0.3f;
    public static final float LoadVolume = 0.2f;
    public static final float BusyVolume = 0.55f;
    public static final float BusyPitch = 1.03f;

    private static final List<Rack> Racks = new ArrayList<>();
    @Nullable
    private static ClientLevel LastWorld;
    private static int Ticks;

    private ServerAmbienceClient() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        LocalPlayer User = Game.player;
        if (World == null || User == null) {
            silence();
            return;
        }
        if (World != LastWorld) {
            silence();
            LastWorld = World;
        }
        SoundManager Sounds = Game.getSoundManager();
        Ticks++;
        if (Ticks % RefreshTicks == 0) {
            refresh(World, User.position(), Sounds);
        }
        for (Rack Each : Racks) {
            if (Each.LateStart > 0 && --Each.LateStart == 0 && !Each.Early.isFading()) {
                Sounds.play(Each.Late);
            }
        }
        Racks.removeIf(Rack::isDone);
    }

    private static void silence() {
        for (Rack Each : Racks) {
            Each.fade();
        }
        Racks.clear();
        ServerAmbience.forget();
        LastWorld = null;
    }

    private static void refresh(ClientLevel World, Vec3 Ear, SoundManager Sounds) {
        Racks.removeIf(Each -> !Sounds.isActive(Each.Early) && !Sounds.isActive(Each.Late));
        List<Member> Active = new ArrayList<>();
        Iterator<Map.Entry<BlockPos, ServerAmbience.Sighting>> Each = ServerAmbience.seen().entrySet().iterator();
        while (Each.hasNext()) {
            Map.Entry<BlockPos, ServerAmbience.Sighting> Entry = Each.next();
            Vec3 Centre = Vec3.atCenterOf(Entry.getKey());
            if (Centre.distanceToSqr(Ear) > Range * Range || !World.isLoaded(Entry.getKey())) {
                Each.remove();
                continue;
            }
            ServerAmbience.Mode Shown = ServerAmbience.modeOf(World.getBlockState(Entry.getKey()), Entry.getValue());
            if (Shown == ServerAmbience.Mode.Off) {
                Each.remove();
                continue;
            }
            Active.add(new Member(Centre, Centre.distanceToSqr(Ear), Shown, ServerAmbience.loadOf(Shown, Entry.getValue())));
        }
        Active.sort(Comparator.comparingDouble(Member::Distance));
        List<Cluster> Clusters = new ArrayList<>();
        for (Member Found : Active) {
            Cluster Home = null;
            for (Cluster Candidate : Clusters) {
                if (Candidate.Anchor.distanceToSqr(Found.Centre()) <= ClusterRadius * ClusterRadius) {
                    Home = Candidate;
                    break;
                }
            }
            if (Home == null) {
                Clusters.add(new Cluster(Found));
            } else {
                Home.add(Found);
            }
        }
        List<Rack> Claimed = new ArrayList<>();
        for (int Index = 0; Index < Clusters.size() && Index < MaxClusters; Index++) {
            Cluster Group = Clusters.get(Index);
            Rack Match = null;
            for (Rack Candidate : Racks) {
                if (!Claimed.contains(Candidate) && !Candidate.Early.isFading() && Candidate.Early.target().distanceToSqr(Group.Anchor) <= KeepRadius * KeepRadius) {
                    Match = Candidate;
                    break;
                }
            }
            if (Match == null) {
                if (Racks.size() >= MaxLoops) {
                    continue;
                }
                Match = new Rack(Group.Anchor);
                Racks.add(Match);
                Sounds.play(Match.Early);
            }
            Claimed.add(Match);
            Match.aim(Group);
        }
        for (Rack Left : Racks) {
            if (!Claimed.contains(Left)) {
                Left.fade();
            }
        }
    }

    private static final class Rack {
        private final ServerLoopSound Early;
        private final ServerLoopSound Late;
        private int LateStart = PhaseOffset;

        private Rack(Vec3 Where) {
            Early = new ServerLoopSound(MiningRegistry.Hum.get(), Where);
            Late = new ServerLoopSound(MiningRegistry.Hum.get(), Where);
        }

        private void aim(Cluster Group) {
            float Crowd = 1.0f + CrowdStep * (Math.min(CrowdCap, Group.Count) - 1);
            boolean Busy = Group.Shown == ServerAmbience.Mode.Busy;
            float Volume = (Busy ? BusyVolume : IdleVolume + LoadVolume * Group.Load) * Crowd;
            float Pitch = Busy ? BusyPitch : 1.0f;
            Early.aim(Group.Anchor, Volume, Pitch);
            Late.aim(Group.Anchor, Volume, Pitch);
        }

        private void fade() {
            Early.fade();
            Late.fade();
        }

        private boolean isDone() {
            return Early.isStopped() && (Late.isStopped() || LateStart > 0);
        }
    }

    private static final class Cluster {
        private final Vec3 Anchor;
        private ServerAmbience.Mode Shown;
        private float Load;
        private int Count;

        private Cluster(Member First) {
            Anchor = First.Centre();
            Shown = First.Shown();
            Load = First.Load();
            Count = 1;
        }

        private void add(Member Found) {
            Count++;
            if (Found.Shown().ordinal() > Shown.ordinal()) {
                Shown = Found.Shown();
            }
            Load = Math.max(Load, Found.Load());
        }
    }

    private record Member(Vec3 Centre, double Distance, ServerAmbience.Mode Shown, float Load) {
    }
}
