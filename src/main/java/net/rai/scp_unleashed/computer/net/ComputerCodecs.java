package net.rai.scp_unleashed.computer.net;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ComputerCodecs {
    private ComputerCodecs() {
    }

    @Nullable
    public static <T> Tag encode(Codec<T> Codec, T Value, HolderLookup.Provider Registries) {
        return Codec.encodeStart(Registries.createSerializationContext(NbtOps.INSTANCE), Value).result().orElse(null);
    }

    public static <T> Optional<T> decode(Codec<T> Codec, @Nullable Tag Value, HolderLookup.Provider Registries) {
        if (Value == null) {
            return Optional.empty();
        }
        return Codec.parse(Registries.createSerializationContext(NbtOps.INSTANCE), Value).result();
    }

    public static <T> ListTag encodeList(Codec<T> Codec, List<T> Values, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (T Value : Values) {
            Tag Encoded = encode(Codec, Value, Registries);
            if (Encoded != null) {
                Saved.add(Encoded);
            }
        }
        return Saved;
    }

    public static <T> List<T> decodeList(Codec<T> Codec, CompoundTag Holder, String Key, HolderLookup.Provider Registries) {
        List<T> Found = new ArrayList<>();
        if (!Holder.contains(Key, Tag.TAG_LIST)) {
            return Found;
        }
        ListTag Saved = Holder.getList(Key, Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            decode(Codec, Saved.get(Index), Registries).ifPresent(Found::add);
        }
        return Found;
    }
}
