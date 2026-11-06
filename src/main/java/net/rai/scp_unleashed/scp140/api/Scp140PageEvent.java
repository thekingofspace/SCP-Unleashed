package net.rai.scp_unleashed.scp140.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public class Scp140PageEvent extends Event {
    private final ServerLevel World;
    private final int Page;
    private final BlockPos Origin;
    @Nullable
    private BlockPos Site;
    @Nullable
    private ResourceKey<Level> SiteDimension;

    public Scp140PageEvent(ServerLevel World, int Page, BlockPos Origin) {
        this.World = World;
        this.Page = Page;
        this.Origin = Origin.immutable();
    }

    public static Scp140PageEvent fire(ServerLevel World, int Page, BlockPos Origin) {
        return NeoForge.EVENT_BUS.post(new Scp140PageEvent(World, Page, Origin));
    }

    public ServerLevel getLevel() {
        return World;
    }

    public int getPage() {
        return Page;
    }

    public BlockPos getOrigin() {
        return Origin;
    }

    public boolean isAwakening() {
        return Page >= Scp140Pages.Awakening;
    }

    @Nullable
    public BlockPos getSite() {
        return Site;
    }

    @Nullable
    public ResourceKey<Level> getSiteDimension() {
        return SiteDimension;
    }

    public void setSite(ResourceKey<Level> SiteDimension, BlockPos Site) {
        this.SiteDimension = SiteDimension;
        this.Site = Site.immutable();
    }
}
