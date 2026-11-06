package net.rai.scp_unleashed.labware;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class LabContamination {
    public static final double CarrierRadius = 3.0;
    public static final int ShedInterval = 10;
    public static final int ShedCount = 2;
    public static final float ShedScale = 0.55f;
    public static final int RinseTicks = 60;
    public static final int RinseGrace = 30;
    public static final int RinsedSplashes = 28;

    private LabContamination() {
    }

    public record Rinse(long Started, long Last) {
        public static final Rinse Idle = new Rinse(Long.MIN_VALUE / 2, Long.MIN_VALUE / 2);
    }

    public static List<ResourceLocation> carried(Entity Subject) {
        List<ResourceLocation> Carried = Subject.getExistingDataOrNull(LabwareRegistry.Contamination.get());
        return Carried == null ? List.of() : Carried;
    }

    public static boolean isContaminated(Entity Subject) {
        return !carried(Subject).isEmpty();
    }

    public static boolean carries(Entity Subject, ResourceLocation Id) {
        return carried(Subject).contains(Id);
    }

    public static boolean canCarry(LivingEntity Target, LabOrganism Organism) {
        return Target.isAlive() && !(Target instanceof Player User && User.isCreative()) && !Target.isSpectator() && Targeting.isTargetable(Target) && Organism.protects(Target);
    }

    public static boolean canCarry(LivingEntity Target, ResourceLocation Id) {
        return LabOrganisms.find(Id).map(Organism -> canCarry(Target, Organism)).orElse(false);
    }

    public static void contaminate(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius, LabOrganism Organism, @Nullable Entity Carrier) {
        ResourceLocation Id = Organism.getId();
        for (ServerPlayer Target : LabExposure.playersInSight(World, Source, Ignored, Radius, Candidate -> Candidate != Carrier && canCarry(Candidate, Organism) && !carries(Candidate, Id))) {
            add(Target, Id);
        }
        for (LivingEntity Target : ControlBodies.inSight(World, Source, Ignored, Radius, Candidate -> Candidate != Carrier && canCarry(Candidate, Organism) && !carries(Candidate, Id))) {
            add(Target, Id);
        }
    }

    public static void add(LivingEntity Target, ResourceLocation Id) {
        List<ResourceLocation> Carried = carried(Target);
        if (Carried.contains(Id)) {
            return;
        }
        List<ResourceLocation> Changed = new ArrayList<>(Carried);
        Changed.add(Id);
        Target.setData(LabwareRegistry.Contamination.get(), List.copyOf(Changed));
        if (Carried.isEmpty() && Target instanceof Player User) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.suit_contaminated").withStyle(ChatFormatting.GOLD), true);
        }
    }

    public static void clear(Entity Target) {
        Target.removeData(LabwareRegistry.Contamination.get());
        Target.removeData(LabwareRegistry.Rinse.get());
    }

    public static void tick(LivingEntity Carrier) {
        List<ResourceLocation> Carried = carried(Carrier);
        if (Carried.isEmpty() || !Carrier.isAlive() || !(Carrier.level() instanceof ServerLevel World)) {
            return;
        }
        if (Math.floorMod(World.getGameTime() + Carrier.getId(), ShedInterval) == 0) {
            shed(World, Carrier, Carried);
        }
        if (!LabExposure.isPulse(World, Carrier.getId())) {
            return;
        }
        Vec3 Centre = Carrier.getBoundingBox().getCenter();
        for (ResourceLocation Id : Carried) {
            LabOrganisms.find(Id).ifPresent(Organism -> LabOrganisms.spread(World, Centre, LabExposure.NothingIgnored, Organism, CarrierRadius, Carrier));
        }
        strip(Carrier, Carried);
    }

    public static void unprotected(LivingEntity Carrier) {
        List<ResourceLocation> Carried = carried(Carrier);
        if (Carried.isEmpty() || !Carrier.isAlive() || !(Carrier.level() instanceof ServerLevel World)) {
            return;
        }
        List<LabOrganism> Released = strip(Carrier, Carried);
        Vec3 Centre = Carrier.getBoundingBox().getCenter();
        for (LabOrganism Organism : Released) {
            LabOrganisms.spread(World, Centre, LabExposure.NothingIgnored, Organism, CarrierRadius, Carrier);
        }
    }

    private static List<LabOrganism> strip(LivingEntity Carrier, List<ResourceLocation> Carried) {
        List<ResourceLocation> Kept = new ArrayList<>();
        List<LabOrganism> Released = new ArrayList<>();
        for (ResourceLocation Id : Carried) {
            LabOrganism Organism = LabOrganisms.find(Id).orElse(null);
            if (Organism != null && Organism.protects(Carrier)) {
                Kept.add(Id);
            } else if (Organism != null) {
                Released.add(Organism);
            }
        }
        if (Kept.isEmpty()) {
            clear(Carrier);
        } else if (Kept.size() != Carried.size()) {
            Carrier.setData(LabwareRegistry.Contamination.get(), List.copyOf(Kept));
        }
        return Released;
    }

    private static void shed(ServerLevel World, LivingEntity Carrier, List<ResourceLocation> Carried) {
        AABB Box = Carrier.getBoundingBox();
        for (ResourceLocation Id : Carried) {
            int Colour = LabOrganisms.mistColourOf(Id);
            DustParticleOptions Speck = new DustParticleOptions(new Vector3f(((Colour >> 16) & 255) / 255.0f, ((Colour >> 8) & 255) / 255.0f, (Colour & 255) / 255.0f), ShedScale);
            World.sendParticles(Speck, Carrier.getX(), Box.minY + Box.getYsize() * 0.5, Carrier.getZ(), ShedCount, Box.getXsize() * 0.35, Box.getYsize() * 0.3, Box.getZsize() * 0.35, 0.0);
        }
    }

    public static void rinse(ServerLevel World, LivingEntity Target) {
        long Now = World.getGameTime();
        Rinse Previous = Target.getData(LabwareRegistry.Rinse.get());
        long Started = Now - Previous.Last() > RinseGrace ? Now : Previous.Started();
        if (Now - Started < RinseTicks) {
            Target.setData(LabwareRegistry.Rinse.get(), new Rinse(Started, Now));
            if (Target instanceof Player User) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.decontaminating").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        clear(Target);
        if (Target instanceof Player User) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.decontaminated").withStyle(ChatFormatting.AQUA), true);
        }
        AABB Box = Target.getBoundingBox();
        LabwareSounds.play(World, null, Target.getX(), Box.minY + Box.getYsize() * 0.6, Target.getZ(), LabwareSounds.Rinsed, SoundSource.PLAYERS, LabwareSounds.pitch(World));
        World.sendParticles(ParticleTypes.SPLASH, Target.getX(), Box.maxY, Target.getZ(), RinsedSplashes, Box.getXsize() * 0.4, 0.1, Box.getZsize() * 0.4, 0.15);
        World.sendParticles(ParticleTypes.FALLING_WATER, Target.getX(), Box.minY + Box.getYsize() * 0.5, Target.getZ(), RinsedSplashes / 2, Box.getXsize() * 0.4, Box.getYsize() * 0.35, Box.getZsize() * 0.4, 0.0);
    }
}
