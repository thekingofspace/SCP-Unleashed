package net.rai.scp_unleashed.powerzone;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PowerZoneRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);

    private static final Map<PowerPropKind, DeferredBlock<PowerPropBlock>> Props = new EnumMap<>(PowerPropKind.class);
    private static final List<DeferredItem<BlockItem>> CreativeOrder = new ArrayList<>();

    static {
        for (PowerPropKind Kind : PowerPropKind.values()) {
            String Name = Kind.blockName();
            DeferredBlock<PowerPropBlock> Registered = BlockRegister.register(Name, () -> new PowerPropBlock(Kind, properties(Kind)));
            Props.put(Kind, Registered);
            CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Name, Registered));
        }
    }

    private PowerZoneRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }

    public static DeferredBlock<PowerPropBlock> prop(PowerPropKind Kind) {
        return Props.get(Kind);
    }

    public static List<DeferredItem<BlockItem>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    private static BlockBehaviour.Properties properties(PowerPropKind Kind) {
        boolean Wooden = Kind == PowerPropKind.CableSpool;
        float Hardness = Wooden ? 2.0f : 3.0f;
        return BlockBehaviour.Properties.of()
                .mapColor(Wooden ? MapColor.WOOD : MapColor.COLOR_GRAY)
                .strength(Hardness, Hardness * 2.0f)
                .sound(Wooden ? SoundType.WOOD : SoundType.METAL)
                .noOcclusion()
                .lightLevel(State -> Kind.getLight());
    }
}
