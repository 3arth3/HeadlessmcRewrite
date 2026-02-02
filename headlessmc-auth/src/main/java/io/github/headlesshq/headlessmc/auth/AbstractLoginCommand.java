package io.github.headlesshq.headlessmc.auth;

import lombok.CustomLog;
import lombok.Setter;
import io.github.headlesshq.headlessmc.api.HeadlessMc;
import io.github.headlesshq.headlessmc.api.command.AbstractCommand;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandUtil;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.step.java.StepMinecraftJavaProfile;
import net.raphimc.minecraftauth.step.java.StepMinecraftJavaToken;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.step.msa.StepMsaToken;
import net.raphimc.minecraftauth.step.xbl.StepXblDeviceToken;
import net.raphimc.minecraftauth.step.xbl.StepXblSisuTokens;
import net.raphimc.minecraftauth.step.xbl.StepXstsToken;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import net.raphimc.minecraftauth.util.logging.ILogger;
import net.raphimc.minecraftauth.util.logging.JavaConsoleLogger;
import net.raphimc.minecraftauth.xbl.model.XblSisuTokens;
import net.raphimc.minecraftauth.xbl.model.XblToken;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@CustomLog
public abstract class AbstractLoginCommand extends AbstractCommand {

    private final List<Thread> threads = new CopyOnWriteArrayList<>();

    @Setter
    protected Supplier<HttpClient> httpClientFactory = MinecraftAuth::createHttpClient;

    public AbstractLoginCommand(HeadlessMc ctx) {
        this(ctx, "login", "Logs you into an account.");
    }

    public AbstractLoginCommand(HeadlessMc ctx, String name, String description) {
        super(ctx, name, description);
        replaceLogger();
    }

    protected abstract void onSuccessfulLogin(MinecraftProfile profile, MinecraftToken token);

    @Override
    public void execute(String line, String... args) throws CommandException {
        if (args.length > 1 && args[1].equalsIgnoreCase("-cancel")) {
            cancelLoginProcess(args);
        } else {
            loginWithDeviceCode(args);
        }
    }

    protected void loginWithDeviceCode(String... args) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    HttpClient httpClient = httpClientFactory.get();
                    ILogger logger = getLogger(args);

                    StepMsaDeviceCode.MsaDeviceCodeCallback callback = msaDeviceCode -> 
                        ctx.log("Please go to " + msaDeviceCode.getDirectVerificationUri() + " and enter the code to login.");

                    StepMsaDeviceCode.MsaDeviceCode msaCode = new StepMsaDeviceCode().getFromInput(logger, httpClient, callback);
                    MinecraftToken msaToken = new StepMsaToken().apply(logger, httpClient, msaCode);
                    XblToken xblToken = new StepXblDeviceToken("Win32").apply(logger, httpClient, msaToken);
                    XblSisuTokens sisuTokens = new StepXblSisuTokens(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY).apply(logger, httpClient, xblToken);
                    XblToken xstsToken = new StepXstsToken(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY).apply(logger, httpClient, sisuTokens.getAuthorizingToken());
                    MinecraftToken mcToken = new StepMinecraftJavaToken().apply(logger, httpClient, xstsToken);
                    MinecraftProfile mcProfile = new StepMinecraftJavaProfile().apply(logger, httpClient, mcToken);

                    onSuccessfulLogin(mcProfile, mcToken);
                } catch (InterruptedException e) {
                    ctx.log("Login process cancelled.");
                } catch (Exception e) {
                    ctx.log("Login failed: " + e.getMessage());
                    log.error("Authentication error", e);
                } finally {
                    threads.remove(this);
                }
            }
        };
        startLoginThread(thread);
    }

    protected void cancelLoginProcess(String... args) throws CommandException {
        if (args.length <= 2) {
            throw new CommandException("Please specify the login process id!");
        }
        String targetName = "HMC Login Thread - " + args[2];
        for (Thread thread : threads) {
            if (targetName.equals(thread.getName())) {
                thread.interrupt();
                threads.remove(thread);
                ctx.log("Cancelled login process " + args[2] + ".");
                return;
            }
        }
        ctx.log("Failed to find login process with id " + args[2] + "!");
    }

    protected void startLoginThread(Thread thread) {
        int threadId = 0;
        String name;
        synchronized (threads) {
            do {
                name = "HMC Login Thread - " + threadId++;
            } while (hasThreadWithName(name));
            thread.setName(name);
            thread.setDaemon(true);
            threads.add(thread);
        }
        ctx.log("Starting login process " + (threadId - 1) + ". Use 'login -cancel " + (threadId - 1) + "' to abort.");
        thread.start();
        ctx.getExitManager().addTaskThread(thread);
    }

    protected ILogger getLogger(String... args) {
        return CommandUtil.hasFlag("-verbose", args) ? MinecraftAuth.LOGGER : NoLogging.INSTANCE;
    }

    protected boolean hasThreadWithName(String threadName) {
        return threads.stream().anyMatch(t -> threadName.equals(t.getName()));
    }

    public static void replaceLogger() {
        MinecraftAuth.LOGGER = new JavaConsoleLogger(java.util.logging.Logger.getLogger("MinecraftAuth"));
    }
}
            
