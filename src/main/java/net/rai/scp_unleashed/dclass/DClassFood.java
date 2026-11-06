package net.rai.scp_unleashed.dclass;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.GameRules;

public final class DClassFood {
    public static final int MaxFood = 20;
    public static final float MaxExhaustion = 40.0f;
    private int Food = MaxFood;
    private float Saturation = 5.0f;
    private float Exhaustion;
    private int Timer;

    public int food() {
        return Food;
    }

    public float saturation() {
        return Saturation;
    }

    public float exhaustion() {
        return Exhaustion;
    }

    public boolean needsFood() {
        return Food < MaxFood;
    }

    public void eat(FoodProperties Properties) {
        add(Properties.nutrition(), Properties.saturation());
    }

    public void eat(int Nutrition, float Modifier) {
        add(Nutrition, FoodConstants.saturationByModifier(Nutrition, Modifier));
    }

    private void add(int Nutrition, float Saturated) {
        Food = Mth.clamp(Nutrition + Food, 0, MaxFood);
        Saturation = Mth.clamp(Saturated + Saturation, 0.0f, (float) Food);
    }

    public void addExhaustion(float Amount) {
        Exhaustion = Math.min(Exhaustion + Amount, MaxExhaustion);
    }

    public void tick(LivingEntity Owner) {
        Difficulty Setting = Owner.level().getDifficulty();
        boolean Regenerates = Owner.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        if (Setting == Difficulty.PEACEFUL && Regenerates) {
            if (Owner.getHealth() < Owner.getMaxHealth() && Owner.tickCount % 20 == 0) {
                Owner.heal(1.0f);
            }
            if (Saturation < MaxFood && Owner.tickCount % 20 == 0) {
                Saturation = Math.min(Food, Saturation + 1.0f);
            }
            if (needsFood() && Owner.tickCount % 10 == 0) {
                Food++;
            }
        }
        if (Exhaustion > 4.0f) {
            Exhaustion -= 4.0f;
            if (Saturation > 0.0f) {
                Saturation = Math.max(Saturation - 1.0f, 0.0f);
            } else if (Setting != Difficulty.PEACEFUL) {
                Food = Math.max(Food - 1, 0);
            }
        }
        boolean Hurt = Owner.getHealth() > 0.0f && Owner.getHealth() < Owner.getMaxHealth();
        if (Regenerates && Saturation > 0.0f && Hurt && Food >= MaxFood) {
            if (++Timer >= 10) {
                float Spent = Math.min(Saturation, 6.0f);
                Owner.heal(Spent / 6.0f);
                addExhaustion(Spent);
                Timer = 0;
            }
        } else if (Regenerates && Food >= 18 && Hurt) {
            if (++Timer >= 80) {
                Owner.heal(1.0f);
                addExhaustion(6.0f);
                Timer = 0;
            }
        } else if (Food <= 0) {
            if (++Timer >= 80) {
                if (Owner.getHealth() > 10.0f || Setting == Difficulty.HARD || Owner.getHealth() > 1.0f && Setting == Difficulty.NORMAL) {
                    Owner.hurt(Owner.damageSources().starve(), 1.0f);
                }
                Timer = 0;
            }
        } else {
            Timer = 0;
        }
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putInt("foodLevel", Food);
        Saved.putInt("foodTickTimer", Timer);
        Saved.putFloat("foodSaturationLevel", Saturation);
        Saved.putFloat("foodExhaustionLevel", Exhaustion);
        return Saved;
    }

    public void load(CompoundTag Saved) {
        if (Saved.contains("foodLevel")) {
            Food = Mth.clamp(Saved.getInt("foodLevel"), 0, MaxFood);
            Timer = Saved.getInt("foodTickTimer");
            Saturation = Saved.getFloat("foodSaturationLevel");
            Exhaustion = Saved.getFloat("foodExhaustionLevel");
        }
    }
}
