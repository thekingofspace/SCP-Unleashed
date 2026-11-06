package net.rai.scp_unleashed.scp3008.plan;

public interface Scp3008Canvas {
    int minX();

    int minZ();

    int maxX();

    int maxZ();

    void put(int X, int Y, int Z, int Code);

    default boolean covers(int X, int Z) {
        return X >= minX() && X <= maxX() && Z >= minZ() && Z <= maxZ();
    }

    default boolean overlaps(int LowX, int LowZ, int HighX, int HighZ) {
        return HighX >= minX() && LowX <= maxX() && HighZ >= minZ() && LowZ <= maxZ();
    }

    default void set(int X, int Y, int Z, int Code) {
        if (covers(X, Z)) {
            put(X, Y, Z, Code);
        }
    }

    default void fill(int LowX, int LowY, int LowZ, int HighX, int HighY, int HighZ, int Code) {
        int FromX = Math.max(Math.min(LowX, HighX), minX());
        int ToX = Math.min(Math.max(LowX, HighX), maxX());
        int FromZ = Math.max(Math.min(LowZ, HighZ), minZ());
        int ToZ = Math.min(Math.max(LowZ, HighZ), maxZ());
        int FromY = Math.min(LowY, HighY);
        int ToY = Math.max(LowY, HighY);
        for (int X = FromX; X <= ToX; X++) {
            for (int Z = FromZ; Z <= ToZ; Z++) {
                for (int Y = FromY; Y <= ToY; Y++) {
                    put(X, Y, Z, Code);
                }
            }
        }
    }
}
