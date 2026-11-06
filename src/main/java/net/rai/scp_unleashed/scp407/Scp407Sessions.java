package net.rai.scp_unleashed.scp407;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.SpeakerBlock;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp407Sessions extends SavedData {
    public static final String DataName = "scp_unleashed_scp407";
    public static final double JoinRange = 16.0;
    public static final long ActDelay = 500L;
    public static final int ScanTicks = 10;
    public static final int VerifyTicks = 20;
    private final Map<Integer, Scp407Session> Sessions = new LinkedHashMap<>();
    private int NextId = 1;

    public static Scp407Sessions get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Scp407Sessions::new, Scp407Sessions::load), DataName);
    }

    public Collection<Scp407Session> all() {
        return Sessions.values();
    }

    @Nullable
    public Scp407Session byId(int Id) {
        return Sessions.get(Id);
    }

    public boolean isPlaying(int Id) {
        Scp407Session Found = Sessions.get(Id);
        return Found != null && Found.isPlaying();
    }

    @Nullable
    public Scp407Session withSpeaker(BlockPos Pos) {
        for (Scp407Session Session : Sessions.values()) {
            if (Session.Tracks.containsKey(Pos)) {
                return Session;
            }
        }
        return null;
    }

    @Nullable
    public Scp407Session nearest(Vec3 At, double Range) {
        Scp407Session Best = null;
        double BestDistance = Range * Range;
        for (Scp407Session Session : Sessions.values()) {
            double Distance = Vec3.atCenterOf(Session.Origin).distanceToSqr(At);
            if (Distance <= BestDistance) {
                Best = Session;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    public boolean owns(BlockPos Pos) {
        long Key = Pos.asLong();
        for (Scp407Session Session : Sessions.values()) {
            if (Session.Grown.contains(Key)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Scp407Session shielding(UUID Key) {
        for (Scp407Session Session : Sessions.values()) {
            if (!Session.isPlaying()) {
                continue;
            }
            Scp407Session.Listener Entry = Session.Listeners.get(Key);
            if (Entry != null && Entry.Fused && !Entry.Struck) {
                return Session;
            }
        }
        return null;
    }

    @Nullable
    public Scp407Session holding(UUID Key) {
        for (Scp407Session Session : Sessions.values()) {
            Scp407Session.Listener Entry = Session.Listeners.get(Key);
            if (Entry != null && Entry.Fused && !Entry.Struck) {
                return Session;
            }
        }
        return null;
    }

    public void started(ServerLevel World, SpeakerBlockEntity Speaker, SoundMessages.Request Request) {
        BlockPos Pos = Speaker.getBlockPos().immutable();
        long Now = World.getGameTime();
        Scp407Session Existing = withSpeaker(Pos);
        if (Existing != null) {
            Scp407Session.Track Current = Existing.Tracks.get(Pos);
            Current.Loop = Current.Loop || Request.Loop();
            Current.Volume = Request.Volume();
            setDirty();
            return;
        }
        Scp407Session Joined = null;
        for (Scp407Session Session : Sessions.values()) {
            if (Session.isPlaying() && Session.Origin.distSqr(Pos) <= JoinRange * JoinRange) {
                Joined = Session;
                break;
            }
        }
        if (Joined == null) {
            Joined = new Scp407Session(NextId++, Now, Pos);
            Sessions.put(Joined.Id, Joined);
        }
        Joined.Tracks.put(Pos, new Scp407Session.Track(Now, Request.Loop(), Request.Volume()));
        setDirty();
    }

    public void silenced(ServerLevel World, BlockPos Speaker) {
        Scp407Session Session = withSpeaker(Speaker);
        if (Session == null) {
            return;
        }
        Session.Tracks.remove(Speaker);
        if (Session.Tracks.isEmpty() && Session.isPlaying()) {
            Scp407Ending.begin(World, Session);
        }
        setDirty();
    }

    public void forceAct(ServerLevel World, Scp407Session Session, byte Act) {
        long Position = Session.position(World.getGameTime());
        if (Position < ActDelay) {
            Session.StartTick = World.getGameTime() - ActDelay;
            Position = ActDelay;
        }
        Session.Act = Act;
        Session.ActTick = Position;
        if (Act == Scp407Session.ActGenesis && Session.Region.isEmpty()) {
            Scp407Enclosure.Result Found = Scp407Enclosure.check(World, Session.Origin, facing(World, Session.Origin));
            Session.Region.addAll(Found.Cells());
        }
        setDirty();
    }

    public void skip(ServerLevel World, Scp407Session Session, long Position) {
        Session.StartTick = World.getGameTime() - Math.max(0L, Position);
        setDirty();
    }

    public void adopt(Scp407Session Session) {
        Sessions.put(Session.Id, Session);
        NextId = Math.max(NextId, Session.Id + 1);
        setDirty();
    }

    public void remove(Scp407Session Session) {
        Sessions.remove(Session.Id);
        setDirty();
    }

    @Nullable
    public static Direction facing(ServerLevel World, BlockPos Speaker) {
        BlockState State = World.getBlockState(Speaker);
        return State.getBlock() instanceof SpeakerBlock ? State.getValue(SpeakerBlock.Facing) : null;
    }

    public void tick(ServerLevel World) {
        if (Sessions.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        List<Scp407Session> Finished = new ArrayList<>();
        for (Scp407Session Session : List.copyOf(Sessions.values())) {
            if (Session.isPlaying()) {
                playing(World, Session, Now);
            }
            if (!Session.isPlaying() && Scp407Ending.tick(World, Session)) {
                Finished.add(Session);
            }
        }
        for (Scp407Session Session : Finished) {
            Scp407Ending.releaseAll(World, Session);
            Sessions.remove(Session.Id);
        }
        setDirty();
    }

    private void playing(ServerLevel World, Scp407Session Session, long Now) {
        if (!Session.Verified || Now % VerifyTicks == Session.Id % VerifyTicks) {
            verify(World, Session);
            Session.Verified = true;
        }
        if (Session.finished(Now)) {
            Scp407Ending.begin(World, Session);
            return;
        }
        long Position = Session.position(Now);
        boolean Scan = Now % ScanTicks == Session.Id % ScanTicks;
        for (Map.Entry<BlockPos, Scp407Session.Track> Entry : Session.Tracks.entrySet()) {
            Scp407Session.Track Current = Entry.getValue();
            if (!Current.Loop && Position >= Scp407Song.Length) {
                continue;
            }
            BlockPos Pos = Entry.getKey();
            if (!World.isLoaded(Pos) || !(World.getBlockEntity(Pos) instanceof SpeakerBlockEntity Speaker)) {
                continue;
            }
            Vec3 Mouth = Speaker.mouth();
            Scp407Song.play(World, Mouth, Position, Current.Volume);
            if (Scan) {
                Scp407Hearing.scan(World, Session, Mouth, Pos, Position);
            }
        }
        if (Session.Act == Scp407Session.ActPending && Position >= ActDelay && !Session.Listeners.isEmpty()) {
            decide(World, Session, Position);
        }
        if (Session.Act == Scp407Session.ActGenesis) {
            Scp407Growth.tick(World, Session, Position);
            Scp407Fusion.tick(World, Session, Position);
            Scp407Spawning.sprouts(World, Session, Position);
        }
        if (Session.Act != Scp407Session.ActPending) {
            Scp407Spawning.titan(World, Session, Position);
        }
        Scp407Fusion.holdAll(World, Session);
    }

    private void verify(ServerLevel World, Scp407Session Session) {
        Iterator<Map.Entry<BlockPos, Scp407Session.Track>> Entries = Session.Tracks.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<BlockPos, Scp407Session.Track> Entry = Entries.next();
            BlockPos Pos = Entry.getKey();
            if (!World.isLoaded(Pos)) {
                continue;
            }
            if (!(World.getBlockEntity(Pos) instanceof SpeakerBlockEntity Speaker)) {
                Entries.remove();
                continue;
            }
            if (Entry.getValue().Loop) {
                SoundMessages.Request Looping = Speaker.getLooping();
                if (Looping == null || !Looping.Id().equals(Scp407Registry.SongId)) {
                    Entries.remove();
                }
            }
        }
    }

    private void decide(ServerLevel World, Scp407Session Session, long Position) {
        BlockPos Anchor = Session.Origin;
        if (!World.isLoaded(Anchor) || !(World.getBlockEntity(Anchor) instanceof SpeakerBlockEntity)) {
            Anchor = null;
            for (BlockPos Pos : Session.Tracks.keySet()) {
                if (World.isLoaded(Pos) && World.getBlockEntity(Pos) instanceof SpeakerBlockEntity) {
                    Anchor = Pos;
                    break;
                }
            }
        }
        if (Anchor == null) {
            return;
        }
        Scp407Enclosure.Result Found = Scp407Enclosure.check(World, Anchor, facing(World, Anchor));
        Session.Origin = Anchor;
        Session.ActTick = Position;
        if (Found.Enclosed()) {
            Session.Act = Scp407Session.ActGenesis;
            Session.Region.addAll(Found.Cells());
        } else {
            Session.Act = Scp407Session.ActWanderer;
        }
    }

    @Override
    public CompoundTag save(CompoundTag Data, HolderLookup.Provider Lookup) {
        Data.putInt("NextId", NextId);
        ListTag Saved = new ListTag();
        for (Scp407Session Session : Sessions.values()) {
            Saved.add(Session.save());
        }
        Data.put("Sessions", Saved);
        return Data;
    }

    public static Scp407Sessions load(CompoundTag Data, HolderLookup.Provider Lookup) {
        Scp407Sessions Loaded = new Scp407Sessions();
        Loaded.NextId = Math.max(1, Data.getInt("NextId"));
        ListTag Saved = Data.getList("Sessions", CompoundTag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            Scp407Session Session = Scp407Session.load(Saved.getCompound(Index), Lookup.lookupOrThrow(Registries.BLOCK));
            Loaded.Sessions.put(Session.Id, Session);
            Loaded.NextId = Math.max(Loaded.NextId, Session.Id + 1);
        }
        return Loaded;
    }
}
