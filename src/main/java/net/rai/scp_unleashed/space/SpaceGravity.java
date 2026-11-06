package net.rai.scp_unleashed.space;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class SpaceGravity {
    public static final double GravityScale = 0.3;
    public static final double JumpScale = 0.8;
    public static final double SafeFallScale = 1.0 / GravityScale;
    public static final double FallDamageScale = GravityScale;
    public static final int CheckTicks = 10;
    public static final List<Tweak> Tweaks = List.of(
            new Tweak(Attributes.GRAVITY, new AttributeModifier(SpaceRegistry.location("space_low_gravity"), GravityScale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)),
            new Tweak(Attributes.JUMP_STRENGTH, new AttributeModifier(SpaceRegistry.location("space_low_gravity_jump"), JumpScale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)),
            new Tweak(Attributes.SAFE_FALL_DISTANCE, new AttributeModifier(SpaceRegistry.location("space_low_gravity_safe_fall"), SafeFallScale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)),
            new Tweak(Attributes.FALL_DAMAGE_MULTIPLIER, new AttributeModifier(SpaceRegistry.location("space_low_gravity_fall_damage"), FallDamageScale - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)));

    private SpaceGravity() {
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (Event.getEntity() instanceof LivingEntity Living) {
            apply(Living, SpaceAir.isAirless(Event.getLevel()));
        }
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Pre Event) {
        Entity Target = Event.getEntity();
        if (Target instanceof LivingEntity Living) {
            if (Living.tickCount % CheckTicks == 0) {
                apply(Living, SpaceAir.isAirless(Living.level()));
            }
            return;
        }
        if ((Target instanceof ItemEntity || Target instanceof ExperienceOrb || Target instanceof FallingBlockEntity) && !Target.onGround() && !Target.isInWater()
                && !Target.isInLava() && SpaceAir.isAirless(Target.level())) {
            double Gravity = Target.getGravity();
            if (Gravity > 0.0) {
                Vec3 Motion = Target.getDeltaMovement();
                Target.setDeltaMovement(Motion.x, Motion.y + Gravity * (1.0 - GravityScale), Motion.z);
            }
        }
    }

    public static boolean isApplied(LivingEntity Living) {
        AttributeInstance Instance = Living.getAttribute(Attributes.GRAVITY);
        return Instance != null && Instance.hasModifier(Tweaks.get(0).Modifier().id());
    }

    public static void apply(LivingEntity Living, boolean Low) {
        for (Tweak Change : Tweaks) {
            AttributeInstance Instance = Living.getAttribute(Change.Target());
            if (Instance == null) {
                continue;
            }
            ResourceLocation Id = Change.Modifier().id();
            boolean Has = Instance.hasModifier(Id);
            if (Low && !Has) {
                Instance.addTransientModifier(Change.Modifier());
            } else if (!Low && Has) {
                Instance.removeModifier(Id);
            }
        }
    }

    public record Tweak(Holder<Attribute> Target, AttributeModifier Modifier) {
    }
}
