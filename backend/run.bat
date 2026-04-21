@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17
set M2_HOME=G:\workSpace\telegram-media-downloader-java\tools\maven\apache-maven-3.9.6
set MAVEN_HOME=G:\workSpace\telegram-media-downloader-java\tools\maven\apache-maven-3.9.6
set PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%
cd /d G:\workSpace\telegram-media-downloader-java\backend
echo Current directory: %CD%
echo JAVA_HOME: %JAVA_HOME%
echo.
echo Starting Spring Boot application...
call mvn spring-boot:run
