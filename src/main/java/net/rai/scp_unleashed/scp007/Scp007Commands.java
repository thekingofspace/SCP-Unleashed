package net.rai.scp_unleashed.scp007;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp007Commands {
    public static final String Prefix = "commands.scp_unleashed.scp007.";
    public static final double Search = 16.0;
    public static final int CommandGaze = 600;

    private Scp007Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp007")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(Run -> spawn(Run.getSource(), Run.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(Run -> spawn(Run.getSource(), Vec3Argument.getVec3(Run, "pos")))))
                        .then(Commands.literal("gaze")
                                .executes(Run -> gaze(Run.getSource())))));
    }

    private static int spawn(CommandSourceStack Source, Vec3 Spot) {
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() + 180.0f : 0.0f;
        Optional<Scp007Entity> Placed = Scp007Arrival.place(Source.getLevel(), Spot, Yaw, false);
        if (Placed.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", (int) Math.floor(Spot.x), (int) Math.floor(Spot.y), (int) Math.floor(Spot.z)), true);
        return 1;
    }

    private static int gaze(CommandSourceStack Source) {
        Vec3 Here = Source.getPosition();
        List<Scp007Entity> Near = Source.getLevel().getEntitiesOfClass(Scp007Entity.class, new AABB(Here, Here).inflate(Search), Found -> Found.isAlive());
        Optional<Scp007Entity> Closest = Near.stream().min(Comparator.comparingDouble(Found -> Found.distanceToSqr(Here)));
        if (Closest.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "gaze.none", (int) Search));
            return 0;
        }
        Closest.get().sitDown(CommandGaze);
        Source.sendSuccess(() -> Component.translatable(Prefix + "gaze.success"), true);
        return 1;
    }
}
