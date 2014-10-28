package test;

import java.awt.EventQueue;

import javax.swing.JFrame;

import org.formbuilder.Form;
import org.formbuilder.FormBuilder;
import org.formbuilder.annotations.UIReadOnly;
import org.formbuilder.annotations.UITitle;


class Test {
	@UITitle("Test name")
	String name;
	
	@UIReadOnly
	@UITitle("Test time")
	long time;

	public String getName() { return name; }
	public void setName(String name) { this.name= name; }
	public long getTime() { return time; }
}


public class FormBuilderTest {

	
	FormBuilderTest() {
	}
	
	
	

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JFrame myFrame = new JFrame();
					myFrame.setBounds(100, 100, 918, 640);
					
					// ... further initialization
					FormBuilder<Test> testBuilder= FormBuilder.map( Test.class);
					testBuilder.doValidation(false);
					
					Form<Test> form = testBuilder.buildForm();
					
					Test t= new Test();
					t.name="DDD";
					t.time=129318;
					form.setValue(t);
					myFrame.getContentPane().add( form.asComponent() );
					myFrame.pack();
					myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					myFrame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});		
	}

	
	
	
}
