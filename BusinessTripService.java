import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Сервис: хранит заявки, следит за уникальностью id, ищет по статусу. Правил переходов здесь нет. */
public class BusinessTripService {

    private final List<BusinessTrip> trips = new ArrayList<>();

    public BusinessTrip create(String id, Employee employee, LocalDate start, LocalDate end) {
        BusinessTrip trip = new BusinessTrip(id, employee, start, end); // все проверки данных - в модели
        for (BusinessTrip t : trips) {
            if (t.getId().equals(trip.getId())) {
                throw new IllegalArgumentException("Заявка с id '" + trip.getId() + "' уже существует");
            }
        }
        trips.add(trip);
        return trip;
    }

    public BusinessTrip find(String id) {
        if (id != null) {
            for (BusinessTrip t : trips) {
                if (t.getId().equals(id.trim())) {
                    return t;
                }
            }
        }
        throw new IllegalArgumentException("Заявка '" + id + "' не найдена");
    }

    /** Копия списка только для чтения: внешний код не может изменить хранилище. */
    public List<BusinessTrip> findByStatus(TripStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Статус не задан");
        }
        List<BusinessTrip> result = new ArrayList<>();
        for (BusinessTrip t : trips) {
            if (t.getStatus() == status) {
                result.add(t);
            }
        }
        return Collections.unmodifiableList(result);
    }
}
