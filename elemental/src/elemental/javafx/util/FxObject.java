package elemental.javafx.util;

import com.google.gwt.core.client.JavaScriptObject;

import netscape.javascript.JSObject;

public class FxObject {
  protected JSObject obj;
  
  public FxObject(JSObject obj) {
    this.obj = obj;
  }
  
  public static JSObject unwrap(FxObject fxobj) {
    return fxobj.obj;
  }
  
  public static FxObject wrap(JSObject obj)
  {
    FxObject fxObj = new FxObject();
    fxObj.obj = obj;
    return fxObj;
  }
  
  
  // Temporary methods to help the transition from JavaScriptObject to FxObject.
  // (It allows FxObject to be used with a JavaScriptObject API temporarily)
  protected FxObject() {
    
  }
  
  @SuppressWarnings("unchecked")
  public final <T extends FxObject> T cast() {
    return (T) this;
  }

}
