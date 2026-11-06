package net.rai.scp_unleashed.food;

import net.minecraft.sounds.SoundEvent;

import java.util.List;

public enum SnackKind {
    Can(16, true),
    Bottle(16, true),
    Bag(64, false);

    private final int StackSize;
    private final boolean Drink;

    SnackKind(int StackSize, boolean Drink) {
        this.StackSize = StackSize;
        this.Drink = Drink;
    }

    public int getStackSize() {
        return StackSize;
    }

    public boolean isDrink() {
        return Drink;
    }

    public List<SoundEvent> getOpenSounds() {
        return switch (this) {
            case Can -> List.of(FoodSounds.CanOpen.get(), FoodSounds.CanOpenSnap.get(), FoodSounds.CanOpenFizz.get());
            case Bottle -> List.of(FoodSounds.BottleOpen.get(), FoodSounds.BottleOpenPop.get(), FoodSounds.BottleOpenFizz.get());
            case Bag -> List.of(FoodSounds.BagOpen.get(), FoodSounds.BagOpenPop.get());
        };
    }
}
