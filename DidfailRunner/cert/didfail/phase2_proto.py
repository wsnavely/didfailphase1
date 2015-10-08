'''
Created on 8 Oct 2015

@author: osboxes
'''
from argparse import ArgumentParser
import xml.etree.ElementTree as ET

class Flow(object):
    def __init__(self):
        self.type = None
        self.src = None
        self.sink = None

class Endpoint(object):   
    Transmitting = "TRANSMITTING"
    Receiving = "RECEIVING"
     
    def __init__(self, type):
        self.type = type

class IntentEndpoint(Endpoint):
    def __init__(self, type, component, intent_id):
        super(IntentEndpoint, self).__init__(type)
        self.component = component 
        self.intent_id = intent_id
       
class AppManifest(object):
    def __init__(self, path):
        self.package = None
        self.flows = []
        self.parse_manifest(path)
        
    def parse_manifest(self, path):
        tree = ET.parse(path)
        root = tree.getroot()
        analysis_results = root.find("analysis_results")
        for flow in analysis_results.find("flow"):
            self.parse_flow(self, flow)
            
    def parse_flow(self, flow):
        result = Flow()
        sink = flow.find("sink")
        sink_attr = sink.attrib
 
        if sink_attr.get('is-intent') == "1":
            intent_id = sink_attr.get('intent-id')
            if not intent_id: 
                raise Exception("Intent ID Missing")
            
            sink_component = sink_attr.get('component')
            result.sink = IntentEndpoint(Endpoint.Transmitting, sink_component, intent_id)
            # TODO: Is this important?
            #rx_type = ComponentType.Activity
            #glo.rx_type_of[(pkg_name, intent_id)] = rx_type
        elif sink.attrib.get('is-intent-result') == "1":
            sink_component = sink.get('component')
            result_sink = IntentResult(Intent(tx=None, rx=(pkg_name, sink_component), intent_id=None))
        else:
            result_sink = "Sink: " + sink.attrib['method']
            
        for src in flow.findall("source"):
            src_method = src.attrib['method']
            component = None
            if src_method.startswith("<android.content.Intent:") or ("getIntent" in src_method):
                component = src.attrib.get('component')
                if glo.discard_tx_comp_name:
                    component = None
                result_src = Intent(tx=None, rx=(pkg_name, component), intent_id=None)
            elif ("@parameter2: android.content.Intent" in src_method): 
                # FIXME: only for "android.app.Activity: void onActivityResult" 
                component = src.attrib['component']
                if glo.discard_tx_comp_name:
                    component = None
                result_src = IntentResult(Intent(tx=(pkg_name, component), rx=None, intent_id=None))
            else:
                result_src = "Src: " + src_method 
            
            # FIXME: What if the the source and sinks are in different components?
            if glo.only_intents and all(type(s) == str for s in [result_src, result_sink]):
                continue
            flows.append(Flow(src=result_src, app=pkg_name, sink=result_sink))
        
        
        

class Phase2(object):
    def __init__(self):
        self.apps = []
    
    def add_manifest(self, manifest):
        tree = ET.parse(manifest)
        root = tree.getroot()
        flows = self.find_flows(root)
    
        
if __name__ == "__main__":
    phase2 = Phase2()
    parser = ArgumentParser()
    parser.add_argument("files", nargs='+')
    args = parser.parse_args()
    
    for path in args.files:
        phase2.add_manifest(path)
    
