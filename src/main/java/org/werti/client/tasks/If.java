package org.werti.client.tasks;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.werti.client.run.ConditionalConfiguration;
import org.werti.client.run.RunConfiguration;

import org.werti.client.ui.ECheckBox;
import org.werti.client.ui.ERadioButton;
import org.werti.client.ui.HasData;

public class If implements Task {

	// the title of this task, as it will appear in the menu.
	private static final String NAME = "Conditionals";

	private static final ECheckBox first = new ECheckBox("1", "1");
	private static final ECheckBox second = new ECheckBox("2", "2");
	private static final ECheckBox third = new ECheckBox("3", "3");
	private static final ECheckBox ommission = new ECheckBox("omis", "Include If Ommission");

	private static final ERadioButton tense = new ERadioButton("Colorize", "methods", "Correct Tense");
	private static final ERadioButton type = new ERadioButton("Ask", "methods", "Conditional Type");

	private static final ERadioButton tenseCondition = new ERadioButton("Ask", "tense", "Condition");
	private static final ERadioButton tenseResult = new ERadioButton("Ask", "tense", "Result");
	private static final ERadioButton tenseRandom = new ERadioButton("Ask", "tense", "Random");

	private static final HasData[] typeConfig = { first, second, third };
	private static final HasData[] taskConfig = { tense, type, tenseCondition, tenseRandom, tenseResult };

	/**
	 * The name of this component.
	 *
	 * @return This component's name.
	 */
	public String name() { return NAME; }

	/**
	 * Build a user interface for this component.
	 *
	 * This component allows selection of one enhancement method and several
	 * PoS tags as targets.
	 *
	 * @return A GWT <code>Widget</code> that allows the user to configure this task.
	 */
	public Widget userInterface() {
		final VerticalPanel main = new VerticalPanel();
		final VerticalPanel advanced = new VerticalPanel();
		advanced.setStyleName("Conditionals-Advanced");

		final HorizontalPanel condType = new HorizontalPanel();
		final HorizontalPanel task = new HorizontalPanel();
		final HorizontalPanel tenseType = new HorizontalPanel();

		condType.add(first);
		condType.add(second);
		condType.add(third);

		task.add(tense);
		task.add(type);

		tenseType.add(tenseCondition);
		tenseType.add(tenseResult);
		tenseType.add(tenseRandom);

		advanced.add(ommission);

		tense.setChecked(true);
		first.setChecked(true);
		second.setChecked(true);
		third.setChecked(true);

		final ConditionalListener cl = new ConditionalListener();

		first.addClickListener(cl);
		second.addClickListener(cl);
		third.addClickListener(cl);

		main.add(condType);
		main.add(advanced);
		main.add(task);
		main.add(tenseType);

		return main;
	}

	private class ConditionalListener implements ClickListener {
		public void onClick(Widget w) {
		}
	}

	/**
	 * Build a Server-Side configuration object.
	 *
	 * This task uses <code>CategoriesConfiguration</code> adjusting tags
	 * and method according to the user interface settings.
	 *
	 * @return A server-side configuration object knowing about the
	 * <code>AnalysisEngine</code>s to use and how to configure them and also
	 * what enhancement module to use.
	 */
	public RunConfiguration configure() {
		return new ConditionalConfiguration();
	}

	/**
	 * A help widget to inform the user.
	 *
	 * @return A widget containing an explanatory text.
	 */
	public Widget helpText() {
		HTML text = new HTML
			( "<h4>Part of Speech</h4>"
			+ "<p>Train your understanding of English conditionals.</p>"
			);
		return text;
	}
}
