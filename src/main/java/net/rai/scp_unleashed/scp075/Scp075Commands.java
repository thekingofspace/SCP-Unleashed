package net.rai.scp_unleashed.scp075;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp075Commands {
    public static final String Prefix = "commands.scp_unleashed.scp075.";
    public static final int MaxCount = 8;

    private Scp075Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp075")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(Context -> spawn(Context.getSource(), Context.getSource().getPosition(), 1))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(Context -> spawn(Context.getSource(), Vec3Argument.getVec3(Context, "pos"), 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, MaxCount))
                                                .executes(Context -> spawn(Context.getSource(), Vec3Argument.getVec3(Context, "pos"), IntegerArgumentType.getInteger(Context, "count"))))))));
    }

    private static int spawn(CommandSourceStack Source, Vec3 Spot, int Count) {
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() + 180.0f : 0.0f;
        int Spawned = Scp075Spawning.spawnGroup(Source.getLevel(), Spot, Count, Yaw, MobSpawnType.COMMAND);
        if (Spawned <= 0) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", Spawned, (int) Math.floor(Spot.x), (int) Math.floor(Spot.y), (int) Math.floor(Spot.z)), true);
        return Spawned;
    }
}
