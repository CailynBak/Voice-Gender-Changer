import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * This is the main thread. It controls the shared buffer and the buffer coordinator.
 */
public class Main {
    /**
     * The audio format that the input and output devices are expected to use.
     */
    public static final AudioFormat format = new AudioFormat(48000f, 16, 1, true, false);;

    public static Mixer selectMixer(Line.Info format, String prompt) {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        ArrayList<Mixer.Info> validMixers = new ArrayList<>();

        for (Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);

            if (mixer.isLineSupported(format)) {
                validMixers.add(mixerInfo);
            }
        }

        for (int i=0; i < validMixers.size(); ++i) {
            System.out.print(i);
            System.out.print('\t');
            System.out.println(validMixers.get(i).getName());
        }

        Scanner sc = new Scanner(System.in);

        int choice = 0;
        do {
            System.out.print(prompt);
            choice = sc.nextInt();

            if (choice < 0 || choice >= validMixers.size()) {
                System.out.println("You must choose one of the options listed!");
            } else {
                break;
            }
        } while (true);

        sc.close();

        System.out.print("Using device ");
        System.out.println(validMixers.get(choice).getName());

        return AudioSystem.getMixer(validMixers.get(choice));
    }

    /**
     * The shared buffer. It is sorted by its keys, which are expected to be the time the data was received for
     * processing at.
     */
    public static ConcurrentSkipListMap<Long, byte[]> sharedBuffer = new ConcurrentSkipListMap<>();

    public static void main(String[] args) {
        Line.Info inLineInfo;
        Line.Info outLineInfo;
        Mixer inMixer;
        Mixer outMixer;
        TargetDataLine inLine = null;
        SourceDataLine outLine = null;

        HRT.Direction direction = null;

        inLineInfo = new DataLine.Info(TargetDataLine.class, format);
        outLineInfo = new DataLine.Info(SourceDataLine.class, format);

        try {
            inMixer = selectMixer(inLineInfo, "Select the input device: ");
            outMixer = selectMixer(outLineInfo, "Select the audio output device: ");

            inLine = (TargetDataLine) inMixer.getLine(inLineInfo);
            outLine = (SourceDataLine) outMixer.getLine(outLineInfo);

            inLine.open(format);
            outLine.open(format);
        } catch (LineUnavailableException ex) {
            System.err.println("Failed to acquire a line in the proper format");
            System.exit(-1);
        }

        System.out.println("What would you like to do to your voice?");
        System.out.println("1\tFEMINIZE\n2\tANDROGYNIZE\n3\tMASCULINIZE");

        Scanner sc = new Scanner(System.in);
        int choice;

        do {
            choice = sc.nextInt();

            if (choice > 0 && choice < 4) {
                break;
            } else {
                System.out.println("You must pick one of the listed options!");
            }
        } while (true);

        switch (choice) {
            case 1:
                System.out.println("Feminizing your voice!");
                direction = HRT.Direction.FEMINIZE;
                break;
            case 2:
                System.out.println("Androgynizing your voice!");
                direction = HRT.Direction.ANDROGYNIZE;
                break;
            case 3:
                System.out.println("Masculinizing your voice!");
                direction = HRT.Direction.MASCULINIZE;
                break;
        }

        // buffer coordinator
        byte[] data = new byte[inLine.getBufferSize()];

        ArrayList<HRT> changerThreads = new ArrayList<>();

        while (true) {
            inLine.start();
            outLine.start();

            inLine.read(data, 0, data.length);

            // Manage threads
            if (changerThreads.size() < 3) {
                HRT newChanger = new HRT(data, direction);
                changerThreads.add(newChanger);
            } else if (changerThreads.size() > 3) {
                for (HRT thread : changerThreads) {
                    if (changerThreads.size() <= 3) {
                        break;
                    }

                    if (thread.isDone()) {
                        thread.kill();
                        changerThreads.remove(thread);
                    }
                }
            }

            for (HRT thread : changerThreads) {
                if (thread.isDone()) {
                    try {
                        thread.setData(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    HRT newChanger = new HRT(data, direction);
                    newChanger.start();
                    changerThreads.add(newChanger);
                }
            }

            outLine.write(data, 0, data.length);
        }
    }
}
