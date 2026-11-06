package net.rai.scp_unleashed.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.item.UpgradeItem;

public class MufflerUpgradeItem extends UpgradeItem {
    public static final float FitVolume = 0.7f;
    public static final float FitPitch = 0.8f;

    public MufflerUpgradeItem(Properties Properties) {
        super(UpgradeType.Muffler, Properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack Stack, UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Clicked = Context.getClickedPos();
        BlockState State = World.getBlockState(Clicked);
        if (!MufflerTargets.muffleable(State)) {
            return InteractionResult.PASS;
        }
        if (!(World instanceof ServerLevel Server)) {
            return InteractionResult.SUCCESS;
        }
        Player User = Context.getPlayer();
        if (Mufflers.isMuffled(Server, Clicked)) {
            if (User != null) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.upgrade.already", getType().getDisplayName()), true);
            }
            return InteractionResult.FAIL;
        }
        Server.playSound(null, Clicked, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, FitVolume, FitPitch);
        Server.playSound(null, Clicked, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, FitVolume, 1.2f);
        if (Mufflers.install(Server, Clicked) == null) {
            return InteractionResult.FAIL;
        }
        if (User == null || !User.getAbilities().instabuild) {
            Stack.shrink(1);
        }
        if (User != null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.upgrade.installed", getType().getDisplayName()), true);
        }
        return InteractionResult.SUCCESS;
    }
}
