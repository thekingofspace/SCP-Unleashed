package net.rai.scp_unleashed.storage.client;

@FunctionalInterface
public interface StorageLayout {
    void render(StorageLayoutContext Context);

    default boolean fits(boolean Tall) {
        return true;
    }

    static StorageLayout sized(StorageLayout Small, StorageLayout Tall) {
        return Context -> (Context.Tall() ? Tall : Small).render(Context);
    }

    static StorageLayout only(StorageFit Fit, StorageLayout Layout) {
        return new StorageLayout() {
            @Override
            public void render(StorageLayoutContext Context) {
                Layout.render(Context);
            }

            @Override
            public boolean fits(boolean Tall) {
                return Fit.fits(Tall) && Layout.fits(Tall);
            }
        };
    }
}
