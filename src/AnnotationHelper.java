import javax.tools.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class AnnotationHelper
{
	private final File javaFile;

    public AnnotationHelper(File javaFile)
    {
    	this.javaFile = javaFile;
    }
    
    private boolean isPointlessToAnnotate(Class<?> c)
    {
    	return c.isPrimitive() || c.isAssignableFrom(String.class) || 
    			c.getName().startsWith("java.") || c.getName().startsWith("tester.") ||
    			c.getName().startsWith("javalib.");
    }
    
    private String annotate(Field f)
    {
		return "... this." + f.getName() + " ...  --" + f.getType().getSimpleName();
    }
    
    private String annotate(Method m, String prefix)
    {
		StringBuilder currentAnnotation = new StringBuilder("... this." + prefix + m.getName() + "(");
		for (Parameter p : m.getParameters())
			currentAnnotation.append(p.getType().getSimpleName() + " ");
		if (m.getParameterCount() > 0)
			currentAnnotation.setLength(currentAnnotation.length() - 1);
		currentAnnotation.append(") ...  --" + m.getReturnType().getSimpleName());
		return currentAnnotation.toString();
    }
    
    private Map<String, String> generateAnnotations(Class<?> c, Map<String, String> currentAnnotations) 
    {
    	// Verify that this hasn't already been generated
    	for (Map.Entry<String, String> entry : currentAnnotations.entrySet()) 
		{
			if (c.getName().equals(entry.getKey()))
			{
				return currentAnnotations;
			}
		}
    	
    	System.out.println("Generating annotations for " + c.getName());
    	
    	// Build and apply annotations
		StringBuilder currentAnnotation = new StringBuilder(
				"// In " + c.getName() + "\n" + 
				"/* TEMPLATE: \n");

		// Fields
		currentAnnotation.append("Fields: \n");
		for (Field f : c.getDeclaredFields()) 
			currentAnnotation.append(annotate(f) + "\n");

		// Methods
		currentAnnotation.append("Methods: \n");
		for (Method m : c.getDeclaredMethods()) 
			currentAnnotation.append(annotate(m, "") + "\n");

		// Methods of fields
		currentAnnotation.append("Methods of fields: \n");
		for (Field f : c.getDeclaredFields()) 
		{
			// don't annotate primitives or Strings
			if (isPointlessToAnnotate(f.getType()))
				continue; 

			// get all methods
			for (Method m : f.getType().getDeclaredMethods()) 
			{
				currentAnnotation.append(annotate(m, f.getName() + ".") + "\n");
			}
		}

		// Finalize current annotation
		currentAnnotation.append("*/");
		
		// Add this annotation to the map
		currentAnnotations.put(c.getName(), currentAnnotation.toString());
		return currentAnnotations;
    }

    public void annotateClass()
    {
		Map<String, String> currentAnnotations = new HashMap<String, String>();

        /// Compile file and generate annotations
        try
        {
        	// Compile file
            String fileToCompile = javaFile.getAbsolutePath();
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            int compilationResult =	compiler.run(null, null, null, fileToCompile);
            if(compilationResult == 0) 
            {
                System.out.println("Compilation is successful");
            } 
            else 
            {
                System.out.println("Compilation Failed");
            }

            /// Load class
            // Convert File to a URL
            URL url = javaFile.getParentFile().toURI().toURL();
            URL[] urls = new URL[]{url};

            // Create a new class loader with the directory
            ClassLoader cl = new URLClassLoader(urls);
			
			// Get all .class files for all classes
			File[] classFiles = javaFile.getParentFile().listFiles(new FilenameFilter() { 
	            public boolean accept(File dir, String fn) 
	            {
	            	return fn.endsWith(".class");
	            }
			});
			
			// For each class file load the class and obtain annotations
			for (File f : classFiles)
			{
				Class<?> loadedClass = cl.loadClass(f.getName().substring(0, f.getName().length() - 6));
				currentAnnotations = generateAnnotations(loadedClass, currentAnnotations);
				f.delete();
			}
        }
        catch (Exception exp)
        {
            exp.printStackTrace();
        }
        
        // Rebuild the map using the simple names
        Map<String, String> rebuiltMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : currentAnnotations.entrySet()) 
		{
        	rebuiltMap.put(entry.getKey().substring(entry.getKey().lastIndexOf(".") + 1), entry.getValue());
		}
        
        // Print map to user
        for (Map.Entry<String, String> entry : rebuiltMap.entrySet()) 
		{
        	System.out.println("{\n" + entry.getKey() + ", \n" + entry.getValue() + "}\n");
		}

		// Now write the annotations to the file
        try 
        {
            FileReader fr = new FileReader(javaFile);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            List<String> lines = new ArrayList<String>();
            while ((line = br.readLine()) != null) 
            {
                lines.add(line);
            	
            	// Ensure that the name of the class is mentioned after the class declaration
            	Map.Entry<String, String> bestEntry = null;
            	int indexOfClassNameString = -1;
            	for (Map.Entry<String, String> entry : currentAnnotations.entrySet()) 
        		{
            		indexOfClassNameString = Math.max(line.indexOf("class " + entry.getKey()), 
            				line.indexOf("interface " + entry.getKey()));
            		if (indexOfClassNameString != -1)
            		{
            			bestEntry = entry;
            			break;
            		}
        		}
            	
            	// Finally add annotation
            	if (bestEntry != null)
            		lines.add(lines.size() - 1, bestEntry.getValue());
            }
            fr.close();
            br.close();
            
            FileWriter fw = new FileWriter(new File(javaFile.getAbsolutePath().replace(".java", "-annotated.java")));
            BufferedWriter out = new BufferedWriter(fw);
            for(String s : lines)
                 out.write(s + "\n");
            out.flush();
            out.close();
        }
        catch (IOException exp)
        {
        	System.out.println("File not found...?");
        	exp.printStackTrace();
        }
        catch (Exception exp)
        {
        	exp.printStackTrace();
        }
    }
}