package net.rai.scp_unleashed.archive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.ArchivePool;
import net.rai.scp_unleashed.archive.document.DocumentClaims;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentRoller;
import net.rai.scp_unleashed.archive.document.SpawnRule;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ArchiveBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FragmentSlot = 0;
    public static final int PaperSlot = 1;
    public static final int OutputSlot = 2;
    public static final int SlotCount = 3;
    public static final int PaperCost = 3;
    public static final int CardCount = 5;
    public static final int PrintTicks = 50;
    public static final int VoidCard = 15;
    public static final int CardData = 0;
    public static final int CountData = 5;
    public static final int SerialData = 6;
    public static final int RemainingData = 7;
    public static final int PrintingData = 8;
    public static final int MaskData = 9;
    public static final int TierData = 10;
    public static final int DataCount = 11;
    public static final int MaskRefreshTicks = 10;

    private final ItemStackHandler Storage = new ItemStackHandler(SlotCount) {
        @Override
        public boolean isItemValid(int Slot, ItemStack Stack) {
            return Slot == FragmentSlot ? isFragment(Stack) : Slot == PaperSlot && isPaper(Stack);
        }

        @Override
        protected void onContentsChanged(int Slot) {
            setChanged();
            if (Slot == FragmentSlot) {
                refreshMask();
            }
        }
    };
    private final List<ResourceLocation> Cards = new ArrayList<>();
    private final List<DocumentClass> CardClasses = new ArrayList<>();
    private final Set<Integer> VoidCards = new LinkedHashSet<>();
    private int Serial;
    private int RollTier;
    private int PrintRemaining;
    private int PrintingCard = -1;
    private ItemStack Printing = ItemStack.EMPTY;
    private int TierMask;

    private final ContainerData Data = new ContainerData() {
        @Override
        public int get(int Index) {
            if (Index < CardCount) {
                return cardCode(Index);
            }
            return switch (Index) {
                case CountData -> Cards.size();
                case SerialData -> Serial;
                case RemainingData -> PrintRemaining;
                case PrintingData -> PrintingCard;
                case MaskData -> TierMask;
                case TierData -> RollTier;
                default -> 0;
            };
        }

        @Override
        public void set(int Index, int Value) {
        }

        @Override
        public int getCount() {
            return DataCount;
        }
    };

    public ArchiveBlockEntity(BlockPos Pos, BlockState State) {
        super(ArchiveRegistry.ArchiveEntity.get(), Pos, State);
    }

    public static boolean isFragment(ItemStack Stack) {
        return ArchivePool.forFragment(Stack).isPresent();
    }

    private void refreshMask() {
        if (level instanceof ServerLevel World) {
            TierMask = DocumentRoller.tierMask(DocumentRoller.eligible(World.getServer(), null, currentPool()));
        }
    }

    public ArchivePool currentPool() {
        return ArchivePool.poolOf(Storage.getStackInSlot(FragmentSlot));
    }

    public ArchiveAutomation automation() {
        return new ArchiveAutomation(Storage);
    }

    public List<ResourceLocation> getCards() {
        return List.copyOf(Cards);
    }

    public static boolean isPaper(ItemStack Stack) {
        return Stack.is(Items.PAPER);
    }

    public ItemStackHandler getStorage() {
        return Storage;
    }

    private int cardCode(int Index) {
        if (Index >= Cards.size()) {
            return 0;
        }
        return VoidCards.contains(Index) ? VoidCard : CardClasses.get(Index).ordinal() + 1;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, ArchiveBlockEntity Terminal) {
        Terminal.tickServer(World);
    }

    private void tickServer(Level World) {
        if (PrintRemaining > 0) {
            PrintRemaining--;
            if (PrintRemaining == 0) {
                finishPrint(World);
            }
        }
        if (World.getGameTime() % MaskRefreshTicks == 0 && World instanceof ServerLevel ServerWorld) {
            TierMask = DocumentRoller.tierMask(DocumentRoller.eligible(ServerWorld.getServer(), null, currentPool()));
        }
    }

    public boolean roll(ServerPlayer User) {
        if (!(level instanceof ServerLevel World) || !Cards.isEmpty() || PrintRemaining > 0) {
            return false;
        }
        ItemStack Fragment = Storage.getStackInSlot(FragmentSlot);
        ItemStack Paper = Storage.getStackInSlot(PaperSlot);
        if (!isFragment(Fragment) || !isPaper(Paper) || Paper.getCount() < PaperCost) {
            return false;
        }
        Map<ResourceLocation, AnomalyDocument> Pool = DocumentRoller.eligible(World.getServer(), User.getUUID(), ArchivePool.poolOf(Fragment));
        int Tier = ArchivePool.tierFor(Fragment);
        List<ResourceLocation> Picked = DocumentRoller.roll(Pool, Tier, CardCount, World.getRandom());
        if (Picked.isEmpty()) {
            World.playSound(null, worldPosition, ArchiveRegistry.Deny.get(), SoundSource.BLOCKS, 0.6f, 1.0f);
            return false;
        }
        Storage.extractItem(FragmentSlot, 1, false);
        Storage.extractItem(PaperSlot, PaperCost, false);
        Cards.clear();
        CardClasses.clear();
        VoidCards.clear();
        for (ResourceLocation Id : Picked) {
            Cards.add(Id);
            CardClasses.add(Pool.get(Id).Classification());
        }
        Serial = (Serial + 1) & 0x7FFF;
        RollTier = Tier;
        TierMask = DocumentRoller.tierMask(Pool);
        setChanged();
        World.playSound(null, worldPosition, ArchiveRegistry.Roll.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        return true;
    }

    public boolean pick(ServerPlayer User, int Index) {
        if (!(level instanceof ServerLevel World) || PrintRemaining > 0 || Index < 0 || Index >= Cards.size() || VoidCards.contains(Index)) {
            return false;
        }
        if (!Storage.getStackInSlot(OutputSlot).isEmpty()) {
            User.displayClientMessage(Component.translatable("gui.scp_unleashed.archive.output_full"), true);
            return false;
        }
        ResourceLocation Id = Cards.get(Index);
        AnomalyDocument Document = AnomalyDocuments.server().get(Id);
        if (Document == null || (Document.Spawn() == SpawnRule.Once && !DocumentClaims.get(World.getServer()).take(Id, ArchiveRules.mode(World.getServer()), User.getUUID()))) {
            VoidCards.add(Index);
            if (VoidCards.size() >= Cards.size()) {
                Cards.clear();
                CardClasses.clear();
                VoidCards.clear();
            }
            setChanged();
            World.playSound(null, worldPosition, ArchiveRegistry.Deny.get(), SoundSource.BLOCKS, 0.7f, 1.0f);
            return true;
        }
        Printing = SealedDocumentItem.create(Id, Document.Classification());
        if (Document.Spawn() == SpawnRule.Once) {
            Printing.set(ArchiveRegistry.ClaimantComponent.get(), User.getUUID());
        }
        PrintRemaining = PrintTicks;
        PrintingCard = Index;
        setChanged();
        World.playSound(null, worldPosition, ArchiveRegistry.Print.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        return true;
    }

    private void finishPrint(Level World) {
        if (Storage.getStackInSlot(OutputSlot).isEmpty()) {
            Storage.setStackInSlot(OutputSlot, Printing);
        } else {
            Containers.dropItemStack(World, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, Printing);
        }
        Printing = ItemStack.EMPTY;
        Cards.clear();
        CardClasses.clear();
        VoidCards.clear();
        PrintingCard = -1;
        setChanged();
    }

    public void dropContents(Level World, BlockPos Pos) {
        for (int Slot = 0; Slot < SlotCount; Slot++) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Storage.getStackInSlot(Slot).copy());
            Storage.setStackInSlot(Slot, ItemStack.EMPTY);
        }
        if (!Printing.isEmpty()) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), Printing);
            Printing = ItemStack.EMPTY;
        }
    }

    public void openFor(ServerPlayer User) {
        if (level instanceof ServerLevel World) {
            TierMask = DocumentRoller.tierMask(DocumentRoller.eligible(World.getServer(), null, currentPool()));
        }
        User.openMenu(this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu Menu, RegistryFriendlyByteBuf Buffer) {
        Buffer.writeBlockPos(worldPosition);
        for (int Index = 0; Index < DataCount; Index++) {
            Buffer.writeVarInt(Index == MaskData && Menu instanceof ArchiveMenu Opened ? Opened.tierMask() : Data.get(Index));
        }
    }

    public static int[] readState(RegistryFriendlyByteBuf Buffer) {
        int[] Values = new int[DataCount];
        for (int Index = 0; Index < DataCount; Index++) {
            Values[Index] = Buffer.readVarInt();
        }
        return Values;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Inventory", Storage.serializeNBT(Registries));
        ListTag SavedCards = new ListTag();
        ListTag SavedClasses = new ListTag();
        for (int Index = 0; Index < Cards.size(); Index++) {
            SavedCards.add(StringTag.valueOf(Cards.get(Index).toString()));
            SavedClasses.add(StringTag.valueOf(CardClasses.get(Index).getSerializedName()));
        }
        Tag.put("Cards", SavedCards);
        Tag.put("CardClasses", SavedClasses);
        Tag.put("VoidCards", new IntArrayTag(VoidCards.stream().mapToInt(Integer::intValue).toArray()));
        Tag.putInt("Serial", Serial);
        Tag.putInt("RollTier", RollTier);
        Tag.putInt("PrintRemaining", PrintRemaining);
        Tag.putInt("PrintingCard", PrintingCard);
        if (!Printing.isEmpty()) {
            Tag.put("Printing", Printing.save(Registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Inventory")) {
            Storage.deserializeNBT(Registries, Tag.getCompound("Inventory"));
        }
        Cards.clear();
        CardClasses.clear();
        VoidCards.clear();
        ListTag SavedCards = Tag.getList("Cards", net.minecraft.nbt.Tag.TAG_STRING);
        ListTag SavedClasses = Tag.getList("CardClasses", net.minecraft.nbt.Tag.TAG_STRING);
        for (int Index = 0; Index < SavedCards.size() && Index < CardCount; Index++) {
            ResourceLocation Id = ResourceLocation.tryParse(SavedCards.getString(Index));
            if (Id == null) {
                continue;
            }
            Cards.add(Id);
            CardClasses.add(classNamed(SavedClasses.getString(Index)));
        }
        for (int Index : Tag.getIntArray("VoidCards")) {
            if (Index >= 0 && Index < Cards.size()) {
                VoidCards.add(Index);
            }
        }
        Serial = Tag.getInt("Serial");
        RollTier = Tag.getInt("RollTier");
        PrintRemaining = Math.max(0, Math.min(PrintTicks, Tag.getInt("PrintRemaining")));
        PrintingCard = Tag.contains("PrintingCard") ? Tag.getInt("PrintingCard") : -1;
        Printing = Tag.contains("Printing") ? ItemStack.parseOptional(Registries, Tag.getCompound("Printing")) : ItemStack.EMPTY;
        if (PrintRemaining > 0 && Printing.isEmpty()) {
            PrintRemaining = 0;
            PrintingCard = -1;
        }
    }

    private static DocumentClass classNamed(String Name) {
        for (DocumentClass Candidate : DocumentClass.values()) {
            if (Candidate.getSerializedName().equals(Name)) {
                return Candidate;
            }
        }
        return DocumentClass.Safe;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.scp_unleashed.archive");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int ContainerId, Inventory PlayerInventory, Player User) {
        return new ArchiveMenu(ContainerId, PlayerInventory, this, new ViewerData(User.getUUID()));
    }

    private final class ViewerData implements ContainerData {
        private final UUID Viewer;
        private int Mask = -1;
        private long Checked;
        private ArchivePool Checking = ArchivePool.Normal;

        private ViewerData(UUID Viewer) {
            this.Viewer = Viewer;
        }

        @Override
        public int get(int Index) {
            return Index == MaskData ? mask() : Data.get(Index);
        }

        private int mask() {
            if (!(level instanceof ServerLevel World) || !ArchiveRules.perPlayer(World.getServer())) {
                Mask = -1;
                return TierMask;
            }
            long Now = World.getGameTime();
            ArchivePool Source = currentPool();
            if (Mask < 0 || Source != Checking || Now - Checked >= MaskRefreshTicks || Now < Checked) {
                Mask = DocumentRoller.tierMask(DocumentRoller.eligible(World.getServer(), Viewer, Source));
                Checked = Now;
                Checking = Source;
            }
            return Mask;
        }

        @Override
        public void set(int Index, int Value) {
        }

        @Override
        public int getCount() {
            return DataCount;
        }
    }

    public boolean isUsableBy(Player User) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && User.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5) <= 64.0;
    }
}
