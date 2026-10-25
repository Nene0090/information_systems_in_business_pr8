# Практическая работа №8 (вариант 5)

Требуется JDK 17 или новее (проверено на JDK 21).

Сборка и запуск всех проверок и примера:

    ./build.sh

Вручную:

    javac -encoding UTF-8 -d out src/*.java
    java -cp out SelfCheck          # самопроверка, код возврата 1 при ошибке
    java -cp out Main               # обработка data/sample.csv
    java -cp out Main --line "..."  # один сценарий (формат описан в комментарии к Main)

Структура: src/ (исходный код), data/sample.csv (открытый набор: id;ops;expected), DECISIONS.md (два принципиальных решения).
