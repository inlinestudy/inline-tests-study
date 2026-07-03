package test;

import com.ftpix.sherdogparser.models.Organization; 
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.text.ParseException;

public class E {

        public Organization parseDocument(Document doc) throws IOException, ParseException {
                Organization organization = new Organization();
                organization.setSherdogUrl(ParserUtils.getSherdogPageUrl(doc));

                String url = organization.getSherdogUrl();
                url += "/recent-events/%d";
                int page = 1;

                doc = ParserUtils.parseDocument(String.format(url, page));
                return organization;
        }
}
