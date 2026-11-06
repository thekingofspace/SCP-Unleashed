package net.rai.scp_unleashed.notice;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.stranger.StrangerVariants;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class NoticeCommands {
    public static final ResourceLocation SampleDocument = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_173");
    public static final ResourceLocation MeteorDocument = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_002");
    private static final SuggestionProvider<CommandSourceStack> Kinds = (Context, Builder) -> SharedSuggestionProvider.suggest(kinds(), Builder);

    private NoticeCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("notice")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("test")
                                .then(Commands.argument("kind", StringArgumentType.word())
                                        .suggests(Kinds)
                                        .executes(Context -> test(Context.getSource(), StringArgumentType.getString(Context, "kind"), List.of(Context.getSource().getPlayerOrException())))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(Context -> test(Context.getSource(), StringArgumentType.getString(Context, "kind"), EntityArgument.getPlayers(Context, "targets"))))))));
    }

    public static List<String> kinds() {
        List<String> Names = new ArrayList<>(List.of("info", "lead", "alert", "falling", "witness", "stack"));
        for (DocumentClass Classification : DocumentClass.values()) {
            Names.add("anomaly." + Classification.getSerializedName());
        }
        StrangerVariants.ids().stream().sorted().forEach(Variant -> Names.add("stranger." + Variant));
        return Names;
    }

    private static int test(CommandSourceStack Source, String Kind, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            List<Notice> Built = build(Kind, Target);
            if (Built.isEmpty()) {
                Source.sendFailure(Component.translatable("commands.scp_unleashed.notice.unknown", Kind));
                return 0;
            }
            for (Notice Shown : Built) {
                Notices.send(Target, Shown);
            }
            Count++;
        }
        int Sent = Count;
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.notice.sent", Kind, Sent), false);
        return Count;
    }

    private static List<Notice> build(String Kind, ServerPlayer Target) {
        ServerLevel World = Target.serverLevel();
        BlockPos Pos = Target.blockPosition().offset(37, 0, -112);
        ResourceLocation Sample = sample();
        if (Kind.startsWith("anomaly.")) {
            DocumentClass Classification = classNamed(Kind.substring("anomaly.".length()));
            if (Classification == null) {
                return List.of();
            }
            AnomalyDocument Found = firstOfClass(Classification);
            if (Found != null) {
                return List.of(Notices.surfaced(Found, World, Pos, true));
            }
            return List.of(Notice.anomaly(Classification, Component.translatable("notice.scp_unleashed.test.anomaly", Classification.getName()))
                    .withDetail(Notices.where(World, Pos, "notice.scp_unleashed.anomaly.surfaced")));
        }
        if (Kind.startsWith("stranger.")) {
            String Variant = Kind.substring("stranger.".length());
            return StrangerVariants.exists(Variant) ? List.of(Notices.strangerWatching(StrangerVariants.get(Variant))) : List.of();
        }
        return switch (Kind) {
            case "info" -> List.of(Notices.forCase(Sample, Component.translatable("archive.scp_unleashed.lead.unsurfaced"), true, NoticePriority.Info));
            case "lead" -> List.of(Notices.forCase(Sample, Component.translatable("archive.scp_unleashed.lead.site_near"), true, NoticePriority.Lead));
            case "alert" -> List.of(Notice.alert(Component.translatable("notice.scp_unleashed.test.alert")).withDetail(Notices.where(World, Pos, "notice.scp_unleashed.near")));
            case "falling" -> List.of(Notices.falling(World, MeteorDocument, Pos, Component.translatable("scp002.scp_unleashed.lead.falling")));
            case "witness" -> List.of(Notices.strangerWatchingOther(StrangerVariants.Foundation, Target.getDisplayName()));
            case "stack" -> stack(Target, World, Pos, Sample);
            default -> List.of();
        };
    }

    private static List<Notice> stack(ServerPlayer Target, ServerLevel World, BlockPos Pos, @Nullable ResourceLocation Sample) {
        List<Notice> Built = new ArrayList<>();
        Built.add(Notices.forCase(Sample, Component.translatable("archive.scp_unleashed.lead.unsurfaced"), true, NoticePriority.Info));
        Built.add(Notices.forCase(Sample, Component.translatable("archive.scp_unleashed.lead.site_near"), true, NoticePriority.Lead));
        AnomalyDocument Document = Notices.document(Sample);
        if (Document != null) {
            Built.add(Notices.surfaced(Document, World, Pos, true));
        }
        Built.add(Notices.forCase(Sample, Component.translatable("archive.scp_unleashed.lead.omen"), false, NoticePriority.Lead));
        Built.add(Notice.alert(Component.translatable("notice.scp_unleashed.test.alert")).withDetail(Notices.where(World, Pos, "notice.scp_unleashed.near")));
        Built.add(Notices.strangerWatching(StrangerVariants.Foundation));
        Built.add(Notices.strangerWatchingOther(StrangerVariants.Foundation, Target.getDisplayName()));
        return Built;
    }

    @Nullable
    private static ResourceLocation sample() {
        Map<ResourceLocation, AnomalyDocument> Documents = AnomalyDocuments.server();
        if (Documents.containsKey(SampleDocument)) {
            return SampleDocument;
        }
        return AnomalyDocuments.sorted(Documents).entrySet().stream().filter(Entry -> Entry.getValue().isListed()).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    @Nullable
    private static DocumentClass classNamed(String Name) {
        for (DocumentClass Classification : DocumentClass.values()) {
            if (Classification.getSerializedName().equals(Name)) {
                return Classification;
            }
        }
        return null;
    }

    @Nullable
    private static AnomalyDocument firstOfClass(DocumentClass Classification) {
        for (AnomalyDocument Document : AnomalyDocuments.sorted(AnomalyDocuments.server()).values()) {
            if (Document.isListed() && Document.Classification() == Classification) {
                return Document;
            }
        }
        return null;
    }
}
