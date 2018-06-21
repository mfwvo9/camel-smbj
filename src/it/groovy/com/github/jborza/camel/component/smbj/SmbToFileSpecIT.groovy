/**
 *  Copyright [2018] [Juraj Borza]
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.github.jborza.camel.component.smbj

import org.apache.camel.builder.RouteBuilder
import org.apache.camel.main.Main
import org.apache.commons.io.FileUtils

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class SmbToFileSpecIT extends SmbSpecBase {
    final CONTENT = "Hello, SmbToFile content!"

    def setup() {
        //clear samba target directory
        File directory = new File(getTempDir())
        FileUtils.cleanDirectory(directory)
        //clear up destination
        File targetDirectory = new File("from-smb")
        FileUtils.forceMkdir(targetDirectory)
        FileUtils.cleanDirectory(targetDirectory)
    }

    def setupDirectoryWithFile() {
        File subDir = new File(Paths.get(getTempDir(), "dir").toString())
        subDir.mkdir()
        File srcFile = new File(Paths.get(getTempDir(), "dir", "test.txt").toString())
        FileUtils.writeStringToFile(srcFile, CONTENT, StandardCharsets.UTF_8)
    }

    def "one file from smb directory to file"() {
        given:
        setupDirectoryWithFile()
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/dir/?username=user&password=pass")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()

        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get("from-smb", "test.txt").toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content == CONTENT
    }

    def "one file from smb root to file"() {
        given:
        File srcFile = new File(Paths.get(getTempDir(), "test.txt").toString())
        FileUtils.writeStringToFile(srcFile, CONTENT, StandardCharsets.UTF_8)
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/?username=user&password=pass")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()

        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get("from-smb", "test.txt").toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content == CONTENT
    }

    def "one file from smb root with dfs=true to file"() {
        given:
        File srcFile = new File(Paths.get(getTempDir(), "test.txt").toString())
        FileUtils.writeStringToFile(srcFile, CONTENT, StandardCharsets.UTF_8)
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/?username=user&password=pass&dfs=true")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()

        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get("from-smb", "test.txt").toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content == CONTENT
    }

    def "more files from smb directory to file"() {
        given:
        for (def i = 0; i < 10; i++) {
            File srcFile = new File(Paths.get(getTempDir(), "dir", "test" + i + ".txt").toString())
            FileUtils.writeStringToFile(srcFile, "data" + i, StandardCharsets.UTF_8)
        }
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/dir/?username=user&password=pass")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()

        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File targetDirectory = new File("from-smb")
        targetDirectory.list().size() == 10
        for (def i = 0; i < 10; i++) {
            File target = new File(Paths.get("from-smb", "test" + i + ".txt").toString())
            target.exists()
            String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
            content == "data" + i
        }
    }

    def "one file with filter from smb directory to file"() {
        given:
        setupDirectoryWithFile()
        File anotherFile = new File(Paths.get(getTempDir(), "dir", "dont_match_me.ext").toString())
        FileUtils.touch(anotherFile)
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/dir/?username=user&password=pass&fileName=test.txt")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()

        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get("from-smb", "test.txt").toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content == CONTENT
        !new File(Paths.get("from-smb", "dont_match_me.ext").toString()).exists()
    }

    def "file from directory with localWorkDirectory"() {
        given:
        setupDirectoryWithFile()
        when:
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("smb2://localhost:4445/share/dir/?username=user&password=pass&localWorkDirectory=/tmp")
                        .to("file://from-smb")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get("from-smb", "test.txt").toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content == CONTENT
    }
}