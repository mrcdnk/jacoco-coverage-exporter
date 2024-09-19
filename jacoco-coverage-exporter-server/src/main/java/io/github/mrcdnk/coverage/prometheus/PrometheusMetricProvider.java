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

import io.github.mrcdnk.coverage.GaugeFactory;
import io.github.mrcdnk.coverage.configuration.RemoteCollectionConfiguration;
import io.github.mrcdnk.coverage.jmx.JmxCoverageProvider;
import io.github.mrcdnk.coverage.jmx.JmxJacocoAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ICoverageNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name="coverage.local", havingValue = "false", matchIfMissing = true)
public class PrometheusMetricProvider {

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

        Map<String, String> tagMap = new HashMap<>(prometheusConfiguration.labels());
        tagMap.put(GaugeFactory.PROMETHEUS_APPLICATION_TAG, providerName);

        String[] tags = tagMap
                .entrySet()
                .stream()
                .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue()))
                .toArray(String[]::new);

        String metricName = LocalPrometheusMetricProvider.mapMetricName(counterEntity);

        for (GaugeFactory.Type type : GaugeFactory.Type.values()) {
            GaugeFactory
                    .create(metricName, type, () -> getCoverageCounter(counterEntity, type.getCountGetter(), provider, jmxJacocoAdapter), tags)
                    .register(meterRegistry);

        }
    }


    public static int getCoverageCounter(ICoverageNode.CounterEntity counter, Function<ICounter, Integer> getValue, JmxCoverageProvider provider, JmxJacocoAdapter jmxJacocoAdapter) {
        IBundleCoverage bundleCoverage;

        try {
            bundleCoverage = jmxJacocoAdapter.fetchCoverage(provider);
        } catch (IOException | MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }

        return getValue.apply(bundleCoverage.getCounter(counter));
    }
}
