package net.rai.scp_unleashed.archive.document;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record DocumentStamp(ResourceLocation Document, DocumentClass Classification) {
    public static final Codec<DocumentStamp> StampCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            ResourceLocation.CODEC.fieldOf("document").forGetter(DocumentStamp::Document),
            DocumentClass.ClassCodec.fieldOf("classification").forGetter(DocumentStamp::Classification)
    ).apply(Instance, DocumentStamp::new));
    public static final StreamCodec<ByteBuf, DocumentStamp> StampStreamCodec = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, DocumentStamp::Document,
            DocumentClass.ClassStreamCodec, DocumentStamp::Classification,
            DocumentStamp::new);
}
