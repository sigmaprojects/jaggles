package jaggles.util;

public class TimeKeeper {
    private final long createdMillis = System.currentTimeMillis();

    public int getAgeInSeconds() {
        long nowMillis = System.currentTimeMillis();
        return (int)((nowMillis - this.createdMillis) / 1000);
    }
}
