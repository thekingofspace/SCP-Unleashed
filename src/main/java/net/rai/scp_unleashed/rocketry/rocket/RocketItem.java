package net.rai.scp_unleashed.rocketry.rocket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;
import net.rai.scp_unleashed.rocketry.platform.LaunchPlatformKind;
import net.rai.scp_unleashed.rocketry.platform.PortableLaunchPadBlock;
import net.rai.scp_unleashed.rocketry.platform.RocketPlatformBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RocketItem extends Item {
    public RocketItem(Properties Settings) {
        super(Settings);
    }

    public record Stand(BlockPos Pad, Vec3 At, Direction Front, boolean Portable) {
    }

    @Nullable
    public static Stand standFor(Level World, BlockPos Clicked) {
        BlockState State = World.getBlockState(Clicked);
        if (State.getBlock() instanceof RocketPlatformBlock Platform) {
            BlockPos Origin = Platform.originOf(Clicked, State);
            Direction Front = State.getValue(RocketPlatformBlock.Facing);
            return new Stand(Origin, Platform.standAt(Origin, Front), Front, false);
        }
        if (State.getBlock() instanceof PortableLaunchPadBlock Pad) {
            BlockPos Origin = Pad.originOf(Clicked, State);
            Direction Front = State.getValue(PortableLaunchPadBlock.Facing);
            return new Stand(Origin, Pad.standAt(Origin, Front), Front, true);
        }
        return null;
    }

    public static AABB rocketBox(Vec3 At) {
        double Half = RocketEntity.Width / 2.0;
        return new AABB(At.x - Half, At.y, At.z - Half, At.x + Half, At.y + RocketEntity.Height, At.z + Half);
    }

    public static boolean padTaken(Level World, Vec3 At) {
        return !World.getEntitiesOfClass(RocketEntity.class, rocketBox(At).inflate(0.5), RocketEntity::isOnPad).isEmpty();
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Level World = Context.getLevel();
        Player User = Context.getPlayer();
        Stand Spot = standFor(World, Context.getClickedPos());
        if (Spot == null) {
            if (World.isClientSide && User != null) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.rocketry.needs_pad").withStyle(ChatFormatting.GOLD), true);
            }
            return InteractionResult.FAIL;
        }
        if (padTaken(World, Spot.At())) {
            if (World.isClientSide && User != null) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.rocketry.pad_taken").withStyle(ChatFormatting.GOLD), true);
            }
            return InteractionResult.FAIL;
        }
        if (!World.noCollision(rocketBox(Spot.At()).deflate(0.05))) {
            if (World.isClientSide && User != null) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.rocketry.no_room").withStyle(ChatFormatting.GOLD), true);
            }
            return InteractionResult.FAIL;
        }
        if (World instanceof ServerLevel ServerWorld) {
            ItemStack Stack = Context.getItemInHand();
            RocketEntity Rocket = RocketEntity.standing(ServerWorld, Spot.At(), Spot.Front().toYRot(), Stack, Spot.Pad(), Spot.Portable());
            if (!ServerWorld.addFreshEntity(Rocket)) {
                return InteractionResult.FAIL;
            }
            Stack.shrink(1);
            ServerWorld.playSound(null, Spot.At().x, Spot.At().y + 1.0, Spot.At().z, RocketryRegistry.Clamps.get(), SoundSource.BLOCKS, 1.0f, 1.2f);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.rocket.tooltip").withStyle(ChatFormatting.GRAY));
        int Fuel = Stack.getOrDefault(RocketryRegistry.RocketFuel.get(), 0);
        if (Fuel >= LaunchPlatformKind.Rocket.getFuelCost()) {
            Lines.add(Component.translatable("item.scp_unleashed.rocket.fuelled").withStyle(ChatFormatting.AQUA));
        } else if (Fuel > 0) {
            Lines.add(Component.translatable("item.scp_unleashed.rocket.some_fuel", Fuel).withStyle(ChatFormatting.AQUA));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.rocket.place").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.rocket.board").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.rocket.route").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.rocket.pod").withStyle(ChatFormatting.GOLD));
        }
    }
}
