package net.rai.scp_unleashed.scp610;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.Factions;
import org.jetbrains.annotations.Nullable;

public class Scp610BroodSac extends Scp610Creature {
    public static final int GrowTicks = 60;
    public static final int BirthTicks = 40;
    public static final int ReleaseTick = 24;
    public static final int BirthGap = 1200;
    public static final int FirstBirth = 400;
    public static final int MinFlesh = 128;
    private int BirthReady = FirstBirth;
    @Nullable
    private Scp610Form Pending;

    public Scp610BroodSac(EntityType<? extends Scp610BroodSac> Type, Level World) {
        super(Type, World, Scp610Form.BroodSac);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Scp610Goals.HoldStill(this));
    }

    @Override
    public String spawnClip() {
        return "grow";
    }

    @Override
    public int spawnTicks() {
        return GrowTicks;
    }

    @Nullable
    public Scp610Hive hive() {
        if (!(level() instanceof ServerLevel World)) {
            return null;
        }
        int Site = Factions.bondOf(this).Site();
        return Site < 0 ? Scp610Biomass.nearest(World, blockPosition(), 24.0) : Scp610Hives.hive(World.getServer(), Site);
    }

    @Nullable
    public Scp610Form pending() {
        return Pending;
    }

    @Nullable
    public Scp610Form choose(ServerLevel World, FactionState State, Scp610Hive Hive) {
        if (State.phase() == FactionPhase.Apex && Hive.biomass() >= Scp610Biomass.AdvancedCost) {
            Scp610Form Advanced = Scp610Biomass.pickAdvanced(World.random, Hive);
            if (Advanced != null) {
                return Advanced;
            }
        }
        FactionSite Site = Hive.Site;
        if (State.rosterAt(Site.id(), Scp610Faction.RoleUnit) >= Scp610Faction.Instance.siteCap(State, Site)) {
            return null;
        }
        Scp610Form Form = Scp610Biomass.pickHiveRecruit(World.random, Hive);
        return Hive.biomass() >= Scp610Biomass.costOf(Form) ? Form : null;
    }

    public boolean beginBirth() {
        if (!canAct() || !(level() instanceof ServerLevel World)) {
            return false;
        }
        Scp610Hive Hive = hive();
        if (Hive == null) {
            return false;
        }
        FactionState State = Scp610Hives.state(World.getServer());
        Scp610Form Form = choose(World, State, Hive);
        if (Form == null) {
            return false;
        }
        Pending = Form;
        setAction(ActionBirth, BirthTicks);
        playLayers(Scp610Sounds.EvolveLayers, 1.0f, 1.2f);
        return true;
    }

    @Nullable
    public Scp610Creature release(ServerLevel World) {
        Scp610Form Form = Pending;
        Pending = null;
        Scp610Hive Hive = hive();
        if (Form == null || Hive == null || !Scp610Biomass.spend(Hive, Scp610Biomass.costOf(Form))) {
            return null;
        }
        Vec3 Front = position().add(getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(getBbWidth() * 0.8 + 0.6));
        Scp610Creature Made = Scp610Spawning.raise(World, Form, Front, getYRot(), true);
        if (Made == null) {
            Scp610Biomass.add(Hive, Scp610Biomass.costOf(Form));
            return null;
        }
        Made.setHome(Hive.anchor());
        FactionControl.enlist(Scp610Faction.Instance, Scp610Hives.state(World.getServer()), Made, Hive.id(), Scp610Faction.RoleUnit);
        World.playSound(null, Front.x, Front.y, Front.z, Scp610Sounds.Convert.get(), SoundSource.HOSTILE, 1.2f, 1.1f);
        World.sendParticles(Gore, Front.x, Front.y + 0.5, Front.z, 24, 0.4, 0.4, 0.4, 0.0);
        return Made;
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        if (Kind == ActionBirth && Elapsed == ReleaseTick && level() instanceof ServerLevel World) {
            release(World);
            return;
        }
        super.actionTick(Kind, Elapsed);
    }

    @Override
    protected void serverTick() {
        getNavigation().stop();
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        BirthReady = Math.max(0, BirthReady - 1);
        if (BirthReady <= 0) {
            BirthReady = BirthGap;
            beginBirth();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("BirthReady", BirthReady);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        BirthReady = Tag.contains("BirthReady") ? Tag.getInt("BirthReady") : FirstBirth;
    }
}
