import logging
import subprocess
import threading
import time

class Command(object):
    def __init__(
            self, 
            cmd, 
            args, 
            monitor=False, 
            monitorlog=None,
            monitor_interval=5):
        self.cmd = cmd
        self.args = args
        self.stdout = stdout
        self.stderr = stderr
        self.cwd = cwd
        self.monitor = monitor
        self.monitor_interval = monitor_interval
        self.process = None
        self._done = False

        if self.monitor and not self.monitorlog:
            self.monitorlog = "cmd.stats"

    def run(self, timeout):
        def runner():
            pkg = [self.cmd] + self.args
            logging.debug("Running command (timeout " + str(timeout) + "): " + (" ".join(pkg)))
            stdout_file = subprocess.STDOUT
            stderr_file = subprocess.STDOUT
            
            try:
                if self.stdout:
                    stdout_file = open(self.stdout, "a")
                if self.stderr:
                    if self.stdout and self.stderr == self.stdout:
                        stderr_file = subprocess.STDOUT
                    else:
                        stderr_file = open(self.stderr, "a")

                self.process = subprocess.Popen(pkg, stdout=stdout_file, stderr=stderr_file, cwd=self.cwd, env=None)
                self.process.communicate()
            finally:
                if stdout_file:
                    stdout_file.close()
                if stderr_file and stderr_file != subprocess.STDOUT:
                    stderr_file.close()
    
        def monitor_process():
            mlog = self.monitorlog
            with open(mlog, "a") as logfile:
                while(not self._done):
                    if(self.process):
                        pkg = ["ps", "--no-headers", "-opid,rss,pcpu,time", str(self.process.pid)]
                        proc = subprocess.Popen(pkg, stdout=logfile)
                        proc.communicate()
                        time.sleep(self.monitor_interval)
                    else:
                        time.sleep(1)
        self._done = False
        main_thread = threading.Thread(target=runner)
        monitor_thread = None
        timedout = False

        start_time = time.time()
        main_thread.start()
        if(self.monitor):
            monitor_thread = threading.Thread(target=monitor_process)
            monitor_thread.start()

        main_thread.join(timeout)
        self._done = True
        if(monitor_thread):
            monitor_thread.join()
        end_time = time.time()
        diff = end_time - start_time
        logging.debug("Command runtime: {0}".format(diff))

        if main_thread.is_alive():
            logging.info("Command timed out!")
            timedout = True
            self.process.terminate()
            main_thread.join()

    result = (self.process.returncode, diff, timedout)
    logging.debug("Command result: " + str(result))
        return result
