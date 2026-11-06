package net.rai.scp_unleashed.scp4845;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Employee;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp4845Rules {
    public static final long WarnGap = 50L;
    public static final int Lines = 3;
    private static final Map<UUID, Long> Warned = new ConcurrentHashMap<>();

    private Scp4845Rules() {
    }

    public static boolean protects(LevelAccessor Accessor, BlockPos Pos) {
        if (!(Accessor instanceof Level World)) {
            return false;
        }
        Scp4845Site Site = Scp4845Restaurant.in(World);
        return Site != null && Site.protects(World, Pos);
    }

    public static boolean exempt(Entity Who) {
        return Who instanceof Player User && User.isCreative() && User.hasPermissions(2);
    }

    public static void refuse(Player User) {
        if (!(User instanceof ServerPlayer Visitor)) {
            return;
        }
        long Now = Visitor.serverLevel().getGameTime();
        Long Last = Warned.get(Visitor.getUUID());
        if (Last != null && Now >= Last && Now - Last < WarnGap) {
            return;
        }
        Warned.put(Visitor.getUUID(), Now);
        int Line = Visitor.getRandom().nextInt(Lines);
        Visitor.displayClientMessage(Component.translatable("message.scp_unleashed.scp4845.rule." + Line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breaking(BlockEvent.BreakEvent Event) {
        Player User = Event.getPlayer();
        if (protects(Event.getLevel(), Event.getPos()) && (User == null || !exempt(User))) {
            Event.setCanceled(true);
            if (User != null) {
                refuse(User);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void placing(BlockEvent.EntityPlaceEvent Event) {
        Entity Placer = Event.getEntity();
        if (protects(Event.getLevel(), Event.getPos()) && (Placer == null || !exempt(Placer))) {
            Event.setCanceled(true);
            if (Placer instanceof Player User) {
                refuse(User);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void exploding(ExplosionEvent.Detonate Event) {
        Level World = Event.getLevel();
        Scp4845Site Site = Scp4845Restaurant.in(World);
        if (Site == null) {
            return;
        }
        Event.getAffectedBlocks().removeIf(Pos -> Site.protects(World, Pos));
        Event.getAffectedEntities().removeIf(Target -> Target instanceof Scp4845Employee);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void wrecking(LivingDestroyBlockEvent Event) {
        if (protects(Event.getEntity().level(), Event.getPos())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pushing(PistonEvent.Pre Event) {
        BlockPos Pos = Event.getPos();
        if (protects(Event.getLevel(), Pos) || protects(Event.getLevel(), Pos.relative(Event.getDirection())) || protects(Event.getLevel(), Pos.relative(Event.getDirection(), 2))) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void using(PlayerInteractEvent.RightClickBlock Event) {
        ItemStack Held = Event.getItemStack();
        boolean Risky = Held.getItem() instanceof BucketItem || Held.getItem() instanceof SolidBucketItem || Held.is(ItemTags.CREEPER_IGNITERS);
        if (!Risky || exempt(Event.getEntity())) {
            return;
        }
        BlockPos Pos = Event.getPos();
        BlockPos Beside = Event.getFace() == null ? Pos : Pos.relative(Event.getFace());
        if (protects(Event.getLevel(), Pos) || protects(Event.getLevel(), Beside)) {
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
            refuse(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Warned.clear();
    }
}
