package net.rai.scp_unleashed.scp129;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp129Commands {
    public static final String Prefix = "commands.scp_unleashed.scp129.";
    public static final List<String> StrainSuggestions = List.of("1", "2", "3", "4", "5", "45", "12345", "all");
    public static final List<String> EpisodeNames = List.of("cough", "blur", "freeze");
    public static final int LongestAdvance = 600;

    private Scp129Commands() {
    }

    private interface Action {
        int run(CommandSourceStack Source, Collection<LivingEntity> Targets) throws CommandSyntaxException;
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp129")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("infect")
                                .then(Commands.argument("strains", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(StrainSuggestions, Builder))
                                        .executes(Context -> infect(Context.getSource(), self(Context), StringArgumentType.getString(Context, "strains")))
                                        .then(Commands.argument("targets", EntityArgument.entities())
                                                .executes(Context -> infect(Context.getSource(), living(Context), StringArgumentType.getString(Context, "strains"))))))
                        .then(targeted("cure", Scp129Commands::cure))
                        .then(targeted("status", Scp129Commands::status))
                        .then(Commands.literal("advance")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, LongestAdvance))
                                        .executes(Context -> advance(Context.getSource(), self(Context), IntegerArgumentType.getInteger(Context, "seconds")))
                                        .then(Commands.argument("targets", EntityArgument.entities())
                                                .executes(Context -> advance(Context.getSource(), living(Context), IntegerArgumentType.getInteger(Context, "seconds"))))))
                        .then(Commands.literal("episode")
                                .then(Commands.argument("kind", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(EpisodeNames, Builder))
                                        .executes(Context -> episode(Context.getSource(), self(Context), StringArgumentType.getString(Context, "kind")))
                                        .then(Commands.argument("targets", EntityArgument.entities())
                                                .executes(Context -> episode(Context.getSource(), living(Context), StringArgumentType.getString(Context, "kind"))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String Name, Action Work) {
        return Commands.literal(Name)
                .executes(Context -> Work.run(Context.getSource(), self(Context)))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(Context -> Work.run(Context.getSource(), living(Context))));
    }

    private static Collection<LivingEntity> self(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        return List.of(Context.getSource().getPlayerOrException());
    }

    private static Collection<LivingEntity> living(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        List<LivingEntity> Found = new ArrayList<>();
        for (Entity Candidate : EntityArgument.getEntities(Context, "targets")) {
            if (Candidate instanceof LivingEntity Subject) {
                Found.add(Subject);
            }
        }
        return Found;
    }

    private static int infect(CommandSourceStack Source, Collection<LivingEntity> Targets, String Typed) {
        int Strains = Scp129Strain.parse(Typed);
        if (Strains <= 0) {
            Source.sendFailure(Component.translatable(Prefix + "infect.strains", Typed));
            return 0;
        }
        int Changed = 0;
        for (LivingEntity Target : Targets) {
            if (!Scp129Infection.canCatch(Target)) {
                Source.sendFailure(Component.translatable(Prefix + "infect.failed", Target.getDisplayName()));
                continue;
            }
            Scp129Infection.force(Target, Strains);
            String Now = Scp129Strain.digits(Scp129Infection.strainsOf(Target));
            Source.sendSuccess(() -> Component.translatable(Prefix + "infect.success", Target.getDisplayName(), Now), true);
            Changed++;
        }
        return Changed;
    }

    private static int cure(CommandSourceStack Source, Collection<LivingEntity> Targets) {
        int Changed = 0;
        for (LivingEntity Target : Targets) {
            if (Scp129Infection.cure(Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "cure.success", Target.getDisplayName()), true);
                Changed++;
            } else {
                Source.sendFailure(Component.translatable(Prefix + "cure.failed", Target.getDisplayName()));
            }
        }
        return Changed;
    }

    private static int status(CommandSourceStack Source, Collection<LivingEntity> Targets) {
        int Positive = 0;
        for (LivingEntity Target : Targets) {
            Scp129Infection.State Current = Scp129Infection.stateOf(Target);
            if (Current.Strains() == 0) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.clean", Target.getDisplayName()), false);
            } else {
                Positive++;
                String Carried = Scp129Strain.digits(Current.Strains());
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.carrying", Target.getDisplayName(), Carried), false);
                for (Scp129Strain Strain : Scp129Strain.values()) {
                    if (!Current.has(Strain)) {
                        continue;
                    }
                    String Key = Strain == Scp129Strain.Five || Strain == Scp129Strain.Four && Current.isLethal() ? "status.strain_held" : "status.strain_clearing";
                    int Seconds = (Current.clearing(Strain) + 19) / 20;
                    Source.sendSuccess(() -> Component.translatable(Prefix + Key, Strain.digit(), Seconds), false);
                }
                if (Current.isLethal()) {
                    int Stage = Scp129Course.stageOf(Current.Course());
                    int Elapsed = Current.Course() / 20;
                    int Left = Scp129Course.secondsLeft(Current.Course());
                    Source.sendSuccess(() -> Component.translatable(Prefix + "status.course", Stage, Scp129Course.Stages, Elapsed, Left), false);
                }
            }
            List<String> Resistant = new ArrayList<>();
            for (Scp129Strain Strain : Scp129Strain.values()) {
                if (Current.resisting(Strain) > 0) {
                    Resistant.add(Strain.digit() + " (" + (Current.resisting(Strain) + 19) / 20 + "s)");
                }
            }
            if (!Resistant.isEmpty()) {
                String Listed = String.join(", ", Resistant);
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.resisting", Listed), false);
            }
            boolean Suited = Scp129Infection.isProtected(Target);
            Source.sendSuccess(() -> Component.translatable(Prefix + (Suited ? "status.masked" : "status.unmasked")), false);
        }
        return Positive;
    }

    private static int advance(CommandSourceStack Source, Collection<LivingEntity> Targets, int Seconds) {
        int Changed = 0;
        for (LivingEntity Target : Targets) {
            Scp129Infection.State Current = Scp129Infection.stateOf(Target);
            if (!Current.isLethal()) {
                Source.sendFailure(Component.translatable(Prefix + "advance.failed", Target.getDisplayName()));
                continue;
            }
            Scp129Infection.setCourse(Target, Math.min(Scp129Course.Length - Scp129Infection.Beat, Current.Course() + Seconds * 20));
            int Stage = Scp129Course.stageOf(Scp129Infection.stateOf(Target).Course());
            int Left = Scp129Course.secondsLeft(Scp129Infection.stateOf(Target).Course());
            Source.sendSuccess(() -> Component.translatable(Prefix + "advance.success", Target.getDisplayName(), Stage, Left), true);
            Changed++;
        }
        return Changed;
    }

    private static int episode(CommandSourceStack Source, Collection<LivingEntity> Targets, String Typed) {
        int Kind = switch (Typed.toLowerCase(Locale.ROOT)) {
            case "cough" -> Scp129Symptoms.Cough;
            case "blur" -> Scp129Symptoms.Blur;
            case "freeze" -> Scp129Symptoms.Freeze;
            default -> Scp129Symptoms.None;
        };
        if (Kind == Scp129Symptoms.None) {
            Source.sendFailure(Component.translatable(Prefix + "episode.kind", Typed));
            return 0;
        }
        int Changed = 0;
        for (LivingEntity Target : Targets) {
            if (!(Target.level() instanceof ServerLevel World)) {
                continue;
            }
            int Stage = Scp129Course.symptomStage(Scp129Infection.stateOf(Target));
            if (Scp129Symptoms.begin(Target, World, Kind, Stage, false)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "episode.success", Target.getDisplayName(), Typed), true);
                Changed++;
            } else {
                Source.sendFailure(Component.translatable(Prefix + "episode.failed", Target.getDisplayName()));
            }
        }
        return Changed;
    }
}
