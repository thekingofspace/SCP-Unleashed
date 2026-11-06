package net.rai.scp_unleashed.scp3008.furniture.electronics;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.Scp3008World;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Painter;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;

import java.util.Arrays;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp3008DepartmentFinder {
    public static final int ReachX = 160;
    public static final int ReachZ = 60;

    private Scp3008DepartmentFinder() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp3008")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("find")
                                .requires(Source -> Source.hasPermission(2))
                                .then(Commands.argument("department", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Arrays.asList(Scp3008Plan.Departments), Builder))
                                        .executes(Context -> find(Context, false))
                                        .then(Commands.literal("tp").executes(Context -> find(Context, true)))))));
    }

    public static int[] nearest(int Department, int X, int Z) {
        int Ux0 = Scp3008Plan.unitX(X);
        int Uz0 = Scp3008Plan.unitZ(Z);
        int[] Best = null;
        long BestDistance = Long.MAX_VALUE;
        for (int Ux = Ux0 - ReachX; Ux <= Ux0 + ReachX; Ux++) {
            for (int Uz = Uz0 - ReachZ; Uz <= Uz0 + ReachZ; Uz++) {
                if (Scp3008Plan.cachedDepartment(Ux, Uz) != Department) {
                    continue;
                }
                long Dx = Scp3008Plan.originX(Ux) + 1 - X;
                long Dz = Scp3008Plan.originZ(Uz) + Scp3008Painter.SignLaneZ - Z;
                long Distance = Dx * Dx + Dz * Dz;
                if (Distance < BestDistance) {
                    BestDistance = Distance;
                    Best = new int[]{Scp3008Plan.originX(Ux) + 1, Scp3008Plan.Stand, Scp3008Plan.originZ(Uz) + Scp3008Painter.SignLaneZ};
                }
            }
        }
        return Best;
    }

    private static int find(CommandContext<CommandSourceStack> Context, boolean Teleport) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        ServerPlayer User = Source.getPlayerOrException();
        String Key = StringArgumentType.getString(Context, "department");
        int Department = Arrays.asList(Scp3008Plan.Departments).indexOf(Key);
        if (Department < 0) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp3008.find.unknown", Key));
            return 0;
        }
        boolean Inside = Scp3008World.inside(User);
        int X = Inside ? User.getBlockX() : Scp3008World.arrivalBlock(User.server).getX();
        int Z = Inside ? User.getBlockZ() : Scp3008World.arrivalBlock(User.server).getZ();
        int[] Spot = nearest(Department, X, Z);
        Component Name = Component.translatable("biome.scp_unleashed.scp3008_" + Key);
        if (Spot == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp3008.find.none", Name));
            return 0;
        }
        int Distance = (int) Math.sqrt((double) (Spot[0] - X) * (Spot[0] - X) + (double) (Spot[2] - Z) * (Spot[2] - Z));
        if (Teleport) {
            if (!Inside) {
                Source.sendFailure(Component.translatable("commands.scp_unleashed.scp3008.not_inside"));
                return 0;
            }
            ServerLevel World = User.serverLevel();
            User.teleportTo(World, Spot[0] + 0.5, Spot[1], Spot[2] + 0.5, User.getYRot(), User.getXRot());
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp3008.find.teleported", Name, Spot[0], Spot[1], Spot[2]), true);
            return 1;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp3008.find.found", Name, Spot[0], Spot[1], Spot[2], Distance), false);
        return 1;
    }
}
