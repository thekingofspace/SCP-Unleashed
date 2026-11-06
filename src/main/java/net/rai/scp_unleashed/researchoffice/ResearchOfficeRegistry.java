package net.rai.scp_unleashed.researchoffice;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;
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

public final class ResearchOfficeRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);

    private static final Map<WingProp, DeferredBlock<WingPropBlock>> Props = new EnumMap<>(WingProp.class);
    private static final List<DeferredItem<BlockItem>> CreativeOrder = new ArrayList<>();

    static {
        for (WingProp Kind : WingProp.values()) {
            String Name = Kind.getSerializedName();
            DeferredBlock<WingPropBlock> Registered = BlockRegister.register(Name, () -> new WingPropBlock(Kind, properties(Kind)));
            Props.put(Kind, Registered);
            CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Name, Registered));
        }
    }

    private ResearchOfficeRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }

    public static DeferredBlock<WingPropBlock> prop(WingProp Kind) {
        return Props.get(Kind);
    }

    public static List<DeferredItem<BlockItem>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    private static BlockBehaviour.Properties properties(WingProp Kind) {
        BlockBehaviour.Properties Settings = BlockBehaviour.Properties.of()
                .mapColor(Kind.getColour())
                .strength(Kind.getHardness(), Kind.getHardness() * 2.0f)
                .sound(Kind.getSound())
                .noOcclusion()
                .lightLevel(State -> Kind.getLight());
        if (Kind.getSound() == SoundType.WOOD) {
            Settings.ignitedByLava().instrument(NoteBlockInstrument.BASS);
        }
        if (Kind.getMount() == WingMount.Wall) {
            Settings.pushReaction(PushReaction.DESTROY);
        }
        return Settings;
    }
}
