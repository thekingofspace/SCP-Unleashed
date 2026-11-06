package net.rai.scp_unleashed.scp151;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Scp151Commands {
    public static final String Prefix = "commands.scp_unleashed.scp151.";
    public static final int ScanRadius = 48;

    private Scp151Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp151")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("give")
                                .executes(Context -> give(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(Context -> give(Context.getSource(), EntityArgument.getPlayers(Context, "targets")))))
                        .then(Commands.literal("status")
                                .executes(Context -> status(Context.getSource(), Context.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(Context -> status(Context.getSource(), EntityArgument.getPlayer(Context, "target")))))));
    }

    private static int give(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        for (ServerPlayer Target : Targets) {
            ItemStack Stack = new ItemStack(Scp151Registry.PaintingItem.get());
            if (!Target.getInventory().add(Stack)) {
                Target.drop(Stack, false);
            }
            Source.sendSuccess(() -> Component.translatable(Prefix + "give", Target.getDisplayName()), true);
        }
        return Targets.size();
    }

    private static int status(CommandSourceStack Source, ServerPlayer Target) throws CommandSyntaxException {
        Component Condition;
        if (Scp151Breath.isGazing(Target)) {
            Condition = Component.translatable(Prefix + "staring");
        } else if (Scp151Breath.isRecovering(Target)) {
            long Since = Target.level().getGameTime() - Scp151Breath.lastGaze(Target);
            Condition = Component.translatable(Since < Scp151Breath.RecoveryDelay ? Prefix + "winded" : Prefix + "recovering", Since / 20);
        } else {
            Condition = Component.translatable(Prefix + "breathing");
        }
        Component Shield = Component.translatable(Prefix + (AntiMemetic.isProtected(Target) ? "shielded" : "exposed"));
        Source.sendSuccess(() -> Component.translatable(Prefix + "status", Target.getDisplayName(), Target.getAirSupply(), Target.getMaxAirSupply(), Condition, Shield), false);
        ServerLevel World = Target.serverLevel();
        int Open = 0;
        int Draped = 0;
        Scp151PaintingBlockEntity Nearest = null;
        double Best = Double.MAX_VALUE;
        for (Scp151PaintingBlockEntity Painting : nearby(World, Target.blockPosition())) {
            BlockState State = Painting.getBlockState();
            if (Scp151PaintingBlock.isCovered(State)) {
                Draped++;
            } else {
                Open++;
            }
            double Distance = Painting.getBlockPos().distSqr(Target.blockPosition());
            if (Distance < Best) {
                Best = Distance;
                Nearest = Painting;
            }
        }
        if (Nearest == null) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "none", ScanRadius), false);
            return 0;
        }
        int OpenCount = Open;
        int DrapedCount = Draped;
        Source.sendSuccess(() -> Component.translatable(Prefix + "paintings", ScanRadius, OpenCount, DrapedCount), false);
        Scp151PaintingBlockEntity Found = Nearest;
        BlockPos Pos = Found.getBlockPos();
        BlockState State = Found.getBlockState();
        Scp151Sheet Sheet = State.getValue(Scp151PaintingBlock.Cover);
        Component Cover = Sheet.isDrawn() ? Component.translatable(Prefix + "covered", Sheet.colourName()) : Component.translatable(Prefix + "uncovered");
        Source.sendSuccess(() -> Component.translatable(Prefix + "nearest", Pos.getX(), Pos.getY(), Pos.getZ(),
                State.getValue(HorizontalDirectionalBlock.FACING).getSerializedName(), Cover, Found.getWatchers()), false);
        return OpenCount + DrapedCount;
    }

    private static List<Scp151PaintingBlockEntity> nearby(ServerLevel World, BlockPos Centre) {
        List<Scp151PaintingBlockEntity> Found = new ArrayList<>();
        int Reach = SectionPos.blockToSectionCoord(ScanRadius) + 1;
        int ChunkX = SectionPos.blockToSectionCoord(Centre.getX());
        int ChunkZ = SectionPos.blockToSectionCoord(Centre.getZ());
        for (int X = ChunkX - Reach; X <= ChunkX + Reach; X++) {
            for (int Z = ChunkZ - Reach; Z <= ChunkZ + Reach; Z++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(X, Z);
                if (Chunk == null) {
                    continue;
                }
                for (BlockEntity Each : Chunk.getBlockEntities().values()) {
                    if (Each instanceof Scp151PaintingBlockEntity Painting && within(Painting.getBlockPos(), Centre)) {
                        Found.add(Painting);
                    }
                }
            }
        }
        return Found;
    }

    private static boolean within(BlockPos Pos, BlockPos Centre) {
        return Pos.distSqr(Centre) <= (double) ScanRadius * ScanRadius;
    }
}
