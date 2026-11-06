package net.rai.scp_unleashed.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Rotation;

public record FaceLayer(Kind LayerKind, ResourceLocation Id, Rotation Spin, boolean Translucent) {
    public static final Codec<FaceLayer> LayerCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Kind.KindCodec.fieldOf("kind").forGetter(FaceLayer::LayerKind),
            ResourceLocation.CODEC.fieldOf("id").forGetter(FaceLayer::Id),
            Rotation.CODEC.optionalFieldOf("spin", Rotation.NONE).forGetter(FaceLayer::Spin),
            Codec.BOOL.optionalFieldOf("translucent", false).forGetter(FaceLayer::Translucent)
    ).apply(Instance, FaceLayer::new));

    public static FaceLayer texture(ResourceLocation Texture) {
        return new FaceLayer(Kind.Texture, Texture, Rotation.NONE, false);
    }

    public static FaceLayer model(ResourceLocation Model) {
        return new FaceLayer(Kind.Model, Model, Rotation.NONE, false);
    }

    public FaceLayer withSpin(Rotation NewSpin) {
        return new FaceLayer(LayerKind, Id, NewSpin, Translucent);
    }

    public FaceLayer withTranslucent(boolean NewTranslucent) {
        return new FaceLayer(LayerKind, Id, Spin, NewTranslucent);
    }

    public int spinDegrees() {
        return switch (Spin) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 90;
            case CLOCKWISE_180 -> 180;
            case COUNTERCLOCKWISE_90 -> 270;
        };
    }

    public enum Kind implements StringRepresentable {
        Texture("texture"),
        Model("model");

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
