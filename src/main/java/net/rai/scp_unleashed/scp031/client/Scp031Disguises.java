package net.rai.scp_unleashed.scp031.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.scp031.Scp031Mimic;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp031Disguises {
    public static final ResourceLocation FallbackSkin = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp031/guise.png");
    public static final int MaxRemembered = 512;
    public static final Disguise Fallback = new Disguise(null, FallbackSkin, true, null);
    private static final Map<UUID, UUID> Chosen = new HashMap<>();

    private Scp031Disguises() {
    }

    public record Disguise(@Nullable UUID Source, ResourceLocation Texture, boolean Slim, @Nullable Component Name) {
        public boolean copiesPlayer() {
            return Source != null;
        }
    }

    public static double distanceFromCamera(Scp031Mimic Mimic) {
        Camera View = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 Eye = View.getPosition();
        return Eye.distanceTo(Mimic.position());
    }

    public static boolean isNear(Scp031Mimic Mimic) {
        boolean Near = Mimic.seenNear(distanceFromCamera(Mimic));
        return Near && !AntiMemetic.isProtected(Minecraft.getInstance().player);
    }

    @Nullable
    private static UUID viewer() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player != null) {
            return Client.player.getUUID();
        }
        return Client.getUser().getProfileId();
    }

    public static Disguise of(Scp031Mimic Mimic) {
        ClientPacketListener Connection = Minecraft.getInstance().getConnection();
        UUID Viewer = viewer();
        if (Connection == null || Viewer == null) {
            return Fallback;
        }
        UUID Current = Chosen.get(Mimic.getUUID());
        if (Current != null && !Current.equals(Viewer)) {
            PlayerInfo Still = Connection.getPlayerInfo(Current);
            if (Still != null && Connection.getListedOnlinePlayers().contains(Still)) {
                return from(Still);
            }
        }
        PlayerInfo Best = null;
        long BestScore = Long.MIN_VALUE;
        for (PlayerInfo Info : Connection.getListedOnlinePlayers()) {
            UUID Id = Info.getProfile().getId();
            if (Id == null || Id.equals(Viewer)) {
                continue;
            }
            long Score = mix(Mimic.getUUID(), Viewer, Id);
            if (Best == null || Score > BestScore) {
                Best = Info;
                BestScore = Score;
            }
        }
        if (Best == null) {
            Chosen.remove(Mimic.getUUID());
            return Fallback;
        }
        if (Chosen.size() > MaxRemembered) {
            Chosen.clear();
        }
        Chosen.put(Mimic.getUUID(), Best.getProfile().getId());
        return from(Best);
    }

    private static Disguise from(PlayerInfo Info) {
        PlayerSkin Skin = Info.getSkin();
        Component Name = PlayerTeam.formatNameForTeam(Info.getTeam(), Component.literal(Info.getProfile().getName()));
        return new Disguise(Info.getProfile().getId(), Skin.texture(), Skin.model() == PlayerSkin.Model.SLIM, Name);
    }

    private static long mix(UUID Mimic, UUID Viewer, UUID Candidate) {
        long Value = Mimic.getMostSignificantBits() * 0x9E3779B97F4A7C15L ^ Mimic.getLeastSignificantBits();
        Value = scramble(Value ^ Viewer.getMostSignificantBits() * 0xC2B2AE3D27D4EB4FL ^ Viewer.getLeastSignificantBits());
        Value = scramble(Value ^ Candidate.getMostSignificantBits() * 0x165667B19E3779F9L ^ Candidate.getLeastSignificantBits());
        return scramble(Value);
    }

    private static long scramble(long Value) {
        long Mixed = (Value ^ (Value >>> 30)) * 0xBF58476D1CE4E5B9L;
        Mixed = (Mixed ^ (Mixed >>> 27)) * 0x94D049BB133111EBL;
        return Mixed ^ (Mixed >>> 31);
    }

    public static void reset() {
        Chosen.clear();
    }
}
