package net.rai.scp_unleashed.scp148;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;

public final class Scp148Commands {
    public static final String Prefix = "commands.scp_unleashed.scp148.";

    private Scp148Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp148")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("status").executes(Context -> status(Context, List.of(Context.getSource().getEntityOrException())))
                                .then(Commands.argument("targets", EntityArgument.entities()).executes(Context -> status(Context, EntityArgument.getEntities(Context, "targets")))))
                        .then(Commands.literal("give").executes(Context -> give(Context, List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players()).executes(Context -> give(Context, EntityArgument.getPlayers(Context, "targets")))))));
    }

    private static int status(CommandContext<CommandSourceStack> Context, Collection<? extends Entity> Targets) {
        CommandSourceStack Source = Context.getSource();
        int Placed = Scp148Index.count(Source.getLevel());
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.index", Placed, Source.getLevel().dimension().location().toString(),
                (int) Scp148Shield.Radius), false);
        int Powered = Scp148Index.countPowered(Source.getLevel());
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.blockers", Powered, Source.getLevel().dimension().location().toString(),
                (int) Scp148Shield.PoweredRadius), false);
        int Shielded = 0;
        for (Entity Target : Targets) {
            if (!(Target instanceof LivingEntity Living)) {
                Source.sendFailure(Component.translatable(Prefix + "status.not_living", Target.getDisplayName()));
                continue;
            }
            Component Line = describe(Living);
            Source.sendSuccess(() -> Line, false);
            if (Scp148Shield.isShielded(Living)) {
                Shielded++;
            }
        }
        return Shielded;
    }

    private static Component describe(LivingEntity Living) {
        boolean Helmet = Scp148Shield.wearsHelmet(Living);
        BlockPos Ward = Scp148Shield.ward(Living);
        if (Ward != null) {
            double Distance = Living.getEyePosition().distanceTo(Vec3.atCenterOf(Ward));
            return Component.translatable(Prefix + (Helmet ? "status.warded_helmet" : "status.warded"), Living.getDisplayName(),
                    Ward.getX(), Ward.getY(), Ward.getZ(), String.format("%.1f", Distance));
        }
        BlockPos Blocker = Scp148Shield.poweredWard(Living);
        if (Blocker != null) {
            double Distance = Living.getEyePosition().distanceTo(Vec3.atCenterOf(Blocker));
            return Component.translatable(Prefix + (Helmet ? "status.blocked_helmet" : "status.blocked"), Living.getDisplayName(),
                    Blocker.getX(), Blocker.getY(), Blocker.getZ(), String.format("%.1f", Distance));
        }
        if (Helmet) {
            return Component.translatable(Prefix + "status.helmet", Living.getDisplayName());
        }
        if (!Scp148Shield.canBeWarded(Living)) {
            return Component.translatable(Prefix + "status.not_person", Living.getDisplayName());
        }
        return Component.translatable(Prefix + "status.exposed", Living.getDisplayName());
    }

    private static int give(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) throws CommandSyntaxException {
        for (ServerPlayer Target : Targets) {
            ItemStack Delivery = new ItemStack(Scp148Registry.Ingot.get(), Scp148Registry.DeliveryIngots);
            if (!Target.getInventory().add(Delivery) || !Delivery.isEmpty()) {
                Target.drop(Delivery, false);
            }
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "give", Scp148Registry.DeliveryIngots, Target.getDisplayName()), true);
        }
        return Targets.size();
    }
}
