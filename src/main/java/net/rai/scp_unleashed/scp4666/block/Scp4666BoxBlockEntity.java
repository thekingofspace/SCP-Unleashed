package net.rai.scp_unleashed.scp4666.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Scp4666BoxBlockEntity extends BlockEntity {
    public static final Item[] Foods = {Items.BREAD, Items.COOKED_BEEF, Items.BAKED_POTATO, Items.COOKIE, Items.PUMPKIN_PIE, Items.COOKED_CHICKEN, Items.APPLE, Items.MUSHROOM_STEW,
            Items.COOKED_PORKCHOP, Items.SWEET_BERRIES, Items.COOKED_MUTTON, Items.CAKE};
    private final Set<UUID> Taken = new HashSet<>();
    private boolean Food = true;
    private long Seed;

    public Scp4666BoxBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp4666Registry.BoxEntity.get(), Pos, State);
    }

    public void fill(boolean WithFood, long Roll) {
        Food = WithFood;
        Seed = Roll;
        Taken.clear();
        setChanged();
    }

    public boolean hasFood() {
        return Food;
    }

    public boolean taken(UUID Id) {
        return Taken.contains(Id);
    }

    public List<ItemStack> take(Player User) {
        List<ItemStack> Share = new ArrayList<>();
        if (!Taken.add(User.getUUID())) {
            return Share;
        }
        Share.add(new ItemStack(Scp4666Registry.Cocoa.get()));
        if (Food) {
            RandomSource Random = RandomSource.create(Seed ^ User.getUUID().getMostSignificantBits());
            Share.add(new ItemStack(Foods[Random.nextInt(Foods.length)]));
        }
        setChanged();
        return Share;
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        Saved.putBoolean("Food", Food);
        Saved.putLong("Seed", Seed);
        ListTag Rows = new ListTag();
        for (UUID Id : Taken) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Id", Id);
            Rows.add(Row);
        }
        Saved.put("Taken", Rows);
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        Food = !Saved.contains("Food") || Saved.getBoolean("Food");
        Seed = Saved.getLong("Seed");
        Taken.clear();
        for (Tag Raw : Saved.getList("Taken", 10)) {
            CompoundTag Row = (CompoundTag) Raw;
            if (Row.hasUUID("Id")) {
                Taken.add(Row.getUUID("Id"));
            }
        }
    }
}
