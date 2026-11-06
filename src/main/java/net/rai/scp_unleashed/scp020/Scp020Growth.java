package net.rai.scp_unleashed.scp020;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

public final class Scp020Growth {
    public static final int AllFaces = 63;
    public static final String HostsKey = "Hosts";
    public static final String FacesKey = "Faces";

    public static final IAttachmentSerializer<CompoundTag, Scp020Growth> Saver = new IAttachmentSerializer<>() {
        @Override
        public Scp020Growth read(IAttachmentHolder Holder, CompoundTag Saved, HolderLookup.Provider Registries) {
            Scp020Growth Loaded = new Scp020Growth();
            long[] Hosts = Saved.getLongArray(HostsKey);
            byte[] Faces = Saved.getByteArray(FacesKey);
            for (int Index = 0; Index < Math.min(Hosts.length, Faces.length); Index++) {
                Loaded.put(Hosts[Index], Faces[Index]);
            }
            return Loaded;
        }

        @Nullable
        @Override
        public CompoundTag write(Scp020Growth Colony, HolderLookup.Provider Registries) {
            if (Colony.isEmpty()) {
                return null;
            }
            CompoundTag Saved = new CompoundTag();
            long[] Hosts = new long[Colony.Faces.size()];
            byte[] Faces = new byte[Colony.Faces.size()];
            int Index = 0;
            for (Long2ByteMap.Entry Entry : Colony.Faces.long2ByteEntrySet()) {
                Hosts[Index] = Entry.getLongKey();
                Faces[Index] = Entry.getByteValue();
                Index++;
            }
            Saved.put(HostsKey, new LongArrayTag(Hosts));
            Saved.put(FacesKey, new ByteArrayTag(Faces));
            return Saved;
        }
    };

    private final Long2ByteOpenHashMap Faces = new Long2ByteOpenHashMap();

    public Scp020Growth() {
    }

    public static int bit(Direction Side) {
        return 1 << Side.get3DDataValue();
    }

    public static boolean hasFace(int Mask, Direction Side) {
        return (Mask & bit(Side)) != 0;
    }

    private void put(long Host, int Mask) {
        int Clean = Mask & AllFaces;
        if (Clean != 0) {
            Faces.put(Host, (byte) Clean);
        }
    }

    public boolean isEmpty() {
        return Faces.isEmpty();
    }

    public int hosts() {
        return Faces.size();
    }

    public int faceCount() {
        int Count = 0;
        for (byte Mask : Faces.values()) {
            Count += Integer.bitCount(Mask & AllFaces);
        }
        return Count;
    }

    public int facesAt(long Host) {
        return Faces.get(Host) & AllFaces;
    }

    public boolean has(long Host, Direction Side) {
        return hasFace(facesAt(Host), Side);
    }

    public boolean add(long Host, Direction Side) {
        int Before = facesAt(Host);
        int After = Before | bit(Side);
        if (After == Before) {
            return false;
        }
        Faces.put(Host, (byte) After);
        return true;
    }

    public boolean remove(long Host, Direction Side) {
        int Before = facesAt(Host);
        int After = Before & ~bit(Side);
        if (After == Before) {
            return false;
        }
        if (After == 0) {
            Faces.remove(Host);
        } else {
            Faces.put(Host, (byte) After);
        }
        return true;
    }

    public int removeAll(long Host) {
        if (!Faces.containsKey(Host)) {
            return 0;
        }
        return Integer.bitCount(Faces.remove(Host) & AllFaces);
    }

    public long[] hostArray() {
        return Faces.keySet().toLongArray();
    }

    public void forEach(FaceVisitor Visitor) {
        for (Long2ByteMap.Entry Entry : Faces.long2ByteEntrySet()) {
            Visitor.visit(Entry.getLongKey(), Entry.getByteValue() & AllFaces);
        }
    }

    public interface FaceVisitor {
        void visit(long Host, int Mask);
    }
}
