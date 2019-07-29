package jason.app.gst.tutorial.java;

import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.Element.PAD_ADDED;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.PadLinkException;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Structure;

class CustomData {
	Element pipeline;
	Element source;
	Element convert;
	Element sink;
}
public class BasicTutorial3 {
	private static CustomData data = new CustomData();
	public static void main(String[] args) {
		
		Gst.init();
		data.source = ElementFactory.make("uridecodebin", "source");
		data.convert = ElementFactory.make("audioconvert", "convert");
		data.sink = ElementFactory.make("autoaudiosink", "sink");
		
		Pipeline pipeline = new Pipeline("test-pipeline");
		pipeline.addMany(data.source,data.convert,data.sink);
		data.convert.link(data.sink);
		data.source.set("uri", "https://www.freedesktop.org/software/gstreamer-sdk/data/media/sintel_trailer-480p.webm");
		data.source.connect(padAddedHandler);
		Bus bus = pipeline.getBus();
		bus.connect((Bus.EOS) gstObject -> System.out.println("EOS "+gstObject));
		bus.connect((Bus.ERROR) (gstObject, i, s) -> System.out.println("ERROR "+i+" "+s+" "+gstObject));
		bus.connect((Bus.WARNING) (gstObject, i, s) -> System.out.println("WARN "+i+" "+s+" "+gstObject));
		bus.connect((Bus.EOS) obj -> Gst.quit() );
		pipeline.play();
		Gst.main();
	}

	private static PAD_ADDED padAddedHandler = new PAD_ADDED() {

		@Override
		public void padAdded(Element src, Pad newPad) {
			Pad sinkPad = data.convert.getStaticPad("sink");
			if(!sinkPad.isLinked()) {
				Caps newPadCaps = newPad.getCurrentCaps();
				Structure newPadStruct = newPadCaps.getStructure(0);
				String newPadType = newPadStruct.getName();
				if(newPadType.startsWith("audio/x-raw")) {
					try {
						newPad.link(sinkPad);
						System.out.println(String.format("Link succeeded (type '%s').",newPadType));
					}catch(PadLinkException e) {
						System.out.println(String.format("Type is '%s' but link failed.",newPadType));
					}
				}else {
					System.out.println(String.format("It has type '%s' which is not raw audio. Ignoring.", newPadType));
				}
			}else {
				System.out.println("We are already linked. Ignoring.");
			}
			
		}
		
	};
}
