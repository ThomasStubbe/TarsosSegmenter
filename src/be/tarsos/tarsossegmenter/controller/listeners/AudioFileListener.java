/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.tarsos.tarsossegmenter.controller.listeners;

import java.util.EventListener;

/**
 * <p>
 * The interface to listen to audiofile changes.
 * </p>
 * Registered listeners will be alerted if the new audiofile is loaded.
 * @author Thomas Stubbe
 */

public interface AudioFileListener extends EventListener {
    
    public void audioFileChanged();
    
}

