package net.rai.scp_unleashed.cameras;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record CameraLinks(List<Link> Entries) {
    public static final int MaxLinks = 16;
    public static final int NameLimit = 24;
    public static final CameraLinks Empty = new CameraLinks(List.of());

    public record Link(GlobalPos Where, String Name) {
        public static final Codec<Link> LinkCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(Link::Where),
                Codec.STRING.optionalFieldOf("name", "").forGetter(Link::Name)
        ).apply(Instance, Link::new));
        public static final StreamCodec<ByteBuf, Link> LinkStreamCodec = StreamCodec.composite(
                GlobalPos.STREAM_CODEC, Link::Where,
                ByteBufCodecs.stringUtf8(64), Link::Name,
                Link::new);

        public BlockPos pos() {
            return Where.pos();
        }

        public ResourceKey<Level> dimension() {
            return Where.dimension();
        }

        public Link named(String NewName) {
            return new Link(Where, NewName);
        }
    }

    public static final Codec<CameraLinks> LinksCodec = Link.LinkCodec.listOf().xmap(CameraLinks::new, CameraLinks::Entries);
    public static final StreamCodec<ByteBuf, CameraLinks> LinksStreamCodec = Link.LinkStreamCodec.apply(ByteBufCodecs.list(MaxLinks * 2))
            .map(CameraLinks::new, CameraLinks::Entries);

    public CameraLinks {
        Entries = List.copyOf(Entries);
    }

    public static CameraLinks of(ItemStack Stack) {
        CameraLinks Found = Stack.get(CameraRegistry.Links.get());
        return Found == null ? Empty : Found;
    }

    public static void store(ItemStack Stack, CameraLinks Links) {
        if (Links.Entries.isEmpty()) {
            Stack.remove(CameraRegistry.Links.get());
        } else {
            Stack.set(CameraRegistry.Links.get(), Links);
        }
    }

    public int size() {
        return Entries.size();
    }

    public boolean isEmpty() {
        return Entries.isEmpty();
    }

    @Nullable
    public Link get(int Index) {
        return Index >= 0 && Index < Entries.size() ? Entries.get(Index) : null;
    }

    public int indexOf(GlobalPos Where) {
        for (int Index = 0; Index < Entries.size(); Index++) {
            if (Entries.get(Index).Where().equals(Where)) {
                return Index;
            }
        }
        return -1;
    }

    public boolean isFull() {
        return Entries.size() >= MaxLinks;
    }

    public CameraLinks with(Link Added) {
        List<Link> Next = new ArrayList<>(Entries);
        Next.add(Added);
        return new CameraLinks(Next);
    }

    public CameraLinks without(int Index) {
        if (Index < 0 || Index >= Entries.size()) {
            return this;
        }
        List<Link> Next = new ArrayList<>(Entries);
        Next.remove(Index);
        return new CameraLinks(Next);
    }

    public CameraLinks replaced(int Index, Link Changed) {
        if (Index < 0 || Index >= Entries.size()) {
            return this;
        }
        List<Link> Next = new ArrayList<>(Entries);
        Next.set(Index, Changed);
        return new CameraLinks(Next);
    }

    public static String clean(String Name) {
        String Trimmed = Name == null ? "" : Name.replaceAll("[\\p{Cntrl}]", "").trim();
        return Trimmed.length() > NameLimit ? Trimmed.substring(0, NameLimit).trim() : Trimmed;
    }

    public static String fallbackName(int Number) {
        return String.format(Locale.ROOT, "CAM-%02d", Number);
    }
}
