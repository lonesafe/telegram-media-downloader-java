@echo off
cd /d G:\workSpace\telegram-media-downloader-java\backend
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "PATH=%JAVA_HOME%\bin;D:\tools\apache-maven-3.9.6\bin;%PATH%"
D:\tools\apache-maven-3.9.6\bin\mvn.cmd compile -q 2>&1