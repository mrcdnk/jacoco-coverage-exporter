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

import io.github.mrcdnk.coverage.configuration.RemoteCollectionConfiguration;
import io.github.mrcdnk.coverage.jmx.JmxCoverageProvider;
import io.github.mrcdnk.coverage.jmx.JmxJacocoAdapter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ICoverageNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name="coverage.local",havingValue = "false", matchIfMissing = true)
public class PrometheusMetricProvider {

    public static final String PROMETHEUS_APPLICATION_TAG = "application";
    public static final String PROMETHEUS_METRIC_PREFIX = "jacoco_";
    private final MeterRegistry meterRegistry;
    private final JmxJacocoAdapter jmxJacocoAdapter;

    private final PrometheusConfiguration prometheusConfiguration;

    public PrometheusMetricProvider(
            RemoteCollectionConfiguration remoteCollectionConfiguration,
            JmxJacocoAdapter jmxJacocoAdapter,
            MeterRegistry meterRegistry,
            PrometheusConfiguration prometheusConfiguration) {
        this.jmxJacocoAdapter = jmxJacocoAdapter;
        this.meterRegistry = meterRegistry;
        this.prometheusConfiguration = prometheusConfiguration;

        for (var provider : remoteCollectionConfiguration.providers()) {
            String providerName = provider.name();

            for (ICoverageNode.CounterEntity counterEntity : ICoverageNode.CounterEntity.values()) {
                createGaugeForCounterEntity(providerName, provider, counterEntity);
            }
        }
    }

    private  void createGaugeForCounterEntity(String providerName, JmxCoverageProvider provider, ICoverageNode.CounterEntity counterEntity) {

        String[] constantTags = prometheusConfiguration.labels()
                .entrySet()
                .stream()
                .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue()))
                .toArray(String[]::new);

        String metricName = mapMetricName(counterEntity);

        Gauge
                .builder(PROMETHEUS_METRIC_PREFIX + metricName + "_covered", () -> getCoverageCounter(counterEntity, ICounter::getCoveredCount, provider, jmxJacocoAdapter))
                .description("Number of currently covered " + metricName)
                .tag(PROMETHEUS_APPLICATION_TAG, providerName)
                .tags(constantTags)
                .register(meterRegistry);

        Gauge
                .builder(PROMETHEUS_METRIC_PREFIX + metricName + "_missed", () -> getCoverageCounter(counterEntity, ICounter::getMissedCount, provider, jmxJacocoAdapter))
                .description("Number of currently missed " + metricName)
                .tag(PROMETHEUS_APPLICATION_TAG, providerName)
                .tags(constantTags)
                .register(meterRegistry);
        Gauge
                .builder(PROMETHEUS_METRIC_PREFIX + metricName + "_total", () -> getCoverageCounter(counterEntity, ICounter::getTotalCount, provider, jmxJacocoAdapter))
                .description("Total amount of " + metricName + " that can be covered")
                .tag(PROMETHEUS_APPLICATION_TAG, providerName)
                .tags(constantTags)
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


    private static int getCoverageCounter(ICoverageNode.CounterEntity counter, Function<ICounter, Integer> getValue, JmxCoverageProvider provider, JmxJacocoAdapter jmxJacocoAdapter) {
        IBundleCoverage bundleCoverage;

        try {
            bundleCoverage = jmxJacocoAdapter.fetchCoverage(provider);
        } catch (IOException | MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }

        return getValue.apply(bundleCoverage.getCounter(counter));
    }
}
