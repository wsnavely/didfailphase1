#!/usr/local/bin/python2.7
# encoding: utf-8
'''
cert.didfail.phase1 -- Runs the first phase of a Didfail analysis, on a set of APKs.

cert.didfail.phase1

@author:     TODO
@contact:    TODO
'''

from argparse import ArgumentParser
from argparse import RawDescriptionHelpFormatter
import errno
import json
import logging
import os
import sys

from cert.didfail.flowdroid import run_flowdroid, FlowdroidOptions
from cert.didfail.interapp import ICOptions, run_ic
from cert.didfail.retarget import DareOptions, run_dare
import command


__all__ = []
__version__ = 0.1
__date__ = '2015-09-29'
__updated__ = '2015-09-29'

DEBUG = 1

class DidfailSettings(object):
    def __init__(self):
        pass

class CLIError(Exception):
    '''Generic exception to raise and log different fatal errors.'''
    def __init__(self, msg):
        super(CLIError).__init__(type(self))
        self.msg = "E: %s" % msg
    def __str__(self):
        return self.msg
    def __unicode__(self):
        return self.msg

def mkdir_p(path):
    try:
        os.makedirs(path)
    except OSError as exc:  # Python >2.5
        if exc.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else: raise
        
def get_flowdroid_options(conf):
    fd_options = FlowdroidOptions()
    if conf and ("options" in conf) and ("flowdroid" in conf["options"]):
        options = conf["options"]["flowdroid"]
        if "platforms" in options:
            fd_options.platforms = options["platforms"]
        if "timeout" in options:
            fd_options.timeout = options["timeout"]
        if "heap_size" in options:
            fd_options.heap_size = options["heap_size"]
    fd_options.transform = True
    return fd_options

def get_ic3_options(conf):
    ic_options = ICOptions()
    if conf and ("options" in conf) and ("ic3" in conf["options"]):
        options = conf["options"]["ic3"]
        if "androidjar" in options:
            ic_options.android_jar = options["androidjar"]
        if "timeout" in options:
            ic_options.timeout = options["timeout"]
        if "heap_size" in options:
            ic_options.heap_size = options["heap_size"]
    return ic_options
        
def process_apk(path, outdir, fd_opt, ic_opt, dare_opt):
    logging.debug("Processing APK: " + path)

    apk_name = os.path.basename(path)
    apk_outdir = os.path.join(outdir, apk_name)
    logging.debug("Creating output directory for APK: " + apk_outdir)    
    mkdir_p(apk_outdir)
    
    # Flowdroid
    fd_out = os.path.join(apk_outdir, "fd.xml")
    run_flowdroid(path, fd_out, apk_outdir, fd_opt)
    
    # Retarget
    tform = os.path.join(apk_outdir, apk_name)
    dare_out = os.path.join(apk_outdir, "dare")
    run_dare(tform, dare_out, dare_opt)
    
    # IC3
    ic_out = os.path.join(apk_outdir, "ic.xml")
    retgt = os.path.join(dare_out, "retargeted")
    retgt = os.path.join(retgt, apk_name.replace(".apk", "")) 
    run_ic(tform, retgt, ic_out, ic_opt)

def main(argv=None):
    if argv is None:
        argv = sys.argv
    else:
        sys.argv.extend(argv)
        
    command.__whatif__ = True

    program_shortdesc = __import__('__main__').__doc__.split("\n")[1]

    try:
        parser = ArgumentParser(description=program_shortdesc, formatter_class=RawDescriptionHelpFormatter)
        parser.add_argument('-o', '--out', help="The directory where outputs should be stored.")
        parser.add_argument('-c', '--config', help="Path to phase 1 configuration file.")
        parser.add_argument('apks', nargs='+', metavar='apk', help="A list of paths to apk files.")
        args = parser.parse_args()
               
        config = None
        if args.config:
            with open(args.config) as config_file:
                config = json.load(config_file)
        
        fd_opt = get_flowdroid_options(config)
        ic_opt = get_ic3_options(config)
        dare_opt = DareOptions()
                
        apks = args.apks
        outdir = os.path.realpath(args.out)
        mkdir_p(outdir)
        
        for apk in apks:
            process_apk(os.path.realpath(apk), outdir, fd_opt, ic_opt, dare_opt)
        
    except KeyboardInterrupt:
        return 0
    

if __name__ == "__main__":
    if DEBUG:
        logging.basicConfig(level="DEBUG")
    sys.exit(main())