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

package io.github.mrcdnk.coverage;

import io.micrometer.core.instrument.Gauge;
import org.jacoco.core.analysis.ICounter;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

public class GaugeFactory {

    public static final String PROMETHEUS_APPLICATION_TAG = "application";
    public static final String PROMETHEUS_METRIC_PREFIX = "jacoco.";

    private  GaugeFactory() {}

    public static Gauge.Builder<?> create(String metricName, GaugeFactory.Type gaugeType, Supplier<Integer> coverageCounter, String[] tags) {
        return Gauge
                .builder(PROMETHEUS_METRIC_PREFIX + metricName + "." + gaugeType.getSuffix(), coverageCounter::get)
                .description("Number of currently " + gaugeType.getSuffix() + " " + metricName)
                .tags(tags);
    }

    public enum Type {
        COVERED(ICounter::getCoveredCount),
        MISSED(ICounter::getMissedCount),
        TOTAL(ICounter::getTotalCount),
        ;

        private final Function<ICounter, Integer> countGetter;

        Type(Function<ICounter, Integer> countGetter) {
            this.countGetter = countGetter;
        }

        public Function<ICounter, Integer> getCountGetter() {
            return countGetter;
        }

        String getSuffix() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

}
