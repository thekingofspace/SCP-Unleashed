package net.rai.scp_unleashed.voice;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class VoiceFiles {
    public static final int ClipMagic = 0x53435643;
    public static final int IndexMagic = 0x53435649;
    public static final int OptOutMagic = 0x5343564F;
    public static final String OptOutName = "voice_optout.dat";
    public static final int Version = 1;
    public static final String IndexName = "index.dat";
    public static final String ClipSuffix = ".clip";
    public static final int MaxFrames = 4096;
    public static final int MaxFrameBytes = 4000;

    private VoiceFiles() {
    }

    public record Loaded(VoiceClip Meta, List<byte[]> Frames) {
    }

    public static Path clipPath(Path Folder, UUID Id) {
        return Folder.resolve(Id + ClipSuffix);
    }

    private static void writeMeta(DataOutputStream Out, VoiceClip Meta) throws IOException {
        Out.writeLong(Meta.Id().getMostSignificantBits());
        Out.writeLong(Meta.Id().getLeastSignificantBits());
        Out.writeLong(Meta.Speaker().getMostSignificantBits());
        Out.writeLong(Meta.Speaker().getLeastSignificantBits());
        Out.writeUTF(Meta.SpeakerName());
        Out.writeLong(Meta.Recorded());
        Out.writeBoolean(Meta.Listener() != null);
        if (Meta.Listener() != null) {
            Out.writeLong(Meta.Listener().getMostSignificantBits());
            Out.writeLong(Meta.Listener().getLeastSignificantBits());
        }
        Out.writeInt(Meta.Frames());
        Out.writeInt(Meta.Bytes());
    }

    private static VoiceClip readMeta(DataInputStream In, String Anomaly) throws IOException {
        UUID Id = new UUID(In.readLong(), In.readLong());
        UUID Speaker = new UUID(In.readLong(), In.readLong());
        String Name = In.readUTF();
        long Recorded = In.readLong();
        UUID Listener = In.readBoolean() ? new UUID(In.readLong(), In.readLong()) : null;
        int Frames = In.readInt();
        int Bytes = In.readInt();
        if (Frames < 0 || Frames > MaxFrames || Bytes < 0) {
            throw new IOException("Damaged voice clip header");
        }
        return new VoiceClip(Id, Anomaly, Speaker, Name, Recorded, Listener, Frames, Bytes);
    }

    private static void replace(Path Temporary, Path Target) throws IOException {
        try {
            Files.move(Temporary, Target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException Problem) {
            Files.move(Temporary, Target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void writeClip(Path Folder, VoiceClip Meta, List<byte[]> Frames) throws IOException {
        Files.createDirectories(Folder);
        Path Target = clipPath(Folder, Meta.Id());
        Path Temporary = Folder.resolve(Meta.Id() + ".tmp");
        try (DataOutputStream Out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(Temporary))))) {
            Out.writeInt(ClipMagic);
            Out.writeInt(Version);
            writeMeta(Out, Meta);
            Out.writeInt(Frames.size());
            for (byte[] Frame : Frames) {
                Out.writeShort(Frame.length);
                Out.write(Frame);
            }
        }
        replace(Temporary, Target);
    }

    public static Loaded readClip(Path File, String Anomaly) throws IOException {
        try (DataInputStream In = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(File))))) {
            if (In.readInt() != ClipMagic || In.readInt() != Version) {
                throw new IOException("Not a voice clip: " + File);
            }
            VoiceClip Meta = readMeta(In, Anomaly);
            int Count = In.readInt();
            if (Count < 0 || Count > MaxFrames) {
                throw new IOException("Damaged voice clip: " + File);
            }
            List<byte[]> Frames = new ArrayList<>(Count);
            for (int Index = 0; Index < Count; Index++) {
                int Length = In.readUnsignedShort();
                if (Length > MaxFrameBytes) {
                    throw new IOException("Damaged voice frame: " + File);
                }
                byte[] Frame = new byte[Length];
                In.readFully(Frame);
                Frames.add(Frame);
            }
            return new Loaded(Meta, Frames);
        }
    }

    public static void writeIndex(Path Folder, Collection<VoiceClip> Clips) throws IOException {
        Files.createDirectories(Folder);
        Path Temporary = Folder.resolve(IndexName + ".tmp");
        try (DataOutputStream Out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(Temporary))))) {
            Out.writeInt(IndexMagic);
            Out.writeInt(Version);
            Out.writeInt(Clips.size());
            for (VoiceClip Each : Clips) {
                writeMeta(Out, Each);
            }
        }
        replace(Temporary, Folder.resolve(IndexName));
    }

    @Nullable
    public static List<VoiceClip> readIndex(Path Folder, String Anomaly) {
        Path File = Folder.resolve(IndexName);
        if (!Files.isRegularFile(File)) {
            return null;
        }
        try (DataInputStream In = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(File))))) {
            if (In.readInt() != IndexMagic || In.readInt() != Version) {
                return null;
            }
            int Count = In.readInt();
            if (Count < 0 || Count > 100_000) {
                return null;
            }
            List<VoiceClip> Clips = new ArrayList<>(Count);
            for (int Index = 0; Index < Count; Index++) {
                Clips.add(readMeta(In, Anomaly));
            }
            return Clips;
        } catch (IOException Problem) {
            return null;
        }
    }

    public static List<VoiceClip> rebuild(Path Folder, String Anomaly) {
        List<VoiceClip> Clips = new ArrayList<>();
        if (!Files.isDirectory(Folder)) {
            return Clips;
        }
        try (Stream<Path> Listing = Files.list(Folder)) {
            for (Path Each : (Iterable<Path>) Listing.filter(File -> File.getFileName().toString().endsWith(ClipSuffix))::iterator) {
                try {
                    Clips.add(readClip(Each, Anomaly).Meta());
                } catch (IOException Problem) {
                    Files.deleteIfExists(Each);
                }
            }
        } catch (IOException Problem) {
            return Clips;
        }
        Clips.sort(Comparator.comparingLong(VoiceClip::Recorded));
        return Clips;
    }

    public static void writeOptOuts(Path File, Collection<UUID> Speakers) throws IOException {
        Files.createDirectories(File.getParent());
        Path Temporary = File.resolveSibling(OptOutName + ".tmp");
        try (DataOutputStream Out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(Temporary))))) {
            Out.writeInt(OptOutMagic);
            Out.writeInt(Version);
            Out.writeInt(Speakers.size());
            for (UUID Each : Speakers) {
                Out.writeLong(Each.getMostSignificantBits());
                Out.writeLong(Each.getLeastSignificantBits());
            }
        }
        replace(Temporary, File);
    }

    public static List<UUID> readOptOuts(Path File) {
        List<UUID> Found = new ArrayList<>();
        if (!Files.isRegularFile(File)) {
            return Found;
        }
        try (DataInputStream In = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(File))))) {
            if (In.readInt() != OptOutMagic || In.readInt() != Version) {
                return Found;
            }
            int Count = In.readInt();
            for (int Index = 0; Index < Count && Index < 1_000_000; Index++) {
                Found.add(new UUID(In.readLong(), In.readLong()));
            }
        } catch (IOException Problem) {
            return Found;
        }
        return Found;
    }

    public static long folderBytes(Path Folder) {
        if (!Files.isDirectory(Folder)) {
            return 0L;
        }
        try (Stream<Path> Walk = Files.walk(Folder)) {
            return Walk.filter(Files::isRegularFile).mapToLong(File -> {
                try {
                    return Files.size(File);
                } catch (IOException Problem) {
                    return 0L;
                }
            }).sum();
        } catch (IOException Problem) {
            return 0L;
        }
    }

    public static void deleteTree(Path Folder) throws IOException {
        if (!Files.exists(Folder)) {
            return;
        }
        try (Stream<Path> Walk = Files.walk(Folder)) {
            for (Path Each : Walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(Each);
            }
        }
    }

    public static List<String> anomalies(Path Root) {
        List<String> Found = new ArrayList<>();
        if (!Files.isDirectory(Root)) {
            return Found;
        }
        try (Stream<Path> Listing = Files.list(Root)) {
            Listing.filter(Files::isDirectory).map(Each -> Each.getFileName().toString()).filter(VoicePolicy::validId).sorted().forEach(Found::add);
        } catch (IOException Problem) {
            return Found;
        }
        return Found;
    }
}
