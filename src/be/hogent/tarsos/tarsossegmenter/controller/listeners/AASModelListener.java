/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.hogent.tarsos.tarsossegmenter.controller.listeners;

import java.util.EventListener;

/**
 * <p>
 * The interface to listen to the model.
 * </p>
 * Registered listeners will be alerted if the the segmentation analyse has started or finished.
 * @author Thomas Stubbe
 */

public interface AASModelListener extends EventListener {
    
    public void calculationStarted();
    
    public void calculationDone();
    
    
}
