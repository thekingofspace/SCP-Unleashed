package net.rai.scp_unleashed.integration.curios;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.goggles.Goggles;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class CuriosGoggles {
    private static final ICurioItem Strapped = wearable(true);
    private static final ICurioItem Clipped = wearable(false);

    private CuriosGoggles() {
    }

    public static void register() {
        for (Goggles.Kind Kind : Goggles.Kind.values()) {
            CuriosApi.registerCurio(Kind.item(), Kind.equipsFromUse() ? Strapped : Clipped);
        }
    }

    private static ICurioItem wearable(boolean FromUse) {
        return new ICurioItem() {
            @Override
            public boolean canEquipFromUse(SlotContext Context, ItemStack Stack) {
                return FromUse;
            }

            @Override
            public ICurio.SoundInfo getEquipSound(SlotContext Context, ItemStack Stack) {
                SoundEvent Sound = Stack.getItem() instanceof ArmorItem Armor ? Armor.getMaterial().value().equipSound().value() : SoundEvents.ARMOR_EQUIP_GENERIC.value();
                return new ICurio.SoundInfo(Sound, 1.0F, 1.0F);
            }
        };
    }
}
