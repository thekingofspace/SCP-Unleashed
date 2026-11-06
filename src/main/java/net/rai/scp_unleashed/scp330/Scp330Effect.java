package net.rai.scp_unleashed.scp330;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

public class Scp330Effect extends MobEffect {
    private final boolean Stubborn;

    public Scp330Effect(MobEffectCategory Category, int Colour, boolean Stubborn) {
        super(Category, Colour);
        this.Stubborn = Stubborn;
    }

    @Override
    public void fillEffectCures(Set<EffectCure> Cures, MobEffectInstance Instance) {
        if (!Stubborn) {
            super.fillEffectCures(Cures, Instance);
        }
    }
}
