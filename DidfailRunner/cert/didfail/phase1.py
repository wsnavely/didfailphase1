import command
import logging

def action(process):
    print "HI!", process.pid
    
logging.basicConfig(level="DEBUG")
print command.run_command("sleep", ["10"], monitor_action=action, monitor_interval=2.0)