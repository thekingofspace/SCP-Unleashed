package net.rai.scp_unleashed.scp001scarlet.court;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class CourtEvents {
    private CourtEvents() {
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            ScarletCourt.Instance.idle(World);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        ScarletCourt.Instance.forget();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scarletcourt")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("status").executes(CourtEvents::status))
                        .then(Commands.literal("begin").executes(CourtEvents::begin))
                        .then(Commands.literal("skip").executes(CourtEvents::skip))
                        .then(Commands.literal("reset").executes(CourtEvents::reset))
                        .then(Commands.literal("end").executes(CourtEvents::end))));
    }

    private static int status(CommandContext<CommandSourceStack> Context) {
        ScarletCourt Court = ScarletCourt.Instance;
        ServerLevel World = Context.getSource().getLevel();
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scarletcourt.status", Court.stage().name(), Court.daughtersRemaining(),
                Court.living(World), ScarletCourt.cap(Court.players())), false);
        return 1;
    }

    private static int begin(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = Context.getSource().getLevel();
        BlockPos Centre = BlockPos.containing(Context.getSource().getPosition());
        int Players = Math.max(1, World.players().size());
        ScarletCourt.Instance.begin(World, Centre, null, Players);
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scarletcourt.begin", ScarletCourt.Instance.daughtersRemaining()), true);
        return 1;
    }

    private static int skip(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = Context.getSource().getLevel();
        CourtDaughter Current = ScarletCourt.Instance.currentDaughter(World);
        if (Current == null) {
            Context.getSource().sendFailure(Component.translatable("commands.scp_unleashed.scarletcourt.none"));
            return 0;
        }
        for (CourtDaughter Each : ScarletCourt.Instance.daughters(World)) {
            Each.beginDying();
        }
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scarletcourt.skip"), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> Context) {
        ScarletCourt.Instance.reset(Context.getSource().getLevel());
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scarletcourt.reset", ScarletCourt.Instance.daughtersRemaining()), true);
        return 1;
    }

    private static int end(CommandContext<CommandSourceStack> Context) {
        ScarletCourt.Instance.end(Context.getSource().getLevel());
        Context.getSource().sendSuccess(() -> Component.translatable("commands.scp_unleashed.scarletcourt.end"), true);
        return 1;
    }
}
