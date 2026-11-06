package net.rai.scp_unleashed.scp256;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

public final class Scp256Commands {
    public static final String Prefix = "commands.scp_unleashed.scp256.";
    public static final double LookReach = 8.0;

    private Scp256Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp256")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("type").executes(Context -> type(Context, null))
                                .then(Commands.argument("line", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Scp256Line.values()).map(Scp256Line::getId), Builder))
                                        .executes(Context -> type(Context, StringArgumentType.getString(Context, "line")))))
                        .then(Commands.literal("info").executes(Scp256Commands::info))));
    }

    @Nullable
    private static Scp256TypewriterBlockEntity looked(CommandContext<CommandSourceStack> Context) {
        if (!(Context.getSource().getEntity() instanceof ServerPlayer User)) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "no_target"));
            return null;
        }
        Vec3 Eye = User.getEyePosition();
        Vec3 End = Eye.add(User.getLookAngle().scale(LookReach));
        BlockHitResult Hit = User.level().clip(new ClipContext(Eye, End, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, User));
        Scp256TypewriterBlockEntity Typewriter = Hit.getType() == HitResult.Type.BLOCK ? Scp256TypewriterBlock.typewriterAt(User.level(), Hit.getBlockPos()) : null;
        if (Typewriter == null) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "no_target"));
        }
        return Typewriter;
    }

    private static int type(CommandContext<CommandSourceStack> Context, @Nullable String Wanted) {
        Scp256TypewriterBlockEntity Typewriter = looked(Context);
        if (Typewriter == null) {
            return 0;
        }
        ServerLevel World = Context.getSource().getLevel();
        BlockPos Pos = Typewriter.getBlockPos();
        if (Wanted == null) {
            if (!Typewriter.press(World, null)) {
                Context.getSource().sendFailure(Component.translatable(Prefix + "busy"));
                return 0;
            }
        } else {
            Optional<Scp256Line> Line = Scp256Line.byId(Wanted);
            if (Line.isEmpty()) {
                Context.getSource().sendFailure(Component.translatable(Prefix + "unknown", Wanted));
                return 0;
            }
            Typewriter.begin(World, Line.get(), World.getGameTime());
        }
        String Typed = Typewriter.getLine().map(Scp256Line::getId).orElse("?");
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "typed", Typed, Pos.getX(), Pos.getY(), Pos.getZ()), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> Context) {
        Scp256TypewriterBlockEntity Typewriter = looked(Context);
        if (Typewriter == null) {
            return 0;
        }
        ServerLevel World = Context.getSource().getLevel();
        long Now = World.getGameTime();
        String Line = Typewriter.getLine().map(Scp256Line::getId).orElse("-");
        Component State = Component.translatable(Prefix + (Typewriter.isTyping(Now) ? "typing" : "idle"));
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "info", Line, State, Typewriter.getUses(), Typewriter.getStreak(),
                Scp256Line.isNight(World) ? Component.translatable(Prefix + "night") : Component.translatable(Prefix + "day")), false);
        return 1;
    }
}
