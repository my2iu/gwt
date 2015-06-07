package elemental.javafx.util;

import java.util.WeakHashMap;

import netscape.javascript.JSObject;

public class GwtFxBridge {
  // Keeps track of JS objects that already have a Java wrapper (so that
  // you don't have two FxObjects representing the same JS Object).
  private static WeakHashMap<JSObject, FxObject> wrappedObjects = new WeakHashMap<>();
  
  public static FxObject jsoToFx(JSObject jso) {
    return new FxElementalBase(jso);
  }
  
  public static Object wrapJS(Object obj) {
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
}
