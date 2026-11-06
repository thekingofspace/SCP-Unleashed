package net.rai.scp_unleashed.scp002;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp002Commands {
    public static final int HastenTicks = 60;

    private Scp002Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp002")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("meteor")
                                .executes(Context -> meteor(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(Context -> meteor(Context.getSource(), EntityArgument.getPlayers(Context, "targets")))))
                        .then(Commands.literal("strike")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(Scp002Commands::strike)))
                        .then(Commands.literal("place")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(Scp002Commands::place)))
                        .then(Commands.literal("pending")
                                .executes(Context -> pending(Context.getSource())))
                        .then(Commands.literal("hasten")
                                .executes(Context -> hasten(Context.getSource())))));
    }

    private static int meteor(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        Scp002Strikes Data = Scp002Strikes.get(Source.getServer());
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            if (!Scp002Strikes.fallsIn(Target.level())) {
                Source.sendFailure(Component.translatable("commands.scp_unleashed.scp002.meteor.overworld", Target.getDisplayName()));
                continue;
            }
            if (!Scp002Strikes.isValidTarget(Target)) {
                Source.sendFailure(Component.translatable("commands.scp_unleashed.scp002.meteor.invalid", Target.getDisplayName()));
                continue;
            }
            Data.strikeNear(Target, 25, 60);
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp002.meteor.success", Target.getDisplayName()), true);
            Count++;
        }
        return Count;
    }

    private static int strike(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        BlockPos Target = BlockPosArgument.getBlockPos(Context, "pos");
        ServerLevel World = Source.getLevel();
        if (!Scp002Strikes.fallsIn(World)) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp002.strike.overworld"));
            return 0;
        }
        if (!World.isInWorldBounds(Target)) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp002.strike.outside"));
            return 0;
        }
        Scp002Strikes.get(Source.getServer()).strikeAt(World, Target, Source.getPosition());
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp002.strike.success", Target.getX(), Target.getY(), Target.getZ()), true);
        return 1;
    }

    private static int place(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        BlockPos Target = BlockPosArgument.getLoadedBlockPos(Context, "pos");
        ServerLevel World = Source.getLevel();
        Direction Front = Scp002Impact.frontToward(Target, Source.getPosition(), World.random);
        Scp002Structure.build(World, Target, Front, true, Pos -> !Scp002Impact.isProtected(World, Pos, World.getBlockState(Pos)));
        Scp002Impact.clearPorch(World, Target, Front, World.random);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp002.place.success", Target.getX(), Target.getY(), Target.getZ()), true);
        return 1;
    }

    private static int pending(CommandSourceStack Source) {
        List<Scp002Strikes.Strike> Entries = Scp002Strikes.get(Source.getServer()).list();
        if (Entries.isEmpty()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp002.pending.none"), false);
            return 0;
        }
        long Now = Source.getServer().overworld().getGameTime();
        for (Scp002Strikes.Strike Entry : Entries) {
            Component Line = switch (Entry.getPhase()) {
                case Waiting -> Component.translatable("commands.scp_unleashed.scp002.pending.waiting", Math.max(0L, (Entry.getFireAt() - Now) / 20L));
                case Inbound -> Component.translatable("commands.scp_unleashed.scp002.pending.inbound", Entry.getImpact().getX(), Entry.getImpact().getY(), Entry.getImpact().getZ());
                case Landed -> Component.translatable("commands.scp_unleashed.scp002.pending.landed", Entry.getSite().getX(), Entry.getSite().getY(), Entry.getSite().getZ());
            };
            Source.sendSuccess(() -> Line, false);
        }
        return Entries.size();
    }

    private static int hasten(CommandSourceStack Source) {
        long Now = Source.getServer().overworld().getGameTime();
        int Count = Scp002Strikes.get(Source.getServer()).hasten(Now, HastenTicks);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp002.hasten.success", Count), true);
        return Count;
    }
}
