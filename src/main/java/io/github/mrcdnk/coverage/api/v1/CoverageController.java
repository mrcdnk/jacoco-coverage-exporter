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

package io.github.mrcdnk.coverage.api.v1;

import io.github.mrcdnk.coverage.configuration.ApplicationList;
import io.github.mrcdnk.coverage.configuration.RemoteCollectionConfiguration;
import io.github.mrcdnk.coverage.jmx.JmxCoverageProvider;
import io.github.mrcdnk.coverage.jmx.JmxJacocoAdapter;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICoverageNode;
import org.jacoco.core.analysis.IPackageCoverage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@RestController
@RequestMapping(path = "/v1")
public class CoverageController {

	private final RemoteCollectionConfiguration remoteCollectionConfiguration;
	private final JmxJacocoAdapter jmxJacocoAdapter;

	public CoverageController(RemoteCollectionConfiguration remoteCollectionConfiguration, JmxJacocoAdapter jmxJacocoAdapter) {
		this.remoteCollectionConfiguration = remoteCollectionConfiguration;
		this.jmxJacocoAdapter = jmxJacocoAdapter;
	}

	@PostMapping(path = "/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
	public void reset(ApplicationList applicationList) throws IOException, MalformedObjectNameException {

		validateProviders();

		final List<JmxCoverageProvider> relevantProviders;

		if (applicationList != null && applicationList.applications().length > 0) {
			relevantProviders = Stream.of(remoteCollectionConfiguration.providers())
					.filter(provider -> Arrays.stream(applicationList.applications())
							.anyMatch(s -> Objects.equals(provider.name(), s)))
					.toList();
		} else {
			relevantProviders = List.of(remoteCollectionConfiguration.providers());
		}

		for (var provider : relevantProviders) {
			jmxJacocoAdapter.resetCoverage(provider);
		}
	}

	@GetMapping("/coverage")
	public String coverage() throws MalformedObjectNameException, IOException {

		StringBuilder builder = new StringBuilder();

		validateProviders();

		for (var provider : remoteCollectionConfiguration.providers()) {
			IBundleCoverage bundleCoverage = jmxJacocoAdapter.fetchCoverage(provider);

			StringBuilder builder2 = new StringBuilder("<h1>" + provider.name() + " " + calculateInstructionCoverage(bundleCoverage) + "%</h2><br>");

			for (IPackageCoverage packageCoverage : bundleCoverage.getPackages()) {
				builder2.append(packageCoverage.getName())
						.append(": ")
						.append(packageCoverage.getCounter(ICoverageNode.CounterEntity.INSTRUCTION).getCoveredRatio())
						.append(", ")
						.append(packageCoverage.getCounter(ICoverageNode.CounterEntity.BRANCH).getCoveredRatio())
						.append("<br>");
			}

			builder.append(builder2);
		}

		return builder.toString();
	}

	private void validateProviders() {
		if (remoteCollectionConfiguration.providers().length == 0) {
			throw new MissingProvidersException("No Providers have been configured, please add providers to collect the coverage from!");
		}
	}

	private static double calculateInstructionCoverage(IBundleCoverage bundle) {
		double coverageSum = 0;

		for (IPackageCoverage packageCoverage : bundle.getPackages()) {
			coverageSum += packageCoverage.getInstructionCounter().getCoveredRatio();
		}

		return coverageSum/bundle.getPackages().size();
	}

}
