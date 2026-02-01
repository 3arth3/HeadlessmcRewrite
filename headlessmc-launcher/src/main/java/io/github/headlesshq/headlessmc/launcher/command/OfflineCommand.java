package io.github.headlesshq.headlessmc.launcher.command;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherProperties;
import io.github.headlesshq.headlessmc.launcher.auth.LaunchAccount;
import io.github.headlesshq.headlessmc.api.config.Property;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;
import static io.github.headlesshq.headlessmc.api.config.PropertyTypes.string;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Command to log in using an offline account from config.properties.
 * Successfully bypasses Premium checks by placing the account at the primary index.
 */
public class OfflineCommand extends AbstractLauncherCommand {

    public OfflineCommand(Launcher ctx) {
        super(ctx, "offline", "Log in with an offline account from the configuration file.");
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        // 1. Get the suffix ID (e.g., "1" from "offline 1")
        String id = args.length > 0 ? args[0] : "";
        String keyName = LauncherProperties.OFFLINE_ACCOUNT_PREFIX.getName() + id;
        Property<String> dynamicProperty = string(keyName);
        
        // 2. Fetch username from config.properties
        String username = ctx.getConfig().get(dynamicProperty, "");

        if (username == null || username.isEmpty()) {
            throw new CommandException("No offline account found in config for key: " + keyName);
        }

        // 3. Generate a deterministic UUID for the offline player
        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();
        
        // 4. Create the offline account. 
        // Based on your AccountManager, we use "offline" for the token to trigger the JavaLaunchCommandBuilder logic.
        // Format: name, uuid, token, type, xuid
        LaunchAccount offlineAccount = new LaunchAccount(username, uuid, "offline", "offline", "0");
        
        // 5. Use the existing addAccount method.
        // Your AccountManager.addAccount(ValidatedAccount) will:
        // - Remove duplicates
        // - Add this account at index 0 (making it the Primary Account)
        // - Automatically call save() to update .accounts.json
        ctx.getAccountManager().addAccount(offlineAccount);

        ctx.log("Successfully logged in with offline account: " + username);
    }
}
