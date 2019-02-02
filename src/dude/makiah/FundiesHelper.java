package dude.makiah;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 Compile with this:
 C:\Documents and Settings\glow\My Documents\j>javac DumpMethods.java

 Run like this, and results follow
 C:\Documents and Settings\glow\My Documents\j>java DumpMethods
 public void DumpMethods.foo()
 public int DumpMethods.bar()
 public java.lang.String DumpMethods.baz()
 public static void DumpMethods.main(java.lang.String[])
 */

public class FundiesHelper extends JPanel implements ActionListener
{
    private JButton go;

    private JFileChooser chooser;
    private String choosertitle;

    public FundiesHelper()
    {
        go = new JButton("Select new file to annotate");
        go.addActionListener(this);
        add(go);
    }

    public void actionPerformed(ActionEvent e)
    {
        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle(choosertitle);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);

        // Java files have to be compiled but are accepted
        chooser.addChoosableFileFilter(new FileFilter() {

            public String getDescription() {
                return "Java Files (*.java)";
            }

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    return f.getName().toLowerCase().endsWith(".java");
                }
            }
        });

        // Add choosable file filter for class files
        chooser.addChoosableFileFilter(new FileFilter() {

            public String getDescription() {
                return "Class Files (*.class)";
            }

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    return f.getName().toLowerCase().endsWith(".class");
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
            new AnnotationHelper(chooser.getSelectedFile()).annotateClass();
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
        frame.getContentPane().add(panel,"Center");
        frame.setSize(panel.getPreferredSize());
        frame.setVisible(true);
    }
}
