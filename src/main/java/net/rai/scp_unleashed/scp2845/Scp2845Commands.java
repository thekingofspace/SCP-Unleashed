package net.rai.scp_unleashed.scp2845;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp002.Scp002Impact;
import net.rai.scp_unleashed.scp002.Scp002Strikes;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp2845Commands {
    public static final int CometNear = 30;
    public static final int CometFar = 50;
    public static final double LapseReach = 48.0;

    private Scp2845Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp2845")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("comet").executes(Context -> comet(Context.getSource())))
                        .then(Commands.literal("strike").then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(Context -> strike(Context.getSource(), BlockPosArgument.getLoadedBlockPos(Context, "pos")))))
                        .then(Commands.literal("lapse").executes(Context -> lapse(Context.getSource())))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    private static int comet(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        if (!Scp002Strikes.fallsIn(World)) {
            Source.sendFailure(Component.literal("SCP-2845 only falls in the overworld"));
            return 0;
        }
        Vec3 Around = Source.getPosition();
        Optional<BlockPos> Site = Scp002Impact.chooseSite(World, Around, CometNear, CometFar, World.random);
        if (Site.isEmpty()) {
            Source.sendFailure(Component.literal("No open ground for SCP-2845 nearby"));
            return 0;
        }
        Scp2845Comet.launch(World, Site.get(), Around, null);
        Scp2845Arrival.keepLoaded(World, Site.get());
        Source.sendSuccess(() -> Component.literal("SCP-2845 falling toward " + Site.get().toShortString()), true);
        return 1;
    }

    private static int strike(CommandSourceStack Source, BlockPos Pos) {
        ServerLevel World = Source.getLevel();
        if (!Scp002Strikes.fallsIn(World)) {
            Source.sendFailure(Component.literal("SCP-2845 only falls in the overworld"));
            return 0;
        }
        BlockPos Ground = new BlockPos(Pos.getX(), Scp2845Crater.groundY(World, Pos.getX(), Pos.getZ()), Pos.getZ());
        Scp2845Comet.launch(World, Ground, Source.getPosition(), null);
        Scp2845Arrival.keepLoaded(World, Ground);
        Source.sendSuccess(() -> Component.literal("SCP-2845 falling toward " + Ground.toShortString()), true);
        return 1;
    }

    private static int lapse(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        int Count = 0;
        for (Scp2845 Deer : World.getEntitiesOfClass(Scp2845.class, new AABB(BlockPos.containing(Source.getPosition())).inflate(LapseReach))) {
            if (Deer.isContained()) {
                Deer.feed(World, World.getGameTime() - Scp2845.UpkeepTicks - 1L);
                Count++;
            }
        }
        int Total = Count;
        Source.sendSuccess(() -> Component.literal("Lapsed the upkeep of " + Total + " contained SCP-2845-1"), true);
        return Count;
    }

    private static int status(CommandSourceStack Source) {
        Map<UUID, Scp2845Records.Entry> All = Scp2845Records.get(Source.getServer()).all();
        Source.sendSuccess(() -> Component.literal("SCP-2845-1 records: " + All.size()), false);
        for (Map.Entry<UUID, Scp2845Records.Entry> Each : All.entrySet()) {
            Scp2845Records.Entry Row = Each.getValue();
            String Stone = Row.Stone() == null ? "none" : Row.Stone().toShortString();
            Source.sendSuccess(() -> Component.literal(" " + Each.getKey() + " " + Row.Phase() + " in " + Row.Dimension() + " stone " + Stone), false);
        }
        return All.size();
    }
}
