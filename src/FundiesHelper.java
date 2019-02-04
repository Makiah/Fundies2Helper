import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class FundiesHelper extends JPanel implements ActionListener
{
	private final JButton go;
	private final JCheckBox includeMethodAnnotations;
	
    public FundiesHelper()
    {
        // Add file selection button
    	go = new JButton("Select new file to annotate");
        go.addActionListener(this);
        add(go);
        
        // Add checkbox for whether or not we should include method annotations
        includeMethodAnnotations = new JCheckBox("Include Method Annotations");
        add(includeMethodAnnotations);
    }

    public void actionPerformed(ActionEvent e)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Select file to be annotated");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // Disable "All Files" option
        chooser.setAcceptAllFileFilterUsed(false);

        // Java files have to be compiled but are accepted
        chooser.addChoosableFileFilter(new FileFilter() 
        {
            public String getDescription() 
            {
                return "Java Files (*.java)";
            }

            public boolean accept(File f) 
            {
                if (f.isDirectory()) {
                    return true;
                } else {
                    return f.getName().toLowerCase().endsWith(".java");
                }
            }
        });

        // Show the open dialog
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            System.out.println("getCurrentDirectory(): "
                    +  chooser.getCurrentDirectory());
            System.out.println("getSelectedFile() : "
                    +  chooser.getSelectedFile());

            // Annotate the file
            System.out.println("Including annotations: " + includeMethodAnnotations.isSelected());
            new AnnotationHelper(chooser.getSelectedFile()).annotateClass(includeMethodAnnotations.isSelected());
        }
        else
        {
            System.out.println("No Selection ");
        }
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(500, 75);
    }

    public static void main(String[] args)
    {
        JFrame frame = new JFrame("");
        FundiesHelper panel = new FundiesHelper();
        frame.addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            }
        );
        frame.getContentPane().add(panel, "Center");
        frame.setSize(panel.getPreferredSize());
        frame.setVisible(true);
    }
}