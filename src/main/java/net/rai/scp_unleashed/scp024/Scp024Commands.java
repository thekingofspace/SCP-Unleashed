package net.rai.scp_unleashed.scp024;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp024Commands {
    public static final double AimReach = 96.0;

    private Scp024Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp024")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("build").executes(Scp024Commands::build))
                        .then(Commands.literal("enter").executes(Scp024Commands::enter))
                        .then(Commands.literal("leave").executes(Scp024Commands::leave))
                        .then(Commands.literal("game")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Scp024Games.ids(), Builder))
                                        .executes(Scp024Commands::game)))
                        .then(Commands.literal("win").executes(Context -> force(Context, Scp024Outcome.Win)))
                        .then(Commands.literal("lose").executes(Context -> force(Context, Scp024Outcome.Lose)))
                        .then(Commands.literal("status").executes(Scp024Commands::status))
                        .then(Commands.literal("audit").executes(Scp024Commands::audit))
                        .then(Commands.literal("simulate")
                                .then(Commands.literal("death").executes(Context -> simulate(Context, "death")))
                                .then(Commands.literal("dimension").executes(Context -> simulate(Context, "dimension")))
                                .then(Commands.literal("stop").executes(Context -> simulate(Context, "stop"))))));
    }

    private static int build(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        ServerPlayer User = Source.getPlayerOrException();
        ServerLevel World = User.serverLevel();
        if (World.dimension() != Level.OVERWORLD) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp024.build.overworld"));
            return 0;
        }
        Vec3 Eye = User.getEyePosition();
        Vec3 Reach = Eye.add(User.getLookAngle().scale(AimReach));
        BlockHitResult Hit = World.clip(new ClipContext(Eye, Reach, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, User));
        if (Hit.getType() != HitResult.Type.BLOCK) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp024.build.aim"));
            return 0;
        }
        BlockPos Site = Hit.getBlockPos();
        Direction Front = Scp024Warehouse.facing(Site, User.position());
        Scp024Warehouse.place(World, Site, Front);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp024.build.success", Site.getX(), Site.getY(), Site.getZ()), true);
        return 1;
    }

    private static int enter(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (Scp024Stage.inStage(User)) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp024.enter.already"));
            return 0;
        }
        boolean Started = Scp024Runs.start(User, null);
        if (Started) {
            Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp024.enter.success"), true);
        }
        return Started ? 1 : 0;
    }

    private static int leave(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (!Scp024Runs.leave(User)) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp024.none"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp024.leave.success"), true);
        return 1;
    }

    private static int game(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        String Id = StringArgumentType.getString(Context, "id");
        if (!Scp024Games.exists(Id)) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp024.game.unknown", Id));
            return 0;
        }
        if (!Scp024Runs.forceGame(User, Id)) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp024.none"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp024.game.success", Id), true);
        return 1;
    }

    private static int force(CommandContext<CommandSourceStack> Context, Scp024Outcome Result) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (!Scp024Runs.force(User, Result)) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp024.force.none"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Result == Scp024Outcome.Win ? "commands.scp_unleashed.scp024.win.success"
                : "commands.scp_unleashed.scp024.lose.success"), true);
        return 1;
    }

    private static int audit(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        ServerPlayer User = Source.getPlayerOrException();
        Scp024Run Run = Scp024Runs.runOf(User);
        int Bars = Scp024Hud.barsHolding(User.getUUID());
        int Owned = 0;
        int Orphans = 0;
        ServerLevel Stage = Scp024Stage.level(Source.getServer());
        if (Stage != null) {
            for (Entity Found : Stage.getAllEntities()) {
                if (Scp024Stage.isOwned(Found)) {
                    Owned++;
                    if (!Scp024Runs.isLive(Scp024Stage.owner(Found))) {
                        Orphans++;
                    }
                }
            }
        }
        int Total = Owned;
        int Stray = Orphans;
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp024.audit.state", Run == null ? "-" : Run.phase().name(), Bars,
                Scp024Hud.liveBars(), Scp024Runs.liveTokens(), Total, Stray), false);
        boolean Clean = Run == null && Bars == 0 && Orphans == 0 && !Scp024Stage.inStage(User);
        if (Run != null || Scp024Stage.inStage(User)) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp024.audit.running"), false);
        } else {
            Source.sendSuccess(() -> Component.translatable(Clean ? "commands.scp_unleashed.scp024.audit.clean" : "commands.scp_unleashed.scp024.audit.dirty"), false);
        }
        return Clean ? 1 : 0;
    }

    private static int simulate(CommandContext<CommandSourceStack> Context, String Path0) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        ServerPlayer User = Source.getPlayerOrException();
        if (Scp024Runs.runOf(User) == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp024.none"));
            return 0;
        }
        switch (Path0) {
            case "death" -> Scp024Hazards.eliminate(User);
            case "dimension" -> {
                Scp024Runs.Outside Spot = Scp024Runs.outside(User);
                Scp024Stage.teleport(User, Spot.World(), Spot.Pos(), Spot.Yaw(), 0.0f);
            }
            default -> Scp024Runs.simulateStop(User);
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp024.simulate.done", Path0), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        Scp024World Data = Scp024World.get(Source.getServer());
        BlockPos Door = Data.door();
        if (Door != null) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp024.status.warehouse", Door.getX(), Door.getY(), Door.getZ()), false);
        } else {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp024.status.no_warehouse"), false);
        }
        ServerPlayer User = Source.getPlayer();
        Scp024Run Run = User == null ? null : Scp024Runs.runOf(User);
        if (Run != null) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp024.status.run", Run.wins(), Scp024Runs.Rounds, Run.phase().name(),
                    Run.gameId().isEmpty() ? "-" : Run.gameId(), Run.field()), false);
        }
        return 1;
    }
}
