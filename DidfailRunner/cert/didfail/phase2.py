#!/usr/local/bin/python2.7
# encoding: utf-8
'''
cert.didfail.phase2 -- Runs the second phase of the Didfail analysis.
'''

from argparse import ArgumentParser
from argparse import RawDescriptionHelpFormatter
import logging
import sys
from cert.didfail.entities import AppInfo, Component, xml_to_str, Flow, Intent
import xml.etree.ElementTree as ET


# TODO Verification methods
# Ensure sane inputs!
class Phase2Analysis(object):
    def __init__(self):
        self.appinfo = {}
            
    def add_manifest_file(self, manifest):
        logging.info("Processing manifest: " + manifest)
        tree = ET.parse(manifest)
        manifest_root = tree.getroot()
        
        package = manifest_root.attrib.get("package")        
        analysis_results = manifest_root.find("analysis_results")
     
        info = AppInfo(package)
     
        # Process components
        for component in Component.from_manifest_xml(manifest_root):
            info.components[component.cid] = component
            logging.info(xml_to_str(component))
             
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
            print xml_to_str(tx_intent)
                        
            for rx_cmp in self.get_components(ctype=tx_meth_type):        
                if tx_intent.matches_component(rx_cmp):
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
    logging.basicConfig(level="INFO")
    sys.exit(main())
