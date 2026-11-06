package net.rai.scp_unleashed.food;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import org.jetbrains.annotations.Nullable;

public enum Snack {
    KeterKola("keter_kola", SnackKind.Can, 3, MobEffects.MOVEMENT_SPEED, 600, 0),
    BreachEnergy("breach_energy", SnackKind.Can, 4, MobEffects.DIG_SPEED, 900, 1),
    AnchorRootBeer("anchor_root_beer", SnackKind.Bottle, 4, MobEffects.DAMAGE_RESISTANCE, 600, 0),
    OverseerOrange("overseer_orange", SnackKind.Bottle, 3, MobEffects.NIGHT_VISION, 900, 0),
    DClassCrunch("d_class_crunch", 2, 4, 0.3f, false),
    RedactedRanch("redacted_ranch", 3, 5, 0.4f, false),
    MeltdownHotChips("meltdown_hot_chips", 3, 5, 0.4f, false),
    PocketDimensionPuffs("pocket_dimension_puffs", 2, 4, 0.3f, true);

    public static final int DrinkNutrition = 2;
    public static final float DrinkSaturation = 0.1f;
    public static final int Columns = 2;

    private final String Name;
    private final SnackKind Kind;
    private final int Price;
    @Nullable
    private final Holder<MobEffect> Effect;
    private final int Duration;
    private final int Amplifier;
    private final int Nutrition;
    private final float Saturation;
    private final boolean Fast;

    Snack(String Name, SnackKind Kind, int Price, Holder<MobEffect> Effect, int Duration, int Amplifier) {
        this.Name = Name;
        this.Kind = Kind;
        this.Price = Price;
        this.Effect = Effect;
        this.Duration = Duration;
        this.Amplifier = Amplifier;
        this.Nutrition = DrinkNutrition;
        this.Saturation = DrinkSaturation;
        this.Fast = false;
    }

    Snack(String Name, int Price, int Nutrition, float Saturation, boolean Fast) {
        this.Name = Name;
        this.Kind = SnackKind.Bag;
        this.Price = Price;
        this.Effect = null;
        this.Duration = 0;
        this.Amplifier = 0;
        this.Nutrition = Nutrition;
        this.Saturation = Saturation;
        this.Fast = Fast;
    }

    @Nullable
    public static Snack byIndex(int Index) {
        Snack[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : null;
    }

    public String getName() {
        return Name;
    }

    public SnackKind getKind() {
        return Kind;
    }

    public int getPrice() {
        return Price;
    }

    public String getCode() {
        return String.valueOf((char) ('A' + ordinal() / Columns)) + (ordinal() % Columns + 1);
    }

    @Nullable
    public MobEffectInstance effect() {
        return Effect == null ? null : new MobEffectInstance(Effect, Duration, Amplifier);
    }

    public FoodProperties food() {
        FoodProperties.Builder Builder = new FoodProperties.Builder().nutrition(Nutrition).saturationModifier(Saturation);
        if (Effect != null) {
            Builder.alwaysEdible().effect(() -> new MobEffectInstance(Effect, Duration, Amplifier), 1.0f);
        }
        if (Fast) {
            Builder.fast();
        }
        return Builder.build();
    }
}
