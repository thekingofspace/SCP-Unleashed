package net.rai.scp_unleashed.scp001scarlet.king;

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

public class ScarletKingPart extends PartEntity<ScarletKing> {
    public final ScarletKing King;
    public final String Name;
    public final float Multiplier;
    private final EntityDimensions Size;

    public ScarletKingPart(ScarletKing Owner, String Name, float Width, float Height, float Multiplier) {
        super(Owner);
        this.King = Owner;
        this.Name = Name;
        this.Multiplier = Multiplier;
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
        return King.isAlive();
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return King.getPickResult();
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return King.hurtPart(this, Source, Amount);
    }

    @Override
    public boolean is(Entity Other) {
        return this == Other || King == Other;
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
