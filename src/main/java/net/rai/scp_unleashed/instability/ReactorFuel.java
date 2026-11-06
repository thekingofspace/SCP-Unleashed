package net.rai.scp_unleashed.instability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.anomaly.RiftFragment;
import org.jetbrains.annotations.Nullable;

public enum ReactorFuel {
    Faint(FragmentRarity.Faint, 400, 24, 0.015f, 0.98f, 1400),
    Wavering(FragmentRarity.Wavering, 900, 30, 0.025f, 0.95f, 900),
    Volatile(FragmentRarity.Volatile, 1800, 36, 0.035f, 0.91f, 600),
    Severe(FragmentRarity.Severe, 3500, 45, 0.045f, 0.87f, 400),
    Abyssal(FragmentRarity.Abyssal, 6000, 60, 0.06f, 0.82f, 260);

    public static final int TicksPerMinute = 1200;
    public static final float MaxKick = 1.6f;

    private final FragmentRarity Grade;
    private final int Output;
    private final int Minutes;
    private final float Swing;
    private final float Calm;
    private final int SurgeInterval;

    ReactorFuel(FragmentRarity Grade, int Output, int Minutes, float Swing, float Calm, int SurgeInterval) {
        this.Grade = Grade;
        this.Output = Output;
        this.Minutes = Minutes;
        this.Swing = Swing;
        this.Calm = Calm;
        this.SurgeInterval = SurgeInterval;
    }

    @Nullable
    public static ReactorFuel of(ItemStack Stack) {
        if (Stack.isEmpty() || !(Stack.getItem() instanceof RiftFragment Fragment)) {
            return null;
        }
        return of(Fragment.getRarity());
    }

    @Nullable
    public static ReactorFuel of(FragmentRarity Grade) {
        for (ReactorFuel Fuel : values()) {
            if (Fuel.Grade == Grade) {
                return Fuel;
            }
        }
        return null;
    }

    @Nullable
    public static ReactorFuel byTier(int Tier) {
        for (ReactorFuel Fuel : values()) {
            if (Fuel.getTier() == Tier) {
                return Fuel;
            }
        }
        return null;
    }

    public static boolean isFuel(ItemStack Stack) {
        return of(Stack) != null;
    }

    public FragmentRarity getGrade() {
        return Grade;
    }

    public int getTier() {
        return Grade.getTier();
    }

    public int getColour() {
        return Grade.getColour() & 0xFFFFFF;
    }

    public int getOutput() {
        return Output;
    }

    public int getMinutes() {
        return Minutes;
    }

    public int getBurnTicks() {
        return Minutes * TicksPerMinute;
    }

    public long getTotalEnergy() {
        return (long) Output * getBurnTicks();
    }

    public int getPeak() {
        return (int) Math.ceil(Output * (1.0f + Swing * MaxKick));
    }

    public float getSwing() {
        return Swing;
    }

    public float getCalm() {
        return Calm;
    }

    public int getSurgeInterval() {
        return SurgeInterval;
    }

    public ResourceLocation getItemId() {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "unstable_fragment_" + Grade.getSerializedName());
    }
}
