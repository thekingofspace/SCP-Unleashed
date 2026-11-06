package net.rai.scp_unleashed.forcefield;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.forcefield.client.ForcefieldClientHooks;
import net.rai.scp_unleashed.forcefield.network.ForcefieldHitPayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ForcefieldEvents {
    public static final float ExplosionScale = 10.0f;
    public static final int MaxBlastReach = 12;

    private ForcefieldEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(ForcefieldHitPayload.PayloadType, ForcefieldHitPayload.PayloadCodec, (Payload, Context) -> ForcefieldClientHooks.onHit(Payload));
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(new ItemStack(ForcefieldRegistry.EmitterItem.get()));
        }
    }

    @SubscribeEvent
    public static void breakBarrier(BlockEvent.BreakEvent Event) {
        if (!(Event.getState().getBlock() instanceof ForcefieldBarrierBlock)) {
            return;
        }
        if (Event.getPlayer() == null || !Event.getPlayer().isCreative()) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void detonate(ExplosionEvent.Detonate Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        Explosion Blast = Event.getExplosion();
        float Radius = Blast.radius();
        if (Radius <= 0.0f) {
            return;
        }
        Vec3 Centre = Blast.center();
        int Reach = Math.min(MaxBlastReach, Mth.ceil(Radius));
        if (!ForcefieldIndex.anyNear(World, Centre, Reach)) {
            return;
        }
        BlockPos Middle = BlockPos.containing(Centre);
        Map<ForcefieldEmitterBlockEntity, Vec3> Closest = new HashMap<>();
        Map<ForcefieldEmitterBlockEntity, Double> Distances = new HashMap<>();
        for (BlockPos Pos : BlockPos.betweenClosed(Middle.offset(-Reach, -Reach, -Reach), Middle.offset(Reach, Reach, Reach))) {
            if (!(World.getBlockState(Pos).getBlock() instanceof ForcefieldBarrierBlock)) {
                continue;
            }
            Vec3 Point = Vec3.atCenterOf(Pos);
            double Distance = Math.max(0.0, Point.distanceTo(Centre) - 0.5);
            if (Distance > Radius) {
                continue;
            }
            ForcefieldEmitterBlockEntity Owner = ForcefieldBarrierBlock.owner(World, Pos);
            if (Owner == null) {
                continue;
            }
            ForcefieldEmitterBlockEntity Head = Owner.leader();
            ForcefieldEmitterBlockEntity Wall = Head == null ? Owner : Head;
            if (Distance < Distances.getOrDefault(Wall, Double.MAX_VALUE)) {
                Distances.put(Wall, Distance);
                Closest.put(Wall, Point);
            }
        }
        for (Map.Entry<ForcefieldEmitterBlockEntity, Vec3> Entry : Closest.entrySet()) {
            double Distance = Distances.get(Entry.getKey());
            float Amount = (float) (ExplosionScale * Radius * (1.0 - Distance / (2.0 * Radius)));
            ForcefieldEmitterBlockEntity Target = ForcefieldBarrierBlock.owner(World, BlockPos.containing(Entry.getValue()));
            if (Target != null) {
                Target.damage(World, Amount, Entry.getValue());
            }
        }
    }

    @SubscribeEvent
    public static void tickMiners(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            ForcefieldMining.tick(World);
        }
    }

    @SubscribeEvent
    public static void forgetMiner(PlayerEvent.PlayerLoggedOutEvent Event) {
        ForcefieldMining.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void forgetMiners(ServerStoppedEvent Event) {
        ForcefieldMining.clear();
        ForcefieldIndex.clear();
    }
}
