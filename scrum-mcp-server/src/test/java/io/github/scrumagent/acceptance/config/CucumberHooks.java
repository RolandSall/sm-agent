package io.github.scrumagent.acceptance.config;

import io.cucumber.java.Before;
import io.github.scrumagent.acceptance.MockServerSupport;

/** Resets MockServer expectations and request log before every scenario so scenarios stay independent. */
public class CucumberHooks {

    @Before
    public void resetMockServer() {
        MockServerSupport.server().reset();
    }
}
