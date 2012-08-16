package name.kazennikov.fb2;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.NodeVisitor;
import org.jsoup.select.QueryParser;

import java.io.IOException;
import java.io.InputStream;

/**
* User: kazennikov
* Date: 16.08.12
* Time: 15:08
*/
public abstract class AbstractJSoupProcessor implements FBReader.Processor {
    Evaluator cleaner = QueryParser.parse("title, subtitle, poem, table, epigraph, empty-line, stanza, subtitle");


    public abstract void process(String text);

    public String render(Element e) {
        final StringBuilder sb = new StringBuilder();

        e.traverse(new NodeVisitor() {

            @Override
            public void tail(Node node, int depth) {
                if(node instanceof Element && ((Element)node).tagName().equals("p")) {
                    sb.append("\n");
                }
            }

            @Override
            public void head(Node node, int depth) {
                if(node instanceof TextNode) {
                    sb.append(((TextNode) node).getWholeText());
                }
            }
        });

        return sb.toString();
    }

    @Override
    public void process(String id, InputStream is) throws IOException {
        Document doc = Jsoup.parse(is, null, "", Parser.xmlParser());

        Elements lang = doc.select("lang");
        // skip non-russian
        if(lang.first() != null && !lang.text().equals("ru")) {
            return;
        }

        doc.select(cleaner).remove();

        Elements cnt = doc.select("p");
        cnt.select("poem, table, epigraph, empty-line, stanza, subtitle").remove();

        for(Element e : cnt) {
            String text = render(e);

            process(text);

        }

        cnt.size();

    }
}
