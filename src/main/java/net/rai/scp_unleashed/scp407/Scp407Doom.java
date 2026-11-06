package net.rai.scp_unleashed.scp407;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.rai.scp_unleashed.anomaly.Targeting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Scp407Doom extends SavedData {
    public static final String DataName = "scp_unleashed_scp407_doom";
    public static final int SweepTicks = 40;
    private static final SavedData.Factory<Scp407Doom> DataFactory = new SavedData.Factory<>(Scp407Doom::new, Scp407Doom::load);
    private final Set<UUID> Pending = new LinkedHashSet<>();

    public static Scp407Doom get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean isPending(UUID Key) {
        return Pending.contains(Key);
    }

    public int size() {
        return Pending.size();
    }

    public void add(UUID Key) {
        if (Pending.add(Key)) {
            setDirty();
        }
    }

    public void forget(UUID Key) {
        if (Pending.remove(Key)) {
            setDirty();
        }
    }

    public void clear() {
        if (!Pending.isEmpty()) {
            Pending.clear();
            setDirty();
        }
    }

    public static boolean strike(MinecraftServer Server, UUID Key) {
        LivingEntity Target = Scp407Hearing.resolve(Server, Key);
        if (Target == null || !Targeting.isTargetable(Target)) {
            get(Server).add(Key);
            return false;
        }
        get(Server).forget(Key);
        Scp407Fusion.release(Target);
        if (!Target.isAlive() || Target instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return true;
        }
        DamageSource Source = Target.damageSources().source(Scp407Registry.Withered);
        Target.level().playSound(null, Target.blockPosition(), Scp407Registry.WitherCrumble.get(), Target.getSoundSource(), 1.0f, 0.8f);
        Target.hurt(Source, Float.MAX_VALUE);
        return true;
    }

    public void sweep(MinecraftServer Server) {
        if (Pending.isEmpty()) {
            return;
        }
        List<UUID> Keys = new ArrayList<>(Pending);
        for (UUID Key : Keys) {
            if (Scp407Hearing.resolve(Server, Key) != null) {
                strike(Server, Key);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag Data, HolderLookup.Provider Lookup) {
        ListTag Keys = new ListTag();
        for (UUID Key : Pending) {
            CompoundTag Saved = new CompoundTag();
            Saved.putUUID("Key", Key);
            Keys.add(Saved);
        }
        Data.put("Pending", Keys);
        return Data;
    }

    public static Scp407Doom load(CompoundTag Data, HolderLookup.Provider Lookup) {
        Scp407Doom Loaded = new Scp407Doom();
        ListTag Keys = Data.getList("Pending", CompoundTag.TAG_COMPOUND);
        for (int Index = 0; Index < Keys.size(); Index++) {
            CompoundTag Saved = Keys.getCompound(Index);
            if (Saved.hasUUID("Key")) {
                Loaded.Pending.add(Saved.getUUID("Key"));
            }
        }
        return Loaded;
    }
}
