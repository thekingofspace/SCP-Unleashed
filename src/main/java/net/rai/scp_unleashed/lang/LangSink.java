package net.rai.scp_unleashed.lang;

import java.util.AbstractMap;
import java.util.Set;
import java.util.function.BiConsumer;

public final class LangSink extends AbstractMap<String, String> {
    private final BiConsumer<String, String> Output;

    public LangSink(BiConsumer<String, String> Output) {
        this.Output = Output;
    }

    @Override
    public String put(String Key, String Value) {
        Output.accept(Key, Value);
        return null;
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return Set.of();
    }
}
