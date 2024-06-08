/*
 *    Copyright 2024 Marco Deneke
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.mrcdnk.coverage.prometheus;

import io.github.mrcdnk.coverage.LocalJacocoAdapter;
import io.github.mrcdnk.coverage.LocalJacocoConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ICoverageNode;

import java.util.function.Function;

public class LocalPrometheusMetricProvider {

    public static final String PROMETHEUS_APPLICATION_TAG = "application";
    public static final String PROMETHEUS_METRIC_PREFIX = "jacoco_";
    private final MeterRegistry meterRegistry;
    private final LocalJacocoAdapter jmxJacocoAdapter;

    public LocalPrometheusMetricProvider(
            LocalJacocoAdapter jmxJacocoAdapter,
            MeterRegistry meterRegistry,
            LocalJacocoConfig localJacocoConfig) {
        this.jmxJacocoAdapter = jmxJacocoAdapter;
        this.meterRegistry = meterRegistry;

        String providerName = localJacocoConfig.name();

        for (ICoverageNode.CounterEntity counterEntity : ICoverageNode.CounterEntity.values()) {
            createGaugeForCounterEntity(providerName, localJacocoConfig, counterEntity);
        }
    }

    private  void createGaugeForCounterEntity(String providerName, LocalJacocoConfig provider, ICoverageNode.CounterEntity counterEntity) {

        String metricName = mapMetricName(counterEntity);

        Gauge
                .builder(PROMETHEUS_METRIC_PREFIX + metricName + "_covered", () -> getCoverageCounter(counterEntity, ICounter::getCoveredCount, provider, jmxJacocoAdapter))
                .description("Number of currently covered " + metricName)
                .tag(PROMETHEUS_APPLICATION_TAG, providerName)
                .register(meterRegistry);

        Gauge
                .builder(PROMETHEUS_METRIC_PREFIX + metricName + "_missed", () -> getCoverageCounter(counterEntity, ICounter::getMissedCount, provider, jmxJacocoAdapter))
                .description("Number of currently missed " + metricName)
                .tag(PROMETHEUS_APPLICATION_TAG, providerName)
                .register(meterRegistry);
        Gauge
                .builder(PROMETHEUS_METRIC_PREFIX + metricName + "_total", () -> getCoverageCounter(counterEntity, ICounter::getTotalCount, provider, jmxJacocoAdapter))
                .description("Total amount of " + metricName + " that can be covered")
                .tag(PROMETHEUS_APPLICATION_TAG, providerName)
                .register(meterRegistry);
    }

    private String mapMetricName(ICoverageNode.CounterEntity counterEntity) {
        return switch (counterEntity) {
            case BRANCH -> "branches";
            case INSTRUCTION -> "instructions";
            case METHOD -> "methods";
            case CLASS -> "classes";
            case LINE -> "lines";
            case COMPLEXITY -> "complexity";
        };
    }


    private static int getCoverageCounter(ICoverageNode.CounterEntity counter, Function<ICounter, Integer> getValue, LocalJacocoConfig provider, LocalJacocoAdapter jmxJacocoAdapter) {
        IBundleCoverage bundleCoverage;

        bundleCoverage = jmxJacocoAdapter.fetchCoverage(provider);

        return getValue.apply(bundleCoverage.getCounter(counter));
    }
}
