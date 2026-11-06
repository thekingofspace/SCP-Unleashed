package net.rai.scp_unleashed.mtf;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MtfMission {
    public enum Stage {
        Inbound,
        Deploying,
        Active,
        Extracting
    }

    public static final int EngageMemory = 200;
    public static final int AimMemory = 40;
    public static final int FocusMemory = 60;

    private final UUID Caller;
    private final ResourceKey<Level> Dimension;
    private final MtfTier Tier;
    private final long Called;
    private MtfLandingZone.Zone Zone;
    private Stage Current = Stage.Inbound;
    @Nullable
    private UUID Helicopter;
    private final List<UUID> Operatives = new ArrayList<>();
    private int Deployed;
    private int Boarded;
    private int Fallen;
    private long ActiveSince = -1L;
    private long Ends = -1L;
    private boolean ExtractRequested;
    @Nullable
    private UUID Engaged;
    private long EngagedAt;
    @Nullable
    private UUID Attacker;
    private long AttackerAt;
    @Nullable
    private UUID Aimed;
    private long AimedAt;
    @Nullable
    private UUID Focus;
    private long FocusAt;

    public MtfMission(UUID Caller, ResourceKey<Level> Dimension, MtfTier Tier, MtfLandingZone.Zone Zone, long Called) {
        this.Caller = Caller;
        this.Dimension = Dimension;
        this.Tier = Tier;
        this.Zone = Zone;
        this.Called = Called;
    }

    public UUID caller() {
        return Caller;
    }

    public ResourceKey<Level> dimension() {
        return Dimension;
    }

    public MtfTier tier() {
        return Tier;
    }

    public long called() {
        return Called;
    }

    public MtfLandingZone.Zone zone() {
        return Zone;
    }

    public void setZone(MtfLandingZone.Zone Value) {
        Zone = Value;
    }

    public Stage stage() {
        return Current;
    }

    public void setStage(Stage Value) {
        Current = Value;
    }

    @Nullable
    public UUID helicopter() {
        return Helicopter;
    }

    public void setHelicopter(@Nullable UUID Value) {
        Helicopter = Value;
    }

    public List<UUID> operatives() {
        return Operatives;
    }

    public void enlist(UUID Id) {
        if (!Operatives.contains(Id)) {
            Operatives.add(Id);
            Deployed++;
        }
    }

    public int deployed() {
        return Deployed;
    }

    public int boarded() {
        return Boarded;
    }

    public void board(UUID Id) {
        if (Operatives.remove(Id)) {
            Boarded++;
        }
    }

    public int alive() {
        return Operatives.size();
    }

    public void fall(UUID Id) {
        if (Operatives.remove(Id)) {
            Fallen++;
        }
    }

    public int fallen() {
        return Fallen;
    }

    public boolean wipedOut() {
        return Deployed > 0 && Fallen >= Deployed;
    }

    public long activeSince() {
        return ActiveSince;
    }

    public long ends() {
        return Ends;
    }

    public void start(long Now) {
        ActiveSince = Now;
        Ends = Now + Tier.durationTicks();
        Current = Stage.Active;
    }

    public int remaining(long Now) {
        if (Ends < 0L) {
            return Tier.durationTicks();
        }
        return (int) Math.max(0L, Ends - Now);
    }

    public boolean extractRequested() {
        return ExtractRequested;
    }

    public void requestExtraction() {
        ExtractRequested = true;
    }

    public void noteEngaged(Entity Target, long Now) {
        Engaged = Target.getUUID();
        EngagedAt = Now;
    }

    public void noteAttacker(Entity Target, long Now) {
        Attacker = Target.getUUID();
        AttackerAt = Now;
    }

    public void noteAimed(Entity Target, long Now) {
        Aimed = Target.getUUID();
        AimedAt = Now;
    }

    public void noteFocus(Entity Target, long Now) {
        Focus = Target.getUUID();
        FocusAt = Now;
    }

    @Nullable
    private static LivingEntity living(ServerLevel World, @Nullable UUID Id) {
        if (Id == null) {
            return null;
        }
        return World.getEntity(Id) instanceof LivingEntity Found && Found.isAlive() ? Found : null;
    }

    @Nullable
    public LivingEntity callerFight(ServerLevel World, long Now) {
        LivingEntity Best = null;
        long BestAt = Long.MIN_VALUE;
        if (Now - AimedAt <= AimMemory) {
            LivingEntity Found = living(World, Aimed);
            if (Found != null && MtfFriends.canEngage(Caller, Found, true)) {
                Best = Found;
                BestAt = AimedAt + EngageMemory;
            }
        }
        if (Now - EngagedAt <= EngageMemory && EngagedAt > BestAt) {
            LivingEntity Found = living(World, Engaged);
            if (Found != null && MtfFriends.canEngage(Caller, Found, true)) {
                Best = Found;
                BestAt = EngagedAt;
            }
        }
        if (Now - AttackerAt <= EngageMemory && AttackerAt > BestAt) {
            LivingEntity Found = living(World, Attacker);
            if (Found != null && MtfFriends.canEngage(Caller, Found, true)) {
                Best = Found;
            }
        }
        return Best;
    }

    @Nullable
    public LivingEntity focus(ServerLevel World, long Now) {
        if (Now - FocusAt > FocusMemory) {
            return null;
        }
        LivingEntity Found = living(World, Focus);
        return Found != null && MtfFriends.canEngage(Caller, Found, false) ? Found : null;
    }
}
