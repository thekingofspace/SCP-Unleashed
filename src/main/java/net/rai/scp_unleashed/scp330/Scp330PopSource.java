package net.rai.scp_unleashed.scp330;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class Scp330PopSource extends DamageSource {
    public static final String MessageKey = "death.attack.scp_unleashed.scp330_pop";

    public Scp330PopSource(Holder<DamageType> Type, Entity Eater) {
        super(Type, null, Eater);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity Victim) {
        Entity Eater = getEntity();
        if (Eater == null || Eater == Victim || Eater.getUUID().equals(Victim.getUUID())) {
            return Component.translatable(MessageKey, Victim.getDisplayName());
        }
        return Component.translatable(MessageKey + ".player", Victim.getDisplayName(), Eater.getDisplayName());
    }
}
