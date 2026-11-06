package net.rai.scp_unleashed.scp372;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp372Commands {
    public static final String Prefix = "commands.scp_unleashed.scp372.";
    public static final int AnchorSearch = 16;

    private Scp372Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp372")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("glimpse")
                                .executes(Context -> glimpse(Context.getSource(), Context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(Context -> glimpse(Context.getSource(), EntityArgument.getPlayer(Context, "player")))))
                        .then(Commands.literal("anchor").executes(Context -> anchor(Context.getSource())))
                        .then(Commands.literal("release").executes(Context -> release(Context.getSource())))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    private static int glimpse(CommandSourceStack Source, ServerPlayer Target) {
        if (!Scp372Glimpses.force(Target)) {
            Source.sendFailure(Component.translatable(Prefix + "glimpse.held"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "glimpse.success", Target.getDisplayName()), true);
        return 1;
    }

    public static Optional<Scp372AnchorBlockEntity> nearestAnchor(ServerLevel World, Vec3 Origin) {
        BlockPos Centre = BlockPos.containing(Origin);
        Scp372AnchorBlockEntity Best = null;
        double Closest = Double.MAX_VALUE;
        for (BlockPos Pos : BlockPos.betweenClosed(Centre.offset(-AnchorSearch, -AnchorSearch, -AnchorSearch), Centre.offset(AnchorSearch, AnchorSearch, AnchorSearch))) {
            if (!World.isLoaded(Pos) || !(World.getBlockEntity(Pos) instanceof Scp372AnchorBlockEntity Anchor)) {
                continue;
            }
            double Distance = Pos.distToCenterSqr(Origin);
            if (Distance < Closest) {
                Closest = Distance;
                Best = Anchor;
            }
        }
        return Optional.ofNullable(Best);
    }

    private static int anchor(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Optional<Scp372AnchorBlockEntity> Found = nearestAnchor(World, Source.getPosition());
        if (Found.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "anchor.none", AnchorSearch));
            return 0;
        }
        Scp372AnchorBlockEntity Anchor = Found.get();
        BlockPos Pos = Anchor.getBlockPos();
        if (!Anchor.isPowered()) {
            Source.sendFailure(Component.translatable(Prefix + "anchor.unpowered", Pos.getX(), Pos.getY(), Pos.getZ()));
            return 0;
        }
        if (!Anchor.begin(World)) {
            Source.sendFailure(Component.translatable(Prefix + "anchor.busy", Pos.getX(), Pos.getY(), Pos.getZ()));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "anchor.success", Pos.getX(), Pos.getY(), Pos.getZ()), true);
        return 1;
    }

    private static int release(CommandSourceStack Source) {
        if (!Scp372Anchoring.release(Source.getServer())) {
            Source.sendFailure(Component.translatable(Prefix + "release.none"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "release.success"), true);
        return 1;
    }

    private static int status(CommandSourceStack Source) {
        Scp372State State = Scp372State.get(Source.getServer());
        long Now = Source.getServer().overworld().getGameTime();
        String Held = State.held().map(Hold -> Hold.Dimension().location() + " " + Hold.Anchor().toShortString()).orElse("-");
        String Creature = Scp372Anchoring.creature(Source.getServer()).map(Found -> (int) Found.getX() + " " + (int) Found.getY() + " " + (int) Found.getZ()).orElse("-");
        long Resume = Math.max(0L, State.resumeAt() - Now) / 20L;
        Source.sendSuccess(() -> Component.translatable(Prefix + "status", Held, Creature, Resume, State.unlockedCount(), State.holds(), State.escapes()), false);
        ServerPlayer User = Source.getPlayer();
        if (User != null) {
            int Left = Scp372Glimpses.remaining(User.getUUID());
            String Next = Left < 0 ? "-" : String.valueOf(Math.round(Left / (20.0f * Scp372Glimpses.rate(User))));
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.player", Scp372Unlock.isUnlocked(User), Next, Scp372Glimpses.sentTo(User.getUUID()),
                    Scp372Glimpses.light(User)), false);
        }
        return 1;
    }
}
