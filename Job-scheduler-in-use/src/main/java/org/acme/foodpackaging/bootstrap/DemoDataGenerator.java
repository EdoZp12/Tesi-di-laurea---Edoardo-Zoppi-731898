package org.acme.foodpackaging.bootstrap;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.quarkus.runtime.StartupEvent;
import org.acme.foodpackaging.domain.Job;
import org.acme.foodpackaging.domain.Line;
import org.acme.foodpackaging.domain.ProductionSchedule;
import org.acme.foodpackaging.domain.Product;
import org.acme.foodpackaging.domain.WorkCalendar;
import org.acme.foodpackaging.persistence.ProductionScheduleRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DemoDataGenerator {

    @Inject
    ProductionScheduleRepository repository;

    @ConfigProperty(name = "demo-data.line-count", defaultValue = "5")
    int lineCount;
    @ConfigProperty(name = "demo-data.job-count", defaultValue = "100")
    int jobCount;

    @Transactional
    public void generateDemoData(@Observes StartupEvent startupEvent) {
        int noCleaningMinutes = 10;
        int cleaningMinutesMinimum = 30;
        int cleaningMinutesMaximum = 60;
        int jobDurationMinutesMinimum = 120;
        int jobDurationMinutesMaximum = 300;
        int averageCleaningAndJobDurationMinutes =
                (2 * noCleaningMinutes + cleaningMinutesMinimum + cleaningMinutesMaximum) / 4
                + (jobDurationMinutesMinimum + jobDurationMinutesMaximum) / 2;

        final LocalDate START_DATE = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        final LocalDateTime START_DATE_TIME = LocalDateTime.of(START_DATE, LocalTime.MIDNIGHT);
        final LocalDate END_DATE = START_DATE.plusWeeks(2);
        final LocalDateTime END_DATE_TIME = LocalDateTime.of(END_DATE, LocalTime.MIDNIGHT);

        Random random = new Random(37);
        ProductionSchedule solution = new ProductionSchedule();

        solution.setWorkCalendar(new WorkCalendar(START_DATE, END_DATE));

        Map<Product, Set<String>> ordiniMap = new HashMap<>(PRODUCT_TYPE_LIST.size() * PRODUCT_VARIATION_LIST.size() * 3);
        long productId = 0;
        for (int i = 0; i < PRODUCT_TYPE_LIST.size(); i++) {
            String ingredient = PRODUCT_TYPE_LIST.get(i);
            int r = random.nextInt(PRODUCT_TYPE_LIST.size() - 4);
//            String ingredientA = PRODUCT_TYPE_LIST.get((i + r + 1) % PRODUCT_TYPE_LIST.size());
//            String ingredientB = PRODUCT_TYPE_LIST.get((i + r + 2) % PRODUCT_TYPE_LIST.size());
//            String ingredientC = PRODUCT_TYPE_LIST.get((i + r + 3) % PRODUCT_TYPE_LIST.size());
            for (String productVariation : PRODUCT_VARIATION_LIST) {
                ordiniMap.put(new Product(productId++, ingredient + " " + productVariation), Set.of(ingredient));
            }
//            ingredientMap.put(new Product(productId++, ingredient + " and " + ingredientA + " " + PRODUCT_VARIATION_LIST.get(1)), Set.of(ingredient, ingredientA));
//            ingredientMap.put(new Product(productId++, ingredient + " and " + ingredientB + " " + PRODUCT_VARIATION_LIST.get(2)), Set.of(ingredient, ingredientB));
//            ingredientMap.put(new Product(productId++, ingredient + ", " + ingredientA + " and " + ingredientC + " " + PRODUCT_VARIATION_LIST.get(1)), Set.of(ingredient, ingredientA, ingredientC));
        }
        ArrayList<Product> productList = new ArrayList<>(ordiniMap.keySet());
        for (Product product : productList) {
            Map<Product, Duration> cleaningDurationMap = new HashMap<>(productList.size());
            Set<String> ingredientSet = ordiniMap.get(product);
            for (Product previousProduct : productList) {
                boolean noCleaning = ingredientSet.containsAll(ordiniMap.get(previousProduct));
                Duration cleaningDuration = Duration.ofMinutes(product == previousProduct ? 0
                        : noCleaning ? noCleaningMinutes
                        : cleaningMinutesMinimum + random.nextInt(cleaningMinutesMaximum - cleaningMinutesMinimum));
                cleaningDurationMap.put(previousProduct, cleaningDuration);
            }
            product.setCleaningDurationMap(cleaningDurationMap);
        }
        solution.setProductList(productList);

        List<Line> lineList = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            String name = "Line " + (i + 1);
            String operator = "Operator " + ((char) ('A' + (i / 2)));
            lineList.add(new Line((long) i, name, operator, START_DATE_TIME));
        }
        solution.setLineList(lineList);

        List<Job> jobList = new ArrayList<>(jobCount);
        for (int i = 0; i < jobCount; i++) {
            Product product = productList.get(random.nextInt(productList.size()));
            String name = product.getName();
            Duration duration = Duration.ofMinutes(jobDurationMinutesMinimum
                    + random.nextInt(jobDurationMinutesMaximum - jobDurationMinutesMinimum));
            int targetDayIndex = (i / lineCount) * averageCleaningAndJobDurationMinutes / (24 * 60);
            LocalDateTime readyDateTime = START_DATE.plusDays(random.nextInt(Math.max(1, targetDayIndex - 2))).atTime(LocalTime.MIDNIGHT);
            LocalDateTime idealEndDateTime = START_DATE.plusDays(targetDayIndex + random.nextInt(3)).atTime(16, 0);
            LocalDateTime dueDateTime = idealEndDateTime.plusDays(1 + random.nextInt(3));
            jobList.add(new Job((long) i, name, product, duration, readyDateTime, idealEndDateTime, dueDateTime, 1, false));
        }
        jobList.sort(Comparator.comparing(Job::getName));
        solution.setJobList(jobList);

        repository.write(solution); //call che salva i dati creati sulla repository.
    }

    private static final List<String> PRODUCT_TYPE_LIST = List.of(
            "Articolo 1",
            "Articolo 2",
            "Articolo 3",
            "Articolo 4",
            "Articolo 5",
            "Articolo 6",
            "Articolo 7",
            "Articolo 8",
            "Articolo 9",
            "Articolo 10");
    private static final List<String> PRODUCT_VARIATION_LIST = List.of(
            "Mescola 1",
            "Mescola 2",
            "Mescola 3");

}
