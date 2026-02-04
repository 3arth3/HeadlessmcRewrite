package io.github.headlesshq.headlessmc.launcher.command.login;

import io.github.headlesshq.headlessmc.api.command.Command;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.LauncherMock;
import io.github.headlesshq.headlessmc.launcher.command.LaunchContext;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginCommandTest {
    @Test
    public void testLoginCommand() {
        Launcher launcher = LauncherMock.INSTANCE;
        new ArrayList<>(launcher.getAccountManager().getAccounts()).forEach(acc -> launcher.getAccountManager().removeAccount(acc));
        assertTrue(launcher.getAccountManager().getAccounts().isEmpty());

        try {
            launcher.getCommandLine().setCommandContext(new LaunchContext(launcher));
            boolean ran = false;
            for (Command command : launcher.getCommandLine().getCommandContext()) {
                if (command instanceof LoginCommand) {
                    LoginCommand loginCommand = (LoginCommand) command;
                    
                    // UUID , name
                    MinecraftProfile profile = new MinecraftProfile(UUID.randomUUID(), "DummyName");
                    
                    // (long expiry, String token, String refreshToken)
                    long expiry = System.currentTimeMillis() + 3600000L;
                    MinecraftToken token = new MinecraftToken(expiry, "dummy-token", "dummy-refresh");
                    
                    loginCommand.onSuccessfulLogin(profile, token);
                    
                    assertTrue(launcher.getAccountManager().getAccounts().stream()
                            .anyMatch(acc -> acc.getProfile() != null && acc.getProfile().getId().equals(profile.getId())));
                    
                    ran = true;
                }
            }

            assertTrue(ran);
        } finally {
            new ArrayList<>(launcher.getAccountManager().getAccounts()).forEach(acc -> launcher.getAccountManager().removeAccount(acc));
        }
    }
}
