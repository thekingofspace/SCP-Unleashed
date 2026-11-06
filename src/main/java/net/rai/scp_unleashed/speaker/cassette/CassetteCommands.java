package net.rai.scp_unleashed.speaker.cassette;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.speaker.SoundCatalog;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class CassetteCommands {
    public static final int MaxTitleLength = 64;

    private CassetteCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("cassette")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("give")
                                .then(Commands.argument("sound_id", ResourceLocationArgument.id())
                                        .suggests(CassetteCommands::suggestSounds)
                                        .executes(Context -> give(Context.getSource(), ResourceLocationArgument.getId(Context, "sound_id"), null))
                                        .then(Commands.argument("title", StringArgumentType.greedyString())
                                                .executes(Context -> give(Context.getSource(), ResourceLocationArgument.getId(Context, "sound_id"),
                                                        StringArgumentType.getString(Context, "title"))))))));
    }

    private static CompletableFuture<Suggestions> suggestSounds(CommandContext<CommandSourceStack> Context, SuggestionsBuilder Builder) {
        return SharedSuggestionProvider.suggestResource(BuiltInRegistries.SOUND_EVENT.keySet(), Builder);
    }

    private static int give(CommandSourceStack Source, ResourceLocation SoundId, @Nullable String Title) throws CommandSyntaxException {
        ServerPlayer Target = Source.getPlayerOrException();
        Component Name = Title == null || Title.isBlank() ? defaultTitle(SoundId) : Component.literal(clip(Title.strip()));
        ItemStack Tape = CassetteItem.create(SoundId, Name, null);
        if (!Target.getInventory().add(Tape)) {
            Target.drop(Tape, false);
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.cassette.give.success", Target.getDisplayName(), Name), true);
        return 1;
    }

    private static Component defaultTitle(ResourceLocation SoundId) {
        SoundCatalog.Entry Entry = SoundCatalog.find(SoundId);
        if (Entry != null) {
            return Component.translatable(Entry.getTranslationKey());
        }
        StringBuilder Words = new StringBuilder();
        for (String Word : SoundId.getPath().split("[._/]+")) {
            if (Word.isEmpty()) {
                continue;
            }
            if (!Words.isEmpty()) {
                Words.append(' ');
            }
            Words.append(Word.substring(0, 1).toUpperCase(Locale.ROOT)).append(Word.substring(1));
        }
        return Component.literal(clip(Words.isEmpty() ? SoundId.toString() : Words.toString()));
    }

    private static String clip(String Text) {
        return Text.length() <= MaxTitleLength ? Text : Text.substring(0, MaxTitleLength);
    }
}
