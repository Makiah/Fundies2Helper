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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;

public class AnnotationHelper
{
	private final File javaFile;
	private final boolean annotateMethods;

	public AnnotationHelper(File javaFile, boolean annotateMethods)
	{
		this.javaFile = javaFile;
		this.annotateMethods = annotateMethods;
	}

	/**
	 * Get line number of the current class.
	 * 
	 * @param c the CtClass to get the line number of
	 * @return the line number
	 */
	private int getClassLineNumber(CtClass c)
	{
		try
		{
			return c.getConstructors()[0].getMethodInfo().getLineNumber(0);
		} catch (Exception e)
		{
			System.out.println("Couldn't find constructor for " + c.getName() + " so ignoring template");
			return -1;
		}
	}

	/**
	 * Whether this class is useless to annotate.
	 * 
	 * @param c the class in question
	 * @return whether it's pointless
	 */
	private boolean isPointlessToAnnotate(CtClass c)
	{
		return c.isPrimitive() || c.getName().startsWith("java.") || c.getName().startsWith("tester.")
				|| c.getName().startsWith("javalib.");
	}

	/**
	 * Annotates a provided field
	 * 
	 * @param f
	 * @return
	 */
	private String annotate(CtField f) throws NotFoundException
	{
		return "... this." + f.getName() + " ...  --" + f.getType().getSimpleName();
	}

	/**
	 * Annotates a provided method with a given prefix.
	 * 
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

	/**
	 * Annotates a class method
	 * 
	 * @param m
	 * @return
	 * @throws NotFoundException
	 */
	private String annotateClassMethod(CtMethod m) throws NotFoundException
	{
		StringBuilder currentAnnotation = new StringBuilder("// In " + m.getName() + "() \n");
		currentAnnotation.append("/* TEMPLATE\nFields: \n");
		for (CtClass p : m.getParameterTypes())
			currentAnnotation.append("... " + p.getSimpleName() + " ... \n");

		currentAnnotation.append("Returns: \n... " + m.getReturnType().getSimpleName() + " ...\n");
		
		currentAnnotation.append("Methods of fields: \n");
		for (CtClass c : m.getParameterTypes())
		{
			// don't annotate primitives or Strings
			if (isPointlessToAnnotate(c))
				continue;

			// get all methods
			for (CtMethod m2 : c.getDeclaredMethods())
			{
				currentAnnotation.append(annotate(m2, c.getName() + ".") + "\n");
			}
		}
		
		currentAnnotation.append("*/");
		return currentAnnotation.toString();
	}

	/**
	 * Generate annotations for this class and the methods it has which contain
	 * complex data.
	 * 
	 * @param c                  the class to annotate
	 * @param currentAnnotations the current set of annotations (prevents
	 *                           duplicates)
	 * @return the new set of annotations
	 * @throws NotFoundException
	 */
	private Map<Integer, String> generateAnnotations(CtClass c, Map<Integer, String> currentAnnotations)
			throws NotFoundException
	{
		// Verify that this hasn't already been generated, return what already exists in
		// the map if so.
		for (Map.Entry<Integer, String> entry : currentAnnotations.entrySet())
		{
			if (getClassLineNumber(c) == entry.getKey())
			{
				return currentAnnotations;
			}
		}

		// Begin the process
		System.out.println("Generating annotations for " + c.getName());

		// Build and apply annotations
		StringBuilder currentAnnotation = new StringBuilder("// In " + c.getName() + "\n" + "/* TEMPLATE: \n");

		// Fields
		currentAnnotation.append("Fields: \n");
		for (CtField f : c.getDeclaredFields())
			currentAnnotation.append(annotate(f) + "\n");

		// Methods
		currentAnnotation.append("Methods: \n");
		for (CtMethod m : c.getDeclaredMethods())
		{
			currentAnnotation.append(annotate(m, "") + "\n");

			// Don't continue if we don't need to annotate methods
			if (!annotateMethods)
				continue;

			// Determine whether this method has complex data, indicating it needs to be
			// annotated in of itself.
			boolean shouldAnnotate = false;
			for (CtClass methodParam : m.getParameterTypes())
			{
				if (isPointlessToAnnotate(methodParam))
					continue;
				
				shouldAnnotate = true;
				break;
			}
			shouldAnnotate = shouldAnnotate || !isPointlessToAnnotate(m.getReturnType());
			if (shouldAnnotate)
			{
				// Annotate
				String methodAnnotation = annotateClassMethod(m);

				// Indent
				methodAnnotation = "    " + methodAnnotation;
				methodAnnotation = methodAnnotation.replace("\n", "\n    ");

				// Add to map
				currentAnnotations.put(m.getMethodInfo().getLineNumber(0) - 1, methodAnnotation);
			}
		}

		// Methods of fields
		currentAnnotation.append("Methods of fields: \n");
		for (CtField f : c.getDeclaredFields())
		{
			// don't annotate primitives or Strings
			if (isPointlessToAnnotate(f.getType()))
				continue;

			// get all methods
			for (CtMethod m : f.getType().getDeclaredMethods())
			{
				currentAnnotation.append(annotate(m, f.getName() + ".") + "\n");
			}
		}

		// Finalize current annotation
		currentAnnotation.append("*/");

		// Add indentation
		String finalAnnotation = ("    " + currentAnnotation.toString()).replace("\n", "\n    ");

		// Add this annotation to the map
		int classLineNumber = getClassLineNumber(c);
		if (classLineNumber >= 0)
			currentAnnotations.put(classLineNumber, finalAnnotation);
		return currentAnnotations;
	}

	/**
	 * Annotates a java file
	 * 
	 * @param includeMethodAnnotations optionally include method annotations
	 */
	public void annotateJavaFile()
	{
		Map<Integer, String> currentAnnotations = new TreeMap<Integer, String>(); // treemap for sorting

		/// Compile file for class names and generate annotations
		try
		{
			// Compile file
			String fileToCompile = javaFile.getAbsolutePath();
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			int compilationResult = compiler.run(null, null, null, fileToCompile);
			if (compilationResult == 0)
			{
				System.out.println("Compilation is successful");
			} else
			{
				System.out.println("Compilation Failed");
			}

			/// Load class
			// Convert File to a URL
			URL url = javaFile.getParentFile().toURI().toURL();
			URL[] urls = new URL[]
			{ url };

			// Create a new class loader with the directory
			ClassLoader cl = new URLClassLoader(urls);

			// Get all .class files for all classes
			File[] classFiles = javaFile.getParentFile().listFiles(new FilenameFilter()
			{
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
		} catch (Exception exp)
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
				int insertIndex = entry.getKey() + currentOffset;
				if (insertIndex < 0) // shouldn't happen
					continue;
				
				lines.add(insertIndex, entry.getValue());
				currentOffset++;
			}

			// Write the file out
			FileWriter fw = new FileWriter(new File(javaFile.getAbsolutePath().replace(".java", "-annotated.java")));
			BufferedWriter out = new BufferedWriter(fw);
			for (String s : lines)
				out.write(s + "\n");
			out.flush();
			out.close();
		} catch (IOException exp)
		{
			System.out.println("File not found...?");
			exp.printStackTrace();
		} catch (Exception exp)
		{
			exp.printStackTrace();
		}
	}
}