package net.rai.scp_unleashed.scp3008;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp3008Rules {
    public static final long WarnGap = 50L;
    public static final int SleepLines = 3;
    private static final Map<UUID, Long> Warned = new ConcurrentHashMap<>();

    private Scp3008Rules() {
    }

    public static boolean sealed(BlockState State) {
        return State.is(Scp3008Registry.Unbreakable);
    }

    public static void refuse(Player User, String Rule) {
        if (!(User instanceof ServerPlayer Visitor)) {
            return;
        }
        long Now = Visitor.serverLevel().getGameTime();
        Long Last = Warned.get(Visitor.getUUID());
        if (Last != null && Now >= Last && Now - Last < WarnGap) {
            return;
        }
        Warned.put(Visitor.getUUID(), Now);
        Visitor.displayClientMessage(Component.translatable("message.scp_unleashed.scp3008.rule." + Rule).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breaking(BlockEvent.BreakEvent Event) {
        Player User = Event.getPlayer();
        if (User != null && sealed(Event.getState()) && !Scp3008World.exempt(User)) {
            Event.setCanceled(true);
            refuse(User, "break");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void exploding(ExplosionEvent.Detonate Event) {
        Level World = Event.getLevel();
        Event.getAffectedBlocks().removeIf((BlockPos Pos) -> sealed(World.getBlockState(Pos)));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void portal(BlockEvent.PortalSpawnEvent Event) {
        if (Event.getLevel() instanceof Level World && Scp3008World.strict(World)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void spawning(FinalizeSpawnEvent Event) {
        if (!(Event.getLevel().getLevel() instanceof Level World) || !Scp3008World.strict(World)) {
            return;
        }
        MobSpawnType Kind = Event.getSpawnType();
        Entity Spawned = Event.getEntity();
        boolean Ours = Scp_unleashed.ModId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(Spawned.getType()).getNamespace());
        if (!Ours && (Kind == MobSpawnType.NATURAL || Kind == MobSpawnType.CHUNK_GENERATION || Kind == MobSpawnType.PATROL || Kind == MobSpawnType.STRUCTURE)) {
            Event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void sleeping(CanPlayerSleepEvent Event) {
        ServerPlayer User = Event.getEntity();
        if (!Scp3008World.visitor(User)) {
            return;
        }
        Event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
        int Line = User.getRandom().nextInt(SleepLines);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp3008.no_sleep." + Line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @SubscribeEvent
    public static void forget(ServerStoppedEvent Event) {
        Warned.clear();
    }
}
