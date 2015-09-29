#!/usr/local/bin/python2.7
# encoding: utf-8
'''
cert.didfail.phase1 -- Runs the first phase of a Didfail analysis, on a set of APKs.

cert.didfail.phase1

@author:     TODO
@contact:    TODO
'''

import sys
import os
import logging

from argparse import ArgumentParser
from argparse import RawDescriptionHelpFormatter

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
    
def process_apk(path):
    logging.debug("Processing APK " + path)

def main(argv=None): # IGNORE:C0111
    '''Command line options.'''

    if argv is None:
        argv = sys.argv
    else:
        sys.argv.extend(argv)

    program_name = os.path.basename(sys.argv[0])
    program_version = "v%s" % __version__
    program_build_date = str(__updated__)
    program_version_message = '%%(prog)s %s (%s)' % (program_version, program_build_date)
    program_shortdesc = __import__('__main__').__doc__.split("\n")[1]

    try:
        parser = ArgumentParser(description=program_shortdesc, formatter_class=RawDescriptionHelpFormatter)
        parser.add_argument('-V', '--version', action='version', version=program_version_message)
        parser.add_argument('-o', '--out', help="The directory where outputs should be stored.")
        parser.add_argument('apks', nargs='+', metavar='apk', help="A list of paths to apk files.")
        args = parser.parse_args()

        apks = args.apks
        outdir = args.out
        for apk in apks:
            process_apk(apk)


    except KeyboardInterrupt:
        return 0
    except Exception, e:
        if DEBUG:
            raise(e)
        indent = len(program_name) * " "
        sys.stderr.write(program_name + ": " + repr(e) + "\n")
        sys.stderr.write(indent + "  for help use --help")
        return 2

if __name__ == "__main__":
    if DEBUG:
        logging.basicConfig(level="DEBUG")
    sys.exit(main())