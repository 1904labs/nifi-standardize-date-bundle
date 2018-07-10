/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nineteen04labs.processors.standardizedate;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;

public class StandardizeDateJsonTest {

    private final Path unprocessedFile = Paths.get("src/test/resources/unprocessed.json");
    private final TestRunner runner = TestRunners.newTestRunner(new StandardizeDate());

    @Test
    public void testNoProcessing() throws IOException {
        runner.setProperty(StandardizeDateProperties.FLOW_FORMAT, "JSON");

        runner.enqueue(unprocessedFile);

        runner.run();
        runner.assertQueueEmpty();
        runner.assertAllFlowFilesTransferred(StandardizeDateRelationships.REL_BYPASS, 1);

        final MockFlowFile outFile = runner.getFlowFilesForRelationship(StandardizeDateRelationships.REL_BYPASS).get(0);

        outFile.assertContentEquals(unprocessedFile);
    }

    @Test
    public void testStandardization() throws IOException {
        final Path processedFile = Paths.get("src/test/resources/processed.json");

        runner.setProperty(StandardizeDateProperties.FLOW_FORMAT, "JSON");
        runner.setProperty(StandardizeDateProperties.INVALID_DATES, "{\"bad_date\":\"MM/dd/yy\"}");
        runner.setProperty(StandardizeDateProperties.TIMEZONE, "America/Chicago");

        runner.enqueue(unprocessedFile);

        runner.run();
        runner.assertQueueEmpty();
        runner.assertAllFlowFilesTransferred(StandardizeDateRelationships.REL_SUCCESS, 1);

        final MockFlowFile outFile = runner.getFlowFilesForRelationship(StandardizeDateRelationships.REL_SUCCESS).get(0);

        outFile.assertContentEquals(processedFile);
    }

    @Test
    public void testShortZoneId() throws IOException {
        final Path processedFile = Paths.get("src/test/resources/processed.json");

        runner.setProperty(StandardizeDateProperties.FLOW_FORMAT, "JSON");
        runner.setProperty(StandardizeDateProperties.INVALID_DATES, "{\"bad_date\":\"MM/dd/yy\"}");
        runner.setProperty(StandardizeDateProperties.TIMEZONE, "CST");

        runner.enqueue(unprocessedFile);

        runner.run();
        runner.assertQueueEmpty();
        runner.assertAllFlowFilesTransferred(StandardizeDateRelationships.REL_SUCCESS, 1);

        final MockFlowFile outFile = runner.getFlowFilesForRelationship(StandardizeDateRelationships.REL_SUCCESS).get(0);

        outFile.assertContentEquals(processedFile);
    }
}
