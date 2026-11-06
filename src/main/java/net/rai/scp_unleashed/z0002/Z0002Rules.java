package net.rai.scp_unleashed.z0002;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Z0002Rules {
    private Z0002Rules() {
    }

    private static boolean guarded(Level World, BlockPos Pos) {
        return Z0002Domains.domainAt(World, Pos) != null;
    }

    private static boolean shaping(ItemStack Stack) {
        Item Kind = Stack.getItem();
        return Kind instanceof BlockItem || Kind instanceof BucketItem || Kind instanceof FlintAndSteelItem || Kind instanceof FireChargeItem;
    }

    private static void refuse(Entity Who, String Key) {
        if (Who instanceof ServerPlayer User) {
            Z0002Domains.hint(User, Key);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breaking(BlockEvent.BreakEvent Event) {
        if (Event.getLevel() instanceof Level World && guarded(World, Event.getPos()) && (Event.getPlayer() == null || !Event.getPlayer().isCreative())) {
            Event.setCanceled(true);
            refuse(Event.getPlayer(), "rule");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakSpeed(PlayerEvent.BreakSpeed Event) {
        if (Event.getPosition().isPresent() && guarded(Event.getEntity().level(), Event.getPosition().get())) {
            Event.setNewSpeed(0.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void placing(BlockEvent.EntityPlaceEvent Event) {
        if (Event.getLevel() instanceof Level World && guarded(World, Event.getPos())) {
            Event.setCanceled(true);
            refuse(Event.getEntity(), "rule");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void toolUse(BlockEvent.BlockToolModificationEvent Event) {
        if (Event.getLevel() instanceof Level World && guarded(World, Event.getPos())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void fluidMade(BlockEvent.FluidPlaceBlockEvent Event) {
        if (Event.getLevel() instanceof Level World && guarded(World, Event.getPos())) {
            Event.setNewState(Event.getOriginalState());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void portal(BlockEvent.PortalSpawnEvent Event) {
        if (Event.getLevel() instanceof Level World && guarded(World, Event.getPos())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void detonate(ExplosionEvent.Detonate Event) {
        Level World = Event.getLevel();
        if (!World.isClientSide) {
            Event.getAffectedBlocks().removeIf(Pos -> guarded(World, Pos));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useOnBlock(PlayerInteractEvent.RightClickBlock Event) {
        Player User = Event.getEntity();
        if (User.level().isClientSide) {
            return;
        }
        BlockPos Pos = Event.getPos();
        if (guarded(User.level(), Pos)) {
            Event.setUseBlock(TriState.FALSE);
        }
        if (!shaping(Event.getItemStack())) {
            return;
        }
        if (guarded(User.level(), Pos) || guarded(User.level(), Pos.relative(Event.getFace() == null ? Direction.UP : Event.getFace()))) {
            Event.setUseItem(TriState.FALSE);
            refuse(User, "rule");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void piston(PistonEvent.Pre Event) {
        if (!(Event.getLevel() instanceof Level World) || World.isClientSide) {
            return;
        }
        if (guarded(World, Event.getPos()) || guarded(World, Event.getFaceOffsetPos())) {
            Event.setCanceled(true);
            return;
        }
        PistonStructureResolver Helper = Event.getStructureHelper();
        if (Helper == null || !Helper.resolve()) {
            return;
        }
        for (BlockPos Moved : Helper.getToPush()) {
            if (guarded(World, Moved) || guarded(World, Moved.relative(Helper.getPushDirection()))) {
                Event.setCanceled(true);
                return;
            }
        }
        for (BlockPos Broken : Helper.getToDestroy()) {
            if (guarded(World, Broken)) {
                Event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useItem(PlayerInteractEvent.RightClickItem Event) {
        Player User = Event.getEntity();
        if (User.level().isClientSide || !(Event.getItemStack().getItem() instanceof BucketItem)) {
            return;
        }
        if (Z0002Domains.bound(User) || guarded(User.level(), User.blockPosition())) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
            refuse(User, "rule");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pearl(EntityTeleportEvent.EnderPearl Event) {
        if (Z0002Domains.bound(Event.getEntity())) {
            Event.setCanceled(true);
            refuse(Event.getEntity(), "escape");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void chorus(EntityTeleportEvent.ChorusFruit Event) {
        if (Z0002Domains.bound(Event.getEntity())) {
            Event.setCanceled(true);
            refuse(Event.getEntity(), "escape");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void command(EntityTeleportEvent.TeleportCommand Event) {
        if (Z0002Domains.bound(Event.getEntity())) {
            Event.setCanceled(true);
            refuse(Event.getEntity(), "escape");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void spread(EntityTeleportEvent.SpreadPlayersCommand Event) {
        if (Z0002Domains.bound(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void travel(EntityTravelToDimensionEvent Event) {
        if (Z0002Domains.bound(Event.getEntity())) {
            Event.setCanceled(true);
            refuse(Event.getEntity(), "escape");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void sleeping(CanPlayerSleepEvent Event) {
        if (Z0002Domains.bound(Event.getEntity())) {
            Event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void spawning(FinalizeSpawnEvent Event) {
        if (!(Event.getLevel().getLevel() instanceof Level World) || !guarded(World, BlockPos.containing(Event.getX(), Event.getY(), Event.getZ()))) {
            return;
        }
        MobSpawnType Kind = Event.getSpawnType();
        if (Kind != MobSpawnType.COMMAND && Kind != MobSpawnType.MOB_SUMMONED && Kind != MobSpawnType.EVENT && Kind != MobSpawnType.SPAWN_EGG) {
            Event.setSpawnCancelled(true);
        }
    }
}
