package net.rai.scp_unleashed.fluiddump;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

public final class PollutionCommands {
    public static final String Prefix = "commands.scp_unleashed.pollution.";
    public static final int MaxClearRadius = 4096;

    private PollutionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("pollution")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("status")
                                .executes(Context -> status(Context.getSource())))
                        .then(Commands.literal("clear")
                                .executes(Context -> clear(Context.getSource(), null))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, MaxClearRadius))
                                        .executes(Context -> clear(Context.getSource(), IntegerArgumentType.getInteger(Context, "radius")))))));
    }

    private static int status(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        PollutionField Field = PollutionField.get(World);
        Vec3 Here = Source.getPosition();
        BlockPos Spot = BlockPos.containing(Here);
        double Local = Field.densityAt(Here.x, Here.y, Here.z);
        int Count = Field.getSources().size();
        Source.sendSuccess(() -> Component.translatable(Prefix + "status", percent(Local), Component.translatable(Prefix + "level." + PollutionMath.level(Local)), Count), false);
        PollutionSource Nearest = Field.nearest(Spot, Double.MAX_VALUE);
        if (Nearest == null) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "none"), false);
            return 0;
        }
        BlockPos Centre = Nearest.getCentre();
        long Now = World.getGameTime();
        Component State = Component.translatable(Prefix + (Nearest.isFeeding(Now) ? "feeding" : "clearing"));
        Source.sendSuccess(() -> Component.translatable(Prefix + "nearest", Centre.getX(), Centre.getY(), Centre.getZ(), Math.round(Nearest.getRadius()),
                percent(Nearest.getDensity()), String.format("%.1f", Nearest.getLoad()), State), false);
        if (PollutionConversion.busy(World)) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "converting"), false);
        }
        return Count;
    }

    private static int clear(CommandSourceStack Source, @Nullable Integer Radius) {
        ServerLevel World = Source.getLevel();
        PollutionField Field = PollutionField.get(World);
        int Removed = Radius == null ? Field.clear(null, 0.0) : Field.clear(BlockPos.containing(Source.getPosition()), Radius);
        Field.broadcast(World);
        Source.sendSuccess(() -> Component.translatable(Prefix + "cleared", Removed), true);
        return Removed;
    }

    private static int percent(double Density) {
        return (int) Math.round(Density * 100.0);
    }
}
