package net.rai.scp_unleashed.scp140;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp140.api.Scp140Pages;

public final class Scp140Ink {
    public static final double WitnessRadius = 16.0;
    public static final double PersonFactor = 2.5;
    public static final double FirstPageCost = 120.0;
    public static final double CostGrowth = 1.12;
    public static final double CostStep = 5.0;
    public static final double GlistenFraction = 0.5;
    public static final double WhisperFraction = 0.85;
    public static final long FreshTicks = 6000L;
    public static final int WhisperInterval = 20;
    public static final int WhisperChance = 30;

    private Scp140Ink() {
    }

    public static double cost(int Page) {
        int Clamped = Math.max(1, Math.min(Scp140Pages.Total, Page));
        return Math.round(FirstPageCost * Math.pow(CostGrowth, Clamped - 1) / CostStep) * CostStep;
    }

    public static double costThrough(int Page) {
        double Total = 0.0;
        for (int Index = 1; Index <= Math.min(Scp140Pages.Total, Page); Index++) {
            Total += cost(Index);
        }
        return Total;
    }

    public static boolean isPerson(LivingEntity Victim) {
        return Victim instanceof Player || Victim instanceof AbstractVillager || ControlBodies.isPersonLike(Victim);
    }

    public static double inkFor(LivingEntity Victim, float Damage) {
        double Felt = Math.min(Damage, Math.max(1.0f, Victim.getMaxHealth()));
        return Felt * (isPerson(Victim) ? PersonFactor : 1.0);
    }
}
