package net.rai.scp_unleashed.scp4845;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp4845Commands {
    private Scp4845Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp4845")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("where").executes(Context -> where(Context.getSource())))
                        .then(Commands.literal("build").executes(Context -> build(Context.getSource())))
                        .then(Commands.literal("restaff").executes(Context -> restaff(Context.getSource())))
                        .then(Commands.literal("forget").executes(Context -> forget(Context.getSource())))));
    }

    private static int where(CommandSourceStack Source) {
        Scp4845Site Site = Scp4845Restaurant.current(Source.getServer());
        if (Site == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp4845.none"));
            return 0;
        }
        BlockPos Door = Site.Entrance.getFirst();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp4845.where", Door.getX(), Door.getY(), Door.getZ(), Site.Dimension.location().toString()), false);
        return 1;
    }

    private static int build(CommandSourceStack Source) {
        ServerPlayer User = Source.getPlayer();
        if (User == null || !Scp4845Restaurant.isMoon(User.level())) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp4845.not_moon"));
            return 0;
        }
        if (Scp4845State.get(Source.getServer()).built()) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp4845.exists"));
            return 0;
        }
        ServerLevel World = User.serverLevel();
        BlockPos Centre = User.blockPosition().relative(User.getDirection(), Scp4845Restaurant.HalfSpan + 4);
        BlockPos Ground = new BlockPos(Centre.getX(), Scp4845Restaurant.ground(World, Centre.getX(), Centre.getZ()), Centre.getZ());
        Scp4845Site Site = Scp4845Restaurant.place(World, Ground, Scp4845Restaurant.facing(Ground, User.position()));
        BlockPos Door = Site.Entrance.getFirst();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp4845.built", Door.getX(), Door.getY(), Door.getZ()), true);
        return 1;
    }

    private static int restaff(CommandSourceStack Source) {
        Scp4845Site Site = Scp4845Restaurant.current(Source.getServer());
        ServerLevel World = Site == null ? null : Source.getServer().getLevel(Site.Dimension);
        if (Site == null || World == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp4845.none"));
            return 0;
        }
        int Hired = Scp4845Staffing.restaff(World, Site);
        Scp4845State.get(Source.getServer()).markStaffed();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp4845.restaffed", Hired), true);
        return Hired;
    }

    private static int forget(CommandSourceStack Source) {
        Scp4845State.get(Source.getServer()).forget();
        Scp4845Restaurant.forget();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp4845.forgot"), true);
        return 1;
    }
}
