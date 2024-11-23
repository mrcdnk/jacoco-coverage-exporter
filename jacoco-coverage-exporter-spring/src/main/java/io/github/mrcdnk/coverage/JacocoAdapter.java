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

import io.micrometer.core.annotation.Timed;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataStore;

import javax.management.MalformedObjectNameException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

public interface JacocoAdapter<T extends CoverageProvider> {
    void resetCoverage(T coverageProvider) throws IOException, MalformedObjectNameException;

    @Timed(description = "Time required to fetch the coverage for a single application", value = "jacoco.scrape.duration.seconds", histogram = true)
    IBundleCoverage fetchCoverage(T coverageProvider) throws IOException, MalformedObjectNameException;

    default IBundleCoverage analyze(final ExecutionDataStore data, Collection<File> clazzFiles) throws IOException {
        final CoverageBuilder builder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(data, builder);

        for (final File f : clazzFiles) {
            analyzer.analyzeAll(f);
        }

        return builder.getBundle("some-report");
    }
}
