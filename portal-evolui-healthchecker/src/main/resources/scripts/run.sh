nohup java -Xmx2048M -Xms1024M -jar "-Dfile.encoding=UTF-8" "-Dlog4j.configuration=file:/sistemas/healthchecker/log4j.properties" /sistemas/healthchecker/healthchecker.jar -run > /dev/null 2>&1 &
