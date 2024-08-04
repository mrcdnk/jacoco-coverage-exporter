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

import io.github.mrcdnk.coverage.LocalJacocoAdapter;
import io.github.mrcdnk.coverage.LocalJacocoConfig;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;

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

}
