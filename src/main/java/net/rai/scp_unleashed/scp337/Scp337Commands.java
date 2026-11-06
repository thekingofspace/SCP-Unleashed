package net.rai.scp_unleashed.scp337;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp337Commands {
    public static final String Prefix = "commands.scp_unleashed.scp337.";
    public static final double StatusRange = 64.0;

    private Scp337Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp337")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(Context -> spawn(Context.getSource(), Context.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(Context -> spawn(Context.getSource(), Vec3Argument.getVec3(Context, "pos")))))
                        .then(Commands.literal("status")
                                .executes(Context -> status(Context.getSource())))));
    }

    private static int spawn(CommandSourceStack Source, Vec3 Spot) {
        ServerLevel World = Source.getLevel();
        Scp337Mass Mass = Scp337Registry.Mass.get().create(World);
        if (Mass == null) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() + 180.0f : 0.0f;
        Mass.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Mass.setYBodyRot(Yaw);
        Mass.setYHeadRot(Yaw);
        if (!World.noCollision(Mass)) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Mass.finalizeSpawn(World, World.getCurrentDifficultyAt(Mass.blockPosition()), MobSpawnType.COMMAND, null);
        if (!World.addFreshEntity(Mass)) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", (int) Math.floor(Spot.x), (int) Math.floor(Spot.y), (int) Math.floor(Spot.z)), true);
        return 1;
    }

    private static int status(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Vec3 Here = Source.getPosition();
        List<Scp337Mass> Found = World.getEntitiesOfClass(Scp337Mass.class, new AABB(Here, Here).inflate(StatusRange), Scp337Mass::isAlive);
        if (Found.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "status.none"));
            return 0;
        }
        Found.sort(Comparator.comparingDouble(Mass -> Mass.distanceToSqr(Here)));
        Scp337Mass Mass = Found.get(0);
        LivingEntity Target = Mass.getTarget();
        Component Hunting = Target != null ? Target.getDisplayName() : Component.translatable(Prefix + "status.nothing");
        Component State = Component.translatable(Prefix + "status." + stateKey(Mass.state()));
        Source.sendSuccess(() -> Component.translatable(Prefix + "status", (int) Math.ceil(Mass.getHealth()), (int) Mass.getMaxHealth(), State, Hunting, Mass.taken(),
                Mass.blockPosition().getX(), Mass.blockPosition().getY(), Mass.blockPosition().getZ()), false);
        return Found.size();
    }

    public static String stateKey(byte State) {
        return switch (State) {
            case Scp337Mass.StateReaching -> "reaching";
            case Scp337Mass.StateLashing -> "lashing";
            case Scp337Mass.StateSettling -> "settling";
            default -> "restless";
        };
    }
}
