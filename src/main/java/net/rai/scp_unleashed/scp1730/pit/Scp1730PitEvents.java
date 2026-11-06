package net.rai.scp_unleashed.scp1730.pit;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp1730.Scp1730State;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Pit;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp1730PitEvents {
    public static final int JobDweller = 60;
    public static final int SpawnEvery = 10;
    public static final double RoomReach = 9.0;

    private Scp1730PitEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp1730PitRegistry.PitType.get(), Scp1730PitForm.createAttributes().build());
        Event.put(Scp1730PitRegistry.MassType.get(), Scp1730Mass.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp1730PitRegistry.PitEgg.get());
            Event.accept(Scp1730PitRegistry.MassEgg.get());
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        if (Server.getTickCount() % SpawnEvery != 0) {
            return;
        }
        ServerLevel World = Scp1730World.level(Server);
        if (World == null) {
            return;
        }
        Scp1730State State = Scp1730State.get(Server);
        if (!State.active()) {
            return;
        }
        spawnFor(World, State, World.players());
    }

    public static boolean nearPit(Scp1730Pit Pit, int Lx, double Y, int Lz) {
        return Lx >= Pit.MinX() - RoomReach && Lx <= Pit.MaxX() + RoomReach && Lz >= Pit.MinZ() - RoomReach && Lz <= Pit.MaxZ() + RoomReach && Y >= Pit.RimY() - 3
                && Y <= Pit.RimY() + 9;
    }

    public static void spawnFor(ServerLevel World, Scp1730State State, List<ServerPlayer> Players) {
        int Run = State.run();
        Scp1730Plan Plan = Scp1730World.plan(Run);
        for (Scp1730Pit Pit : Plan.pits()) {
            if (State.jobDone(Pit.Zone(), JobDweller)) {
                continue;
            }
            for (ServerPlayer User : Players) {
                if (User.isSpectator() || !User.isAlive() || User.level() != World) {
                    continue;
                }
                int Lx = Scp1730World.localX(Run, User.getX());
                int Lz = Scp1730World.localZ(User.getZ());
                if (!nearPit(Pit, Lx, User.getY(), Lz)) {
                    continue;
                }
                double MinX = Scp1730World.originX(Run) + Pit.MinX();
                double MinZ = Scp1730World.originZ() + Pit.MinZ();
                double MaxX = Scp1730World.originX(Run) + Pit.MaxX() + 1.0;
                double MaxZ = Scp1730World.originZ() + Pit.MaxZ() + 1.0;
                if (!World.hasChunkAt(net.minecraft.core.BlockPos.containing((MinX + MaxX) / 2.0, Pit.RimY(), (MinZ + MaxZ) / 2.0))) {
                    break;
                }
                if (World.getEntitiesOfClass(Scp1730PitForm.class, new AABB(MinX, Pit.BottomY() - 4, MinZ, MaxX, Pit.RimY() + 10, MaxZ)).isEmpty()) {
                    Scp1730PitForm.spawnIn(World, MinX, MinZ, MaxX, MaxZ, Pit.RimY());
                }
                State.markJob(Pit.Zone(), JobDweller);
                break;
            }
        }
    }
}
