package org.acme.foodpackaging.persistence;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;

import org.acme.foodpackaging.domain.ProductionSchedule;

@ApplicationScoped
public class ProductionScheduleRepository {

    private final AtomicReference<ProductionSchedule> solutionReference = new AtomicReference<>();

    public ProductionSchedule read() {
        return solutionReference.get();
    }

    public void write(ProductionSchedule schedule) {
        solutionReference.set(schedule);
    }

}
