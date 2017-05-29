/*
 * Copyright 2017. assertj-generator-gradle-plugin contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.assertj.generator.gradle.parameter

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Path
import java.nio.file.Paths

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat

/**
 * Checks the behaviour of overriding globals in a project
 */
class ConfigureGeneration {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile
    private Path srcPackagePath
    private Path packagePath

    @Before
    void setup() {
        buildFile = testProjectDir.newFile('build.gradle')

        File srcDir = testProjectDir.newFolder('src', 'main', 'java')

        packagePath = Paths.get("org/example/")

        srcPackagePath = srcDir.toPath().resolve(packagePath)
        srcPackagePath.toFile().mkdirs()
        File helloWorldJava = srcPackagePath.resolve('HelloWorld.java').toFile()

        helloWorldJava << """
            |package org.example;
            |
            |public final class HelloWorld {
            |    // Field
            |    public boolean hasSomeBrains = false;
            |}""".stripMargin()
    }


    @Test
    void change_generate_from_sourceSet() {


        buildFile << """
            // Add required plugins and source sets to the sub projects
            plugins { id "net.navatwo.assertj.generator.gradle.plugin" } // Note must use this syntax
            
            sourceSets {
                main {
                    assertJ {
                        entryPoints {
                            standard = true
                            junitSoft = true
                            soft = true
                        }     
                    }
                }
            }
            
            // add some classpath dependencies
            repositories {
                mavenCentral()
            }
                        
            dependencies {
                // https://mvnrepository.com/artifact/com.google.guava/guava
                compile group: 'com.google.guava', name: 'guava', version: '20.0'
                
                // https://mvnrepository.com/artifact/org.assertj/assertj-core
                testCompile group: 'org.assertj', name: 'assertj-core', version: '3.8.0'
                
                testCompile group: 'junit', name: 'junit', version: '4.12'
            }
        """.stripMargin()

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withDebug(true)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'test')
                .build()

        result.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        result.task(':test').outcome == TaskOutcome.SUCCESS

        Path generatedPackage = testProjectDir.root.toPath()
                .resolve("build/generated-src/test/java")
                .resolve(packagePath)
        def files = ["HelloWorldAssert.java",
                     "Assertions.java",             // Standard
                     //"BddAssertions.java",          // BDD
                     "JUnitSoftAssertions.java",    // JUNIT_SOFT
                     "SoftAssertions.java"]         // SOFT

        files.each {
            assertThat(generatedPackage.resolve(it)).exists()
        }

    }

    @Test
    void change_generate_from_global() {

        buildFile << """
            plugins { id "net.navatwo.assertj.generator.gradle.plugin" } 

            assertJ {
                entryPoints = ['bdd']
            }
            
            sourceSets {
                main {
                    assertJ { }
                }
            }
            
            // add some classpath dependencies
            repositories {
                mavenCentral()
            }
                        
            dependencies {
                // https://mvnrepository.com/artifact/com.google.guava/guava
                compile group: 'com.google.guava', name: 'guava', version: '20.0'
                
                // https://mvnrepository.com/artifact/org.assertj/assertj-core
                testCompile group: 'org.assertj', name: 'assertj-core', version: '3.8.0'
                
                testCompile group: 'junit', name: 'junit', version: '4.12'
            }
        """.stripMargin()

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withDebug(true)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'test')
                .build()

        result.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        result.task(':test').outcome == TaskOutcome.SUCCESS

        Path generatedPackage = testProjectDir.root.toPath()
                .resolve("build/generated-src/test/java")
                .resolve(packagePath)
        def files = ["HelloWorldAssert.java",
                     //"Assertions.java",             // Standard
                     "BddAssertions.java",        // BDD
                     //"JUnitSoftAssertions.java",    // JUNIT_SOFT
                     //"SoftAssertions.java"         // SOFT
                    ]

        files.each {
            assertThat(generatedPackage.resolve(it)).exists()
        }

    }

}