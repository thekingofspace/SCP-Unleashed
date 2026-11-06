package net.rai.scp_unleashed.scp107;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp107Commands {
    private Scp107Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        LiteralArgumentBuilder<CommandSourceStack> Rain = Commands.literal("rain");
        for (Scp107RainKind Kind : Scp107RainKind.values()) {
            Rain.then(Commands.literal(Kind.id())
                    .executes(Context -> rain(Context.getSource(), Kind, 0))
                    .then(Commands.argument("seconds", IntegerArgumentType.integer(1, Scp107Storm.MaxCommandSeconds))
                            .executes(Context -> rain(Context.getSource(), Kind, IntegerArgumentType.getInteger(Context, "seconds")))));
        }
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp107")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Rain)
                        .then(Commands.literal("stop").executes(Context -> stop(Context.getSource())))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    private static Component kindName(Scp107RainKind Kind) {
        return Component.translatable("commands.scp_unleashed." + Kind.key());
    }

    private static int rain(CommandSourceStack Source, Scp107RainKind Kind, int Seconds) {
        MinecraftServer Server = Source.getServer();
        int Ticks = Seconds > 0 ? Seconds * 20 : Scp107Storm.pourTicks(Server.overworld().random);
        Scp107Storm.begin(Server, Kind, Ticks, false);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp107.started", kindName(Kind), Ticks / 20), true);
        return 1;
    }

    private static int stop(CommandSourceStack Source) {
        Scp107Rain State = Scp107Rain.get(Source.getServer());
        Scp107RainKind Kind = State.kind();
        if (Kind == null || !Scp107Storm.end(Source.getServer(), true)) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp107.none"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp107.stopped", kindName(Kind)), true);
        return 1;
    }

    private static int status(CommandSourceStack Source) {
        Scp107Rain State = Scp107Rain.get(Source.getServer());
        Scp107RainKind Kind = State.kind();
        if (Kind == null || !State.active()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp107.none"), false);
            return 0;
        }
        int Seconds = State.remaining() / 20;
        int Strength = Math.round(State.strength() * 100.0f);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp107.status", kindName(Kind), Seconds, Strength), false);
        return 1;
    }
}
