package net.rai.scp_unleashed.scp5001.xk;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.worldend.WorldEndConfig;
import net.rai.scp_unleashed.worldend.WorldEndRules;
import net.rai.scp_unleashed.worldend.WorldEnding;

import java.util.Locale;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class XkCommands {
    public static final String Prefix = "commands.scp_unleashed.scp5001_xk.";

    private XkCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp5001")
                        .then(Commands.literal("xk")
                                .requires(Source -> Source.hasPermission(2))
                                .then(Commands.literal("status").executes(XkCommands::status))
                                .then(Commands.literal("start").requires(Source -> Source.hasPermission(4)).executes(XkCommands::start))
                                .then(Commands.literal("skip").requires(Source -> Source.hasPermission(4)).executes(XkCommands::skip))
                                .then(Commands.literal("cancel").requires(Source -> Source.hasPermission(4)).executes(XkCommands::cancel)))));
    }

    private static int start(CommandContext<CommandSourceStack> Context) {
        Scp5001Xk.start(Context.getSource().getServer());
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "start"), true);
        return 1;
    }

    private static int skip(CommandContext<CommandSourceStack> Context) {
        String Result = Scp5001Xk.skip(Context.getSource().getServer());
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "skip." + Result), true);
        return 1;
    }

    private static int cancel(CommandContext<CommandSourceStack> Context) {
        Scp5001Xk.cancel(Context.getSource().getServer());
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "cancel"), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> Context) {
        MinecraftServer Server = Context.getSource().getServer();
        Scp5001State Shared = Scp5001State.get(Server);
        XkState State = XkState.get(Server);
        YaldabaothFlight Flight = State.flight();
        Vec3 Head = Flight.head();
        String Where = Flight.present() ? String.format(Locale.ROOT, "%.0f %.0f %.0f", Head.x, Head.y, Head.z) : "-";
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "status", Shared.phase().name(), State.stage().name(), State.firstElapsed(),
                State.secondElapsed(), String.format(Locale.ROOT, "%.3f", State.sky()), Flight.mode().name(), Where, WorldEnding.stage(Server).name(),
                String.valueOf(WorldEndRules.endings(Server)), String.valueOf(WorldEndConfig.allowDeletion())), false);
        return 1;
    }
}
