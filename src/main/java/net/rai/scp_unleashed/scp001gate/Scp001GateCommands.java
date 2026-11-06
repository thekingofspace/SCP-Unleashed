package net.rai.scp_unleashed.scp001gate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp001GateCommands {
    private Scp001GateCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp001gate").requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("site").executes(Context -> site(Context.getSource()))
                                .then(Commands.literal("reset").executes(Context -> reset(Context.getSource())))
                                .then(Commands.literal("here").executes(Context -> here(Context.getSource()))))
                        .then(Commands.literal("garden").executes(Context -> garden(Context.getSource())))));
    }

    private static int site(CommandSourceStack Source) {
        Scp001GateSites Data = Scp001GateSites.get(Source.getServer());
        if (!Data.planned()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp001_gate.site.none"), false);
            return 0;
        }
        BlockPos Centre = Data.centre();
        int Total = Data.chunks().size();
        int Built = Data.finished();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp001_gate.site.where", Centre.getX(), Centre.getY(), Centre.getZ(),
                Data.front().getSerializedName()), false);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp001_gate.site.progress", Built, Total, Scp001GateEmitter.pending()), false);
        return 1;
    }

    private static int reset(CommandSourceStack Source) {
        Scp001GateSites.get(Source.getServer()).forget();
        Scp001GateEmitter.clear();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp001_gate.site.reset"), true);
        return 1;
    }

    private static int here(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        ServerLevel World = Source.getServer().overworld();
        if (User.level().dimension() != Level.OVERWORLD) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp001_gate.site.overworld"));
            return 0;
        }
        Scp001GateSites Data = Scp001GateSites.get(Source.getServer());
        Direction Front = User.getDirection().getOpposite();
        BlockPos Spot = User.blockPosition().relative(User.getDirection(), 70);
        int Ground = World.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, Spot.getX(), Spot.getZ());
        Data.plan(Spot.getX(), Spot.getZ(), Ground, Front, World.getSeed() ^ Scp001GatePlan.Salt ^ Spot.asLong());
        Scp001GateEmitter.clear();
        Scp001GateEmitter.planned(World, Data);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp001_gate.site.where", Spot.getX(), Ground, Spot.getZ(), Front.getSerializedName()), true);
        return 1;
    }

    private static int garden(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        if (Scp001GateGarden.inside(User.level())) {
            Scp001GateGarden.leave(User);
            return 1;
        }
        return Scp001GateGarden.enter(User, User.blockPosition(), User.getDirection().getOpposite()) ? 1 : 0;
    }
}
