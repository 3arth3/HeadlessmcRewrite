package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.auth.LaunchAccount;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class OfflineCommand extends AbstractLauncherCommand {

    public OfflineCommand(Launcher ctx) {
        super(ctx, "offline", "Logs in with an offline account from config.");
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        // 1. Get the ID (e.g., "1" from "offline 1", or empty string if just "offline")
        String id = args.length > 0 ? args[0] : "";
        
        // 2. Build the config key (e.g., hmc.offline.account1)
        String configKey = LauncherProperties.OFFLINE_ACCOUNT_PREFIX.getName() + id;
        
        // 3. Retrieve the username from config.properties
        String username = ctx.getConfig().get(configKey, "");

        if (username == null || username.isEmpty()) {
            throw new CommandException("Could not find an offline account for key: " + configKey + " in config.properties");
        }

        // 4. Generate a deterministic Offline UUID based on the username
        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();
        
        // 5. Create a new LaunchAccount with a dummy token "offline" to bypass Premium checks
        LaunchAccount offlineAccount = new LaunchAccount(username, uuid, "offline");
        
        // 6. Set the current account (this overrides any existing Premium/Microsoft account)
        ctx.getAccountManager().setCurrentAccount(offlineAccount);

        // 7. Success message in English as requested
        ctx.log("Successfully logged in with offline account: " + username);
    }
}
