package org.werti.uima.ae;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.EmptyStringList;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.NonEmptyStringList;
import org.apache.uima.resource.ResourceInitializationException;
import org.werti.uima.types.annot.CGReading;
import org.werti.uima.types.annot.CGToken;
import org.werti.uima.types.annot.Token;

public class Vislcg3Annotator extends JCasAnnotator_ImplBase {

	private static final Logger log =
		Logger.getLogger(Vislcg3Annotator.class);	
	
	/**
	 * A runnable class that reads from a reader (that may
	 * be fed by {@link Process}) and puts stuff read to
	 * the logger as debug messages.
	 * @author nott
	 */
	public class ExtCommandConsume2Logger implements Runnable {
		
		BufferedReader reader;
		String msgPrefix;

		public ExtCommandConsume2Logger(BufferedReader reader, String msgPrefix) {
			super();
			this.reader = reader;
			this.msgPrefix = msgPrefix;
		}

		/**
		 * Reads from the external command linewise and puts the result to the logger.
		 * Exceptions are never thrown but stuffed into the logger as well.
		 */
		public void run() {
			String line = null;
			try {
				while ( (line = reader.readLine()) != null ) {
					log.debug(msgPrefix + line);
				}
			} catch (IOException e) {
				log.error("Error in reading STDERR from external command.", e);
			}
		}
	}

	private String vislcg3Loc;
	private String vislcg3GrammarLoc;
	
	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
		vislcg3Loc = (String) context.getConfigParameterValue("vislcg3Loc");
		vislcg3GrammarLoc = (String) context.getConfigParameterValue("vislcg3GrammarLoc");
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		// collect original tokens here
		ArrayList<Token> originalTokens = new ArrayList<Token>();
		FSIterator tokenIter = jcas.getAnnotationIndex(Token.type).iterator();
		while (tokenIter.hasNext()) {
			originalTokens.add((Token)tokenIter.next());
		}
		// convert token list to cg input
		String cg3input = toCG3Input(originalTokens);
		try {
			// run vislcg3
			String cg3output = runVislcg3(cg3input);
			// parse cg output
			List<CGToken> newTokens = parseCGOutput(cg3output, jcas);
			// assert that we got as many tokens back as we provided
			if (newTokens.size() != originalTokens.size()) {
				throw new IllegalArgumentException("Token list size mismatch: " +
				"Original tokens: " + originalTokens.size() + ", After CG3: " + newTokens.size());
			}
				
			// complete new tokens with information from old ones
			for (int i = 0; i < originalTokens.size(); i++) {
				Token origT = originalTokens.get(i);
				CGToken newT = newTokens.get(i);
				copy(origT, newT);
				// update CAS
				jcas.removeFsFromIndexes(origT);
				jcas.addFsToIndexes(newT);
			}
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		} catch (IllegalArgumentException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
	
	/*
	 * helper for copying over information from Token to CGToken
	 */
	private void copy(Token source, CGToken target) {
		target.setBegin(source.getBegin());
		target.setEnd(source.getEnd());
		target.setTag(source.getTag());
		target.setLemma(source.getLemma());
	}
	
	/*
	 * helper for converting Token annotations to a String for vislcg3
	 */
	private String toCG3Input(List<Token> tokenList) {
		StringBuilder result = new StringBuilder();
		for (Token t : tokenList) {
			result.append("\"<" + t.getCoveredText() + ">\"\n");
			result.append("\t\"");
			if (t.getLemma() != null) {
				result.append(t.getLemma());
			}
			result.append("\"");
			String tag = t.getTag();
			if (tag != null) {
				tag = tag.toUpperCase();
			} else {
				tag = "NOTAG";
			}
			result.append(" " + tag + "\n");
		}
		return result.toString();
	}
	
	/*
	 * helper for running vislcg3
	 */
	private String runVislcg3(String input) throws IOException {
		// build argument list
		ArrayList<String> argList = new ArrayList<String>();
		argList.add(vislcg3Loc);
		argList.add("--grammar");
		argList.add(vislcg3GrammarLoc);
		
		// obtain process
		ProcessBuilder builder = new ProcessBuilder(argList);
		Process process = builder.start();
		// get input and output streams (are they internally buffered??)
		BufferedWriter toCG =  new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
		BufferedReader fromCG = new BufferedReader(new InputStreamReader(process.getInputStream()));
		BufferedReader errorCG = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		
		// take care of VislCG's STDERR inside a special thread.
		(new Thread(new ExtCommandConsume2Logger(errorCG, "VislCG STDERR: "),
			"VislCG STDERR catcher")).start();
		
		// write input
		// FIXME: is it guaranteed that the buffers are large enough for doing this?
		toCG.write(input);
		toCG.close();
		// get back output
		String output ="";
		String line = null;
		
		while ( ( line = fromCG.readLine() ) != null ) {
			output += line + "\n";
		}
		fromCG.close();
		errorCG.close();
		return output;
	}

	/*
	 * helper for parsing output from vislcg3 back into our CGTokens
	 */
	private List<CGToken> parseCGOutput(String cgOutput, JCas jcas) {
		ArrayList<CGToken> result = new ArrayList<CGToken>();
		
		// current token and its readings
		CGToken current = null;
		ArrayList<CGReading> currentReadings = new ArrayList<CGReading>();
		// read output line by line, eat multiple newlines
		for (String line : cgOutput.split("\n+")) {
			// case 1: new cohort
			if (line.startsWith("\"<")) {
				if (current != null) {
					// save previous token
					current.setReadings(new FSArray(jcas, currentReadings.size()));
					int i = 0;
					for (CGReading cgr : currentReadings) {
						current.setReadings(i, cgr);
						i++;
					}
					result.add(current);
				}
				// create new token
				current = new CGToken(jcas);
				currentReadings = new ArrayList<CGReading>();
			// case 2: a reading in the current cohort
			} else {
				CGReading reading = new CGReading(jcas);
				// split reading line into tags
				String[] temp = line.split("\\s+");
				reading.setTail(new EmptyStringList(jcas));
				reading.setHead(temp[temp.length-1]);
				// iterate backwards due to UIMAs prolog list disease
				for (int i = temp.length-2; i >= 0; i--) {
					if (temp[i].equals("")) {
						break;
					}
					// in order to extend the list, we have to set the old one as tail and the new element as head
					NonEmptyStringList old = reading;
					reading = new CGReading(jcas);
					reading.setTail(old);
					reading.setHead(temp[i]);
				}
				// add the reading
				currentReadings.add(reading);
			}
		}
		if (current != null) {
			// save last token
			current.setReadings(new FSArray(jcas, currentReadings.size()));
			int i = 0;
			for (CGReading cgr : currentReadings) {
				current.setReadings(i, cgr);
				i++;
			}
			result.add(current);
		}
		return result;
	}
}
