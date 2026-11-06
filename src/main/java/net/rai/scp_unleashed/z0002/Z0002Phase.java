package net.rai.scp_unleashed.z0002;

public enum Z0002Phase {
    Building("building"),
    Active("active"),
    Collapsing("collapsing");

    private final String Key;

    Z0002Phase(String Key) {
        this.Key = Key;
    }

    public String key() {
        return Key;
    }

    public static Z0002Phase byKey(String Wanted) {
        for (Z0002Phase Each : values()) {
            if (Each.Key.equals(Wanted)) {
                return Each;
            }
        }
        return Active;
    }
}
