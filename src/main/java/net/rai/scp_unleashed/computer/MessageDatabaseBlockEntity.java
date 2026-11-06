package net.rai.scp_unleashed.computer;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.computer.data.ChatChannels;
import net.rai.scp_unleashed.computer.data.ChatMessage;
import net.rai.scp_unleashed.computer.data.ComputerText;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MessageDatabaseBlockEntity extends ComputerDeviceEntity {
    public static final int Draw = 3;
    public static final int Capacity = 200;
    private static final Logger Log = LogUtils.getLogger();

    private final ArrayDeque<ChatMessage> Messages = new ArrayDeque<>();
    private UUID Id = UUID.randomUUID();
    private String Name = "";
    private long NextId = 1L;

    public MessageDatabaseBlockEntity(BlockPos Pos, BlockState State) {
        super(ComputerRegistry.MessageDatabaseEntity.get(), Pos, State);
    }

    @Override
    public int getDraw() {
        return Draw;
    }

    public String getKey() {
        return ChatChannels.database(Id);
    }

    public String getChannelName() {
        return Name.isEmpty() ? defaultName(Id) : Name;
    }

    public static String defaultName(UUID Id) {
        return "CHANNEL-" + Id.toString().substring(0, 4).toUpperCase(Locale.ROOT);
    }

    public boolean rename(String Requested) {
        String Cleaned = ComputerText.upper(Requested, ChatChannels.MaxName).replace(' ', '-');
        if (Cleaned.equals(Name)) {
            return false;
        }
        Name = Cleaned;
        syncClients();
        return true;
    }

    public ChatMessage append(ChatMessage Message) {
        ChatMessage Stored = Message.withId(NextId++);
        Messages.addLast(Stored);
        while (Messages.size() > Capacity) {
            Messages.removeFirst();
        }
        syncClients();
        return Stored;
    }

    public void clear() {
        Messages.clear();
        syncClients();
    }

    public List<ChatMessage> messages() {
        return List.copyOf(Messages);
    }

    public int count() {
        return Messages.size();
    }

    public long lastId() {
        return Messages.isEmpty() ? 0L : Messages.getLast().Id();
    }

    private CompoundTag saveStorage(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putUUID("ChannelId", Id);
        Tag.putString("ChannelName", Name);
        Tag.putLong("NextId", NextId);
        ListTag Saved = new ListTag();
        for (ChatMessage Message : Messages) {
            ChatMessage.MessageCodec.encodeStart(Registries.createSerializationContext(NbtOps.INSTANCE), Message).result().ifPresent(Saved::add);
        }
        Tag.put("Messages", Saved);
        return Tag;
    }

    private void loadStorage(CompoundTag Tag, HolderLookup.Provider Registries) {
        if (Tag.hasUUID("ChannelId")) {
            Id = Tag.getUUID("ChannelId");
        }
        Name = ComputerText.upper(Tag.getString("ChannelName"), ChatChannels.MaxName);
        NextId = Math.max(1L, Tag.getLong("NextId"));
        Messages.clear();
        ListTag Saved = Tag.getList("Messages", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            ChatMessage.MessageCodec.parse(Registries.createSerializationContext(NbtOps.INSTANCE), Saved.getCompound(Index))
                    .resultOrPartial(Problem -> Log.warn("Skipping a damaged stored message: {}", Problem))
                    .ifPresent(Messages::addLast);
        }
        while (Messages.size() > Capacity) {
            Messages.removeFirst();
        }
        for (ChatMessage Message : Messages) {
            NextId = Math.max(NextId, Message.Id() + 1L);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.put("Storage", saveStorage(Registries));
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        if (Tag.contains("Storage", Tag.TAG_COMPOUND)) {
            loadStorage(Tag.getCompound("Storage"), Registries);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        if (level != null) {
            Components.set(ComputerRegistry.StorageComponent.get(), CustomData.of(saveStorage(level.registryAccess())));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        CustomData Stored = Input.get(ComputerRegistry.StorageComponent.get());
        if (Stored != null && level != null) {
            loadStorage(Stored.copyTag(), level.registryAccess());
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag Tag) {
        super.removeComponentsFromTag(Tag);
        Tag.remove("Storage");
    }

    public static int storedCount(CustomData Stored) {
        return Stored.copyTag().getList("Messages", Tag.TAG_COMPOUND).size();
    }

    public static String storedName(CustomData Stored) {
        CompoundTag Tag = Stored.copyTag();
        String Name = Tag.getString("ChannelName");
        return Name.isEmpty() && Tag.hasUUID("ChannelId") ? defaultName(Tag.getUUID("ChannelId")) : Name;
    }
}
