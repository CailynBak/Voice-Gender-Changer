import javax.sound.sampled.SourceDataLine;
import java.text.ParseException;

/**
 * Writes the data
 */
public class Writer implements Runnable {
    /**
     * The time delay for writing
     */
    public static final long delay = 5;

    /**
     * The line to write to
     */
    private SourceDataLine line;

    /**
     * Constructs a new Writer.
     * @param line The line to write to.
     */
    public Writer(SourceDataLine line) {
        this.line = line;
    }

    /**
     * The writing loop. It checks for new data, and idles when no data is available.
     */
    // TODO: fix potential thread unsafety
    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();

        if ((currentTime - Main.sharedBuffer.firstKey()) < delay) {

        } else if ((currentTime - Main.sharedBuffer.firstKey()) > delay) {
            Main.sharedBuffer.remove(Main.sharedBuffer.firstKey());
        } else {
            byte[] data = Main.sharedBuffer.get(Main.sharedBuffer.firstKey());
            line.write(data, 0, data.length);
        }

    }
}
