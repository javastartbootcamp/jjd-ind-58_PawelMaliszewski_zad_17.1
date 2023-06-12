package pl.javastart.streamsexercise;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class PaymentService {

    private final PaymentRepository paymentRepository;
    private final DateTimeProvider dateTimeProvider;

    PaymentService(PaymentRepository paymentRepository, DateTimeProvider dateTimeProvider) {
        this.paymentRepository = paymentRepository;
        this.dateTimeProvider = dateTimeProvider;
    }

    /*
        Znajdź i zwróć płatności posortowane po dacie rosnąco
         */
    List<Payment> findPaymentsSortedByDateAsc() {
        return paymentRepository.findAll().stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate))
                .collect(Collectors.toList());
    }

    /*
    Znajdź i zwróć płatności posortowane po dacie malejąco
     */
    List<Payment> findPaymentsSortedByDateDesc() {
        return paymentRepository.findAll().stream()
                .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                .collect(Collectors.toList());
    }

    /*
    Znajdź i zwróć płatności posortowane po liczbie elementów rosnąco
     */
    List<Payment> findPaymentsSortedByItemCountAsc() {
        return paymentRepository.findAll().stream()
                .sorted(Comparator.comparing(payment -> payment.getPaymentItems().size()))
                .collect(Collectors.toList());
    }

    /*
    Znajdź i zwróć płatności posortowane po liczbie elementów malejąco
     */
    List<Payment> findPaymentsSortedByItemCountDesc() {
        return paymentRepository.findAll().stream()
                .sorted(Comparator.comparing(payment -> -payment.getPaymentItems().size()))
                .collect(Collectors.toList());
    }

    /*
    Znajdź i zwróć płatności dla wskazanego miesiąca
     */
    List<Payment> findPaymentsForGivenMonth(YearMonth yearMonth) {
        return paymentRepository.findAll().stream()
                .filter(payment -> getPaymentDate(payment).equals(yearMonth))
                .collect(Collectors.toList());
    }

    private static YearMonth getPaymentDate(Payment payment) {
        return YearMonth.from(payment.getPaymentDate());
    }

    /*
    Znajdź i zwróć płatności dla aktualnego miesiąca
     */
    List<Payment> findPaymentsForCurrentMonth() {
        return paymentRepository.findAll().stream()
                .filter(this::isItFromCurrentYearAndMonth)
                .collect(Collectors.toList());
    }

    /*
    Znajdź i zwróć płatności dla ostatnich X dni
     */
    List<Payment> findPaymentsForGivenLastDays(int days) {
        return paymentRepository.findAll().stream()
                .filter(payment -> checkPaymentYear(days, payment))
                .filter(payment -> paymentBeforeDaysCount(days, payment))
                .collect(Collectors.toList());
    }

    private boolean paymentBeforeDaysCount(int days, Payment payment) {
        return payment.getPaymentDate().getDayOfYear() > dateTimeProvider.zonedDateTimeNow().minusDays(days).getDayOfYear();
    }

    private boolean checkPaymentYear(int days, Payment payment) {
        return payment.getPaymentDate().getYear() == dateTimeProvider.zonedDateTimeNow().minusDays(days).getYear();
    }

    /*
    Znajdź i zwróć płatności z jednym elementem
     */
    Set<Payment> findPaymentsWithOnePaymentItem() {
        return paymentRepository.findAll().stream()
                .filter(payment -> payment.getPaymentItems().size() == 1)
                .collect(Collectors.toSet());
    }

    /*
    Znajdź i zwróć nazwy produktów sprzedanych w aktualnym miesiącu
     */
    Set<String> findProductsSoldInCurrentMonth() {
        return paymentRepository.findAll().stream()
                .filter(this::isItFromCurrentYearAndMonth)
                .map(Payment::getPaymentItems)
                .flatMap(List::stream)
                .map(PaymentItem::getName)
                .collect(Collectors.toSet());
    }

    /*
    Policz i zwróć sumę sprzedaży dla wskazanego miesiąca
     */
    BigDecimal sumTotalForGivenMonth(YearMonth yearMonth) {
        return paymentRepository.findAll().stream()
                .filter(payment -> areTheDatesTheSame(payment, yearMonth))
                .map(Payment::getPaymentItems)
                .flatMap(List::stream)
                .map(PaymentItem::getFinalPrice)
                .reduce(BigDecimal.valueOf(0), BigDecimal::add);
    }

    /*
    Policz i zwróć sumę przyznanych rabatów dla wskazanego miesiąca
     */
    BigDecimal sumDiscountForGivenMonth(YearMonth yearMonth) {
        return paymentRepository.findAll().stream()
                .filter(payment -> areTheDatesTheSame(payment, yearMonth))
                .map(Payment::getPaymentItems)
                .flatMap(List::stream)
                .map(PaymentService::getPriceDifference)
                .reduce(BigDecimal.valueOf(0), BigDecimal::add);
    }

    private static BigDecimal getPriceDifference(PaymentItem paymentItem) {
        return paymentItem.getRegularPrice().subtract(paymentItem.getFinalPrice());
    }

    /*
    Znajdź i zwróć płatności dla użytkownika z podanym mailem
     */
    List<PaymentItem> getPaymentsForUserWithEmail(String userEmail) {
        return paymentRepository.findAll().stream()
                .filter(payment -> foundUser(userEmail, payment))
                .map(Payment::getPaymentItems)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private static boolean foundUser(String userEmail, Payment payment) {
        return payment.getUser().getEmail().equals(userEmail);
    }

    /*
    Znajdź i zwróć płatności, których wartość przekracza wskazaną granicę
     */
    Set<Payment> findPaymentsWithValueOver(int value) {
        return paymentRepository.findAll().stream()
                .filter(payment -> paymentsGraterThan(payment, value))
                .collect(Collectors.toSet());
    }

    private boolean paymentsGraterThan(Payment payment, int value) {
        BigDecimal bigDecimal = new BigDecimal(value);
        int result = payment.getPaymentItems().stream()
                .map(PaymentItem::getFinalPrice)
                .reduce(BigDecimal.valueOf(0), BigDecimal::add)
                .compareTo(BigDecimal.valueOf(value));
        return result > 0;
    }

    private boolean isItFromCurrentYearAndMonth(Payment payment) {
        boolean year = payment.getPaymentDate().getYear() == dateTimeProvider.zonedDateTimeNow().getYear();
        boolean month = payment.getPaymentDate().getMonth().equals(dateTimeProvider.zonedDateTimeNow().getMonth());
        return year && month;
    }

    private boolean areTheDatesTheSame(Payment payment, YearMonth yearMonth) {
        boolean year = payment.getPaymentDate().getYear() == yearMonth.getYear();
        boolean month = payment.getPaymentDate().getMonth().equals(yearMonth.getMonth());
        return year && month;
    }
}
