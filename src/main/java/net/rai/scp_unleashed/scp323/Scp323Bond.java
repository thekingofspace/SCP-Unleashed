package net.rai.scp_unleashed.scp323;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record Scp323Bond(int Stage, UUID Creature, long LastNight, boolean Interrupted, int Kills, boolean FirstDone, boolean Armed) {
    public static final int Horned = 1;
    public static final int Transformed = 2;
    public static final Scp323Bond None = new Scp323Bond(0, Util.NIL_UUID, -1L, false, 0, false, true);
    public static final Codec<Scp323Bond> BondCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.INT.fieldOf("stage").forGetter(Scp323Bond::Stage),
            UUIDUtil.CODEC.optionalFieldOf("creature", Util.NIL_UUID).forGetter(Scp323Bond::Creature),
            Codec.LONG.optionalFieldOf("last_night", -1L).forGetter(Scp323Bond::LastNight),
            Codec.BOOL.optionalFieldOf("interrupted", false).forGetter(Scp323Bond::Interrupted),
            Codec.INT.optionalFieldOf("kills", 0).forGetter(Scp323Bond::Kills),
            Codec.BOOL.optionalFieldOf("first_done", false).forGetter(Scp323Bond::FirstDone),
            Codec.BOOL.optionalFieldOf("armed", false).forGetter(Scp323Bond::Armed)
    ).apply(Instance, Scp323Bond::new));
    public static final String AttachmentsKey = "Attachments";
    public static final String NeoAttachmentsKey = "neoforge:attachments";

    public static Scp323Bond fresh() {
        return new Scp323Bond(Horned, Util.NIL_UUID, -1L, false, 0, false, true);
    }

    public boolean horned() {
        return Stage == Horned;
    }

    public boolean transformed() {
        return Stage == Transformed;
    }

    public Scp323Bond transform(UUID Id, long Night) {
        return new Scp323Bond(Transformed, Id, Night, false, Kills, FirstDone, false);
    }

    public Scp323Bond settle(boolean Done) {
        return new Scp323Bond(Horned, Util.NIL_UUID, LastNight, false, 0, FirstDone || Done, Armed);
    }

    public Scp323Bond interrupt(int KillsSoFar) {
        return new Scp323Bond(Horned, Util.NIL_UUID, LastNight, true, KillsSoFar, FirstDone, Armed);
    }

    public Scp323Bond withKills(int Value) {
        return new Scp323Bond(Stage, Creature, LastNight, Interrupted, Value, FirstDone, Armed);
    }

    public Scp323Bond arm() {
        return new Scp323Bond(Stage, Creature, LastNight, Interrupted, Kills, FirstDone, true);
    }

    public boolean newNight(long Time) {
        return Scp323Night.isNight(Time) && (Armed || Interrupted || Scp323Night.nightId(Time) != LastNight);
    }

    public static Scp323Bond of(@Nullable Entity Target) {
        if (Target == null) {
            return None;
        }
        Scp323Bond Found = Target.getExistingDataOrNull(Scp323Registry.Bond.get());
        return Found == null ? None : Found;
    }

    public static boolean isBound(@Nullable Entity Target) {
        return of(Target).Stage() > 0;
    }

    public static boolean isHorned(@Nullable Entity Target) {
        return of(Target).horned();
    }

    public static boolean isTransformed(@Nullable Entity Target) {
        return of(Target).transformed();
    }

    public static void set(LivingEntity Target, Scp323Bond Value) {
        if (Value.Stage() <= 0) {
            clear(Target);
            return;
        }
        Target.setData(Scp323Registry.Bond.get(), Value);
        Scp323Sync.bond(Target);
    }

    public static boolean clear(LivingEntity Target) {
        if (!Target.hasData(Scp323Registry.Bond.get())) {
            return false;
        }
        Target.removeData(Scp323Registry.Bond.get());
        Scp323Sync.bond(Target);
        return true;
    }

    public static Scp323Bond fromAttachments(CompoundTag Attachments) {
        String Key = Scp323Registry.Bond.getId().toString();
        if (!Attachments.contains(Key)) {
            return None;
        }
        Tag Saved = Attachments.get(Key);
        return BondCodec.parse(NbtOps.INSTANCE, Saved).result().orElse(None);
    }
}
