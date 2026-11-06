package net.rai.scp_unleashed.scp002;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp002Lockdown {
    public static final int ScanDepth = 4;
    public static final long RefuseCooldown = 40L;
    public static final int RefuseLines = 4;
    private static final Map<UUID, Long> LastRefusal = new ConcurrentHashMap<>();

    private Scp002Lockdown() {
    }

    public static boolean isFurnishing(BlockState State) {
        return State.getBlock() instanceof FleshFixtureBlock || State.getBlock() instanceof BoneLampBlock;
    }

    private static boolean answers(BlockState State) {
        return isFurnishing(State) || State.getBlock() instanceof BulkheadDoorBlock;
    }

    private static boolean isOpenSpace(BlockGetter World, BlockPos Pos, BlockState State) {
        if (State.getBlock() instanceof BulkheadDoorBlock) {
            return false;
        }
        return State.isAir() || isFurnishing(State) || State.getCollisionShape(World, Pos).isEmpty();
    }

    public static boolean isRoomCell(BlockGetter World, BlockPos Pos) {
        BlockPos.MutableBlockPos Cursor = Pos.mutable();
        for (int Step = 0; Step < ScanDepth; Step++) {
            Cursor.move(Direction.DOWN);
            BlockState State = World.getBlockState(Cursor);
            if (State.is(Scp002Registry.FleshFloor.get())) {
                return true;
            }
            if (!isOpenSpace(World, Cursor, State)) {
                return false;
            }
        }
        return false;
    }

    public static boolean isExempt(Player User) {
        return User.isSpectator() || User.getAbilities().invulnerable;
    }

    public static boolean isLocked(Player User) {
        if (!User.isAlive() || isExempt(User)) {
            return false;
        }
        return isRoomCell(User.level(), BlockPos.containing(User.getX(), User.getY() + 0.05, User.getZ()));
    }

    public static boolean placesInRoom(BlockGetter World, BlockHitResult Hit) {
        BlockPos Clicked = Hit.getBlockPos();
        if (isRoomCell(World, Clicked.relative(Hit.getDirection()))) {
            return true;
        }
        BlockState Target = World.getBlockState(Clicked);
        return (Target.isAir() || Target.canBeReplaced() || isFurnishing(Target)) && isRoomCell(World, Clicked);
    }

    private static boolean holdsFluid(ItemStack Held) {
        if (Held.getItem() instanceof BucketItem Bucket && Bucket.content != Fluids.EMPTY) {
            return true;
        }
        return FluidUtil.getFluidContained(Held).map(Contained -> !Contained.isEmpty()).orElse(false);
    }

    private static boolean poursIntoRoom(Player User, ItemStack Held) {
        if (!holdsFluid(Held)) {
            return false;
        }
        HitResult Hit = User.pick(User.blockInteractionRange(), 1.0f, false);
        return Hit.getType() == HitResult.Type.BLOCK && Hit instanceof BlockHitResult Aimed && placesInRoom(User.level(), Aimed);
    }

    private static void refuse(Player User) {
        if (!(User instanceof ServerPlayer Guest)) {
            return;
        }
        long Now = Guest.level().getGameTime();
        Long Last = LastRefusal.get(Guest.getUUID());
        if (Last != null && Now >= Last && Now - Last < RefuseCooldown) {
            return;
        }
        LastRefusal.put(Guest.getUUID(), Now);
        Component Line = Component.translatable("scp002.scp_unleashed.room.refuse." + Guest.getRandom().nextInt(RefuseLines)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
        Guest.displayClientMessage(Line, true);
    }

    @SubscribeEvent
    public static void useItem(PlayerInteractEvent.RightClickItem Event) {
        Player User = Event.getEntity();
        if (isExempt(User)) {
            return;
        }
        if (isLocked(User) || poursIntoRoom(User, Event.getItemStack())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
            refuse(User);
        }
    }

    @SubscribeEvent
    public static void useItemOnBlock(PlayerInteractEvent.RightClickBlock Event) {
        Player User = Event.getEntity();
        if (Event.getItemStack().isEmpty() || isExempt(User)) {
            return;
        }
        if (!isLocked(User) && !placesInRoom(Event.getLevel(), Event.getHitVec())) {
            return;
        }
        Event.setUseItem(TriState.FALSE);
        if (!answers(Event.getLevel().getBlockState(Event.getPos()))) {
            refuse(User);
        }
    }

    @SubscribeEvent
    public static void useItemOnEntity(PlayerInteractEvent.EntityInteract Event) {
        if (!Event.getItemStack().isEmpty() && isLocked(Event.getEntity())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
            refuse(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void useItemOnEntityPart(PlayerInteractEvent.EntityInteractSpecific Event) {
        if (!Event.getItemStack().isEmpty() && isLocked(Event.getEntity())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void hitBlock(PlayerInteractEvent.LeftClickBlock Event) {
        Player User = Event.getEntity();
        if (isLocked(User)) {
            Event.setCanceled(true);
            if (Event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) {
                refuse(User);
            }
        }
    }

    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent Event) {
        if (isLocked(Event.getPlayer())) {
            Event.setCanceled(true);
            refuse(Event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void placeBlock(BlockEvent.EntityPlaceEvent Event) {
        if (Event.getEntity() instanceof Player User) {
            if (!isExempt(User) && (isLocked(User) || isRoomCell(Event.getLevel(), Event.getPos()))) {
                Event.setCanceled(true);
                refuse(User);
            }
            return;
        }
        if (Event.getEntity() != null && isRoomCell(Event.getLevel(), Event.getPos())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void forget(ServerStoppedEvent Event) {
        LastRefusal.clear();
    }
}
