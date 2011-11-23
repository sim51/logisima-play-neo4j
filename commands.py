# Here you can create play commands that are specific to the module, and extend existing commands

MODULE = 'neo4j'

# Commands that are specific to your module

COMMANDS = ['neo4j:help', 'neo4j:import', 'neo4j:export' ]

def execute(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    if command == "neo4j:help":
        print "~ Help for logisima-play-neo4j module"
        print "~ Available commands are:"
        print "~ ~~~~~~~~~~~~~~~~~~~~~~~"
        print "~ import       Import an yaml file (conf/data.yml by default) to database"
        print "~     with --filename you can specify the yaml filename file (without the yml extension !)"
        print "~     with --folder you can specify the folder where yaml file will be written (conf by default)"
        print "~ export       Export your database into yaml format (to file conf/data.yml)"
        print "~     with --filename you can specify the yaml filename file (without the yml extension !)"
        print "~     with --folder you can specify the folder where yaml file will be read (conf by default)"
        print       
        sys.exit(0)
   
    if command == "neo4j:import":
        print "~ Import yml to database"
        print "~ "
        java_cmd = app.java_cmd([], None, "play.modules.neo4j.cli.Import", args)
        try:
            subprocess.call(java_cmd, env=os.environ)
        except OSError:
            print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
            sys.exit(-1)
        print
        
    if command == "neo4j:export":
        print "~ Generating yml from the database"
        print "~ "
        java_cmd = app.java_cmd([], None, "play.modules.neo4j.cli.Export", args)
        try:
            subprocess.call(java_cmd, env=os.environ)
        except OSError:
            print "Could not execute the java executable, please make sure the JAVA_HOME environment variable is set properly (the java executable should reside at JAVA_HOME/bin/java). "
            sys.exit(-1)
        print


# This will be executed before any command (new, run...)
def before(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")


# This will be executed after any command (new, run...)
def after(**kargs):
    command = kargs.get("command")
    app = kargs.get("app")
    args = kargs.get("args")
    env = kargs.get("env")

    if command == "new":
        pass
