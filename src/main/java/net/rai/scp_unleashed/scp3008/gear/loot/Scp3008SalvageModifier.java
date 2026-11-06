package net.rai.scp_unleashed.scp3008.gear.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearRegistry;
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearTags;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008CarryParts;

public class Scp3008SalvageModifier extends LootModifier {
    public static final TagKey<Item> DisassemblyTools = TagKey.create(Registries.ITEM, Scp3008GearRegistry.location("scp3008_econ/disassembly_tools"));
    public static final MapCodec<Scp3008SalvageModifier> ModifierCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            IGlobalLootModifier.LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(Modifier -> Modifier.conditions),
            Codec.intRange(0, 16).fieldOf("table_legs_min").forGetter(Modifier -> Modifier.LegsMin),
            Codec.intRange(0, 16).fieldOf("table_legs_max").forGetter(Modifier -> Modifier.LegsMax),
            Codec.intRange(0, 16).fieldOf("cabinet_doors_min").forGetter(Modifier -> Modifier.DoorsMin),
            Codec.intRange(0, 16).fieldOf("cabinet_doors_max").forGetter(Modifier -> Modifier.DoorsMax)).apply(Instance, Scp3008SalvageModifier::new));
    private final int LegsMin;
    private final int LegsMax;
    private final int DoorsMin;
    private final int DoorsMax;

    public Scp3008SalvageModifier(LootItemCondition[] Conditions, int LegsMin, int LegsMax, int DoorsMin, int DoorsMax) {
        super(Conditions);
        this.LegsMin = LegsMin;
        this.LegsMax = Math.max(LegsMin, LegsMax);
        this.DoorsMin = DoorsMin;
        this.DoorsMax = Math.max(DoorsMin, DoorsMax);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> Loot, LootContext Context) {
        BlockState State = Context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (State == null || !Scp3008CarryParts.isMain(State)) {
            return Loot;
        }
        ItemStack Tool = Context.getParamOrNull(LootContextParams.TOOL);
        if (Tool != null && Tool.is(DisassemblyTools)) {
            return Loot;
        }
        RandomSource Random = Context.getRandom();
        if (State.is(Scp3008GearTags.TableLegs)) {
            add(Loot, Scp3008GearRegistry.TableLeg.get(), LegsMin + Random.nextInt(LegsMax - LegsMin + 1));
        }
        if (State.is(Scp3008GearTags.CabinetDoors)) {
            add(Loot, Scp3008GearRegistry.CabinetDoor.get(), DoorsMin + Random.nextInt(DoorsMax - DoorsMin + 1));
        }
        return Loot;
    }

    private static void add(ObjectArrayList<ItemStack> Loot, Item Kind, int Count) {
        if (Count > 0) {
            Loot.add(new ItemStack(Kind, Count));
        }
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModifierCodec;
    }
}
