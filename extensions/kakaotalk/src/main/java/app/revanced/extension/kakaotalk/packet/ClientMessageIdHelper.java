package app.revanced.extension.kakaotalk.packet;

import java.util.Calendar;

public class ClientMessageIdHelper {

    public static final ClientMessageIdHelper INSTANCE = new ClientMessageIdHelper();

    private static final byte DEVICE_HASH_32BIT = (byte) ("e2dc694aee2540c2de6b4a8be2d7718846a0dfb9".hashCode() % 100);
    private static final int STEP = 100;
    private static final long MAX_ID = 2147483647L;
    private static final long MODULO = 2147483547L;

    private static volatile long lastId;
    private static volatile long lastGenId;
    private static volatile long lastTimeMillis;

    static {
        long currentTime = System.currentTimeMillis();
        lastId = INSTANCE.calculateTime(currentTime);
        lastGenId = lastId;
    }

    private ClientMessageIdHelper() {}

    public long calculateTime(long timeMillis) {
        return (((timeMillis % MODULO) / STEP) * STEP) + DEVICE_HASH_32BIT;
    }

    public synchronized long generateId(long timeMillis) {
        long calculatedId = calculateTime(timeMillis);
        long newId;

        if (calculatedId > lastId || calculatedId < lastGenId) {
            newId = calculatedId;
        } else {
            newId = lastId + STEP;
            if (newId > MAX_ID) {
                newId = calculateTime(newId);
            }
        }

        lastId = newId;
        lastGenId = calculatedId;
        return lastId;
    }

    public static synchronized long getMonotonicTime() {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        
        if (currentTime > lastTimeMillis) {
            lastTimeMillis = currentTime;
        } else {
            lastTimeMillis++;
        }
        
        return lastTimeMillis;
    }
}
