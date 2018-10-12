date

this_dir=`pwd`

dirname $0|grep "^/" >/dev/null
if [ $? -eq 0 ];then
		this_dir=`dirname $0`
		else
		dirname $0|grep "^\." >/dev/null
		retval=$?
			if [ $retval -eq 0 ];then
				this_dir=`dirname $0|sed "s#^.#$this_dir#"`
			else
				this_dir=`dirname $0|sed "s#^#$this_dir/#"`
			fi
fi
echo $this_dir
cd $this_dir

game_server_path=$this_dir

setevn_path="$this_dir/global_config/$(cat config.txt)"
echo "setevn_path: $setevn_path"

file_time="$(date +"%Y_%m_%d_%H_%M_%S")"
source $setevn_path/setEvn.sh
# ---------------------------------------------------------------------------
# Setup start options
# ---------------------------------------------------------------------------

# executive JVM
EXEC_JAVA=java

# other options
OTHER_OPT="-server -d64"

# memory options
MEM_CONFIG="-Xms$TOTAL_AVAILABLE_MEMORY -Xmx$TOTAL_AVAILABLE_MEMORY -Xmn$YOUNG_EDEN -Xss256k -XX:PermSize=$PERM_SIZE -XX:MaxPermSize=$MAX_PERM_SIZE"

# Debug options
DEBUG="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=n"

# JVM action options
JVM_ACTION_OPT="-XX:+DisableExplicitGC -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+UseBiasedLocking -XX:+AggressiveOpts"

# JVM optimize options
JVM_OPTIMIZE_OPT="-XX:+OptimizeStringConcat -XX:+UseCompressedStrings -XX:+UseStringCache -XX:+UseFastAccessorMethods -XX:+UseSpinning -XX:PreBlockSpin=10"

# garbage collection options
GARBAGE_COLLECTION="-XX:SurvivorRatio=65535 -XX:MaxTenuringThreshold=0 -XX:+DisableExplicitGC -XX:+UseParNewGC -XX:ParallelGCThreads=$CPU_NUM -XX:+UseAdaptiveSizePolicy -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:CMSFullGCsBeforeCompaction=10 -XX:CMSInitiatingOccupancyFraction=$CMS_FRACTION -XX:+CMSParallelRemarkEnabled"

# jvm log options
#GARBAGE_COLLECTION_LOG="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintHeapAtGC -Xloggc:logs/gc.log"

# jvm heap dump
#HEAP_DUMP="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=logs/heapdump.hprof"

# jmx config options
JCONSOLE_CONFIG="-Dcom.sun.management.jmxremote.port=$JMX_PORT -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Djava.rmi.server.hostname=$RMI_LOCAL"

# encoding config options
ENCODEING_CONFIG="-Dfile.encoding=UTF-8"

# Classpath config options
CLASSPATH="lib/*:./game-proxy.jar"

# main class
MAIN_CLASS="GoProxyServer"

echo " "
if [ -f "log_start.log" ]; then
	mv log_start.log z_log_start.$file_time.log
	echo "rename log_start.log ok"
else
	echo "log_start.log,file does not exist"
fi

if [ -f "logs/monitor.err.log" ]; then
	mv logs/monitor.err.log logs/monitor.err.log.$file_time
	echo "rename logs/monitor.err.log ok"
else
	echo "logs/monitor.err.log,File does not exist"
fi
echo " "

nohup $EXEC_JAVA $OTHER_OPT $DEBUG $MEM_CONFIG $JVM_ACTION_OPT $GARBAGE_COLLECTION $GARBAGE_COLLECTION_LOG $HEAP_DUMP $JCONSOLE_CONFIG $ENCODEING_CONFIG -classpath $CLASSPATH $MAIN_CLASS > $game_server_path/log_start.log &

exit 0


