package net.rai.scp_unleashed.scp105;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

public record Scp105Photo(Kind Subject, ResourceLocation Id, int Count, boolean Young, int Scene, int Look, long Started, long Due, boolean Told) {
    public static final int Looks = 4;
    public static final Codec<Scp105Photo> PhotoCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Kind.KindCodec.fieldOf("kind").forGetter(Scp105Photo::Subject),
            ResourceLocation.CODEC.fieldOf("subject").forGetter(Scp105Photo::Id),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Scp105Photo::Count),
            Codec.BOOL.optionalFieldOf("young", false).forGetter(Scp105Photo::Young),
            Codec.intRange(0, 255).optionalFieldOf("scene", 0).forGetter(Scp105Photo::Scene),
            Codec.intRange(0, Looks - 1).optionalFieldOf("look", 0).forGetter(Scp105Photo::Look),
            Codec.LONG.optionalFieldOf("started", 0L).forGetter(Scp105Photo::Started),
            Codec.LONG.optionalFieldOf("due", 0L).forGetter(Scp105Photo::Due),
            Codec.BOOL.optionalFieldOf("told", false).forGetter(Scp105Photo::Told)
    ).apply(Instance, Scp105Photo::new));

    public boolean developing() {
        return Due > 0L;
    }

    public Scp105Photo begin(long Now, long When) {
        return new Scp105Photo(Subject, Id, Count, Young, Scene, Look, Now, When, false);
    }

    public Scp105Photo told() {
        return new Scp105Photo(Subject, Id, Count, Young, Scene, Look, Started, Due, true);
    }

    public Scp105Photo delay(long When) {
        return new Scp105Photo(Subject, Id, Count, Young, Scene, Look, Started, When, Told);
    }

    public Scp105Photo retarget(Kind NewKind, ResourceLocation NewId, int NewCount, boolean NewYoung) {
        return new Scp105Photo(NewKind, NewId, NewCount, NewYoung, Scene, Look, Started, Due, Told);
    }

    public enum Kind implements StringRepresentable {
        Thing("item"),
        Creature("creature");

        public static final Codec<Kind> KindCodec = StringRepresentable.fromEnum(Kind::values);

        private final String SerializedName;

        Kind(String SerializedName) {
            this.SerializedName = SerializedName;
        }

        @Override
        public String getSerializedName() {
            return SerializedName;
        }
    }
}
