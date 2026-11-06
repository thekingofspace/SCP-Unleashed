package net.rai.scp_unleashed.scp085;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.stranger.StrangerEncounters;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp085Commands {
    public static final String Prefix = "commands.scp_unleashed.scp085.";
    private static final List<String> Acts = List.of("wave", "note", "sign", "cat", "bird", "rabbit", "spot", "hop");

    private Scp085Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp085")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("where").executes(Context -> where(Context.getSource())))
                        .then(Commands.literal("call").executes(Context -> call(Context.getSource())))
                        .then(Commands.literal("home").executes(Context -> home(Context.getSource())))
                        .then(Commands.literal("kit").executes(Context -> kit(Context.getSource())))
                        .then(Commands.literal("reset").executes(Context -> reset(Context.getSource())))
                        .then(Commands.literal("act")
                                .then(Commands.argument("kind", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Acts, Builder))
                                        .executes(Context -> act(Context.getSource(), StringArgumentType.getString(Context, "kind")))))));
    }

    private static int where(CommandSourceStack Source) {
        Scp085Figure Figure = Scp085Figure.get(Source.getServer());
        GlobalPos Spot = Figure.spot();
        GlobalPos Home = Figure.home();
        if (Spot != null) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "where.sheet", describe(Spot), Spot.equals(Home)
                    ? Component.translatable(Prefix + "where.home") : Component.translatable(Prefix + "where.drawing")), false);
        } else if (Figure.bonded()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "where.item"), false);
        } else {
            Source.sendSuccess(() -> Component.translatable(Prefix + "where.none"), false);
        }
        if (Home != null) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "where.home_at", describe(Home)), false);
        }
        return 1;
    }

    private static Component describe(GlobalPos Where) {
        BlockPos Pos = Where.pos();
        return Component.literal(Pos.getX() + " " + Pos.getY() + " " + Pos.getZ() + " (" + Where.dimension().location() + ")");
    }

    @Nullable
    private static BlockPos lookedAt(ServerPlayer User) {
        Vec3 Eye = User.getEyePosition();
        Vec3 End = Eye.add(User.getViewVector(1.0f).scale(12.0));
        BlockHitResult Hit = User.level().clip(new ClipContext(Eye, End, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, User));
        if (Hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return User.level().getBlockEntity(Hit.getBlockPos()) instanceof Scp085SheetBlockEntity ? Hit.getBlockPos() : null;
    }

    private static int call(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        BlockPos Target = lookedAt(User);
        ServerLevel World = User.serverLevel();
        Scp085Figure Figure = Scp085Figure.get(World);
        if (Target == null || !Scp085Brain.validTarget(World, Target, Figure)) {
            Source.sendFailure(Component.translatable(Prefix + "no_sheet"));
            return 0;
        }
        GlobalPos Spot = Figure.spot();
        if (Spot != null && Spot.dimension() == World.dimension() && World.isLoaded(Spot.pos())
                && World.getBlockEntity(Spot.pos()) instanceof Scp085SheetBlockEntity Current && Current.isHere() && !Spot.pos().equals(Target)) {
            Scp085Brain.startHop(World, Spot.pos(), Current, World.getGameTime(), Target);
        } else {
            Figure.moveTo(World, Target);
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "call.success"), true);
        return 1;
    }

    private static int home(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        BlockPos Target = lookedAt(User);
        ServerLevel World = User.serverLevel();
        if (Target == null || !(World.getBlockState(Target).getBlock() instanceof Scp085HomeSheetBlock)) {
            Source.sendFailure(Component.translatable(Prefix + "no_home"));
            return 0;
        }
        Scp085Figure Figure = Scp085Figure.get(World);
        GlobalPos Old = Figure.home();
        if (Old != null && !Figure.isHome(World, Target)) {
            ServerLevel OldWorld = World.getServer().getLevel(Old.dimension());
            if (OldWorld != null) {
                Figure.homeRemoved(OldWorld, Old.pos());
            }
        }
        Scp085Brain.homePlaced(World, Target);
        Source.sendSuccess(() -> Component.translatable(Prefix + "home.success"), true);
        return 1;
    }

    private static int kit(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        StrangerEncounters.give(User, new ItemStack(Scp085Registry.HomeSheetItem.get()));
        for (Scp085Scene Scene : Scp085Scene.Drawings) {
            StrangerEncounters.give(User, Scp085DrawingBlock.stackOf(Scene, 1));
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "kit.success"), true);
        return 1;
    }

    private static int reset(CommandSourceStack Source) {
        Scp085Figure.get(Source.getServer()).reset(Source.getServer());
        Source.sendSuccess(() -> Component.translatable(Prefix + "reset.success"), true);
        return 1;
    }

    private static int act(CommandSourceStack Source, String Kind) {
        Scp085Figure Figure = Scp085Figure.get(Source.getServer());
        GlobalPos Spot = Figure.spot();
        ServerLevel World = Spot == null ? null : Source.getServer().getLevel(Spot.dimension());
        if (Spot == null || World == null || !World.isLoaded(Spot.pos()) || !(World.getBlockEntity(Spot.pos()) instanceof Scp085SheetBlockEntity Sheet) || !Sheet.isHere()) {
            Source.sendFailure(Component.translatable(Prefix + "act.absent"));
            return 0;
        }
        if (!Acts.contains(Kind)) {
            Source.sendFailure(Component.translatable(Prefix + "act.unknown", Kind));
            return 0;
        }
        if (!Scp085Brain.force(World, Spot.pos(), Sheet, Kind)) {
            Source.sendFailure(Component.translatable(Prefix + "act.failed", Kind));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "act.success", Kind), true);
        return 1;
    }
}
