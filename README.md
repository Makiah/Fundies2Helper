# Fundies2Helper

Auto-generates class templates via `java.lang.reflect.*` and some hacky methods.

<img width="700px" src="screenshot.png"></img>

## Usage

1. Download the JAR from [here](https://github.com/Makiah/Fundies2Helper/releases/download/1.1/helper.jar).  
2. Double click on the file to run it, which should result in a pop up.  
3. (You might have to right click on the file and then open it if your computer says that it can't run code from unauthenticated developers)
4. Provide the GUI a .java file by clicking "Select new file to annotate".  

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
// In A
/* TEMPLATE: 
Fields: 
Methods: 
... this.someMethodA(int int String) ...  --String
... this.someOtherMethodB(String A) ...  --double
Methods of fields: 
*/
class A {
  public A() {}

  public String someMethodA(int a, int b, String c) {
    return "";
  }

  public double someOtherMethodB(String a, A b) {
    return 0.0;
  }
}

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
class B {
  A a;

  public B() {}

  public A someNewMethod(int b) {
    return new A();
  }
}
```
