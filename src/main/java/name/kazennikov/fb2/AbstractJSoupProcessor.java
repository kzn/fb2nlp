package name.kazennikov.fb2;

import gnu.trove.map.hash.TObjectIntHashMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: ant
 * Date: 16.08.12
 * Time: 20:21
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractJSoupProcessor implements FBReader.Processor {
    TObjectIntHashMap<String> words = new TObjectIntHashMap<String>();


    public void add(String word) {
        for(int i = 0; i != word.length(); i++) {
            if(Character.isDigit(word.charAt(i)))
                return;
        }

        words.adjustOrPutValue(new String(word.toLowerCase()), 1, 1);
    }


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


        Elements cnt = doc.select("p");
        cnt.select("poem table epigraph empty-line stanza subtitle").remove();

        for(Element e : cnt) {
            String text = render(e);
            process(text);

        }

        cnt.size();

    }


}
