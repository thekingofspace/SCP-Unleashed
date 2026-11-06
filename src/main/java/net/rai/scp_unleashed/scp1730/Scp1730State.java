package net.rai.scp_unleashed.scp1730;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class Scp1730State extends SavedData {
    public static final String Name = "scp_unleashed_scp1730";
    public static final int ZoneCount = 4;
    public static final int SealedZones = 3;
    public static final int HeavyZone = 2;
    public static final int DeepZone = 3;

    private int Run = -1;
    private boolean Active;
    private long EmptySince = -1L;
    private long Started;
    private final boolean[] Power = {true, true, true, true};
    private final boolean[] Open = new boolean[SealedZones];
    private int Populated;
    private boolean GuardianDead;
    private boolean CorePulled;
    private long CollapseStart = -1L;
    private final long[] NextBreach = new long[ZoneCount];
    private final long[] Jobs = new long[ZoneCount];
    private final long[] WingClose = new long[ZoneCount];

    public static Scp1730State get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Scp1730State::new, Scp1730State::load, null), Name);
    }

    public static Scp1730State load(CompoundTag Tag, HolderLookup.Provider Lookup) {
        Scp1730State State = new Scp1730State();
        State.Run = Tag.getInt("Run");
        if (!Tag.contains("Run")) {
            State.Run = -1;
        }
        State.Active = Tag.getBoolean("Active");
        State.EmptySince = Tag.contains("EmptySince") ? Tag.getLong("EmptySince") : -1L;
        State.Started = Tag.getLong("Started");
        for (int Zone = 0; Zone < ZoneCount; Zone++) {
            State.Power[Zone] = !Tag.getBoolean("Cut" + Zone);
            State.NextBreach[Zone] = Tag.getLong("Breach" + Zone);
            State.Jobs[Zone] = Tag.getLong("Jobs" + Zone);
            State.WingClose[Zone] = Tag.getLong("WingClose" + Zone);
        }
        for (int Zone = 0; Zone < SealedZones; Zone++) {
            State.Open[Zone] = Tag.getBoolean("Open" + Zone);
        }
        State.Populated = Tag.getInt("Populated");
        State.GuardianDead = Tag.getBoolean("GuardianDead");
        State.CorePulled = Tag.getBoolean("CorePulled");
        State.CollapseStart = Tag.contains("CollapseStart") ? Tag.getLong("CollapseStart") : -1L;
        return State;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Lookup) {
        Tag.putInt("Run", Run);
        Tag.putBoolean("Active", Active);
        Tag.putLong("EmptySince", EmptySince);
        Tag.putLong("Started", Started);
        for (int Zone = 0; Zone < ZoneCount; Zone++) {
            Tag.putBoolean("Cut" + Zone, !Power[Zone]);
            Tag.putLong("Breach" + Zone, NextBreach[Zone]);
            Tag.putLong("Jobs" + Zone, Jobs[Zone]);
            Tag.putLong("WingClose" + Zone, WingClose[Zone]);
        }
        for (int Zone = 0; Zone < SealedZones; Zone++) {
            Tag.putBoolean("Open" + Zone, Open[Zone]);
        }
        Tag.putInt("Populated", Populated);
        Tag.putBoolean("GuardianDead", GuardianDead);
        Tag.putBoolean("CorePulled", CorePulled);
        Tag.putLong("CollapseStart", CollapseStart);
        return Tag;
    }

    public int run() {
        return Run;
    }

    public boolean active() {
        return Active && Run >= 0;
    }

    public void startRun(long Now) {
        Run++;
        Active = true;
        Started = Now;
        EmptySince = -1L;
        for (int Zone = 0; Zone < ZoneCount; Zone++) {
            Power[Zone] = true;
            NextBreach[Zone] = 0L;
            Jobs[Zone] = 0L;
            WingClose[Zone] = 0L;
        }
        for (int Zone = 0; Zone < SealedZones; Zone++) {
            Open[Zone] = false;
        }
        Populated = 0;
        GuardianDead = false;
        CorePulled = false;
        CollapseStart = -1L;
        setDirty();
    }

    public void endRun() {
        Active = false;
        EmptySince = -1L;
        setDirty();
    }

    public long started() {
        return Started;
    }

    public long emptySince() {
        return EmptySince;
    }

    public void setEmptySince(long Tick) {
        if (EmptySince != Tick) {
            EmptySince = Tick;
            setDirty();
        }
    }

    public boolean powered(int Zone) {
        return Zone < 0 || Zone >= ZoneCount || Power[Zone];
    }

    public void setPower(int Zone, boolean On) {
        if (Zone >= 0 && Zone < ZoneCount && Power[Zone] != On) {
            Power[Zone] = On;
            setDirty();
        }
    }

    public boolean open(int Zone) {
        return Zone >= 0 && Zone < SealedZones && Open[Zone];
    }

    public void setOpen(int Zone) {
        if (Zone >= 0 && Zone < SealedZones && !Open[Zone]) {
            Open[Zone] = true;
            setDirty();
        }
    }

    public boolean populated(int Zone) {
        return (Populated & 1 << Zone) != 0;
    }

    public void markPopulated(int Zone) {
        Populated |= 1 << Zone;
        setDirty();
    }

    public boolean guardianDead() {
        return GuardianDead;
    }

    public void setGuardianDead() {
        GuardianDead = true;
        setDirty();
    }

    public boolean corePulled() {
        return CorePulled;
    }

    public boolean collapsing() {
        return CollapseStart >= 0L;
    }

    public long collapseStart() {
        return CollapseStart;
    }

    public void startCollapse(long Now) {
        CorePulled = true;
        CollapseStart = Now;
        setDirty();
    }

    public long nextBreach(int Zone) {
        return Zone >= 0 && Zone < ZoneCount ? NextBreach[Zone] : Long.MAX_VALUE;
    }

    public void setNextBreach(int Zone, long Tick) {
        if (Zone >= 0 && Zone < ZoneCount) {
            NextBreach[Zone] = Tick;
            setDirty();
        }
    }

    public long wingClose(int Zone) {
        return Zone >= 0 && Zone < ZoneCount ? WingClose[Zone] : 0L;
    }

    public void setWingClose(int Zone, long Tick) {
        if (Zone >= 0 && Zone < ZoneCount && WingClose[Zone] != Tick) {
            WingClose[Zone] = Tick;
            setDirty();
        }
    }

    public boolean jobDone(int Zone, int Index) {
        return Zone >= 0 && Zone < ZoneCount && Index >= 0 && Index < 64 && (Jobs[Zone] & 1L << Index) != 0L;
    }

    public void markJob(int Zone, int Index) {
        if (Zone >= 0 && Zone < ZoneCount && Index >= 0 && Index < 64) {
            Jobs[Zone] |= 1L << Index;
            setDirty();
        }
    }
}
