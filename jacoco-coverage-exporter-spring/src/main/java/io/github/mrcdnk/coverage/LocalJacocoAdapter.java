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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.springframework.jmx.access.MBeanProxyFactoryBean;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

public class LocalJacocoAdapter implements JacocoAdapter<LocalJacocoConfig> {


    private final Collection<File> clazzFiles = new ArrayList<>();

    private final MBeanProxyFactoryBean proxyFactoryBean;
    private final Log log = LogFactory.getLog(getClass());

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
            final byte[] data = proxy.getExecutionData(false);

            ExecFileLoader loader  = new ExecFileLoader();
            try {
                loader.load(new ByteArrayInputStream(data));

                final Collection<File> classFiles = getClazzFiles(coverageProvider);

                return analyze(loader.getExecutionDataStore(), classFiles);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load class files: ", e);
            }
        } else {
            throw new IllegalStateException("Proxy has unexpected type!");
        }
    }

    private Collection<File> getClazzFiles(LocalJacocoConfig coverageProvider) throws IOException {
        if (coverageProvider.enableClassesCache() && !clazzFiles.isEmpty()) {
            return clazzFiles;
        }

        final Collection<File> classFiles = new ArrayList<>();

        for (String sourcesLocation : coverageProvider.classesLocations()) {
            File clazzDir = new File(sourcesLocation);

            if (log.isDebugEnabled()) {
                log.debug("Checking class file source [" + sourcesLocation + "]");
            }

            FileVisitor<Path> matcherVisitor = new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes fileAttributes) {
                    FileSystem fs = FileSystems.getDefault();

                    boolean toBeIncluded = false;

                    if (log.isDebugEnabled()) {
                        log.debug("Checking [" + file.toAbsolutePath() + "]");
                    }

                    if (coverageProvider.includePatterns().length != 0) {
                        for (String pattern : coverageProvider.includePatterns()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Trying inclusion pattern [" + pattern + "]");
                            }

                            if (fs.getPathMatcher(pattern).matches(file.toAbsolutePath())) {
                                toBeIncluded = true;
                                break;
                            }
                        }
                    } else {
                        toBeIncluded = true;
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("File [" + file.toAbsolutePath() + "] included: [" + toBeIncluded + "]");
                    }

                    for (String pattern : coverageProvider.excludePatterns()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Trying exclusion pattern [" + pattern + "]");
                        }

                        if (fs.getPathMatcher(pattern).matches(file.toAbsolutePath())) {
                            if (log.isDebugEnabled()) {
                                log.debug("[" + file.toAbsolutePath() + "] excluded by pattern [" + pattern + "]");
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    }

                    if (toBeIncluded) {
                        if (log.isDebugEnabled()) {
                            log.debug("File [" + file.toAbsolutePath() + "] added to coverage scan.");
                        }

                        classFiles.add(file.toFile());
                    }

                    return FileVisitResult.CONTINUE;
                }
            };

            Files.walkFileTree(clazzDir.toPath(), matcherVisitor);
        }

        if (coverageProvider.enableClassesCache()) {
            clazzFiles.clear();
            clazzFiles.addAll(classFiles);

            if (log.isDebugEnabled()) {
                log.debug("Cache is filled! " + clazzFiles.size());
            }
        }

        return classFiles;
    }
}
