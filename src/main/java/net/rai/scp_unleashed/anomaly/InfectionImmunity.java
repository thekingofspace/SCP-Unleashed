package net.rai.scp_unleashed.anomaly;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public final class InfectionImmunity {
    public static final TagKey<EntityType<?>> Immune = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "infection_immune"));
    public static final TagKey<EntityType<?>> SemiImmortal = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "semi_immortal"));

    private record Absorber(BiPredicate<LivingEntity, ResourceLocation> Takes, BiConsumer<LivingEntity, ResourceLocation> Absorb) {
    }

    private static final List<BiPredicate<Entity, TagKey<EntityType<?>>>> Spares = new CopyOnWriteArrayList<>();
    private static final List<Absorber> Absorbers = new CopyOnWriteArrayList<>();

    private InfectionImmunity() {
    }

    public static void addSpare(BiPredicate<Entity, TagKey<EntityType<?>>> Spare) {
        Spares.add(Spare);
    }

    public static void addAbsorber(BiPredicate<LivingEntity, ResourceLocation> Takes, BiConsumer<LivingEntity, ResourceLocation> Absorb) {
        Absorbers.add(new Absorber(Takes, Absorb));
    }

    private static boolean spared(Entity Target, TagKey<EntityType<?>> Own) {
        for (BiPredicate<Entity, TagKey<EntityType<?>>> Each : Spares) {
            if (Each.test(Target, Own)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isImmune(@Nullable Entity Target) {
        if (!(Target instanceof LivingEntity Living) || Living instanceof ArmorStand || Living.isInvulnerable() || Living.getType().is(Immune) || Targeting.isStatue(Living)) {
            return true;
        }
        return Living instanceof Player User && (User.isCreative() || User.isSpectator());
    }

    public static boolean isImmune(@Nullable Entity Target, TagKey<EntityType<?>> Own) {
        return isImmune(Target) || Target.getType().is(Own) || spared(Target, Own);
    }

    public static boolean absorbs(@Nullable Entity Target, ResourceLocation Agent) {
        if (!(Target instanceof LivingEntity Living) || !Living.isAlive()) {
            return false;
        }
        for (Absorber Each : Absorbers) {
            if (Each.Takes().test(Living, Agent)) {
                return true;
            }
        }
        return false;
    }

    public static boolean absorb(@Nullable Entity Target, ResourceLocation Agent) {
        if (!(Target instanceof LivingEntity Living) || !Living.isAlive() || Living.level().isClientSide) {
            return false;
        }
        boolean Taken = false;
        for (Absorber Each : Absorbers) {
            if (Each.Takes().test(Living, Agent)) {
                Each.Absorb().accept(Living, Agent);
                Taken = true;
            }
        }
        return Taken;
    }
}
