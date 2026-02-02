package io.github.headlesshq.headlessmc.launcher.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import io.github.headlesshq.headlessmc.api.HasName;

@Data
@Builder
@AllArgsConstructor
public class LaunchAccount implements HasName {

    private final String type;
    private final String name;
    private final String id;
    private final String token;
    private final String xuid;

}
