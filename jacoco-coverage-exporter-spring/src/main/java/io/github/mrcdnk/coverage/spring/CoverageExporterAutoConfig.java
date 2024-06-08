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

import io.github.mrcdnk.coverage.JacocoMBeanProxy;
import io.github.mrcdnk.coverage.LocalJacocoAdapter;
import io.github.mrcdnk.coverage.LocalJacocoConfig;
import io.github.mrcdnk.coverage.prometheus.LocalPrometheusMetricProvider;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.access.MBeanProxyFactoryBean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Optional;

@Configuration
@ConditionalOnProperty(name="coverage.local", havingValue = "true")
public class CoverageExporterAutoConfig {

    @Value("${coverage.classesLocations}")
    private String classesLocations;

    @Value("${coverage.name}")
    private String name;

    private final MeterRegistry meterRegistry;

    public CoverageExporterAutoConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Bean
    public MBeanProxyFactoryBean mBeanProxyFactoryBean() throws MalformedObjectNameException {
        MBeanProxyFactoryBean factory =  new MBeanProxyFactoryBean();

        factory.setProxyInterface(JacocoMBeanProxy.class);
        factory.setObjectName(new ObjectName("org.jacoco:type=Runtime"));

        return factory;
    }

    @Bean
    public LocalJacocoAdapter localJacocoAdapter() throws MalformedObjectNameException {
        return new LocalJacocoAdapter(mBeanProxyFactoryBean());
    }

    @Bean
    public LocalJacocoConfig localJacocoConfig() {
        String[] sources = Optional.ofNullable(classesLocations).orElse("/app/classes/").split(";");

        return new LocalJacocoConfig(name, sources);
    }

    @Bean
    public LocalPrometheusMetricProvider localPrometheusMetricProvider(LocalJacocoConfig localJacocoConfig) throws MalformedObjectNameException {
        return new LocalPrometheusMetricProvider(localJacocoAdapter(), meterRegistry, localJacocoConfig);
    }


}
