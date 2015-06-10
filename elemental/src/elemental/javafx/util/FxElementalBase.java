/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package elemental.javafx.util;

import netscape.javascript.JSObject;
import elemental.util.Indexable;
import elemental.util.IndexableInt;
import elemental.util.IndexableNumber;
import elemental.util.Mappable;
import elemental.util.Settable;
import elemental.util.SettableInt;
import elemental.util.SettableNumber;

/**
 * All Elemental classes must extend this base class, mixes in support for
 * Indexable, Settable, Mappable.
 */
// TODO (cromwellian) add generic when JSO bug in gwt-dev fixed
// TODO(iu): do proper wrapping and unwrapping of JSObjects 
public class FxElementalBase extends FxObject implements Mappable, 
    Indexable, IndexableInt, IndexableNumber, Settable, SettableInt, SettableNumber {

  protected FxElementalBase() {}
  public FxElementalBase(JSObject obj) { super(obj); }
  
  public final Object /* T */ at(int index) {
    return GwtFxBridge.wrapJs(obj.getSlot(index));
  }

  public final double numberAt(int index) {
    return ((Number)obj.getSlot(index)).doubleValue();
  }

  public final int intAt(int index) {
    return ((Number)obj.getSlot(index)).intValue();
  }

  public final int length() {
    return ((Number)obj.getMember("length")).intValue();
  }

  public final void setAt(int index, Object /* T */ value) {
    obj.setSlot(index, GwtFxBridge.unwrapToJs(value));
  }

  public final void setAt(int index, double value) {
    obj.setSlot(index, value);
  }

  public final void setAt(int index, int value) {
    obj.setSlot(index, value);
  }

  public final Object /* T */ at(String key) {
    return GwtFxBridge.wrapJs(obj.getMember(key));
  }

  public final int intAt(String key) {
    return ((Number)obj.getMember(key)).intValue();
  }

  public final double numberAt(String key) {
    return ((Number)obj.getMember(key)).doubleValue();
  }

  public final void setAt(String key, Object /* T */ value) {
    obj.setMember(key, GwtFxBridge.unwrapToJs(value));
  }

  public final void setAt(String key, int value) {
    obj.setMember(key, value);
  }

  public final void setAt(String key, double value) {
    obj.setMember(key, value);
  }
}
