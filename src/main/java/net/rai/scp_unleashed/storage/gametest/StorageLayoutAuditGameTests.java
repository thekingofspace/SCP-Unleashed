package net.rai.scp_unleashed.storage.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageRegistry;
import net.rai.scp_unleashed.storage.StorageTags;
import net.rai.scp_unleashed.storage.TallStorageContainerBlock;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class StorageLayoutAuditGameTests {
    public static final String Arena = "scp444_arena";
    public static final String AuditClass = "net.rai.scp_unleashed.storage.client.StorageLayoutAudit";
    public static final String LayoutsReference = "net/rai/scp_unleashed/storage/client/StorageLayouts";
    public static final String CorePackage = "net/rai/scp_unleashed/storage/client/";
    public static final String Examples = "net.rai.scp_unleashed.storage.client.StorageLayoutExamples";
    public static final String ReportName = "locker_audit.txt";
    public static final String DumpName = "locker_audit.json";
    private static final BlockPos SmallSpot = new BlockPos(6, 2, 8);
    private static final BlockPos TallSpot = new BlockPos(10, 2, 8);

    private StorageLayoutAuditGameTests() {
    }

    public static List<String> registrars() throws IOException {
        IModFile Source = ModList.get().getModFileById(Scp_unleashed.ModId).getFile();
        Path Root = Source.findResource("net", "rai", "scp_unleashed");
        byte[] Wanted = LayoutsReference.getBytes(StandardCharsets.UTF_8);
        List<String> Found = new ArrayList<>();
        Path Base = Root.getParent().getParent().getParent();
        try (Stream<Path> Files0 = Files.walk(Root)) {
            for (Path Each : Files0.filter(Candidate -> Candidate.toString().endsWith(".class")).toList()) {
                String Relative = Base.relativize(Each).toString().replace('\\', '/');
                if (Relative.startsWith(CorePackage) || Relative.contains("$") || Relative.contains("/gametest/")) {
                    continue;
                }
                if (contains(Files.readAllBytes(Each), Wanted)) {
                    String Name = Relative.substring(0, Relative.length() - ".class".length()).replace('/', '.');
                    if (!Name.equals(Examples)) {
                        Found.add(Name);
                    }
                }
            }
        }
        Found.sort(String::compareTo);
        return Found;
    }

    private static boolean contains(byte[] Haystack, byte[] Needle) {
        for (int Start = 0; Start <= Haystack.length - Needle.length; Start++) {
            int Index = 0;
            while (Index < Needle.length && Haystack[Start + Index] == Needle[Index]) {
                Index++;
            }
            if (Index == Needle.length) {
                return true;
            }
        }
        return false;
    }

    private static StorageContainerBlockEntity small(GameTestHelper Helper) {
        Helper.setBlock(SmallSpot, StorageRegistry.SmallContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Direction.NORTH));
        return Helper.getBlockEntity(SmallSpot);
    }

    private static StorageContainerBlockEntity tall(GameTestHelper Helper) {
        BlockState Lower = StorageRegistry.TallContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Direction.NORTH);
        Helper.setBlock(TallSpot, Lower);
        Helper.setBlock(TallSpot.above(), Lower.setValue(TallStorageContainerBlock.Half, DoubleBlockHalf.UPPER));
        return Helper.getBlockEntity(TallSpot);
    }

    @SuppressWarnings("unchecked")
    @GameTest(template = Arena, timeoutTicks = 60, batch = "storage_layout_audit")
    public static void everyStoredAnomalyIsVisible(GameTestHelper Helper) {
        StorageContainerBlockEntity Small = small(Helper);
        StorageContainerBlockEntity Tall = tall(Helper);
        Helper.assertTrue(Tall.isTall() && !Small.isTall(), "the audit lockers did not form");
        List<ItemStack> Stacks = new ArrayList<>();
        for (Holder<Item> Entry : BuiltInRegistries.ITEM.getTagOrEmpty(StorageTags.Storable)) {
            Stacks.add(new ItemStack(Entry.value()));
        }
        Helper.assertTrue(Stacks.size() > 10, "the storable tag looks empty: " + Stacks.size());
        List<String> Lines;
        List<String> Dump = new ArrayList<>();
        List<String> Registrars;
        try {
            Registrars = registrars();
            ClientCodeLoader Loader = new ClientCodeLoader(StorageLayoutAuditGameTests.class.getClassLoader());
            Class<?> Audit = Loader.loadClass(AuditClass);
            Method Run = Audit.getMethod("run", List.class, Level.class, StorageContainerBlockEntity.class, StorageContainerBlockEntity.class, List.class, Function.class, List.class);
            Function<String, InputStream> Resources = Loader::getResourceAsStream;
            Lines = (List<String>) Run.invoke(null, Stacks, Helper.getLevel(), Small, Tall, Registrars, Resources, Dump);
        } catch (Throwable Problem) {
            Throwable Root = Problem;
            while (Root.getCause() != null && Root.getCause() != Root) {
                Root = Root.getCause();
            }
            Helper.fail("the locker audit could not run: " + Root);
            return;
        }
        List<String> Report = new ArrayList<>();
        Report.add("registrars " + Registrars.size() + ": " + String.join(", ", Registrars));
        Report.add("stored items " + Stacks.size());
        Report.addAll(Lines);
        try {
            Files.write(FMLPaths.GAMEDIR.get().resolve(ReportName), Report, StandardCharsets.UTF_8);
            Files.write(FMLPaths.GAMEDIR.get().resolve(DumpName), List.of("[" + String.join(",\n", Dump) + "]"), StandardCharsets.UTF_8);
        } catch (IOException Problem) {
            Helper.fail("could not write the locker audit report: " + Problem.getMessage());
            return;
        }
        List<String> Problems = Report.stream().filter(Line -> Line.startsWith("PROBLEM")).toList();
        Helper.assertTrue(Problems.isEmpty(), Problems.size() + " stored anomalies would be invisible in a locker: " + String.join(" | ", Problems));
        Helper.assertTrue(Lines.stream().anyMatch(Line -> Line.startsWith("ok translucent")), "the closed glass door check did not run: " + Lines.stream()
                .filter(Line -> Line.startsWith("note")).toList());
        Helper.succeed();
    }
}
