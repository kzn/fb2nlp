package name.kazennikov.fb2;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import name.kazennikov.tokens.AbstractToken;
import name.kazennikov.tokens.BaseToken;
import name.kazennikov.tokens.BaseTokenType;
import name.kazennikov.tokens.SentenceSplitter;
import name.kazennikov.tokens.SimpleTokenizer;
import name.kazennikov.tokens.TokenStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;


public class FBReader {
	
	public static interface Processor {
		public void process(String id, InputStream is) throws IOException;
	}
	
	public static class JSoupProcessor implements Processor {
		int totCount = 0;
		int rusCount = 0;
		long chars = 0;
		int tokens = 0;
		long tokenChars = 0;
		int sents = 0;
		TObjectIntHashMap<String> words = new TObjectIntHashMap<String>();
		
		public void add(String word) {
			for(int i = 0; i != word.length(); i++) {
				if(Character.isDigit(word.charAt(i)))
					return;
			}
			
			words.adjustOrPutValue(new String(word.toLowerCase()), 1, 1);
		}
		
		SentenceSplitter ss = new SentenceSplitter(new HashSet<String>());
		
		public void parse(String text) {
			List<AbstractToken> tokens = SimpleTokenizer.tokenize(text);
			
			List<BaseToken> sents = ss.split(new TokenStream(tokens));
			this.sents += sents.size();
			
			List<AbstractToken> filtered = new ArrayList<AbstractToken>(tokens.size());
			
			for(AbstractToken t : tokens) {
				if(t.is(BaseTokenType.SPACE) || t.is(BaseTokenType.NEWLINE) || t.is(BaseTokenType.PUNC))
					continue;
				
				filtered.add(t);
			}
			
			
			for(AbstractToken t : filtered) {
				this.tokens++;
				tokenChars += t.text().length();
				add(t.text());
			}
			
			
		}
		
		public String render(Elements e) {
			final StringBuilder sb = new StringBuilder();
			
			e.traverse(new NodeVisitor() {
				
				@Override
				public void tail(Node node, int depth) {
					if(node instanceof Element && ((Element)node).tagName().equals("p")) {
						sb.append("\n\n");
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
			totCount++;
			// skip non-russian
			if(lang.first() != null && !lang.text().equals("ru")) {
				return;
			}
			
			
			rusCount++;
			Elements cnt = doc.select("p");
			cnt.select("poem table epigraph empty-line stanza subtitle").remove();
			String text = render(cnt);
			chars += text.length();
			parse(text);

			cnt.size();

		}
	}
	
	public void process(File f, Processor p) throws IOException {
		if(f.getName().endsWith(".zip")) {

			ZipFile zip = new ZipFile(f);
			try {
				for(Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
					ZipEntry zipEntry = e.nextElement();
					System.out.printf("processing %s%n", zipEntry.getName());
					p.process(zipEntry.getName(), zip.getInputStream(zipEntry));
				}
				zip.close();
			} finally {
				zip.close();
			}
		} else {
			FileInputStream fis = new FileInputStream(f);
			try {
				p.process(f.getName(), fis);
			} finally {
				fis.close();
			}
			
		}
		
	}
	
	
	public static void main(String[] args) throws IOException {
		
		FBReader fbr = new FBReader();
		JSoupProcessor p = new JSoupProcessor();
		
		long st = System.currentTimeMillis();
		
		File fb2Path = new File("/media/f978cba3-c1ac-42e8-9daa-f34ee4b3ea55/torrents/_LIB.RUS.EC/lib.rus.ec");
		//File fb2Path = new File(".");
		int count = 0;
		
		for(File f : fb2Path.listFiles()) {
			if(!f.getName().endsWith(".zip"))
				continue;
			System.err.printf("Processing %s%n", f);
			fbr.process(f, p);
			
			count++;
			
			if(count == 50)
				break;
		}
		
		//fbr.process(new File("101016-102097.zip"), p);
		
		System.out.printf("Elapsed: %d ms%n", System.currentTimeMillis() - st);
		
		System.out.printf("Russian: %d/%d%n", p.rusCount, p.totCount);
		System.out.printf("Characters: %d%n", p.chars);
		System.out.printf("Tokens: %d%n", p.tokens);
		System.out.printf("Tokens chars: %d%n", p.tokenChars);
		System.out.printf("Sents: %d%n", p.sents);
		System.out.printf("Distinct tokens: %d%n", p.words.size());
		
		final PrintWriter pw = new PrintWriter("words.tsv");
		
		p.words.forEachEntry(new TObjectIntProcedure<String>() {

			@Override
			public boolean execute(String a, int b) {
				pw.printf("%s\t%d%n", a, b);
				return true;
			}
		});
		
		pw.close();
		
	}

}
