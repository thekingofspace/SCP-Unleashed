package net.rai.scp_unleashed.guns.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.Stat;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.rai.scp_unleashed.guns.GunSoundLengths;
import net.rai.scp_unleashed.guns.GunSoundMixer;
import net.rai.scp_unleashed.guns.GunVoiceBudget;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

public class GunPerfListener extends ServerPlayer {
    private int Sounds;
    private int Payloads;
    private int Particles;
    private int Others;
    private boolean Listed;
    private final PriorityQueue<Long> Ends = new PriorityQueue<>();
    private final PriorityQueue<Long> Loose = new PriorityQueue<>();
    private final GunVoiceBudget<Long> Budget = new GunVoiceBudget<>(GunVoiceBudget.ClientLimit);
    private final List<Long> Stolen = new ArrayList<>();
    private long Handles;
    private long Busy;
    private long Kept;
    private int Samples;
    private int Peak;
    private int KeptPeak;
    private int Refused;
    private int Cut;
    private final List<ClientboundSoundPacket> Heard = new ArrayList<>();

    public GunPerfListener(ServerLevel World, String Name, Vec3 Spot) {
        super(World.getServer(), World, new GameProfile(UUID.randomUUID(), Name), ClientInformation.createDefault());
        new Link(World.getServer(), this);
        setPos(Spot.x, Spot.y, Spot.z);
    }

    @Override
    public boolean isSpectator() {
        return true;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    @Override
    public void awardStat(Stat<?> Counted, int Amount) {
    }

    public int sounds() {
        return Sounds;
    }

    public int payloads() {
        return Payloads;
    }

    public int particles() {
        return Particles;
    }

    public int others() {
        return Others;
    }

    public void reset() {
        Sounds = 0;
        Payloads = 0;
        Particles = 0;
        Others = 0;
        Busy = 0L;
        Kept = 0L;
        Samples = 0;
        Peak = 0;
        KeptPeak = 0;
        Refused = 0;
        Cut = 0;
        Heard.clear();
    }

    public void sample() {
        long Now = server.getTickCount();
        while (!Ends.isEmpty() && Ends.peek() <= Now) {
            Ends.poll();
        }
        while (!Loose.isEmpty() && Loose.peek() <= Now) {
            Loose.poll();
        }
        int Voices = Budget.size(Now) + Loose.size();
        Busy += Ends.size();
        Kept += Voices;
        Samples++;
        Peak = Math.max(Peak, Ends.size());
        KeptPeak = Math.max(KeptPeak, Voices);
    }

    public double kept() {
        return Samples == 0 ? 0.0 : (double) Kept / Samples;
    }

    public int keptPeak() {
        return KeptPeak;
    }

    public int refused() {
        return Refused;
    }

    public int cut() {
        return Cut;
    }

    public List<ClientboundSoundPacket> heard() {
        return Heard;
    }

    public int managed() {
        int Count = 0;
        for (ClientboundSoundPacket Played : Heard) {
            if (GunVoiceBudget.managed(Played.getSound().value().getLocation())) {
                Count++;
            }
        }
        return Count;
    }

    private void hear(ClientboundSoundPacket Played) {
        Heard.add(Played);
        long Now = server.getTickCount();
        int Ticks = GunSoundLengths.ticks(Played.getSound().value().getLocation(), Played.getPitch());
        Ends.add(Now + Ticks);
        if (!GunVoiceBudget.managed(Played.getSound().value().getLocation())) {
            Loose.add(Now + Ticks);
            return;
        }
        float Volume = Played.getVolume();
        double Reach = Math.max(1.0, Played.getSound().value().getRange(Volume));
        double Dx = Played.getX() - getX();
        double Dy = Played.getY() - getEyeY();
        double Dz = Played.getZ() - getZ();
        float Gain = (float) (Volume * Math.max(0.0, 1.0 - Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz) / Reach));
        Stolen.clear();
        if (!Budget.offer(Handles++, Gain, Now, Ticks, Stolen)) {
            Refused++;
        }
        Cut += Stolen.size();
    }

    public double channels() {
        return Samples == 0 ? 0.0 : (double) Busy / Samples;
    }

    public int peak() {
        return Peak;
    }

    private void count(Packet<?> Sent) {
        if (Sent instanceof ClientboundBundlePacket Bundle) {
            for (Packet<?> Inner : Bundle.subPackets()) {
                count(Inner);
            }
        } else if (Sent instanceof ClientboundSoundPacket Played) {
            Sounds++;
            hear(Played);
        } else if (Sent instanceof ClientboundSoundEntityPacket) {
            Sounds++;
        } else if (Sent instanceof ClientboundCustomPayloadPacket) {
            Payloads++;
        } else if (Sent instanceof ClientboundLevelParticlesPacket) {
            Particles++;
        } else {
            Others++;
        }
    }

    @SuppressWarnings("unchecked")
    public void enlist(boolean Joining) {
        if (Joining == Listed) {
            return;
        }
        PlayerList Roster = server.getPlayerList();
        Field Players = ObfuscationReflectionHelper.findField(PlayerList.class, "players");
        Field ByUuid = ObfuscationReflectionHelper.findField(PlayerList.class, "playersByUUID");
        try {
            List<ServerPlayer> Members = (List<ServerPlayer>) Players.get(Roster);
            Map<UUID, ServerPlayer> Keyed = (Map<UUID, ServerPlayer>) ByUuid.get(Roster);
            if (Joining) {
                Members.add(this);
                Keyed.put(getUUID(), this);
            } else {
                Members.remove(this);
                Keyed.remove(getUUID(), this);
                GunSoundMixer.forget(getUUID());
            }
            Listed = Joining;
        } catch (IllegalAccessException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    public static final class Link extends ServerGamePacketListenerImpl {
        private final GunPerfListener Owner;

        public Link(MinecraftServer Server, GunPerfListener User) {
            super(Server, Quiet.make(), User, CommonListenerCookie.createInitial(User.getGameProfile(), false));
            this.Owner = User;
        }

        @Override
        public void send(Packet<?> Sent) {
            if (Owner != null) {
                Owner.count(Sent);
            }
        }

        @Override
        public void send(Packet<?> Sent, @Nullable PacketSendListener Listener) {
            if (Owner != null) {
                Owner.count(Sent);
            }
        }

        @Override
        public void tick() {
        }

        @Override
        public void disconnect(DisconnectionDetails Details) {
        }
    }

    private static final class Quiet extends Connection {
        private Quiet() {
            super(PacketFlow.SERVERBOUND);
        }

        private static Quiet make() {
            Quiet Made = new Quiet();
            new EmbeddedChannel(Made);
            return Made;
        }

        @Override
        public void setListenerForServerboundHandshake(PacketListener Listener) {
        }
    }
}
