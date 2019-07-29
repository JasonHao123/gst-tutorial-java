package jason.app.gst.tutorial.java;

import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;

public class BasicTutorial2 {

	public static void main(String[] args) {
		Gst.init();
		Element source = ElementFactory.make("videotestsrc", "source");
		Element sink = ElementFactory.make("autovideosink", "sink");
		Pipeline pipeline = new Pipeline("test-pipeline");
		pipeline.addMany(source,sink);
		source.link(sink);
		source.set("pattern", 0);
		
		Bus bus = pipeline.getBus();
		bus.connect((Bus.EOS) gstObject -> System.out.println("EOS "+gstObject));
		bus.connect((Bus.ERROR) (gstObject, i, s) -> System.out.println("ERROR "+i+" "+s+" "+gstObject));
		bus.connect((Bus.WARNING) (gstObject, i, s) -> System.out.println("WARN "+i+" "+s+" "+gstObject));
		bus.connect((Bus.EOS) obj -> Gst.quit() );
		pipeline.play();
		Gst.main();
	}

}
