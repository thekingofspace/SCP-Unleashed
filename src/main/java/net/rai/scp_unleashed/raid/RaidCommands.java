package net.rai.scp_unleashed.raid;

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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionType;
import net.rai.scp_unleashed.faction.FactionWorld;
import net.rai.scp_unleashed.faction.Factions;
import net.rai.scp_unleashed.raid.base.BaseEstimate;
import net.rai.scp_unleashed.raid.base.BaseHeatmap;
import net.rai.scp_unleashed.raid.base.RaidBases;
import org.joml.Vector3f;

import java.util.Locale;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class RaidCommands {
    public static final DustParticleOptions Centre = new DustParticleOptions(new Vector3f(0.95f, 0.75f, 0.2f), 1.6f);
    public static final DustParticleOptions Edge = new DustParticleOptions(new Vector3f(0.85f, 0.12f, 0.1f), 1.2f);
    private static final DynamicCommandExceptionType Unknown = new DynamicCommandExceptionType(Id -> Component.translatable("commands.scp_unleashed.faction.unknown", Id));
    private static final SuggestionProvider<CommandSourceStack> Raiders = (Context, Builder) ->
            SharedSuggestionProvider.suggestResource(Factions.all().stream().filter(Type -> Type.raidStyle() != null).map(FactionType::id), Builder);

    private RaidCommands() {
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> faction() {
        return Commands.argument("faction", ResourceLocationArgument.id()).suggests(Raiders);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("raid")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("start").then(faction().executes(RaidCommands::start)))
                        .then(Commands.literal("stop").executes(Context -> stop(Context.getSource())))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))
                        .then(Commands.literal("base").executes(Context -> base(Context.getSource())))));
    }

    private static FactionType type(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ResourceLocation Id = ResourceLocationArgument.getId(Context, "faction");
        FactionType Found = Factions.get(Id);
        if (Found == null) {
            throw Unknown.create(Id.toString());
        }
        return Found;
    }

    private static String round(double Value) {
        return String.format(Locale.ROOT, "%.0f", Value);
    }

    private static String share(double Value) {
        return String.format(Locale.ROOT, "%.0f%%", Value * 100.0);
    }

    private static int start(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        FactionType Type = type(Context);
        RaidStyle Style = Type.raidStyle();
        if (Style == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.raid.cannot", Type.displayName()));
            return 0;
        }
        MinecraftServer Server = Source.getServer();
        if (RaidDirector.peaceful(Server)) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.raid.peaceful"));
            return 0;
        }
        FactionState State = FactionWorld.state(Server, Type);
        if (State.phase() == FactionPhase.Defeated) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.faction.defeated", Type.displayName()));
            return 0;
        }
        RaidWorld Data = RaidWorld.get(Server);
        if (Data.activeFor(Type.id()) != null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.raid.busy", Type.displayName()));
            return 0;
        }
        ServerPlayer User = Source.getPlayerOrException();
        if (!Style.allowsDimension(User.level().dimension())) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.raid.dimension", Type.displayName()));
            return 0;
        }
        RaidDirector.Candidate Chosen = RaidDirector.forcedCandidate(User, Style);
        Raid Started = RaidDirector.launch(Server, Data, Type, Style, State, Chosen, true, Server.overworld().getGameTime());
        if (Started == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.raid.failed", Type.displayName()));
            return 0;
        }
        BlockPos Target = Started.target();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.raid.started", Type.displayName(), Target.getX(), Target.getY(), Target.getZ(),
                round(Started.error())), true);
        return 1;
    }

    private static int stop(CommandSourceStack Source) {
        int Count = RaidDirector.stopAll(Source.getServer());
        if (Count == 0) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.raid.none"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.raid.stopped", Count), true);
        return Count;
    }

    private static int status(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        RaidWorld Data = RaidWorld.get(Server);
        long Now = Server.overworld().getGameTime();
        Source.sendSuccess(() -> Component.translatable(RaidRegistry.enabled(Server) ? "commands.scp_unleashed.raid.rule_on" : "commands.scp_unleashed.raid.rule_off",
                RaidRegistry.RuleName), false);
        for (Raid Current : Data.active()) {
            FactionType Type = Factions.get(Current.faction());
            Component Name = Type != null ? Type.displayName() : Component.literal(Current.faction().toString());
            BlockPos Target = Current.target();
            RaidStyle Style = Type != null ? Type.raidStyle() : null;
            long End = switch (Current.stage()) {
                case Warning -> Current.stageAt() + (Style != null ? Style.warningTicks() : 0L);
                case Active -> Current.deadline();
                case Retreating -> Current.stageAt() + (Style != null ? Style.retreatTicks() : 0L);
                default -> Now;
            };
            long Left = Math.max(0L, End - Now) / 20L;
            Component Line = Component.translatable("commands.scp_unleashed.raid.status_raid", Current.id(), Name, Current.stage().serialName(), Left,
                    Current.remaining(), Current.spawned(), Current.lost(), Target.getX(), Target.getY(), Target.getZ(), round(Current.error()));
            Source.sendSuccess(() -> Line, false);
        }
        for (FactionType Type : Factions.all()) {
            RaidStyle Style = Type.raidStyle();
            if (Style == null) {
                continue;
            }
            FactionState State = FactionWorld.state(Server, Type);
            RaidLedger Ledger = Data.ledger(Type.id(), State.era());
            long Next = Ledger.nextRaid() <= 0L ? -1L : Math.max(0L, Ledger.nextRaid() - Now) / 20L;
            Component Ready = Component.translatable(Type.canRaid(State) ? "commands.scp_unleashed.raid.ready" : "commands.scp_unleashed.raid.not_ready");
            Component Line = Component.translatable("commands.scp_unleashed.raid.status_faction", Type.displayName().copy().withColor(Type.colour()), Ready,
                    Next < 0L ? "-" : String.valueOf(Next), Ledger.raids(), Ledger.repelled(), Ledger.spoils().size()).withStyle(ChatFormatting.GRAY);
            Source.sendSuccess(() -> Line, false);
        }
        if (Data.active().isEmpty()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.raid.none"), false);
        }
        return Data.active().size();
    }

    private static int base(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        MinecraftServer Server = Source.getServer();
        long Now = Server.overworld().getGameTime();
        String Group = RaidBases.groupOf(User);
        BaseHeatmap Heat = RaidBases.get(Server).find(Group, User.level().dimension());
        BaseEstimate Estimate = Heat == null ? null : Heat.estimate(Now);
        if (Estimate == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.raid.base_none", Group));
            return 0;
        }
        int Intel = 0;
        BlockPos Centre = BlockPos.containing(Estimate.X(), Estimate.Y(), Estimate.Z());
        for (FactionType Type : Factions.all()) {
            if (Type.raidStyle() != null) {
                Intel = Math.max(Intel, RaidWorld.get(Server).ledger(Type.id(), FactionWorld.state(Server, Type).era()).intelNear(User.level().dimension(), Centre));
            }
        }
        int Known = Intel;
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.raid.base_head", Group, Centre.getX(), Centre.getY(), Centre.getZ(),
                round(Estimate.Footprint()), Estimate.large() ? Component.translatable("commands.scp_unleashed.raid.base_large") : Component.empty()), false);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.raid.base_quality", share(Estimate.Confidence()), share(Estimate.Share()),
                round(Estimate.error(0)), round(Estimate.error(Known)), Known), false);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.raid.base_cells", Estimate.Cells(), Heat.size(), round(Estimate.Mass()),
                round(Estimate.Total()), Heat.stashes().size(), round(Estimate.horizontalGap(User.getX(), User.getZ()))).withStyle(ChatFormatting.GRAY), false);
        show(User.serverLevel(), User, Estimate, Estimate.error(Known));
        return 1;
    }

    private static void show(ServerLevel World, ServerPlayer User, BaseEstimate Estimate, double Error) {
        double Y = Estimate.Y() + 1.2;
        for (int Step = 0; Step < 6; Step++) {
            World.sendParticles(User, Centre, true, Estimate.X(), Y + Step * 0.6, Estimate.Z(), 2, 0.1, 0.1, 0.1, 0.0);
        }
        int Points = Mth.clamp((int) (Error * 2.0), 16, 160);
        for (int Index = 0; Index < Points; Index++) {
            double Angle = Index * Mth.TWO_PI / Points;
            World.sendParticles(User, Edge, true, Estimate.X() + Math.cos(Angle) * Error, Y, Estimate.Z() + Math.sin(Angle) * Error, 1, 0.0, 0.2, 0.0, 0.0);
        }
    }
}
