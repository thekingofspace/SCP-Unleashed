package net.rai.scp_unleashed.scp323;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp323Commands {
    public static final String Prefix = "commands.scp_unleashed.scp323.";
    public static final double PlaceReach = 6.0;

    private Scp323Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp323")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("place").executes(Scp323Commands::placeAhead))
                        .then(targeted("transform", Scp323Commands::transform))
                        .then(targeted("retract", Scp323Commands::retract))
                        .then(targeted("status", Scp323Commands::status))
                        .then(targeted("clear", Scp323Commands::clear))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String Name, BiFunction<CommandSourceStack, Collection<? extends Entity>, Integer> Action) {
        return Commands.literal(Name)
                .executes(Context -> Action.apply(Context.getSource(), List.of(Context.getSource().getEntityOrException())))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(Context -> Action.apply(Context.getSource(), EntityArgument.getEntities(Context, "targets"))));
    }

    private static int placeAhead(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        Entity Looker = Source.getEntityOrException();
        Vec3 Eye = Looker.getEyePosition();
        Vec3 End = Eye.add(Looker.getViewVector(1.0f).scale(PlaceReach));
        BlockHitResult Hit = Source.getLevel().clip(new ClipContext(Eye, End, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Looker));
        Vec3 Spot = Hit.getType() == HitResult.Type.MISS ? End : Hit.getLocation().add(Vec3.atLowerCornerOf(Hit.getDirection().getNormal()).scale(0.2));
        if (Scp323Skull.place(Source.getLevel(), Spot, Looker.getYRot(), true) == null) {
            Source.sendFailure(Component.translatable(Prefix + "place.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "place.success", Math.round(Spot.x), Math.round(Spot.y), Math.round(Spot.z)), true);
        return 1;
    }

    private static int transform(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Target : Targets) {
            if (!(Target instanceof LivingEntity Host) || Target instanceof Scp323Beast) {
                Source.sendFailure(Component.translatable(Prefix + "not_host", Target.getDisplayName()));
                continue;
            }
            Scp323Bond Bond = Scp323Bond.of(Host);
            boolean Done;
            if (Bond.Stage() <= 0) {
                Done = Scp323Hosts.canHost(Host) && Scp323Hosts.fuse(Host);
            } else {
                Done = Bond.horned() && Scp323Hosts.transform(Host, false) != null;
            }
            if (Done) {
                Count++;
                Source.sendSuccess(() -> Component.translatable(Prefix + "transform.success", Target.getDisplayName()), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "transform.failed", Target.getDisplayName()));
            }
        }
        return Count;
    }

    private static int retract(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Target : Targets) {
            Scp323Beast Beast = beastOf(Source, Target);
            if (Beast == null || Beast.isReleased()) {
                Source.sendFailure(Component.translatable(Prefix + "retract.none", Target.getDisplayName()));
                continue;
            }
            Beast.beginRetract();
            Count++;
            Source.sendSuccess(() -> Component.translatable(Prefix + "retract.success", Target.getDisplayName()), true);
        }
        return Count;
    }

    private static int status(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Target : Targets) {
            if (Target instanceof Scp323Beast Beast) {
                Component Host = Beast.hostName().isEmpty() ? Component.translatable(Prefix + "status.nobody") : Component.literal(Beast.hostName());
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.beast", Target.getDisplayName(), Beast.stateKey(), Host, Beast.kills(),
                        Beast.firstEpisode() ? "first" : "nightly", Math.round(Beast.getHealth()), Math.round(Beast.getMaxHealth())), false);
                Count++;
                continue;
            }
            Scp323Bond Bond = Scp323Bond.of(Target);
            if (Bond.Stage() <= 0) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.unbound", Target.getDisplayName()), false);
                continue;
            }
            long Time = Scp323Night.time(Source.getServer());
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.host", Target.getDisplayName(), Bond.transformed() ? "transformed" : "horned",
                    Bond.FirstDone() ? "yes" : "no", Bond.LastNight(), Bond.Interrupted() ? "yes" : "no", Scp323Night.isNight(Time) ? "night" : "day",
                    Math.floorMod(Time, Scp323Night.DayLength)), false);
            Count++;
        }
        return Count;
    }

    private static int clear(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Target : Targets) {
            if (clearOne(Source, Target)) {
                Count++;
                Source.sendSuccess(() -> Component.translatable(Prefix + "clear.success", Target.getDisplayName()), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "clear.none", Target.getDisplayName()));
            }
        }
        return Count;
    }

    private static boolean clearOne(CommandSourceStack Source, Entity Target) {
        if (Target instanceof Scp323Beast Beast) {
            return clearBeast(Source, Beast);
        }
        if (!(Target instanceof LivingEntity Host)) {
            return false;
        }
        Scp323Bond Bond = Scp323Bond.of(Host);
        if (Bond.Stage() <= 0) {
            return false;
        }
        Scp323Beast Beast = Bond.transformed() ? Scp323Hosts.beast(Source.getServer(), Bond.Creature()) : null;
        if (Beast != null) {
            return clearBeast(Source, Beast);
        }
        Scp323Bond.clear(Host);
        if (Host instanceof ServerPlayer User) {
            Scp323Watch.stop(User);
        }
        return true;
    }

    private static boolean clearBeast(CommandSourceStack Source, Scp323Beast Beast) {
        if (!(Beast.level() instanceof ServerLevel World) || Beast.isReleased()) {
            return false;
        }
        Vec3 At = Beast.position();
        ServerPlayer User = Scp323Hosts.player(Source.getServer(), Beast.hostPlayer());
        if (User != null && Beast.getUUID().equals(Scp323Bond.of(User).Creature())) {
            Scp323Bond.clear(User);
            Scp323Watch.stop(User);
            User.teleportTo(World, At.x, At.y, At.z, Beast.getYRot(), 0.0f);
        } else if (Beast.storedBody() != null) {
            Scp323Hosts.restore(World, Beast.storedBody(), At, Beast.getYRot(), Scp323Bond.None);
        }
        Scp323Hosts.retracted(World, At);
        Beast.vanish();
        return true;
    }

    private static Scp323Beast beastOf(CommandSourceStack Source, Entity Target) {
        if (Target instanceof Scp323Beast Beast) {
            return Beast;
        }
        Scp323Bond Bond = Scp323Bond.of(Target);
        return Bond.transformed() ? Scp323Hosts.beast(Source.getServer(), Bond.Creature()) : null;
    }
}
