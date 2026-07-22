package io.github.scrumagent.acceptance;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * JUnit Platform Suite entry point that runs every {@code .feature} under {@code classpath:features}
 * with the Cucumber engine, using the glue (step definitions + hooks + Spring config) in
 * {@code io.github.scrumagent.acceptance}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "io.github.scrumagent.acceptance")
public class RunCucumberTest {
}
