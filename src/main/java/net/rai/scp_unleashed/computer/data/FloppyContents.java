package net.rai.scp_unleashed.computer.data;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public record FloppyContents(List<ComputerFile> Files) {
    public static final int Capacity = 6;
    public static final FloppyContents Empty = new FloppyContents(List.of());
    public static final Codec<FloppyContents> ContentsCodec = ComputerFile.FileCodec.listOf().xmap(FloppyContents::new, FloppyContents::Files);
    public static final StreamCodec<RegistryFriendlyByteBuf, FloppyContents> ContentsStreamCodec = ComputerFile.FileStreamCodec
            .apply(ByteBufCodecs.list(Capacity)).map(FloppyContents::new, FloppyContents::Files);

    public FloppyContents {
        Files = List.copyOf(Files.size() > Capacity ? Files.subList(0, Capacity) : Files);
    }

    public boolean isFull() {
        return Files.size() >= Capacity;
    }

    public boolean isEmpty() {
        return Files.isEmpty();
    }

    public FloppyContents with(ComputerFile File) {
        List<ComputerFile> Changed = new ArrayList<>(Files);
        Changed.add(File);
        return new FloppyContents(Changed);
    }

    public FloppyContents without(int Index) {
        if (Index < 0 || Index >= Files.size()) {
            return this;
        }
        List<ComputerFile> Changed = new ArrayList<>(Files);
        Changed.remove(Index);
        return new FloppyContents(Changed);
    }

    public FloppyContents replaced(int Index, ComputerFile File) {
        if (Index < 0 || Index >= Files.size()) {
            return this;
        }
        List<ComputerFile> Changed = new ArrayList<>(Files);
        Changed.set(Index, File);
        return new FloppyContents(Changed);
    }
}
