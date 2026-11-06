package net.rai.scp_unleashed.scp3008.furniture.electronics;

import net.rai.scp_unleashed.scp3008.plan.Scp3008Hash;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Scp3008ElectronicsLedger {
    public static final double Share = 1.0 / 12.0;
    public static final int Salt = 53;
    public static final int Margin = 16;
    public static final int RegionSide = 32;
    public static final int HeaderEntries = RegionSide * RegionSide;
    private static volatile Set<Long> Frozen = Set.of();
    private static volatile boolean Everything;
    private static volatile int Epoch;

    private Scp3008ElectronicsLedger() {
    }

    public static long key(int Sx, int Sz) {
        return (long) Sx << 32 | Sz & 0xFFFFFFFFL;
    }

    public static int highOf(long Key) {
        return (int) (Key >> 32);
    }

    public static int lowOf(long Key) {
        return (int) Key;
    }

    public static long chunk(int Cx, int Cz) {
        return (long) Cx & 0xFFFFFFFFL | ((long) Cz & 0xFFFFFFFFL) << 32;
    }

    public static boolean rolls(int Sx, int Sz) {
        return Scp3008Hash.chance(Scp3008Hash.mix(Sx, Sz, Salt), Share);
    }

    public static boolean frozen(int Sx, int Sz) {
        return Everything || Frozen.contains(key(Sx, Sz));
    }

    public static int claim(int Sx, int Sz, int Classic) {
        return rolls(Sx, Sz) && !frozen(Sx, Sz) ? Scp3008Plan.Electronics : Classic;
    }

    public static int departmentWith(int Ux, int Uz, Set<Long> FrozenSites) {
        long Site = Scp3008Plan.site(Ux, Uz);
        int Sx = highOf(Site);
        int Sz = lowOf(Site);
        int Classic = Scp3008Plan.classic(Sx, Sz);
        return rolls(Sx, Sz) && !FrozenSites.contains(Site) ? Scp3008Plan.Electronics : Classic;
    }

    public static int epoch() {
        return Epoch;
    }

    public static Set<Long> frozenSites() {
        return Frozen;
    }

    public static synchronized void install(Collection<Long> Sites) {
        Frozen = Set.copyOf(Sites);
        Everything = false;
        Epoch = Epoch + 1 & 0x3FFFFFFF;
    }

    public static synchronized void freezeAll() {
        Frozen = Set.of();
        Everything = true;
        Epoch = Epoch + 1 & 0x3FFFFFFF;
    }

    public static boolean everythingFrozen() {
        return Everything;
    }

    public static void reset() {
        install(List.of());
    }

    public static Set<Long> sitesTouching(Collection<Long> Chunks) {
        Set<Long> Cells = new HashSet<>();
        for (long Chunk : Chunks) {
            int Cx = (int) Chunk;
            int Cz = (int) (Chunk >>> 32);
            int MinX = Cx * 16 - Margin;
            int MinZ = Cz * 16 - Margin;
            int MaxX = Cx * 16 + 15 + Margin;
            int MaxZ = Cz * 16 + 15 + Margin;
            for (int Ux = Scp3008Plan.unitX(MinX); Ux <= Scp3008Plan.unitX(MaxX); Ux++) {
                for (int Uz = Scp3008Plan.unitZ(MinZ); Uz <= Scp3008Plan.unitZ(MaxZ); Uz++) {
                    Cells.add(key(Ux, Uz));
                }
            }
        }
        Set<Long> Sites = new HashSet<>();
        for (long Cell : Cells) {
            Sites.add(Scp3008Plan.site(highOf(Cell), lowOf(Cell)));
        }
        return Sites;
    }

    public static List<Long> regionChunks(Path File) throws IOException {
        List<Long> Found = new ArrayList<>();
        String[] Parts = File.getFileName().toString().split("\\.");
        if (Parts.length != 4 || !Parts[0].equals("r") || !Parts[3].equals("mca")) {
            return Found;
        }
        int Rx;
        int Rz;
        try {
            Rx = Integer.parseInt(Parts[1]);
            Rz = Integer.parseInt(Parts[2]);
        } catch (NumberFormatException Problem) {
            return Found;
        }
        if (Files.size(File) < HeaderEntries * 4L) {
            return Found;
        }
        try (InputStream Raw = Files.newInputStream(File); DataInputStream Header = new DataInputStream(Raw)) {
            for (int Index = 0; Index < HeaderEntries; Index++) {
                if (Header.readInt() != 0) {
                    Found.add(chunk(Rx * RegionSide + (Index & 31), Rz * RegionSide + (Index >> 5)));
                }
            }
        }
        return Found;
    }

    public static List<Long> folderChunks(Path Folder) throws IOException {
        List<Long> Found = new ArrayList<>();
        if (!Files.isDirectory(Folder)) {
            return Found;
        }
        try (DirectoryStream<Path> Files0 = Files.newDirectoryStream(Folder, "r.*.mca")) {
            for (Path File : Files0) {
                Found.addAll(regionChunks(File));
            }
        }
        return Found;
    }
}
