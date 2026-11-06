package net.rai.scp_unleashed.archive;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.notice.Notices;
import net.rai.scp_unleashed.discord.DiscordNotices;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AnomalySpawners {
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<ResourceLocation, Spawner> Spawners = new ConcurrentHashMap<>();

    private AnomalySpawners() {
    }

    public static void register(ResourceLocation DocumentId, Spawner Hook) {
        Spawner Previous = Spawners.put(DocumentId, Hook);
        if (Previous != null) {
            Log.warn("Anomaly spawner for {} was replaced", DocumentId);
        }
    }

    public static Optional<Spawner> spawnerFor(ResourceLocation DocumentId) {
        return Optional.ofNullable(Spawners.get(DocumentId));
    }

    public static Outcome discover(Discovery Found) {
        Spawner Hook = Spawners.get(Found.DocumentId());
        if (Hook == null) {
            return Outcome.Unsurfaced;
        }
        if (!AnomalyRealm.allows(Found.World(), Found.DocumentId())) {
            return Outcome.Failed;
        }
        boolean Spawned;
        try {
            Spawned = Hook.spawn(Found);
        } catch (RuntimeException Problem) {
            Log.error("Anomaly spawner for {} failed", Found.DocumentId(), Problem);
            return Outcome.Failed;
        }
        if (!Spawned) {
            return Outcome.Failed;
        }
        announce(Found);
        DiscordNotices.anomalyArrived(Found);
        return Outcome.Spawned;
    }

    private static void announce(Discovery Found) {
        AnomalyDocument Document = Found.Document();
        BlockPos Where = Found.Pos();
        MutableComponent Prefix = Component.translatable("archive.scp_unleashed.chat.prefix").withStyle(ChatFormatting.DARK_GRAY);
        Component Designation = Document.designation().copy().withStyle(Look -> Look.withColor(Document.Classification().getColour() & 0xFFFFFF).withBold(true));
        String Key = Found.Manifested() ? "archive.scp_unleashed.chat.manifested" : "archive.scp_unleashed.chat.located";
        Component Line = Component.translatable(Key, Designation, Document.title().copy().withStyle(ChatFormatting.WHITE),
                Where.getX(), Where.getY(), Where.getZ()).withStyle(ChatFormatting.GRAY);
        Found.Investigator().sendSystemMessage(Prefix.append(" ").append(Line));
        Notices.sendToAll(Found.World().getServer(), Notices.surfaced(Document, Found.World(), Where, Found.Manifested()));
        LeadPlanner.playTo(Found.Investigator(), ArchiveRegistry.DiscoverySting, LeadPlanner.towards(Found.Investigator(), Where.getX() + 0.5, Where.getZ() + 0.5, 4.0), 1.2f, 1.0f);
    }

    public enum Outcome {
        Spawned,
        Unsurfaced,
        Failed
    }

    @FunctionalInterface
    public interface Spawner {
        boolean spawn(Discovery Found);
    }

    public record Discovery(ServerLevel World, BlockPos Pos, ServerPlayer Investigator, ResourceLocation DocumentId, AnomalyDocument Document, boolean Manifested) {
    }
}
