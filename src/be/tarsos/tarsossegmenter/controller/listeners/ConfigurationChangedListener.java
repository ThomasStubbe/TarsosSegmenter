package be.tarsos.tarsossegmenter.controller.listeners;

import java.util.EventListener;

/**
 * <p>
 * The interface to listen to the configuration.
 * </p>
 * Registered listeners will be alerted if the configuration has changed.
 * @author Thomas Stubbe
 */

public interface ConfigurationChangedListener extends EventListener {
    
    public void configurationChanged();
}
