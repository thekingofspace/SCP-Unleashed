package net.rai.scp_unleashed.scp098;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp098Commands {
    public static final String Prefix = "commands.scp_unleashed.scp098.";
    public static final double FindRange = 64.0;

    private Scp098Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp098")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("hive")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.literal("stage")
                                                .then(Commands.argument("stage", IntegerArgumentType.integer(0, Scp098HiveBlock.MaxStage))
                                                        .executes(Context -> hiveStage(Context, IntegerArgumentType.getInteger(Context, "stage")))))
                                        .then(Commands.literal("fill")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, Scp098HiveBlockEntity.FullnessCap))
                                                        .executes(Context -> hiveFill(Context, IntegerArgumentType.getInteger(Context, "amount")))))
                                        .then(Commands.literal("cloud").executes(Scp098Commands::hiveCloud))
                                        .then(Commands.literal("info").executes(Scp098Commands::hiveInfo))))
                        .then(Commands.literal("infect")
                                .executes(Context -> infect(Context, List.of(Context.getSource().getEntityOrException()), null))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(Context -> infect(Context, EntityArgument.getEntities(Context, "targets"), null))
                                        .then(Commands.argument("hive", BlockPosArgument.blockPos())
                                                .executes(Context -> infect(Context, EntityArgument.getEntities(Context, "targets"), BlockPosArgument.getLoadedBlockPos(Context, "hive"))))))
                        .then(Commands.literal("host")
                                .then(Commands.literal("stage")
                                        .then(targeted(Commands.argument("stage", IntegerArgumentType.integer(1, 3)), Scp098Commands::hostStage))))
                        .then(targeted(Commands.literal("cure"), Scp098Commands::cure))
                        .then(targeted(Commands.literal("status"), Scp098Commands::status))
                        .then(Commands.literal("release")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(Context -> release(Context, 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, Scp098HiveBlockEntity.Capacity))
                                                .executes(Context -> release(Context, IntegerArgumentType.getInteger(Context, "count"))))))));
    }

    private interface TargetAction {
        int run(CommandContext<CommandSourceStack> Context, Collection<? extends Entity> Targets) throws CommandSyntaxException;
    }

    private static <T extends ArgumentBuilder<CommandSourceStack, T>> T targeted(T Builder, TargetAction Action) {
        return Builder.executes(Context -> Action.run(Context, List.of(Context.getSource().getEntityOrException())))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(Context -> Action.run(Context, EntityArgument.getEntities(Context, "targets"))));
    }

    @Nullable
    private static Scp098HiveBlockEntity hiveAt(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        BlockPos Pos = BlockPosArgument.getLoadedBlockPos(Context, "pos");
        if (Context.getSource().getLevel().getBlockEntity(Pos) instanceof Scp098HiveBlockEntity Hive) {
            return Hive;
        }
        Context.getSource().sendFailure(Component.translatable(Prefix + "no_hive", Pos.getX(), Pos.getY(), Pos.getZ()));
        return null;
    }

    private static int hiveStage(CommandContext<CommandSourceStack> Context, int Stage) throws CommandSyntaxException {
        Scp098HiveBlockEntity Hive = hiveAt(Context);
        if (Hive == null) {
            return 0;
        }
        Hive.setStage(Context.getSource().getLevel(), Stage);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "hive.stage", Stage), true);
        return 1;
    }

    private static int hiveFill(CommandContext<CommandSourceStack> Context, int Amount) throws CommandSyntaxException {
        Scp098HiveBlockEntity Hive = hiveAt(Context);
        if (Hive == null) {
            return 0;
        }
        Hive.setFullness(Amount);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "hive.fill", Amount, Scp098HiveBlockEntity.FullAt), true);
        return 1;
    }

    private static int hiveCloud(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        Scp098HiveBlockEntity Hive = hiveAt(Context);
        if (Hive == null) {
            return 0;
        }
        Hive.startCloud(Context.getSource().getLevel());
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "hive.cloud"), true);
        return 1;
    }

    private static int hiveInfo(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        Scp098HiveBlockEntity Hive = hiveAt(Context);
        if (Hive == null) {
            return 0;
        }
        ServerLevel World = Context.getSource().getLevel();
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "hive.info", Hive.stage(), Hive.growth(), Hive.occupants(), Scp098HiveBlockEntity.Capacity,
                Hive.fullness(), Scp098HiveBlockEntity.FullAt, Hive.outside(World), Scp098Hives.get(World).count()), false);
        return 1;
    }

    private static List<LivingEntity> living(Collection<? extends Entity> Targets) {
        List<LivingEntity> Found = new ArrayList<>();
        for (Entity Target : Targets) {
            if (Target instanceof LivingEntity Living) {
                Found.add(Living);
            }
        }
        return Found;
    }

    private static int infect(CommandContext<CommandSourceStack> Context, Collection<? extends Entity> Targets, @Nullable BlockPos Hive) {
        CommandSourceStack Source = Context.getSource();
        int Done = 0;
        for (LivingEntity Target : living(Targets)) {
            if (!(Target.level() instanceof ServerLevel World)) {
                continue;
            }
            BlockPos Linked = Hive != null ? Hive : Scp098Hives.get(World).nearest(Target.blockPosition(), FindRange);
            if (Linked == null || !Scp098Hives.get(World).contains(Linked)) {
                Source.sendFailure(Component.translatable(Prefix + "infect.no_hive", Target.getDisplayName()));
                continue;
            }
            if (!Scp098Host.infect(Target, World, Linked)) {
                Source.sendFailure(Component.translatable(Prefix + "infect.failed", Target.getDisplayName()));
                continue;
            }
            Source.sendSuccess(() -> Component.translatable(Prefix + "infect.success", Target.getDisplayName(), Linked.getX(), Linked.getY(), Linked.getZ()), true);
            Done++;
        }
        return Done;
    }

    private static int hostStage(CommandContext<CommandSourceStack> Context, Collection<? extends Entity> Targets) {
        int Stage = IntegerArgumentType.getInteger(Context, "stage");
        int Done = 0;
        for (LivingEntity Target : living(Targets)) {
            if (!Scp098Host.isHost(Target)) {
                Context.getSource().sendFailure(Component.translatable(Prefix + "not_host", Target.getDisplayName()));
                continue;
            }
            Scp098Host.setStage(Target, Stage);
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "host.stage", Target.getDisplayName(), Stage), true);
            Done++;
        }
        return Done;
    }

    private static int cure(CommandContext<CommandSourceStack> Context, Collection<? extends Entity> Targets) {
        int Done = 0;
        for (LivingEntity Target : living(Targets)) {
            if (Scp098Host.cure(Target)) {
                Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "cure.success", Target.getDisplayName()), true);
                Done++;
            } else {
                Context.getSource().sendFailure(Component.translatable(Prefix + "not_host", Target.getDisplayName()));
            }
        }
        return Done;
    }

    private static int status(CommandContext<CommandSourceStack> Context, Collection<? extends Entity> Targets) {
        int Hosts = 0;
        for (LivingEntity Target : living(Targets)) {
            Scp098Host.State Current = Scp098Host.stateOf(Target);
            if (!Current.active()) {
                Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "status.clear", Target.getDisplayName()), false);
                continue;
            }
            int Limit = Current.Stage() == 1 ? Scp098Host.StageOneTicks : Current.Stage() == 2 ? Scp098Host.StageTwoTicks : 0;
            int Seconds = Limit > 0 ? Math.max(0, Limit - Current.Ticks()) / 20 : 0;
            int Distance = Scp098Host.hiveLevel(Target, Current) == null ? -1 : (int) Math.round(Target.position().distanceTo(Scp098Host.hiveCentre(Current)));
            BlockPos Hive = Current.Hive();
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "status.host", Target.getDisplayName(), Current.Stage(), Seconds, Hive.getX(), Hive.getY(), Hive.getZ(),
                    Distance, Scp098Host.inRange(Target, Current) ? Component.translatable(Prefix + "status.in_range") : Component.translatable(Prefix + "status.out_of_range")), false);
            Hosts++;
        }
        return Hosts;
    }

    private static int release(CommandContext<CommandSourceStack> Context, int Count) throws CommandSyntaxException {
        Scp098HiveBlockEntity Hive = hiveAt(Context);
        if (Hive == null) {
            return 0;
        }
        int Released = 0;
        for (int Index = 0; Index < Count; Index++) {
            if (Hive.release(Context.getSource().getLevel(), null, null)) {
                Released++;
            }
        }
        int Total = Released;
        if (Total == 0) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "release.empty"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "release.success", Total), true);
        return Total;
    }
}
