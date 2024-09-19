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

import io.github.mrcdnk.coverage.JacocoAdapter;
import io.github.mrcdnk.coverage.JacocoMBeanProxy;
import io.micrometer.core.annotation.Timed;
import org.apache.commons.io.FileUtils;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@Service
@Primary
public class JmxJacocoAdapter implements JacocoAdapter<JmxCoverageProvider> {

    private final BundleCoverageCache bundleCoverageCache;

    public JmxJacocoAdapter(BundleCoverageCache bundleCoverageCache) {
        this.bundleCoverageCache = bundleCoverageCache;
    }


    @Override
    public void resetCoverage(JmxCoverageProvider jmxCoverageProvider) throws IOException, MalformedObjectNameException {
        try (JMXConnector jmxc = getJmxConnector(jmxCoverageProvider.host(), jmxCoverageProvider.port())) {
            final MBeanServerConnection connection = jmxc.getMBeanServerConnection();
            final JacocoMBeanProxy proxy = getJacocoProxy(connection);
            proxy.reset();
        }
    }

    @Override
    @Timed(description = "Time required to compute the coverage for a single provider" ,value = "jacoco_scrape_duration_seconds", histogram = true)
    public IBundleCoverage fetchCoverage(JmxCoverageProvider jmxCoverageProvider) throws IOException, MalformedObjectNameException {
        return bundleCoverageCache.getOrCompute(jmxCoverageProvider.name(), () ->  {

            final Collection<File> classFiles = new ArrayList<>();

            for (String sourcesLocation : jmxCoverageProvider.classesLocations()) {
                File clazzDir = new File(sourcesLocation);

                classFiles.addAll(FileUtils.listFiles(clazzDir, null, true));
            }

            try (JMXConnector jmxConnector = getJmxConnector(jmxCoverageProvider.host(), jmxCoverageProvider.port())) {
                final MBeanServerConnection connection = jmxConnector.getMBeanServerConnection();

                final JacocoMBeanProxy proxy = getJacocoProxy(connection);

                final byte[] data = proxy.getExecutionData(false);

                ExecFileLoader loader  = new ExecFileLoader();
                loader.load(new ByteArrayInputStream(data));

                return analyze(loader.getExecutionDataStore(), classFiles);
            } catch (IOException | MalformedObjectNameException e) {
                throw new RuntimeException(e);
            }
        });
    }



    private static JacocoMBeanProxy getJacocoProxy(MBeanServerConnection connection) throws MalformedObjectNameException {
        return MBeanServerInvocationHandler
                .newProxyInstance(connection,
                        new ObjectName("org.jacoco:type=Runtime"), JacocoMBeanProxy.class,
                        false);
    }

    private static JMXConnector getJmxConnector(String service, int port) throws IOException {
        final JMXServiceURL url = new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", service, port));

        return JMXConnectorFactory.connect(url, null);
    }
}
