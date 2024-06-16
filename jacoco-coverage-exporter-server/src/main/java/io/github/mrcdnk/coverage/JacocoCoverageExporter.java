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

import io.github.mrcdnk.coverage.configuration.RemoteCollectionConfiguration;
import io.github.mrcdnk.coverage.prometheus.PrometheusConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("io.github.mrcdnk.coverage")
@EnableConfigurationProperties({RemoteCollectionConfiguration.class, PrometheusConfiguration.class})
public class JacocoCoverageExporter {

	public static void main(String[] args) {
		SpringApplication.run(JacocoCoverageExporter.class, args);
	}

}
