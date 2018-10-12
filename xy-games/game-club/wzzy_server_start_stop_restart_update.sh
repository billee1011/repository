#!/bin/bash
# wzzy_server_start_stop_restart_update.sh: Game server console.
#


###########################################################################################################
#
# config
#

waiting_server_shutdown_time=45

###########################################################################################################

# date

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
echo " "
echo "        this_dir: $this_dir"

game_server_path=$this_dir
echo "game_server_path: $game_server_path"

setevn_path="$this_dir/global_config/$(cat config.txt)"
echo "setevn_path: $setevn_path"

###############################################################

file_time="$(date +"%Y_%m_%d_%H_%M_%S")"
wzzy_server_sh_start=$game_server_path/start.sh
wzzy_server_sh_stop=$game_server_path/shutdown.sh
server_sign=`cat $setevn_path/setEvn.sh |grep DEBUG_PORT|awk -F= '{print $2}'`
game_server_pid=`ps -aef | grep java | grep "$server_sign" |grep -v grep |awk '{print $2}'`

############################################################################################################

cd $game_server_path
#echo " "
echo "     server_sign: $server_sign"
echo " game_server_pid: $game_server_pid"

case "$1" in

#============================================================================================================

start)

	game_server_pid=`ps -aef | grep java | grep "$server_sign" |grep -v grep |awk '{print $2}'`
	
	if [ -z $game_server_pid ]; then	
		echo " "
	else
		echo " "
		echo "   server status:"
		echo " "
		ps -aef | grep java | grep "$server_sign" | grep -v "grep"
	fi
	
	echo " "
	if [ -z $game_server_pid ]; then
	
		# if [ -f "nohup.out" ]; then
			# mv nohup.out nohup.out.$file_time
			# echo "rename nohup.out ok"
		# else
			# echo "nohup.out,file does not exist"
		# fi

		# if [ -f "logs/monitor.err.log" ]; then
			# mv logs/monitor.err.log logs/monitor.err.log.$file_time
			# echo "rename logs/monitor.err.log ok"
		# else
			# echo "logs/monitor.err.log,File does not exist"
		# fi		
		
		echo " "
		echo "== Begin to start server =="
		echo " "
		sh $wzzy_server_sh_start
		sleep 3
		echo " "
		echo "== Check server status =="
		echo " "
		ps -aef | grep java | grep "$server_sign" | grep -v "grep"
		echo " "
		game_server_pid=`ps -aef | grep java | grep "$server_sign" |grep -v grep |awk '{print $2}'`
		echo " "
		echo "Current game server: "
		echo "game_server_pid: $game_server_pid"
		echo " "
		echo "All game server:"
		jps | grep -v "Jps"
		echo " "
		echo "...."
		echo "Game server has been online."
		echo "...."
		echo " "
	else
		echo "Current game server: "
		echo "game_server_pid: $game_server_pid"
		echo " "
		echo "All game server:"
		jps | grep -v "Jps"
		echo " "
		echo "...."
		echo "Server has been online,Please check server status."
		echo "...."
		echo " "

	fi
    ;;

#============================================================================================================
	
stop)

	echo ps -aef | grep java | grep "$server_sign" | grep -v "grep"
	ps -aef | grep java | grep "$server_sign" | grep -v "grep"

	game_server_pid=`ps -aef | grep java | grep "$server_sign" |grep -v grep |awk '{print $2}'`
#	echo "game_server_pid: $game_server_pid"

	if [ -z $game_server_pid ]; then
		echo " "
		echo "Current game server: "
		echo "game_server_pid: $game_server_pid"
		echo " "
		echo "All game server:"
		jps | grep -v "Jps"
		echo " "
		echo "Game server does not start."
	else
		echo ""
		echo "== begin to shutdown server =="
		echo ""
		kill $game_server_pid
		echo " "
		date
		echo "...."
		echo "Waiting for server shutdown,Please do not exit the current screen or Ctrl + C."
		echo "...."
#		sleep $waiting_server_shutdown_time

		for((i=1;i<$waiting_server_shutdown_time;i++));do
			game_server_pid=`ps -aef | grep java | grep "$server_sign" |grep -v grep |awk '{print $2}'`
			if [ -z $game_server_pid ]; then
				echo " "
				echo "Current game server: "
				echo "game_server_pid: $game_server_pid"
				echo " "
				echo "All game server:"
				jps | grep -v "Jps"
				echo " "
				echo "Game server has been shutdown."
				i=$waiting_server_shutdown_time
			else
				echo " "
				echo "Waiting for server shutdown,Please do not exit the current screen or Ctrl + C."
				echo " "
				echo "$i seconds.Check pid:"
				echo " "
				#jps
				echo "game_server_pid: $game_server_pid"
				echo " "
				sleep 1
			fi
		done
		
		echo " "
		date
	fi


#-----------------
	
	game_server_pid=`ps -aef | grep java | grep "$server_sign" |grep -v grep |awk '{print $2}'`
#	echo "game_server_pid: $game_server_pid"

#	if [ $game_server_pid > 0 ]; then		
	if [ -z $game_server_pid ]; then
#		echo "...."
#		echo "Game server has been shutdown."
#		echo "...."
		echo ""
#		echo "Check jps:"
#		jps
	else
		echo " "
		echo "Game_server_pid: $game_server_pid"
		echo " "
		echo "-------------------------------------------------------------------------"
		echo "- Shutdown game server timeout in $waiting_server_shutdown_time seconds.-"
		echo "-------------------------------------------------------------------------"
#		echo "So,Start forced to close."
#		echo " "
#		ps -ef | grep java |grep "$server_sign" |grep -v grep |awk '{print $2}'| xargs kill -9
#		echo "Kill game server's pid done."
#		echo " "
#		sleep 3
#		echo "Check jps:"
#		jps
	fi	


	
	echo " "
	ps -aef | grep java | grep "$server_sign" | grep -v "grep"
	echo " "
	echo "Current game server: "
	echo "game_server_pid: $game_server_pid"
	echo " "
	echo "All game server:"
	jps | grep -v "Jps"
	echo " "
    ;;

#============================================================================================================
	
restart)
kill $game_server_pid
$game_server_path/wzzy_server_start_stop_restart_update.sh start
	;;	   

#============================================================================================================
	
update)

$game_server_path/wzzy_server_start_stop_restart_update.sh stop

	
	echo " "
	date
	echo "== Begin to update server =="
	echo " "	
	
	svn update
	echo ""
	echo "The first update is completed."
	echo ""
	
	sleep 5
	date
	echo ""
	
	svn update
	echo ""
	echo "The second update is completed."
	echo ""

	echo " "
	echo "== update server done=="
	date
	echo " "	

$game_server_path/wzzy_server_start_stop_restart_update.sh start

	;;	   

	
#============================================================================================================
	
open_gameworld)

for((i=1;i<30;i++));do
	gameworld_status=`cat $game_server_path/service_status.txt`
	if [ $gameworld_status == 1 ]; then
		echo " "
		echo "2" > "$game_server_path/service_status.txt"
		echo ""
		echo "Input 2 to service_status.txt has been done."		
		echo ""
		i=30
	else
		echo ""
		echo "Waiting for $game_server_path start,Please do not exit the current screen or Ctrl + C."
		echo ""
		echo "$i seconds.Check status:"
		echo ""
		#jps
		echo "gameworld_status: $gameworld_status"
		echo ""
		sleep 1
	fi
done
	
	;;		

	
#============================================================================================================
	
status)

	echo " "
	ps -aef | grep java | grep "$server_sign" | grep -v "grep"
	echo " "
	echo "Current game server: "
	echo "game_server_pid: $game_server_pid"
	echo " "
	echo "All game server:"
	jps | grep -v "Jps"
	echo " "

	;;
	   
*)
	echo " "
    echo "Usage: $0 {start|open_gameworld|stop|restart|update|status}"
	echo " "
    exit 1
esac

#============================================================================================================

exit 0
