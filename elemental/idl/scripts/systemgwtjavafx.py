#!/usr/bin/python
# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

"""This module providesfunctionality for systems to generate
Elemental interfaces from the IDL database."""

import pdb
import os
import json
import systembaseelemental
from generator_java import *


class ElementalJavaFxSystem(systembaseelemental.SystemElemental):

  def __init__(self, templates, database, emitters, output_dir):
    super(ElementalJavaFxSystem, self).__init__(
        templates, database, emitters, output_dir)
    self._dart_interface_file_paths = []

  def InterfaceGenerator(self,
                         interface,
                         common_prefix,
                         super_interface_name,
                         source_filter):
    """."""

    module = getModule(interface.annotations)
    if super_interface_name is not None:
      interface_name = super_interface_name
    else:
      interface_name = interface.id

    template_file = 'javafx_impl_%s.darttemplate' % interface_name
    template = self._templates.TryLoad(template_file)
    if not template:
      template = self._templates.Load('javafx_impl.darttemplate')

    if interface_name in self._mixins or interface_name.endswith("Callback") or interface_name.endswith('Handler'):
      return NullInterfaceGenerator(module, self._database,
        interface, None,
        template,
        common_prefix, super_interface_name,
        source_filter)

    dart_interface_file_path = self._FilePathForElementalInterface(module, interface_name)

    self._dart_interface_file_paths.append(dart_interface_file_path)

    dart_interface_code = self._emitters.FileEmitter(dart_interface_file_path)

    return ElementalInterfaceGenerator(module, self._database,
        interface, dart_interface_code,
        template,
        common_prefix, super_interface_name,
        source_filter, self._mixins)

  def ProcessCallback(self, interface, info):
    pass


  def _FilePathForElementalInterface(self, module, interface_name):
    """Returns the file path of the Dart interface definition."""
    return os.path.join(self._output_dir, 'src', 'elemental', "javafx", module,
                        'Fx%s.java' % interface_name)


# ------------------------------------------------------------------------------
# Used to suppress generation of JSO classes that are not needed
class NullInterfaceGenerator(systembaseelemental.ElementalBase):
  def __init__(self, module, database, interface, emitter, template,
               common_prefix, super_interface, source_filter):
    pass

  def StartInterface(self):
    pass
  def AddOperation(self, x, inherited):
    pass

  def AddIndexer(self, x):
    pass

  def AddConstant(self, x):
    pass

  def AddAttribute(self, x, y, inheritedGetter, inheritedSetter):
    pass

  def FinishInterface(self):
    pass


class ElementalInterfaceGenerator(systembaseelemental.ElementalBase):
  """Generates Elemental Interface definition for one DOM IDL interface."""

  def __init__(self, module, database, interface, emitter, template,
               common_prefix, super_interface, source_filter, mixins):
    """Generates Dart code for the given interface.

    Args:
      interface -- an IDLInterface instance. It is assumed that all types have
        been converted to Dart types (e.g. int, String), unless they are in the
        same package as the interface.
      common_prefix -- the prefix for the common library, if any.
      super_interface -- the name of the common interface that this interface
        implements, if any.
      source_filter -- if specified, rewrites the names of any superinterfaces
        that are not from these sources to use the common prefix.
    """
    super(self.__class__, self).__init__()
    self._module = module
    self._database = database
    self._interface = interface
    self._emitter = emitter
    self._template = template
    self._common_prefix = common_prefix
    self._super_interface = super_interface
    self._source_filter = source_filter
    self._mixins = mixins

    current_dir = os.path.dirname(__file__)
    self._docdatabase = json.load(open(os.path.join(current_dir, '..', 'docs/database.json')))


  def addImport(self, imports, typeid):
      # skip primitive types that are all lowercase first letter
      etype = DartType(typeid)

      if '<' not in typeid and etype[0].isupper():
        rawtype = etype.split('<')[0]

        if rawtype in ['Indexable', 'Settable', 'Mappable']:
          pmodule = 'util'
          imports['import elemental.javafx.%s.%s;\n' % (pmodule, 'Fx' + rawtype)]=1
          imports['import elemental.%s.%s;\n' % (pmodule, rawtype)]=1
        elif etype not in java_lang and self._database.HasInterface(typeid):
          pinterface = self._database.GetInterface(typeid)
          pmodule = getModule(pinterface.annotations)
          if pmodule != self._module and not rawtype.endswith("Callback") and not rawtype.endswith("Handler") and not rawtype == 'EventTarget':
            imports['import elemental.javafx.%s.%s;\n' % (pmodule, 'Fx' + rawtype)]=1
          imports['import elemental.%s.%s;\n' % (pmodule, rawtype)]=1

  def StartInterface(self):
    if self._super_interface:
      typename = self._super_interface
    else:
      typename = self._interface.id


    implements = []
    implements_raw = {}
    extends = ''
    suppressed_extends = []
    imports = {}

    alreadyImplemented = {}        
    for p in self._interface.parents:
      self.getImplements(alreadyImplemented, p)

    for attr in self._interface.attributes:
      self.addImport(imports, attr.type.id)

    for oper in self._interface.operations:
      self.addImport(imports, oper.type.id)
      for arg in oper.arguments:
        self.addImport(imports, arg.type.id)

    for parent in self._interface.parents:
      if not parent.type.id in alreadyImplemented or parent.type.id == 'ElementalMixinBase':
        self.addImport(imports, parent.type.id)
      # TODO(vsm): Remove source_filter.
        rawtype = DartType(parent.type.id).split('<')[0]
        if rawtype == 'Object':
          continue
        if MatchSourceFilter(self._source_filter, parent) and DartType(parent.type.id) != 'Object' and rawtype not in implements_raw:
          # Parent is a DOM type.
          implements.append(DartType(parent.type.id))
          implements_raw[rawtype]=1

#TODO(cromwellian) add in Indexable/IndexableInt/IndexableNumber
#      elif '<' in parent.type.id:
        # Parent is a Dart collection type.
        # TODO(vsm): Make this check more robust.
#        extends.append(parent.type.id)
#      else:
#        suppressed_extends.append('%s.%s' %
#                                  (self._common_prefix, parent.type.id))

    comment = ' extends'

    implements_str = ''

    # the Mixin base class is special, it extends JSO
    if self._interface.id == 'ElementalMixinBase':
      extends = ' extends FxElementalBase'
      filtered_mixins = []
      for mixin in self._mixins:
        filtered_mixins.append(DartType(mixin))
      implements.extend(filtered_mixins)
    else:
      # default, every type extends the Mixin base
      extends = ' extends FxElementalMixinBase'
    
    rawtype = DartType(self._interface.id).split('<')[0]
    if rawtype not in implements_raw and rawtype != 'Object':
      implements.insert(0, DartType(self._interface.id))
    self.addImport(imports, self._interface.id)

    # parents[0] is the implementing superclass, if exists, but not a mixin, reference its JSO impl
    if len(self._interface.parents) > 0 and not self._interface.parents[0].type.id in self._mixins:
      extends = ' extends Fx' + DartType(self._interface.parents[0].type.id)
      self.addImport(imports, self._interface.parents[0].type.id)

    if implements:
      implements_str += ' implements ' + ', '.join(implements)
      comment = ','

    factory_provider = None
    constructor_info = AnalyzeConstructor(self._interface)

    # TODO(vsm): Add appropriate package / namespace syntax.
    (self._members_emitter,
     self._top_level_emitter) = self._emitter.Emit(
         self._template + '$!TOP_LEVEL',
         ID='Fx' + typename,
         IMPLEMENTS=implements_str,
         EXTENDS=extends, PACKAGE='elemental.javafx.' + self._module,
         IMPORTS = ''.join(imports.keys()))

# TODO(cromwellian) auto-generate factory classes?

#    if constructor_info:
#      self._members_emitter.Emit(
#          '\n'
#          '  $CTOR($PARAMS);\n',
#          CTOR=typename,
#          PARAMS=constructor_info.ParametersInterfaceDeclaration());

#    element_type = MaybeTypedArrayElementType(self._interface)
#    if element_type:
#      self._members_emitter.Emit(
#          '\n'
#          '  $CTOR(int length);\n'
#          '\n'
#          '  $CTOR.fromList(List<$TYPE> list);\n'
#          '\n'
#          '  $CTOR.fromBuffer(ArrayBuffer buffer,'
#                            ' [int byteOffset, int length]);\n',
#          CTOR=self._interface.id,
#          TYPE=DartType(element_type))


  def FinishInterface(self):
     # add all create* method implementations to Document
     if self._interface.id == 'Document':
       for iface in self._database.GetInterfaces():
         if iface.id.endswith("Element"):
           # try to determine tag name from interface name
           elementName = iface.id;
           elementName = elementName[0:len(elementName) - len("Element")].lower()
           # tablecaption -> <caption>
           if elementName == 'tablecaption':
             elementName = 'caption'
           # SVG is special, it needs createElementNS, strip off prefix
           if elementName.startswith("svg"):
             elementName = elementName[3:]
             # special case, SVGElement is a root class, not a createable element
             if elementName != '':
               callName = DartType(iface.id)
               # SVGSVGElement -> createSVGElement with tag as <svg>
               if elementName == 'svg':
                 callName = 'SVGElement'
               self._members_emitter.Emit('\n  public final Fx$TYPE create$CALL() {\n    return createSvgElement("$ELEMENT").cast();\n  }\n',
                                          TYPE=DartType(iface.id),
                                          CALL = callName,
                                          ELEMENT=elementName)
           elif elementName != '':
               self._members_emitter.Emit('\n  public final Fx$TYPE create$TYPE() {\n    return createElement("$ELEMENT").cast();\n  }\n',
                                          TYPE=DartType(iface.id),
                                          ELEMENT=elementName)
     if self._interface.id == 'Window':
       for iface in self._database.GetInterfaces():
         element_type = MaybeTypedArrayElementType(iface)
         if element_type:
           self._members_emitter.Emit(
           '\n'
           '  public final Fx$TYPE new$CTOR(int length) { return Fx$TYPE.wrap(((JSObject)obj.eval("(function(arg1){ return new $TYPE(arg1); })")).call("call", new Object[] {obj, length})); }\n'
           '\n'
           '  public final Fx$TYPE new$CTOR(IndexableNumber list) { return Fx$TYPE.wrap(((JSObject)obj.eval("(function(arg1){ return new $TYPE(arg1); })")).call("call", new Object[] {obj, GwtFxBridge.unwrapToJs(list)})); }\n'
           '\n'
           '  public final Fx$TYPE new$CTOR(ArrayBuffer buffer,'
           ' int byteOffset, int length) { return Fx$TYPE.wrap(((JSObject)obj.eval("(function(arg1,arg2,arg3){ return new $TYPE(arg1,arg2,arg3); })")).call("call", new Object[] {obj, GwtFxBridge.unwrapToJs(buffer), byteOffset, length})); }\n',
           CTOR=iface.id,
           TYPE=iface.id)
         constructor_info = AnalyzeConstructor(iface)
         if constructor_info:
           args = constructor_info.ParametersAsJavaFxArgumentList(JavaFxUnwrapJs, self._database);
           chainedArgs = ','.join(map(lambda x: 'arg' + str(x), range(len(constructor_info.param_infos))))
           self._members_emitter.Emit(
             '\n'
             '  public final Fx$TYPE new$CTOR($PARAMS) { return Fx$TYPE.wrap(((JSObject)obj.eval("(function($CHAINEDARGS){ return new $TYPE($CHAINEDARGS); })")).call("call", new Object[] {obj, $ARGS})); }\n',
             TYPE=iface.id,
             CTOR=iface.id,
             JSTYPE=JsType(iface.id),
             PARAMS=constructor_info.ParametersInterfaceDeclaration(),
             CHAINEDARGS=chainedArgs,
             ARGS=args);


  def AddConstant(self, constant):
    pass

  def AddAttribute(self, getter, setter, inheritedGetter, inheritedSetter):
    # you can't override methods in a JSO superclass
    if getter and not inheritedGetter:
      if getter.type.id == 'EventListener':
        field_template = 'obj.getMember("%s")' % getter.id
        self._members_emitter.Emit('\n  public final $TYPE $NAME() {\n    return $GETTER;\n  }\n',
                                   NAME=getterName(getter),
                                   TYPE=TypeOrVar(DartType(getter.type.id),
                                                  getter.type.id),
                                   FIELD=getter.id,
                                   GETTER=JavaFxWrapJs(DartType(getter.type.id), field_template))
      else:
#        field = 'this.$FIELD'
#        if getter.id in self.reserved_keywords:
#          field_template = "this['$FIELD']"
#        else:
#          field_template = "this.$FIELD"
        field_template = 'obj.getMember("%s")' % getter.id
        self._members_emitter.Emit('\n  public final $TYPE $NAME() {\n    return $GETTER;\n  }\n',
                                   NAME=getterName(getter),
                                   TYPE=JavaFxTypeOrVar(DartType(getter.type.id), self._mixins),
                                   FIELD=getter.id,
                                   GETTER=JavaFxWrapJs(JavaFxTypeOrVar(DartType(getter.type.id), self._mixins), field_template))
    if setter and not inheritedSetter:
        if setter.type.id == 'EventListener':
          self._members_emitter.Emit('\n  public final void $NAME($TYPE listener) {\n    obj.setMember("$FIELD", GwtFxBridge.entryPoint(obj, listener, "handleEvent", elemental.events.Event.class));\n  }',
                                     NAME=setterName(setter),
                                     TYPE=TypeOrVar(DartType(setter.type.id),
                                                    setter.type.id),
                                     FIELD=setter.id)
        else:
          #if setter.id in self.reserved_keywords:
          #  field_template = "this['$FIELD']"
          #else:
          #  field_template = "this.$FIELD"
          field_param = "param_%s" % setter.id
          self._members_emitter.Emit('\n  public final void $NAME($TYPE param_$FIELD) {\n    obj.setMember(\"$FIELD\", $VAL);\n  }\n',
                                     NAME=setterName(setter),
                                     TYPE=TypeOrVar(DartType(setter.type.id)),
                                     FIELD=setter.id,
                                     VAL=JavaFxUnwrapJs(JavaFxTypeOrVar(DartType(setter.type.id), self._mixins), field_param))

  def AddIndexer(self, element_type):
    # Interface inherits all operations from List<element_type>.
    pass

  def AddOperation(self, info, inherited):
    """
    Arguments:
      info - an OperationInfo object
      operations - contains the overloads, one or more operations with the same
        name.
    """
    # you can't override methods on a JSO superclass
    if inherited:
      return
    # implemented on mixin base template (hack)
    if info.name == 'addEventListener' or info.name == 'removeEventListener':
      return

    get_attrs = []
    set_attrs = []
    for attr in self._interface.attributes:
      get_attrs.append(getterName(attr))
      set_attrs.append('set%s' % ucfirst(attr.id))

    if info.name in get_attrs or info.name in set_attrs:
      return
    body = ''
    if info.type_name != 'void':
      body += 'return '
#    if op.is_fc_deleter:
#            w('delete ')
#     if info.id is None:
#            w('this[')
#        else:
    args = info.ParametersAsJavaFxArgumentList(JavaFxUnwrapJs, self._database)
    call = "obj.call(\"%s\", new Object[]{%s}" % (info.name, args)
#    if info.name in self.reserved_keywords:
#      body += "this['%s'](%s" % (info.name, args)
#    else:
#      body += 'this.%s(%s' % (info.name, args)


    #if op.id is None:
#            w('];\n')
#        else:
    call += ')'
    if info.type_name != 'void':
      call = JavaFxWrapJs(JavaFxTypeOrVar(info.type_name, self._mixins), call)
    body += "%s;" % call

    self._members_emitter.Emit('\n  public final $TYPE $NAME($PARAMS) {\n    $BODY\n  }\n',
                               TYPE=JavaFxTypeOrVar(info.type_name, self._mixins),
                               NAME=self.fixReservedKeyWords(info.name),
                               PARAMS=info.ParametersInterfaceDeclaration(),
                               BODY=body)

  def AddStaticOperation(self, info, inherited):
    pass

  # Interfaces get secondary members directly via the superinterfaces.
  def AddSecondaryAttribute(self, interface, getter, setter):
    pass

  def AddSecondaryOperation(self, interface, attr):
    pass


def ucfirst(string):
  """Upper cases the 1st character in a string"""
  if len(string):
    return '%s%s' % (string[0].upper(), string[1:])
  return string

def getterName(attr):
  name = DartDomNameOfAttribute(attr)
  if attr.type.id == 'boolean':
    if name.startswith('is'):
      name = name[2:]
    return 'is%s' % ucfirst(name)
  return 'get%s' % ucfirst(name)

def setterName(attr):
  name = DartDomNameOfAttribute(attr)
  return 'set%s' % ucfirst(name)


def getModule(annotations):
  htmlaliases = ['audio', 'webaudio', 'inspector', 'offline', 'p2p', 'window', 'websockets', 'threads', 'view', 'storage', 'fileapi']

  module = "dom"
  if 'WebKit' in annotations and 'module' in annotations['WebKit']:
    module = annotations['WebKit']['module']
  if module in  htmlaliases:
    return "html"
  if module == 'core':
    return 'dom'
  return module

java_lang = ['Object', 'String', 'Exception', 'DOMTimeStamp', 'DOMString']


#--------------------------------------------------------------------
# Special generator for creating some utility code

class ElementalJavaFxWrapUtil(systembaseelemental.SystemElemental):

  def __init__(self, templates, database, emitters, output_dir):
    super(ElementalJavaFxWrapUtil, self).__init__(
        templates, database, emitters, output_dir)
    self._dart_interface_file_paths = []
    self._interfaces = []

  def InterfaceGenerator(self,
                         interface,
                         common_prefix,
                         super_interface_name,
                         source_filter):
    """."""
    if super_interface_name is not None:
      interface_name = super_interface_name
    else:
      interface_name = interface.id

    self._interfaces.append((interface, interface_name))
    return None

    dart_interface_file_path = self._FilePathForElementalInterface(module, interface_name)

    self._dart_interface_file_paths.append(dart_interface_file_path)

    dart_interface_code = self._emitters.FileEmitter(dart_interface_file_path)

    return ElementalInterfaceGenerator(module, self._database,
        interface, dart_interface_code,
        template,
        common_prefix, super_interface_name,
        source_filter, self._mixins)

  def ProcessCallback(self, interface, info):
    pass


  def _FilePathForElementalInterface(self, module, interface_name):
    """Returns the file path of the Dart interface definition."""
    return os.path.join(self._output_dir, 'src', 'elemental', "javafx", module,
                        'Fx%s.java' % interface_name)

  def Finish(self):
    template = self._templates.Load('javafx_impl_FxJavaWrap.darttemplate')
    dart_interface_file_path = self._FilePathForElementalInterface('util', 'JavaWrap')
    emitter = self._emitters.FileEmitter(dart_interface_file_path)
    
    cases_emitter = emitter.Emit(template)
    for (interface, interface_name) in self._interfaces:
      module = getModule(interface.annotations)
      if interface_name in self._mixins or interface_name.endswith("Callback") or interface_name.endswith('Handler'):
        continue
      cases_emitter.Emit('\n      case "$NAME": return new $FXNAME(obj);',
          NAME=interface.raw_js_name,
          FXNAME='elemental.javafx.%s.Fx%s' % (module, interface.id))
