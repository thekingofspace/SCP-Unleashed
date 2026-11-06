package net.rai.scp_unleashed.decor;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.DecoratableBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DecorRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final float Hardness = 2.0f;
    public static final float BlastResistance = 6.0f;

    private static final Map<FacilityZone, Map<ZonePart, DeferredBlock<Block>>> ZoneBlocks = new EnumMap<>(FacilityZone.class);
    private static final Map<FacilityMaterial, DeferredBlock<Block>> MaterialBlocks = new EnumMap<>(FacilityMaterial.class);
    private static final Map<FacilityMaterial, DeferredBlock<StairBlock>> MaterialStairs = new EnumMap<>(FacilityMaterial.class);
    private static final Map<FacilityMaterial, DeferredBlock<SlabBlock>> MaterialSlabs = new EnumMap<>(FacilityMaterial.class);
    private static final Map<LampColour, DeferredBlock<FacilityLampBlock>> ColouredLamps = new EnumMap<>(LampColour.class);
    private static final List<DeferredItem<BlockItem>> CreativeOrder = new ArrayList<>();

    static {
        for (FacilityZone Zone : FacilityZone.values()) {
            Map<ZonePart, DeferredBlock<Block>> Parts = new EnumMap<>(ZonePart.class);
            ZoneBlocks.put(Zone, Parts);
            for (ZonePart Part : ZonePart.values()) {
                String Name = Zone.blockName(Part);
                DeferredBlock<Block> Registered = BlockRegister.register(Name, () -> createZoneBlock(Zone, Part));
                Parts.put(Part, Registered);
                CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Name, Registered));
            }
        }
        for (FacilityMaterial Material : FacilityMaterial.values()) {
            String Name = Material.blockName();
            DeferredBlock<Block> Registered = BlockRegister.register(Name, () -> createMaterialBlock(Material));
            MaterialBlocks.put(Material, Registered);
            CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Name, Registered));
            if (Material.hasShapes()) {
                DeferredBlock<StairBlock> Stairs = BlockRegister.register(Name + "_stairs", () -> new StairBlock(Registered.get().defaultBlockState(), materialProperties(Material)));
                MaterialStairs.put(Material, Stairs);
                CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Name + "_stairs", Stairs));
                DeferredBlock<SlabBlock> Slab = BlockRegister.register(Name + "_slab", () -> new SlabBlock(materialProperties(Material)));
                MaterialSlabs.put(Material, Slab);
                CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Name + "_slab", Slab));
            }
        }
        for (LampColour Colour : LampColour.values()) {
            String Name = Colour.blockName();
            DeferredBlock<FacilityLampBlock> Registered = BlockRegister.register(Name, () -> new FacilityLampBlock(lampProperties(Colour.getGlowColour())));
            ColouredLamps.put(Colour, Registered);
            CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Name, Registered));
        }
    }

    private DecorRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }

    public static DeferredBlock<Block> zoneBlock(FacilityZone Zone, ZonePart Part) {
        return ZoneBlocks.get(Zone).get(Part);
    }

    public static DeferredBlock<Block> materialBlock(FacilityMaterial Material) {
        return MaterialBlocks.get(Material);
    }

    public static DeferredBlock<StairBlock> materialStairs(FacilityMaterial Material) {
        return MaterialStairs.get(Material);
    }

    public static DeferredBlock<SlabBlock> materialSlab(FacilityMaterial Material) {
        return MaterialSlabs.get(Material);
    }

    public static DeferredBlock<FacilityLampBlock> colouredLamp(LampColour Colour) {
        return ColouredLamps.get(Colour);
    }

    public static List<DeferredItem<BlockItem>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    private static Block createZoneBlock(FacilityZone Zone, ZonePart Part) {
        return switch (Part) {
            case Lamp -> new FacilityLampBlock(lampProperties(Zone.getMapColour(Part)));
            case WallBottom, WallMiddle, WallTop -> new FacilityWallBlock(zoneProperties(Zone, Part));
            case WallWindow -> new FacilityWindowBlock(zoneProperties(Zone, Part));
            case Pillar -> new FacilityPillarBlock(zoneProperties(Zone, Part));
            case FloorMarked, FloorArrow -> new FacilityFloorMarkBlock(zoneProperties(Zone, Part));
            case FloorMarkedCorner, FloorMarkedOuter -> new FacilityFloorCornerBlock(zoneProperties(Zone, Part));
            case Stairs -> new StairBlock(zoneBlock(Zone, ZonePart.Floor).get().defaultBlockState(), zoneProperties(Zone, Part));
            case WallStairs -> new StairBlock(zoneBlock(Zone, ZonePart.WallMiddle).get().defaultBlockState(), zoneProperties(Zone, Part));
            case Slab, WallSlab -> new SlabBlock(zoneProperties(Zone, Part));
            default -> new DecoratableBlock(zoneProperties(Zone, Part));
        };
    }

    private static Block createMaterialBlock(FacilityMaterial Material) {
        return switch (Material.getKind()) {
            case Mesh -> new FacilityMeshBlock(materialProperties(Material));
            case Marked -> new FacilityFloorMarkBlock(materialProperties(Material));
            default -> new DecoratableBlock(materialProperties(Material));
        };
    }

    private static BlockBehaviour.Properties zoneProperties(FacilityZone Zone, ZonePart Part) {
        return BlockBehaviour.Properties.of()
                .mapColor(Zone.getMapColour(Part))
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(Hardness, BlastResistance)
                .sound(Zone.getSound(Part));
    }

    private static BlockBehaviour.Properties materialProperties(FacilityMaterial Material) {
        BlockBehaviour.Properties Settings = BlockBehaviour.Properties.of()
                .mapColor(Material.getMapColour())
                .instrument(Material.getInstrument())
                .strength(Material.getHardness(), Material.getBlastResistance())
                .sound(Material.getSound());
        if (Material.needsTool()) {
            Settings.requiresCorrectToolForDrops();
        }
        if (Material.getKind() == MaterialKind.Mesh) {
            Settings.noOcclusion()
                    .isValidSpawn((State, World, Pos, Type) -> false)
                    .isRedstoneConductor((State, World, Pos) -> false)
                    .isSuffocating((State, World, Pos) -> false)
                    .isViewBlocking((State, World, Pos) -> false);
        }
        return Settings;
    }

    private static BlockBehaviour.Properties lampProperties(MapColor GlowColour) {
        return BlockBehaviour.Properties.of()
                .mapColor(State -> State.getValue(FacilityLampBlock.Lit) ? GlowColour : MapColor.COLOR_BLACK)
                .instrument(NoteBlockInstrument.HAT)
                .requiresCorrectToolForDrops()
                .strength(Hardness, BlastResistance)
                .sound(SoundType.GLASS)
                .lightLevel(State -> State.getValue(FacilityLampBlock.Lit) ? FacilityLampBlock.Brightness : 0);
    }
}
