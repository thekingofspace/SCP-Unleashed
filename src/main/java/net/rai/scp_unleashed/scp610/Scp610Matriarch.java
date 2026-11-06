package net.rai.scp_unleashed.scp610;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class Scp610Matriarch extends Scp610Unit {
    public static final double AuraReach = 12.0;
    public static final int AuraGap = 40;
    public static final int AuraTicks = 60;
    public static final int RallyGap = 200;
    public static final int RallyTicks = 30;
    private int RallyReady = RallyGap;

    public Scp610Matriarch(EntityType<? extends Scp610Matriarch> Type, Level World) {
        super(Type, World, Scp610Form.Matriarch);
    }

    public int aura(ServerLevel World) {
        AABB Area = getBoundingBox().inflate(AuraReach, 6.0, AuraReach);
        List<Scp610Creature> Allies = World.getEntitiesOfClass(Scp610Creature.class, Area, Each -> Each.isAlive() && Each.distanceToSqr(this) <= AuraReach * AuraReach);
        for (Scp610Creature Ally : Allies) {
            Ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, AuraTicks, 0, true, false));
            Ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, AuraTicks, 0, true, false));
        }
        return Allies.size();
    }

    @Override
    protected void serverTick() {
        super.serverTick();
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        RallyReady = Math.max(0, RallyReady - 1);
        if (tickCount % AuraGap == 0) {
            int Count = aura(World);
            if (Count > 1 && RallyReady <= 0 && canAct() && getTarget() == null) {
                RallyReady = RallyGap;
                setAction(ActionRally, RallyTicks);
                playLayers(voice().Idle(), 1.6f, voice().Pitch() * 0.9f);
            }
        }
    }
}
