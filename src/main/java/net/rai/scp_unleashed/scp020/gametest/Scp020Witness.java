package net.rai.scp_unleashed.scp020.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.Stat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp020Witness extends FakePlayer {
    private final List<String> Seen = new ArrayList<>();
    private AABB Area = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

    public Scp020Witness(ServerLevel World, String Name) {
        super(World, new GameProfile(UUID.randomUUID(), Name));
        new Recorder(World.getServer(), this);
    }

    @Override
    public void awardStat(Stat<?> Counted, int Amount) {
    }

    public List<String> seen() {
        return List.copyOf(Seen);
    }

    public void forget() {
        Seen.clear();
    }

    public void watch(AABB Watched) {
        Area = Watched;
    }

    private boolean near(double X, double Y, double Z) {
        return Area.contains(X, Y, Z);
    }

    private void note(Packet<?> Sent) {
        if (Sent instanceof ClientboundBundlePacket Bundle) {
            for (Packet<?> Inner : Bundle.subPackets()) {
                note(Inner);
            }
        } else if (Sent instanceof ClientboundLevelParticlesPacket Particles) {
            if (near(Particles.getX(), Particles.getY(), Particles.getZ())) {
                Seen.add("particles " + BuiltInRegistries.PARTICLE_TYPE.getKey(Particles.getParticle().getType()));
            }
        } else if (Sent instanceof ClientboundLevelEventPacket Happened) {
            if (Area.contains(Vec3.atCenterOf(Happened.getPos()))) {
                Seen.add("level event " + Happened.getType() + " " + Happened.getData());
            }
        } else if (Sent instanceof ClientboundSoundPacket Heard) {
            if (near(Heard.getX(), Heard.getY(), Heard.getZ())) {
                Seen.add("sound " + Heard.getSound().value().getLocation());
            }
        } else if (Sent instanceof ClientboundSoundEntityPacket Heard) {
            Entity Source = serverLevel().getEntity(Heard.getId());
            if (Source != null && Area.contains(Source.position())) {
                Seen.add("entity sound " + Heard.getSound().value().getLocation());
            }
        } else if (Sent instanceof ClientboundBlockEventPacket Pulsed) {
            if (Area.contains(Vec3.atCenterOf(Pulsed.getPos()))) {
                Seen.add("block event " + BuiltInRegistries.BLOCK.getKey(Pulsed.getBlock()) + " " + Pulsed.getB0());
            }
        } else if (Sent instanceof ClientboundBlockDestructionPacket Cracked) {
            if (Area.contains(Vec3.atCenterOf(Cracked.getPos()))) {
                Seen.add("block destruction");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void enlist(boolean Joining) {
        PlayerList Roster = server.getPlayerList();
        Field Players = ObfuscationReflectionHelper.findField(PlayerList.class, "players");
        Field ByUuid = ObfuscationReflectionHelper.findField(PlayerList.class, "playersByUUID");
        try {
            List<ServerPlayer> Listed = (List<ServerPlayer>) Players.get(Roster);
            Map<UUID, ServerPlayer> Keyed = (Map<UUID, ServerPlayer>) ByUuid.get(Roster);
            if (Joining) {
                Listed.add(this);
                Keyed.put(getUUID(), this);
            } else {
                Listed.remove(this);
                Keyed.remove(getUUID(), this);
            }
        } catch (IllegalAccessException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    public static final class Recorder extends ServerGamePacketListenerImpl {
        private final Scp020Witness Owner;

        public Recorder(MinecraftServer Server, Scp020Witness User) {
            super(Server, Quiet.make(), User, CommonListenerCookie.createInitial(User.getGameProfile(), false));
            Owner = User;
        }

        @Override
        public void send(Packet<?> Sent) {
            if (Owner != null) {
                Owner.note(Sent);
            }
        }

        @Override
        public void send(Packet<?> Sent, @Nullable PacketSendListener Listener) {
            if (Owner != null) {
                Owner.note(Sent);
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
