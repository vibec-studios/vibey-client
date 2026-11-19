package dev.henny.vibeyclient.module.clips.encode;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import dev.henny.vibeyclient.Vibey;

import java.util.Locale;

/**
 * Keeps the encoder from competing with the game: below-normal priority on Windows (JNA, shipped with Minecraft),
 * {@code renice} elsewhere. Also reads the encoder's memory for the stats overlay. Never throws.
 */
final class Priority {
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    private static final int PROCESS_SET_INFORMATION = 0x0200;
    private static final int PROCESS_QUERY_LIMITED_INFORMATION = 0x1000;
    private static final int PROCESS_VM_READ = 0x0010;

    private Priority() {}

    static void belowNormal(Process p) {
        try {
            if (WINDOWS) {
                WinNT.HANDLE h = Kernel32.INSTANCE.OpenProcess(PROCESS_SET_INFORMATION | PROCESS_QUERY_LIMITED_INFORMATION, false, (int) p.pid());
                if (h == null) return;
                try {
                    Kernel32.INSTANCE.SetPriorityClass(h, Kernel32.BELOW_NORMAL_PRIORITY_CLASS);
                } finally {
                    Kernel32.INSTANCE.CloseHandle(h);
                }
            } else {
                new ProcessBuilder("renice", "-n", "10", "-p", String.valueOf(p.pid()))
                        .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            }
        } catch (Throwable t) {
            Vibey.LOG.debug("clips: could not lower the encoder priority: {}", t.toString());
        }
    }

    /** Resident memory of {@code p} in bytes (Windows: working set), or -1. */
    static long rss(Process p) {
        if (!WINDOWS || !p.isAlive()) return -1;
        try {
            WinNT.HANDLE h = Kernel32.INSTANCE.OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION | PROCESS_VM_READ, false, (int) p.pid());
            if (h == null) return -1;
            try {
                MemoryCounters c = new MemoryCounters();
                if (!PsapiMem.INSTANCE.GetProcessMemoryInfo(h, c, c.size())) return -1;
                return c.WorkingSetSize.longValue();
            } finally {
                Kernel32.INSTANCE.CloseHandle(h);
            }
        } catch (Throwable t) {
            return -1;
        }
    }

    /** PROCESS_MEMORY_COUNTERS (psapi.h); jna-platform does not map GetProcessMemoryInfo. */
    @Structure.FieldOrder({"cb", "PageFaultCount", "PeakWorkingSetSize", "WorkingSetSize", "QuotaPeakPagedPoolUsage",
            "QuotaPagedPoolUsage", "QuotaPeakNonPagedPoolUsage", "QuotaNonPagedPoolUsage", "PagefileUsage", "PeakPagefileUsage"})
    public static final class MemoryCounters extends Structure {
        public WinDef.DWORD cb = new WinDef.DWORD(0);
        public WinDef.DWORD PageFaultCount;
        public BaseTSD.SIZE_T PeakWorkingSetSize, WorkingSetSize, QuotaPeakPagedPoolUsage, QuotaPagedPoolUsage,
                QuotaPeakNonPagedPoolUsage, QuotaNonPagedPoolUsage, PagefileUsage, PeakPagefileUsage;

        public MemoryCounters() {
            cb = new WinDef.DWORD(size());
        }
    }

    interface PsapiMem extends StdCallLibrary {
        PsapiMem INSTANCE = Native.load("psapi", PsapiMem.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean GetProcessMemoryInfo(WinNT.HANDLE process, MemoryCounters counters, int cb);
    }
}
