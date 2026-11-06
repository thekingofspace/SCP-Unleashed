package net.rai.scp_unleashed.seasonal;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class SeasonCommands {
    public static final String Prefix = "commands.scp_unleashed.season.";

    private SeasonCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        LiteralArgumentBuilder<CommandSourceStack> Root = Commands.literal("season").requires(Source -> Source.hasPermission(2));
        Root.executes(Context -> status(Context.getSource()));
        for (Season Each : Season.values()) {
            LiteralArgumentBuilder<CommandSourceStack> Branch = Commands.literal(Each.getKey()).executes(Context -> status(Context.getSource()));
            for (SeasonMode Mode : SeasonMode.values()) {
                Branch.then(Commands.literal(Mode.getKey()).executes(Context -> set(Context.getSource(), Each, Mode)));
            }
            Root.then(Branch);
        }
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId).then(Root));
    }

    private static Component name(Season Which) {
        return Component.translatable(Which.getLangKey());
    }

    private static Component mode(SeasonMode Mode) {
        return Component.translatable(Prefix + "mode." + Mode.getKey());
    }

    public static Component line(Season Which, int Mask) {
        boolean On = Which.in(Mask);
        Component State = Component.translatable(Prefix + (On ? "active" : "inactive")).withStyle(On ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        SeasonMode Session = Seasons.forced(Which);
        SeasonMode Config = SeasonConfig.mode(Which);
        Component Reason;
        if (Session != SeasonMode.Auto) {
            Reason = Component.translatable(Prefix + "reason.session", mode(Session));
        } else if (Config != SeasonMode.Auto) {
            Reason = Component.translatable(Prefix + "reason.config", mode(Config));
        } else {
            Reason = Component.translatable(Prefix + "reason.calendar", Which.window());
        }
        return Component.translatable(Prefix + "line", name(Which), State, Reason);
    }

    private static int status(CommandSourceStack Source) {
        SeasonSync.update(Source.getServer());
        int Mask = Seasons.serverMask();
        Source.sendSuccess(() -> Component.translatable(Prefix + "date", Seasons.today().toString()), false);
        for (Season Each : Season.values()) {
            Source.sendSuccess(() -> line(Each, Mask), false);
        }
        return Integer.bitCount(Mask);
    }

    private static int set(CommandSourceStack Source, Season Which, SeasonMode Mode) {
        Seasons.force(Which, Mode);
        SeasonSync.update(Source.getServer());
        int Mask = Seasons.serverMask();
        if (Mode == SeasonMode.Auto) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "cleared", name(Which)), true);
        } else {
            Source.sendSuccess(() -> Component.translatable(Prefix + "set", name(Which), mode(Mode)), true);
        }
        Source.sendSuccess(() -> line(Which, Mask), false);
        return Which.in(Mask) ? 1 : 0;
    }
}
