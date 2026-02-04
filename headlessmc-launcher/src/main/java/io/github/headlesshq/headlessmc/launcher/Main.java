package io.github.headlesshq.headlessmc.launcher;

import lombok.CustomLog;
import lombok.experimental.UtilityClass;
import io.github.headlesshq.headlessmc.api.HeadlessMcApi;
import io.github.headlesshq.headlessmc.api.exit.ExitManager;
import io.github.headlesshq.headlessmc.launcher.auth.AuthException;
import io.github.headlesshq.headlessmc.launcher.launch.ExitToWrapperException;
import io.github.headlesshq.headlessmc.launcher.version.VersionUtil;

import java.util.Scanner;

@CustomLog
@UtilityClass
public final class Main {

    public static void main(String[] args) {
        ExitManager exitManager = new ExitManager();
        Throwable throwable = null;
        try {
            runHeadlessMc(exitManager, args);
        } catch (Throwable t) {
            throwable = t;
        } finally {
            exitManager.onMainThreadEnd(throwable);
            if (throwable instanceof ExitToWrapperException) {
                HeadlessMcApi.setInstance(null);
                LauncherApi.setLauncher(null);
            } else {
                handleSystemExit(exitManager, throwable);
            }
        }
    }

    private static void runHeadlessMc(ExitManager exitManager, String... args) throws AuthException {
        LauncherBuilder builder = new LauncherBuilder();
        builder.exitManager(exitManager);
        builder.initLogging();

        if (Main.class.getClassLoader() == ClassLoader.getSystemClassLoader()) {
            log.warn("Not running from the wrapper. No plugin support and in-memory launching.");
        }

        Launcher launcher = builder.buildDefault();
        if (!QuickExitCliHandler.checkQuickExit(launcher, args)) {
            log.info(String.format("Detected OS: %s", builder.os()));
            log.info(String.format("Minecraft Dir: %s", launcher.getMcFiles()));
            launcher.log(VersionUtil.makeTable(VersionUtil.releases(launcher.getVersionService().getContents())));

            startCommandLoop(launcher);
        }
    }

    private static void startCommandLoop(Launcher launcher) {
        log.info("Terminal initialized (Scanner-mode). Type 'exit' to quit.");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                break;
            }
            if (!line.isEmpty()) {
                // call
                launcher.getCommandLine().execute(line);
            }
        }
    }

    private static void handleSystemExit(ExitManager exitManager, Throwable throwable) {
        try {
            if (throwable == null) {
                exitManager.exit(0);
            } else {
                log.error(throwable);
                exitManager.exit(-1);
            }
        } catch (Throwable exitThrowable) {
            if (throwable != null && exitThrowable.getClass() == throwable.getClass()) {
                log.error("Failed to exit gracefully!", exitThrowable);
            }
        }
    }
}
            
