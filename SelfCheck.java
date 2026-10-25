import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Самопроверка без внешних библиотек. Код возврата 1, если хотя бы одна проверка не прошла.
 * Запуск: java SelfCheck [путь к sample.csv]
 */
public class SelfCheck {

    private static int passed = 0;
    private static int failed = 0;
    private static final Employee E = new Employee("E1", "Иван Петров");
    private static final LocalDate START = LocalDate.of(2026, 12, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 5);
    private static final String REASON = "Причина отклонения достаточно длинная";

    public static void main(String[] args) throws IOException {
        try {
            runAll(args);
        } catch (RuntimeException e) {
            failed++;
            System.out.println("FAIL  неожиданное исключение в проверке: " + e);
        }    }

    private static void runAll(String[] args) throws IOException {
        System.out.println("--- Таблица переходов: каждое состояние x каждая операция ---");
        String[][] matrix = {
            {"DRAFT", "submit", "SUBMITTED"},
            {"DRAFT", "approve", null},
            {"DRAFT", "reject", null},
            {"DRAFT", "cancel", "CANCELLED"},
            {"DRAFT", "complete", null},
            {"SUBMITTED", "submit", null},
            {"SUBMITTED", "approve", "APPROVED"},
            {"SUBMITTED", "reject", "REJECTED"},
            {"SUBMITTED", "cancel", "CANCELLED"},
            {"SUBMITTED", "complete", null},
            {"APPROVED", "submit", null},
            {"APPROVED", "approve", null},
            {"APPROVED", "reject", null},
            {"APPROVED", "cancel", "CANCELLED"},
            {"APPROVED", "complete", "COMPLETED"},
            {"REJECTED", "submit", null},
            {"REJECTED", "approve", null},
            {"REJECTED", "reject", null},
            {"REJECTED", "cancel", null},
            {"REJECTED", "complete", null},
            {"CANCELLED", "submit", null},
            {"CANCELLED", "approve", null},
            {"CANCELLED", "reject", null},
            {"CANCELLED", "cancel", null},
            {"CANCELLED", "complete", null},
            {"COMPLETED", "submit", null},
            {"COMPLETED", "approve", null},
            {"COMPLETED", "reject", null},
            {"COMPLETED", "cancel", null},
            {"COMPLETED", "complete", null},
        };
        for (String[] row : matrix) {
            transition(row[0], row[1], row[2]);
        }
        int allowed = 0;
        for (TripStatus a : TripStatus.values()) {
            for (TripStatus b : TripStatus.values()) {
                if (a.canMoveTo(b)) {
                    allowed++;
                }
            }
        }
        check("в enum ровно 7 допустимых пар переходов", allowed == 7);

        System.out.println("--- Конструктор: id, сотрудник, даты ---");
        expectIae("пустой id", () -> new BusinessTrip(" ", E, START, END));
        expectIae("null id", () -> new BusinessTrip(null, E, START, END));
        expectIae("null сотрудник", () -> new BusinessTrip("T", null, START, END));
        expectIae("задано только начало", () -> new BusinessTrip("T", E, START, null));
        expectIae("задан только конец", () -> new BusinessTrip("T", E, null, END));
        expectIae("конец раньше начала на 1 день", () -> new BusinessTrip("T", E, END, END.minusDays(5)));
        check("командировка в один день допустима (начало = конец)", new BusinessTrip("T", E, START, START).getEnd().equals(START));
        check("черновик без дат допустим", new BusinessTrip("T", E, null, null).getStatus() == TripStatus.DRAFT);
        check("новая заявка в состоянии DRAFT, причины нет",
                trip().getStatus() == TripStatus.DRAFT && trip().getRejectionReason() == null);

        System.out.println("--- planDates и обязательные данные для submit ---");
        BusinessTrip d = new BusinessTrip("T", E, null, null);
        expectIse("submit без дат", d::submit);
        check("после отказа статус DRAFT", d.getStatus() == TripStatus.DRAFT);
        expectIae("planDates: конец раньше начала", () -> d.planDates(END, START));
        expectIae("planDates: null дата", () -> d.planDates(START, null));
        check("после отказов даты не заданы", d.getStart() == null && d.getEnd() == null);
        d.planDates(START, END);
        d.submit();
        check("после planDates submit проходит", d.getStatus() == TripStatus.SUBMITTED);
        expectIse("planDates после submit", () -> d.planDates(START, END));

        System.out.println("--- Причина отклонения: минимум 10 символов ---");
        expectIae("reject: null", () -> submitted().reject(null));
        expectIae("reject: пустая", () -> submitted().reject(""));
        expectIae("reject: только пробелы", () -> submitted().reject("            "));
        expectIae("reject: 9 символов", () -> submitted().reject("123456789"));
        expectIae("reject: 9 символов + пробелы по краям", () -> submitted().reject("   123456789   "));
        BusinessTrip r10 = submitted();
        r10.reject("1234567890");
        check("reject: ровно 10 символов допустимо", r10.getStatus() == TripStatus.REJECTED);
        BusinessTrip rt = submitted();
        rt.reject("  Нет бюджета на поездку  ");
        check("причина сохраняется без пробелов по краям", rt.getRejectionReason().equals("Нет бюджета на поездку"));
        BusinessTrip rs = submitted();
        try {
            rs.reject("short");
        } catch (IllegalArgumentException e) {
            // ожидаемо
        }
        check("после отказа статус SUBMITTED и причина не записана",
                rs.getStatus() == TripStatus.SUBMITTED && rs.getRejectionReason() == null);
        expectIse("reject из DRAFT с коротким текстом: сначала проверка состояния", () -> trip().reject("x"));

        System.out.println("--- Завершение: строго после даты окончания ---");
        expectIse("complete: за день до окончания", () -> approved().complete(END.minusDays(1)));
        expectIse("complete: в день окончания", () -> approved().complete(END));
        BusinessTrip c1 = approved();
        c1.complete(END.plusDays(1));
        check("complete: на следующий день после окончания", c1.getStatus() == TripStatus.COMPLETED);
        expectIae("complete: null дата", () -> approved().complete(null));

        System.out.println("--- Правило отмены (вариант 5: APPROVED не позднее чем за 30 дней) ---");
        expectIse("cancel APPROVED: осталось 29 дней", () -> approved().cancel(START.minusDays(29)));
        BusinessTrip c30 = approved();
        c30.cancel(START.minusDays(30));
        check("cancel APPROVED: осталось ровно 30 дней - допустимо", c30.getStatus() == TripStatus.CANCELLED);
        BusinessTrip c31 = approved();
        c31.cancel(START.minusDays(31));
        check("cancel APPROVED: осталось 31 день - допустимо", c31.getStatus() == TripStatus.CANCELLED);
        expectIse("cancel APPROVED: командировка уже началась", () -> approved().cancel(START.plusDays(1)));
        BusinessTrip c29 = approved();
        try {
            c29.cancel(START.minusDays(29));
        } catch (IllegalStateException e) {
            // ожидаемо
        }
        check("после отказа статус остался APPROVED", c29.getStatus() == TripStatus.APPROVED);
        BusinessTrip cs = submitted();
        cs.cancel(START.minusDays(1));
        check("cancel SUBMITTED накануне начала допустим (правило только для APPROVED)", cs.getStatus() == TripStatus.CANCELLED);
        BusinessTrip cd = trip();
        cd.cancel(START.plusDays(100));
        check("cancel DRAFT допустим при любой дате", cd.getStatus() == TripStatus.CANCELLED);
        expectIae("cancel: null дата", () -> trip().cancel(null));
        expectIse("cancel после COMPLETED", () -> completed().cancel(START.minusDays(100)));

        System.out.println("--- Нет произвольной установки статуса ---");
        boolean hasSetter = false;
        for (Method m : BusinessTrip.class.getDeclaredMethods()) {
            if (m.getName().equals("setStatus")) {
                hasSetter = true;
            }
            if (Modifier.isPublic(m.getModifiers())) {
                for (Class<?> p : m.getParameterTypes()) {
                    if (p == TripStatus.class) {
                        hasSetter = true;
                    }
                }
            }
        }
        check("у BusinessTrip нет setStatus и public-методов с параметром TripStatus (рефлексия)", !hasSetter);
        boolean publicField = false;
        for (java.lang.reflect.Field f : BusinessTrip.class.getDeclaredFields()) {
            if (Modifier.isPublic(f.getModifiers())) {
                publicField = true;
            }
        }
        check("у BusinessTrip нет public-полей", !publicField);

        System.out.println("--- Сервис ---");
        BusinessTripService s = new BusinessTripService();
        BusinessTrip a = s.create("A", E, START, END);
        s.create("B", E, START, END).submit();
        expectIae("дубликат id", () -> s.create("A", E, START, END));
        check("find возвращает созданную заявку", s.find("A") == a);
        expectIae("find: неизвестный id", () -> s.find("Z"));
        expectIae("find: null", () -> s.find(null));
        check("findByStatus(DRAFT) = 1", s.findByStatus(TripStatus.DRAFT).size() == 1);
        check("findByStatus(SUBMITTED) = 1", s.findByStatus(TripStatus.SUBMITTED).size() == 1);
        check("findByStatus(COMPLETED) пуст", s.findByStatus(TripStatus.COMPLETED).isEmpty());
        expectIae("findByStatus: null", () -> s.findByStatus(null));
        expectUnsupported("findByStatus: список только для чтения", () -> s.findByStatus(TripStatus.DRAFT).clear());
        check("после попытки изменения хранилище целое", s.findByStatus(TripStatus.DRAFT).size() == 1);

        System.out.println("--- Сценарии (TripScenarioRunner) ---");
        scenario("пустой сценарий", "", "ERROR|IllegalArgumentException|-");
        scenario("null вместо сценария", null, "ERROR|IllegalArgumentException|-");
        scenario("операция до create", "submit", "ERROR|IllegalArgumentException|-");
        scenario("неизвестная операция", "create:T:2026-12-01:2026-12-05|ship", "ERROR|IllegalArgumentException|DRAFT");
        scenario("пустая операция", "create:T:2026-12-01:2026-12-05||submit", "ERROR|IllegalArgumentException|DRAFT");
        scenario("неверный формат даты", "create:T:01.12.2026:05.12.2026", "ERROR|IllegalArgumentException|-");
        scenario("plan + submit", "create:T::|plan:2026-12-01:2026-12-05|submit", "OK|SUBMITTED");

        System.out.println("--- Файл sample.csv ---");
        checkSampleCsv(args.length == 1 ? args[0] : "data/sample.csv");

        System.out.println();
        System.out.println("Итого: пройдено " + passed + ", не пройдено " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** Проверка одной ячейки таблицы переходов: либо новый статус, либо IllegalStateException без изменения. */
    private static void transition(String from, String op, String to) {
        BusinessTrip t = tripIn(TripStatus.valueOf(from));
        String reasonBefore = t.getRejectionReason();
        String name = from + " --" + op + "--> " + (to == null ? "запрещено" : to);
        try {
            switch (op) {
                case "submit": t.submit(); break;
                case "approve": t.approve(); break;
                case "reject": t.reject(REASON); break;
                case "cancel": t.cancel(START.minusDays(100)); break;
                default: t.complete(END.plusDays(1)); break;
            }
            check(name, to != null && t.getStatus() == TripStatus.valueOf(to));
        } catch (IllegalStateException e) {
            boolean unchanged = t.getStatus() == TripStatus.valueOf(from)
                    && java.util.Objects.equals(reasonBefore, t.getRejectionReason());
            check(name + " (статус и причина не изменены)", to == null && unchanged);
        }
    }

    private static BusinessTrip tripIn(TripStatus st) {
        BusinessTrip t = trip();
        switch (st) {
            case DRAFT: break;
            case SUBMITTED: t.submit(); break;
            case APPROVED: t.submit(); t.approve(); break;
            case REJECTED: t.submit(); t.reject(REASON); break;
            case CANCELLED: t.cancel(START.minusDays(100)); break;
            default: t.submit(); t.approve(); t.complete(END.plusDays(1)); break;
        }
        return t;
    }

    private static BusinessTrip trip() {
        return new BusinessTrip("T", E, START, END);
    }

    private static BusinessTrip submitted() {
        return tripIn(TripStatus.SUBMITTED);
    }

    private static BusinessTrip approved() {
        return tripIn(TripStatus.APPROVED);
    }

    private static BusinessTrip completed() {
        return tripIn(TripStatus.COMPLETED);
    }

    private static void scenario(String name, String ops, String expected) {
        String actual = new TripScenarioRunner().run(ops);
        check("сценарий: " + name + " -> " + expected, actual.startsWith(expected));
    }

    private static void checkSampleCsv(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (int i = 1; i < lines.size(); i++) {
            String[] c = lines.get(i).split(";", -1);
            String actual = new TripScenarioRunner().run(c[1]);
            check("CSV " + c[0] + " (ожидалось " + c[2] + ")", actual.startsWith(c[2]));
        }
    }

    private static void expectIae(String name, Runnable action) {
        expect(name, action, IllegalArgumentException.class);
    }

    private static void expectIse(String name, Runnable action) {
        expect(name, action, IllegalStateException.class);
    }

    private static void expectUnsupported(String name, Runnable action) {
        expect(name, action, UnsupportedOperationException.class);
    }

    private static void expect(String name, Runnable action, Class<? extends RuntimeException> type) {
        try {
            action.run();
            check(name + " (исключение не возникло)", false);
        } catch (RuntimeException e) {
            check(name + (e.getClass() == type ? "" : " (получено " + e.getClass().getSimpleName() + ")"),
                    e.getClass() == type);
        }
    }

    private static void check(String name, boolean cond) {
        if (cond) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }
}
