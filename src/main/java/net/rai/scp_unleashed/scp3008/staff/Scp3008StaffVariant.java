package net.rai.scp_unleashed.scp3008.staff;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.rai.scp_unleashed.motion.StrideGait;

public enum Scp3008StaffVariant {
    Stout("stout", 1.0f, 1.65f, 1.44f, 52.0, 7.0, 1.19, 0.3, 4.0, 0.7, true, false, 0.4083, 1.1049, 0.5f, 3),
    Lanky("lanky", 0.6f, 2.6f, 2.36f, 34.0, 5.0, 1.36, 1.0, 1.0, 0.2, false, false, 0.9526, 2.7755, 0.35f, 3),
    Lump("lump", 0.8f, 2.5f, 2.21f, 44.0, 6.0, 1.26, 0.5, 3.0, 0.4, false, false, 0.8165, 2.1094, 0.42f, 2),
    Stalk("stalk", 0.6f, 2.6f, 2.46f, 36.0, 5.0, 1.32, 0.6, 2.0, 0.3, false, false, 0.8846, 2.4424, 0.35f, 2),
    Thorn("thorn", 0.65f, 2.45f, 2.28f, 40.0, 6.0, 1.29, 0.6, 2.0, 0.4, false, true, 0.8165, 2.2148, 0.38f, 2),
    Hunch("hunch", 0.85f, 2.3f, 2.04f, 48.0, 8.0, 1.22, 0.9, 3.0, 0.6, true, false, 0.6592, 1.808, 0.48f, 2);

    public static final double StillPace = 0.004;
    public static final double RunEnter = 0.12;
    public static final double RunExit = 0.09;
    public static final double MaxStep = 0.6;
    public static final int Blend = 2;

    public final String Name;
    public final float Width;
    public final float Height;
    public final float Eye;
    public final double Health;
    public final double Damage;
    public final double HuntModifier;
    public final double Reach;
    public final double Armor;
    public final double Knockback;
    public final boolean ShieldBreaker;
    public final boolean Thorns;
    public final double WalkDistance;
    public final double RunDistance;
    public final float Shadow;
    public final int Weight;
    public final StrideGait.Shape Gait;
    public final EntityDimensions Size;

    Scp3008StaffVariant(String Name, float Width, float Height, float Eye, double Health, double Damage, double HuntModifier, double Reach, double Armor, double Knockback,
                        boolean ShieldBreaker, boolean Thorns, double WalkDistance, double RunDistance, float Shadow, int Weight) {
        this.Name = Name;
        this.Width = Width;
        this.Height = Height;
        this.Eye = Eye;
        this.Health = Health;
        this.Damage = Damage;
        this.HuntModifier = HuntModifier;
        this.Reach = Reach;
        this.Armor = Armor;
        this.Knockback = Knockback;
        this.ShieldBreaker = ShieldBreaker;
        this.Thorns = Thorns;
        this.WalkDistance = WalkDistance;
        this.RunDistance = RunDistance;
        this.Shadow = Shadow;
        this.Weight = Weight;
        this.Gait = new StrideGait.Shape(StillPace, RunEnter, RunExit, WalkDistance, RunDistance, MaxStep, Blend);
        this.Size = EntityDimensions.scalable(Width, Height).withEyeHeight(Eye);
    }

    public static Scp3008StaffVariant byId(int Id) {
        Scp3008StaffVariant[] All = values();
        return All[Math.floorMod(Id, All.length)];
    }

    public static Scp3008StaffVariant random(RandomSource Random) {
        int Total = 0;
        for (Scp3008StaffVariant Kind : values()) {
            Total += Kind.Weight;
        }
        int Roll = Random.nextInt(Total);
        for (Scp3008StaffVariant Kind : values()) {
            Roll -= Kind.Weight;
            if (Roll < 0) {
                return Kind;
            }
        }
        return Stout;
    }

    public String modelName() {
        return "scp3008_staff_" + Name;
    }

    public ResourceLocation model() {
        return Scp3008StaffRegistry.location("geo/entity/scp3008_staff/" + modelName() + ".geo.json");
    }

    public ResourceLocation texture() {
        return Scp3008StaffRegistry.location("textures/entity/scp3008_staff/" + modelName() + ".png");
    }

    public ResourceLocation animations() {
        return Scp3008StaffRegistry.location("animations/entity/scp3008_staff/" + modelName() + ".animation.json");
    }
}
