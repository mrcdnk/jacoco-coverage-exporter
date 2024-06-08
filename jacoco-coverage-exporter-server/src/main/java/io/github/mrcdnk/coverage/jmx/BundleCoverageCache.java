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

package io.github.mrcdnk.coverage.jmx;

import org.jacoco.core.analysis.IBundleCoverage;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@RequestScope
@Service
public class BundleCoverageCache {
    private final Map<String, IBundleCoverage> bundleCoverageMap = new HashMap<>();

    public IBundleCoverage getOrCompute(String application, Supplier<IBundleCoverage> bundleCoverageSupplier) throws IOException, MalformedObjectNameException {
        return bundleCoverageMap.computeIfAbsent(application, s -> bundleCoverageSupplier.get());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BundleCoverageCache) obj;
        return Objects.equals(this.bundleCoverageMap, that.bundleCoverageMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bundleCoverageMap);
    }

    @Override
    public String toString() {
        return "BundleCoverageCache[" +
                "bundleCoverageMap=" + bundleCoverageMap + ']';
    }
}
