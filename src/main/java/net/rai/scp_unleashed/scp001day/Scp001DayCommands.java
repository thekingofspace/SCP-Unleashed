package net.rai.scp_unleashed.scp001day;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.gateway.GatewayStash;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayRift;
import net.rai.scp_unleashed.scp001day.network.Scp001DaySunlightDebugPayload;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySun;
import net.rai.scp_unleashed.scp001day.sun.SunVector;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp001DayCommands {
    public static final String Prefix = "commands.scp_unleashed.scp001_day.";

    private Scp001DayCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        LiteralArgumentBuilder<CommandSourceStack> Root = Commands.literal("scp001day").requires(Source -> Source.hasPermission(2));
        Root.then(Commands.literal("sun").executes(Context -> sun(Context.getSource())));
        Root.then(Commands.literal("rift").executes(Context -> rift(Context.getSource())));
        Root.then(Commands.literal("enter").executes(Context -> enter(Context.getSource())));
        Root.then(Commands.literal("sunlight").then(Commands.literal("debug").executes(Context -> sunlight(Context.getSource()))));
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId).then(Root));
    }

    private static int sunlight(CommandSourceStack Source) {
        if (!(Source.getEntity() instanceof ServerPlayer User)) {
            Source.sendFailure(Component.translatable(Prefix + "player"));
            return 0;
        }
        PacketDistributor.sendToPlayer(User, Scp001DaySunlightDebugPayload.Instance);
        Source.sendSuccess(() -> Component.translatable(Prefix + "sunlight"), false);
        return 1;
    }

    private static int sun(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        SunVector Sun = Scp001DaySun.direction(World.getDayTime());
        float Exposure = 0.0f;
        float Turned = 0.0f;
        if (Source.getEntity() instanceof ServerPlayer User) {
            Scp001DaySunlight.State Probe = new Scp001DaySunlight.State();
            Exposure = Scp001DaySunlight.exposure(World, User, Probe);
            Turned = Scp001DayTurning.turning(User) ? Scp001DayTurning.progress(User) * 100.0f : 0.0f;
        }
        String Elevation = String.format("%.1f", Scp001DaySun.elevation(Sun));
        String Shown = String.format("%.2f", Exposure);
        String Warm = String.format("%.0f%%", Turned);
        boolean Lethal = Scp001DaySun.lethal(Sun);
        Source.sendSuccess(() -> Component.translatable(Prefix + "sun", Elevation, Lethal, Shown, Warm), false);
        return 1;
    }

    private static int rift(CommandSourceStack Source) {
        if (!(Source.getEntity() instanceof ServerPlayer User)) {
            Source.sendFailure(Component.translatable(Prefix + "player"));
            return 0;
        }
        Scp001DayRift Made = Scp001DayRifts.spawnNear(Source.getLevel(), User, Source.getLevel().random);
        if (Made == null) {
            Source.sendFailure(Component.translatable(Prefix + "rift_failed"));
            return 0;
        }
        String Where = Made.blockPosition().toShortString();
        Source.sendSuccess(() -> Component.translatable(Prefix + "rift", Where), false);
        return 1;
    }

    private static int enter(CommandSourceStack Source) {
        if (!(Source.getEntity() instanceof ServerPlayer User)) {
            Source.sendFailure(Component.translatable(Prefix + "player"));
            return 0;
        }
        GatewayDestination Destination = GatewayDestinations.get(Scp001DayDestination.Id);
        if (Destination == null || User.server.getLevel(Destination.Dimension()) == null) {
            Source.sendFailure(Component.translatable(Prefix + "missing"));
            return 0;
        }
        GatewayStash.Spot Back = new GatewayStash.Spot(User.level().dimension(), User.position(), User.getYRot());
        if (GatewayTravel.enter(User, Destination, Back)) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "enter"), false);
            return 1;
        }
        return 0;
    }
}
