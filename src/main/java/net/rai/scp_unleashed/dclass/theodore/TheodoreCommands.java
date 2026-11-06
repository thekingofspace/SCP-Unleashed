package net.rai.scp_unleashed.dclass.theodore;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;

public final class TheodoreCommands {
    public static final String Prefix = "commands.scp_unleashed.theodore.";
    private static final SimpleCommandExceptionType Failed = new SimpleCommandExceptionType(Component.translatable(Prefix + "outbreak.failed"));
    private static final SimpleCommandExceptionType Crowded = new SimpleCommandExceptionType(Component.translatable(Prefix + "outbreak.crowded", TheodoreClusters.WorldCap));

    private TheodoreCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("theodore")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("outbreak")
                                .executes(Context -> outbreak(Context.getSource())))));
    }

    private static int outbreak(CommandSourceStack Source) throws CommandSyntaxException {
        ServerLevel World = Source.getLevel();
        if (TheodoreClusters.population(World) >= TheodoreClusters.WorldCap) {
            throw Crowded.create();
        }
        Theodore Patient = TheodoreRegistry.TheodoreType.get().create(World);
        if (Patient == null) {
            throw Failed.create();
        }
        Vec3 At = Source.getPosition();
        float Yaw = Source.getRotation().y + 180.0f;
        Patient.moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        Patient.setYHeadRot(Yaw);
        Patient.finalizeSpawn(World, World.getCurrentDifficultyAt(Patient.blockPosition()), MobSpawnType.COMMAND, null);
        if (!World.addFreshEntity(Patient)) {
            throw Failed.create();
        }
        Patient.beginEating();
        Source.sendSuccess(() -> Component.translatable(Prefix + "outbreak.success", Theodore.ClusterSize), true);
        return 1;
    }
}
