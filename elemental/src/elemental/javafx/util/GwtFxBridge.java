package elemental.javafx.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import elemental.javafx.html.FxParagraphElement;
import netscape.javascript.JSObject;

public class GwtFxBridge {
  // Keeps track of JS objects that already have a Java wrapper (so that
  // you don't have two FxObjects representing the same JS Object).
  private static WeakHashMap<JSObject, FxObject> wrappedObjects = new WeakHashMap<>();
  
  public static FxObject jsoToFx(JSObject jso) {
    // TODO(iu): Is there a better way of doing this?
    
    // Try to figure out what sort of object we have
    String result = (String)((JSObject)jso.eval("Object.prototype.toString")).call("call", new Object[]{jso});
    Matcher classNameMatcher = Pattern.compile("\\[object\\s+(.*)\\]").matcher(result);
    if (classNameMatcher.find()) {
      String className = classNameMatcher.group(1);
      return FxJavaWrap.wrapJs(className, jso);
    }
    
//    System.out.println(result);
//    Object constructor = jso.getMember("constructor");
//    if (constructor != null && constructor instanceof JSObject) {
//      Object nameMember = ((JSObject)constructor).getMember("name");
//      if (nameMember != null && nameMember instanceof String) {
//        String name = (String)nameMember;
//        System.out.println(name);
//      }
//    }
    return new FxElementalBase(jso);
  }
  
  public static Object wrapJs(Object obj) {
    if (obj == null) {
      return null;
    } else if (obj instanceof JSObject) {
      JSObject jso = (JSObject)obj;
      FxObject wrapped = wrappedObjects.get(jso);
      if (wrapped == null) {
        wrapped = jsoToFx(jso);
        wrappedObjects.put(jso, wrapped);
      }
      return wrapped;
    } else if (obj instanceof Number) {
      return obj;
    } else if (obj instanceof Boolean) {
      return obj;
    } else if ("undefined".equals(obj)) {
      return null;
    } else if (obj instanceof String) {
      return obj;
    }
    return null;
  }
  
  public static Object unwrapToJs(Object obj) {
    if (obj == null) {
      return null;
    } else if (obj instanceof FxObject) {
      return ((FxObject)obj).obj;
    } else if (obj instanceof Number 
        || obj instanceof String 
        || obj instanceof Boolean) {
      return obj;
    }
    return null;
  }
  
  public static JSObject entryPoint(String method, Object callback)
  {
    return null;
  }
  
  /**
   * Takes a JavaScript object that is already wrapped in an  FxObject 
   * and rewraps it so that it will have a certain interface. In theory, this
   * method shouldn't be necessary if 
   */
  public static <E> E cast(Object obj, Class<E> intf) {
    // TODO(iu): Make a version of this that works with compiled GWT so that the
    //     same code can be reused with JavaScriptObject and FxObject
    if (!intf.getName().startsWith("elemental."))
      throw new IllegalArgumentException("Can only cast to elemental interfaces");
    if (!(obj instanceof FxObject))
      throw new IllegalArgumentException("Expecting an FxObject");
    
    // Change the package prefix from elemental. to elemental.javafx. and add Fx to the class name
    Matcher match = Pattern.compile("^elemental(.*)[.]([^.]*)$").matcher(intf.getName());
    if (!match.find())
      throw new IllegalArgumentException("Cannot find matching FxObject for the elemental interface");
    String expectedFxClassName = "elemental.javafx" + match.group(1) + ".Fx" + match.group(2);
    
    // Take the JSObject out and wrap it using an FxObject type that implements the desired interface
    try {
      Class<?> fxClass = (Class<?>)Class.forName(expectedFxClassName);
      Constructor<?> constructor = fxClass.getConstructor(JSObject.class);
      return (E)constructor.newInstance(((FxObject)obj).obj);
    } catch (InstantiationException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException 
        | ClassNotFoundException | NoSuchMethodException | SecurityException e) {
      throw new IllegalArgumentException("Cannot find matching FxObject for the elemental interface", e);
    }
  }
}
