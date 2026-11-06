package net.rai.scp_unleashed.scp093;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class Scp093Commands {
    public static final String Prefix = "commands.scp_unleashed.scp093.";
    public static final int LocateRadius = 100;
    public static final double SummonDistance = 10.0;

    private Scp093Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp093")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("enter").executes(Scp093Commands::enter))
                        .then(Commands.literal("leave").executes(Scp093Commands::leave))
                        .then(Commands.literal("vial").executes(Context -> vial(Context, 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 16)).executes(Context -> vial(Context, IntegerArgumentType.getInteger(Context, "count")))))
                        .then(Commands.literal("doses").executes(Scp093Commands::showDoses)
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0f, Scp093Tears.Lethal)).executes(Scp093Commands::doses)))
                        .then(Commands.literal("summon").executes(Scp093Commands::summon))
                        .then(Commands.literal("locate")
                                .then(Commands.literal("city").executes(Context -> locate(Context, Scp093Registry.City, "city")))
                                .then(Commands.literal("shack").executes(Context -> locate(Context, Scp093Registry.Shack, "shack"))))
                        .then(Commands.literal("status").executes(Scp093Commands::status))));
    }

    private static int enter(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (Scp093Wastes.inside(User)) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "already_inside"));
            return 0;
        }
        if (Scp093Wastes.level(User.server) == null) {
            Context.getSource().sendFailure(Component.translatable("message.scp_unleashed.scp093.no_dimension"));
            return 0;
        }
        Scp093Transit.enterDirect(User);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "enter"), true);
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (!Scp093Wastes.inside(User)) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "not_inside"));
            return 0;
        }
        Scp093Transit.leave(User);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "leave"), true);
        return 1;
    }

    private static int vial(CommandContext<CommandSourceStack> Context, int Count) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        ItemStack Stack = new ItemStack(Scp093Registry.Tears.get(), Count);
        if (!User.getInventory().add(Stack)) {
            User.drop(Stack, false);
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "vial", Count), true);
        return 1;
    }

    private static int showDoses(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        Scp093Doses Current = Scp093Tears.read(User);
        String Amount = String.format(Locale.ROOT, "%.2f", Current.Amount());
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "doses.show", Amount, String.format(Locale.ROOT, "%.0f", Scp093Tears.Lethal)), false);
        return 1;
    }

    private static int doses(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        float Amount = FloatArgumentType.getFloat(Context, "amount");
        Scp093Tears.setDoses(User, Amount);
        String Shown = String.format(Locale.ROOT, "%.2f", Scp093Tears.read(User).Amount());
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "doses.set", Shown), true);
        return 1;
    }

    private static int summon(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        ServerLevel World = User.serverLevel();
        Scp093Torso Torso = Scp093Registry.Torso.get().create(World);
        if (Torso == null) {
            return 0;
        }
        Vec3 Facing = Vec3.directionFromRotation(0.0f, User.getYRot());
        Vec3 Spot = Scp093Torso.roomNear(World, User.position().add(Facing.scale(SummonDistance)));
        Torso.moveTo(Spot.x, Spot.y, Spot.z, Mth.wrapDegrees(User.getYRot() + 180.0f), 0.0f);
        Torso.finalizeSpawn(World, World.getCurrentDifficultyAt(Torso.blockPosition()), MobSpawnType.COMMAND, null);
        World.addFreshEntity(Torso);
        Torso.beginRise();
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "summon"), true);
        return 1;
    }

    private static int locate(CommandContext<CommandSourceStack> Context, ResourceKey<Structure> Key, String Label) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        ServerLevel Wastes = Scp093Wastes.level(User.server);
        if (Wastes == null) {
            Context.getSource().sendFailure(Component.translatable("message.scp_unleashed.scp093.no_dimension"));
            return 0;
        }
        Holder<Structure> Found = Wastes.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(Key).orElse(null);
        if (Found == null) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "locate.none", Component.translatable(Prefix + "locate." + Label)));
            return 0;
        }
        BlockPos Centre = Scp093Wastes.inside(User) ? User.blockPosition() : BlockPos.containing(User.getX(), 72, User.getZ());
        Pair<BlockPos, Holder<Structure>> Result = Wastes.getChunkSource().getGenerator().findNearestMapStructure(Wastes, HolderSet.direct(Found), Centre, LocateRadius, false);
        if (Result == null) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "locate.none", Component.translatable(Prefix + "locate." + Label)));
            return 0;
        }
        BlockPos Where = Result.getFirst();
        int Gap = Mth.floor(Math.sqrt(Centre.distToCenterSqr(Where.getX(), Centre.getY(), Where.getZ())));
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "locate.found", Component.translatable(Prefix + "locate." + Label), Where.getX(), Where.getZ(), Gap), false);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        Scp093World.Origin From = Scp093Transit.originOf(User);
        Component Where = From == null ? Component.translatable(Prefix + "status.no_origin")
                : From.Gateway() == null ? Component.translatable(Prefix + "status.origin_spot", From.Dimension().location().toString())
                : Component.translatable(Prefix + "status.origin_mirror", From.Gateway().getX(), From.Gateway().getY(), From.Gateway().getZ(), From.Dimension().location().toString());
        String Amount = String.format(Locale.ROOT, "%.2f", Scp093Tears.read(User).Amount());
        int Gates = Scp093World.get(User.server).gates().size();
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "status", Scp093Wastes.inside(User) ? Component.translatable(Prefix + "status.inside")
                : Component.translatable(Prefix + "status.outside"), Where, Amount, Gates), false);
        return 1;
    }
}
