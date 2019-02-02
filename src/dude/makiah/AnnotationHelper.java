package dude.makiah;

import java.io.File;
import java.lang.reflect.Method;

public class AnnotationHelper
{
    private final File javaFile;

    public AnnotationHelper(File javaFile)
    {
        this.javaFile = javaFile;
    }

    public void annotateClass()
    {
        try {
            Class c = Class.forName("");
            Method[] m = c.getDeclaredMethods();
            for (int i = 0; i < m.length; i++)
                System.out.println(m[i].toString());
        } catch (Throwable e) {
            System.err.println(e);
        }
    }
}
