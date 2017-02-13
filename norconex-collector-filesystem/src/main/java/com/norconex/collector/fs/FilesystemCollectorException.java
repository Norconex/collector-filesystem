/* Copyright 2013-2017 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.collector.fs;

import com.norconex.collector.core.CollectorException;

/**
 * Runtime exception for most unrecoverable issues thrown by Filesystem Collector
 * classes.
 * @author Pascal Dimassimo
 * @author Pascal Essiembre
 * @deprecated as of 2.7.0 use {@link CollectorException}
 */
@Deprecated
public class FilesystemCollectorException extends CollectorException {

    private static final long serialVersionUID = -805913995358009121L;

    public FilesystemCollectorException() {
        super();
    }

    public FilesystemCollectorException(String message) {
        super(message);
    }

    public FilesystemCollectorException(Throwable cause) {
        super(cause);
    }

    public FilesystemCollectorException(String message, Throwable cause) {
        super(message, cause);
    }

}
