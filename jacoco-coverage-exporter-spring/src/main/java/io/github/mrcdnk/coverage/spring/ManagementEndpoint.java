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

package io.github.mrcdnk.coverage.spring;

import io.github.mrcdnk.coverage.GaugeFactory;
import io.github.mrcdnk.coverage.LocalJacocoAdapter;
import io.github.mrcdnk.coverage.LocalJacocoConfig;
import io.github.mrcdnk.coverage.prometheus.LocalPrometheusMetricProvider;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICoverageNode;
import org.jacoco.core.analysis.IPackageCoverage;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;

import java.util.HashMap;
import java.util.Map;

@WebEndpoint(id = "jacoco")
public class ManagementEndpoint {

    private final LocalJacocoAdapter jmxJacocoAdapter;
    private final LocalJacocoConfig localJacocoConfig;

    public ManagementEndpoint(LocalJacocoAdapter jmxJacocoAdapter,
                              LocalJacocoConfig localJacocoConfig) {
        this.jmxJacocoAdapter = jmxJacocoAdapter;
        this.localJacocoConfig = localJacocoConfig;
    }


    @WriteOperation
    public void reset(boolean reset) {
        if (reset) {
            jmxJacocoAdapter.resetCoverage(localJacocoConfig);
        }
    }

    @ReadOperation
    public Map<String, Map<String, Map<String, String>>> readCoverage() {
        IBundleCoverage bundleCoverage = jmxJacocoAdapter.fetchCoverage(localJacocoConfig);

        Map<String, Map<String, Map<String, String>>> coverageStats = new HashMap<>();

        for (IPackageCoverage pkgStats : bundleCoverage.getPackages()) {
           Map<String, Map<String, String>> stats = coverageStats.computeIfAbsent(pkgStats.getName(), key -> new HashMap<>());


            for (ICoverageNode.CounterEntity counterEntity : ICoverageNode.CounterEntity.values()) {
                Map<String, String> counter = stats.computeIfAbsent(LocalPrometheusMetricProvider.mapMetricName(counterEntity), key -> new HashMap<>());

                for (GaugeFactory.Type type : GaugeFactory.Type.values()) {
                    counter.put(type.getSuffix(), LocalPrometheusMetricProvider.getCoverageCounter(counterEntity, type.getCountGetter(), localJacocoConfig, jmxJacocoAdapter)+"");
                }
            }

        }

        return coverageStats;
    }

}
