package org.werti.uima.ae;

import java.io.StringReader;

import java.util.Iterator;

import edu.stanford.nlp.ling.Word;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;

import org.apache.uima.cas.FSIndex;

import org.apache.uima.jcas.JCas;

import org.apache.uima.util.Level;

import org.werti.uima.types.annot.RelevantText;
import org.werti.uima.types.annot.Token;

public class PTBTokenizer extends JCasAnnotator_ImplBase {

	/**
	 * Go through all relevant text areas in the RelevantText-AnnotationIndex and annotate them
	 * with Token-Annotations.
	 *
	 * We only set the CAS-relative Begin and End, but also set the Word-Property of Token for now. This may
	 * change in the future for efficiency reasons.
	 */
	public void process(JCas cas) {
		getContext().getLogger().log(Level.INFO, "Starting tokenization process.");
		final FSIndex textIndex = cas.getAnnotationIndex(RelevantText.type);
		final Iterator<RelevantText> tit = textIndex.iterator();

		while (tit.hasNext()) {
			final RelevantText rt = tit.next();
			final String span = rt.getCoveredText();
			final edu.stanford.nlp.process.PTBTokenizer<Word> tokzr =
				edu.stanford.nlp.process.PTBTokenizer.newPTBTokenizer(new StringReader(span), false);

			// global and local skews
			final int gskew = rt.getBegin();
			int lskew = 0;

			tokenizeSpan: while (tokzr.hasNext()) {
				final String w = tokzr.next().word();
				final int index = span.indexOf(w, lskew);
				if (index == -1) continue tokenizeSpan;
				else lskew = index;
				final Token t = new Token(cas);
				t.setBegin(lskew + gskew);
				t.setEnd((lskew += w.length()) + gskew);
				t.addToIndexes();
			}
		}
		getContext().getLogger().log(Level.INFO, "Finished tokenization process.");
	}
}