package net.rai.scp_unleashed.goggles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.extraction.ExtractionRegistry;
import net.rai.scp_unleashed.integration.curios.CuriosGoggles;
import net.rai.scp_unleashed.scp020.Scp020Registry;
import net.rai.scp_unleashed.scp096.Scp096Registry;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Goggles {
    public static final String SlotId = "goggles";
    public static final String WornKey = "tooltip.scp_unleashed.goggles.worn_curios";
    public static final String CarriedKey = "tooltip.scp_unleashed.goggles.carried_curios";

    public enum Kind {
        Scramble(true),
        Hyperspectral(true),
        RiftDetector(false);

        private final boolean FromUse;

        Kind(boolean FromUse) {
            this.FromUse = FromUse;
        }

        public Item item() {
            return switch (this) {
                case Scramble -> Scp096Registry.Goggles.get();
                case Hyperspectral -> Scp020Registry.Goggles.get();
                case RiftDetector -> ExtractionRegistry.RiftDetector.get();
            };
        }

        public boolean equipsFromUse() {
            return FromUse;
        }
    }

    private Goggles() {
    }

    public static boolean isWearing(@Nullable LivingEntity Wearer, Kind Worn) {
        return Senses.isWearing(Wearer, Worn.item());
    }

    @Nullable
    public static Kind kindOf(ItemStack Stack) {
        for (Kind Each : Kind.values()) {
            if (Stack.is(Each.item())) {
                return Each;
            }
        }
        return null;
    }

    public static void addSlotLine(List<Component> Lines, Kind Worn) {
        if (Senses.curiosLoaded()) {
            Lines.add(Component.translatable(Worn.equipsFromUse() ? WornKey : CarriedKey).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent Event) {
        if (Senses.curiosLoaded()) {
            CuriosGoggles.register();
        }
    }
}
