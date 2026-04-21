@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=C:\Program Files\Java\jdk-17\bin;D:\tools\apache-maven-3.9.6\bin;%PATH%"
call mvn -f "G:\workSpace\telegram-media-downloader-java\backend\pom.xml" compile
