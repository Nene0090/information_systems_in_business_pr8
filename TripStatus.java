/**
 * Состояния заявки на командировку и ТАБЛИЦА допустимых переходов (единственный источник правды).
 * DRAFT -> SUBMITTED | CANCELLED
 * SUBMITTED -> APPROVED | REJECTED | CANCELLED
 * APPROVED -> COMPLETED | CANCELLED
 * REJECTED, CANCELLED, COMPLETED - конечные состояния.
 * Дополнительные условия (даты, причина, срок отмены) проверяет BusinessTrip.
 */
public enum TripStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    public boolean canMoveTo(TripStatus target) {
        switch (this) {
            case DRAFT:
                return target == SUBMITTED || target == CANCELLED;
            case SUBMITTED:
                return target == APPROVED || target == REJECTED || target == CANCELLED;
            case APPROVED:
                return target == COMPLETED || target == CANCELLED;
            default:
                return false; // REJECTED, CANCELLED, COMPLETED - конечные
        }
    }
}
