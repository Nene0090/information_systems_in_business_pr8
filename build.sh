#!/bin/sh
# Сборка и запуск. Нужен JDK 17+ (проверено на 21).
set -e
cd "$(dirname "$0")"
javac -encoding UTF-8 -d out src/*.java 2>/dev/null || java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d out src/*.java
echo "== SelfCheck =="
java -Dstdout.encoding=UTF-8 -cp out SelfCheck
echo
echo "== Main (data/sample.csv) =="
java -Dstdout.encoding=UTF-8 -cp out Main
