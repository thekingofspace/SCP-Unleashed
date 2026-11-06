package net.rai.scp_unleashed.labware;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.plumbing.EntityWashedEvent;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class LabwareEvents {
    private LabwareEvents() {
    }

    @SubscribeEvent
    public static void swabEntity(PlayerInteractEvent.EntityInteract Event) {
        if (!(Event.getTarget() instanceof LivingEntity Target) || Target instanceof ArmorStand || !Target.isAlive() || !LabVials.isSterileSwab(Event.getItemStack())) {
            return;
        }
        InteractionResult Result = LabSwabItem.swab(Event.getEntity(), Event.getHand(), Target);
        if (Result.consumesAction()) {
            Event.setCancellationResult(Result);
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void sneakOnStorage(PlayerInteractEvent.RightClickBlock Event) {
        if (!Event.getEntity().isSecondaryUseActive()) {
            return;
        }
        BlockState State = Event.getLevel().getBlockState(Event.getPos());
        if (State.getBlock() instanceof LabViralStorageBlock && State.getValue(LabViralStorageBlock.Hatch)) {
            Event.setUseBlock(TriState.TRUE);
        }
    }

    @SubscribeEvent
    public static void tickCarrier(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer Carrier) {
            LabContamination.tick(Carrier);
        }
    }

    @SubscribeEvent
    public static void unmaskCarrier(LivingEquipmentChangeEvent Event) {
        if (Event.getSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR && Event.getEntity() instanceof ServerPlayer Carrier) {
            LabContamination.unprotected(Carrier);
        }
    }

    @SubscribeEvent
    public static void washCarrier(EntityWashedEvent Event) {
        if (Event.getEntity() instanceof LivingEntity Target && (Target instanceof ServerPlayer || ControlBodies.isPersonLike(Target)) && Event.getLiquid().is(FluidTags.WATER) && LabContamination.isContaminated(Target)) {
            LabContamination.rinse(Event.getLevel(), Target);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clearOnDeath(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Fallen) {
            LabContamination.clear(Fallen);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            for (LabOrganism Organism : LabOrganisms.all()) {
                Event.accept(Organism.sealedVial());
                Event.accept(Organism.openVial());
            }
            return;
        }
        if (Event.getTabKey() != ModCreativeTabs.Facility.getKey()) {
            return;
        }
        Event.accept(LabwareRegistry.Vial.get());
        Event.accept(LabVials.openVial(null));
        Event.accept(LabwareRegistry.Swab.get());
        Event.accept(LabwareRegistry.Wipe.get());
        Event.accept(LabwareRegistry.ResultSlip.get());
        Event.accept(LabwareRegistry.PetriDishItem.get());
        Event.accept(LabwareRegistry.TubeHolderItem.get());
        Event.accept(LabwareRegistry.ViralStorageItem.get());
        Event.accept(LabwareRegistry.AnalyzerItem.get());
    }
}
