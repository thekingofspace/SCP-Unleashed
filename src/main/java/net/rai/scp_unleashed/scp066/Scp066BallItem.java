package net.rai.scp_unleashed.scp066;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp066BallItem extends Item {
    public Scp066BallItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        if (!(Context.getLevel() instanceof ServerLevel World)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack Stack = Context.getItemInHand();
        BlockPos Clicked = Context.getClickedPos();
        Direction Side = Context.getClickedFace();
        BlockState State = World.getBlockState(Clicked);
        BlockPos Spot = State.getCollisionShape(World, Clicked).isEmpty() ? Clicked : Clicked.relative(Side);
        Scp066Entity Placed = Scp066Registry.Yarn.get().spawn(World, Stack, Context.getPlayer(), Spot, MobSpawnType.SPAWN_EGG, true,
                !Clicked.equals(Spot) && Side == Direction.UP);
        if (Placed == null) {
            return InteractionResult.FAIL;
        }
        if (Context.getPlayer() != null) {
            Placed.setYRot(Context.getPlayer().getYRot() + 180.0f);
            Placed.setYBodyRot(Placed.getYRot());
            Placed.setYHeadRot(Placed.getYRot());
        }
        World.playSound(null, Placed.getX(), Placed.getY(), Placed.getZ(), Scp066Registry.Place.get(), SoundSource.NEUTRAL, 0.8f, 1.0f);
        World.gameEvent(Context.getPlayer(), GameEvent.ENTITY_PLACE, Spot);
        Stack.consume(1, Context.getPlayer());
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp066_ball.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.scp066_ball.place").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp066_ball.treat").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp066_ball.scoop").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp066_ball.danger").withStyle(ChatFormatting.RED));
        }
    }
}
