package net.rai.scp_unleashed.scp085;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp085.network.Scp085ResidentPayload;
import org.jetbrains.annotations.Nullable;

public class Scp085Figure extends SavedData {
    private static final String DataName = "scp_unleashed_scp085";
    private static final SavedData.Factory<Scp085Figure> DataFactory = new SavedData.Factory<>(Scp085Figure::new, Scp085Figure::load);

    @Nullable
    private GlobalPos Home;
    @Nullable
    private GlobalPos Spot;
    private boolean Bonded;

    public static Scp085Figure get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public static Scp085Figure get(ServerLevel World) {
        return get(World.getServer());
    }

    @Nullable
    public GlobalPos home() {
        return Home;
    }

    @Nullable
    public GlobalPos spot() {
        return Spot;
    }

    public boolean bonded() {
        return Bonded;
    }

    public boolean resident() {
        return Bonded && Home == null && Spot == null;
    }

    public boolean isAt(Level World, BlockPos Pos) {
        return Spot != null && Spot.dimension() == World.dimension() && Spot.pos().equals(Pos);
    }

    public boolean isHome(Level World, BlockPos Pos) {
        return Home != null && Home.dimension() == World.dimension() && Home.pos().equals(Pos);
    }

    public boolean claimHome(ServerLevel World, BlockPos Pos) {
        if (Home != null && homeStands(World.getServer())) {
            return false;
        }
        Home = GlobalPos.of(World.dimension(), Pos.immutable());
        Bonded = true;
        if (Spot == null || World.getServer().getLevel(Spot.dimension()) == null) {
            Spot = Home;
        }
        changed(World.getServer());
        return true;
    }

    public void homeRemoved(ServerLevel World, BlockPos Pos) {
        if (!isHome(World, Pos)) {
            return;
        }
        if (Spot != null && Spot.equals(Home)) {
            Spot = null;
        }
        Home = null;
        changed(World.getServer());
    }

    public void sheetRemoved(ServerLevel World, BlockPos Pos) {
        if (!isAt(World, Pos)) {
            return;
        }
        Spot = Home;
        changed(World.getServer());
    }

    public void moveTo(ServerLevel World, BlockPos Pos) {
        Spot = GlobalPos.of(World.dimension(), Pos.immutable());
        changed(World.getServer());
    }

    public void sendHome(MinecraftServer Server) {
        Spot = Home;
        changed(Server);
    }

    public void reset(MinecraftServer Server) {
        Home = null;
        Spot = null;
        Bonded = false;
        changed(Server);
    }

    public void validate(MinecraftServer Server) {
        boolean Changed = false;
        if (Home != null && !standing(Server, Home, true)) {
            if (Spot != null && Spot.equals(Home)) {
                Spot = null;
            }
            Home = null;
            Changed = true;
        }
        if (Spot != null && !standing(Server, Spot, false)) {
            Spot = Home;
            Changed = true;
        }
        if (Changed) {
            changed(Server);
        }
    }

    private boolean homeStands(MinecraftServer Server) {
        return Home != null && standing(Server, Home, true);
    }

    private static boolean standing(MinecraftServer Server, GlobalPos Where, boolean NeedHome) {
        ServerLevel World = Server.getLevel(Where.dimension());
        if (World == null) {
            return false;
        }
        if (!World.isLoaded(Where.pos())) {
            return true;
        }
        if (NeedHome) {
            return World.getBlockState(Where.pos()).getBlock() instanceof Scp085HomeSheetBlock;
        }
        return World.getBlockState(Where.pos()).getBlock() instanceof Scp085SheetBlock;
    }

    private void changed(MinecraftServer Server) {
        setDirty();
        PacketDistributor.sendToAllPlayers(new Scp085ResidentPayload(resident()));
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        if (Home != null) {
            Tag.put("Home", write(Home));
        }
        if (Spot != null) {
            Tag.put("Spot", write(Spot));
        }
        Tag.putBoolean("Bonded", Bonded);
        return Tag;
    }

    private static CompoundTag write(GlobalPos Where) {
        CompoundTag Tag = new CompoundTag();
        Tag.putString("Dimension", Where.dimension().location().toString());
        Tag.putLong("Pos", Where.pos().asLong());
        return Tag;
    }

    @Nullable
    private static GlobalPos read(CompoundTag Tag, String Key) {
        if (!Tag.contains(Key)) {
            return null;
        }
        CompoundTag Entry = Tag.getCompound(Key);
        ResourceLocation Id = ResourceLocation.tryParse(Entry.getString("Dimension"));
        if (Id == null) {
            return null;
        }
        ResourceKey<Level> Dimension = ResourceKey.create(Registries.DIMENSION, Id);
        return GlobalPos.of(Dimension, BlockPos.of(Entry.getLong("Pos")));
    }

    public static Scp085Figure load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp085Figure Figure = new Scp085Figure();
        Figure.Home = read(Tag, "Home");
        Figure.Spot = read(Tag, "Spot");
        Figure.Bonded = Tag.getBoolean("Bonded");
        return Figure;
    }
}
