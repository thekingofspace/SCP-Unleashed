package net.rai.scp_unleashed.computer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.computer.data.FloppyContents;

import java.util.List;

public class UploadPortBlockEntity extends ComputerDeviceEntity {
    public static final int Draw = 2;

    private ItemStack Content = ItemStack.EMPTY;

    public UploadPortBlockEntity(BlockPos Pos, BlockState State) {
        super(ComputerRegistry.UploadPortEntity.get(), Pos, State);
    }

    @Override
    public int getDraw() {
        return Draw;
    }

    public static boolean isReadableDossier(ItemStack Stack) {
        return (Stack.is(ArchiveRegistry.OpenedDocument.get()) || Stack.is(ArchiveRegistry.DatabaseCopy.get())) && Stack.get(ArchiveRegistry.StampComponent.get()) != null;
    }

    public static boolean accepts(ItemStack Stack) {
        return isReadableDossier(Stack) || Stack.getItem() instanceof FloppyDiscItem;
    }

    public static PortMedia mediaOf(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return PortMedia.Empty;
        }
        return Stack.getItem() instanceof FloppyDiscItem ? PortMedia.Floppy : PortMedia.Dossier;
    }

    public ItemStack getContent() {
        return Content;
    }

    public PortMedia getMedia() {
        return mediaOf(Content);
    }

    public boolean isEmpty() {
        return Content.isEmpty();
    }

    public boolean isWritable() {
        return getMedia() == PortMedia.Floppy && Content.getItem() instanceof FloppyDiscItem Disc && Disc.isWritable(Content);
    }

    public int capacity() {
        return switch (getMedia()) {
            case Floppy -> FloppyContents.Capacity;
            case Dossier -> 1;
            case Empty -> 0;
        };
    }

    public List<ComputerFile> files() {
        return switch (getMedia()) {
            case Floppy -> FloppyDiscItem.contentsOf(Content).Files();
            case Dossier -> {
                ComputerFile File = ComputerFile.fromStack(Content);
                yield File == null ? List.of() : List.of(File);
            }
            case Empty -> List.of();
        };
    }

    public void insert(ItemStack Stack) {
        Content = Stack.copyWithCount(1);
        contentChanged();
        play(ComputerRegistry.DiskInsert.get(), 0.8f, 1.0f);
        play(ComputerRegistry.DiskSeek.get(), 0.35f, 0.85f);
    }

    public ItemStack eject() {
        ItemStack Taken = Content;
        Content = ItemStack.EMPTY;
        contentChanged();
        play(ComputerRegistry.DiskEject.get(), 0.8f, 1.0f);
        return Taken;
    }

    public ItemStack takeForDrop() {
        ItemStack Taken = Content;
        Content = ItemStack.EMPTY;
        return Taken;
    }

    public boolean write(ComputerFile File) {
        if (!isWritable()) {
            return false;
        }
        FloppyContents Current = FloppyDiscItem.contentsOf(Content);
        if (Current.isFull()) {
            return false;
        }
        FloppyDiscItem.setContents(Content, Current.with(File));
        contentChanged();
        play(ComputerRegistry.DiskSeek.get(), 0.6f, 1.0f);
        return true;
    }

    public boolean delete(int Index) {
        if (!isWritable()) {
            return false;
        }
        FloppyContents Current = FloppyDiscItem.contentsOf(Content);
        if (Index < 0 || Index >= Current.Files().size()) {
            return false;
        }
        FloppyDiscItem.setContents(Content, Current.without(Index));
        contentChanged();
        play(ComputerRegistry.DiskSeek.get(), 0.5f, 1.15f);
        return true;
    }

    public boolean rename(int Index, String Name) {
        if (!isWritable()) {
            return false;
        }
        FloppyContents Current = FloppyDiscItem.contentsOf(Content);
        if (Index < 0 || Index >= Current.Files().size()) {
            return false;
        }
        FloppyDiscItem.setContents(Content, Current.replaced(Index, Current.Files().get(Index).renamed(Name)));
        contentChanged();
        return true;
    }

    public void readSound() {
        play(ComputerRegistry.DiskSeek.get(), 0.45f, 0.9f);
    }

    private void play(SoundEvent Sound, float Volume, float Pitch) {
        if (level instanceof ServerLevel World) {
            World.playSound(null, worldPosition, Sound, SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    private void contentChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState State = getBlockState();
            if (State.hasProperty(UploadPortBlock.Media) && State.getValue(UploadPortBlock.Media) != getMedia()) {
                level.setBlock(worldPosition, State.setValue(UploadPortBlock.Media, getMedia()), Block.UPDATE_ALL);
            } else {
                level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (!Content.isEmpty()) {
            Tag.put("Content", Content.save(Registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Content = Tag.contains("Content", Tag.TAG_COMPOUND) ? ItemStack.parseOptional(Registries, Tag.getCompound("Content")) : ItemStack.EMPTY;
    }

    @Override
    protected void writeClient(CompoundTag Tag, HolderLookup.Provider Registries) {
        if (!Content.isEmpty()) {
            Tag.put("Content", Content.save(Registries));
        }
    }
}
