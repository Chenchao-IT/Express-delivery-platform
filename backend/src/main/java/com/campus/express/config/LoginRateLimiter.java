package com.campus.express.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档 4.2：登录尝试限流 LOGIN_ATTEMPT - 内存实现，多级时间窗口 3/1min + 10/10min
 */
@Component
public class LoginRateLimiter implements LoginRateLimiterService {

    private static final int WINDOW_60_SECONDS = 60;
    private static final int WINDOW_600_SECONDS = 600;
    private static final int MAX_60 = 3;
    private static final int MAX_600 = 10;

    private final Map<String, TwoWindows> windows = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> e.getValue().expires600 < now);
        TwoWindows w = windows.compute(key, (k, v) -> {
            if (v == null) {
                return new TwoWindows(1, now + WINDOW_60_SECONDS * 1000L, 1, now + WINDOW_600_SECONDS * 1000L);
            }
            boolean valid60 = v.expires60 > now;
            boolean valid600 = v.expires600 > now;
            int c60 = valid60 ? v.count60 + 1 : 1;
            long exp60 = valid60 ? v.expires60 : now + WINDOW_60_SECONDS * 1000L;
            int c600 = valid600 ? v.count600 + 1 : 1;
            long exp600 = valid600 ? v.expires600 : now + WINDOW_600_SECONDS * 1000L;
            return new TwoWindows(c60, exp60, c600, exp600);
        });
        return w.count60 <= MAX_60 && w.count600 <= MAX_600;
    }

    @Override
    public int getRemainingWaitSeconds(String key) {
        TwoWindows w = windows.get(key);
        if (w == null) return 0;
        long now = System.currentTimeMillis();
        long remain60 = w.count60 > MAX_60 && w.expires60 > now ? (w.expires60 - now) / 1000 : 0;
        long remain600 = w.count600 > MAX_600 && w.expires600 > now ? (w.expires600 - now) / 1000 : 0;
        return Math.max(0, (int) Math.max(remain60, remain600));
    }

    @Override
    public void clearAll() {
        windows.clear();
    }

    private static class TwoWindows {
        final int count60;
        final long expires60;
        final int count600;
        final long expires600;

        TwoWindows(int count60, long expires60, int count600, long expires600) {
            this.count60 = count60;
            this.expires60 = expires60;
            this.count600 = count600;
            this.expires600 = expires600;
        }
    }
}
