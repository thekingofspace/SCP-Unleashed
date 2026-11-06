package net.rai.scp_unleashed.scp105;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class Scp105Subjects {
    public static final float CreatureChance = 0.25f;
    public static final float YoungChance = 0.2f;
    public static final int ItemScenes = 6;
    public static final int CreatureScenes = 6;
    public static final int YoungScenes = 3;
    public static final int HostileScenes = 3;
    public static final int SceneRange = 256;
    public static final String Prefix = "message.scp_unleashed.scp105.";

    public record ItemEntry(ResourceLocation Id, int Weight, int Least, int Most) {
    }

    public record CreatureEntry(ResourceLocation Id, int Weight, boolean Grows) {
    }

    public static final List<ItemEntry> ItemPool = List.of(
            item("apple", 12, 1, 3),
            item("bread", 10, 1, 2),
            item("cookie", 10, 2, 6),
            item("sweet_berries", 8, 3, 8),
            item("melon_slice", 7, 2, 5),
            item("baked_potato", 6, 1, 3),
            item("pumpkin_pie", 6, 1, 1),
            item("honey_bottle", 5, 1, 1),
            item("cake", 4, 1, 1),
            item("poppy", 8, 1, 3),
            item("dandelion", 8, 1, 3),
            item("cornflower", 6, 1, 2),
            item("sunflower", 4, 1, 1),
            item("lily_of_the_valley", 4, 1, 1),
            item("book", 6, 1, 1),
            item("feather", 6, 1, 4),
            item("lantern", 5, 1, 1),
            item("candle", 5, 1, 2),
            item("painting", 3, 1, 1),
            item("compass", 3, 1, 1),
            item("clock", 3, 1, 1),
            item("spyglass", 3, 1, 1),
            item("name_tag", 2, 1, 1),
            item("saddle", 2, 1, 1),
            item("music_disc_cat", 1, 1, 1),
            item("emerald", 2, 1, 2),
            item("golden_apple", 2, 1, 1),
            item("diamond", 1, 1, 1));

    public static final List<CreatureEntry> CreaturePool = List.of(
            creature("sheep", 10, true),
            creature("cow", 10, true),
            creature("pig", 10, true),
            creature("chicken", 10, true),
            creature("rabbit", 7, true),
            creature("cat", 5, true),
            creature("fox", 4, true),
            creature("wolf", 3, true),
            creature("horse", 3, true),
            creature("donkey", 2, true),
            creature("llama", 2, true),
            creature("goat", 2, true),
            creature("frog", 3, false),
            creature("turtle", 2, true),
            creature("parrot", 2, false),
            creature("armadillo", 2, true),
            creature("panda", 1, true),
            creature("mooshroom", 1, true),
            creature("allay", 1, false),
            creature("zombie", 2, false),
            creature("skeleton", 2, false),
            creature("spider", 2, false),
            creature("slime", 1, false),
            creature("creeper", 1, false));

    public static final Set<ResourceLocation> BlockedCreatures = Set.of(vanilla("ender_dragon"), vanilla("wither"), vanilla("warden"), vanilla("elder_guardian"),
            vanilla("giant"), vanilla("illusioner"), vanilla("player"), vanilla("zombie_horse"));
    public static final Set<ResourceLocation> BlockedItems = Set.of(vanilla("air"), vanilla("command_block"), vanilla("chain_command_block"), vanilla("repeating_command_block"),
            vanilla("command_block_minecart"), vanilla("structure_block"), vanilla("structure_void"), vanilla("jigsaw"), vanilla("barrier"), vanilla("light"),
            vanilla("debug_stick"), vanilla("knowledge_book"), vanilla("spawner"), vanilla("trial_spawner"), vanilla("vault"), vanilla("bedrock"), vanilla("end_portal_frame"),
            vanilla("reinforced_deepslate"));

    private Scp105Subjects() {
    }

    private static ResourceLocation vanilla(String Path) {
        return ResourceLocation.withDefaultNamespace(Path);
    }

    private static ItemEntry item(String Path, int Weight, int Least, int Most) {
        return new ItemEntry(vanilla(Path), Weight, Least, Most);
    }

    private static CreatureEntry creature(String Path, int Weight, boolean Grows) {
        return new CreatureEntry(vanilla(Path), Weight, Grows);
    }

    public static boolean allowedItem(Item Thing) {
        ResourceLocation Id = BuiltInRegistries.ITEM.getKey(Thing);
        return Thing != Items.AIR && !BlockedItems.contains(Id) && !Scp_unleashed.ModId.equals(Id.getNamespace());
    }

    public static boolean allowedCreature(EntityType<?> Type) {
        ResourceLocation Id = BuiltInRegistries.ENTITY_TYPE.getKey(Type);
        return !BlockedCreatures.contains(Id) && !Scp_unleashed.ModId.equals(Id.getNamespace()) && !Type.is(Tags.EntityTypes.BOSSES) && Type.canSummon();
    }

    public static boolean livingCreature(EntityType<?> Type, Level World) {
        Entity Made = Type.create(World);
        boolean Living = Made instanceof Mob;
        if (Made != null) {
            Made.discard();
        }
        return Living;
    }

    public static boolean hostile(EntityType<?> Type) {
        return Type.getCategory() == MobCategory.MONSTER;
    }

    public static Scp105Photo roll(RandomSource Random, boolean Peaceful) {
        if (Random.nextFloat() < CreatureChance) {
            Optional<Scp105Photo> Found = rollCreature(Random, Peaceful);
            if (Found.isPresent()) {
                return Found.get();
            }
        }
        return rollItem(Random);
    }

    public static Scp105Photo rollItem(RandomSource Random) {
        int Total = 0;
        for (ItemEntry Entry : ItemPool) {
            Total += Entry.Weight();
        }
        int Pick = Random.nextInt(Total);
        for (ItemEntry Entry : ItemPool) {
            Pick -= Entry.Weight();
            if (Pick < 0 && BuiltInRegistries.ITEM.containsKey(Entry.Id())) {
                int Count = Entry.Least() + Random.nextInt(Entry.Most() - Entry.Least() + 1);
                return made(Scp105Photo.Kind.Thing, Entry.Id(), Count, false, Random);
            }
        }
        return made(Scp105Photo.Kind.Thing, vanilla("apple"), 1, false, Random);
    }

    public static Optional<Scp105Photo> rollCreature(RandomSource Random, boolean Peaceful) {
        int Total = 0;
        for (CreatureEntry Entry : CreaturePool) {
            if (usable(Entry, Peaceful)) {
                Total += Entry.Weight();
            }
        }
        if (Total <= 0) {
            return Optional.empty();
        }
        int Pick = Random.nextInt(Total);
        for (CreatureEntry Entry : CreaturePool) {
            if (!usable(Entry, Peaceful)) {
                continue;
            }
            Pick -= Entry.Weight();
            if (Pick < 0) {
                boolean Young = Entry.Grows() && Random.nextFloat() < YoungChance;
                return Optional.of(made(Scp105Photo.Kind.Creature, Entry.Id(), 1, Young, Random));
            }
        }
        return Optional.empty();
    }

    private static boolean usable(CreatureEntry Entry, boolean Peaceful) {
        Optional<EntityType<?>> Type = BuiltInRegistries.ENTITY_TYPE.getOptional(Entry.Id());
        return Type.isPresent() && !(Peaceful && hostile(Type.get()));
    }

    public static Scp105Photo made(Scp105Photo.Kind Kind, ResourceLocation Id, int Count, boolean Young, RandomSource Random) {
        return new Scp105Photo(Kind, Id, Math.max(1, Math.min(64, Count)), Young, Random.nextInt(SceneRange), Random.nextInt(Scp105Photo.Looks), 0L, 0L, false);
    }

    public static boolean grows(EntityType<?> Type, Level World) {
        Entity Made = Type.create(World);
        boolean Ageable = Made instanceof AgeableMob;
        if (Made != null) {
            Made.discard();
        }
        return Ageable;
    }

    public static Component displayName(Scp105Photo Photo) {
        if (Photo.Subject() == Scp105Photo.Kind.Thing) {
            Item Thing = BuiltInRegistries.ITEM.get(Photo.Id());
            return new ItemStack(Thing).getHoverName();
        }
        return BuiltInRegistries.ENTITY_TYPE.get(Photo.Id()).getDescription();
    }

    public static Component noun(Scp105Photo Photo) {
        Component Name = displayName(Photo);
        String Path = Photo.Id().getNamespace() + "." + Photo.Id().getPath();
        Language Words = Language.getInstance();
        if (Photo.Subject() == Scp105Photo.Kind.Creature && Photo.Young()) {
            String Young = Prefix + "young." + Path;
            return Component.translatable(Words.has(Young) ? Young : Prefix + "young", Name);
        }
        String Noun = Prefix + "noun." + Path;
        if (Words.has(Noun)) {
            return Component.translatable(Noun, Name);
        }
        String Plain = Name.getString().toLowerCase(Locale.ROOT);
        boolean Vowel = !Plain.isEmpty() && "aeiou".indexOf(Plain.charAt(0)) >= 0;
        return Component.translatable(Vowel ? Prefix + "noun.vowel" : Prefix + "noun", Name);
    }

    public static String sceneKey(Scp105Photo Photo) {
        if (Photo.Subject() == Scp105Photo.Kind.Thing) {
            return Prefix + "scene.item." + Photo.Scene() % ItemScenes;
        }
        EntityType<?> Type = BuiltInRegistries.ENTITY_TYPE.get(Photo.Id());
        if (hostile(Type)) {
            return Prefix + "scene.hostile." + Photo.Scene() % HostileScenes;
        }
        if (Photo.Young()) {
            return Prefix + "scene.young." + Photo.Scene() % YoungScenes;
        }
        return Prefix + "scene.creature." + Photo.Scene() % CreatureScenes;
    }

    public static MutableComponent description(Scp105Photo Photo) {
        return Component.translatable(sceneKey(Photo), noun(Photo));
    }
}
