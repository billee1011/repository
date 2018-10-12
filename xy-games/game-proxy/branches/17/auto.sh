echo '重启构建项目'

#编译公共模块
echo '=================编译公共模块 game-common=========================='
svn up /opt/game/game-common/
mvn -file /opt/game/game-common/pom.xml clean install
echo '================编译公共模块 game-common 完成========================'
#

cd /opt/game/game-proxy

cp wzzy_server_start_stop_restart_update.sh target/

./target/wzzy_server_start_stop_restart_update.sh stop

#svn up

svn up

#编译
mvn clean package -DskipTests -o

#配置文件
cp -r global_config ./target/
cp -r lib/* target/lib/
cp config.txt setEvn.sh start.sh wzzy_server_start_stop_restart_update.sh target/
./target/wzzy_server_start_stop_restart_update.sh start

sleep 2

#tail -f ./target/log_start.log
tail -n 200 ./target/log_start.log