package net.rai.scp_unleashed.scp109;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Scp109Commands {
    public static final String Prefix = "commands.scp_unleashed.scp109.";
    public static final double LookReach = 8.0;

    private Scp109Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp109")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("tap").executes(Context -> report(Context, null))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(Context -> report(Context, BlockPosArgument.getLoadedBlockPos(Context, "pos")))))
                        .then(Commands.literal("seat").executes(Context -> seat(Context, null))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(Context -> seat(Context, BlockPosArgument.getLoadedBlockPos(Context, "pos")))))));
    }

    @Nullable
    private static BlockPos looked(CommandSourceStack Source) {
        if (!(Source.getEntity() instanceof ServerPlayer User)) {
            return null;
        }
        Vec3 Eye = User.getEyePosition();
        Vec3 End = Eye.add(User.getLookAngle().scale(LookReach));
        BlockHitResult Hit = User.level().clip(new ClipContext(Eye, End, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, User));
        return Hit.getType() == HitResult.Type.BLOCK ? Hit.getBlockPos() : null;
    }

    @Nullable
    private static Scp109TapBlockEntity find(CommandContext<CommandSourceStack> Context, @Nullable BlockPos Given) {
        BlockPos Pos = Given != null ? Given : looked(Context.getSource());
        if (Pos == null) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "no_target"));
            return null;
        }
        Scp109TapBlockEntity Tap = Scp109TapBlock.tapAt(Context.getSource().getLevel(), Pos);
        if (Tap == null) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "not_tap", Pos.getX(), Pos.getY(), Pos.getZ()));
        }
        return Tap;
    }

    private static int report(CommandContext<CommandSourceStack> Context, @Nullable BlockPos Given) {
        Scp109TapBlockEntity Tap = find(Context, Given);
        if (Tap == null) {
            return 0;
        }
        BlockPos Pos = Tap.getBlockPos();
        Component State = Component.translatable(Prefix + (Tap.hasCanteen() ? (Tap.isFlowing() ? "flowing" : "seated") : "empty"));
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "report", Pos.getX(), Pos.getY(), Pos.getZ(), State, Tap.getRate(),
                Scp109TapBlockEntity.MaxPerSecond, Tap.getTotal()), false);
        return Tap.hasCanteen() ? 1 : 0;
    }

    private static int seat(CommandContext<CommandSourceStack> Context, @Nullable BlockPos Given) {
        Scp109TapBlockEntity Tap = find(Context, Given);
        if (Tap == null) {
            return 0;
        }
        BlockPos Pos = Tap.getBlockPos();
        if (Tap.hasCanteen()) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "occupied", Pos.getX(), Pos.getY(), Pos.getZ()));
            return 0;
        }
        ServerLevel World = Context.getSource().getLevel();
        BlockState State = World.getBlockState(Pos);
        Scp109TapBlock.seat(World, Pos, State, Tap, new ItemStack(Scp109Registry.Canteen.get()));
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "seat", Pos.getX(), Pos.getY(), Pos.getZ()), true);
        return 1;
    }
}
