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

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.springframework.jmx.access.MBeanProxyFactoryBean;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class LocalJacocoAdapter implements JacocoAdapter<LocalJacocoConfig> {

    private final MBeanProxyFactoryBean proxyFactoryBean;

    public LocalJacocoAdapter(MBeanProxyFactoryBean proxyFactoryBean) {
        this.proxyFactoryBean = proxyFactoryBean;
    }

    @Override
    public void resetCoverage(LocalJacocoConfig coverageProvider) {
        if (proxyFactoryBean.getObject() instanceof JacocoMBeanProxy proxy) {
            proxy.reset();
        } else {
            throw new IllegalStateException("Proxy has unexpected type!");
        }
    }

    @Override
    public IBundleCoverage fetchCoverage(LocalJacocoConfig coverageProvider) {
        if (proxyFactoryBean.getObject() instanceof JacocoMBeanProxy proxy) {
            final Collection<File> classFiles = new ArrayList<>();

            for (String sourcesLocation : coverageProvider.classesLocations()) {
                File clazzDir = new File(sourcesLocation);
                classFiles.add(clazzDir);
            }

            final byte[] data = proxy.getExecutionData(false);

            ExecFileLoader loader  = new ExecFileLoader();
            try {
                loader.load(new ByteArrayInputStream(data));

                return analyze(loader.getExecutionDataStore(), classFiles);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load class files: ", e);
            }
        } else {
            throw new IllegalStateException("Proxy has unexpected type!");
        }
    }
}
