package net.rai.scp_unleashed.mining.shop;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalyDocumentItem;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.FieldNotes;
import net.rai.scp_unleashed.computer.ComputerBlockEntity;
import net.rai.scp_unleashed.computer.ComputerDeviceBlock;
import net.rai.scp_unleashed.computer.ComputerRegistry;
import net.rai.scp_unleashed.computer.net.ComputerCodecs;
import net.rai.scp_unleashed.computer.net.ComputerGrid;
import net.rai.scp_unleashed.computer.net.ComputerSection;
import net.rai.scp_unleashed.computer.net.ComputerSessions;
import net.rai.scp_unleashed.computer.network.ComputerNetworking;
import net.rai.scp_unleashed.computer.network.ComputerNoticePayload;
import net.rai.scp_unleashed.mining.MiningGrid;
import net.rai.scp_unleashed.mining.PointBankBlockEntity;
import net.rai.scp_unleashed.stasis.StasisContents;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ShopService {
    public static final int BuyCooldown = 10;
    public static final String NoticePrefix = "gui.scp_unleashed.computer.notice.";
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<UUID, Long> LastBuy = new HashMap<>();

    private ShopService() {
    }

    public static String points(long Amount) {
        return String.format(Locale.ROOT, "%,d", Amount);
    }

    public static CompoundTag sync(ServerLevel World, BlockPos ComputerPos) {
        List<PointBankBlockEntity> All = MiningGrid.banks(World, ComputerPos);
        List<PointBankBlockEntity> Running = MiningGrid.runningBanks(World, ComputerPos);
        CompoundTag Data = new CompoundTag();
        Data.putInt("Banks", Running.size());
        Data.putInt("Offline", Math.max(0, All.size() - Running.size()));
        Data.putLong("Balance", MiningGrid.total(Running));
        Data.put("Entries", ComputerCodecs.encodeList(ShopListing.ListingCodec, listings(), World.registryAccess()));
        return Data;
    }

    public static List<ShopListing> listings() {
        List<ShopListing> Found = new ArrayList<>();
        for (Map.Entry<ResourceLocation, ShopEntry> Listed : ShopCatalog.entries().entrySet()) {
            Found.add(listing(Listed.getKey(), Listed.getValue()));
        }
        return Found;
    }

    private static ShopListing listing(ResourceLocation Id, ShopEntry Entry) {
        AnomalyDocument Document = Entry.Document().map(Key -> AnomalyDocuments.server().get(Key)).orElse(null);
        Optional<DocumentClass> Classification = Document == null ? Optional.empty() : Optional.of(Document.Classification());
        Component Name = Entry.Name().orElseGet(() -> defaultName(Entry, Document));
        Component Description = Entry.Description().orElseGet(() -> Document == null ? Component.empty() : Document.title());
        ItemStack Icon = Entry.Icon().map(ItemStack::copy).orElseGet(() -> defaultIcon(Entry));
        int Count = Entry.Kind() == ShopKind.Item ? Entry.Goods().map(ItemStack::getCount).orElse(1) : 1;
        return new ShopListing(Id, Entry.Kind(), Entry.Price(), Name, Description, Icon, Count, Classification, Document != null);
    }

    private static Component defaultName(ShopEntry Entry, @Nullable AnomalyDocument Document) {
        if (Entry.Kind() == ShopKind.Item) {
            return Entry.Goods().map(ItemStack::getHoverName).orElse(Component.empty());
        }
        if (Document != null) {
            return Document.designation();
        }
        return Entry.Creature().flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional).map(EntityType::getDescription).orElse(Component.empty());
    }

    private static ItemStack defaultIcon(ShopEntry Entry) {
        if (Entry.Kind() == ShopKind.Item) {
            return Entry.Goods().map(Stack -> Stack.copyWithCount(1)).orElse(ItemStack.EMPTY);
        }
        EntityType<?> Type = Entry.Creature().flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional).orElse(null);
        SpawnEggItem Egg = Type == null ? null : SpawnEggItem.byId(Type);
        return Egg != null ? new ItemStack(Egg) : new ItemStack(StasisRegistry.podFor(Entry.Pod()));
    }

    public static void buy(ServerPlayer User, ServerLevel World, ComputerBlockEntity Computer, CompoundTag Data) {
        long Now = World.getGameTime();
        Long Last = LastBuy.get(User.getUUID());
        if (Last != null && Now >= Last && Now - Last < BuyCooldown) {
            return;
        }
        LastBuy.put(User.getUUID(), Now);
        BlockPos Pos = Computer.getBlockPos();
        ResourceLocation Id = ResourceLocation.tryParse(Data.getString("Entry"));
        ShopEntry Entry = Id == null ? null : ShopCatalog.get(Id);
        if (Entry == null) {
            notice(User, Pos, "gone", true);
            return;
        }
        if (Data.contains("Price") && Data.getLong("Price") != Entry.Price()) {
            notice(User, Pos, "price_changed", true);
            refresh(World, Pos);
            return;
        }
        List<PointBankBlockEntity> Banks = MiningGrid.runningBanks(World, Pos);
        if (Banks.isEmpty()) {
            notice(User, Pos, "no_bank", true);
            return;
        }
        if (MiningGrid.total(Banks) < Entry.Price()) {
            notice(User, Pos, "funds", true);
            return;
        }
        Delivery Goods = prepare(World, Computer, Entry);
        if (Goods == null) {
            notice(User, Pos, "failed", true);
            return;
        }
        long Owed = Entry.Price();
        List<Withdrawal> Taken = new ArrayList<>();
        for (PointBankBlockEntity Bank : Banks) {
            if (Owed <= 0L) {
                break;
            }
            long Paid = Bank.withdraw(Owed);
            if (Paid > 0L) {
                Taken.add(new Withdrawal(Bank, Paid));
                Owed -= Paid;
            }
        }
        if (Owed > 0L) {
            for (Withdrawal Refund : Taken) {
                Refund.Bank().deposit(Refund.Amount());
            }
            notice(User, Pos, "funds", true);
            return;
        }
        if (Goods.Released() != null && !release(World, Computer, Goods.Released())) {
            for (Withdrawal Refund : Taken) {
                Refund.Bank().deposit(Refund.Amount());
            }
            notice(User, Pos, "failed", true);
            return;
        }
        for (ItemStack Stack : Goods.Stacks()) {
            give(User, World, Computer, Stack);
        }
        World.playSound(null, Pos, Entry.Kind() == ShopKind.Entity ? StasisRegistry.CaptureChime.get() : ComputerRegistry.DiskSeek.get(), SoundSource.BLOCKS, 0.6f, 1.1f);
        ComputerNetworking.notice(User, Pos, Component.translatable(NoticePrefix + "purchased", listing(Id, Entry).Name()), ComputerNoticePayload.Info);
        refresh(World, Pos);
    }

    public static void forget(UUID Id) {
        LastBuy.remove(Id);
    }

    private static void refresh(ServerLevel World, BlockPos Pos) {
        ComputerSessions.touchReach(World, ComputerGrid.reach(World, Pos), ComputerSection.Shop.bit());
        ComputerSessions.touch(World, Pos, ComputerSection.Shop.bit());
    }

    private static void notice(ServerPlayer User, BlockPos Pos, String Key, boolean Error) {
        ComputerNetworking.notice(User, Pos, Component.translatable(NoticePrefix + Key), Error ? ComputerNoticePayload.Error : ComputerNoticePayload.Info);
    }

    @Nullable
    private static Delivery prepare(ServerLevel World, ComputerBlockEntity Computer, ShopEntry Entry) {
        List<ItemStack> Stacks = new ArrayList<>();
        Entity Released = null;
        if (Entry.Kind() == ShopKind.Item) {
            Entry.Goods().ifPresent(Stack -> Stacks.add(Stack.copy()));
            if (Stacks.isEmpty()) {
                return null;
            }
        } else {
            EntityType<?> Type = Entry.Creature().flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional).orElse(null);
            if (Type == null) {
                return null;
            }
            Entity Made;
            try {
                Made = Type.create(World);
            } catch (RuntimeException Problem) {
                Log.warn("Shop could not create {}", Entry.Creature().get(), Problem);
                return null;
            }
            if (Made == null) {
                return null;
            }
            Vec3 Spot = front(Computer);
            Made.moveTo(Spot.x, Spot.y, Spot.z, World.random.nextFloat() * 360.0f, 0.0f);
            if (Made instanceof Mob Creature) {
                Creature.finalizeSpawn(World, World.getCurrentDifficultyAt(Made.blockPosition()), MobSpawnType.SPAWN_EGG, null);
            }
            ItemStack Pod = podFor(World, Made, Entry);
            if (Pod.isEmpty()) {
                Released = Made;
            } else {
                Stacks.add(Pod);
            }
        }
        for (ItemStack Extra : Entry.Extras()) {
            Stacks.add(Extra.copy());
        }
        Entry.Document().ifPresent(Key -> {
            AnomalyDocument Document = AnomalyDocuments.server().get(Key);
            if (Document == null) {
                Log.warn("Shop entry dossier {} is not a loaded anomaly document", Key);
                return;
            }
            FieldNotes Notes = new FieldNotes(List.of(Component.translatable("archive.scp_unleashed.notes.procured")), true, Optional.empty());
            Stacks.add(AnomalyDocumentItem.create(Key, Document.Classification(), Notes));
        });
        return new Delivery(Stacks, Released);
    }

    private static ItemStack podFor(ServerLevel World, Entity Made, ShopEntry Entry) {
        EntityType<?> Type = Made.getType();
        if (!(Made instanceof Mob) || Type.is(StasisRegistry.Immune) || Made.isMultipartEntity() || !Type.canSerialize() || StasisPods.isVetoed(Made)) {
            return ItemStack.EMPTY;
        }
        CompoundTag Saved = new CompoundTag();
        if (!Made.save(Saved)) {
            return ItemStack.EMPTY;
        }
        Saved.remove("Passengers");
        Saved.remove("leash");
        float Health = Made instanceof LivingEntity Living ? Living.getHealth() : 0.0f;
        float MaxHealth = Made instanceof LivingEntity Living ? Living.getMaxHealth() : 0.0f;
        StasisContents Held = new StasisContents(Saved, Made.getName().copy(), Type.getDescription().copy(), Health, MaxHealth,
                World.getGameTime() + StasisPodItem.budget(Entry.Pod()));
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(Entry.Pod()));
        Pod.set(StasisRegistry.Contents.get(), Held);
        return Pod;
    }

    private static boolean release(ServerLevel World, ComputerBlockEntity Computer, Entity Made) {
        Vec3 Spot = StasisPods.findRoom(World, Made, front(Computer), Made.getYRot());
        Made.moveTo(Spot.x, Spot.y, Spot.z, Made.getYRot(), 0.0f);
        return StasisPods.place(World, Made, Made.getYRot());
    }

    private static Vec3 front(ComputerBlockEntity Computer) {
        BlockState State = Computer.getBlockState();
        Direction Facing = State.hasProperty(ComputerDeviceBlock.Facing) ? State.getValue(ComputerDeviceBlock.Facing) : Direction.NORTH;
        return Vec3.atBottomCenterOf(Computer.getBlockPos().relative(Facing));
    }

    private static void give(ServerPlayer User, ServerLevel World, ComputerBlockEntity Computer, ItemStack Stack) {
        if (Stack.isEmpty()) {
            return;
        }
        User.getInventory().add(Stack);
        if (Stack.isEmpty()) {
            return;
        }
        Vec3 Spot = front(Computer).add(0.0, 0.25, 0.0);
        ItemEntity Dropped = new ItemEntity(World, Spot.x, Spot.y, Spot.z, Stack.copy());
        Dropped.setDeltaMovement(Vec3.ZERO);
        Dropped.setDefaultPickUpDelay();
        World.addFreshEntity(Dropped);
    }

    private record Delivery(List<ItemStack> Stacks, @Nullable Entity Released) {
    }

    private record Withdrawal(PointBankBlockEntity Bank, long Amount) {
    }
}
