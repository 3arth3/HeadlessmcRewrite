package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;
import io.github.headlesshq.headlessmc.api.config.Property;
import static io.github.headlesshq.headlessmc.api.config.PropertyTypes.string;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Command to log in using an offline account defined in config.properties.
 * Uses the custom ValidatedAccount constructor to bypass Microsoft session requirements.
 */
public class OfflineCommand extends AbstractLauncherCommand {

    public OfflineCommand(Launcher ctx) {
        super(ctx, "offline", "Log in with an offline account from the configuration file.");
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        // 1. Resolve the config key (e.g., hmc.offline.account + id)
        String id = args.length > 0 ? args[0] : "";
        String keyName = LauncherProperties.OFFLINE_ACCOUNT_PREFIX.getName() + id;
        Property<String> dynamicProperty = string(keyName);
        
        // 2. Retrieve the username from configuration
        String username = ctx.getConfig().get(dynamicProperty, "");

        if (username == null || username.isEmpty()) {
            throw new CommandException("No offline account found for key: " + keyName);
        }

        // 3. Generate a deterministic Version 3 UUID based on the username
        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();
        
        // 4. Create a ValidatedAccount using the Offline constructor
        // Parameters: name, uuid, xuid
        ValidatedAccount offlineAccount = new ValidatedAccount(username, uuid, "0");
        
        // 5. Add to AccountManager
        // This automatically sets it as primary and persists it to .accounts.json
        ctx.getAccountManager().addAccount(offlineAccount);

        ctx.log("Successfully logged in with offline account: " + username);
    }
}
