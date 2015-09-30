import argparse
import logging
import command

from cert.didfail import DIDFAIL_FD, DEFAULT_FD_RUNTIME, DEFAULT_TW, \
    DEFAULT_SRC_SINK, DIDFAIL_BIN

class FlowdroidOptions(object):
    def __init__(self):
        self.platforms = None
        self.transform = False
        self.heap_size = "4096m"
        self.timeout = 60 * 15
        self.sources_and_sinks = DEFAULT_SRC_SINK
        self.taint_wrapper = DEFAULT_TW
        self.runtime_options = DEFAULT_FD_RUNTIME

def run_flowdroid(apk, didfail_out, soot_out, options, stdout=None, stderr=None):
    logging.info("Processing APK: " + apk)
    
    args = []
    args += ["-Xmx" + options.heap_size] 
    args += ["-jar", DIDFAIL_FD]
    args += ["-apk", apk]
    args += ["-platforms", options.platforms]
    args += ["-config", options.runtime_options]
    args += ["-ss", options.sources_and_sinks]
    args += ["-tw", options.taint_wrapper]
    
    if didfail_out:
        args += ["-out", didfail_out]
    if soot_out:
        args += ["-sootout", soot_out]
    if options.transform:
        args += ["-labelsinks"]
            
    command.run_cmd("java", args, cwd=DIDFAIL_BIN, stdout=stdout, stderr=stderr, timeout=options.timeout)
    
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("-a", "--apk", required=True)
    parser.add_argument("-p", "--platforms", required=True)
    parser.add_argument("-o", "--out", default=None)
    parser.add_argument("-d", "--sootout", default=None)
    parser.add_argument("-m", "--heapsize", default=None)
    parser.add_argument("-s", "--sourcesandsinks", default=None)
    parser.add_argument("-w", "--taintwrapper", default=None)
    parser.add_argument("-c", "--config", default=None)
    parser.add_argument("-x", "--transform", action="store_true", default=False)
    parser.add_argument("-t", "--timeout", default=None)
    args = parser.parse_args()
    
    options = FlowdroidOptions()
    options.platforms = args.platforms
    options.transform = args.transform
    if args.heapsize:
        options.heap_size = args.heapsize
    if args.sourcesandsinks:
        options.sources_and_sinks = args.sourcesandsinks
    if args.taintwrapper:
        options.taint_wrapper = args.taintwrapper
    if args.config:
        options.runtime_options = args.config
    if args.timeout:
        options.timeout = args.timeout

    run_flowdroid(args.apk, args.out, args.sootout, options)
