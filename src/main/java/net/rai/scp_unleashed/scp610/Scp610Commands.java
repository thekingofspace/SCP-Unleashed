package net.rai.scp_unleashed.scp610;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.rai.scp_unleashed.faction.FactionState;

import java.util.Collection;
import java.util.Locale;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp610Commands {
    public static final String Prefix = "commands.scp_unleashed.scp610.";

    private Scp610Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp610")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("infect").then(Commands.argument("targets", EntityArgument.entities()).executes(Scp610Commands::infect)))
                        .then(Commands.literal("cure").then(Commands.argument("targets", EntityArgument.entities()).executes(Scp610Commands::cure)))
                        .then(Commands.literal("stage").then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("stage", IntegerArgumentType.integer(1, Scp610Infection.Stages)).executes(Scp610Commands::stage))))
                        .then(Commands.literal("query").then(Commands.argument("target", EntityArgument.entity()).executes(Scp610Commands::query)))
                        .then(Commands.literal("convert").then(Commands.argument("target", EntityArgument.entity()).executes(Scp610Commands::convert)))
                        .then(Commands.literal("hive")
                                .then(Commands.literal("found").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(Scp610Commands::found)))
                                .then(Commands.literal("list").executes(Scp610Commands::list))
                                .then(Commands.literal("grow").then(Commands.argument("id", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 20000)).executes(Scp610Commands::grow)))))
                        .then(Commands.literal("biomass").then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(Scp610Commands::biomass)
                                .then(Commands.literal("set").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0, Scp610Limits.BiomassCeiling))
                                        .executes(Scp610Commands::setBiomass)))))
                        .then(Commands.literal("lord").then(Commands.literal("form").executes(Scp610Commands::lord)))
                        .then(Commands.literal("pact")
                                .then(Commands.literal("status").executes(Scp610Commands::pactStatus))
                                .then(Commands.literal("clear").executes(Scp610Commands::pactClear)))));
    }

    private static int infect(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        int Count = 0;
        for (Entity Found : EntityArgument.getEntities(Context, "targets")) {
            if (Found instanceof LivingEntity Living && Scp610Infection.infect(Living)) {
                Count++;
            }
        }
        int Total = Count;
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "infect", Total), true);
        return Count;
    }

    private static int cure(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        int Count = 0;
        for (Entity Found : EntityArgument.getEntities(Context, "targets")) {
            if (Found instanceof LivingEntity Living && Scp610Infection.cure(Living)) {
                Count++;
            }
        }
        int Total = Count;
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "cure", Total), true);
        return Count;
    }

    private static int stage(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        int Stage = IntegerArgumentType.getInteger(Context, "stage");
        Collection<? extends Entity> Found = EntityArgument.getEntities(Context, "targets");
        int Count = 0;
        for (Entity Each : Found) {
            if (Each instanceof LivingEntity Living && (Scp610Infection.isInfected(Living) || Scp610Infection.infect(Living))) {
                Scp610Infection.setStage(Living, Stage);
                Count++;
            }
        }
        int Total = Count;
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "stage", Total, Stage), true);
        return Count;
    }

    private static int query(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        Entity Found = EntityArgument.getEntity(Context, "target");
        if (!(Found instanceof LivingEntity Living) || !Scp610Infection.isInfected(Living)) {
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "query.clean", Found.getDisplayName()), false);
            return 0;
        }
        int Stage = Scp610Infection.stageOf(Living);
        int Elapsed = Scp610Infection.elapsed(Living);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "query.infected", Found.getDisplayName(), Stage, Elapsed / 20), false);
        return Stage;
    }

    private static int convert(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        Entity Found = EntityArgument.getEntity(Context, "target");
        if (!(Found instanceof LivingEntity Living) || Living instanceof Scp610Creature || !(Found.level() instanceof ServerLevel World)) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "convert.failed"));
            return 0;
        }
        Scp610Creature Made = Scp610Hosts.convert(World, Living);
        if (Made == null) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "convert.failed"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "convert.done", Made.getDisplayName()), true);
        return 1;
    }

    private static int found(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        BlockPos Pos = BlockPosArgument.getLoadedBlockPos(Context, "pos");
        Scp610Spewer Made = Scp610Founding.found(Context.getSource().getLevel(), Pos, 0.0f);
        if (Made == null) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "hive.failed"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "hive.founded", Made.hive(), Pos.getX(), Pos.getY(), Pos.getZ()), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        FactionState State = Scp610Hives.state(Source.getServer());
        Source.sendSuccess(() -> Component.translatable(Prefix + "hive.head", Component.translatable("commands.scp_unleashed.faction.phase." + State.phase().getSerializedName()),
                Scp610Hives.list(Source.getServer()).size(), (int) State.power()), false);
        for (Scp610Hive Hive : Scp610Hives.list(Source.getServer())) {
            BlockPos At = Hive.anchor();
            Source.sendSuccess(() -> Component.translatable(Prefix + "hive.entry", Hive.id(), At.getX(), At.getY(), At.getZ(), Hive.flesh(), Hive.carved(), Hive.Plan.size()), false);
        }
        return Scp610Hives.list(Source.getServer()).size();
    }

    private static int biomass(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        Scp610Hive Hive = Scp610Hives.hive(Source.getServer(), IntegerArgumentType.getInteger(Context, "id"));
        if (Hive == null) {
            Source.sendFailure(Component.translatable(Prefix + "hive.unknown"));
            return 0;
        }
        String Amount = String.format(Locale.ROOT, "%.1f", Hive.biomass());
        Source.sendSuccess(() -> Component.translatable(Prefix + "biomass", Hive.id(), Amount, (long) Scp610Biomass.capOf(Hive)), false);
        return (int) Hive.biomass();
    }

    private static int setBiomass(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        Scp610Hive Hive = Scp610Hives.hive(Source.getServer(), IntegerArgumentType.getInteger(Context, "id"));
        if (Hive == null) {
            Source.sendFailure(Component.translatable(Prefix + "hive.unknown"));
            return 0;
        }
        Hive.setBiomass(DoubleArgumentType.getDouble(Context, "amount"));
        Scp610Hives.flush(Source.getServer());
        String Amount = String.format(Locale.ROOT, "%.1f", Hive.biomass());
        Source.sendSuccess(() -> Component.translatable(Prefix + "biomass.set", Hive.id(), Amount), true);
        return 1;
    }

    private static int grow(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        Scp610Hive Hive = Scp610Hives.hive(Source.getServer(), IntegerArgumentType.getInteger(Context, "id"));
        ServerLevel World = Hive == null ? null : Source.getServer().getLevel(Hive.Site.dimension());
        if (Hive == null || World == null) {
            Source.sendFailure(Component.translatable(Prefix + "hive.unknown"));
            return 0;
        }
        int Count = IntegerArgumentType.getInteger(Context, "count");
        FactionState State = Scp610Hives.state(Source.getServer());
        int Made = 0;
        for (int Step = 0; Step < Count; Step++) {
            if (Scp610Growth.surface(World, Hive)) {
                Made++;
            }
        }
        Made += Scp610Growth.runAll(World, State, Hive, Count);
        int Total = Made;
        Source.sendSuccess(() -> Component.translatable(Prefix + "hive.grown", Total, Hive.flesh()), true);
        return Made;
    }

    private static int lord(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        FactionState State = Scp610Hives.state(Source.getServer());
        int Made = Scp610Lord.formNow(Source.getServer().overworld(), State);
        if (!Scp610Lord.formed(State)) {
            Source.sendFailure(Component.translatable(Prefix + "lord.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "lord.formed", Made), true);
        return 1;
    }

    private static int pactStatus(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        FactionState State = Scp610Hives.state(Source.getServer());
        long Now = Source.getServer().overworld().getGameTime();
        boolean Active = Scp610Pact.active(State);
        long Due = Active ? Math.max(0L, Scp610Pact.nextTribute(State) - Now) / 20L : 0L;
        Source.sendSuccess(() -> Component.translatable(Active ? Prefix + "pact.active" : Prefix + "pact.none", Due), false);
        return Active ? 1 : 0;
    }

    private static int pactClear(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        Scp610Pact.clear(Scp610Hives.state(Source.getServer()));
        Source.sendSuccess(() -> Component.translatable(Prefix + "pact.cleared"), true);
        return 1;
    }
}
