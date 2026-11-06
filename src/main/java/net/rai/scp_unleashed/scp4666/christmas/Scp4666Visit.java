package net.rai.scp_unleashed.scp4666.christmas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

import java.util.ArrayList;
import java.util.List;

public final class Scp4666Visit {
    public static final int DoorMemory = 8;
    public static final Codec<Scp4666Visit> CODEC = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.BOOL.optionalFieldOf("survived", false).forGetter(Entry -> Entry.Survived),
            Codec.LONG.optionalFieldOf("note_at", 0L).forGetter(Entry -> Entry.NoteAt),
            Codec.BOOL.optionalFieldOf("note_done", false).forGetter(Entry -> Entry.NoteDone),
            Codec.LONG.listOf().optionalFieldOf("doors", List.of()).forGetter(Entry -> List.copyOf(Entry.Doors)),
            Codec.BOOL.optionalFieldOf("read", false).forGetter(Entry -> Entry.Read),
            Codec.LONG.optionalFieldOf("sighting_day", -1L).forGetter(Entry -> Entry.SightingDay),
            Codec.LONG.optionalFieldOf("sighting_tick", -1L).forGetter(Entry -> Entry.SightingTick)
    ).apply(Instance, Scp4666Visit::new));

    public boolean Survived;
    public long NoteAt;
    public boolean NoteDone;
    public final List<Long> Doors = new ArrayList<>();
    public boolean Read;
    public long SightingDay = -1L;
    public long SightingTick = -1L;

    public Scp4666Visit() {
    }

    public Scp4666Visit(boolean Survived, long NoteAt, boolean NoteDone, List<Long> Doors, boolean Read, long SightingDay, long SightingTick) {
        this.Survived = Survived;
        this.NoteAt = NoteAt;
        this.NoteDone = NoteDone;
        this.Doors.addAll(Doors);
        this.Read = Read;
        this.SightingDay = SightingDay;
        this.SightingTick = SightingTick;
    }

    public static Scp4666Visit of(Player User) {
        return User.getData(Scp4666Registry.Visit);
    }

    public void rememberDoor(BlockPos Pos) {
        Long Key = Pos.asLong();
        Doors.remove(Key);
        Doors.add(Key);
        while (Doors.size() > DoorMemory) {
            Doors.remove(0);
        }
    }

    public boolean pending() {
        return NoteAt > 0L && !NoteDone;
    }
}
