package elemental.javafx.util;

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
      switch(className) {
        case "HTMLParagraphElement": return new FxParagraphElement(jso);
        default: System.out.println(className); break; 
      }
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
}
