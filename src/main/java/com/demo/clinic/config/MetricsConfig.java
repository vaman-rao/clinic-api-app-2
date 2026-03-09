package com.demo.clinic.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Observability infrastructure — wires the AOP aspect for @Timed.
 *
 * This is the ONLY config needed here. Metric definitions live elsewhere:
 *
 *   ClinicMetricsListener  — business counters (event-driven)
 *   ClinicGaugesRegistrar  — live-state gauges (DB-queried per scrape)
 *
 * Adding @Timed("some.metric.name") to any service method automatically
 * creates Prometheus histograms: _seconds_bucket / _count / _sum.
 * No code changes needed in this file.
 */
@Configuration
@EnableAspectJAutoProxy
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
