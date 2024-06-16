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
import io.github.mrcdnk.coverage.LocalJacocoAdapter;
import io.github.mrcdnk.coverage.LocalJacocoConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ICoverageNode;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class LocalPrometheusMetricProvider {

    private final MeterRegistry meterRegistry;
    private final LocalJacocoAdapter jmxJacocoAdapter;
    private final Map<String, String> addedTags;

    public LocalPrometheusMetricProvider(
            LocalJacocoAdapter jmxJacocoAdapter,
            MeterRegistry meterRegistry,
            LocalJacocoConfig localJacocoConfig,
            Map<String, String> addedTags) {
        this.jmxJacocoAdapter = jmxJacocoAdapter;
        this.meterRegistry = meterRegistry;
        this.addedTags = addedTags;

        for (ICoverageNode.CounterEntity counterEntity : ICoverageNode.CounterEntity.values()) {
            createGaugeForCounterEntity(localJacocoConfig, counterEntity);
        }
    }

    private  void createGaugeForCounterEntity(LocalJacocoConfig provider, ICoverageNode.CounterEntity counterEntity) {
        String metricName = mapMetricName(counterEntity);

        String[] constantTags = addedTags.entrySet()
                .stream()
                .flatMap(entry -> Stream.of(entry.getKey(), entry.getValue()))
                .toArray(String[]::new);;

        for (GaugeFactory.Type type : GaugeFactory.Type.values()) {
            GaugeFactory
                    .create(metricName, type, () -> getCoverageCounter(counterEntity, type.getCountGetter(), provider, jmxJacocoAdapter), constantTags)
                    .register(meterRegistry);

        }
    }

    protected static String mapMetricName(ICoverageNode.CounterEntity counterEntity) {
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
