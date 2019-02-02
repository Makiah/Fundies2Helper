import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnnotationHelper
{
    private Class<?> loadedClass;

    public AnnotationHelper(File javaFile)
    {
        System.out.println(javaFile.getName());

        try
        {
            // Compile file
            String fileToCompile = javaFile.getAbsolutePath();
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            int compilationResult =	compiler.run(null, null, null, fileToCompile);
            if(compilationResult == 0) {
                System.out.println("Compilation is successful");
            } else {
                System.out.println("Compilation Failed");
            }

            // Load class
            // Convert File to a URL
            URL url = javaFile.getParentFile().toURI().toURL();          // file:/c:/myclasses/
            URL[] urls = new URL[]{url};

            // Create a new class loader with the directory
            ClassLoader cl = new URLClassLoader(urls);

            // Load in the class; MyClass.class should be located in
            // the directory file:/c:/myclasses/com/mycompany
            Class cls = cl.loadClass(javaFile.getName().replaceFirst("[.][^.]+$", ""));
            for (Method m : cls.getDeclaredMethods())
            {
                System.out.println(m.getName());
            }
        }
        catch (Exception exp)
        {
            exp.printStackTrace();
        }
    }

    public void annotateClass()
    {
    }
}