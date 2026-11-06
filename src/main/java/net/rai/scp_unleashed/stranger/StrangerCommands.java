package net.rai.scp_unleashed.stranger;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class StrangerCommands {
    private static final SuggestionProvider<CommandSourceStack> DocumentNames = (Context, Builder) -> SharedSuggestionProvider.suggest(encounterNames(), Builder);

    private StrangerCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("stranger")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.argument("document", StringArgumentType.word())
                                .suggests(DocumentNames)
                                .executes(Context -> summon(Context.getSource(), StringArgumentType.getString(Context, "document"), List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(Context -> summon(Context.getSource(), StringArgumentType.getString(Context, "document"), EntityArgument.getPlayers(Context, "targets")))))));
    }

    private static Stream<String> encounterNames() {
        return AnomalyDocuments.server().entrySet().stream()
                .filter(Entry -> Entry.getValue().Condition() instanceof StrangerEncounterCondition)
                .map(Map.Entry::getKey)
                .filter(Id -> Scp_unleashed.ModId.equals(Id.getNamespace()))
                .map(ResourceLocation::getPath);
    }

    private static int summon(CommandSourceStack Source, String Name, Collection<ServerPlayer> Targets) {
        ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
        AnomalyDocument Document = AnomalyDocuments.server().get(Id);
        if (Document == null || !(Document.Condition() instanceof StrangerEncounterCondition Condition)) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.stranger.unknown", Name));
            return 0;
        }
        StrangerEncounterCondition.ensureSpawner(Id);
        StrangerPlan Plan = Condition.plan();
        Component Designation = Document.designation();
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            if (!StrangerEncounters.isValidReader(Target)) {
                Source.sendFailure(Component.translatable("commands.scp_unleashed.stranger.invalid", Target.getDisplayName()));
                continue;
            }
            if (StrangerEncounters.get(Source.getServer()).summonNow(Target, Plan, Id)) {
                Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.stranger.success", Designation, Target.getDisplayName()), true);
                Count++;
            } else {
                Source.sendFailure(Component.translatable("commands.scp_unleashed.stranger.no_ground", Target.getDisplayName()));
            }
        }
        return Count;
    }
}
