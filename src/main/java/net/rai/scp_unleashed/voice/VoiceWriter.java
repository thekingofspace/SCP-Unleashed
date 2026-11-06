package net.rai.scp_unleashed.voice;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class VoiceWriter {
    public static final long FlushSeconds = 10L;
    private static final Logger Log = LogUtils.getLogger();
    private static final ExecutorService Worker = Executors.newSingleThreadExecutor(Task -> {
        Thread Made = new Thread(Task, "scp_unleashed-voice-writer");
        Made.setDaemon(true);
        return Made;
    });

    private VoiceWriter() {
    }

    public interface Job {
        void run() throws Exception;
    }

    public static void submit(String Label, Job Work) {
        Worker.execute(() -> {
            try {
                Work.run();
            } catch (Exception Problem) {
                Log.warn("Voice archive could not {}: {}", Label, Problem.toString());
            }
        });
    }

    public static boolean flush() {
        Future<?> Barrier = Worker.submit(() -> {
        });
        try {
            Barrier.get(FlushSeconds, TimeUnit.SECONDS);
            return true;
        } catch (Exception Problem) {
            Log.warn("Voice archive writes did not finish in time: {}", Problem.toString());
            return false;
        }
    }
}
