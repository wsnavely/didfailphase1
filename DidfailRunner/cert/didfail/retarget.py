import argparse
import logging

from cert.didfail import DIDFAIL_DARE
import command

class DareOptions(object):
    def __init__(self):
        self.timeout = 900
        
def run_dare(apk, out, options, stdout=None, stderr=None):
    logging.info("Processing APK: " + apk)
    args = []
    args += ["-d", out]
    args += [apk]
    command.run_cmd(DIDFAIL_DARE, args, stdout=stdout, stderr=stderr, timeout=options.timeout)
    
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("-a", "--apk", required=True)
    parser.add_argument("-o", "--out", required=True)
    parser.add_argument("-t", "--timeout", default=(60 * 15))
    args = parser.parse_args()
    
    options = DareOptions()
    options.timeout = args.timeout
    run_dare(args.apk, args.out, options)
