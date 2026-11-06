package net.rai.scp_unleashed.faction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class FactionEvents {
    private FactionEvents() {
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        Entity Body = Event.getEntity();
        FactionBond Bond = Factions.bondOf(Body);
        if (!Bond.bound()) {
            for (FactionType Type : Factions.all()) {
                if (Type.claims(Body)) {
                    FactionControl.stamp(Type, FactionWorld.state(World.getServer(), Type), Body);
                    Bond = Factions.bondOf(Body);
                    break;
                }
            }
            if (!Bond.bound()) {
                return;
            }
        }
        FactionType Type = Factions.factionOf(Body);
        if (Type == null) {
            return;
        }
        FactionState State = FactionWorld.state(World.getServer(), Type);
        if (!FactionControl.current(State, Bond)) {
            State.dismiss(Body.getUUID());
            Type.staleMember(World, Body);
            return;
        }
        FactionLive.add(Type.id(), Body);
        if (!Bond.enlisted()) {
            return;
        }
        FactionMember Known = State.member(Body.getUUID());
        if (Known != null) {
            State.touch(Known.seenAt(World.dimension(), Body.blockPosition()));
        } else if (State.roster().size() < Type.rules().RosterCap()) {
            State.enroll(new FactionMember(Body.getUUID(), Bond.Site(), Bond.Role(), World.dimension(), Body.blockPosition()));
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        Entity Body = Event.getEntity();
        FactionBond Bond = Factions.bondOf(Body);
        if (!Bond.bound()) {
            return;
        }
        ResourceLocation Id = ResourceLocation.tryParse(Bond.Faction());
        if (Id == null) {
            return;
        }
        FactionLive.remove(Id, Body);
        FactionType Type = Factions.get(Id);
        if (Type == null || !Bond.enlisted()) {
            return;
        }
        FactionState State = FactionWorld.state(World.getServer(), Type);
        Entity.RemovalReason Reason = Body.getRemovalReason();
        if (Reason == Entity.RemovalReason.DISCARDED || Reason == Entity.RemovalReason.KILLED) {
            State.dismiss(Body.getUUID());
            return;
        }
        FactionMember Known = State.member(Body.getUUID());
        if (Known != null) {
            State.touch(Known.seenAt(World.dimension(), Body.blockPosition()));
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent Event) {
        LivingEntity Body = Event.getEntity();
        if (!(Body.level() instanceof ServerLevel World)) {
            return;
        }
        FactionBond Bond = Factions.bondOf(Body);
        FactionType Lost = Factions.factionOf(Body);
        if (Lost != null && Bond.enlisted()) {
            FactionState State = FactionWorld.state(World.getServer(), Lost);
            if (State.dismiss(Body.getUUID())) {
                State.addLoss();
            }
            if (FactionControl.current(State, Bond)) {
                Lost.memberLost(World, State, Body, Event.getSource());
            }
        }
        Entity Killer = Event.getSource().getEntity();
        if (Killer instanceof LivingEntity Member && Killer != Body) {
            FactionType Winner = Factions.factionOf(Member);
            if (Winner != null) {
                FactionState State = FactionWorld.state(World.getServer(), Winner);
                if (FactionControl.current(State, Factions.bondOf(Member))) {
                    State.addKill();
                    Winner.memberKilled(World, State, Member, Body);
                }
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        FactionLive.clear();
    }
}
