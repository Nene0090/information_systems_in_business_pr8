import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Выполняет текстовый сценарий над одной заявкой (для Main и SelfCheck). Операции разделяются '|':
 *   create:<id>:<начало>:<конец>   (даты ГГГГ-ММ-ДД; можно пустые), plan:<начало>:<конец>, submit, approve,
 *   reject:<причина>, cancel:<сегодня>, complete:<сегодня>.
 * Результат:
 *   OK|статус
 *   ERROR|тип исключения|статус заявки в момент ошибки ("-" если заявка не создана)|сообщение
 * Сценарий останавливается на первой ошибке; по статусу видно, что объект при ошибке не изменился.
 */
public class TripScenarioRunner {

    private final BusinessTripService service = new BusinessTripService();
    private final Employee employee = new Employee("E1", "Иван Петров");

    public String run(String ops) {
        BusinessTrip trip = null;
        try {
            if (ops == null || ops.trim().isEmpty()) {
                throw new IllegalArgumentException("Сценарий не задан");
            }
            String[] tokens = ops.split("\\|", -1);
            for (int i = 0; i < tokens.length; i++) {
                String op = tokens[i].trim();
                if (op.isEmpty()) {
                    throw new IllegalArgumentException("Операция " + (i + 1) + ": пустая операция");
                }
                trip = apply(trip, op, i + 1);
            }
            return "OK|" + trip.getStatus();
        } catch (IllegalArgumentException | IllegalStateException e) {
            String status = trip == null ? "-" : trip.getStatus().name();
            return "ERROR|" + e.getClass().getSimpleName() + "|" + status + "|" + e.getMessage();
        }
    }

    private BusinessTrip apply(BusinessTrip trip, String op, int position) {
        String name = op.contains(":") ? op.substring(0, op.indexOf(':')) : op;
        String rest = op.contains(":") ? op.substring(op.indexOf(':') + 1) : "";
        if (name.equals("create")) {
            String[] a = rest.split(":", -1);
            if (a.length != 3) {
                throw new IllegalArgumentException("Операция " + position + ": ожидается create:<id>:<начало>:<конец>");
            }
            return service.create(a[0], employee, parseDate(a[1]), parseDate(a[2]));
        }
        if (trip == null) {
            throw new IllegalArgumentException("Операция " + position + ": сначала нужно выполнить create");
        }
        switch (name) {
            case "plan": {
                String[] a = rest.split(":", -1);
                if (a.length != 2) {
                    throw new IllegalArgumentException("Операция " + position + ": ожидается plan:<начало>:<конец>");
                }
                trip.planDates(parseDate(a[0]), parseDate(a[1]));
                break;
            }
            case "submit":
                trip.submit();
                break;
            case "approve":
                trip.approve();
                break;
            case "reject":
                trip.reject(rest);
                break;
            case "cancel":
                trip.cancel(parseDate(rest));
                break;
            case "complete":
                trip.complete(parseDate(rest));
                break;
            default:
                throw new IllegalArgumentException("Операция " + position + ": неизвестная операция '" + name + "'");
        }
        return trip;
    }

    /** Пустая строка даёт null; несуществующая календарная дата (2026-02-30) - контролируемая ошибка. */
    private LocalDate parseDate(String s) {
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверная дата '" + t + "' (ожидается ГГГГ-ММ-ДД)");
        }
    }
}
