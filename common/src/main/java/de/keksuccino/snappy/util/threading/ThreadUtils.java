package de.keksuccino.snappy.util.threading;

public class ThreadUtils {

    public static void sleep(long millis) {
        try {
             Thread.sleep(millis);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
