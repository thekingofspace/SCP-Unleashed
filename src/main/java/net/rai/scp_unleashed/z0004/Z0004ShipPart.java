package net.rai.scp_unleashed.z0004;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

public class Z0004ShipPart extends PartEntity<Z0004Ship> {
    public final Z0004Ship Ship;
    public final String Name;
    public final double Along;
    public final double Lift;
    private final EntityDimensions Size;

    public Z0004ShipPart(Z0004Ship Owner, String Name, float Width, float Height, double Along, double Lift) {
        super(Owner);
        this.Ship = Owner;
        this.Name = Name;
        this.Along = Along;
        this.Lift = Lift;
        this.Size = EntityDimensions.scalable(Width, Height);
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return Ship.getPickResult();
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return !isInvulnerableTo(Source) && Ship.hurtPart(this, Source, Amount);
    }

    @Override
    public boolean is(Entity Other) {
        return this == Other || Ship == Other;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity Tracker) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityDimensions getDimensions(Pose Stance) {
        return Size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
