package net.rai.scp_unleashed.scp106;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp106Commands {
    public static final double AimReach = 48.0;
    public static final double SearchReach = 160.0;
    public static final double SeatReach = 16.0;

    private Scp106Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp106")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn").executes(Scp106Commands::spawn))
                        .then(Commands.literal("warp").executes(Context -> warp(Context, null))
                                .then(Commands.literal("floor").executes(Context -> warp(Context, Scp106Arrival.Style.Floor)))
                                .then(Commands.literal("wall").executes(Context -> warp(Context, Scp106Arrival.Style.Wall)))
                                .then(Commands.literal("ceiling").executes(Context -> warp(Context, Scp106Arrival.Style.Ceiling))))
                        .then(Commands.literal("pocket").executes(Context -> pocket(Context, null))
                                .then(Commands.argument("targets", EntityArgument.entities()).executes(Context -> pocket(Context, EntityArgument.getEntities(Context, "targets")))))
                        .then(Commands.literal("leave").executes(Scp106Commands::leave))
                        .then(Commands.literal("breach").executes(Scp106Commands::breach))
                        .then(Commands.literal("unseat").executes(Scp106Commands::unseat))
                        .then(Commands.literal("release").executes(Scp106Commands::release))
                        .then(Commands.literal("status").executes(Scp106Commands::status))));
    }

    @Nullable
    private static Scp106OldMan nearest(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp106OldMan Found = Scp106FemurBreakerBlockEntity.nearestOldMan(World, Source.getPosition());
        if (Found == null || Found.distanceToSqr(Source.getPosition()) > SearchReach * SearchReach) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp106.none", (int) SearchReach));
            return null;
        }
        return Found;
    }

    private static int spawn(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        ServerPlayer User = Source.getPlayerOrException();
        ServerLevel World = User.serverLevel();
        Vec3 Eye = User.getEyePosition();
        BlockHitResult Hit = World.clip(new ClipContext(Eye, Eye.add(User.getLookAngle().scale(AimReach)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, User));
        BlockPos Feet = Hit.getType() == HitResult.Type.BLOCK ? Hit.getBlockPos().relative(Hit.getDirection()) : User.blockPosition();
        Vec3 Spot = Vec3.atBottomCenterOf(Feet);
        Vec3 Toward = User.position().subtract(Spot);
        float Yaw = (float) (Math.atan2(Toward.z, Toward.x) * 180.0 / Math.PI) - 90.0f;
        Scp106OldMan Created = Scp106Spawning.emerge(World, Spot, Yaw, MobSpawnType.COMMAND);
        if (Created == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp106.spawn.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp106.spawn.success", Feet.getX(), Feet.getY(), Feet.getZ()), true);
        return 1;
    }

    private static int warp(CommandContext<CommandSourceStack> Context, @Nullable Scp106Arrival.Style Style) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        ServerPlayer User = Source.getPlayerOrException();
        Scp106OldMan Found = nearest(Source);
        if (Found == null) {
            return 0;
        }
        Found.forceStyle(Style);
        Found.forceWarp(User.serverLevel(), User);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp106.warp"), true);
        return 1;
    }

    private static int pocket(CommandContext<CommandSourceStack> Context, @Nullable Collection<? extends Entity> Targets) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        int Sent = 0;
        if (Targets == null) {
            if (Scp106Pocket.take(Source.getPlayerOrException())) {
                Sent++;
            }
        } else {
            for (Entity Target : Targets) {
                if (Target instanceof LivingEntity Living && !(Target instanceof Scp106OldMan) && Scp106Pocket.take(Living)) {
                    Sent++;
                }
            }
        }
        if (Sent == 0) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp106.pocket.failed"));
            return 0;
        }
        int Count = Sent;
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp106.pocket", Count), true);
        return Sent;
    }

    private static int leave(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (!Scp106Pocket.inPocket(User)) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scp106.not_inside"));
            return 0;
        }
        Scp106Pocket.leave(User, true);
        return 1;
    }

    private static int breach(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        Scp106OldMan Found = nearest(Source);
        if (Found == null) {
            return 0;
        }
        if (!Found.startBreach(Source.getLevel())) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp106.breach.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp106.breach"), true);
        return 1;
    }

    private static int unseat(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        if (Source.getEntity() != null && Source.getEntity().getVehicle() instanceof Scp106Restraint Seat) {
            Seat.ejectPassengers();
            Seat.discard();
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp106.unseat"), true);
            return 1;
        }
        ServerLevel World = Source.getLevel();
        int Released = 0;
        for (Scp106Restraint Seat : World.getEntitiesOfClass(Scp106Restraint.class, new AABB(BlockPos.containing(Source.getPosition())).inflate(SeatReach))) {
            if (Seat.isVehicle()) {
                Seat.ejectPassengers();
                Released++;
            }
            Seat.discard();
        }
        if (Released == 0) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp106.unseat.none"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp106.unseat"), true);
        return Released;
    }

    private static int release(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        int Count = Scp106PocketWorld.get(Source.getServer()).captives().size();
        Scp106Pocket.releaseAllCaptives(Source.getServer());
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp106.release", Count), true);
        return Count;
    }

    private static int status(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        Scp106OldMan Found = nearest(Source);
        if (Found == null) {
            return 0;
        }
        String Mode = Scp106Modes.key(Found.mode());
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp106.status", Component.translatable("jade.scp_unleashed.scp106.mode." + Mode),
                Found.containedTicks() / 20, Found.warpCooldown() / 20, Found.satedTicks() / 20, (int) Found.getHealth(), (int) Found.getMaxHealth()), false);
        return 1;
    }
}
