package net.rai.scp_unleashed.scp1730.puzzle;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Puzzles;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp1730PuzzleEvents {
    private Scp1730PuzzleEvents() {
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post Event) {
        if (Event.getServer().getTickCount() % Scp1730PuzzleDirector.TickEvery == 0) {
            Scp1730PuzzleDirector.tick(Event.getServer());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            for (DeferredItem<?> Entry : Scp1730PuzzleRegistry.items()) {
                Event.accept(new ItemStack(Entry.get()));
            }
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            for (DeferredItem<?> Entry : Scp1730PuzzleRegistry.blockItems()) {
                Event.accept(new ItemStack(Entry.get()));
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp1730").requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("puzzle").executes(Scp1730PuzzleEvents::status)
                                .then(Commands.literal("solve").then(Commands.argument("zone", IntegerArgumentType.integer(0, Scp1730Puzzles.Zones - 1))
                                        .executes(Scp1730PuzzleEvents::solve))))));
    }

    private static int status(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = Scp1730World.level(Context.getSource().getServer());
        if (World == null) {
            Context.getSource().sendFailure(Component.literal("The SCP-1730 facility is not loaded."));
            return 0;
        }
        for (String Line : Scp1730PuzzleDirector.status(World)) {
            Context.getSource().sendSuccess(() -> Component.literal(Line), false);
        }
        return 1;
    }

    private static int solve(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = Scp1730World.level(Context.getSource().getServer());
        int Zone = IntegerArgumentType.getInteger(Context, "zone");
        if (World == null || !Scp1730PuzzleDirector.forceSolve(World, Zone)) {
            Context.getSource().sendFailure(Component.literal("No SCP-1730 puzzle to solve in zone " + Zone + "."));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.literal("SCP-1730 zone " + Zone + " puzzle solved."), true);
        return 1;
    }
}
