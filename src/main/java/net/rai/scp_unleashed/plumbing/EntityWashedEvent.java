package net.rai.scp_unleashed.plumbing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.fluids.FluidStack;

public class EntityWashedEvent extends EntityEvent implements ICancellableEvent {
    private final ServerLevel World;
    private final BlockPos Source;
    private final FluidStack Liquid;
    private final int Strength;
    private boolean Extinguish = true;
    private boolean Rinse = true;

    public EntityWashedEvent(Entity Target, ServerLevel World, BlockPos Source, FluidStack Liquid, int Strength) {
        super(Target);
        this.World = World;
        this.Source = Source.immutable();
        this.Liquid = Liquid.copy();
        this.Strength = Strength;
    }

    public static boolean wash(Entity Target, ServerLevel World, BlockPos Source, FluidStack Liquid, int Strength) {
        EntityWashedEvent Event = NeoForge.EVENT_BUS.post(new EntityWashedEvent(Target, World, Source, Liquid, Strength));
        if (Event.isCanceled()) {
            return false;
        }
        Washing.applyDefaults(Event);
        return true;
    }

    public ServerLevel getLevel() {
        return World;
    }

    public BlockPos getSource() {
        return Source;
    }

    public FluidStack getLiquid() {
        return Liquid;
    }

    public int getStrength() {
        return Strength;
    }

    public boolean shouldExtinguish() {
        return Extinguish;
    }

    public void setExtinguish(boolean Extinguish) {
        this.Extinguish = Extinguish;
    }

    public boolean shouldRinse() {
        return Rinse;
    }

    public void setRinse(boolean Rinse) {
        this.Rinse = Rinse;
    }
}
