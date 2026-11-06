package net.rai.scp_unleashed.transferpod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.stasis.StasisPodItem;

import java.util.List;

public class TransferPodItem extends Item {
    public static final String Key = "item.scp_unleashed.transfer_pod.";
    public static final int Colour = 0xF2D58C;
    public static final int Pips = FragmentRarity.values().length + 1;
    private static final String Pip = "◆";

    public TransferPodItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public Component getName(ItemStack Stack) {
        return Component.translatable(getDescriptionId(Stack)).withStyle(Style.EMPTY.withColor(Colour));
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        ItemStack Stack = Context.getItemInHand();
        if (!TransferPods.isLoaded(Stack)) {
            return InteractionResult.PASS;
        }
        Level World = Context.getLevel();
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos Clicked = Context.getClickedPos();
        Direction Face = Context.getClickedFace();
        BlockState State = World.getBlockState(Clicked);
        VoxelShape Shape = State.getCollisionShape(World, Clicked);
        BlockPos Spot = Shape.isEmpty() ? Clicked : Clicked.relative(Face);
        double Y = Spot.getY();
        if (!Shape.isEmpty() && Face == Direction.UP) {
            Y = Clicked.getY() + Shape.max(Direction.Axis.Y);
        }
        Player User = Context.getPlayer();
        float Yaw = User != null ? Mth.wrapDegrees(User.getYRot() + 180.0f) : 0.0f;
        int Freed = TransferPods.open((ServerLevel) World, Stack, new Vec3(Spot.getX() + 0.5, Y, Spot.getZ() + 0.5), Yaw, User);
        if (Freed <= 0) {
            return InteractionResult.FAIL;
        }
        spend(Stack);
        return InteractionResult.CONSUME;
    }

    public static void spend(ItemStack Stack) {
        Stack.shrink(1);
    }

    @Override
    public void onDestroyed(ItemEntity Dropped, DamageSource Source) {
        ItemStack Stack = Dropped.getItem();
        if (Dropped.level() instanceof ServerLevel World && TransferPods.isLoaded(Stack)) {
            TransferPods.breakOut(World, Stack.copy(), Dropped.position());
        }
    }

    @Override
    public int getEntityLifespan(ItemStack Stack, Level World) {
        return TransferPods.isLoaded(Stack) ? Integer.MAX_VALUE : super.getEntityLifespan(Stack, World);
    }

    @Override
    public boolean isBarVisible(ItemStack Stack) {
        return TransferPods.isLoaded(Stack);
    }

    @Override
    public int getBarWidth(ItemStack Stack) {
        return StasisPodItem.FullBar;
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        TransferPodContent Held = TransferPods.contentOf(Stack);
        return Held != null ? Held.rgb() : Colour;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack OldStack, ItemStack NewStack, boolean SlotChanged) {
        return SlotChanged || !ItemStack.isSameItem(OldStack, NewStack) || TransferPods.isLoaded(OldStack) != TransferPods.isLoaded(NewStack);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.stasis_pod.tier", Component.literal(Pip.repeat(Pips)).withStyle(Style.EMPTY.withColor(Colour)))
                .withStyle(ChatFormatting.DARK_GRAY));
        boolean Loaded = TransferPods.isLoaded(Stack);
        MutableComponent Held = TransferPods.labelOf(Stack).copy().withStyle(Loaded ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
        Lines.add(Component.translatable(Key + "holds", Held).withStyle(ChatFormatting.GRAY));
        if (Loaded) {
            Lines.add(Component.translatable(Key + "sealed").withStyle(ChatFormatting.AQUA));
        } else {
            Lines.add(Component.translatable(Key + "spent").withStyle(ChatFormatting.RED));
        }
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "release").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "single").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Key + "never").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Key + "broken").withStyle(ChatFormatting.GRAY));
        }
    }
}
