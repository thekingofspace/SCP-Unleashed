package net.rai.scp_unleashed.facilitysign;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.EnumMap;
import java.util.Map;

public final class FacilitySignRegistry {
    public static final int GlowLight = 5;
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final Map<FacilitySignDesign, DeferredBlock<FacilitySignBlock>> Signs = new EnumMap<>(FacilitySignDesign.class);
    public static final Map<FacilitySignDesign, DeferredBlock<FacilityPlaqueBlock>> Plaques = new EnumMap<>(FacilitySignDesign.class);
    public static final Map<FacilitySignDesign, DeferredItem<FacilitySignItem>> SignItems = new EnumMap<>(FacilitySignDesign.class);
    public static final Map<FacilitySignDesign, DeferredItem<FacilitySignItem>> PlaqueItems = new EnumMap<>(FacilitySignDesign.class);
    public static final Map<FacilitySignDesign, DeferredBlock<FacilityHeaderBlock>> Headers = new EnumMap<>(FacilitySignDesign.class);
    public static final Map<FacilitySignDesign, DeferredItem<FacilitySignItem>> HeaderItems = new EnumMap<>(FacilitySignDesign.class);

    static {
        for (FacilitySignDesign Design : FacilitySignDesign.values()) {
            DeferredBlock<FacilitySignBlock> Sign = BlockRegister.register(Design.largeId(), () -> new FacilitySignBlock(properties(Design)));
            DeferredBlock<FacilityPlaqueBlock> Plaque = BlockRegister.register(Design.smallId(), () -> new FacilityPlaqueBlock(properties(Design)));
            Signs.put(Design, Sign);
            Plaques.put(Design, Plaque);
            SignItems.put(Design, ItemRegister.register(Design.largeId(), () -> new FacilitySignItem(Sign.get(), new Item.Properties())));
            PlaqueItems.put(Design, ItemRegister.register(Design.smallId(), () -> new FacilitySignItem(Plaque.get(), new Item.Properties())));
            if (Design.hasHeader()) {
                DeferredBlock<FacilityHeaderBlock> Header = BlockRegister.register(Design.headerId(), () -> new FacilityHeaderBlock(properties(Design)));
                Headers.put(Design, Header);
                HeaderItems.put(Design, ItemRegister.register(Design.headerId(), () -> new FacilitySignItem(Header.get(), new Item.Properties())));
            }
        }
    }

    private FacilitySignRegistry() {
    }

    private static BlockBehaviour.Properties properties(FacilitySignDesign Design) {
        int Light = Design.glows() ? GlowLight : 0;
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0f, 4.0f).sound(SoundType.METAL).noOcclusion()
                .pushReaction(PushReaction.DESTROY).lightLevel(State -> Light)
                .isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
                .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false);
    }

    public static FacilitySignBlock sign(FacilitySignDesign Design) {
        return Signs.get(Design).get();
    }

    public static FacilityPlaqueBlock plaque(FacilitySignDesign Design) {
        return Plaques.get(Design).get();
    }

    public static FacilityHeaderBlock header(FacilitySignDesign Design) {
        return Headers.get(Design).get();
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }
}
