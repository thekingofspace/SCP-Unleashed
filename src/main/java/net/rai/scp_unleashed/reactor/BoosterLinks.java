package net.rai.scp_unleashed.reactor;

import net.rai.scp_unleashed.reactor.core.CoreTower;
import net.rai.scp_unleashed.reactor.core.TowerType;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class BoosterLinks {
    private BoosterLinks() {
    }

    public static float gate(CoreTower Booster) {
        float Open = 0.0f;
        for (CoreTower.Contact Contact : Booster.RodContacts) {
            Open = Math.max(Open, 1.0f - Contact.Other().Insertion / 100.0f);
        }
        return Booster.strength() * Open;
    }

    public static int reachedFuel(CoreTower Booster) {
        Set<CoreTower> Reached = Collections.newSetFromMap(new IdentityHashMap<>());
        for (CoreTower.Contact Contact : Booster.RodContacts) {
            for (CoreTower.Contact Fuel : Contact.Other().FuelContacts) {
                if (Fuel.Other().Type == TowerType.Fuel && Math.max(Contact.Low(), Fuel.Low()) <= Math.min(Contact.High(), Fuel.High())) {
                    Reached.add(Fuel.Other());
                }
            }
        }
        return Reached.size();
    }

    public static float rodPass(CoreTower Rod) {
        float Strength = 0.0f;
        for (CoreTower.Contact Contact : Rod.BoosterContacts) {
            Strength += Contact.Other().strength();
        }
        return Math.min(1.0f, Strength) * (1.0f - Rod.Insertion / 100.0f);
    }
}
