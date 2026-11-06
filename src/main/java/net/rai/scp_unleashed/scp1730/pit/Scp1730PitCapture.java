package net.rai.scp_unleashed.scp1730.pit;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.List;

public final class Scp1730PitCapture {
    public static final List<String> PitKeys = List.of("HasHome", "HomeX", "HomeZ", "HalfX", "HalfZ", "RimY", "Weakness", "PitState", "StateTicks", "PullWait",
            "attributes", "NoGravity", "DeathTime", "HurtTime", "HurtByTimestamp", "FallFlying", "Brain", "Rotation");
    private static final Logger Log = LogUtils.getLogger();

    private Scp1730PitCapture() {
    }

    public static boolean refuses(Entity Target) {
        if (Target instanceof Scp1730PitForm Pit) {
            return !Pit.dazed();
        }
        if (Target instanceof Scp1730Mass Mass) {
            return !Mass.contained();
        }
        return Target instanceof Scp1730PitWhip || Target instanceof Scp1730PitTongue;
    }

    public static void captured(ServerLevel World, Player User, Mob Target, CompoundTag Data) {
        if (!(Target instanceof Scp1730PitForm Pit)) {
            return;
        }
        Data.putString("id", Scp1730PitRegistry.MassType.getId().toString());
        for (String Key : PitKeys) {
            Data.remove(Key);
        }
        Data.putFloat("Health", Scp1730Mass.MaxHealth);
        Pit.captured(World);
        Log.info("SCP-1730-3 was taken into stasis by {} at {} in {}", User.getName().getString(), Pit.blockPosition().toShortString(), World.dimension().location());
    }

    public static void released(ServerLevel World, Entity Spawned) {
        if (Spawned instanceof Scp1730Mass Mass) {
            Mass.released(World);
        }
    }
}
