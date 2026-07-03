package test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class EE {

    private URL url;

    public URLFileOpener(String address) {
        try {
            this.url = new File(address.replace("/", File.separator)).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(String.format("THIS SHOULD NOT HAPPEN: error while forming URL from path '%s'", address), e);
        }
    }
}
