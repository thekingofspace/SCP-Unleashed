package net.rai.scp_unleashed.gear;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class GearEquipSounds {
    public static final int SettleTicks = 5;
    private static final Map<GearOutfit, DeferredHolder<SoundEvent, SoundEvent>> Layers = Map.of(
            GearOutfit.Goc, GearSounds.GocEquipLayer,
            GearOutfit.SerpentsHand, GearSounds.SerpentsHandEquipLayer,
            GearOutfit.ChaosInsurgency, GearSounds.ChaosInsurgencyEquipLayer,
            GearOutfit.SiteGuardTactical, GearSounds.SiteGuardTacticalEquipLayer,
            GearOutfit.Scientist, GearSounds.ScientistEquipLayer,
            GearOutfit.BrokenGod, GearSounds.BrokenGodEquipLayer,
            GearOutfit.Sarkic, GearSounds.SarkicEquipLayer);

    private GearEquipSounds() {
    }

    @Nullable
    public static DeferredHolder<SoundEvent, SoundEvent> layerFor(GearOutfit Outfit) {
        return Layers.get(Outfit);
    }

    @SubscribeEvent
    public static void equipmentChanged(LivingEquipmentChangeEvent Event) {
        LivingEntity Wearer = Event.getEntity();
        if (Wearer.level().isClientSide() || Wearer.isSilent() || Wearer.isSpectator() || Wearer.tickCount <= SettleTicks) {
            return;
        }
        ItemStack Worn = Event.getTo();
        if (!(Worn.getItem() instanceof GearArmorItem Gear) || Gear.getEquipmentSlot() != Event.getSlot() || ItemStack.isSameItem(Event.getFrom(), Worn)) {
            return;
        }
        DeferredHolder<SoundEvent, SoundEvent> Layer = layerFor(Gear.getPiece().getOutfit());
        if (Layer != null) {
            Wearer.level().playSound(null, Wearer.getX(), Wearer.getY(), Wearer.getZ(), Layer.get(), Wearer.getSoundSource(), 1.0F, 1.0F);
        }
    }
}
