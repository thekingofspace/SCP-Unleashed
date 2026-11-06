package net.rai.scp_unleashed.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.upgrade.Upgradeable;
import net.rai.scp_unleashed.wire.WireHostBlock;

import java.util.List;

public class UpgradeItem extends Item {
    private final UpgradeType Type;

    public UpgradeItem(UpgradeType Type, Properties Properties) {
        super(Properties);
        this.Type = Type;
    }

    public UpgradeType getType() {
        return Type;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack Stack, UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState State = World.getBlockState(Pos);
        if (State.getBlock() instanceof WireHostBlock Host) {
            Pos = Host.devicePos(State, Pos);
        }
        if (!(World.getBlockEntity(Pos) instanceof Upgradeable Target) || !Target.acceptsUpgrade(Type)) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player User = Context.getPlayer();
        int Limit = Target.upgradeLimit(Type);
        int Installed = Target.countUpgrade(Type);
        if (Installed >= Limit) {
            if (User != null) {
                User.displayClientMessage(Limit > 1 ? Component.translatable("message.scp_unleashed.upgrade.full", Type.getDisplayName(), Limit)
                        : Component.translatable("message.scp_unleashed.upgrade.already", Type.getDisplayName()), true);
            }
            return InteractionResult.FAIL;
        }
        Target.installUpgrade(Type);
        if (User == null || !User.getAbilities().instabuild) {
            Stack.shrink(1);
        }
        if (User != null) {
            User.displayClientMessage(Limit > 1 ? Component.translatable("message.scp_unleashed.upgrade.installed_count", Type.getDisplayName(), Installed + 1, Limit)
                    : Component.translatable("message.scp_unleashed.upgrade.installed", Type.getDisplayName()), true);
        }
        World.playSound(null, Context.getClickedPos(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Type.getDescription().copy().withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.upgrade.fits", Type.getFits()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.upgrade.install").withStyle(ChatFormatting.GOLD));
        }
    }
}
