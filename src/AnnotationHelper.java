import javax.tools.*;

import javassist.ClassPool;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

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
import java.util.Map;
import java.util.TreeMap;
import java.util.List;

public class AnnotationHelper
{
	private final File javaFile;

    public AnnotationHelper(File javaFile)
    {
    	this.javaFile = javaFile;
    }
    
    /**
     * Get line number of the current class.  
     * @param c the CtClass to get the line number of
     * @return the line number
     */
    private int getClassLineNumber(CtClass c)
    {
    	try {
    		return c.getConstructors()[0].getMethodInfo().getLineNumber(0);
    	}
    	catch (Exception e)
    	{
    		System.out.println("Couldn't find constructor for " + c.getName() + " so ignoring template");
    		return -1;
    	}
    }
    
    /**
     * Whether this class is useless to annotate.  
     * @param c the class in question
     * @return whether it's pointless
     */
    private boolean isPointlessToAnnotate(Class<?> c)
    {
    	return c.isPrimitive() || c.isAssignableFrom(String.class) || 
    			c.getName().startsWith("java.") || c.getName().startsWith("tester.") ||
    			c.getName().startsWith("javalib.");
    }
    
    /**
     * Annotates a provided field 
     * @param f
     * @return
     */
    private String annotate(CtField f) throws NotFoundException
    {
		return "... this." + f.getName() + " ...  --" + f.getType().getSimpleName();
    }
    
    /**
     * Annotates a provided method with a given prefix.  
     * @param m
     * @param prefix
     * @return
     */
    private String annotate(CtMethod m, String prefix) throws NotFoundException
    {
		StringBuilder currentAnnotation = new StringBuilder("... this." + prefix + m.getName() + "(");
		for (CtClass p : m.getParameterTypes())
			currentAnnotation.append(p.getSimpleName() + " ");
		if (m.getParameterTypes().length > 0)
			currentAnnotation.setLength(currentAnnotation.length() - 1);
		currentAnnotation.append(") ...  --" + m.getReturnType().getSimpleName());
		return currentAnnotation.toString();
    }
    
    private Map<Integer, String> generateAnnotations(CtClass c, Map<Integer, String> currentAnnotations) throws NotFoundException
    {
    	// Verify that this hasn't already been generated, return what already exists in the map if so.  
    	for (Map.Entry<Integer, String> entry : currentAnnotations.entrySet()) 
		{
			if (getClassLineNumber(c) == entry.getKey())
			{
				return currentAnnotations;
			}
		}
    	
    	// Verify that this class has a constructor otherwise can't annotate
    	int classLineNumber = getClassLineNumber(c);
    	if (classLineNumber < 0)
    		return currentAnnotations;
    	
    	// Begin the process
    	System.out.println("Generating annotations for " + c.getName());
    	
    	// Build and apply annotations
		StringBuilder currentAnnotation = new StringBuilder(
				"// In " + c.getName() + "\n" + 
				"/* TEMPLATE: \n");

		// Fields
		currentAnnotation.append("Fields: \n");
		for (CtField f : c.getDeclaredFields()) 
			currentAnnotation.append(annotate(f) + "\n");

		// Methods
		currentAnnotation.append("Methods: \n");
		for (CtMethod m : c.getDeclaredMethods()) 
		{
			currentAnnotation.append(annotate(m, "") + "\n");
			
			// Determine whether this method has complex data, indicating it needs to be annotated in of itself.  
			
		}

		// Methods of fields
		currentAnnotation.append("Methods of fields: \n");
		for (CtField f : c.getDeclaredFields()) 
		{
			// don't annotate primitives or Strings
			if (isPointlessToAnnotate(f.getClass()))
				continue; 

			// get all methods
			for (CtMethod m : f.getType().getDeclaredMethods()) 
			{
				currentAnnotation.append(annotate(m, f.getName() + ".") + "\n");
			}
		}

		// Finalize current annotation
		currentAnnotation.append("*/");
		
		// Add this annotation to the map
		currentAnnotations.put(classLineNumber, currentAnnotation.toString());
		return currentAnnotations;
    }

    public void annotateClass(boolean includeMethodAnnotations)
    {
		Map<Integer, String> currentAnnotations = new TreeMap<Integer, String>(); // treemap for sorting

        /// Compile file for class names and generate annotations
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
	            ClassPool classPool = ClassPool.getDefault();
	            classPool.appendClassPath(new LoaderClassPath(cl));
	            currentAnnotations = generateAnnotations(
	            		classPool.getCtClass(f.getName().substring(0, f.getName().length() - 6)), currentAnnotations);
	            
	            // Compiled file no longer necessary
				f.delete();
			}
        }
        catch (Exception exp)
        {
            exp.printStackTrace();
        }
        
        // Print map to user
        for (Map.Entry<Integer, String> entry : currentAnnotations.entrySet()) 
		{
        	System.out.println("{\n" + entry.getKey() + ", \n" + entry.getValue() + "}\n");
		}

		// Now write the annotations to the file
        try 
        {
        	// Read the file lines
            FileReader fr = new FileReader(javaFile);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            List<String> lines = new ArrayList<String>();
            while ((line = br.readLine()) != null) 
            {
                lines.add(line);
            }
            fr.close();
            br.close();
            
            // Add all current annotations by line number
            int currentOffset = 0;
            for (Map.Entry<Integer, String> entry : currentAnnotations.entrySet()) 
    		{
            	lines.add(entry.getKey() + currentOffset, entry.getValue());
            	currentOffset++;
    		}
            
            // Write the file out
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