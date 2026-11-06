package net.rai.scp_unleashed.scp330;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp330Bloom {
    public static final int Cooldown = 8;
    public static final int Sparkles = 15;
    private static final Map<UUID, Long> LastGrowth = new HashMap<>();

    public enum Result {
        Pass,
        Grew,
        Resting
    }

    private Scp330Bloom() {
    }

    public static boolean growable(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.getBlock() instanceof BonemealableBlock Plant && Plant.isValidBonemealTarget(World, Pos, State);
    }

    public static Result use(Player User, Level World, BlockPos Pos) {
        if (!User.hasEffect(Scp330Registry.Bloom) || !growable(World, Pos)) {
            return Result.Pass;
        }
        if (!(World instanceof ServerLevel Server)) {
            return Result.Grew;
        }
        long Now = Server.getGameTime();
        Long Last = LastGrowth.get(User.getUUID());
        if (Last != null && Now >= Last && Now - Last < Cooldown) {
            return Result.Resting;
        }
        LastGrowth.put(User.getUUID(), Now);
        if (BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), Server, Pos, User)) {
            Server.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, Pos, Sparkles);
            User.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        }
        return Result.Grew;
    }

    public static void forget(UUID Id) {
        LastGrowth.remove(Id);
    }

    public static void clear() {
        LastGrowth.clear();
    }
}
