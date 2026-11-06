package net.rai.scp_unleashed.scp1730.guardian;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.scp1730.Scp1730Collapse;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;
import org.slf4j.Logger;

public final class Scp1730GuardianCapture {
    public static final String HomeKey = "Home";
    private static final Logger Log = LogUtils.getLogger();

    private Scp1730GuardianCapture() {
    }

    public static float threshold(Scp1730Guardian Guardian) {
        return (float) (Guardian.getMaxHealth() * Scp1730GuardianConfig.captureShare());
    }

    public static float floor(Scp1730Guardian Guardian) {
        double Share = Math.min(Scp1730GuardianConfig.windowShare(), Scp1730GuardianConfig.captureShare());
        return Math.max(1.0f, (float) (Guardian.getMaxHealth() * Share));
    }

    public static boolean anchoredNow(Scp1730Guardian Guardian) {
        return Guardian.level().isClientSide ? Guardian.anchored() : Guardian.senseAnchor();
    }

    public static boolean weakened(Scp1730Guardian Guardian) {
        return Guardian.getHealth() <= threshold(Guardian) + 1.0e-3f;
    }

    public static boolean capturable(Scp1730Guardian Guardian) {
        return Guardian.isAlive() && !Guardian.isDeadOrDying() && !Guardian.isRemoved() && weakened(Guardian) && anchoredNow(Guardian);
    }

    public static void window(Scp1730Guardian Guardian, DamageSource Source, float Before) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || Float.isNaN(Guardian.getHealth())) {
            return;
        }
        float Floor = floor(Guardian);
        if (Before > threshold(Guardian) && Guardian.getHealth() < Floor) {
            Guardian.setHealth(Floor);
        }
    }

    public static void captured(ServerLevel World, Player User, Mob Target, CompoundTag Data) {
        if (!(Target instanceof Scp1730Guardian Guardian)) {
            return;
        }
        Data.putString("id", Scp1730GuardianRegistry.ContainedType.getId().toString());
        Data.remove(HomeKey);
        Data.remove(Scp1730ContainedGuardian.BoundKey);
        Data.remove(Scp1730ContainedGuardian.FreeKey);
        if (Guardian instanceof Scp1730ContainedGuardian) {
            return;
        }
        Scp1730Collapse.guardianFell(World, Guardian);
        Log.info("SCP-1730-2 was taken into stasis by {} at {} in {}", User.getName().getString(), Guardian.blockPosition().toShortString(), World.dimension().location());
    }
}
