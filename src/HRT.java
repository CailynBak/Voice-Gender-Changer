import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;

/**
 * This is the part that changes the gender
 */
public class HRT extends Thread {
    private byte[] data;
    private Direction direction;
    private boolean finishedProcessing;
    private boolean shouldDie;
    private long startTime;
    private Object lock = new Object();

    /**
     * Specifies how the gender of the voice should be changed.
     * FEMINIZE feminizes the voice, ANDROGYNIZE androgynizes the voice, and MASCULINIZE masculinizes the voice.
     */
    public enum Direction {
        FEMINIZE,
        ANDROGYNIZE,
        MASCULINIZE
    }

    /**
     * Constructs a new HRT object.
     * @param data The audio data to change the gender of.
     * @param direction The direction to move the voice.
     */
    public HRT(byte[] data, Direction direction) {
        super();

        this.data = Arrays.copyOf(data, data.length);
        this.direction = direction;

        finishedProcessing = false;
        shouldDie = false;
    }

    /**
     * Give the thread new data to process.
     * @param data The data to process
     * @throws Exception When the thread is still processing data
     */
    public synchronized void setData(byte[] data) throws Exception {
        if (finishedProcessing) {
            this.data = data;
            startTime = System.currentTimeMillis();
            finishedProcessing = false;
        } else {
            throw new Exception("Thread cannot accept new data before previous data has been processed");
        }
    }

    /**
     * Whether or not this thread has finished processing the last set of data given to it.
     * @return
     */
    public synchronized boolean isDone() {
        return finishedProcessing;
    }

    /**
     * Tells the thread that it should die, once it has finished processing its data.
     */
    public synchronized void kill() {
        shouldDie = true;
    }

    /**
     * Feminizes the data
     */
    private void feminize() {
        double[] collectedData = new double[data.length];   // This needs to be the same size because the size is halved and then needs to be doubled later on

        // convert into correct format for fourier transforms
        for (int i=0, j=0; i < data.length; i += 2, ++j) {
            collectedData[j] = (short)((data[i] << 8) | (data[i+1] & 0xFF));
        }

        DoubleFFT_1D fft = new DoubleFFT_1D(collectedData.length / 2);
        fft.realForward(collectedData);



        fft.complexInverse(collectedData, true);

        for (int i=0, j=0; i < collectedData.length; i += 2, j+= 2) {
            data[j] = (byte) (((int)collectedData[i]) >> 8);
            data[j+1] = (byte) (((int)collectedData[i]) & 0xFF);
        }
    }

    /**
     * Androgynizes the data
     */
    private void androgynize() {

    }

    /**
     * Masculinizes the data.
     */
    private void masculinize() {

    }

    /**
     * Starts the processing loop.
     */
    @Override
    public void run() {
        boolean continueLoop;
        boolean processed;
        do {
            synchronized (lock) {
                processed = finishedProcessing;
                continueLoop = shouldDie;
            }

            if (!processed) {
                switch (direction) {
                    case FEMINIZE:
                        feminize();
                        break;
                    case ANDROGYNIZE:
                        androgynize();
                        break;
                    case MASCULINIZE:
                        masculinize();
                        break;
                }

                // Write data to shared buffer
                Main.sharedBuffer.put(startTime, data);

                synchronized (lock) {
                    finishedProcessing = true;
                }
            } else {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    // Don't do anything; just continue onto the next iteration of the loop
                }
            }
        } while (continueLoop);
    }
}
