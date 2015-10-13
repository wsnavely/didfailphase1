#!/usr/local/bin/python2.7
# encoding: utf-8
'''
cert.didfail.phase2 -- Runs the second phase of the Didfail analysis.
'''

from argparse import ArgumentParser
from argparse import RawDescriptionHelpFormatter
import xml.etree.ElementTree as ET
import logging
import json
import os
import re
import sys
from __builtin__ import classmethod
import collections
from collections import OrderedDict
from collections import namedtuple

# Model

ANDROID_PFX = "{http://schemas.android.com/apk/res/android}"
    
ComponentId = namedtuple('ComponentId', ['package', 'name'])

class ComponentType(object):
    Activity = "Activity"
    Service = "Service"
    BroadcastReceiver = "BroadcastReceiver"
    ContentProvider = "ContentProvider"
    
    @classmethod
    def from_sink_method_name(cls, method_name):
        result = None
        if "startActivity" in method_name:
            result = ComponentType.Activity
        elif "startService" in method_name:
            result = ComponentType.Service
        elif "peekService" in method_name:
            result = ComponentType.Service
        elif "bindService" in method_name:
            result = ComponentType.Service
        elif "sendBroadcast" in method_name:
            result = ComponentType.BroadcastReceiver
        return result

class Component(object):
    def __init__(self):
        self.cid = None
        self.ctype = None
        self.filters = []
    
    @classmethod
    def from_manifest_xml(cls, root):
        package = root.attrib.get("package")
        
        # Intent filters can be used with Activities as well as Activity-aliases
        # Alias is used to have a different label for the same activity
        all_components = (
            root.findall(".//activity") + 
            root.findall(".//activity-alias") + 
            root.findall(".//service") + 
            # root.findall(".//provider") +
            root.findall(".//receiver") + 
            [])

        for component in all_components:
            comp_name = None
            comp_type = None
            
            def read_component_name(xml_attrib):
                ret = component.attrib[ANDROID_PFX + xml_attrib]
                if ret.startswith("."):
                    ret = package + ret
                return ret
            
            # Component name for an Activity is stored as the "name" attribute
            if component.tag == "activity":
                comp_name = read_component_name("name")
                comp_type = ComponentType.Activity
            # Component name for an Activity-alias is stored as the "targetActivity" attribute
            elif component.tag == "activity-alias":
                comp_name = read_component_name("targetActivity")
                comp_type = ComponentType.Activity
                # TODO glo.act_alias_to_targ[component.attrib[android_pfx + "name"]] = comp_name
            elif component.tag == "service":
                comp_name = read_component_name("name")
                comp_type = ComponentType.Service
            elif component.tag == "provider":
                comp_name = read_component_name("name")
                comp_type = ComponentType.ContentProvider
            elif component.tag == "receiver":
                comp_name = read_component_name("name")
                comp_type = ComponentType.BroadcastReceiver
            else:
                raise ValueError("Unrecognized component tag: " + component.tag)
       
            result = Component()
            result.cid = ComponentId(package, comp_name)
            result.ctype = comp_type
            for intent_node in component.findall(".//intent-filter"):
                result.filters.append(IntentFilter.from_intent_filter_xml(intent_node))
            yield result
            
    def __str__(self, *args, **kwargs):
        fmt = '"component":{{"package":{0}, "name":{1}, "filters":{2}}}'
        filter_strs = [str(f) for f in self.filters]
        return fmt.format(self.cid.package, self.cid.name, str(filter_strs))

class Intent(object):
    def __init__(self):
        self.tx = None
        self.rx = None
        self.intent_id = None
        self.icc = []
    
    @classmethod
    def from_sink_xml(cls, sink_root, package):
        comp = sink_root.attrib.get('component')
        iid = sink_root.attrib.get('intent-id')
        intent = cls()
        intent.intent_id = iid
        intent.tx = ComponentId(package, comp)
        intent.rx = None
        for pv in sink_root.findall("pv"):
            pv_dict = {}
            for field in pv.findall("field"):
                name = field.attrib.get("name")
                values = [v.text for v in field.findall("value")]
                pv_dict[name] = values
            intent.icc.append(pv_dict)
        return intent
        
    @classmethod
    def from_src_xml(cls, src_root, package):
        comp = src_root.attrib.get('component')
        intent = cls()
        intent.tx = None
        intent.rx = ComponentId(package, comp)
        return intent
        
    def __str__(self, *args, **kwargs):
        fmt = '{{"intent":{{"id":"{0}", "rx":"{1}", "tx":"{2}", "icc":"{3}"}}}}'
        return fmt.format(self.intent_id, str(self.rx), str(self.tx), str(self.icc))
 
class IntentResult(object):
    def __init__(self):
        self.i = None
    
    @classmethod
    def from_sink_xml(cls, sink_root, package):
        comp = sink_root.attrib.get('component')
        intent_result = cls()
        intent = Intent()
        intent.intent_id = None
        intent.tx = None
        intent.rx = ComponentId(package, comp)
        intent_result.i = intent
        return intent_result
    
    @classmethod
    def from_src_xml(cls, src_root, package):
        comp = src_root.attrib.get('component')
        intent_result = cls()
        intent = Intent()
        intent.intent_id = None
        intent.tx = ComponentId(package, comp)
        intent.rx = None
        intent_result.i = intent
        return intent_result
    
    def __str__(self, *args, **kwargs):
        return '"{{intent_result":{{"i":"{0}""}}}}'.format(str(self.i))

class IntentFilter(object):    
    def __init__(self, action=None, category=None, mime_type=None):
        self.action = action or []
        self.category = category or []
        self.mime_type = mime_type or []

    def init_from_bcast(self, bi):
        if 'Actions' in bi:
            self.action = bi['Actions']
        if 'Category' in bi:
            self.category = bi['Category']
    
    @classmethod
    def from_intent_filter_xml(cls, intent_filter_root):
        intent_filter = IntentFilter()
        for sub in intent_filter_root.findall("*"):
            filter_attr = OrderedDict()
            for (key, val) in sub.attrib.iteritems():
                # E.g., key might be "android:name" (for action and category) or
                # "android:scheme" (for data), but with "android:" expanded out to
                # android_pfx.
                key = key.replace(ANDROID_PFX, "")
                filter_attr[key] = val
                
            if sub.tag in ['action', 'category']:
                intent_filter.__dict__[sub.tag].append(filter_attr['name'])
            elif sub.tag == 'data':
                intent_filter.mime_type.append(filter_attr.get('mimeType', None))
        return intent_filter
        
    def __str__(self):
        fmt = '{{"intent-filter":{{"action"={0}, "category"={1}, mime_type={2}}}}}'
        return fmt.format(self.action, self.category, self.mime_type)

class Flow(object):
    def __init__(self):
        self.src = None
        self.sink = None
        self.app = None
            
    @classmethod
    def from_flow_xml(cls, flow_root, package):
        sink_data = flow_root.find("sink")
        sink_method = sink_data.attrib.get("method")
        sink = None
        src = None
        
        if sink_data.attrib.get('is-intent') == "1":
            sink = Intent.from_sink_xml(sink_data, package)
        elif sink_data.attrib.get('is-intent-result') == "1":
            sink = IntentResult.from_sink_xml(sink_data, package)
        else:
            sink = "Sink: " + sink_method
        
        for src_data in flow_root.findall("source"):
            src_method = src_data.attrib['method']
            try:
                in_meth = src_data.find("in").text
            except:
                in_meth = None
                
            if ("getIntent" in src_method):
                src = Intent.from_src_xml(src_data, package)
            elif ("@parameter2: android.content.Intent" in src_method) and (in_meth == "onActivityResult"):
                src = IntentResult.from_src_xml(src_data, package)
            elif ("@parameter0: android.content.Intent" in src_method) and (in_meth == "onStartCommand"):
                src = Intent.from_src_xml(src_data, package)
            elif ("@parameter1: android.content.Intent" in src_method) and (in_meth == "onReceive"):
                src = Intent.from_src_xml(src_data, package)
            else:
                src = "Src: " + src_method
            
            result_flow = cls()
            result_flow.src = src
            result_flow.sink = sink
            result_flow.app = package
            yield result_flow        

    def __str__(self, *args, **kwargs):
        return '{{"flow":{{"app":"{0}", "src":"{1}", "sink":"{2}"}}}}'.format(self.app, str(self.src), str(self.sink))   
       
class Phase2Analysis(object):
    def __init__(self):
        self.flows = {}
        self.components = {}
            
    def add_manifest_file(self, manifest):
        logging.debug("Processing manifest: " + manifest)
        tree = ET.parse(manifest)
        manifest_root = tree.getroot()
        package = manifest_root.attrib.get("package")
        self.flows[package] = []
        self.components[package] = []
        
        analysis_results = manifest_root.find("analysis_results")
     
        for component in Component.from_manifest_xml(manifest_root):
            print component
             
        # Process flows
        for flow_data in analysis_results.findall("flow"):
            for flow in Flow.from_flow_xml(flow_data, package):
                self.flows[package].append(flow)
            
def main(argv=None):
    if argv is None:
        argv = sys.argv
    else:
        sys.argv.extend(argv)
        
    program_shortdesc = __import__('__main__').__doc__.split("\n")[1]

    try:
        parser = ArgumentParser(description=program_shortdesc, formatter_class=RawDescriptionHelpFormatter)
        parser.add_argument('manifests', nargs='+', metavar='m', help="A list of paths to phase1 manifests.")
        args = parser.parse_args()
        phase2 = Phase2Analysis()
        for manifest in args.manifests:
            phase2.add_manifest_file(manifest)
        
    except KeyboardInterrupt:
        return 0

if __name__ == "__main__":
    logging.basicConfig(level="DEBUG")
    sys.exit(main())
