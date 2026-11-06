package net.rai.scp_unleashed.cameras;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.cameras.client.CameraClientHooks;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.senses.Senses;

import java.util.List;

public class CameraTabletItem extends Item {
    public CameraTabletItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Player User = Context.getPlayer();
        Level World = Context.getLevel();
        if (User == null || !User.isSecondaryUseActive() || !(World.getBlockEntity(Context.getClickedPos()) instanceof CameraBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (User instanceof ServerPlayer Viewer && World instanceof ServerLevel Where) {
            return CameraSessions.link(Viewer, Context.getItemInHand(), Where, Context.getClickedPos());
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (World.isClientSide) {
            if (!Senses.isBlindfolded(User) && !CameraClientHooks.isViewing()) {
                CameraClientHooks.openTablet(Hand);
            }
        } else if (!Senses.refuseSight(User)) {
            World.playSound(null, User.getX(), User.getY(), User.getZ(), CameraRegistry.TabletOpen.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
        }
        User.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.camera_tablet.tooltip").withStyle(ChatFormatting.GRAY));
        int Count = CameraLinks.of(Stack).size();
        Lines.add(Component.translatable("item.scp_unleashed.camera_tablet.count", Count, CameraLinks.MaxLinks).withStyle(Count > 0 ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.camera_tablet.detail").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.camera_tablet.detail_view").withStyle(ChatFormatting.GOLD));
        }
    }
}
