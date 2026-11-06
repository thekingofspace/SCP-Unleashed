package net.rai.scp_unleashed.scp294;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class Scp294DamageSource extends DamageSource {
    private final String DeathMessage;

    public Scp294DamageSource(Holder<DamageType> Type, String DeathMessage) {
        super(Type);
        this.DeathMessage = DeathMessage == null ? "" : DeathMessage;
    }

    public static Scp294DamageSource of(Level World, ResourceKey<DamageType> Key, String DeathMessage) {
        return new Scp294DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Key), DeathMessage);
    }

    public String getDeathMessage() {
        return DeathMessage;
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity Victim) {
        if (DeathMessage.isBlank()) {
            return super.getLocalizedDeathMessage(Victim);
        }
        return Component.translatableWithFallback(DeathMessage, DeathMessage, Victim.getDisplayName());
    }
}
