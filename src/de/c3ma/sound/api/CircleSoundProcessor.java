    package de.c3ma.sound.api;

import java.awt.Color;
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

public class CircleSoundProcessor implements KJDigitalSignalProcessor {

    public static final float DEFAULT_VU_METER_DECAY = 0.02f;

    protected float vuDecay = DEFAULT_VU_METER_DECAY;
    
    private static int TIMER_RESET = 6;
    
    float[] oldVolume;

    private int width = 0;
    private int height = 0;
    private RawClient rc;

    private int xmittel;

    private int ymittel;

    private int r;

    private int count = 0;
    
    public CircleSoundProcessor(String addr) throws Exception {
        rc = new RawClient(addr);
        rc.requestInformation();

        
        while (true) {
            Thread.sleep(10);
            FullcircleSerialize got = rc.readNetwork();
            if (got != null) {
                System.out.println(got);
                if (got instanceof InfoAnswer) {
                    /*
                     * Extract the expected resolution and use these values for
                     * the request
                     */
                    InfoAnswer ia = (InfoAnswer) got;

                    height = ia.getHeight();
                    width = ia.getWidth();
                    
                    xmittel = (int) Math.ceil(width / 2);
                    ymittel = (int) Math.ceil(height / 2);
                    r = Math.min(xmittel, ymittel)  - 1;

                    /*
                     * when we got the resolution of the map, in this example we
                     * now want to start to send something
                     */
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
        oldVolume = new float[arg1.getFormat().getChannels()];
    }

    @Override
    public void process(KJDigitalSignalSynchronizer.Context arg0) {

        float[][] pChannels = arg0.getDataNormalized();

        float pFrrh = arg0.getFrameRatioHint();

        float[] wVolume = new float[pChannels.length];

        float wSadfrr = (vuDecay * pFrrh);

        for (int a = 0; a < pChannels.length; a++) {

//            System.out.println("Channel " + a + ":" + pChannels[a].length);

            for (int b = 0; b < pChannels[a].length; b++) {

                float wAmp = Math.abs(pChannels[a][b]);

                if (wAmp > wVolume[a]) {
                    wVolume[a] = wAmp;
                }

            }

            if (wVolume[a] >= (oldVolume[a] - wSadfrr)) {
                oldVolume[a] = wVolume[a];
            } else {
                oldVolume[a] -= wSadfrr;

                if (oldVolume[a] < 0) {
                    oldVolume[a] = 0;
                }

            }

            // System.out.println(a +" - "+ oldVolume[ a ] * ((float)
            // (height*255) - 32) );

        }

        final Frame f = new Frame();

        int a = 0;
        int b = 0;

        
        if ((oldVolume[0] - wVolume[0]) > DEFAULT_VU_METER_DECAY)
            a =(int) (oldVolume[0] * ((float) (height) ));
        if ((oldVolume[1] - wVolume[1]) > DEFAULT_VU_METER_DECAY)
            b = (int) (oldVolume[1] * ((float) (height) ));
        
        final int loudertmp = Math.max(a, b);
        final int louder = loudertmp + 1;
        
        count ++;
        new RainbowEllipse(this.xmittel, this.ymittel, this.r + louder / 20, this.r + louder / 20) {

            @Override
            protected void drawPixel(int x, int y, Color c) {
                Color c2 = new Color(Math.min(255, c.getRed() * louder), 
                        Math.min(255, c.getGreen() * louder), 
                        Math.min(255, c.getBlue() * louder) );
                f.add(new Pixel(x, y, c2));                        
            }
            
        }.drawEllipse(count / 100);

        try {
            rc.sendFrame(f);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}