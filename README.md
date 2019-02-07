# Fundies2Helper

Auto-generates class, interface, and method templates via `javassist`.  

<img width="700px" src="screenshot2.png"></img>

## Usage

1. Download the JAR from [here](https://github.com/Makiah/Fundies2Helper/releases/download/1.3/helper.jar).  
2. Open up Command Prompt or Terminal (depending on your OS) and `cd` to the correct directory.  
3. Type `java -jar helper.jar`
4. Provide the GUI a .java file by clicking "Select new file to annotate", and optionally select different options.  

## Before 
```
class A {
  public A() {}

  public String someMethodA(int a, int b, String c) {
    return "";
  }

  public double someOtherMethodB(String a, A b) {
    return 0.0;
  }
}

class B {
  A a;

  public B() {}

  public A someNewMethod(int b) {
    return new A();
  }
}
```

## After
```
class A {
  public A() {}
    // In A
    /* TEMPLATE: 
    Fields: 
    Methods: 
    ... this.someMethodA(int int String) ...  --String
    ... this.someOtherMethodB(String A) ...  --double
    Methods of fields: 
    */

  public String someMethodA(int a, int b, String c) {
    return "";
  }

  public double someOtherMethodB(String a, A b) {
    // In someOtherMethodB() 
    /* TEMPLATE
    Fields: 
    ... String ... 
    ... A ... 
    Returns: 
    ... double ...
    Methods of fields: 
    ... this.A.someMethodA(int int String) ...  --String
    ... this.A.someOtherMethodB(String A) ...  --double
    */
    return 0.0;
  }
}

class B {
  A a;

  public B() {}
    // In B
    /* TEMPLATE: 
    Fields: 
    ... this.a ...  --A
    Methods: 
    ... this.someNewMethod(int) ...  --A
    Methods of fields: 
    ... this.a.someMethodA(int int String) ...  --String
    ... this.a.someOtherMethodB(String A) ...  --double
    */

  public A someNewMethod(int b) {
    // In someNewMethod() 
    /* TEMPLATE
    Fields: 
    ... int ... 
    Returns: 
    ... A ...
    Methods of fields: 
    */
    return new A();
  }
}
```
