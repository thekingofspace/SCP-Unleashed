package net.rai.scp_unleashed.storage.client;

public enum StorageFit {
    Both,
    SmallOnly,
    TallOnly;

    public boolean fits(boolean Tall) {
        return this == Both || (this == TallOnly) == Tall;
    }
}
