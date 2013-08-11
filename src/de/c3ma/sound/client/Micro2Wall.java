package de.c3ma.sound.client;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Line.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import kjdss.KJDigitalSignalSynchronizer;
import de.c3ma.sound.api.MonoSoundProcessor;

class Micro2Wall {
    private static final int READ_BUFFER_SIZE = 1024 * 4;

    public static void main(String args[]) throws Exception {

        if (args.length < 1) {
            System.out.println("Usage: command <IP of Wall> <name of mic target>");
            System.out.println("Usage: command mics\t show all available audio sources");
            return;
        }

        String address = args[0];
        
        if (address.equals("mics") ) {
            displayMics();
            return;
        }
        
        String micname = args[1];
        
        KJDigitalSignalSynchronizer dss;
        
        Info info = getTarget(micname);
        if (info == null) {
            System.err.println("Cannot open Microphone");
            return;
        }
        
        if (!AudioSystem.isLineSupported(info)) {
            // Handle the error ...
            System.out.println("Not supported by microhone-API");
            return;
        }
        // Obtain and open the line.


//      AudioFormat format = new AudioFormat(22000, 16, 2, true, true);
        AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
        
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        MonoSoundProcessor sp = new MonoSoundProcessor(address);

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

        // -- Read from WAV file and write to DSS
        while ((wRs = line.read(wRb, 0, READ_BUFFER_SIZE)) != -1) {
            dss.writeAudioData(wRb, 0, wRs);
        }

        // -- EOF, stop monitoring source data line.
        dss.stop();

        // -- Stop and close the source data line.
        wSdl.stop();
        wSdl.close();

    }
    
    private static Info getTarget(final String name) {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info: mixerInfos){
         Mixer m = AudioSystem.getMixer(info);
         if (!info.getName().contains(name))
             continue;
         System.out.println ("found " + info.getName());
         
         
         System.out.println("=== Targets ===");
         Info[] lineInfos = m.getTargetLineInfo();
         for (Line.Info lineInfo:lineInfos){
             return lineInfo;
         }
        }
        return null;
    }

    /**
     * List available audio sources
     * @throws LineUnavailableException
     */
    private static void displayMics() throws LineUnavailableException {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info: mixerInfos){
         Mixer m = AudioSystem.getMixer(info);
         System.out.println ("== " + info.getName() + " ==");
         
         System.out.println("=== Sources ===");
         Line.Info[] lineInfos = m.getSourceLineInfo();
         for (Line.Info lineInfo:lineInfos){
          System.out.println (info.getName()+" : "+lineInfo);
          Line line = m.getLine(lineInfo);
          System.out.println("\t"+line);
         }
         
         System.out.println("=== Targets ===");
         lineInfos = m.getTargetLineInfo();
         for (Line.Info lineInfo:lineInfos){
          System.out.println (m.getLineInfo() +" : " + lineInfo);
          Line line = m.getLine(lineInfo);
          System.out.println("\t"+line.getLineInfo());
         }
        }
    }

}