package com.yoursway.modelediting.swt.demo;import org.eclipse.swt.SWT;import org.eclipse.swt.custom.StyledText;import org.eclipse.swt.layout.FillLayout;import org.eclipse.swt.widgets.Display;import org.eclipse.swt.widgets.Listener;import org.eclipse.swt.widgets.Shell;import com.yoursway.modelediting.Fragment;import com.yoursway.modelediting.IModelListener;import com.yoursway.modelediting.Model;import com.yoursway.modelediting.ReplaceImpossibleException;import com.yoursway.modelediting.StaticTextFragment;import com.yoursway.modelediting.TextFragment;import com.yoursway.modelediting.swt.IStyledText;import com.yoursway.modelediting.swt.ModelReconciler;public class ModelEditingDemo {	private StyledText styledText;	private AngryLetter angryLetter;	private TextFormModel formModel;	private ModelReconciler reconciler;	private class StyledTextWrap implements IStyledText {		public void addListener(int eventType, Listener listener) {			styledText.addListener(eventType, listener);		}		public String getText() {			return styledText.getText();		}		public void removeListener(int eventType, Listener listener) {			styledText.removeListener(eventType, listener);		}		public void setText(String text) {			styledText.setText(text);		}	}	public void run() {		Display display = new Display();		Shell shell = new Shell(display);		shell.setSize(400, 300);		shell.setLayout(new FillLayout());		styledText = new StyledText(shell, SWT.BORDER);		angryLetter = new AngryLetter();		formModel = new TextFormModel();		formModel.fragments().add(new StaticTextFragment("From: "));		formModel.fragments().add(new TextFragment("<yourname here>", true, true));		formModel.fragments().add(new StaticTextFragment("\n"));		formModel.fragments().add(new StaticTextFragment("To: "));		formModel.fragments().add(new TextFragment("<recipient name here>", true, true));		formModel.fragments().add(new StaticTextFragment("\n"));		formModel.fragments().add(new StaticTextFragment("Message: "));		formModel.fragments().add(new TextFragment("<message here>", true, true));		formModel.fragments().add(new StaticTextFragment("\n"));		formModel.fragments().add(				new StaticTextFragment("And again, fucking dumb, I'm writing you: "));		formModel.fragments().add(new TextFragment("", true, true));		formModel.fragments().add(new StaticTextFragment("\n"));		formModel.addListener(new IModelListener() {			public void modelChanged(Object sender, Model model, int firstFragment, int oldCount,					int newCount) {				if (sender == angryLetter)					return;				if (oldCount != newCount || oldCount != 1)					throw new RuntimeException("Ooops...");				String fragmentText = model.fragments().get(firstFragment).toString();				switch (firstFragment) {				case 1:					angryLetter.setFrom(fragmentText);					break;				case 4:					angryLetter.setTo(fragmentText);					break;				case 7:				case 10:					angryLetter.setMessage(fragmentText);					break;				default:					throw new RuntimeException("Trying to change non-changable!");				}			}		});		angryLetter.addListener(new LetterListener() {			private void setFragmentToString(int index, String string) {				Fragment fragment = formModel.fragments().get(index);				try {					formModel.replace(angryLetter, fragment, 0, fragment.toString().length(),							string);				} catch (ReplaceImpossibleException e) {					throw new RuntimeException(e);				}			}			public void fromChanged() {				setFragmentToString(1, angryLetter.from());			}			public void messageChanged() {				setFragmentToString(7, angryLetter.message());				setFragmentToString(10, angryLetter.message());			}			public void toChanged() {				setFragmentToString(4, angryLetter.to());			}		});		reconciler = new ModelReconciler(new StyledTextWrap(), formModel);		shell.open();		while (!shell.isDisposed()) {			if (!display.readAndDispatch())				display.sleep();		}		display.dispose();	}	public static void main(String[] args) {		new ModelEditingDemo().run();	}}