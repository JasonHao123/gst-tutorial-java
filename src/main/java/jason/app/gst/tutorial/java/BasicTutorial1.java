package jason.app.gst.tutorial.java;

import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;

public class BasicTutorial1 {

	public static void main(String[] args) {
		Gst.init();
		Pipeline pipeline = (Pipeline) Gst.parseLaunch("playbin uri=https://www.freedesktop.org/software/gstreamer-sdk/data/media/sintel_trailer-480p.webm");

		Bus bus = pipeline.getBus();
		bus.connect((Bus.EOS) gstObject -> System.out.println("EOS "+gstObject));
		bus.connect((Bus.ERROR) (gstObject, i, s) -> System.out.println("ERROR "+i+" "+s+" "+gstObject));
		bus.connect((Bus.WARNING) (gstObject, i, s) -> System.out.println("WARN "+i+" "+s+" "+gstObject));
		bus.connect((Bus.EOS) obj -> Gst.quit() );
		pipeline.play();
		Gst.main();
	}

}
