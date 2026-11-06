package net.rai.scp_unleashed.scp268.integration;

import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.scp268.Scp268Registry;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

public final class Scp268Curios {
    public static final String HeadSlot = "head";

    private static final ICurioItem Wearable = new ICurioItem() {
        @Override
        public boolean canEquip(SlotContext Context, ItemStack Stack) {
            return !Scp268.settling(Context.entity());
        }

        @Override
        public boolean canUnequip(SlotContext Context, ItemStack Stack) {
            return !Scp268.settling(Context.entity());
        }

        @Override
        public ICurio.SoundInfo getEquipSound(SlotContext Context, ItemStack Stack) {
            return new ICurio.SoundInfo(SoundEvents.EMPTY, 0.0F, 1.0F);
        }
    };

    private Scp268Curios() {
    }

    public static void register() {
        CuriosApi.registerCurio(Scp268Registry.Cap.get(), Wearable);
    }

    public static boolean isWorn(Player User) {
        return CuriosApi.getCuriosInventory(User).flatMap(Inventory -> Inventory.findFirstCurio(Stack -> Stack.is(Scp268Registry.Cap.get()))).isPresent();
    }

    public static boolean wear(Player User, ItemStack Held) {
        ICurioStacksHandler Handler = CuriosApi.getCuriosInventory(User).map(Inventory -> Inventory.getCurios().get(HeadSlot)).orElse(null);
        if (Handler == null) {
            return false;
        }
        IDynamicStackHandler Stacks = Handler.getStacks();
        NonNullList<Boolean> Active = Handler.getActiveStates();
        for (int Slot = 0; Slot < Stacks.getSlots(); Slot++) {
            boolean Open = Active.size() <= Slot || Active.get(Slot);
            if (Open && Stacks.getStackInSlot(Slot).isEmpty() && Stacks.isItemValid(Slot, Held)) {
                Stacks.setStackInSlot(Slot, User.isCreative() ? Held.copyWithCount(1) : Held.split(1));
                return true;
            }
        }
        return false;
    }
}
