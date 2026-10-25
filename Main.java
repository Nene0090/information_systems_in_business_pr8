import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Запуск:
 *   java Main                      - обработать data/sample.csv (id;ops;expected)
 *   java Main путь/к/файлу.csv     - обработать указанный файл
 *   java Main --line "create:T1:2026-12-01:2026-12-05|submit|approve"   - выполнить один сценарий
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length == 2 && args[0].equals("--line")) {
            System.out.println(new TripScenarioRunner().run(args[1]));
            return;
        }
        String path = args.length == 1 ? args[0] : "data/sample.csv";
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (int i = 1; i < lines.size(); i++) { // строка 0 - заголовок
            String[] c = lines.get(i).split(";", -1);
            if (c.length < 3) {
                continue;
            }
            System.out.println(c[0] + ": " + new TripScenarioRunner().run(c[1]));
        }
    }
}
