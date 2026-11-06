package net.rai.scp_unleashed.scp330;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class Scp330Commands {
    public static final String Prefix = "commands.scp_unleashed.scp330.";

    private Scp330Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp330")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("greed")
                                .executes(Context -> greed(Context.getSource(), List.of(Context.getSource().getEntityOrException())))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(Context -> greed(Context.getSource(), EntityArgument.getEntities(Context, "targets")))))
                        .then(Commands.literal("cure")
                                .executes(Context -> cure(Context.getSource(), List.of(Context.getSource().getEntityOrException())))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(Context -> cure(Context.getSource(), EntityArgument.getEntities(Context, "targets")))))
                        .then(Commands.literal("forget")
                                .executes(Context -> forget(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(Context -> forget(Context.getSource(), EntityArgument.getPlayers(Context, "targets")))))
                        .then(Commands.literal("candy")
                                .then(Commands.argument("colour", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Scp330Colour.values()).map(Scp330Colour::id), Builder))
                                        .executes(Context -> candy(Context, List.of(Context.getSource().getPlayerOrException())))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(Context -> candy(Context, EntityArgument.getPlayers(Context, "targets"))))))));
    }

    private static int greed(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Each : Targets) {
            if (Each instanceof LivingEntity Living && Living.isAlive()) {
                Scp330Greed.afflict(Living);
                Count++;
            }
        }
        int Total = Count;
        Source.sendSuccess(() -> Component.translatable(Prefix + "greed", Total), true);
        return Count;
    }

    private static int cure(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Each : Targets) {
            if (Each instanceof LivingEntity Living && Scp330Greed.cure(Living)) {
                Count++;
            }
        }
        int Total = Count;
        Source.sendSuccess(() -> Component.translatable(Prefix + "cure", Total), true);
        return Count;
    }

    private static int forget(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Records = 0;
        List<Scp330BowlBlockEntity> Bowls = Scp330Bowls.all();
        for (ServerPlayer Target : Targets) {
            for (Scp330BowlBlockEntity Bowl : Bowls) {
                if (Bowl.forget(Target.getUUID())) {
                    Records++;
                }
                if (!Scp330Taking.takerId(Target).equals(Target.getUUID()) && Bowl.forget(Scp330Taking.takerId(Target))) {
                    Records++;
                }
            }
        }
        int Total = Records;
        int Loaded = Bowls.size();
        Source.sendSuccess(() -> Component.translatable(Prefix + "forget", Targets.size(), Total, Loaded), true);
        return Records;
    }

    private static int candy(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        String Name = StringArgumentType.getString(Context, "colour");
        Scp330Colour Colour = Scp330Colour.byId(Name);
        if (Colour == null) {
            Source.sendFailure(Component.translatable(Prefix + "unknown_colour", Name));
            return 0;
        }
        for (ServerPlayer Target : Targets) {
            ItemStack Candy = new ItemStack(Scp330Registry.candy(Colour));
            if (!Target.getInventory().add(Candy) && !Candy.isEmpty()) {
                Target.drop(Candy, false);
            }
            Source.sendSuccess(() -> Component.translatable(Prefix + "candy", Target.getDisplayName(), Component.translatable(Scp330Registry.candy(Colour).getDescriptionId())), true);
        }
        return Targets.size();
    }
}
