package net.rai.scp_unleashed.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.raid.RaidStyle;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class FactionType {
    private final ResourceLocation Id;
    private final int Colour;
    private final FactionRules Rules;

    protected FactionType(ResourceLocation Id, int Colour, FactionRules Rules) {
        this.Id = Id;
        this.Colour = Colour;
        this.Rules = Rules;
    }

    public final ResourceLocation id() {
        return Id;
    }

    public final int colour() {
        return Colour;
    }

    public final FactionRules rules() {
        return Rules;
    }

    public Component displayName() {
        return Component.translatable("faction." + Id.getNamespace() + "." + Id.getPath());
    }

    public double accrual(FactionState State, int Members) {
        return Rules.BasePowerTick() + Members * Rules.PowerPerMemberTick();
    }

    public int siteCap(FactionState State, FactionSite Site) {
        return 0;
    }

    public long reinforceInterval(FactionState State, FactionSite Site) {
        return 1200L;
    }

    public int siteRadius(FactionSite Site) {
        return 16;
    }

    public void awaken(ServerLevel World, FactionState State, BlockPos Near, @Nullable Entity Cause) {
    }

    public void advance(ServerLevel World, FactionState State, BlockPos Near) {
        planExpansion(World.getServer(), State, Near);
    }

    public void defeat(MinecraftServer Server, FactionState State, @Nullable BlockPos Where) {
    }

    public void reset(MinecraftServer Server, FactionState State) {
    }

    public boolean buildSite(ServerLevel World, FactionState State, FactionSite Site) {
        return false;
    }

    public void siteBuilt(ServerLevel World, FactionState State, FactionSite Site) {
    }

    public void tickSite(ServerLevel World, FactionState State, FactionSite Site) {
    }

    @Nullable
    public Mob recruit(ServerLevel World, FactionState State, FactionSite Site) {
        return null;
    }

    public boolean planExpansion(MinecraftServer Server, FactionState State, @Nullable BlockPos Near) {
        return false;
    }

    public boolean patrolsEnabled(FactionState State) {
        return false;
    }

    public void patrol(ServerLevel World, FactionState State, FactionSite Site, Vec3 Target, List<Mob> Group) {
    }

    public void raid(ServerLevel World, FactionState State, ServerPlayer Target, FactionSite From) {
    }

    @Nullable
    public RaidStyle raidStyle() {
        return null;
    }

    public boolean canRaid(FactionState State) {
        return raidStyle() != null && State.phase().active();
    }

    public void tick(MinecraftServer Server, FactionState State) {
    }

    public boolean claims(Entity Body) {
        return false;
    }

    public void staleMember(ServerLevel World, Entity Member) {
        Member.discard();
    }

    public void memberLost(ServerLevel World, FactionState State, LivingEntity Member, DamageSource Source) {
    }

    public void memberKilled(ServerLevel World, FactionState State, LivingEntity Member, LivingEntity Victim) {
    }

    public boolean fallsWithLastSite() {
        return false;
    }

    public void siteRazed(ServerLevel World, FactionState State, FactionSite Site, @Nullable BlockPos Where) {
    }

    public boolean idleMember(Mob Member) {
        return Member.getTarget() == null;
    }
}
