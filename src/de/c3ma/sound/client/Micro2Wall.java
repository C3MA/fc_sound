package de.c3ma.sound.client;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import kjdss.KJDigitalSignalSynchronizer;
import de.c3ma.sound.api.MonoSoundProcessor;
import de.c3ma.sound.api.SoundProcessor_3Part;

class Micro2Wall {
    private static final int READ_BUFFER_SIZE = 1024 * 4;

    public static void main(String args[]) throws Exception {

        if (args.length != 1) {
            System.out.println("Usage: <IP of Wall>");
            return;
        }

        String adress = args[0];
        KJDigitalSignalSynchronizer dss;

//        AudioFormat format = new AudioFormat(22000, 16, 2, true, true);
        AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
        TargetDataLine line = null;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); // format
                                                                              // is
                                                                              // an
                                                                              // AudioFormat
                                                                              // object
        if (!AudioSystem.isLineSupported(info)) {
            // Handle the error ...
        }
        // Obtain and open the line.

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        MonoSoundProcessor sp = new MonoSoundProcessor(adress);

        dss = new KJDigitalSignalSynchronizer();

        dss.add(sp);

        SourceDataLine wSdl = AudioSystem.getSourceDataLine(line.getFormat());

        // -- Open the source data line and start it.
        wSdl.open();
        wSdl.start();

        // -- Have the DSS monitor the source data line.
        dss.start(wSdl);

        // -- Allocate a read buffer.
        byte[] wRb = new byte[READ_BUFFER_SIZE];
        int wRs = 0;

        // -- Read from WAV file and write to DSS (and the monitored source data
        // line)
        while ((wRs = line.read(wRb, 0, READ_BUFFER_SIZE)) != -1) {
            dss.writeAudioData(wRb, 0, wRs);
        }

        // -- EOF, stop monitoring source data line.
        dss.stop();

        // -- Stop and close the source data line.
        wSdl.stop();
        wSdl.close();

    }

}