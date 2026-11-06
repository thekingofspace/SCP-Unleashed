package net.rai.scp_unleashed.decal;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.FaceLayer;
import org.joml.Vector3f;

import java.util.Set;

public enum DecalKind {
    ClawMarks("claw_marks", 4, false, false, Orientation.Scatter, Set.of(), -1),
    BloodSplatter("blood_splatter", 4, false, true, Orientation.Scatter, Set.of(3), 0x6A080D),
    StrangeFluid("strange_fluid", 4, false, true, Orientation.Scatter, Set.of(2), 0x8E5F1A),
    Corrosion("corrosion", 4, false, true, Orientation.Scatter, Set.of(2), 0x1C1519),
    Footprints("footprints", 3, false, true, Orientation.Facing, Set.of(), 0x5C442C),
    Puddle("puddle", 3, false, true, Orientation.Scatter, Set.of(), 0x58708A),
    OilStain("oil_stain", 3, false, true, Orientation.Scatter, Set.of(), 0x1E1C1A),
    Grime("grime", 3, false, true, Orientation.Scatter, Set.of(), 0x4A5238),
    RustStreaks("rust_streaks", 3, false, true, Orientation.Scatter, Set.of(1, 2, 3), 0x8C4620),
    ScorchMarks("scorch_marks", 3, false, true, Orientation.Scatter, Set.of(3), 0x24201C),
    BulletHoles("bullet_holes", 3, false, false, Orientation.Scatter, Set.of(), -1),
    Cracks("cracks", 3, false, false, Orientation.Scatter, Set.of(), -1),
    HazardSign("hazard_sign", 6, false, false, Orientation.Facing, Set.of(), 0xE6B83C),
    ZoneSign("zone_sign", 5, false, false, Orientation.Facing, Set.of(), 0xD6D8D6),
    ArrowMarker("arrow_marker", 4, false, false, Orientation.Facing, Set.of(), 0xEEC440),
    FoundationEmblem("foundation_emblem", 2, false, false, Orientation.Facing, Set.of(), 0xD6D8D6),
    Vent("vent", 4, true, false, Orientation.Facing, Set.of(), 0xAEB4BA),
    Fixture("fixture", 7, true, false, Orientation.Facing, Set.of(), 0x8A9098);

    private final String Name;
    private final int Variants;
    private final boolean Modelled;
    private final boolean Translucent;
    private final Orientation Turning;
    private final Set<Integer> Hanging;
    private final int DustColour;

    DecalKind(String Name, int Variants, boolean Modelled, boolean Translucent, Orientation Turning, Set<Integer> Hanging, int DustColour) {
        this.Name = Name;
        this.Variants = Variants;
        this.Modelled = Modelled;
        this.Translucent = Translucent;
        this.Turning = Turning;
        this.Hanging = Hanging;
        this.DustColour = DustColour;
    }

    public String getName() {
        return Name;
    }

    public int getVariants() {
        return Variants;
    }

    public ResourceLocation layerId(int Variant) {
        String Folder = Modelled ? "block/layer/decal/" : "block/decal/";
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Folder + Name + "_" + Variant);
    }

    public FaceLayer layer(int Variant, Direction Face, Direction Facing, RandomSource Random) {
        ResourceLocation Id = layerId(Variant);
        FaceLayer Base = Modelled ? FaceLayer.model(Id) : FaceLayer.texture(Id);
        return Base.withSpin(spin(Variant, Face, Facing, Random)).withTranslucent(Translucent);
    }

    private Rotation spin(int Variant, Direction Face, Direction Facing, RandomSource Random) {
        if (Face.getAxis().isHorizontal()) {
            return Turning == Orientation.Facing || Hanging.contains(Variant) ? Rotation.NONE : Rotation.getRandom(Random);
        }
        if (Turning != Orientation.Facing) {
            return Rotation.getRandom(Random);
        }
        return Modelled ? modelSpin(Face, Facing) : textureSpin(Face, Facing);
    }

    private static Rotation modelSpin(Direction Face, Direction Facing) {
        return switch (Facing) {
            case NORTH -> Rotation.CLOCKWISE_180;
            case SOUTH -> Rotation.NONE;
            case WEST -> Face == Direction.UP ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
            default -> Face == Direction.UP ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
        };
    }

    private static Rotation textureSpin(Direction Face, Direction Facing) {
        return switch (Facing) {
            case NORTH -> Rotation.NONE;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case EAST -> Face == Direction.UP ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
            default -> Face == Direction.UP ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
        };
    }

    public SoundEvent placeSound() {
        return Modelled ? DecalRegistry.DecalMount.get() : DecalRegistry.DecalPlace.get();
    }

    public ParticleOptions particle(BlockState Surface) {
        if (DustColour < 0) {
            return new BlockParticleOption(ParticleTypes.BLOCK, Surface);
        }
        Vector3f Colour = new Vector3f((DustColour >> 16 & 0xFF) / 255.0f, (DustColour >> 8 & 0xFF) / 255.0f, (DustColour & 0xFF) / 255.0f);
        return new DustParticleOptions(Colour, 0.8f);
    }

    public Component variantLine(int Variant) {
        if (Variant <= 0) {
            return Component.translatable("message.scp_unleashed.decal.random");
        }
        return Component.translatable("message.scp_unleashed.decal.variant", Variant, Variants,
                Component.translatable("decal.scp_unleashed." + Name + "." + Variant));
    }

    public enum Orientation {
        Scatter,
        Facing
    }
}
