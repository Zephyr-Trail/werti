package org.werti.enhancements.client;

import com.google.gwt.user.client.Element;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesKeyboardEvents;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A basic cloze item.
 *
 * Checks against a preset target, knows about its successor and will focus it
 * before it disables itself. Use the styles <tt>WERTiClozeText{,Helped,Win,Fail}</tt>
 * to indicate user's success.
 *
 * @author Aleksandar Dimitrov
 * @version 0.1
 */
public class ClozeItem
		implements HasText
			   , SourcesClickEvents
			   , SourcesKeyboardEvents
			   , SourcesChangeEvents {
	private String target;
	private ClozeItem next;
	private final TextBox item;
	private final Button help;
	private static int fails, wins, helps = 0;
	private final RootPanel root;

	/**
	 * A keyboard listener for ClozeItems.
	 *
	 * On shift+Enter it will give the result to the user,
	 * on Enter it will checkt the user's answer.
	 *
	 * When giving a result or showing a correct answer, the ClozeItem disappears
	 * and a text consisting of the target word with a corresponding style shows
	 * up.
	 *
	 * <li>When the user enters a wrong answer, the ClozeItem will be assigend the
	 * style id <i>WERTiClozeTextFail</i>.</li>
	 *
	 * <li>The style for a correct answer (the target word in a <code>&lt;span&gt;</code>)
	 * is <i>WERTiClozeTextWin</i>.</li>
	 *
	 * <li>The style for an item the user did ask for (also a word in a <code>&lt;span&gt;</code>)
	 * is <i>WERTiClozeTextHelped</i>.</li>
	 */
	private class ClozeItemListener implements KeyboardListener {
		public void onKeyPress(Widget sender, char keyCode, int mods) {
			if (keyCode == KeyboardListener.KEY_ENTER) {
				if (mods == 1) {
					setText("WERTiClozeTextHelped");
					helps++;
					next.item.setFocus(true);
				} else if (item.getText().trim().toLowerCase().equals
						(target.toLowerCase())) {
					setText("WERTiClozeTextWin");
					wins++;
					next.item.setFocus(true);
				} else {
					item.setStyleName("WERTiClozeTextFail");
					fails++;
				}
			}
		}

		public void onKeyDown(Widget w, char c, int i) { }
		public void onKeyUp(Widget w, char c, int i) { }

		private void setText(String style) {
			final Element e = root.getElement();
			e.setInnerHTML("<span class=\"" + style + "\">"
					+ target
					+ "</span>");
		}
	}

	/**
	 * Construct the cloze item in the given <tt>RootPanel</tt>.
	 * 
	 * @param root The RootPanel the cloze item has to be added to.
	 * Ideally a <tt>&lt;span&gt;</tt>
	 */
	public ClozeItem(final RootPanel root) {
		this.item = new TextBox();
		item.addKeyboardListener(new ClozeItemListener());
		item.setStyleName("WERTiClozeText");
		this.help = new Button("?");
		help.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				item.setStyleName("WERTiClozeTextHelped");
				item.setText(target);
				item.setEnabled(false);
				helps++;
			}
		});
		help.setStyleName("WERTiHelpButton");
		this.root = root;
	}

	/**
	 * Called to put everything onto the root panel of this item
	 */
	public void finish() {
		root.add(item);
		//root.add(help);
	}

	public ClozeItem(RootPanel root, final String target, final ClozeItem next) {
		this(root);
		this.target = target;
		this.next = next;
	}

	/**
	 * Forwards to the text box
	 *
	 * @return The TextBox's <code>getText()</code>
	 */
	public String getText() {
		return this.item.getText();
	}

	/**
	 * Forwards to the text box
	 *
	 * @param t What the TextBox should display
	 */
	public void setText(String t) {
		this.item.setText(t);
	}

	/**
	 * Forwards to the Button
	 *
	 * @param listener A listener to add to the Button
	 */
	public void addClickListener(ClickListener listener) {
		this.help.addClickListener(listener);
	}

	/**
	 * Forwards to the Button
	 *
	 * @param listener A listener to remove from the Button
	 */
	public void removeClickListener(ClickListener listener) {
		this.help.removeClickListener(listener);
	}

	/**
	 * Forwards to the TextBox
	 *
	 * @param listener A listener to add to the TextBox
	 */
	public void addChangeListener(ChangeListener l) {
		this.item.addChangeListener(l);
	}

	/**
	 * Forwards to the TextBox
	 *
	 * @param listener A listener to remove from the TextBox
	 */
	public void removeChangeListener(ChangeListener l) {
		this.item.removeChangeListener(l);
	}

	/**
	 * Forwards to the TextBox
	 *
	 * @param listener A listener to add to the TextBox
	 */
	public void addKeyboardListener(KeyboardListener l) {
		this.item.addKeyboardListener(l);
	}

	/**
	 * Forwards to the TextBox
	 *
	 * @param listener A listener to remove from the TextBox
	 */
	public void removeKeyboardListener(KeyboardListener l) {
		this.item.removeKeyboardListener(l);
	}

	/**
	 * Gets the target for this instance. (i.e. the String this cloze item listens for)
	 *
	 * @return The target.
	 */
	public String getTarget () {
		return this.target;
	}

	/**
	 * Sets the pointer to the next cloze item for this instance.
	 *
	 * @param next The next.
	 */
	public void setNext (ClozeItem next) {
		this.next = next;
	}

	/**
	 * Sets the target for this instance. (i.e. the String this cloze item listens for)
	 *
	 * @param target The target.
	 */
	public void setTarget (String target) {
		this.target = target;
	}

	/**
	 * Gets the pointer to the next cloze item for this instance.
	 *
	 * @return The next.
	 */
	public ClozeItem getNext () {
		return this.next;
	}
}
