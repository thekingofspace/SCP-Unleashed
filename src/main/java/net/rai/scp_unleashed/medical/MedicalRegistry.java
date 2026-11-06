package net.rai.scp_unleashed.medical;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.decor.DecorRegistry;
import net.rai.scp_unleashed.decor.FacilityFloorMarkBlock;
import net.rai.scp_unleashed.decor.FacilityZone;
import net.rai.scp_unleashed.decor.ZonePart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MedicalRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);

    private static final List<DeferredItem<BlockItem>> CreativeOrder = new ArrayList<>();
    private static final Map<MedicalPropKind, DeferredBlock<MedicalPropBlock>> Props = new EnumMap<>(MedicalPropKind.class);

    public static final DeferredBlock<FacilityFloorMarkBlock> FloorGuide = floor("medical_floor_guide");
    public static final DeferredBlock<FacilityFloorMarkBlock> FloorGuideTurn = floor("medical_floor_guide_turn");

    static {
        for (MedicalPropKind Kind : MedicalPropKind.values()) {
            String Name = Kind.getSerializedName();
            DeferredBlock<MedicalPropBlock> Registered = BlockRegister.register(Name,
                    () -> Kind.isLightable() ? new MedicalLightBlock(Kind, lightProperties(Kind)) : new MedicalPropBlock(Kind, propProperties(Kind)));
            Props.put(Kind, Registered);
            CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Name, Registered));
        }
    }

    private MedicalRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }

    public static DeferredBlock<MedicalPropBlock> prop(MedicalPropKind Kind) {
        return Props.get(Kind);
    }

    public static List<DeferredItem<BlockItem>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    private static DeferredBlock<FacilityFloorMarkBlock> floor(String Name) {
        DeferredBlock<FacilityFloorMarkBlock> Registered = BlockRegister.register(Name, () -> new FacilityFloorMarkBlock(floorProperties()));
        CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Name, Registered));
        return Registered;
    }

    private static BlockBehaviour.Properties floorProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(FacilityZone.Medical.getMapColour(ZonePart.FloorMarked))
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(DecorRegistry.Hardness, DecorRegistry.BlastResistance)
                .sound(FacilityZone.Medical.getSound(ZonePart.FloorMarked));
    }

    private static BlockBehaviour.Properties propProperties(MedicalPropKind Kind) {
        MedicalMaterial Material = Kind.getMaterial();
        BlockBehaviour.Properties Settings = BlockBehaviour.Properties.of()
                .mapColor(Kind.getMapColour())
                .strength(Material.getHardness(), Material.getHardness() * 2.0f)
                .sound(Material.getSound())
                .noOcclusion();
        if (!Material.isSolid()) {
            Settings.noCollission().ignitedByLava();
        }
        return Settings;
    }

    private static BlockBehaviour.Properties lightProperties(MedicalPropKind Kind) {
        int Brightness = Kind.getLight();
        return propProperties(Kind).lightLevel(State -> State.getValue(MedicalLightBlock.Lit) ? Brightness : 0);
    }
}
