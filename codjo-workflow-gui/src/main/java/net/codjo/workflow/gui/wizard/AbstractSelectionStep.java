/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import net.codjo.gui.toolkit.wizard.Step;
import net.codjo.gui.toolkit.wizard.StepPanel;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
/**
 *
 */
public abstract class AbstractSelectionStep extends StepPanel {
    private final Step subStep;


    protected AbstractSelectionStep(Step subStep, String title) {
        this.subStep = subStep;
        setName(title);
        buildLayout();
        if (subStep != null) {
            add(subStep.getGui());
        }
    }


    @Override
    public boolean isFulfilled() {
        if (subStep != null) {
            return super.isFulfilled() && subStep.isFulfilled();
        } else {
            return super.isFulfilled();
        }
    }


    @Override
    public void start(Map previousStepState) {
        initGui();
        if (subStep != null) {
            subStep.start(previousStepState);
        }
    }


    @Override
    public void cancel() {
        if (subStep != null) {
            subStep.cancel();
        }
    }


    @Override
    public Map<String, Object> getState() {
        Map<String, Object> states = new HashMap<String, Object>(super.getState());
        if (subStep != null) {
            states.putAll(subStep.getState());
        }
        return states;
    }


    @Override
    public synchronized void addPropertyChangeListener(String propertyName,
                                                       PropertyChangeListener listener) {
        super.addPropertyChangeListener(propertyName, listener);
        if (subStep != null) {
            subStep.addPropertyChangeListener(propertyName, listener);
        }
    }


    @Override
    public synchronized void removePropertyChangeListener(String propertyName,
                                                          PropertyChangeListener listener) {
        super.removePropertyChangeListener(propertyName, listener);

        if (subStep != null) {
            subStep.removePropertyChangeListener(propertyName, listener);
        }
    }


    public abstract void initGui();


    public abstract void buildLayout();
}
