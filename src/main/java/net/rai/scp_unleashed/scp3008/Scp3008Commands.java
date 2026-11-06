package net.rai.scp_unleashed.scp3008;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;
import net.rai.scp_unleashed.scp3008.world.Scp3008Healing;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp3008Commands {
    public static final double AimReach = 96.0;
    public static final int DefaultRadius = 2;

    private Scp3008Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp3008")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("place").executes(Scp3008Commands::place))
                        .then(Commands.literal("enter").executes(Scp3008Commands::enter))
                        .then(Commands.literal("leave").executes(Scp3008Commands::leave))
                        .then(Commands.literal("exit").executes(Scp3008Commands::exit))
                        .then(Commands.literal("status").executes(Scp3008Commands::status))
                        .then(Commands.literal("regen").executes(Context -> regen(Context, DefaultRadius, false))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, Scp3008Healing.MaxRadius))
                                        .executes(Context -> regen(Context, IntegerArgumentType.getInteger(Context, "radius"), false))
                                        .then(Commands.literal("confirm").executes(Context -> regen(Context, IntegerArgumentType.getInteger(Context, "radius"), true)))))
                        .then(Commands.literal("time")
                                .then(Commands.literal("day").executes(Context -> time(Context, Scp3008Hours.Arrival)))
                                .then(Commands.literal("dusk").executes(Context -> time(Context, Scp3008Hours.Dusk - 100L)))
                                .then(Commands.literal("night").executes(Context -> time(Context, Scp3008Hours.Dusk + 1000L)))
                                .then(Commands.literal("dawn").executes(Context -> time(Context, Scp3008Hours.Dawn - 100L)))
                                .then(Commands.argument("ticks", LongArgumentType.longArg(0L)).executes(Context -> time(Context, LongArgumentType.getLong(Context, "ticks")))))));
    }

    private static int regen(CommandContext<CommandSourceStack> Context, int Radius, boolean Apply) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        ServerPlayer User = Source.getPlayerOrException();
        ServerLevel World = User.serverLevel();
        if (!Scp3008World.inside(World)) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp3008.not_inside"));
            return 0;
        }
        if (Scp3008Healing.busy()) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp3008.regen.busy"));
            return 0;
        }
        int[] Skipped = {0};
        List<ChunkPos> Chunks = Scp3008Healing.around(World, User.blockPosition(), Radius, Skipped);
        Scp3008Healing.start(Source, Chunks, Apply, Radius);
        Source.sendSuccess(() -> Component.translatable(Apply ? "commands.scp_unleashed.scp3008.regen.rebuilding" : "commands.scp_unleashed.scp3008.regen.checking",
                Chunks.size(), Skipped[0]), Apply);
        return Chunks.size();
    }

    private static int place(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        ServerPlayer User = Source.getPlayerOrException();
        ServerLevel World = User.serverLevel();
        if (World.dimension() != Level.OVERWORLD) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp3008.place.overworld"));
            return 0;
        }
        Vec3 Eye = User.getEyePosition();
        BlockHitResult Hit = World.clip(new ClipContext(Eye, Eye.add(User.getLookAngle().scale(AimReach)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, User));
        if (Hit.getType() != HitResult.Type.BLOCK) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp3008.place.aim"));
            return 0;
        }
        BlockPos Site = Hit.getBlockPos();
        Direction Front = Scp3008Storefront.facing(Site, User.position());
        Scp3008Storefront.place(World, Site, Front);
        BlockPos Door = Scp3008Storefront.door(Site, Front);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp3008.place.success", Door.getX(), Door.getY(), Door.getZ()), true);
        return 1;
    }

    private static int enter(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (Scp3008World.inside(User)) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp3008.already"));
            return 0;
        }
        if (Scp3008World.level(User.server) == null) {
            Context.getSource().sendFailure(Component.translatable("message.scp_unleashed.scp3008.no_store"));
            return 0;
        }
        Scp3008Transit.enterDirect(User);
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (!Scp3008World.inside(User)) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp3008.not_inside"));
            return 0;
        }
        Scp3008Transit.leave(User, User.blockPosition());
        return 1;
    }

    private static int exit(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (!Scp3008World.inside(User)) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp3008.not_inside"));
            return 0;
        }
        int[] Door = Scp3008Plan.nearestExit(User.getBlockX(), User.getBlockZ());
        int Distance = (int) Math.sqrt(User.distanceToSqr(Door[0] + 1.0, Door[1], Door[2]));
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp3008.exit", Door[0], Door[1], Door[2], Distance), false);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        Scp3008State Data = Scp3008State.get(Source.getServer());
        BlockPos Door = Data.door();
        Component Store = Door == null ? Component.translatable("commands.scp_unleashed.scp3008.status.no_store")
                : Component.translatable("commands.scp_unleashed.scp3008.status.store", Door.getX(), Door.getY(), Door.getZ());
        long Hour = Scp3008Hours.hourOf(Scp3008Clock.time());
        Component Phase = Component.translatable(Scp3008Hours.nightAt(Scp3008Clock.time()) ? "commands.scp_unleashed.scp3008.status.night" : "commands.scp_unleashed.scp3008.status.day");
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp3008.status", Store, Phase, Hour), false);
        return 1;
    }

    private static int time(CommandContext<CommandSourceStack> Context, long Value) {
        CommandSourceStack Source = Context.getSource();
        Scp3008Events.setClock(Source.getServer(), Value);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp3008.time", Scp3008Hours.hourOf(Value)), true);
        return 1;
    }
}
