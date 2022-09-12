package io.jenkins.plugins.reporter.steps;

import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import io.jenkins.plugins.util.JenkinsFacade;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;

public abstract class Provider extends AbstractDescribableImpl<Provider>  implements Serializable {
    
    private static final long serialVersionUID = -1356603376948787474L;
    
    private String id = StringUtils.EMPTY;
    
    private String name = StringUtils.EMPTY;

    private JenkinsFacade jenkins = new JenkinsFacade();

    /**
     * Called after de-serialization to retain backward compatibility.
     *
     * @return this
     */
    protected Object readResolve() {
        jenkins = new JenkinsFacade();

        return this;
    }
    
    @DataBoundSetter
    public void setId(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @DataBoundSetter
    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the {@link Symbol} name of this provider.
     *
     * @return the name of this provider, or "undefined" if no symbol has been defined
     */
    public String getSymbolName() {
        return getDescriptor().getSymbolName();
    }

    @Override
    public ProviderDescriptor getDescriptor() {
        return (ProviderDescriptor) jenkins.getDescriptorOrDie(getClass());
    }

    public abstract static class ProviderDescriptor extends Descriptor<Provider> {
        private final String defaultId;

        /**
         * Creates a new instance of {@link ProviderDescriptor} with the given ID.
         *
         * @param defaultId
         *         the unique ID of the tool
         */
        protected ProviderDescriptor(final String defaultId) {
            super();

            this.defaultId = defaultId;
        }

        @Override
        public String getId() {
            return defaultId;
        }

        /**
         * Returns the default name of this tool.
         *
         * @return the name
         */
        public String getName() {
            return getDisplayName();
        }

        /**
         * Returns the {@link Symbol} name of this provider.
         *
         * @return the name of this provider, or "undefined" if no symbol has been defined
         */
        public String getSymbolName() {
            Symbol annotation = getClass().getAnnotation(Symbol.class);

            if (annotation != null) {
                String[] symbols = annotation.value();
                if (symbols.length > 0) {
                    return symbols[0];
                }
            }
            return "undefined";
        }
    }
}
