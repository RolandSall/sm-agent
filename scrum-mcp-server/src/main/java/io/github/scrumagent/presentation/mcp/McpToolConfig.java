package io.github.scrumagent.presentation.mcp;

import io.github.scrumagent.shared.ReadTool;
import io.github.scrumagent.shared.WriteTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * The single MCP registration seam. Every {@code *Tools} bean implements {@link ReadTool} or
 * {@link WriteTool}; both collections are injected and registered here — no hard-coded bean list,
 * so a new capability's tools are picked up just by implementing the marker.
 *
 * <p>Governance: {@link WriteTool} beans carry
 * {@code @ConditionalOnProperty("scrum.governance.read-only"=false)}, so in the default read-only
 * profile the {@code writeTools} list is empty and no write/commit/notify tool is ever exposed
 * over MCP. (The command handlers enforce the same flag, covering REST/CLI callers too.)
 */
@Configuration
class McpToolConfig {

    @Bean
    ToolCallbackProvider scrumTools(List<ReadTool> readTools, List<WriteTool> writeTools) {
        List<Object> all = new ArrayList<>();
        all.addAll(readTools);
        all.addAll(writeTools);
        return MethodToolCallbackProvider.builder()
                .toolObjects(all.toArray())
                .build();
    }
}
