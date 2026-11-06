package net.rai.scp_unleashed.nethub.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.nethub.HubStatus;
import net.rai.scp_unleashed.nethub.NetHubRegistry;
import net.rai.scp_unleashed.nethub.NetworkHubBlock;
import net.rai.scp_unleashed.nethub.NetworkHubBlockEntity;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class NetHubClientHooks {
    public static final double HearRange = 24.0;
    public static final int QuietChance = 46;
    public static final int BusyChance = 12;
    public static final int BusyTicks = 60;
    public static final int TickChance = 3;
    public static final int RetryTicks = 40;
    private static final Map<NetworkHubBlockEntity, Ambience> Sounds = new WeakHashMap<>();

    private NetHubClientHooks() {
    }

    public static void open(BlockPos Origin) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.level != null && Game.level.getBlockEntity(Origin) instanceof NetworkHubBlockEntity) {
            Game.setScreen(new NetworkHubScreen(Origin.immutable()));
        }
    }

    public static void tick(NetworkHubBlockEntity Hub, BlockState State) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.player == null || Game.level == null || Hub.getLevel() != Game.level || !State.hasProperty(NetworkHubBlock.Status)) {
            return;
        }
        HubStatus Shown = State.getValue(NetworkHubBlock.Status);
        Vec3 Centre = Hub.getCentre();
        boolean Near = Game.player.distanceToSqr(Centre) < HearRange * HearRange;
        Ambience Held = Sounds.computeIfAbsent(Hub, Key -> new Ambience());
        long Now = Game.level.getGameTime();
        if (Shown.isPowered() && Near && !Held.playing(Now)) {
            Held.StartedAt = Now;
            Held.Loops = List.of(new NetHubLoopSound(Hub, NetHubRegistry.Hum.get()), new NetHubLoopSound(Hub, NetHubRegistry.HumDrone.get()));
            Held.Loops.forEach(Game.getSoundManager()::play);
        }
        long Traffic = Hub.getRelayed() + Hub.getDelivered();
        if (Traffic != Held.Traffic) {
            Held.Traffic = Traffic;
            Held.BusyUntil = Now + BusyTicks;
        }
        RandomSource Random = Game.level.getRandom();
        int Chance = Now < Held.BusyUntil ? BusyChance : QuietChance;
        if (Shown == HubStatus.Bridging && Near && Random.nextInt(Chance) == 0) {
            double Rise = 2.5 + Random.nextDouble() * 5.0;
            double X = Hub.getBlockPos().getX() + 0.5 + (Centre.x - Hub.getBlockPos().getX() - 0.5) * (0.6 + Random.nextDouble() * 0.8);
            double Z = Hub.getBlockPos().getZ() + 0.5 + (Centre.z - Hub.getBlockPos().getZ() - 0.5) * (0.6 + Random.nextDouble() * 0.8);
            double Y = Hub.getBlockPos().getY() + Rise;
            Game.level.playLocalSound(X, Y, Z, NetHubRegistry.Chirp.get(), SoundSource.BLOCKS, 1.0f, 0.94f + Random.nextFloat() * 0.12f, false);
            if (Random.nextInt(TickChance) == 0) {
                Game.level.playLocalSound(X, Y, Z, NetHubRegistry.ChirpTick.get(), SoundSource.BLOCKS, 1.0f, 0.9f + Random.nextFloat() * 0.2f, false);
            }
        }
    }

    public static void removed(NetworkHubBlockEntity Hub) {
        Ambience Held = Sounds.remove(Hub);
        if (Held != null) {
            Held.Loops.forEach(NetHubLoopSound::fadeOut);
        }
    }

    private static final class Ambience {
        private List<NetHubLoopSound> Loops = List.of();
        private long Traffic;
        private long BusyUntil;
        private long StartedAt = Long.MIN_VALUE / 2;

        private boolean playing(long Now) {
            Minecraft Game = Minecraft.getInstance();
            for (NetHubLoopSound Loop : Loops) {
                if (!Loop.isStopped() && (Now - StartedAt < RetryTicks || Game.getSoundManager().isActive(Loop))) {
                    return true;
                }
            }
            return false;
        }
    }
}
