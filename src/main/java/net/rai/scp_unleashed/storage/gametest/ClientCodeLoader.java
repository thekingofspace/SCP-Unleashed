package net.rai.scp_unleashed.storage.gametest;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ClientCodeLoader extends ClassLoader {
    public static final String ModPrefix = "net.rai.scp_unleashed.";
    public static final List<String> ClientPrefixes = List.of("net.minecraft.client.", "com.mojang.blaze3d.", "net.neoforged.neoforge.client.");
    public static final List<String> Sources = List.of("scp_unleashed", "minecraft", "neoforge");
    private final ClassLoader Game;

    public ClientCodeLoader(ClassLoader Game) {
        super("scp_unleashed_locker_audit", null);
        this.Game = Game;
    }

    public static boolean ownsFirst(String Name) {
        if (Name.startsWith(ModPrefix)) {
            return Name.contains(".client.");
        }
        for (String Prefix : ClientPrefixes) {
            if (Name.startsWith(Prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Class<?> loadClass(String Name, boolean Resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(Name)) {
            Class<?> Found = findLoadedClass(Name);
            if (Found == null) {
                if (ownsFirst(Name)) {
                    Found = define(Name);
                } else {
                    try {
                        Found = Game.loadClass(Name);
                    } catch (Throwable Refused) {
                        Found = define(Name);
                    }
                }
            }
            if (Resolve) {
                resolveClass(Found);
            }
            return Found;
        }
    }

    private Class<?> define(String Name) throws ClassNotFoundException {
        byte[] Bytes = bytes(Name.replace('.', '/') + ".class");
        if (Bytes == null) {
            throw new ClassNotFoundException(Name);
        }
        return defineClass(Name, Bytes, 0, Bytes.length);
    }

    public byte[] bytes(String Path0) {
        for (String Source : Sources) {
            IModFileInfo Info = ModList.get().getModFileById(Source);
            if (Info == null) {
                continue;
            }
            Path Found = Info.getFile().findResource(Path0.split("/"));
            if (Files.isRegularFile(Found)) {
                try {
                    return Files.readAllBytes(Found);
                } catch (IOException Problem) {
                    return null;
                }
            }
        }
        try (InputStream Stream = Game.getResourceAsStream(Path0)) {
            return Stream == null ? null : Stream.readAllBytes();
        } catch (IOException Problem) {
            return null;
        }
    }

    @Override
    public InputStream getResourceAsStream(String Name) {
        byte[] Found = bytes(Name);
        return Found == null ? null : new ByteArrayInputStream(Found);
    }
}
