package net.rai.scp_unleashed.anomaly;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public class AnomalyCure extends Event {
    private final LivingEntity Target;
    private final ServerLevel World;
    @Nullable
    private final Entity Source;
    private boolean Cured;

    public AnomalyCure(LivingEntity Target, ServerLevel World, @Nullable Entity Source) {
        this.Target = Target;
        this.World = World;
        this.Source = Source;
    }

    public LivingEntity target() {
        return Target;
    }

    public ServerLevel world() {
        return World;
    }

    @Nullable
    public Entity source() {
        return Source;
    }

    public boolean cured() {
        return Cured;
    }

    public void markCured() {
        Cured = true;
    }

    public void markCured(boolean Changed) {
        if (Changed) {
            Cured = true;
        }
    }

    public static boolean cure(LivingEntity Target, @Nullable Entity Source) {
        if (!(Target.level() instanceof ServerLevel World) || Target.isRemoved()) {
            return false;
        }
        AnomalyCure Event = new AnomalyCure(Target, World, Source);
        NeoForge.EVENT_BUS.post(Event);
        return Event.cured();
    }
}
