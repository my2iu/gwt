package elemental.javafx.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gwt.core.shared.GwtIncompatible;

import netscape.javascript.JSObject;

@GwtIncompatible
public class GwtFxBridge {
  // Keeps track of JS objects that already have a Java wrapper (so that
  // you don't have two FxObjects representing the same JS Object).
  private static WeakHashMap<JSObject, FxObject> wrappedObjects = new WeakHashMap<>();
  
  private static String extractJsClassName(JSObject jso) {
    // TODO(iu): Is there a better way of doing this?
    
    // Try to figure out what sort of object we have
    String result = (String)((JSObject)jso.eval("Object.prototype.toString")).call("call", new Object[]{jso});
    Matcher classNameMatcher = Pattern.compile("\\[object\\s+(.*)\\]").matcher(result);
    if (classNameMatcher.find()) {
      return classNameMatcher.group(1);
    }
    return null;
    
//    System.out.println(result);
//    Object constructor = jso.getMember("constructor");
//    if (constructor != null && constructor instanceof JSObject) {
//      Object nameMember = ((JSObject)constructor).getMember("name");
//      if (nameMember != null && nameMember instanceof String) {
//        String name = (String)nameMember;
//        System.out.println(name);
//      }
//    }
  }
  
  private static FxObject jsoToFx(JSObject jso, String className) {
    // TODO(iu): Is there a better way of doing this?
    
    // Try to figure out what sort of object we have
    if (className != null) {
      return FxJavaWrap.wrapJs(className, jso);
    }
    
    return new FxElementalBase(jso);
  }
  
  /**
   * Takes an object from the JS interpreter and wraps it with an
   * appropriate Java object if necessary
   */
  public static Object wrapJs(Object obj) {
    if (obj == null) {
      return null;
    } else if (obj instanceof JSObject) {
      JSObject jso = (JSObject)obj;
      FxObject wrapped = wrappedObjects.get(jso);
      if (wrapped == null) {
        String inferredClassName = extractJsClassName(jso);
        if ("Function".equals(inferredClassName)) {
          // Check if it a wrapped callback
          Object javaFxCallback = jso.getMember("javaFxCallback");
          if (javaFxCallback != null && !"undefined".equals(javaFxCallback)) {
            return javaFxCallback;
          }
        }
        wrapped = jsoToFx(jso, inferredClassName);
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
  
  /**
   * Takes a Java object and converts it for use in a JS interpreter. If the
   * Java object is simply a wrapped JS object, it will be unwrapped and the 
   * JS object returned.
   */
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
    return obj;
  }
  
  public static class CallbackRedirector {
    public Object call(Object javaCallback, Method method, JSObject jsArgs) {
      Object[] args = new Object[method.getParameterCount()];
      for (int n = 0; n < method.getParameterCount(); n++)
        args[n] = wrapJs(jsArgs.getSlot(n));
      try {
        return unwrapToJs(method.invoke(javaCallback, args));
      } catch (IllegalAccessException | IllegalArgumentException
          | InvocationTargetException e) {
        throw new IllegalArgumentException("Call to method failed", e);
      }
    }
  }
  
  /**
   * Provides an object that JavaScript code can call to trigger a callback
   * in Java-land.
   */
  private static CallbackRedirector redirector = new CallbackRedirector(); 

  /**
   * Creates a JS function that will call the Java callback object.
   * 
   * @param method method to be called on the callback object
   * @param callback object where that should be callable from JavaScript 
   * @param scope the JavaScript scope where the callback will be placed.
   *     This might be needed if there are multiple browsers with multiple JavaScript
   *     interpreters running. I'm not sure if JavaFx allows this though. 
   * @return
   */
  public static JSObject entryPoint(JSObject scope, Object callback, String method, Class<?>...methodParams)
  {
    try {
      Method m = callback.getClass().getMethod(method, methodParams);
      m.setAccessible(true);
      Object entryPointCreator = scope.eval("(function(redirector, callback, method) { var fn = function() { redirector.call(callback, method, arguments); }; fn.javaFxCallback = callback; return fn; })");
      Object entryPoint = ((JSObject)entryPointCreator).call("call", new Object[] {null, redirector, callback, m});
      return (JSObject)entryPoint;
    } catch (NoSuchMethodException | SecurityException e) {
      throw new IllegalArgumentException("Cannot find callback method", e);
    }
  }

  /**
   * Calls new on a JS constructor function to create a new object.
   */
  public static JSObject newJsObject(JSObject constructor, Object...args) {
    // TODO(iu): This seems to work fine with some objects, like Date, but not with others, like Uint8Array (Webkit's Uint8Array does not seem to be a function)
    JSObject constructorBinder = (JSObject)
        constructor.eval("(function(constructor) { var f = constructor.bind.apply(constructor, arguments); return new f(); })");
    Object[] shiftedArgs = new Object[args.length + 2];
    for (int n = 0; n < args.length; n++) {
      shiftedArgs[n + 2] = args[n];
    }
    shiftedArgs[0] = constructor;
    shiftedArgs[1] = constructor;
    
    return (JSObject)constructorBinder.call("call", shiftedArgs);
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
