package org.apache.servicemix.kernel.gshell.core;

import java.util.Map;
import java.util.Dictionary;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import org.springframework.osgi.context.BundleContextAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.registry.CommandRegistry;
import org.apache.geronimo.gshell.registry.AliasRegistry;
import org.apache.geronimo.gshell.wisdom.command.CommandSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandBundle implements BundleContextAware, InitializingBean, DisposableBean {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired(required = false)
    private CommandRegistry commandRegistry;

    @Autowired(required = false)
    private AliasRegistry aliasRegistry;

    private BundleContext bundleContext;

    private Map<String,Command> commands;

    private Map<String,String> aliases;

    private List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();

    public CommandBundle() {
    }

    public Map<String, Command> getCommands() {
        return commands;
    }

    public void setCommands(final Map<String, Command> commands) {
        assert commands != null;

        this.commands = commands;
    }

    public Map<String, String> getAliases() {
        return aliases;
    }

    public void setAliases(final Map<String, String> aliases) {
        assert aliases != null;

        this.aliases = aliases;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void afterPropertiesSet() throws Exception {
        log.debug("Initializing command bundle");
        if (commandRegistry != null && aliasRegistry != null) {
            log.debug("Command bundle is using the auto wired command/alias registry");
            for (String name : commands.keySet()) {
                log.debug("Registering command: {}", name);
                commandRegistry.registerCommand(name, commands.get(name));
            }
            for (String name : aliases.keySet()) {
                log.debug("Registering alias: {}", name);
                aliasRegistry.registerAlias(name, aliases.get(name));
            }
        } else if (bundleContext != null) {
            if (aliases != null && aliases.size() > 0) {
                throw new Exception("Aliases are not supported in OSGi");
            }
            log.debug("Command bundle is using the OSGi registry");
            for (String name : commands.keySet()) {
                log.debug("Registering command: {}", name);
                Dictionary props = new Properties();
                props.put(OsgiCommandRegistry.NAME, name);
                registrations.add(bundleContext.registerService(Command.class.getName(), commands.get(name), props));
            }
        } else {
            throw new Exception("Command bundle should be wired to the command/alias registry or be used in an OSGi context");
        }
    }

    public void destroy() {
        log.debug("Destroying command bundle");
        for (ServiceRegistration reg : registrations) {
            reg.unregister();
        }
    }

}
