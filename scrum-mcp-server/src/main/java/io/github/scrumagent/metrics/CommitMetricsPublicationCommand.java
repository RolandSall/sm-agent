package io.github.scrumagent.metrics;

import io.github.springmediator.mediator.core.ICommand;

/** Write side of the human-in-the-loop publish: publish the previously prepared content. */
public class CommitMetricsPublicationCommand implements ICommand {

    private final String token;

    public CommitMetricsPublicationCommand(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
