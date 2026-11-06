package net.rai.scp_unleashed.scp143;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp143Commands {
    public static final double AimReach = 32.0;
    public static final int ShedRadius = 16;
    public static final int ShedCount = 24;

    private Scp143Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp143")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("locate").executes(Context -> locate(Context.getSource())))
                        .then(Commands.literal("place").then(Commands.literal("here").executes(Context -> place(Context.getSource()))))
                        .then(Commands.literal("shed").executes(Context -> shed(Context.getSource())))
                        .then(Commands.literal("grow").executes(Context -> grow(Context.getSource())))));
    }

    private static int locate(CommandSourceStack Source) {
        Scp143World Data = Scp143World.get(Source.getServer());
        BlockPos Site = Data.site();
        if (Site == null) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp143.locate.none"), false);
            return 0;
        }
        Vec3 From = Source.getPosition();
        int Distance = (int) Math.round(Math.hypot(From.x - (Site.getX() + 1), From.z - (Site.getZ() + 1)));
        String Key = Data.isBuilt() ? "commands.scp_unleashed.scp143.locate.built" : "commands.scp_unleashed.scp143.locate.planned";
        Source.sendSuccess(() -> Component.translatable(Key, Site.getX(), Site.getY(), Site.getZ(), Distance), false);
        return 1;
    }

    private static int place(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        if (World.dimension() != Level.OVERWORLD) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp143.place.overworld"));
            return 0;
        }
        Vec3 From = Source.getPosition();
        int X = (int) Math.floor(From.x);
        int Z = (int) Math.floor(From.z);
        BlockPos Base = new BlockPos(X, Scp143Trees.ground(World, X, Z) + 1, Z);
        if (World.isOutsideBuildHeight(Base) || World.isOutsideBuildHeight(Base.above(Scp143Trees.originalBounds()[4] + 1))) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp143.place.room"));
            return 0;
        }
        Scp143Sites.placeHere(World, Base);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp143.place.success", Base.getX(), Base.getY(), Base.getZ()), true);
        return 1;
    }

    private static int shed(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        BlockPos Centre = BlockPos.containing(Source.getPosition());
        int Released = 0;
        BlockPos Nearest = null;
        double Best = Double.MAX_VALUE;
        for (BlockPos Pos : BlockPos.betweenClosed(Centre.offset(-ShedRadius, -4, -ShedRadius), Centre.offset(ShedRadius, ShedRadius, ShedRadius))) {
            BlockState State = World.getBlockState(Pos);
            if (State.getBlock() instanceof Scp143LeavesBlock) {
                double Distance = Pos.distSqr(Centre);
                if (Distance < Best) {
                    Best = Distance;
                    Nearest = Pos.immutable();
                }
            }
        }
        if (Nearest == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp143.shed.none", ShedRadius));
            return 0;
        }
        for (int Index = 0; Index < ShedCount; Index++) {
            BlockPos Leaf = Nearest.offset(World.getRandom().nextInt(7) - 3, World.getRandom().nextInt(3) - 1, World.getRandom().nextInt(7) - 3);
            if (World.getBlockState(Leaf).getBlock() instanceof Scp143LeavesBlock
                    && Scp143Petals.emit(World, Leaf.getX() + 0.5, Leaf.getY() - 0.02, Leaf.getZ() + 0.5, World.getRandom(), true)) {
                Released++;
            }
        }
        int Count = Released;
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp143.shed.success", Count), true);
        return Count;
    }

    private static int grow(CommandSourceStack Source) throws CommandSyntaxException {
        ServerLevel World = Source.getLevel();
        BlockPos Target = BlockPos.containing(Source.getPosition());
        if (Source.getEntity() instanceof ServerPlayer User) {
            Vec3 Eye = User.getEyePosition();
            BlockHitResult Hit = World.clip(new ClipContext(Eye, Eye.add(User.getLookAngle().scale(AimReach)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, User));
            if (Hit.getType() == HitResult.Type.BLOCK) {
                BlockPos Looked = Hit.getBlockPos();
                Target = World.getBlockState(Looked).getBlock() instanceof Scp143SaplingBlock ? Looked : Looked.relative(Hit.getDirection());
            }
        }
        BlockPos Spot = Target;
        if (!(World.getBlockState(Spot).getBlock() instanceof Scp143SaplingBlock)) {
            if (!World.getBlockState(Spot).canBeReplaced()) {
                Source.sendFailure(Component.translatable("commands.scp_unleashed.scp143.grow.blocked"));
                return 0;
            }
            World.setBlock(Spot, Scp143Registry.Sapling.get().defaultBlockState(), 3);
        }
        if (!Scp143Trees.growYoung(World, Spot, World.getRandom().nextLong())) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp143.grow.blocked"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp143.grow.success", Spot.getX(), Spot.getY(), Spot.getZ()), true);
        return 1;
    }
}
