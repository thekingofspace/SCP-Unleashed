package net.rai.scp_unleashed.notice;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.stranger.StrangerVariant;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class Notices {
    public static final int Rounding = 10;
    public static final double WitnessRange = 64.0;
    public static final int WitnessDuration = 120;
    public static final String StrangerFallback = "A stranger is watching you";
    public static final String StrangerOtherFallback = "A stranger is watching %s";

    private Notices() {
    }

    public static void send(ServerPlayer Player, Notice Shown) {
        if (Player.hasDisconnected() || Player.connection == null) {
            return;
        }
        if (Player.connection.hasChannel(NoticePayload.PayloadType)) {
            PacketDistributor.sendToPlayer(Player, new NoticePayload(Shown));
        } else {
            Player.displayClientMessage(Shown.Title(), true);
        }
    }

    public static void sendTo(Collection<ServerPlayer> Players, Notice Shown) {
        for (ServerPlayer Player : List.copyOf(Players)) {
            send(Player, Shown);
        }
    }

    public static void sendToAll(MinecraftServer Server, Notice Shown) {
        sendTo(Server.getPlayerList().getPlayers(), Shown);
    }

    public static void sendToDimension(ServerLevel World, Notice Shown) {
        sendTo(World.players(), Shown);
    }

    public static void sendNear(ServerLevel World, Vec3 Centre, double Range, Notice Shown) {
        for (ServerPlayer Player : List.copyOf(World.players())) {
            if (Player.distanceToSqr(Centre) <= Range * Range) {
                send(Player, Shown);
            }
        }
    }

    public static void announce(MinecraftServer Server, Component Title, @Nullable Component Detail) {
        sendToAll(Server, Notice.alert(Title).withDetail(Detail));
    }

    public static int rounded(int Value) {
        return Math.round(Value / (float) Rounding) * Rounding;
    }

    public static Component where(Level World, BlockPos Pos, String Key) {
        int X = rounded(Pos.getX());
        int Z = rounded(Pos.getZ());
        if (World.dimension().equals(Level.OVERWORLD)) {
            return Component.translatable(Key, X, Z);
        }
        return Component.translatable(Key + ".dimension", X, Z, LeadPlanner.dimensionName(LeadPlanner.dimensionOf(World)));
    }

    @Nullable
    public static AnomalyDocument document(@Nullable ResourceLocation Id) {
        return Id == null ? null : AnomalyDocuments.server().get(Id);
    }

    public static Component designation(AnomalyDocument Document) {
        return Component.translatable("notice.scp_unleashed.anomaly.title", Document.designation().copy().withStyle(ChatFormatting.BOLD), Document.title());
    }

    public static Notice surfaced(AnomalyDocument Document, Level World, BlockPos Pos, boolean Manifested) {
        return Notice.anomaly(Document.Classification(), designation(Document))
                .withDetail(where(World, Pos, Manifested ? "notice.scp_unleashed.anomaly.manifested" : "notice.scp_unleashed.anomaly.surfaced"));
    }

    public static Notice forCase(@Nullable ResourceLocation DocumentId, Component Text, boolean NameCase, NoticePriority Priority) {
        AnomalyDocument Document = document(DocumentId);
        if (Document == null) {
            return Notice.lead(Text).withPriority(Priority);
        }
        Notice Shown = Notice.anomaly(Document.Classification(), Text).withPriority(Priority);
        return NameCase ? Shown.withDetail(Document.designation()) : Shown;
    }

    public static void lead(ServerPlayer Reader, @Nullable ResourceLocation DocumentId, Component Text, boolean NameCase) {
        send(Reader, forCase(DocumentId, Text, NameCase, NoticePriority.Lead));
    }

    public static void caseInfo(ServerPlayer Reader, @Nullable ResourceLocation DocumentId, Component Text, boolean NameCase) {
        send(Reader, forCase(DocumentId, Text, NameCase, NoticePriority.Info));
    }

    public static Notice falling(ServerLevel World, @Nullable ResourceLocation DocumentId, BlockPos Ground, Component Text) {
        AnomalyDocument Document = document(DocumentId);
        Notice Shown = Document == null ? Notice.alert(Text).withPriority(NoticePriority.AnomalySpawn) : Notice.anomaly(Document.Classification(), Text);
        return Shown.withDetail(where(World, Ground, "notice.scp_unleashed.near"));
    }

    public static void sendFalling(ServerLevel World, @Nullable ResourceLocation DocumentId, BlockPos Ground, Component Text) {
        sendToDimension(World, falling(World, DocumentId, Ground, Text));
    }

    public static Notice strangerWatching(StrangerVariant Variant) {
        Component Title = Component.translatableWithFallback("notice.scp_unleashed.stranger." + Variant.Id(), StrangerFallback);
        return Notice.stranger(Variant.Id(), Title, Variant.GlintColour()).withDetail(Component.translatable("notice.scp_unleashed.stranger.close"));
    }

    public static Notice strangerWatchingOther(StrangerVariant Variant, Component Reader) {
        Component Title = Component.translatableWithFallback("notice.scp_unleashed.stranger." + Variant.Id() + ".other", StrangerOtherFallback, Reader);
        return Notice.stranger(Variant.Id(), Title, Variant.GlintColour()).withDuration(WitnessDuration);
    }

    public static void strangerArrived(ServerPlayer Reader, StrangerVariant Variant, BlockPos Spot, Collection<UUID> Former) {
        send(Reader, strangerWatching(Variant));
        Notice Witnessed = strangerWatchingOther(Variant, Reader.getDisplayName());
        Vec3 Centre = Vec3.atCenterOf(Spot);
        for (ServerPlayer Other : List.copyOf(Reader.server.getPlayerList().getPlayers())) {
            if (Other == Reader || Other.isSpectator()) {
                continue;
            }
            boolean Near = Other.serverLevel() == Reader.serverLevel() && Other.distanceToSqr(Centre) <= WitnessRange * WitnessRange;
            if (Near || Former.contains(Other.getUUID())) {
                send(Other, Witnessed);
            }
        }
    }
}
