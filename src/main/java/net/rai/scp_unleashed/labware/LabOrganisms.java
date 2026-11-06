package net.rai.scp_unleashed.labware;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public final class LabOrganisms {
    public static final int UnknownColour = 0xB7C4B2;
    public static final String UnknownKey = "labware.scp_unleashed.agent.unknown";
    private static final Map<ResourceLocation, LabOrganism> Entries = new ConcurrentHashMap<>();
    private static final List<LabOrganism> Ordered = new CopyOnWriteArrayList<>();

    private LabOrganisms() {
    }

    public static synchronized <T extends LabOrganism> T register(T Organism) {
        LabOrganism Previous = Entries.put(Organism.getId(), Organism);
        if (Previous != null) {
            Ordered.remove(Previous);
        }
        Ordered.add(Organism);
        return Organism;
    }

    public static Optional<LabOrganism> find(@Nullable ResourceLocation Id) {
        return Id == null ? Optional.empty() : Optional.ofNullable(Entries.get(Id));
    }

    public static boolean isKnown(@Nullable ResourceLocation Id) {
        return Id != null && Entries.containsKey(Id);
    }

    public static List<LabOrganism> all() {
        return List.copyOf(Ordered);
    }

    public static List<ResourceLocation> detect(LivingEntity Subject) {
        List<ResourceLocation> Found = new ArrayList<>();
        for (LabOrganism Organism : Ordered) {
            if (Organism.isPresentOn(Subject) || LabContamination.carries(Subject, Organism.getId())) {
                Found.add(Organism.getId());
            }
        }
        return List.copyOf(Found);
    }

    private static int colour(@Nullable ResourceLocation Id, ToIntFunction<LabOrganism> Picker) {
        return find(Id).map(Organism -> Picker.applyAsInt(Organism) & 0xFFFFFF).orElse(UnknownColour);
    }

    public static int colourOf(@Nullable ResourceLocation Id) {
        return colour(Id, LabOrganism::getColour);
    }

    public static int vialColourOf(@Nullable ResourceLocation Id) {
        return colour(Id, LabOrganism::getVialColour);
    }

    public static int cultureColourOf(@Nullable ResourceLocation Id) {
        return colour(Id, LabOrganism::getCultureColour);
    }

    public static int mistColourOf(@Nullable ResourceLocation Id) {
        return colour(Id, LabOrganism::getMistColour);
    }

    public static double radiusOf(@Nullable ResourceLocation Id) {
        return find(Id).map(LabOrganism::getRadius).orElse(LabExposure.Radius);
    }

    public static MutableComponent nameOf(@Nullable ResourceLocation Id) {
        return find(Id).map(LabOrganism::getName).orElseGet(() -> Component.translatable(UnknownKey));
    }

    public static MutableComponent styledNameOf(@Nullable ResourceLocation Id) {
        return find(Id).map(LabOrganism::getStyledName).orElseGet(() -> Component.translatable(UnknownKey).withStyle(ChatFormatting.GRAY));
    }

    public static void expose(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, @Nullable ResourceLocation Id) {
        find(Id).ifPresent(Organism -> spread(World, Source, Ignored, Organism, Organism.getRadius(), null));
    }

    public static void spread(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, LabOrganism Organism, double Radius, @Nullable Entity Carrier) {
        Organism.expose(World, Source, Ignored, Radius);
        LabContamination.contaminate(World, Source, Ignored, Radius, Organism, Carrier);
    }

    public static void exposeAll(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, Collection<ResourceLocation> Ids) {
        for (ResourceLocation Id : new LinkedHashSet<>(Ids)) {
            expose(World, Source, Ignored, Id);
        }
    }
}
