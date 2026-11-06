package net.rai.scp_unleashed.scp016;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp016Commands {
    public static final String Prefix = "commands.scp_unleashed.scp016.";
    public static final int ListedInstances = 8;
    public static final int MaxHaste = 600;
    public static final Map<String, Supplier<EntityType<? extends Scp016Spawn>>> Forms = forms();

    private Scp016Commands() {
    }

    private static Map<String, Supplier<EntityType<? extends Scp016Spawn>>> forms() {
        Map<String, Supplier<EntityType<? extends Scp016Spawn>>> Found = new LinkedHashMap<>();
        Found.put("skitter", Scp016Registry.SkitterType::get);
        Found.put("brute", Scp016Registry.BruteType::get);
        Found.put("slinger", Scp016Registry.SlingerType::get);
        Found.put("duelist", Scp016Registry.DuelistType::get);
        Found.put("glutton", Scp016Registry.GluttonType::get);
        Found.put("swimmer", Scp016Registry.SwimmerType::get);
        Found.put("flyer", Scp016Registry.FlyerType::get);
        return Found;
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp016")
                        .requires(Source -> Source.hasPermission(2))
                        .then(targeted("afflict", Scp016Commands::afflict))
                        .then(targeted("cure", Scp016Commands::cure))
                        .then(targeted("query", Scp016Commands::query))
                        .then(Commands.literal("hasten")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, MaxHaste))
                                        .executes(Context -> hasten(Context.getSource(), List.of(Context.getSource().getPlayerOrException()),
                                                IntegerArgumentType.getInteger(Context, "seconds")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(Context -> hasten(Context.getSource(), EntityArgument.getPlayers(Context, "targets"),
                                                        IntegerArgumentType.getInteger(Context, "seconds"))))))
                        .then(Commands.literal("count").executes(Context -> count(Context.getSource())))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("form", StringArgumentType.word()).suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Forms.keySet(), Builder))
                                        .executes(Context -> spawn(Context.getSource(), StringArgumentType.getString(Context, "form"), Context.getSource().getPosition()))
                                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                                .executes(Context -> spawn(Context.getSource(), StringArgumentType.getString(Context, "form"), Vec3Argument.getVec3(Context, "pos"))))))
                        .then(Commands.literal("split")
                                .then(Commands.argument("cause", StringArgumentType.word()).suggests((Context, Builder) -> SharedSuggestionProvider.suggest(causes(), Builder))
                                        .executes(Context -> split(Context.getSource(), StringArgumentType.getString(Context, "cause"), Context.getSource().getPosition()))
                                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                                .executes(Context -> split(Context.getSource(), StringArgumentType.getString(Context, "cause"), Vec3Argument.getVec3(Context, "pos"))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String Name, BiFunction<CommandSourceStack, Collection<ServerPlayer>, Integer> Action) {
        return Commands.literal(Name)
                .executes(Context -> Action.apply(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(Context -> Action.apply(Context.getSource(), EntityArgument.getPlayers(Context, "targets"))));
    }

    private static int afflict(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Changed = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp016Affliction.afflict(Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "afflict.success", Target.getDisplayName(), Scp016Affliction.Duration / 20), true);
                Changed++;
            } else {
                Source.sendFailure(Component.translatable(Prefix + "afflict.failed", Target.getDisplayName()));
            }
        }
        return Changed;
    }

    private static int cure(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Changed = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp016Affliction.cure(Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "cure.success", Target.getDisplayName()), true);
                Changed++;
            } else {
                Source.sendFailure(Component.translatable(Prefix + "cure.failed", Target.getDisplayName()));
            }
        }
        return Changed;
    }

    private static int query(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Positive = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp016Affliction.isAfflicted(Target)) {
                int Stage = Scp016Affliction.stageOf(Scp016Affliction.elapsed(Target));
                int Seconds = (Scp016Affliction.remaining(Target) + 19) / 20;
                Source.sendSuccess(() -> Component.translatable(Prefix + "query.positive", Target.getDisplayName(), Stage, Scp016Affliction.Stages, Seconds), false);
                Positive++;
            } else {
                Source.sendSuccess(() -> Component.translatable(Prefix + "query.negative", Target.getDisplayName()), false);
            }
        }
        return Positive;
    }

    private static int hasten(CommandSourceStack Source, Collection<ServerPlayer> Targets, int Seconds) {
        int Changed = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp016Affliction.hasten(Target, Seconds * 20)) {
                int Left = (Scp016Affliction.remaining(Target) + 19) / 20;
                Source.sendSuccess(() -> Component.translatable(Prefix + "hasten.success", Target.getDisplayName(), Left), true);
                Changed++;
            } else {
                Source.sendFailure(Component.translatable(Prefix + "cure.failed", Target.getDisplayName()));
            }
        }
        return Changed;
    }

    private static List<String> causes() {
        return Arrays.stream(Scp016Cause.values()).map(Scp016Cause::key).toList();
    }

    private static int spawn(CommandSourceStack Source, String Form, Vec3 At) {
        Supplier<EntityType<? extends Scp016Spawn>> Type = Forms.get(Form);
        if (Type == null) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.unknown", Form));
            return 0;
        }
        Scp016Spawn Risen = Scp016Events.raise(Source.getLevel(), Type.get(), At, Scp016Host.random(Source.getLevel().getRandom()), true);
        if (Risen == null) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed", Form));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", Risen.getDisplayName(), BlockPos.containing(At).toShortString()), true);
        return 1;
    }

    private static int split(CommandSourceStack Source, String Name, Vec3 At) {
        Scp016Cause Cause = Scp016Cause.byKey(Name);
        if (Cause == null) {
            Source.sendFailure(Component.translatable(Prefix + "split.unknown", Name));
            return 0;
        }
        Scp016Spawn Risen = Scp016Events.split(Source.getLevel(), At, 0.6f, 1.8f, Cause, Scp016Host.random(Source.getLevel().getRandom()));
        if (Risen == null) {
            Source.sendFailure(Component.translatable(Prefix + "split.capped", Scp016Events.SpawnCap));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "split.success", Cause.key(), Risen.getDisplayName()), true);
        return 1;
    }

    private static int count(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        List<? extends Scp016Spawn> Found = World.getEntities(EntityTypeTest.forClass(Scp016Spawn.class), Instance -> Instance.isAlive());
        Source.sendSuccess(() -> Component.translatable(Prefix + "count", Found.size(), Scp016Events.SpawnCap), false);
        for (Scp016Spawn Instance : Found.subList(0, Math.min(ListedInstances, Found.size()))) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "count.entry", Instance.getDisplayName(), (Instance.getRemaining() + 19) / 20,
                    Instance.blockPosition().toShortString()), false);
        }
        return Found.size();
    }
}
