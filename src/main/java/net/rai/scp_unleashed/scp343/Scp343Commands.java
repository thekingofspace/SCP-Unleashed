package net.rai.scp_unleashed.scp343;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp343Commands {
    public static final String Prefix = "commands.scp_unleashed.scp343.";
    public static final double LeaveReach = 64.0;

    private Scp343Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp343")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("arrive").executes(Context -> arrive(Context.getSource())))
                        .then(Commands.literal("leave").executes(Context -> leave(Context.getSource())))
                        .then(Commands.literal("return").executes(Context -> bringBack(Context.getSource())))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    private static int arrive(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        ServerPlayer Greet = Source.getEntity() instanceof ServerPlayer User ? User : null;
        Optional<Scp343God> Placed = Scp343Arrival.arriveNear(World, BlockPos.containing(Source.getPosition()), Greet);
        if (Placed.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "arrive.failed"));
            return 0;
        }
        BlockPos Where = Placed.get().blockPosition();
        Source.sendSuccess(() -> Component.translatable(Prefix + "arrive.success", Where.getX(), Where.getY(), Where.getZ()), true);
        return 1;
    }

    private static int leave(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        List<Scp343God> Found = World.getEntitiesOfClass(Scp343God.class, new AABB(BlockPos.containing(Source.getPosition())).inflate(LeaveReach), God -> God.isAlive() && !God.isPhasing());
        for (Scp343God God : Found) {
            God.leaveNow(true);
        }
        if (Found.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "leave.none"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "leave.success", Found.size()), true);
        return Found.size();
    }

    private static int bringBack(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        int Waiting = Scp343Data.get(Server).away().size();
        if (Waiting == 0) {
            Source.sendFailure(Component.translatable(Prefix + "return.none"));
            return 0;
        }
        int Count = Scp343Away.returnAll(Server);
        Source.sendSuccess(() -> Component.translatable(Prefix + "return.success", Count, Waiting - Count), true);
        return Count;
    }

    private static int status(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        Scp343Data Data = Scp343Data.get(Server);
        long Now = Server.overworld().getGameTime();
        int Present = 0;
        for (ServerLevel World : Server.getAllLevels()) {
            for (Entity Found : World.getAllEntities()) {
                if (Found instanceof Scp343God God && God.isAlive()) {
                    Present++;
                    BlockPos Where = God.blockPosition();
                    String State = God.isPhasing() ? "away" : God.isSeated() ? "seated" : God.isLeaving() ? "leaving" : "wandering";
                    Component Line = Component.translatable(Prefix + "status.present", World.dimension().location().toString(), Where.getX(), Where.getY(), Where.getZ(),
                            Component.translatable(Prefix + "state." + State));
                    Source.sendSuccess(() -> Line, false);
                }
            }
        }
        for (Scp343Data.Away Record : Data.away()) {
            long Seconds = Math.max(0L, (Record.ReturnAt() - Now) / 20L);
            Component Line = Component.translatable(Prefix + "status.away", Record.From().location().toString(), Seconds);
            Source.sendSuccess(() -> Line, false);
        }
        int Total = Present;
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.summary", Total, Data.away().size(), Data.throneCount()), false);
        return Total + Data.away().size();
    }
}
