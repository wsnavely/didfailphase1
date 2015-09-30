import argparse
import logging
import command

from cert.didfail import DIDFAIL_IC, DIDFAIL_BIN

class ICOptions(object):
    def __init__(self):
        self.android_jar = None
        self.heap_size = "4096m"
        self.timeout = 60 * 15
        
def run_ic(apk, retargeted, out, options, stdout=None, stderr=None):
    logging.info("Processing APK: " + apk)

    args = []
    args += ["-Xmx" + options.heap_size] 
    args += ["-jar", DIDFAIL_IC]
    args += ["-apk", apk]
    args += ["-retargeted", retargeted]
    args += ["-androidjar", options.android_jar]
    
    if out:
        args += ["-out", out]
        
    command.run_cmd("java", args, cwd=DIDFAIL_BIN, stdout=stdout, stderr=stderr, timeout=options.timeout)
    
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("-a", "--apk", required=True)
    parser.add_argument("-r", "--retargeted", required=True)
    parser.add_argument("-j", "--androidjar", required=True)
    parser.add_argument("-o", "--out", default=None)
    parser.add_argument("-m", "--heapsize", default="4096m")
    parser.add_argument("-t", "--timeout", default=(60 * 15))
    args = parser.parse_args()
    
    options = ICOptions()
    options.heap_size = args.heapsize
    options.android_jar = args.androidjar
    options.timeout = args.timeout
    run_ic(args.apk, args.retargeted, args.out, options)
