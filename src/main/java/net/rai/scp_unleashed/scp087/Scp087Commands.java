package net.rai.scp_unleashed.scp087;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
public final class Scp087Commands {
    public static final double AimReach = 64.0;
    public static final int MaxDepth = 999;

    private Scp087Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp087")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("place").executes(Scp087Commands::place))
                        .then(Commands.literal("enter").executes(Scp087Commands::enter))
                        .then(Commands.literal("leave").executes(Scp087Commands::leave))
                        .then(Commands.literal("depth")
                                .then(Commands.argument("flights", IntegerArgumentType.integer(0, MaxDepth)).executes(Scp087Commands::depth)))
                        .then(Commands.literal("summon")
                                .then(Commands.literal("face").executes(Scp087Commands::face))
                                .then(Commands.literal("hunter").executes(Scp087Commands::hunter)))
                        .then(Commands.literal("inert").executes(Scp087Commands::inert))
                        .then(Commands.literal("reset").executes(Scp087Commands::reset))
                        .then(Commands.literal("status").executes(Scp087Commands::status))));
    }

    private static int place(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        ServerPlayer User = Source.getPlayerOrException();
        ServerLevel World = User.serverLevel();
        if (World.dimension() != Level.OVERWORLD) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp087.place.overworld"));
            return 0;
        }
        Vec3 Eye = User.getEyePosition();
        BlockHitResult Hit = World.clip(new ClipContext(Eye, Eye.add(User.getLookAngle().scale(AimReach)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, User));
        if (Hit.getType() != HitResult.Type.BLOCK) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp087.place.aim"));
            return 0;
        }
        BlockPos Site = Hit.getBlockPos();
        Direction Front = Scp087Entrance.facing(Site, User.position());
        Scp087Entrance.place(World, Site, Front);
        BlockPos Door = Scp087Entrance.doorLower(Site, Front);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp087.place.success", Door.getX(), Door.getY(), Door.getZ()), true);
        return 1;
    }

    private static int enter(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (Scp087Descent.visitOf(User) != null) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp087.enter.already"));
            return 0;
        }
        if (!Scp087Descent.enter(User)) {
            Context.getSource().sendFailure(Component.translatable("message.scp_unleashed.scp087.no_stairwell"));
            return 0;
        }
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (!Scp087Stairwell.inStairwell(User)) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp087.not_inside"));
            return 0;
        }
        Scp087Descent.leave(User, null);
        return 1;
    }

    private static int depth(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        int Flights = IntegerArgumentType.getInteger(Context, "flights");
        if (!Scp087Descent.setDepth(User, Flights)) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp087.not_inside"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp087.depth", Flights), true);
        return 1;
    }

    private static int face(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (Scp087Descent.visitOf(User) == null) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp087.not_inside"));
            return 0;
        }
        if (Scp087Encounters.forceFace(User.serverLevel(), User) == null) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp087.no_room"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp087.summoned.face"), true);
        return 1;
    }

    private static int hunter(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (Scp087Descent.visitOf(User) == null || Scp087World.get(User.server).inert()) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp087.not_inside"));
            return 0;
        }
        if (Scp087Encounters.forceHunter(User.serverLevel(), User) == null) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp087.no_room"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp087.summoned.hunter"), true);
        return 1;
    }

    private static int inert(CommandContext<CommandSourceStack> Context) {
        if (Scp087World.get(Context.getSource().getServer()).inert()) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp087.inert.already"));
            return 0;
        }
        Scp087Descent.neutralise(Context.getSource().getServer());
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp087.inert"), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> Context) {
        Scp087Descent.restore(Context.getSource().getServer());
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp087.reset"), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        Scp087World Data = Scp087World.get(Source.getServer());
        BlockPos Door = Data.door();
        Component Where = Door == null ? Component.translatable("commands.scp_unleashed.scp087.status.no_entrance")
                : Component.translatable("commands.scp_unleashed.scp087.status.entrance", Door.getX(), Door.getY(), Door.getZ());
        int Flights = 0;
        if (Source.getEntity() instanceof ServerPlayer User && Scp087Descent.visitOf(User) != null) {
            Flights = Scp087Descent.visitOf(User).Flights;
        }
        int Shown = Flights;
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp087.status", Where, Data.inert() ? Component.translatable("commands.scp_unleashed.scp087.status.inert")
                : Component.translatable("commands.scp_unleashed.scp087.status.active"), Scp087Descent.instances().size(), Shown), false);
        return 1;
    }
}
