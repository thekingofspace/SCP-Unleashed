package net.rai.scp_unleashed.space;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class SpaceFire {
    private SpaceFire() {
    }

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent Event) {
        LevelAccessor Accessor = Event.getLevel();
        if (!(Accessor instanceof Level World) || !Event.getPlacedBlock().is(SpaceTags.NeedsAir) || !SpaceAir.isAirless(World)) {
            return;
        }
        Event.setCanceled(true);
        snuff(World, Event.getPos(), Event.getEntity());
    }

    @SubscribeEvent
    public static void ignite(PlayerInteractEvent.RightClickBlock Event) {
        if (!Event.getItemStack().is(ItemTags.CREEPER_IGNITERS) || !SpaceAir.isAirless(Event.getLevel())) {
            return;
        }
        Event.setCanceled(true);
        Event.setCancellationResult(InteractionResult.FAIL);
        snuff(Event.getLevel(), Event.getPos().relative(Event.getFace() == null ? Direction.UP : Event.getFace()), Event.getEntity());
    }

    @SubscribeEvent
    public static void burning(EntityTickEvent.Post Event) {
        Entity Target = Event.getEntity();
        if (Target.getRemainingFireTicks() > 0 && !Target.isInLava() && SpaceAir.isAirless(Target.level())) {
            Target.clearFire();
        }
    }

    private static void snuff(Level World, BlockPos Pos, Entity Cause) {
        if (Cause instanceof Player User && !World.isClientSide) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.space.no_fire").withStyle(ChatFormatting.GRAY), true);
        }
        if (World instanceof ServerLevel Server) {
            Server.playSound(null, Pos, SpaceRegistry.FlameSnuff.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
            Server.sendParticles(ParticleTypes.SMOKE, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, 6, 0.15, 0.15, 0.15, 0.01);
        }
    }
}
