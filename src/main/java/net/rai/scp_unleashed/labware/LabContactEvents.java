package net.rai.scp_unleashed.labware;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class LabContactEvents {
    public static final int TouchInterval = 10;
    public static final double TouchReach = 0.2;

    private LabContactEvents() {
    }

    public static List<LabOrganism> contagious() {
        List<LabOrganism> Found = new ArrayList<>();
        for (LabOrganism Organism : LabOrganisms.all()) {
            if (Organism.spreadsByContact()) {
                Found.add(Organism);
            }
        }
        return Found;
    }

    public static boolean transmit(LabOrganism Organism, LivingEntity From, LivingEntity To, LabContact Route) {
        if (From == To || !To.isAlive() || To.isSpectator() || To instanceof Player User && User.isCreative()) {
            return false;
        }
        if (!Organism.isPresentOn(From) || Organism.isPresentOn(To) || Organism.shields(From) || Organism.shields(To)) {
            return false;
        }
        float Chance = Organism.contactChance(From, Route);
        if (Chance <= 0.0f || From.getRandom().nextFloat() >= Chance) {
            return false;
        }
        return Organism.infect(To);
    }

    public static int transmit(LivingEntity From, LivingEntity To, LabContact Route) {
        int Count = 0;
        for (LabOrganism Organism : contagious()) {
            if (transmit(Organism, From, To, Route)) {
                Count++;
            }
        }
        return Count;
    }

    @SubscribeEvent
    public static void struck(LivingDamageEvent.Post Event) {
        LivingEntity Victim = Event.getEntity();
        if (Victim.level().isClientSide || Event.getNewDamage() <= 0.0f) {
            return;
        }
        if (Event.getSource().getDirectEntity() instanceof LivingEntity Attacker) {
            transmit(Attacker, Victim, LabContact.Strike);
        }
    }

    @SubscribeEvent
    public static void touch(EntityTickEvent.Post Event) {
        Entity Ticked = Event.getEntity();
        if (Ticked.level().isClientSide || !(Ticked instanceof LivingEntity Carrier) || !Carrier.isAlive()
                || Math.floorMod(Ticked.level().getGameTime() + Ticked.getId(), TouchInterval) != 0) {
            return;
        }
        List<LabOrganism> Carried = null;
        for (LabOrganism Organism : LabOrganisms.all()) {
            if (Organism.spreadsByContact() && Organism.isPresentOn(Carrier) && Organism.contactChance(Carrier, LabContact.Touch) > 0.0f) {
                if (Carried == null) {
                    Carried = new ArrayList<>();
                }
                Carried.add(Organism);
            }
        }
        if (Carried == null) {
            return;
        }
        AABB Reach = Carrier.getBoundingBox().inflate(TouchReach);
        for (LivingEntity Other : Carrier.level().getEntitiesOfClass(LivingEntity.class, Reach, Found -> Found != Carrier && Found.isAlive())) {
            for (LabOrganism Organism : Carried) {
                transmit(Organism, Carrier, Other, LabContact.Touch);
            }
        }
    }
}
