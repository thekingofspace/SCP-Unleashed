package net.rai.scp_unleashed.scp082;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp082Commands {
    public static final String Prefix = "commands.scp_unleashed.scp082.";
    public static final double Reach = 64.0;

    private Scp082Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp082")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(Context -> spawn(Context.getSource(), Context.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(Context -> spawn(Context.getSource(), Vec3Argument.getVec3(Context, "pos")))))
                        .then(Commands.literal("hunger")
                                .executes(Context -> status(Context.getSource()))
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, 100.0f))
                                        .executes(Context -> hunger(Context.getSource(), FloatArgumentType.getFloat(Context, "value")))))
                        .then(Commands.literal("rage")
                                .executes(Context -> rage(Context.getSource())))
                        .then(Commands.literal("release")
                                .executes(Context -> release(Context.getSource())))
                        .then(Commands.literal("status")
                                .executes(Context -> status(Context.getSource())))));
    }

    private static List<Scp082Fernand> nearby(CommandSourceStack Source) {
        Vec3 At = Source.getPosition();
        ServerLevel World = Source.getLevel();
        List<Scp082Fernand> Found = World.getEntitiesOfClass(Scp082Fernand.class, new AABB(At, At).inflate(Reach), Scp082Fernand::isAlive);
        Found.sort(Comparator.comparingDouble(Each -> Each.distanceToSqr(At)));
        return Found;
    }

    private static int spawn(CommandSourceStack Source, Vec3 Spot) {
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() + 180.0f : 0.0f;
        Optional<Scp082Fernand> Placed = Scp082Arrival.place(Source.getLevel(), Spot, Yaw);
        if (Placed.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", (int) Math.floor(Spot.x), (int) Math.floor(Spot.y), (int) Math.floor(Spot.z)), true);
        return 1;
    }

    private static int hunger(CommandSourceStack Source, float Value) {
        List<Scp082Fernand> Found = nearby(Source);
        if (Found.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "none", (int) Reach));
            return 0;
        }
        for (Scp082Fernand Each : Found) {
            Each.setHunger(Value);
        }
        Scp082Mood Mood = Scp082Mood.of(Value);
        Source.sendSuccess(() -> Component.translatable(Prefix + "hunger.set", Found.size(), String.format(Locale.ROOT, "%.0f", Value),
                Component.translatable("jade.scp_unleashed.scp082.mood." + Mood.Key)), true);
        return Found.size();
    }

    private static int rage(CommandSourceStack Source) {
        List<Scp082Fernand> Found = nearby(Source);
        if (Found.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "none", (int) Reach));
            return 0;
        }
        Found.get(0).forceRage(Source.getLevel());
        Source.sendSuccess(() -> Component.translatable(Prefix + "rage"), true);
        return 1;
    }

    private static int release(CommandSourceStack Source) {
        int Count = 0;
        for (Scp082Fernand Each : nearby(Source)) {
            Count += Each.releaseAll(Source.getLevel());
        }
        int Shown = Count;
        Source.sendSuccess(() -> Component.translatable(Prefix + "released", Shown), true);
        return Count;
    }

    private static int status(CommandSourceStack Source) {
        List<Scp082Fernand> Found = nearby(Source);
        if (Found.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "none", (int) Reach));
            return 0;
        }
        for (Scp082Fernand Each : Found) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status", String.format(Locale.ROOT, "%.1f", Each.hunger()),
                    Component.translatable("jade.scp_unleashed.scp082.mood." + Each.mood().Key), Component.translatable("jade.scp_unleashed.scp082.state." + Each.stateKey()),
                    (int) Each.getHealth(), (int) Each.getMaxHealth(), Each.meals()), false);
        }
        return Found.size();
    }
}
