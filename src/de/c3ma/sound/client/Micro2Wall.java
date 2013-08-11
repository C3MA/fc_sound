package de.c3ma.sound.client;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import kjdss.KJDigitalSignalSynchronizer;
import de.c3ma.sound.api.MonoSoundProcessor;

class Micro2Wall {
    private static final int READ_BUFFER_SIZE = 1024 * 4;

    public static void main(String args[]) throws Exception {

        if (args.length != 1) {
            System.out.println("Usage: command <IP of Wall>");
            System.out.println("Usage: command mics\t show all available audio sources");
            return;
        }

        String address = args[0];
        
        if (address.equals("mics") ) {
            displayMics();
            return;
        }

        MonoSoundProcessor sp = new MonoSoundProcessor(address);
        KJDigitalSignalSynchronizer dss = new KJDigitalSignalSynchronizer();
        dss.add(sp);
        
//      AudioFormat format = new AudioFormat(22000, 16, 2, true, true);
        AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
        
        TargetDataLine line = getTargetDataLineForRecord(format);
        final int frameSizeInBytes = format.getFrameSize();
        final int bufferLengthInFrames = line.getBufferSize() / 8;
        final int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
        final byte[] data = new byte[bufferLengthInBytes];
        int numBytesRead;
        line.start();

        SourceDataLine wSdl = AudioSystem.getSourceDataLine(line.getFormat());
        // -- Have the DSS monitor the source data line.
        dss.start(wSdl);
        
        while (line != null) {
            if ((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
                break;
            }
            dss.writeAudioData(data, 0, numBytesRead);
        }
        // we reached the end of the stream. stop and close the line.
        line.stop();
        line.close();
        line = null;
        
        // -- EOF, stop monitoring source data line.
        dss.stop();

        // -- Stop and close the source data line.
        wSdl.stop();
        wSdl.close();

    }
    
    private static TargetDataLine getTargetDataLineForRecord(AudioFormat format) {  
        TargetDataLine line;  
        final DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);  
        if (!AudioSystem.isLineSupported(info)) {  
          return null;  
        }  
        // get and open the target data line for capture.  
        try {  
          line = (TargetDataLine) AudioSystem.getLine(info);  
          line.open(format, line.getBufferSize());  
        } catch (final Exception ex) {  
          return null;  
        }  
        return line;  
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