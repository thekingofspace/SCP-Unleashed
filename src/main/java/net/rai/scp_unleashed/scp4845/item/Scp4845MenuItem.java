package net.rai.scp_unleashed.scp4845.item;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public enum Scp4845MenuItem {
    CrunchyTaco("scp4845_crunchy_taco", 1, 1, 5, 0.6f, true, false, null, 0, 0),
    SoftTaco("scp4845_soft_taco", 1, 1, 5, 0.7f, false, false, MobEffects.REGENERATION, 80, 0),
    Burrito("scp4845_burrito", 2, 1, 9, 0.8f, false, false, MobEffects.ABSORPTION, 600, 0),
    CheesyMelt("scp4845_cheesy_melt", 2, 1, 7, 0.75f, false, false, MobEffects.REGENERATION, 120, 0),
    Nachos("scp4845_nachos", 2, 1, 6, 0.5f, true, false, MobEffects.DIG_SPEED, 900, 0),
    CinnamonPuffs("scp4845_cinnamon_puffs", 1, 1, 4, 0.4f, true, false, MobEffects.SLOW_FALLING, 300, 0),
    LunarBlast("scp4845_lunar_blast", 1, 1, 2, 0.2f, false, true, MobEffects.MOVEMENT_SPEED, 1200, 0),
    SparkSauce("scp4845_spark_sauce", 1, 3, 1, 0.1f, true, false, MobEffects.FIRE_RESISTANCE, 400, 0);

    public static final int StackSize = 16;
    public static final int MaxPerLine = 9;

    public final String Id;
    public final int Price;
    public final int Count;
    public final int Nutrition;
    public final float Saturation;
    public final boolean Fast;
    public final boolean Drink;
    private final Holder<MobEffect> Effect;
    public final int EffectTicks;
    public final int Amplifier;

    Scp4845MenuItem(String Id, int Price, int Count, int Nutrition, float Saturation, boolean Fast, boolean Drink, Holder<MobEffect> Effect, int EffectTicks, int Amplifier) {
        this.Id = Id;
        this.Price = Price;
        this.Count = Count;
        this.Nutrition = Nutrition;
        this.Saturation = Saturation;
        this.Fast = Fast;
        this.Drink = Drink;
        this.Effect = Effect;
        this.EffectTicks = EffectTicks;
        this.Amplifier = Amplifier;
    }

    public FoodProperties food() {
        FoodProperties.Builder Builder = new FoodProperties.Builder().nutrition(Nutrition).saturationModifier(Saturation);
        if (Fast) {
            Builder.fast();
        }
        if (Drink || this == SparkSauce) {
            Builder.alwaysEdible();
        }
        if (Effect != null) {
            Builder.effect(() -> new MobEffectInstance(Effect, EffectTicks, Amplifier), 1.0f);
        }
        return Builder.build();
    }

    public static Scp4845MenuItem byIndex(int Index) {
        Scp4845MenuItem[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : null;
    }
}
