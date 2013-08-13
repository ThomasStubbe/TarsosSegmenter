/**
 *
 * Tarsos is developed by Joren Six at The Royal Academy of Fine Arts & Royal
 * Conservatory, University College Ghent, Hoogpoort 64, 9000 Ghent - Belgium
 * 
*
 */
package be.hogent.tarsos.tarsossegmenter.gui;

import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.tarsossegmenter.util.configuration.ConfKey;
import be.hogent.tarsos.tarsossegmenter.util.configuration.Configuration;
import be.hogent.tarsos.tarsossegmenter.util.configuration.Configuration.ConfigChangeListener;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.Map.Entry;
import javax.swing.*;

/**
 * <p>
 * A panel to configure the configuration parameters.
 * </p>
 *
 * @author Joren Six, Thomas Stubbe
 *
 */
public class ConfigurationPanel extends JPanel {

    private final HashMap<JComponent, ConfKey> ConfigurationTextFields;

    /**
     * Constructs the ConfigurationPanel
     *
     */
    public ConfigurationPanel() {
        super(new BorderLayout());
        FormLayout layout = new FormLayout("right:pref, 3dlu, min:grow");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        ConfigurationTextFields = new HashMap();
        builder.setDefaultDialogBorder();
        builder.setRowGroupingEnabled(true);
        addConfigurationTextFields(builder);
        JComponent center = builder.getPanel();
        add(center, BorderLayout.CENTER);
        Configuration.addListener(new ConfigChangeListener() {
            @Override
            public void configurationChanged(final ConfKey key) {
                for (Entry<JComponent, ConfKey> entry : ConfigurationTextFields.entrySet()) {
                    if (entry.getValue() == key) {
                        if (key.getType().equals(ConfKey.BOOL)) {
                            boolean value = Configuration.getBoolean(key);
                            ((JCheckBox) entry.getKey()).setSelected(value);
                        } else {
                            String value = Configuration.get(key);
                            if (key.equals(ConfKey.framesize)) {
                                ((JComboBox) entry.getKey()).setSelectedIndex(ConfKey.getFrameSizeIndex(Integer.valueOf(value)));
                            } else {
                                ((JTextField) entry.getKey()).setText(value);
                            }
                        }
                    }
                }
                AASModel.getInstance().loadConfiguration();
                TarsosSegmenterGui.getInstance().updateImageOptionsGUI();
            }
        });
    }

    /**
     * Adds a text field for each configured value to the form.
     *
     * @param builder The form builder.
     */
    private void addConfigurationTextFields(final DefaultFormBuilder builder) {

        for (String category : ConfKey.getCategories()) {
            builder.appendSeparator(category);

            List<ConfKey> orderedKeys = new ArrayList();
            orderedKeys.addAll(Arrays.asList(ConfKey.getValues(category)));

            Collections.sort(orderedKeys, new Comparator<ConfKey>() {
                @Override
                public int compare(final ConfKey o1, final ConfKey o2) {
                    int order = o1.getType().compareTo(o2.getType());
                    if (order != 0) {
                        return order;
                    } else {
                        return o1.name().compareTo(o2.name());
                    }
                }
            });

            for (ConfKey key : orderedKeys) {
                String tooltip = Configuration.getDescription(key);
                String label = Configuration.getHumanName(key);
                if (label == null) {
                    label = key.name();
                }
                switch (key.getType()) {
                    case ConfKey.BOOL:
                        JCheckBox confCheckBox = new JCheckBox();
                        boolean value = Configuration.getBoolean(key);

                        confCheckBox.setToolTipText(tooltip);
                        confCheckBox.setSelected(value);
                        builder.append(label + ":", confCheckBox, true);
                        ConfigurationTextFields.put(confCheckBox, key);

                        confCheckBox.addItemListener(new ItemListener() {
                            @Override
                            public void itemStateChanged(ItemEvent e) {
                                JCheckBox confCheckBox = (JCheckBox) e.getSource();
                                ConfKey key = ConfigurationTextFields.get(confCheckBox);
                                boolean value = confCheckBox.isSelected();
                                if (value != Configuration.getBoolean(key)) {
                                    Configuration.set(key, value);
                                }
                            }
                        });
                        break;
                    default:

                        JComponent valueComponent;
                        String value2 = Configuration.get(key);
                        if (key == ConfKey.framesize) {
                            int value3 = Configuration.getInt(key);
                            int selectedIndex = 0;
                            String[] values = new String[ConfKey.FRAMESIZES.length];
                            for (int i = 0; i < values.length; i++) {
                                if (ConfKey.FRAMESIZES[i] == value3) {
                                    selectedIndex = i;
                                }
                                values[i] = Integer.toString(ConfKey.FRAMESIZES[i]);

                            }
                            valueComponent = new JComboBox(values);
                            ((JComboBox) valueComponent).setSelectedIndex(selectedIndex);
                        } else {
                            valueComponent = new JTextField();

                            ((JTextField) valueComponent).setText(value2);


                        }

                        valueComponent.setToolTipText(tooltip);
                        builder.append(label + ":", valueComponent, true);
                        ConfigurationTextFields.put(valueComponent, key);
                        valueComponent.addFocusListener(new FocusListener() {
                            @Override
                            public void focusLost(FocusEvent e) {
                                JComponent component = (JComponent) e.getSource();
                                String value;
                                ConfKey key = ConfigurationTextFields.get(component);
                                if (key == ConfKey.framesize) {
                                    value = ((JComboBox) component).getSelectedItem().toString();
                                } else {
                                    value = ((JTextField) component).getText();
                                }

                                if (!value.equals(Configuration.get(key)) && !"".equals(value.trim())) {
                                    Configuration.set(key, value);
                                }
                            }

                            @Override
                            public void focusGained(FocusEvent e) {
                            }
                        });
                        break;
                }
            }
        }
    }
}
