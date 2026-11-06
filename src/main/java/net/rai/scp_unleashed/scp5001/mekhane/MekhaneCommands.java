package net.rai.scp_unleashed.scp5001.mekhane;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class MekhaneCommands {
    private MekhaneCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp5001")
                        .then(Commands.literal("mekhane")
                                .requires(Source -> Source.hasPermission(2))
                                .then(Commands.literal("status").executes(Context -> status(Context.getSource())))
                                .then(Commands.literal("fill").requires(Source -> Source.hasPermission(4)).executes(Context -> fill(Context.getSource())))
                                .then(Commands.literal("summon").requires(Source -> Source.hasPermission(4)).executes(Context -> summon(Context.getSource())))
                                .then(Commands.literal("site").requires(Source -> Source.hasPermission(4)).executes(Context -> site(Context.getSource())))
                                .then(Commands.literal("reset").requires(Source -> Source.hasPermission(4)).executes(Context -> reset(Context.getSource()))))));
    }

    private static int status(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        Scp5001State State = Scp5001State.get(Server);
        long Elapsed = Server.overworld().getGameTime() - Protocol.stageTick();
        String Text = String.format(java.util.Locale.ROOT, "Protocol %s for %d ticks, phase %s, iron %.1f/%d, electronics %.1f/%d, restarts %d%s",
                Protocol.stage().name(), Elapsed, State.phase().name(), Protocol.iron() / (double) MekhaneValues.Scale, MekhaneConfig.ironDemand(),
                Protocol.electronics() / (double) MekhaneValues.Scale, MekhaneConfig.electronicsDemand(), Protocol.restarts(), Protocol.lone() ? ", alone" : "");
        Source.sendSuccess(() -> Component.literal(Text), false);
        return 1;
    }

    private static int fill(CommandSourceStack Source) {
        MekhaneProtocol Protocol = MekhaneProtocol.get(Source.getServer());
        if (Protocol.stage() != MekhaneProtocol.Stage.Engaged) {
            Source.sendFailure(Component.literal("The emergency protocol is not engaged"));
            return 0;
        }
        Protocol.fill();
        Source.sendSuccess(() -> Component.literal("Emergency protocol demand filled"), true);
        return 1;
    }

    private static int summon(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        if (Protocol.stage() == MekhaneProtocol.Stage.Summon || Protocol.stage() == MekhaneProtocol.Stage.Duel) {
            Source.sendFailure(Component.literal("Mekhane is already answering"));
            return 0;
        }
        ServerLevel World = Server.overworld();
        Protocol.fill();
        MekhaneDirector.beginSummon(World, Protocol, Scp5001State.get(Server), World.getGameTime());
        Source.sendSuccess(() -> Component.literal("Mekhane summoned"), true);
        return 1;
    }

    private static int site(CommandSourceStack Source) {
        MekhaneProtocol Protocol = MekhaneProtocol.get(Source.getServer());
        BlockPos Here = BlockPos.containing(Source.getPosition());
        Protocol.emitter(Here);
        Source.sendSuccess(() -> Component.literal("Emergency protocol site set to " + Here.toShortString()), true);
        return 1;
    }

    private static int reset(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        ServerLevel World = Server.overworld();
        MekhaneDirector.abort(World, Protocol, World.getGameTime());
        Source.sendSuccess(() -> Component.literal("Emergency protocol reset"), true);
        return 1;
    }
}
