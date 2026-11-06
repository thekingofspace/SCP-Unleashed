package net.rai.scp_unleashed.faction;

import com.mojang.brigadier.arguments.DoubleArgumentType;
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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Locale;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class FactionCommands {
    private static final DynamicCommandExceptionType Unknown = new DynamicCommandExceptionType(Id -> Component.translatable("commands.scp_unleashed.faction.unknown", Id));
    private static final SuggestionProvider<CommandSourceStack> Known = (Context, Builder) ->
            SharedSuggestionProvider.suggestResource(Factions.all().stream().map(FactionType::id), Builder);

    private FactionCommands() {
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> faction() {
        return Commands.argument("id", ResourceLocationArgument.id()).suggests(Known);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("faction")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("list").executes(Context -> list(Context.getSource())))
                        .then(Commands.literal("status").then(faction().executes(FactionCommands::status)))
                        .then(Commands.literal("power").then(faction().then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                .executes(FactionCommands::power))))
                        .then(Commands.literal("advance").then(faction().executes(FactionCommands::advance)))
                        .then(Commands.literal("defeat").then(faction().executes(FactionCommands::defeat)))
                        .then(Commands.literal("reset").then(faction().executes(FactionCommands::reset)))));
    }

    private static FactionType type(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ResourceLocation Id = ResourceLocationArgument.getId(Context, "id");
        FactionType Found = Factions.get(Id);
        if (Found == null) {
            throw Unknown.create(Id.toString());
        }
        return Found;
    }

    private static Component phaseName(FactionPhase Phase) {
        return Component.translatable("commands.scp_unleashed.faction.phase." + Phase.getSerializedName());
    }

    private static int list(CommandSourceStack Source) {
        if (Factions.all().isEmpty()) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.faction.none"));
            return 0;
        }
        for (FactionType Type : Factions.all()) {
            FactionState State = FactionControl.state(Source.getServer(), Type);
            Component Line = Component.translatable("commands.scp_unleashed.faction.list_entry", Type.displayName().copy().withColor(Type.colour()), Type.id().toString(),
                    phaseName(State.phase()), State.sites().size(), State.roster().size());
            Source.sendSuccess(() -> Line, false);
        }
        return Factions.all().size();
    }

    private static int status(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        FactionType Type = type(Context);
        FactionState State = FactionControl.state(Source.getServer(), Type);
        long Now = Source.getServer().overworld().getGameTime();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.faction.status_head", Type.displayName().copy().withColor(Type.colour()), phaseName(State.phase()),
                String.format(Locale.ROOT, "%.1f", State.power()), String.format(Locale.ROOT, "%.0f", Type.rules().MaxPower()), State.era()), false);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.faction.status_members", State.roster().size(), FactionLive.count(Type.id()),
                FactionLive.total(), FactionRules.GlobalLoadedCap, State.kills(), State.losses()), false);
        long Awake = State.phase() == FactionPhase.Dormant ? 0L : Math.max(0L, Now - State.awakenedAt()) / 20L;
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.faction.status_timers", Awake, Math.max(0L, Now - State.lastExpansion()) / 20L,
                Math.max(0L, Now - State.lastPatrol()) / 20L, Math.max(0L, Now - State.lastRaid()) / 20L), false);
        for (FactionSite Site : State.sites()) {
            BlockPos At = Site.anchor();
            int Present = State.rosterAt(Site.id());
            int Cap = Type.siteCap(State, Site);
            Component Line = Component.translatable(Site.built() ? "commands.scp_unleashed.faction.site_built" : "commands.scp_unleashed.faction.site_pending", Site.id(),
                    Site.kind(), At.getX(), At.getY(), At.getZ(), Site.dimension().location().toString(), Present, Cap,
                    String.format(Locale.ROOT, "%.1f", Site.reserve()), Site.tries()).withStyle(ChatFormatting.GRAY);
            Source.sendSuccess(() -> Line, false);
        }
        return 1;
    }

    private static int power(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        FactionType Type = type(Context);
        FactionState State = FactionControl.state(Source.getServer(), Type);
        double Amount = DoubleArgumentType.getDouble(Context, "amount");
        State.setPower(Amount, Type.rules().MaxPower());
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.faction.power_set", Type.displayName(), String.format(Locale.ROOT, "%.1f", State.power())), true);
        return 1;
    }

    private static int advance(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        FactionType Type = type(Context);
        FactionState State = FactionControl.state(Source.getServer(), Type);
        ServerLevel World = Source.getLevel();
        BlockPos Near = BlockPos.containing(Source.getPosition());
        if (State.phase() == FactionPhase.Defeated) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.faction.defeated", Type.displayName()));
            return 0;
        }
        if (State.phase() == FactionPhase.Dormant) {
            FactionControl.awaken(World, Type, Near, Source.getEntity());
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.faction.awakened", Type.displayName()), true);
            return 1;
        }
        Type.advance(World, State, Near);
        State.setLastExpansion(Source.getServer().overworld().getGameTime());
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.faction.advanced", Type.displayName(), State.sites().size()), true);
        return 1;
    }

    private static int defeat(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        FactionType Type = type(Context);
        if (!FactionControl.defeat(Source.getServer(), Type, BlockPos.containing(Source.getPosition()))) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.faction.defeated", Type.displayName()));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.faction.defeat_done", Type.displayName()), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        FactionType Type = type(Context);
        FactionControl.reset(Source.getServer(), Type);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.faction.reset_done", Type.displayName()), true);
        return 1;
    }
}
