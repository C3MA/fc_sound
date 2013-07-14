import java.awt.Color;
import java.io.IOException;

import javax.sound.sampled.SourceDataLine;

import de.c3ma.fullcircle.RawClient;
import de.c3ma.proto.fctypes.Frame;
import de.c3ma.proto.fctypes.FullcircleSerialize;
import de.c3ma.proto.fctypes.InfoAnswer;
import de.c3ma.proto.fctypes.Meta;
import de.c3ma.proto.fctypes.Pixel;
import de.c3ma.proto.fctypes.Start;
import de.c3ma.proto.fctypes.Timeout;
import kjdss.KJDigitalSignalProcessor;
import kjdss.KJDigitalSignalSynchronizer;
import kjdss.KJDigitalSignalSynchronizer.Context;


public class SoundProcessor_3Part implements KJDigitalSignalProcessor {

	public static final float DEFAULT_VU_METER_DECAY   = 0.02f;
	
	protected float vuDecay = DEFAULT_VU_METER_DECAY;
	float[] oldVolume;
	
	private int width = 0;
	private int height = 0;
	private RawClient rc;
	
	public SoundProcessor_3Part(String addr) throws Exception{
		rc = new RawClient(addr);
        rc.requestInformation();
        
        while (true) {
            Thread.sleep(10);
	        FullcircleSerialize got = rc.readNetwork();
	        if (got != null) {
	            System.out.println(got);
	            if (got instanceof InfoAnswer) {
	                /* Extract the expected resolution and use these values for the request */
	            	InfoAnswer ia = (InfoAnswer) got;
	
	            	height = ia.getHeight();
	            	width = ia.getWidth();
	            	
	                /* when we got the resolution of the map, in this example we now want to start to send something */
	                rc.requestStart("java", 1, ia.getMeta()); 
	            } else if (got instanceof Start) {
	                System.out.println("We have a GOOO send some data!");
	                break;
	            } else if (got instanceof Timeout) {
	                System.out.println("Too slow, so we close the session");
	                rc.close();
	                System.exit(1);
	            }
	        }
        }
        
	}
	
	@Override
	public void initialize(int arg0, SourceDataLine arg1) {
		oldVolume = new float[ arg1.getFormat().getChannels() ];
	}

	@Override
	public void process(KJDigitalSignalSynchronizer.Context arg0) {
		
		float[][] pChannels = arg0.getDataNormalized();
		
		float pFrrh = arg0.getFrameRatioHint();
		
		float[] wVolume = new float[ pChannels.length ];
		
		float wSadfrr = ( vuDecay * pFrrh );

		for( int a = 0; a < pChannels.length; a++ ) {
			
			System.out.println("Channel "+ a +":"+ pChannels[a].length);
			
			for( int b = 0; b < pChannels[ a ].length; b++ ) {
				
				float wAmp = Math.abs( pChannels[ a ][ b ] );
				
				if ( wAmp > wVolume[ a ] ) {
					wVolume[ a ] = wAmp;
				}
				
			}
			
			if ( wVolume[ a ] >= ( oldVolume[ a ] - wSadfrr ) ) {
				oldVolume[ a ] = wVolume[ a ];
			} else {

				oldVolume[ a ] -= wSadfrr;
				
				if ( oldVolume[ a ] < 0 ) {
					oldVolume[ a ] = 0;
				}
				
			}
			
//			System.out.println(a +" - "+  oldVolume[ a ] * ((float) (height*255) - 32)  );
			
		}
		
		Frame f = new Frame();
				
		int a = (int) (oldVolume[ 0 ] * ((float) (height*255) - 32));
		int b = (int) (oldVolume[ 1 ] * ((float) (height*255) - 32));
		
		int a_rest = a % 255;
		int a_teil = (int) Math.ceil(a / 255);
		int i;
		
//		System.out.println(a + " - "+ a_teil +" / "+ a_rest);
		for (i=0; i < a_teil; i++) {
			f.add(new Pixel(2, i+3, Color.BLUE));
		}
		f.add(new Pixel(2, i+3, new Color(a_rest,0,0)));
		
		int b_rest = b % 255;
		int b_teil = (int) Math.ceil(a / 255);
		
//		System.out.println(b + " - "+ b_teil +" / "+ b_rest);
		for (i=0; i < b_teil; i++) {
			f.add(new Pixel(2+2, i+3, Color.BLUE));
		}
		f.add(new Pixel(2+2, i+3, new Color(b_rest,0,0)));
		
        try {
			rc.sendFrame(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
