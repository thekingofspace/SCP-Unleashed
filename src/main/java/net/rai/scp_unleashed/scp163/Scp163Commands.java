package net.rai.scp_unleashed.scp163;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp002.Scp002Strikes;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp163Commands {
    public static final int MinDistance = 28;
    public static final int MaxDistance = 60;
    public static final double StatusReach = 128.0;
    public static final double MoodReach = 32.0;

    private Scp163Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        LiteralArgumentBuilder<CommandSourceStack> Moods = Commands.literal("mood");
        for (Scp163Mood Mood : Scp163Mood.values()) {
            Moods.then(Commands.literal(Mood.key()).executes(Context -> mood(Context.getSource(), Mood)));
        }
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp163")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("arrive")
                                .executes(Context -> arrive(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(Context -> arrive(Context.getSource(), EntityArgument.getPlayers(Context, "targets")))))
                        .then(Commands.literal("status")
                                .executes(Context -> status(Context.getSource())))
                        .then(Commands.literal("survey")
                                .executes(Context -> survey(Context.getSource())))
                        .then(Moods)));
    }

    private static int arrive(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        Scp163Crashes Data = Scp163Crashes.get(Source.getServer());
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            if (!Scp002Strikes.fallsIn(Target.level())) {
                Source.sendFailure(Component.translatable("commands.scp_unleashed.scp163.arrive.overworld", Target.getDisplayName()));
                continue;
            }
            if (!Scp002Strikes.isValidTarget(Target)) {
                Source.sendFailure(Component.translatable("commands.scp_unleashed.scp163.arrive.invalid", Target.getDisplayName()));
                continue;
            }
            Data.crashNear(Target, MinDistance, MaxDistance);
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp163.arrive.success", Target.getDisplayName()), true);
            Count++;
        }
        return Count;
    }

    private static List<Scp163Castaway> nearby(CommandSourceStack Source, double Reach) {
        ServerLevel World = Source.getLevel();
        Vec3 At = Source.getPosition();
        return World.getEntitiesOfClass(Scp163Castaway.class, new AABB(At, At).inflate(Reach), Scp163Castaway::isAlive).stream()
                .sorted(Comparator.comparingDouble(Creature -> Creature.distanceToSqr(At))).toList();
    }

    private static int status(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        int Apparatus = Scp163Breathing.get(World).count();
        long Pending = Scp163Crashes.get(Source.getServer()).list().stream().filter(Entry -> !Entry.isLanded()).count();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp163.status.header", Apparatus, Pending), false);
        List<Scp163Castaway> Found = nearby(Source, StatusReach);
        if (Found.isEmpty()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp163.status.none"), false);
            return 0;
        }
        for (Scp163Castaway Creature : Found) {
            BlockPos Home = Creature.home();
            Component Breath;
            if (Creature.isGasping()) {
                Breath = Component.translatable("commands.scp_unleashed.scp163.status.gasping", Creature.air() / 20).withStyle(ChatFormatting.RED);
            } else if (Home != null) {
                Breath = Component.translatable("commands.scp_unleashed.scp163.status.breathing", String.format("%.1f", Math.sqrt(Creature.distanceToSqr(Vec3.atCenterOf(Home)))))
                        .withStyle(ChatFormatting.GREEN);
            } else {
                Breath = Component.translatable("commands.scp_unleashed.scp163.status.unknown");
            }
            Component Mood = Component.translatable(Creature.mood().translationKey()).withStyle(Creature.mood().colour());
            Component Suit = Component.translatable(Creature.isSuited() ? "commands.scp_unleashed.scp163.status.suited" : "commands.scp_unleashed.scp163.status.unsuited");
            BlockPos At = Creature.blockPosition();
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp163.status.entry", At.getX(), At.getY(), At.getZ(),
                    String.format("%.0f", Creature.getHealth()), String.format("%.0f", Creature.getMaxHealth()), Mood, Suit, Breath), false);
            Source.sendSuccess(() -> surveyLine(Creature, Source.getLevel().getGameTime()), false);
        }
        return Found.size();
    }

    private static Component surveyLine(Scp163Castaway Creature, long Now) {
        String State = Creature.isSurveying() ? "surveying" : Creature.isSettled() ? "settled" : "unsettled";
        Component Feeling = Component.translatable("commands.scp_unleashed.scp163.survey.state." + State)
                .withStyle(Creature.isSettled() ? ChatFormatting.AQUA : Creature.isSurveying() ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY);
        Component Outcome = Component.translatable("commands.scp_unleashed.scp163.survey.outcome." + Creature.lastOutcome().key());
        long Since = Creature.lastSurveyAt() < 0L ? -1L : (Now - Creature.lastSurveyAt()) / 20L;
        long Next = Creature.surveyDue() < 0L ? -1L : Math.max(0L, (Creature.surveyDue() - Now) / 20L);
        Component Ago = Since < 0L ? Component.translatable("commands.scp_unleashed.scp163.survey.never") : Component.translatable("commands.scp_unleashed.scp163.survey.ago", Since);
        return Component.translatable("commands.scp_unleashed.scp163.survey.line", Feeling, Outcome, Creature.lastCells(), Scp163Survey.MaxCells, Ago, Next);
    }

    private static int survey(CommandSourceStack Source) {
        List<Scp163Castaway> Found = nearby(Source, MoodReach);
        if (Found.isEmpty()) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp163.status.none"));
            return 0;
        }
        Scp163Castaway Creature = Found.get(0);
        Creature.requestSurvey(true);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp163.survey.success"), true);
        return 1;
    }

    private static int mood(CommandSourceStack Source, Scp163Mood Mood) {
        List<Scp163Castaway> Found = nearby(Source, MoodReach);
        if (Found.isEmpty()) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp163.status.none"));
            return 0;
        }
        Scp163Castaway Creature = Found.get(0);
        Creature.forceMood(Mood, Scp163Castaway.ForcedMoodTicks);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp163.mood.success", Component.translatable(Mood.translationKey()).withStyle(Mood.colour()),
                Scp163Castaway.ForcedMoodTicks / 20), true);
        return 1;
    }
}
