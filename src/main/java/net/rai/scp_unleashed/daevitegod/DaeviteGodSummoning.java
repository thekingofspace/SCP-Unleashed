package net.rai.scp_unleashed.daevitegod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp140.api.DaeviteSummonEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DaeviteGodSummoning {
    public static final double AltarLift = 1.0;

    private DaeviteGodSummoning() {
    }

    @SubscribeEvent
    public static void summoned(DaeviteSummonEvent Event) {
        if (Event.isAnswered()) {
            return;
        }
        ServerLevel World = Event.getLevel();
        BlockPos Altar = Event.getAltar();
        DaeviteGod God = call(World, Vec3.atBottomCenterOf(Altar).add(0.0, AltarLift, 0.0));
        if (God != null) {
            Event.answer(God);
        }
    }

    @Nullable
    public static DaeviteGod call(ServerLevel World, Vec3 At) {
        DaeviteGodLedger Ledger = DaeviteGodLedger.get(World.getServer());
        DaeviteGod Existing = Ledger.live(World.getServer());
        if (Existing != null) {
            if (Existing.level() == World) {
                Existing.recall(World, At);
                return Existing;
            }
            int Stage = Existing.stage();
            float Health = Existing.getHealth();
            Existing.vanishSpirits((ServerLevel) Existing.level());
            Existing.discard();
            return spawn(World, At, Stage, Health);
        }
        int Stage = Ledger.alive() ? Ledger.stage() : 1;
        float Health = Ledger.alive() ? Ledger.health() : -1.0f;
        return spawn(World, At, Stage, Health);
    }

    @Nullable
    private static DaeviteGod spawn(ServerLevel World, Vec3 At, int Stage, float Health) {
        DaeviteGod God = DaeviteGodRegistry.God.get().create(World);
        if (God == null) {
            return null;
        }
        God.moveTo(At.x, At.y, At.z, World.random.nextFloat() * 360.0f, 0.0f);
        God.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(At)), MobSpawnType.EVENT, null);
        God.adopt(Stage, Health);
        God.markClaimed();
        DaeviteGodLedger.get(World.getServer()).claim(God);
        if (God.king()) {
            God.recall(World, At);
        } else {
            God.beginEntrance(At);
        }
        World.addFreshEntityWithPassengers(God);
        return God;
    }
}
