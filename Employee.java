/** Сотрудник-командированный. Неизменяемый объект. */
public class Employee {

    private final String id;
    private final String fullName;

    public Employee(String id, String fullName) {
        this.id = requireText(id, "Идентификатор сотрудника");
        this.fullName = requireText(fullName, "ФИО сотрудника");
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " не задан(о)");
        }
        return value.trim();
    }
}
