package net.rai.scp_unleashed.raid.hostile;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionType;
import net.rai.scp_unleashed.faction.FactionWorld;
import net.rai.scp_unleashed.faction.Factions;

import java.util.List;
import java.util.Locale;

public final class HostileCommands {
    private static final DynamicCommandExceptionType Unknown = new DynamicCommandExceptionType(Id -> Component.translatable("commands.scp_unleashed.hostile.unknown", Id));
    private static final SuggestionProvider<CommandSourceStack> Hostiles = (Context, Builder) ->
            SharedSuggestionProvider.suggestResource(Factions.all().stream().filter(Type -> Type instanceof HostileFaction).map(FactionType::id), Builder);

    private HostileCommands() {
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> faction() {
        return Commands.argument("faction", ResourceLocationArgument.id()).suggests(Hostiles);
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("raid")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("ledger").then(faction().executes(HostileCommands::ledger)))
                        .then(Commands.literal("level").then(faction().then(Commands.argument("level", IntegerArgumentType.integer(1, HostileLevels.MaxLevel))
                                .executes(HostileCommands::level))))
                        .then(Commands.literal("xp").then(faction().then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0, HostileLevels.MaxXp))
                                .executes(HostileCommands::xp))))));
    }

    private static HostileFaction type(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ResourceLocation Id = ResourceLocationArgument.getId(Context, "faction");
        if (Factions.get(Id) instanceof HostileFaction Found) {
            return Found;
        }
        throw Unknown.create(Id.toString());
    }

    private static String round(double Value) {
        return String.format(Locale.ROOT, "%.0f", Value);
    }

    public static Component summary(MinecraftServer Server, HostileFaction Type) {
        FactionState State = FactionWorld.state(Server, Type);
        long Now = Server.overworld().getGameTime();
        long Next = Math.max(0L, Type.nextRaid(State) - Now) / 20L;
        double Xp = HostileFaction.xp(State);
        return Component.translatable("commands.scp_unleashed.hostile.summary", Type.displayName().copy().withColor(Type.colour()), HostileFaction.level(State),
                round(Xp), round(HostileLevels.toNext(Xp)), HostileLevels.keycardTier(HostileFaction.level(State)), HostileLedger.size(State), Next,
                Component.translatable(Type.ruleOn(Server) ? "commands.scp_unleashed.hostile.on" : "commands.scp_unleashed.hostile.off"));
    }

    private static int ledger(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        HostileFaction Type = type(Context);
        MinecraftServer Server = Source.getServer();
        FactionState State = FactionWorld.state(Server, Type);
        Source.sendSuccess(() -> summary(Server, Type), false);
        List<HostileLedger.Entry> Entries = HostileLedger.entries(State);
        if (Entries.isEmpty()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.hostile.ledger_empty").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        for (HostileLedger.Entry One : Entries) {
            HostileTrophy Trophy = One.Trophy();
            Component Kind = Component.translatable(Trophy.creature() ? "commands.scp_unleashed.hostile.kind_entity" : "commands.scp_unleashed.hostile.kind_object");
            Component Line = Component.translatable("commands.scp_unleashed.hostile.ledger_entry", One.Id(), Trophy.label(Server.registryAccess()), Kind,
                    Trophy.Type().toString(), One.Day(), One.Time(), One.Level()).withStyle(ChatFormatting.GRAY);
            Source.sendSuccess(() -> Line, false);
        }
        return Entries.size();
    }

    private static int level(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        HostileFaction Type = type(Context);
        FactionState State = FactionWorld.state(Source.getServer(), Type);
        int Level = IntegerArgumentType.getInteger(Context, "level");
        Type.setLevel(State, Level);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.hostile.level_set", Type.displayName(), HostileFaction.level(State),
                round(HostileFaction.xp(State))), true);
        return Level;
    }

    private static int xp(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        HostileFaction Type = type(Context);
        FactionState State = FactionWorld.state(Source.getServer(), Type);
        Type.setXp(State, DoubleArgumentType.getDouble(Context, "amount"));
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.hostile.level_set", Type.displayName(), HostileFaction.level(State),
                round(HostileFaction.xp(State))), true);
        return HostileFaction.level(State);
    }
}
