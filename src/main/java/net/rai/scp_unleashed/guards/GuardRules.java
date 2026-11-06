package net.rai.scp_unleashed.guards;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.rai.scp_unleashed.security.TurretCategory;
import net.rai.scp_unleashed.security.TurretRules;

import java.util.ArrayList;
import java.util.List;

public record GuardRules(int Mask, List<Entry> Entries, boolean HoldFire, boolean Melee) {
    public static final GuardRules Default = new GuardRules(TurretCategory.DefaultMask, List.of(), false, true);
    public static final Codec<Entry> EntryCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.STRING.fieldOf("Key").forGetter(Entry::Key),
            Codec.BOOL.fieldOf("Deny").forGetter(Entry::Deny)
    ).apply(Instance, Entry::new));
    public static final StreamCodec<ByteBuf, Entry> EntryStreamCodec = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(TurretRules.MaxLength), Entry::Key,
            ByteBufCodecs.BOOL, Entry::Deny,
            Entry::new);
    public static final Codec<GuardRules> RulesCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.INT.optionalFieldOf("Mask", TurretCategory.DefaultMask).forGetter(GuardRules::Mask),
            EntryCodec.listOf().optionalFieldOf("Entries", List.of()).forGetter(GuardRules::Entries),
            Codec.BOOL.optionalFieldOf("HoldFire", false).forGetter(GuardRules::HoldFire),
            Codec.BOOL.optionalFieldOf("Melee", true).forGetter(GuardRules::Melee)
    ).apply(Instance, GuardRules::new));
    public static final StreamCodec<ByteBuf, GuardRules> RulesStreamCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GuardRules::Mask,
            EntryStreamCodec.apply(ByteBufCodecs.list(TurretRules.MaxEntries)), GuardRules::Entries,
            ByteBufCodecs.BOOL, GuardRules::HoldFire,
            ByteBufCodecs.BOOL, GuardRules::Melee,
            GuardRules::new);

    public GuardRules {
        Entries = List.copyOf(Entries.size() > TurretRules.MaxEntries ? Entries.subList(0, TurretRules.MaxEntries) : Entries);
    }

    public static GuardRules of(TurretRules Rules, boolean HoldFire, boolean Melee) {
        List<Entry> Copied = new ArrayList<>();
        for (TurretRules.Entry Item : Rules.getEntries()) {
            Copied.add(new Entry(Item.Key(), Item.Deny()));
        }
        return new GuardRules(Rules.getMask(), Copied, HoldFire, Melee);
    }

    public TurretRules toTurret() {
        TurretRules Rules = new TurretRules();
        for (TurretCategory Category : TurretCategory.values()) {
            Rules.set(Category, has(Category));
        }
        for (Entry Item : Entries) {
            Rules.add(Item.Key(), Item.Deny());
        }
        return Rules;
    }

    public boolean has(TurretCategory Category) {
        return (Mask & Category.bit()) != 0;
    }

    public record Entry(String Key, boolean Deny) {
    }
}
