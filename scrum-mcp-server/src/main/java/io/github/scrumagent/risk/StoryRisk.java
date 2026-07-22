package io.github.scrumagent.risk;

/**
 * One active-sprint story's raw risk facts. The agent correlates it to the sprint's cross-team
 * dependencies (by {@code epicKey}) and reasons about the risk level — nothing is judged in code.
 *
 * @param key         the story key
 * @param summary     the story summary
 * @param epicKey     the parent Epic (join key to the dependencies), or {@code null}
 * @param storyPoints the estimate, or {@code null} if unestimated
 * @param unestimated convenience flag: {@code true} when {@code storyPoints} is {@code null}
 */
public record StoryRisk(String key, String summary, String epicKey, Double storyPoints, boolean unestimated) {
}
