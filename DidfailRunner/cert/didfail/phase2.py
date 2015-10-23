#!/usr/local/bin/python2.7
# encoding: utf-8
'''
cert.didfail.phase2 -- Runs the second phase of the Didfail analysis.
'''

from __builtin__ import classmethod
from argparse import ArgumentParser
from argparse import RawDescriptionHelpFormatter
from collections import OrderedDict
from collections import namedtuple
import logging
import sys

from cert.didfail import OrderedSet
import xml.etree.ElementTree as ET

# TODO Verification methods
# Ensure sane inputs!

# Model
ANDROID_PFX = "{http://schemas.android.com/apk/res/android}"
    
ComponentId = namedtuple('ComponentId', ['package', 'name'])

class AppInfo(object):
    def __init__(self, package):
        self.package = package
        self.flows = []
        self.components = {}
        
class ComponentType(object):
    Activity = "Activity"
    Service = "Service"
    BroadcastReceiver = "BroadcastReceiver"
    ContentProvider = "ContentProvider"
    
    @staticmethod
    def get_receiving_component_type(method_name):
        if not method_name:
            return None
        
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
                intent_filter = IntentFilter.from_intent_filter_xml(intent_node)
                intent_filter.component_id = result.cid
                result.filters.append(intent_filter)
            yield result
    
    def __xml__(self):
        root = ET.Element("component")
        root.attrib["name"] = self.cid.name
        root.attrib["package"] = self.cid.package
        root.attrib["type"] = self.ctype
        for f in self.filters:
            root.append(f.__xml__())
        return root                
        
    def __str__(self, *args, **kwargs):
        fmt = 'Component(package={0}, name={1}, filters={2})'
        filter_strs = [str(f) for f in self.filters]
        return fmt.format(self.cid.package, self.cid.name, str(filter_strs))

class IntentFilter(object):    
    def __init__(self):
        self.actions = []
        self.categories = []
        self.data_types = []
        self.component_id = None

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
            
            if sub.tag == 'action':
                intent_filter.actions.append(filter_attr['name'])
            elif sub.tag == 'category':
                intent_filter.categories.append(filter_attr['name'])
            elif sub.tag == 'data':
                intent_filter.data_types.append(filter_attr.get('mimeType', None))
        return intent_filter
    
    def __xml__(self):
        root = ET.Element("intentfilter")
        for action in self.actions:
            e = ET.Element("action")
            e.append(action)
            root.append(e)
        for cat in self.categories:
            e = ET.Element("category")
            e.append(cat)
            root.append(e)
        for dt in self.data_types:
            e = ET.Element("datatype")
            e.append(dt)
            root.append(e)
        return root    
    
    def __str__(self):
        fmt = 'IntentFilter(actions={0}, category={1}, data_types={2})'
        return fmt.format(self.actions, self.categories, self.data_types)
    
class GenericSource(object):
    def __init__(self, src):
        self.src = src
    
    def __xml__(self):
        root = ET.Element("source")
        root.attrib["type"] = "generic"
        root.append(self.src)
        return root  
    
    def __str__(self, *args, **kwargs):
        fmt = "GenericSource({0})"
        return fmt.format(self.src)

class GenericSink(object):
    def __init__(self, sink):
        self.sink = sink
    
    def __xml__(self):
        root = ET.Element("sink")
        root.attrib["type"] = "generic"
        root.append(self.sink)
        return root  
    
    def __str__(self, *args, **kwargs):
        fmt = "GenericSink({0})"
        return fmt.format(self.sink)

class TransmittingIntentInfo():
    ANY = ("__ANY_STRING__",)
    
    def __init__(self):
        self.info = {}
        self.clazz = None
        self.package = None
        self.action = None
        self.uri = None
        self.data_type = None
        self.categories = []
        self.extras = []
 
    def is_explicit_intent(self):
        return self.clazz != None
 
    @classmethod
    def from_sink_xml(cls, sink_root):
        def process_string(s):
            if s == "(*.)":
                return cls.ANY
            else:
                return s
                    
        for pv in sink_root.findall("pv"):
            tii = cls()
            for field in pv.findall("field"):
                name = field.attrib.get("name")
                values = [v.text for v in field.findall("value")]
                
                if name == "package":
                    tii.package = process_string(values[0])
                elif name == "clazz":
                    tii.package = process_string(values[0])
                elif name == "uri":
                    tii.uri = process_string(values[0])
                elif name == "dataType":
                    tii.data_type = process_string(values[0])
                elif name == "action":
                    tii.action = process_string(values[0])
                elif name == "extras":
                    tii.extras = [process_string(s) for s in values]
                elif name == "categories":
                    tii.categories = [process_string(s) for s in values]
            yield tii
        
    def matches_intent_filter(self, intent_filter, match_options):      
        # Check if the intent is an explicit intent.

        if self.is_explicit_intent():
            return self.clazz == self.ANY or self.clazz == intent_filter.component_id.name
        else:
            string_matches = lambda x,xs: (x == self.ANY or x in xs)
            action_set = set(intent_filter.actions)
            cat_set = set(intent_filter.categories)
            if not string_matches(self.action, action_set):
                return False
            
            if not any((string_matches(x, cat_set) for x in self.categories)):
                return False                               
            
            return True
        
class MatchOptions(object):
    def __init__(self):
        self.sound = True

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
        method_name = sink_root.attrib.get('method')
        intent = cls()
        intent.intent_id = iid
        intent.tx = ComponentId(package, comp)
        intent.rx = ComponentType.get_receiving_component_type(method_name)
        intent.icc = list(TransmittingIntentInfo.from_sink_xml(sink_root))
        return intent

    @classmethod
    def from_src_xml(cls, src_root, package):
        comp = src_root.attrib.get('component')
        intent = cls()
        intent.tx = None
        intent.rx = ComponentId(package, comp)
        return intent
    
    def matches_component(self, component, match_options=MatchOptions()):
        for tii in self.icc:
            for intent_filter in component.filters:
                if tii.matches_intent_filter(intent_filter, match_options):
                    return True
        return False
    
    def __str__(self, *args, **kwargs):
        fmt = '"Intent(id={0}, rx={1}, tx={2}, icc={3})'
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
        fmt = 'IntentResult(i={0})'
        return fmt.format(str(self.i))

class Flow(object):
    def __init__(self):
        self.src = None
        self.sink = None
        self.app = None
        self.is_complete = False
            
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
            sink = GenericSink(sink_method)
        
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
                src = GenericSource(src_method)
            
            result_flow = cls()
            result_flow.src = src
            result_flow.sink = sink
            result_flow.app = package
            yield result_flow        

    def __str__(self, *args, **kwargs):
        fmt = 'Flow(app={0}, src={1}, sink={2}, complete={3})'
        return fmt.format(self.app, str(self.src), str(self.sink), self.is_complete)   
       
class Phase2Analysis(object):
    def __init__(self):
        self.appinfo = {}
            
    def add_manifest_file(self, manifest):
        logging.debug("Processing manifest: " + manifest)
        tree = ET.parse(manifest)
        manifest_root = tree.getroot()
        
        package = manifest_root.attrib.get("package")        
        analysis_results = manifest_root.find("analysis_results")
     
        info = AppInfo(package)
     
        # Process components
        for component in Component.from_manifest_xml(manifest_root):
            info.components[component.cid] = component
            print component
             
        # Process flows
        for flow_data in analysis_results.findall("flow"):
            for flow in Flow.from_flow_xml(flow_data, package):
                info.flows.append(flow)
        
        self.appinfo[package] = info
    
    def get_flows(self):
        for app in self.appinfo.values():
            for flow in app.flows:
                yield flow
    
    def get_components(self, ctype=None):
        for app in self.appinfo.values():
            for component in app.components.values():
                if ctype and (component.ctype != ctype):
                    continue
                yield component     
    
    def generate_all_matches(self):   
        for flow in [f for f in self.get_flows() if isinstance(f.sink, Intent)]:
            tx_intent = flow.sink
            tx_meth_type = tx_intent.rx
            print tx_meth_type
            
            for rx_cmp in self.get_components(ctype=tx_meth_type):  
                print rx_cmp          
                if tx_intent.matches_component(rx_cmp):
                    print "Match!"
                    rx_intent = Intent()
                    rx_intent.rx = rx_cmp.cid
                    yield Intent(tx=tx_intent.tx, rx=rx_intent.rx, intent_id=tx_intent.intent_id)
        
    def populate_matches(self):
#        glo.match_by_tx = {}
#        glo.match_by_tx_id = {}
#        glo.match_by_rx = {}
        for i in self.generate_all_matches(): # OrderedSet eliminates duplicates
            print i
            #glo.match_by_rx.setdefault(i.rx, {}).setdefault(i.tx, []).append(i.intent_id)
            #glo.match_by_tx.setdefault(i.tx, {}).setdefault(i.rx, []).append(i.intent_id)
            #glo.match_by_tx_id.setdefault((i.tx, i.intent_id), set()).add(i.rx)
    
    def match_flows(self):
        self.populate_matches()

    def get_sources(self):
        pass
    
    def solve_flows(self):
        pass

    def run_analysis(self):
        pass
            
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
        phase2.match_flows()
        
    except KeyboardInterrupt:
        return 0

if __name__ == "__main__":
    logging.basicConfig(level="DEBUG")
    sys.exit(main())
