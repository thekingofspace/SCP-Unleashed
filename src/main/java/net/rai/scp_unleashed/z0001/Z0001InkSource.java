package net.rai.scp_unleashed.z0001;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class Z0001InkSource extends DamageSource {
    private final String WriterName;

    public Z0001InkSource(Holder<DamageType> Type, @Nullable Entity Writer, String WriterName) {
        super(Type, null, Writer);
        this.WriterName = WriterName;
    }

    public String getWriterName() {
        return WriterName;
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity Victim) {
        String Key = "death.attack." + type().msgId();
        Entity Writer = getEntity();
        if (Writer != null && Writer != Victim) {
            return Component.translatable(Key + ".player", Victim.getDisplayName(), Writer.getDisplayName());
        }
        if (!WriterName.isEmpty() && !WriterName.equals(Victim.getName().getString())) {
            return Component.translatable(Key + ".player", Victim.getDisplayName(), Component.literal(WriterName));
        }
        return Component.translatable(Key, Victim.getDisplayName());
    }
}
