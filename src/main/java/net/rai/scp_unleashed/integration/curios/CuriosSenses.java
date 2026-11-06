package net.rai.scp_unleashed.integration.curios;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.senses.SensesRegistry;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Optional;

public final class CuriosSenses {
    private static final ICurioItem Wearable = new ICurioItem() {
        @Override
        public boolean canEquipFromUse(SlotContext Context, ItemStack Stack) {
            return true;
        }

        @Override
        public ICurio.SoundInfo getEquipSound(SlotContext Context, ItemStack Stack) {
            return new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1.0F, 1.0F);
        }
    };

    private CuriosSenses() {
    }

    public static void register() {
        CuriosApi.registerCurio(SensesRegistry.Earplugs.get(), Wearable);
        CuriosApi.registerCurio(SensesRegistry.Blindfold.get(), Wearable);
    }

    public static boolean isWearing(LivingEntity Wearer, Item Worn) {
        return find(Wearer, Worn).isPresent();
    }

    public static boolean isShown(LivingEntity Wearer, Item Worn) {
        return find(Wearer, Worn).map(Found -> Found.slotContext().visible()).orElse(false);
    }

    private static Optional<SlotResult> find(LivingEntity Wearer, Item Worn) {
        return CuriosApi.getCuriosInventory(Wearer).flatMap(Inventory -> Inventory.findFirstCurio(Worn));
    }
}
