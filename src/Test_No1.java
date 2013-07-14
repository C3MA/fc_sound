import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import kjdss.KJDigitalSignalSynchronizer;

class Test_No1 {
	private static final int READ_BUFFER_SIZE = 1024 * 4;

	public static void main(String args[]) throws Exception {
		String filename = "/home/bene/Downloads/01_sad_robot.mp3";
		String adress = "10.23.42.190";
		KJDigitalSignalSynchronizer dss;
		
		   File file = new File(filename);
		    AudioInputStream in= AudioSystem.getAudioInputStream(file);
		    AudioInputStream wAs = null;
		    AudioFormat baseFormat = in.getFormat();

		    AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);
		    wAs = AudioSystem.getAudioInputStream(decodedFormat, in);
		    
//		    SoundProcessor sp = new SoundProcessor(adress);
		    SoundProcessor_3Part sp = new SoundProcessor_3Part(adress);
		    
		    dss = new KJDigitalSignalSynchronizer();
			
			dss.add( sp );
		    
			SourceDataLine wSdl = AudioSystem.getSourceDataLine( wAs.getFormat() );

        	// -- Open the source data line and start it.
        	wSdl.open();
        	wSdl.start();

        	// -- Have the DSS monitor the source data line.
            dss.start( wSdl );
        	
            // -- Allocate a read buffer.
        	byte[] wRb = new byte[ READ_BUFFER_SIZE ];
        	int    wRs = 0;
        	
        	// -- Read from WAV file and write to DSS (and the monitored source data line)
        	while( ( wRs = wAs.read( wRb ) ) != -1 ) {
        		dss.writeAudioData( wRb, 0, wRs );
        	}

        	// -- EOF, stop monitoring source data line.
        	dss.stop();
        	
        	// -- Stop and close the source data line.
        	wSdl.stop();
        	wSdl.close();
		
	}

}