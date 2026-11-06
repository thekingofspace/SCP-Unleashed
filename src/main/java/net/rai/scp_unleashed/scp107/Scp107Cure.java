package net.rai.scp_unleashed.scp107;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp107Cure {
    private Scp107Cure() {
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        if (Scp107Storm.oiled(Target) && !drenched(Event.world(), Target)) {
            Event.markCured(Scp107Storm.wash(Target));
        }
    }

    private static boolean drenched(ServerLevel World, LivingEntity Target) {
        if (World.dimension() != Level.OVERWORLD) {
            return false;
        }
        Scp107Rain State = Scp107Rain.get(World.getServer());
        return State.kind() == Scp107RainKind.Oil && State.strength() >= Scp107Storm.ActiveFloor && Scp107Storm.caught(World, Target);
    }
}
