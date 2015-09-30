import os

DIDFAIL_BIN = os.path.join(os.path.dirname(os.path.realpath(__file__)), "bin")
DIDFAIL_DARE_BIN = os.path.join(DIDFAIL_BIN, "dare-1.1.0-linux")
DIDFAIL_FD = os.path.join(DIDFAIL_BIN, "didfail_fd.jar")
DIDFAIL_IC = os.path.join(DIDFAIL_BIN, "didfail_ic.jar")
DIDFAIL_DARE = os.path.join(DIDFAIL_DARE_BIN, "dare")

DEFAULT_SRC_SINK = os.path.join(DIDFAIL_BIN, "SourcesAndSinks.txt")
DEFAULT_TW = os.path.join(DIDFAIL_BIN, "EasyTaintWrapperSource.txt")
DEFAULT_FD_RUNTIME = os.path.join(DIDFAIL_BIN, "FlowdroidRuntime.json")