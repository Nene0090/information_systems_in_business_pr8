import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Заявка на командировку. Статус меняется только доменными методами
 * submit, approve, reject, cancel, complete; открытого setter статуса нет.
 *
 * Инварианты, которые защищает объект:
 *  - id и сотрудник заданы всегда;
 *  - если даты заданы, то конец не раньше начала;
 *  - статус меняется только по таблице переходов TripStatus и только вместе с проверкой данных операции;
 *  - rejectionReason заполнена тогда и только тогда, когда статус REJECTED, и содержит не меньше 10 символов;
 *  - при любой ошибке объект не изменяется (сначала все проверки, потом присваивание).
 * Исключения: IllegalStateException - операция недопустима в текущем состоянии объекта;
 * IllegalArgumentException - некорректный аргумент операции.
 * В каждой операции сначала проверяется состояние, затем аргументы.
 */
public class BusinessTrip {

    /** Минимальная длина причины отклонения (после trim). */
    private static final int MIN_REASON_LENGTH = 10;
    /**
     * Вариант 5 ("правило отмены", модификатор 30): отмена УЖЕ ОДОБРЕННОЙ (APPROVED) командировки
     * допустима, только если до её начала осталось не меньше этого числа дней. Это единственное место с порогом.
     */
    private static final int CANCEL_NOTICE_DAYS = 30;

    private final String id;
    private final Employee employee;
    private LocalDate start;
    private LocalDate end;
    private TripStatus status = TripStatus.DRAFT;
    private String rejectionReason;

    /** Даты можно не задавать (оба null) - черновик; тогда submit() будет невозможен до planDates. */
    public BusinessTrip(String id, Employee employee, LocalDate start, LocalDate end) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Идентификатор заявки не задан");
        }
        if (employee == null) {
            throw new IllegalArgumentException("Сотрудник не задан");
        }
        if (start != null || end != null) {
            checkDates(start, end);
        }
        this.id = id.trim();
        this.employee = employee;
        this.start = start;
        this.end = end;
    }

    /** Задать или изменить даты; только в черновике. */
    public void planDates(LocalDate newStart, LocalDate newEnd) {
        if (status != TripStatus.DRAFT) {
            throw new IllegalStateException("Даты можно менять только в состоянии DRAFT, сейчас " + status);
        }
        checkDates(newStart, newEnd);
        start = newStart;
        end = newEnd;
    }

    /** DRAFT -> SUBMITTED при заполненных обязательных данных (даты). */
    public void submit() {
        requireTransition(TripStatus.SUBMITTED, "submit");
        if (start == null || end == null) {
            throw new IllegalStateException("Не заполнены обязательные данные: даты командировки");
        }
        status = TripStatus.SUBMITTED;
    }

    /** SUBMITTED -> APPROVED. */
    public void approve() {
        requireTransition(TripStatus.APPROVED, "approve");
        status = TripStatus.APPROVED;
    }

    /** SUBMITTED -> REJECTED; причина обязательна, не короче 10 символов (после trim). */
    public void reject(String reason) {
        requireTransition(TripStatus.REJECTED, "reject");
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Причина отклонения обязательна");
        }
        String trimmed = reason.trim();
        if (trimmed.length() < MIN_REASON_LENGTH) {
            throw new IllegalArgumentException("Причина отклонения короче " + MIN_REASON_LENGTH
                    + " символов (" + trimmed.length() + ")");
        }
        status = TripStatus.REJECTED;
        rejectionReason = trimmed;
    }

    /**
     * DRAFT, SUBMITTED, APPROVED -> CANCELLED. Вариант 5: для APPROVED до начала командировки
     * должно оставаться не меньше CANCEL_NOTICE_DAYS дней относительно даты today.
     */
    public void cancel(LocalDate today) {
        requireTransition(TripStatus.CANCELLED, "cancel");
        if (today == null) {
            throw new IllegalArgumentException("Текущая дата не задана");
        }
        if (status == TripStatus.APPROVED) {
            long daysLeft = ChronoUnit.DAYS.between(today, start);
            if (daysLeft < CANCEL_NOTICE_DAYS) {
                throw new IllegalStateException("Одобренную командировку можно отменить не позднее чем за "
                        + CANCEL_NOTICE_DAYS + " дней до начала, осталось " + daysLeft);
            }
        }
        status = TripStatus.CANCELLED;
    }

    /** APPROVED -> COMPLETED, только если today строго позже даты окончания. */
    public void complete(LocalDate today) {
        requireTransition(TripStatus.COMPLETED, "complete");
        if (today == null) {
            throw new IllegalArgumentException("Текущая дата не задана");
        }
        if (!today.isAfter(end)) {
            throw new IllegalStateException("Завершить можно только после даты окончания " + end
                    + ", текущая дата " + today);
        }
        status = TripStatus.COMPLETED;
    }

    public String getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public TripStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    private void requireTransition(TripStatus target, String operation) {
        if (!status.canMoveTo(target)) {
            throw new IllegalStateException("Недопустимый переход " + status + " -> " + target
                    + " (операция " + operation + ")");
        }
    }

    private static void checkDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Нужно задать обе даты: начало и окончание");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Дата окончания " + end + " раньше даты начала " + start);
        }
    }
}
