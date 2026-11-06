package net.rai.scp_unleashed.scp143;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class Scp143BoatItem extends Item {
    private final boolean Chest;

    public Scp143BoatItem(boolean Chest, Properties Settings) {
        super(Settings);
        this.Chest = Chest;
    }

    public Boat create(Level World, double X, double Y, double Z) {
        return Chest ? Scp143ChestBoat.at(World, X, Y, Z) : Scp143Boat.at(World, X, Y, Z);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        HitResult Hit = getPlayerPOVHitResult(World, User, ClipContext.Fluid.ANY);
        if (Hit.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(Stack);
        }
        Vec3 View = User.getViewVector(1.0f);
        Vec3 Eye = User.getEyePosition();
        for (var Near : World.getEntities(User, User.getBoundingBox().expandTowards(View.scale(5.0)).inflate(1.0), EntitySelector.NO_SPECTATORS.and(Found -> Found.isPickable()))) {
            AABB Box = Near.getBoundingBox().inflate(Near.getPickRadius());
            if (Box.contains(Eye)) {
                return InteractionResultHolder.pass(Stack);
            }
        }
        if (Hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(Stack);
        }
        Vec3 Spot = Hit.getLocation();
        Boat Made = create(World, Spot.x, Spot.y, Spot.z);
        if (World instanceof ServerLevel ServerWorld) {
            EntityType.<Boat>createDefaultStackConfig(ServerWorld, Stack, User).accept(Made);
        }
        Made.setYRot(User.getYRot());
        if (!World.noCollision(Made, Made.getBoundingBox())) {
            return InteractionResultHolder.fail(Stack);
        }
        if (!World.isClientSide) {
            World.addFreshEntity(Made);
            World.gameEvent(User, GameEvent.ENTITY_PLACE, Spot);
            Stack.consume(1, User);
        }
        User.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide());
    }

    public static final class Dispense extends DefaultDispenseItemBehavior {
        private final DefaultDispenseItemBehavior Fallback = new DefaultDispenseItemBehavior();

        @Override
        public ItemStack execute(BlockSource Source, ItemStack Stack) {
            if (!(Stack.getItem() instanceof Scp143BoatItem Held)) {
                return Fallback.dispense(Source, Stack);
            }
            Direction Facing = Source.state().getValue(DispenserBlock.FACING);
            ServerLevel World = Source.level();
            Vec3 Centre = Source.center();
            double Reach = 0.5625 + Scp143Registry.BoatType.get().getWidth() / 2.0;
            double X = Centre.x() + Facing.getStepX() * Reach;
            double Y = Centre.y() + Facing.getStepY() * 1.125f;
            double Z = Centre.z() + Facing.getStepZ() * Reach;
            BlockPos Ahead = Source.pos().relative(Facing);
            Boat Made = Held.create(World, X, Y, Z);
            EntityType.<Boat>createDefaultStackConfig(World, Stack, null).accept(Made);
            Made.setYRot(Facing.toYRot());
            double Lift;
            if (Made.canBoatInFluid(World.getFluidState(Ahead))) {
                Lift = 1.0;
            } else {
                if (!World.getBlockState(Ahead).isAir() || !Made.canBoatInFluid(World.getFluidState(Ahead.below()))) {
                    return Fallback.dispense(Source, Stack);
                }
                Lift = 0.0;
            }
            Made.setPos(X, Y + Lift, Z);
            World.addFreshEntity(Made);
            Stack.shrink(1);
            return Stack;
        }

        @Override
        protected void playSound(BlockSource Source) {
            Source.level().levelEvent(1000, Source.pos(), 0);
        }
    }
}
