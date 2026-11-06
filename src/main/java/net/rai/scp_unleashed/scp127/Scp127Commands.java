package net.rai.scp_unleashed.scp127;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunItem;

import java.util.Collection;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp127Commands {
    public static final String Prefix = "commands.scp_unleashed.scp127.";
    public static final int MaxKills = 100000;

    private Scp127Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp127")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("give")
                                .executes(Context -> give(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(Context -> give(Context.getSource(), EntityArgument.getPlayers(Context, "targets")))))
                        .then(Commands.literal("level")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, Scp127Levels.MaxLevel))
                                        .executes(Context -> level(Context.getSource(), IntegerArgumentType.getInteger(Context, "level")))))
                        .then(Commands.literal("kills")
                                .then(Commands.argument("kills", IntegerArgumentType.integer(0, MaxKills))
                                        .executes(Context -> kills(Context.getSource(), IntegerArgumentType.getInteger(Context, "kills")))))
                        .then(Commands.literal("rounds")
                                .then(Commands.argument("rounds", IntegerArgumentType.integer(0, Scp127Levels.capacity()))
                                        .executes(Context -> rounds(Context.getSource(), IntegerArgumentType.getInteger(Context, "rounds")))))
                        .then(Commands.literal("starve")
                                .executes(Context -> starve(Context.getSource())))));
    }

    private static int give(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        for (ServerPlayer Target : Targets) {
            ItemStack Stack = Scp127Item.create();
            if (!Target.getInventory().add(Stack)) {
                Target.drop(Stack, false);
            }
            Source.sendSuccess(() -> Component.translatable(Prefix + "give", Target.getDisplayName()), true);
        }
        return Targets.size();
    }

    private static ItemStack carried(ServerPlayer User) {
        for (InteractionHand Hand : InteractionHand.values()) {
            if (Scp127Item.is(User.getItemInHand(Hand))) {
                return User.getItemInHand(Hand);
            }
        }
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            ItemStack Stack = User.getInventory().getItem(Slot);
            if (Scp127Item.is(Stack)) {
                return Stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int level(CommandSourceStack Source, int Rank) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        ItemStack Stack = carried(User);
        if (Stack.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "missing", User.getDisplayName()));
            return 0;
        }
        int Before = Scp127Levels.rankOf(Stack);
        Stack.set(Scp127Registry.Rank.get(), Rank);
        Stack.set(Scp127Registry.Kills.get(), Scp127Levels.killsFor(Rank));
        if (Rank > Before) {
            Scp127Life.levelUp(User, Rank);
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "level", Scp127Levels.numeral(Rank)), true);
        return Rank;
    }

    private static int kills(CommandSourceStack Source, int Kills) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        ItemStack Stack = carried(User);
        if (Stack.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "missing", User.getDisplayName()));
            return 0;
        }
        int Before = Scp127Levels.rankOf(Stack);
        int Rank = Scp127Levels.rankForKills(Kills);
        Stack.set(Scp127Registry.Kills.get(), Kills);
        Stack.set(Scp127Registry.Rank.get(), Rank);
        if (Rank > Before) {
            Scp127Life.levelUp(User, Rank);
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "kills", Kills, Scp127Levels.numeral(Rank)), true);
        return Rank;
    }

    private static int rounds(CommandSourceStack Source, int Rounds) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        ItemStack Stack = carried(User);
        if (Stack.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "missing", User.getDisplayName()));
            return 0;
        }
        GunItem.setAmmo(Stack, Rounds);
        Stack.remove(Scp127Registry.Regrowth.get());
        Source.sendSuccess(() -> Component.translatable(Prefix + "rounds", Rounds, Scp127Levels.capacity()), true);
        return Rounds;
    }

    private static int starve(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        ItemStack Stack = carried(User);
        if (Stack.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "missing", User.getDisplayName()));
            return 0;
        }
        Scp127Life.used(Stack, User.level().getGameTime() - Scp127Life.HungerTicks);
        Source.sendSuccess(() -> Component.translatable(Prefix + "starve"), true);
        return 1;
    }
}
