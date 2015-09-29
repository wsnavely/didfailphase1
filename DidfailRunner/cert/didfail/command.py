import logging
import subprocess
import threading
import time
    
class CmdSettings(object):
    def __init__(self):
        self.stdout = None
        self.stderr = None
        self.cwd = None
        self.env = None
        self.monitor_action = None
        self.monitor_interval = None
        self.timeout = None

def run_cmd_with_settings(cmd, args, settings):
    return run_cmd(
        cmd,
        args,
        stdout=settings.stdout,
        stderr=settings.stderr,
        cwd=settings.cwd,
        env=settings.env,
        monitor_action=settings.monitor_action,
        monitor_interval=settings.monitor_interval,
        timeout=settings.timeout)
    
def run_cmd(
        cmd,
        args,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        cwd=None,
        env=None,
        monitor_action=None,
        monitor_interval=1.0,
        timeout=None):
    
    class CmdResult(object):
        def __init__(self, rc, wt, to, out, err):
            self.return_code = rc
            self.wall_time = wt
            self.timed_out = to
            self.out = out
            self.err = err
        
        def __str__(self, *args, **kwargs):
            return "(RC={0},WT={1},TO={2})".format(self.return_code, self.wall_time, self.timed_out)
    
    class SharedData(object):
        def __init__(self):
            self.process = None
            self.done = False
            self.process_started = threading.Condition()
            self.output = None
            
    shared_data = SharedData()
    
    def run_process():
        pkg = [cmd] + args
        logging.debug("Running Command (Timeout: {0}):\n{1}".format(timeout, (" ".join(pkg))))
        shared_data.process_started.acquire()
        shared_data.process = subprocess.Popen(pkg, stdout=stdout, stderr=stderr, cwd=cwd, env=env)
        shared_data.process_started.notify()
        shared_data.process_started.release()
        shared_data.output = shared_data.process.communicate()

    def monitor_process():
        shared_data.process_started.acquire()
        if not shared_data.process:
            shared_data.process_started.wait()
        shared_data.process_started.release()
        
        while(not shared_data.done):
            monitor_action(shared_data.process)
            time.sleep(monitor_interval)

    main_thread = threading.Thread(target=run_process)
    monitor_thread = None
    timed_out = False

    start_time = time.time()
    
    main_thread.start()
    if(monitor_action):
        monitor_thread = threading.Thread(target=monitor_process)
        monitor_thread.start()

    main_thread.join(timeout)
    shared_data.done = True

    if(monitor_thread):
        monitor_thread.join()
    
    end_time = time.time()

    if main_thread.is_alive():
        timed_out = True
        shared_data.process.terminate()
        main_thread.join()

    rc = shared_data.process.returncode
    out, err = shared_data.output
    wall_time = end_time - start_time
    return CmdResult(rc, wall_time, timed_out, out, err)
