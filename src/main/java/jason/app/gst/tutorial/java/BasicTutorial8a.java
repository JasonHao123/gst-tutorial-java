package jason.app.gst.tutorial.java;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

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


public class BasicTutorial8a {
	public static final int CHUNK_SIZE = 1024;
	public static final BigDecimal SAMPLE_RATE = new BigDecimal(44100);
	public static final BigDecimal GST_SECOND = new BigDecimal(1000000000);
	private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
	public static void main(String[] args) {
		CustomData data = new CustomData();
		 data.b = 1; /* For waveform generation */
		  data.d = 1;
		Gst.init();
		data.appSource = (AppSrc) ElementFactory.make("appsrc", "audio_source");
		data.tee = ElementFactory.make ("tee", "tee");
		data.audioQueue = ElementFactory.make ("queue", "audio_queue");
		data.audioConvert1 = ElementFactory.make ("audioconvert", "audio_convert1");
		data.audioResample = ElementFactory.make ("audioresample", "audio_resample");
		data.audioSink = ElementFactory.make ("autoaudiosink", "audio_sink");
		data.videoQueue = ElementFactory.make ("queue", "video_queue");
		data.audioConvert2 = ElementFactory.make ("audioconvert", "audio_convert2");
		data.visual = ElementFactory.make ("wavescope", "visual");
		data.videoConvert = ElementFactory.make ("videoconvert", "video_convert");
		data.textOverLay = ElementFactory.make ("textoverlay", "textoverlay");
		data.textOverLay.set("font-desc", "San 32");
		data.videoSink = ElementFactory.make ("autovideosink", "video_sink");
		data.appQueue = ElementFactory.make ("queue", "app_queue");
		data.appSink = (AppSink) ElementFactory.make ("appsink", "app_sink");
		Pipeline pipeline = new Pipeline("test-pipeline");
		
		data.visual.set("shader", 0);
		data.visual.set("style", 0);
		Caps caps = Caps.fromString("audio/x-raw ,format=S16LE, layout=interleaved, rate=44100, channels=1");
		data.appSource.setCaps(caps);
		data.appSource.set("format", 3);
		data.appSource.connect((AppSrc.NEED_DATA)(source,size)->startFeed(source,size,data));
		data.appSource.connect((AppSrc.ENOUGH_DATA)(source)->stopFeed(source,data));
		
		data.appSink.set("emit-signals", true);
		data.appSink.setCaps(caps);
		data.appSink.connect((AppSink.NEW_SAMPLE)(sink)->newSample(sink,data));
		
		pipeline.addMany(data.appSource, data.tee, data.audioQueue, data.audioConvert1, data.audioResample,
			      data.audioSink, data.videoQueue, data.audioConvert2, data.visual, data.videoConvert, data.textOverLay, data.videoSink, data.appQueue,
			      data.appSink);

		if(!Element.linkMany(data.appSource,data.tee) ||
		 !Element.linkMany(data.audioQueue, data.audioConvert1, data.audioResample, data.audioSink) || 
		 !Element.linkMany(data.videoQueue, data.audioConvert2, data.visual, data.videoConvert, data.textOverLay, data.videoSink ) || 
		 !Element.linkMany(data.appQueue, data.appSink)) {
			System.out.println("Elements could not be linked.");
			return;
		}
		Pad teeAudioPad =  data.tee.getRequestPad("src_%u");
		System.out.println(String.format("Obtained request pad %s for audio branch.", teeAudioPad.getName()));
		Pad queueAudioPad = data.audioQueue.getStaticPad("sink");
		Pad teeVideoPad =  data.tee.getRequestPad("src_%u");
		System.out.println(String.format("Obtained request pad %s for video branch.", teeVideoPad.getName()));
		Pad queueVideoPad = data.videoQueue.getStaticPad("sink");
		Pad teeAppPad = data.tee.getRequestPad("src_%u");
		System.out.println(String.format("Obtained request pad %s for app branch.", teeVideoPad.getName()));
		Pad queueAppPad = data.appQueue.getStaticPad("sink");
		
		try {
			teeAudioPad.link(queueAudioPad);
			teeVideoPad.link(queueVideoPad);
			teeAppPad.link(queueAppPad);
		}catch(Exception e) {
			System.out.println("Tee could not be linked");
			return;
		}
		
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



