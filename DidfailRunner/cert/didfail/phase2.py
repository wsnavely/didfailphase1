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
        fmt = '"Component(package={0}, name={1}, filters={2})'
        filter_strs = [str(f) for f in self.filters]
        return fmt.format(self.cid.package, self.cid.name, str(filter_strs))

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
        fmt = 'IntentFilter(action={0}, category={1}, mime_type={2})'
        return fmt.format(self.action, self.category, self.mime_type)
    
class GenericSource(object):
    def __init__(self, src):
        self.src = src
    
    def __str__(self, *args, **kwargs):
        fmt = "GenericSource({0})"
        return fmt.format(self.src)

class GenericSink(object):
    def __init__(self, sink):
        self.sink = sink
    
    def __str__(self, *args, **kwargs):
        fmt = "GenericSink({0})"
        return fmt.format(self.sink)

class TransmittingIntentInfo():
    def __init__(self):
        self.info = {}
    
    @classmethod
    def from_sink_xml(cls, sink_root, package):        
        for pv in sink_root.findall("pv"):
            tii = cls()
            for field in pv.findall("field"):
                name = field.attrib.get("name")
                values = [v.text for v in field.findall("value")]
                tii.info[name] = values
            yield tii

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
        method_name = sink_root.attrib.get('method_name')
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
    
    @staticmethod 
    def matches(self, tx_intent, rx_intent, match_options):
        #(tx_epicc, filters) = [get_epicc(tx), get_filters(rx)]
        #(rx_pkg, rx_comp) = rx.rx
    
        #for intent in tx_epicc:
        #    for filt in filters:
        #        if match_intent_subcase(intent, filt, rx_comp):
        #            return True
        pass
    
    def match_intent_subcase(self, epicc, filt, rx_comp):
        # This method implements the action, category, and data tests described in
        # http://developer.android.com/guide/components/intents-filters.html#Resolution
        # Epicc does not produce URI information, so we ignore the URI tests.
        assert(isinstance(filt, IntentFilter))
        if epicc.get('Top', None) == True:
            return (not glo.unsound)
        
        def match_any_string(x):
            return (x == '<any_string>') and not glo.unsound
        
        # Check if the intent is an explicit intent.
        epicc_class = epicc.get('Class', None)
        if epicc_class != None:
            # Lots of false positives here for <any_string>.
            # TODO: Can explicit intents be explicitly designated using an
            # activity alias?  If so, we need to the use information in
            # glo.act_alias_to_targ.
            return ((epicc_class == rx_comp) or match_any_string(epicc_class))
        # Action test
        act = epicc.get('Action', None) or epicc.get('Actions', None)
        if type(act) == str:
            act_set = set([act])
        elif (act is None):
            act_set = set()
        else:
            act_set = set(act)
        act_ok = (
            (((act is None) or match_any_string(act)) and len(filt.action) > 0) or 
            (act_set & set(filt.action)))
        if not act_ok:
            return False
        ############################################################
        # For each category in intent, must be a match in filter.
        # Zero categories in intent, but many in filter: still can be received.
        cat = epicc.get('Categories', None)
        if type(cat) == str:
            cat_set = set([cat])
        elif (cat is None):
            cat_set = set()
        else:
            cat_set = set(cat)
        cat_ok = (
            (cat == None) or any(match_any_string(x) for x in cat_set) or
            ((cat_set & set(filt.category)) == cat_set))
        if not cat_ok:
            return False
        # If glo.unsound, then False negatives returned if <any_string> is
        # intent category EPICC returns and the filter actually matches that
        # category EPICC doesn't process 
    
        ############################################################
        # TODO: data MIME type
        # An intent filter can declare zero or more data elements. Rules:
        # 1. An intent that contains neither a URI nor a MIME type passes the test only if the filter does not specify any URIs or MIME types.
        # (Can't test for this): 2. An intent that contains a URI but no MIME type (neither explicit nor inferable from the URI) passes the test only if its URI matches per test
        # 3. An intent that contains a MIME type but not a URI passes the test only if the filter lists the same MIME type and does not specify a URI format.
        # 4. (Can't test for last rule, since depends on URI)
        # OUTPUT: com.UCMobile.intl.epicc:Actions: [action_local_share], Data types: [*/*]
        # OUTPUT2: Uxpp.UC.epicc:Package: Uxpp/UC, Class: com/nate/android/nateon/uc3/msg/view/MsgListActivity, Extras: [RoomID, FileType, by, ShareUri, ShareType, ShareData, SearchWord, ShareText], Flags: 67108864
    
        #if False:
        #    data = epicc.get('Type', None)
        #    pdb.set_trace()
        #    if type(data) == str:
        #        data_set = set([data])
        #    elif (data is None):
        #        data_set = set()
        #    else:
        #        data_set = set(data)
        #    data_ok = (
        #        ((data == None) and len(filter.data)==0) or
        #        ((data_set & set(filt.data)) == data_set))
        #    if not data_ok:
        #        return False
    
        return True
               
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
        fmt = 'IntentResul(i={0})'
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
             
        # Process flows
        for flow_data in analysis_results.findall("flow"):
            for flow in Flow.from_flow_xml(flow_data, package):
                info.flows.append(flow)
                print flow
    
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
        for flow in self.get_flows().filter(lambda f: isinstance(f.sink, Intent)):
            tx_intent = flow.sink
            tx_meth_type = tx_intent.rx
            
            for rx_cmp in self.get_components(ctype=tx_meth_type):
                rx_intent = Intent()
                rx_intent.rx = rx_cmp.cid
                
                if self.match_intent_attr(tx_intent, rx_intent):
                    yield Intent(tx=tx_intent.tx, rx=rx_intent.rx, intent_id=tx_intent.intent_id)

    def get_sources(self):
        pass
    
    def match_flows(self):
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
        
    except KeyboardInterrupt:
        return 0

if __name__ == "__main__":
    logging.basicConfig(level="DEBUG")
    sys.exit(main())
