package jason.app.gst.tutorial.java;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Sample;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.elements.AppSrc;


public class BasicTutorial8b {
	public static final int CHUNK_SIZE = 1024;
	public static final BigDecimal SAMPLE_RATE = new BigDecimal(44100);
	public static final BigDecimal GST_SECOND = new BigDecimal(1000000000);
	private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
	public static void main(String[] args) {
		CustomData data = new CustomData();
		 data.b = 1; /* For waveform generation */
		  data.d = 1;
		Gst.init();
		Pipeline pipeline  = (Pipeline) Gst.parseLaunch("appsrc name=src ! tee name=t t. ! queue ! audioconvert ! audioresample ! autoaudiosink "
                + "t. ! queue ! audioconvert ! wavescope name=visual ! videoconvert ! textoverlay name=textOverLay  ! autovideosink "
                + "t. ! queue ! appsink name=appsink");
		data.appSource = (AppSrc) pipeline.getElementByName("src");
		Caps caps = Caps.fromString("audio/x-raw ,format=S16LE, layout=interleaved, rate=44100, channels=1");
		data.appSource.setCaps(caps);
		data.appSource.set("format", 3);
		data.appSource.connect((AppSrc.NEED_DATA)(source,size)->startFeed(source,size,data));
		data.appSource.connect((AppSrc.ENOUGH_DATA)(source)->stopFeed(source,data));
		
		data.appSink = (AppSink) pipeline.getElementByName("appsink");
		data.appSink.set("emit-signals", true);
		data.appSink.setCaps(caps);
		data.appSink.connect((AppSink.NEW_SAMPLE)(sink)->newSample(sink,data));
		
		data.visual = pipeline.getElementByName("visual");
		data.visual.set("shader", 0);
		data.visual.set("style", 0);
		
		data.textOverLay = pipeline.getElementByName("textOverLay");
		data.textOverLay.set("font-desc", "San 32");
		
		Bus bus = pipeline.getBus();
		bus.connect((Bus.EOS) gstObject -> System.out.println("EOS "+gstObject));
		bus.connect((Bus.ERROR) (gstObject, i, s) -> System.out.println("ERROR "+i+" "+s+" "+gstObject));
		bus.connect((Bus.WARNING) (gstObject, i, s) -> System.out.println("WARN "+i+" "+s+" "+gstObject));
		bus.connect((Bus.EOS) obj -> Gst.quit() );
		pipeline.play();
		Gst.main();
	}

	private static FlowReturn newSample(AppSink sink, CustomData data) {
		Sample sample = sink.pullSample();
		if(sample!=null) {
			System.out.print("*");
			data.textOverLay.set("text", format.format(new Date()));
			return FlowReturn.OK;
		}
		return FlowReturn.ERROR;
	}

	private static void startFeed(AppSrc source, int size, CustomData data) {
		System.out.println("start feeding");
		if(data.sourceid==null) {
			data.sourceid = new PushData(data);
			data.sourceid.start();
		}else {
			data.sourceid.setRunning(true);
		}
		
	}
	
	private static void stopFeed(AppSrc source, CustomData data) {
		System.out.println("stop feeding");
		if(data.sourceid!=null) {
			data.sourceid.setRunning(false);
		}
	}

	static class CustomData {
		AppSrc appSource;
		AppSink appSink;
		
		Element audioQueue2,audioEnc,audioPay,audioQueue3, audioCapsFilter;
		Element pipeline, tee, audioQueue, audioConvert1, audioResample, audioSink;
		Element videoQueue, audioConvert2, visual, videoConvert,textOverLay, videoSink;
		Element appQueue;
		int numOfSamples;
		float a,b,c,d;
		PushData sourceid;
		
	}
	
	static class PushData extends Thread {
		private CustomData data;
		private boolean running;
		public boolean isRunning() {
			return running;
		}
		public void setRunning(boolean running) {
			this.running = running;
		}
		public PushData(CustomData data) {
			this.data = data;
			running = true;
		}
		@Override
		public void run() {
			while(true) {
				if(running) {
				int numberOfSample = CHUNK_SIZE/2;
				Buffer buffer = new Buffer(CHUNK_SIZE);
				buffer.setPresentationTimestamp(new BigDecimal(data.numOfSamples).multiply(GST_SECOND).divide(SAMPLE_RATE,0,RoundingMode.HALF_UP).longValue());
				buffer.setDuration(new BigDecimal(numberOfSample).multiply(GST_SECOND).divide(SAMPLE_RATE,0,RoundingMode.HALF_UP).longValue());
				ByteBuffer map = buffer.map(true);
				data.c += data.d;
				data.d -= data.c / 1000;
				float freq = 1100 + 1000 * data.d;
				for (int i = 0; i < CHUNK_SIZE; i+=2) {
				    data.a += data.b;
				    data.b -= data.a / freq;
				    int raw = (int) (500 * data.a);
				    map.put(i, (byte) (raw & 0x00FF));
				    map.put(i+1, (byte) (raw>>8));
				}
				buffer.unmap();
				data.numOfSamples += numberOfSample;
				data.appSource.pushBuffer(buffer);
				}else {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		}
		
	}
}



