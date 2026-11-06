package net.rai.scp_unleashed.scp303;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp303Commands {
    public static final String Prefix = "commands.scp_unleashed.scp303.";

    private Scp303Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp303")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(Context -> spawn(Context.getSource(), Context.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(Context -> spawn(Context.getSource(), Vec3Argument.getVec3(Context, "pos")))))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    private static int spawn(CommandSourceStack Source, Vec3 Spot) {
        ServerLevel World = Source.getLevel();
        if (!Scp303Arrival.realm(World)) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.realm"));
            return 0;
        }
        Scp303Doorman Old = Scp303Arrival.loaded(World);
        if (Old != null) {
            Old.discard();
        }
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() + 180.0f : 0.0f;
        Optional<Scp303Doorman> Made = Scp303Arrival.place(World, Spot, Yaw, Source.getEntity() != null ? Source.getEntity().getUUID() : null);
        if (Made.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", (int) Math.floor(Spot.x), (int) Math.floor(Spot.y), (int) Math.floor(Spot.z)), true);
        return 1;
    }

    private static int status(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp303State State = Scp303State.get(World.getServer());
        Scp303Doorman Present = Scp303Arrival.loaded(World.getServer().overworld());
        String Where = Present == null ? "-" : (int) Present.getX() + " " + (int) Present.getY() + " " + (int) Present.getZ() + " (" + Present.mode().name() + ")";
        String Bound = State.bound().map(Door -> Door.Door().toShortString()).orElse("-");
        Source.sendSuccess(() -> Component.translatable(Prefix + "status", State.isActive(), State.isAway(), Where, Bound), false);
        return 1;
    }
}
