package de.c3ma.sound.api;
import java.io.IOException;

import javax.sound.sampled.SourceDataLine;

import kjdss.KJDigitalSignalProcessor;
import kjdss.KJDigitalSignalSynchronizer;
import de.c3ma.animation.RainbowEllipse;
import de.c3ma.fullcircle.RawClient;
import de.c3ma.proto.fctypes.Frame;
import de.c3ma.proto.fctypes.FullcircleSerialize;
import de.c3ma.proto.fctypes.InfoAnswer;
import de.c3ma.proto.fctypes.Pixel;
import de.c3ma.proto.fctypes.Start;
import de.c3ma.proto.fctypes.Timeout;
import de.c3ma.types.SimpleColor;


public class MonoSoundProcessor implements KJDigitalSignalProcessor {

	public static final float DEFAULT_VU_METER_DECAY   = 0.02f;
	
	protected float vuDecay = DEFAULT_VU_METER_DECAY;
	float[] oldVolume;
	
	private int width = 0;
	private int height = 0;
	private RawClient rc;
	
	private int splitPart = 0;

    private int a_max;
	
	public MonoSoundProcessor(String addr) throws Exception{
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
	
	            	height = ia.getHeight()-3;
	            	width = ia.getWidth();
	            	
	            	splitPart = width - 2; 
	            	oldVolume = new float[splitPart + 1];
	            	
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
		
	}

	@Override
	public void process(KJDigitalSignalSynchronizer.Context arg0) {
		
	    if (splitPart == 0)
	        return;
	    
		float[][] pChannels = arg0.getDataNormalized();
//		System.out.println("Channels : " + pChannels.length);
		
		float pFrrh = arg0.getFrameRatioHint();
		
		float[] wVolume = new float[splitPart + 1];
		
		float wSadfrr = ( vuDecay * pFrrh );
		
		for( int a = 0; a < pChannels.length; a++ ) {
			
			int parts = (int) Math.floor(pChannels[a].length / splitPart);
			int tmpPart = parts;
			int actPart = 0;
			
			for( int b = 0; b < pChannels[ a ].length; b++ ) {
				
				float wAmp = Math.abs( pChannels[ a ][ b ] );
				
				if ( wAmp > wVolume[actPart] ) {
					wVolume[actPart] = wAmp;
				}
				if(b > tmpPart) {
					tmpPart += parts;
					actPart ++;
				}
			}
			
			for (int i=0; i<splitPart; i++){
	            System.out.println( i + "\t" + wSadfrr );
				if ( wVolume[i] >= ( oldVolume[i] - wSadfrr ) ) {
					oldVolume[i] = wVolume[i];
				} else {
	
					oldVolume[i] -= wSadfrr;
					
					if ( oldVolume[i] < 0 ) {
						oldVolume[i] = 0;
					}
					
				}
			}
			
			
		}
		
		Frame f = new Frame();
		int i;
		
		for(int c=0; c<splitPart; c++){
		
			int a = (int) (oldVolume[c] * ((float) (height*255) - 32));
			int a_rest = a % 255;
			int a_teil = (int) Math.ceil(a / 255);

            a_max = Math.max(a_teil + 1, a_max);
			for (i=0; i < a_teil; i++) {
			    SimpleColor color = RainbowEllipse.mapRainbowColor(i, 0, a_max);
				f.add(new Pixel(c, height-i, color));
			}
			SimpleColor color = RainbowEllipse.mapRainbowColor(a_max, 0, a_max);
			f.add(new Pixel(c, height-i, new SimpleColor(color.getRed() * a_rest / 255,
			        color.getGreen() * a_rest / 255,
			        color.getBlue() * a_rest / 255)));
		}
		
        try {
			rc.sendFrame(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
