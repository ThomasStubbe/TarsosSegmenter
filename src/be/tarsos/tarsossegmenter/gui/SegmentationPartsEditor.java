package be.tarsos.tarsossegmenter.gui;

import be.tarsos.tarsossegmenter.controller.listeners.AASModelListener;
import be.tarsos.tarsossegmenter.controller.listeners.AudioFileListener;
import be.tarsos.tarsossegmenter.model.AASModel;
import be.tarsos.tarsossegmenter.model.segmentation.Segmentation;
import be.tarsos.tarsossegmenter.model.segmentation.SegmentationList;
import be.tarsos.tarsossegmenter.model.segmentation.SegmentationPart;
import be.tarsos.tarsossegmenter.util.math.Math;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * <p>
 * A graphical user interface displaying a table to add, edit and merge
 * segmentationparts
 * </p>
 * 
 * @author Thomas Stubbe
 */
public class SegmentationPartsEditor extends JPanel implements
		TableModelListener, AASModelListener, AudioFileListener, ActionListener {

	private JTable table;
	private AASModel model;
	private JComboBox bestSegmentationComboBox;
	private JComboBox segmentationLevelComboBox;
	private String[] columnNames = { "Begin", "End", "\u0394T", "Label",
			"Comment" };
	private float END;
	private JScrollPane scrollPane;
	private JButton mergSegmentsButton;
	private JButton newSegmentButton;
	private Segmentation segmentation;
	private boolean constructing;

	public SegmentationPartsEditor() {
		this.setLayout(new BorderLayout());
		constructing = false;
		this.model = AASModel.getInstance();
		model.addModelListener(this);
		model.addAudioFileChangedListener(this);
		segmentation = model.getSegmentation();
		scrollPane = new JScrollPane();
		this.add(scrollPane, BorderLayout.CENTER);
		if (!segmentation.isEmpty()) {
			constructTable();
			END = segmentation.getEnd();
		}
		this.setPreferredSize(new Dimension(280, 300));
		// this.setPreferredSize(new Dimension(280, 500));
		this.setVisible(true);
		// this.setLocationRelativeTo(null);
	}

	private Object[][] constructModel() {
		Object[][] data = new Object[segmentation.getActiveSegmentationSize()][columnNames.length];
		ArrayList<SegmentationList> activeSegmentationLists = segmentation
				.getActiveSegmentationLists();
		int count = 0;
		for (int i = 0; i < activeSegmentationLists.size(); i++) {
			for (int j = 0; j < activeSegmentationLists.get(i).size(); j++) {
				data[count][0] = String.format("%.2f", activeSegmentationLists
						.get(i).get(j).getBegin());
				data[count][1] = String.format("%.2f", activeSegmentationLists
						.get(i).get(j).getEnd());
				data[count][2] = String.format("%.2f", activeSegmentationLists
						.get(i).get(j).getEnd()
						- activeSegmentationLists.get(i).get(j).getBegin());
				data[count][3] = activeSegmentationLists.get(i).get(j)
						.getLabel();
				data[count][4] = activeSegmentationLists.get(i).get(j)
						.getComment();
				if (data[count][3] != null
						&& !data[count][3].equals(SegmentationPart.EMPTY_LABEL)
						&& activeSegmentationLists.get(i).get(j).getMatch() > 0.1) {
					if (data[count][4] != null) {
						data[count][4] = data[count][4]
								+ String.format("%.2f", activeSegmentationLists
										.get(i).get(j).getMatch() * 100) + ")";
					} else {

						data[count][4] = "(Match: "
								+ String.format("%.2f", activeSegmentationLists
										.get(i).get(j).getMatch() * 100) + ")";
					}
				}
				count++;
			}
		}
		return data;
	}

	private void constructTable() {
		if (!constructing) {
			constructing = true;
			if (table != null) {
				table.getModel().removeTableModelListener(this);
			}

			table = new JTable(constructModel(), columnNames);

			table.getColumnModel().getColumn(0).setMaxWidth(45);
			table.getColumnModel().getColumn(1).setMaxWidth(45);
			table.getColumnModel().getColumn(2).setMaxWidth(45);// .setWidth(10);
			table.getColumnModel().getColumn(3).setMaxWidth(55);// .setWidth(10);
			DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
			centerRenderer.setHorizontalAlignment(JLabel.CENTER);
			table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
			table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
			table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
			table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

			table.getModel().addTableModelListener(this);

			table.setFillsViewportHeight(true);
			scrollPane.setViewportView(table);

			if (newSegmentButton == null) {
				newSegmentButton = new JButton("Add Segment...");
				newSegmentButton.setPreferredSize(new Dimension(135, 20));
				newSegmentButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						JTextField beginField = new JTextField();
						JTextField endField = new JTextField();
						JTextField labelField = new JTextField();
						final JComponent[] inputs = new JComponent[] {
								new JLabel("Begin"), beginField,
								new JLabel("End"), endField,
								new JLabel("Label"), labelField };
						int result = JOptionPane.showConfirmDialog(
								TarsosSegmenterGui.getInstance(), inputs,
								"Enter new segmentation point",
								JOptionPane.OK_CANCEL_OPTION);
						if (result == JOptionPane.OK_OPTION) {
							try {
								float begin;
								float end;
								String label;
								try {
									begin = Math.round(Float
											.parseFloat(beginField.getText()),
											2);
									end = Math.round(Float.parseFloat(endField
											.getText()), 2);
									label = SegmentationPart.EMPTY_LABEL;
									if (labelField.getText() != null
											&& !labelField.getText().isEmpty()) {
										label = labelField.getText();
									}
								} catch (Exception e3) {
									throw new Exception(
											"Could not parse the beginning, end or label. Make sure you use a dot instead of a comma for de decimals!");
								}

								if (begin < END && end <= END && begin < end) {
									// Adding the new segmentationpart to the
									// current segmentation
									// @TODO: juiste parameters

									SegmentationPart currentSP = segmentation
											.searchSegmentationPart(begin);
									if (currentSP == null
											|| currentSP.getEnd() < end) {
										throw new Exception(
												"Cannot insert a segment which overlaps more than one other segment! Merge first!");
									}
									// SegmentationPart NewSP = new
									// SegmentationPart(begin, end, label);
									currentSP.getContainer()
											.insertSegmentationPart(begin, end,
													label);
									// currentSP.getContainer().insertSegmentationPart(currentSP,
									// NewSP);

									table.getModel().removeTableModelListener(
											SegmentationPartsEditor.this);
									table.setModel(new DefaultTableModel(
											constructModel(), columnNames));
									table.getModel().addTableModelListener(
											SegmentationPartsEditor.this);
									table.revalidate();
									TarsosSegmenterGui.getInstance()
											.updateWaveFormGUI();
									TarsosSegmenterGui.getInstance()
											.updateAudioStructureGUI();
								} else {
									throw new Exception(
											"The begin should be less than the end, and the end should be less than the end of the song!");
								}
							} catch (Exception e2) {
								JOptionPane.showMessageDialog(
										TarsosSegmenterGui.getInstance(),
										"An error occured while adding the segment: "
												+ e2.getMessage(), "Error",
										JOptionPane.ERROR_MESSAGE);
							}
						}
					}
				});

				mergSegmentsButton = new JButton("Merge segments");
				mergSegmentsButton.setPreferredSize(new Dimension(135, 20));
				mergSegmentsButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						int reply = JOptionPane.showConfirmDialog(
								TarsosSegmenterGui.getInstance(),
								"Are you sure you want to merge the selected segmentation points?",
								"Merge?", JOptionPane.YES_NO_OPTION);
						if (reply == JOptionPane.YES_OPTION) {
							int rowIndices[] = table.getSelectedRows();
							boolean ok = false;
							if (rowIndices.length >= 2) {
								ok = true;
								int current = rowIndices[0];
								for (int i = 1; i < rowIndices.length; i++) {
									current++;
									if (rowIndices[i] != current) {
										ok = false;
									}
								}
							}
							if (!ok) {
								JOptionPane.showMessageDialog(
										TarsosSegmenterGui.getInstance(),
										"Gelieve 2 of meer opeenvolgende segmenten te selecteren",
										"Fout!", JOptionPane.ERROR_MESSAGE);
							} else {
								// Merge the selected segments

								float begin = Float.parseFloat(((String) table
										.getModel()
										.getValueAt(rowIndices[0], 0)).replace(
										",", "."));
								SegmentationPart beginSP = segmentation
										.searchSegmentationPart(begin);
								begin = Float
										.parseFloat(((String) table
												.getModel()
												.getValueAt(
														rowIndices[rowIndices.length - 1],
														0)).replace(",", "."));
								SegmentationPart endSP = segmentation
										.searchSegmentationPart(begin);
								beginSP.getContainer().merge(beginSP, endSP);

								table.getModel().removeTableModelListener(
										SegmentationPartsEditor.this);
								table.setModel(new DefaultTableModel(
										constructModel(), columnNames));
								table.getModel().addTableModelListener(
										SegmentationPartsEditor.this);
								table.revalidate();
								TarsosSegmenterGui.getInstance()
										.updateAudioStructureGUI();
								TarsosSegmenterGui.getInstance()
										.updateWaveFormGUI();
								update();
							}
						}
					}
				});

				JPanel topPanel = new JPanel();
				topPanel.setLayout(new BorderLayout());

				String[] suggestions = new String[segmentation
						.getSegmentation().size()];

				for (int i = 0; i < suggestions.length; i++) {
					suggestions[i] = "Suggestion " + (i + 1);
				}
				bestSegmentationComboBox = new JComboBox(suggestions);
				bestSegmentationComboBox
						.setPreferredSize(new Dimension(120, 20));

				if (suggestions.length > 1) {
					bestSegmentationComboBox.setSelectedIndex(0);
				} else {
					bestSegmentationComboBox.setVisible(false);
				}
				bestSegmentationComboBox.addActionListener(this);
				ArrayList<String> levels = new ArrayList();
				if (segmentation.getSegmentation().hasSubSegmentation()) {
					levels.add("Macro");
					levels.add("Meso");
					if (segmentation.getSegmentation().hasSubSubSegmentation()) {
						levels.add("Micro");
					}
				}
				segmentationLevelComboBox = new JComboBox(levels.toArray());
				segmentationLevelComboBox
						.setPreferredSize(new Dimension(80, 20));
				if (levels.isEmpty() || segmentation.isEmpty()) {
					segmentationLevelComboBox.setVisible(false);
				}
				segmentationLevelComboBox.addActionListener(this);
				JPanel dropBoxPanel = new JPanel();
				dropBoxPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
				dropBoxPanel.add(segmentationLevelComboBox);
				dropBoxPanel.add(bestSegmentationComboBox);
				topPanel.add(dropBoxPanel, BorderLayout.NORTH);

				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
				buttonPanel.add(newSegmentButton);
				buttonPanel.add(mergSegmentsButton);

				this.add(buttonPanel, BorderLayout.SOUTH);

				this.add(topPanel, BorderLayout.NORTH);
			}
			table.revalidate();
			this.revalidate();
			constructing = false;
		}
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		table.getModel().removeTableModelListener(this);
		int row = e.getFirstRow();
		int column = e.getColumn();

		TableModel model = (TableModel) e.getSource();

		String columnName = model.getColumnName(column);
		Object newData = model.getValueAt(row, column);

		SegmentationPart sp;
		if (!columnName.equals("Begin")) {
			Float time = Float.parseFloat(((String) model.getValueAt(row, 0))
					.replace(",", "."));
			sp = segmentation.searchSegmentationPart(time);
		} else {
			if (row > 0) {
				Float time = Float.parseFloat(((String) model.getValueAt(
						row - 1, 1)).replace(",", "."));
				sp = segmentation.searchSegmentationPart(time + 0.1); // 10de
																		// van
																		// seconde
																		// opschuiven
																		// om
																		// zeker
																		// juiste
																		// segment
																		// te
																		// hebben
			} else {
				sp = segmentation.searchSegmentationPart(0);
			}
		}
		if (columnName.equals("Begin")) {
			if (row > 0) {
				Float begin = Float.parseFloat(((String) newData).replace(",",
						"."));
				sp.setBeginEditPrevious(begin);
			} else if (columnName.equals("End")) {
				if (row < table.getRowCount() - 1) {
					Float end = Float.parseFloat(((String) newData).replace(
							",", "."));// (Float)data;//Float.parseFloat((String)data);
					sp.setEndEditNext(end);
				}
			} else if (columnName.equals("Label")) {
				String label;
				if (!((String) newData).isEmpty()) {
					label = ((String) newData);
				} else {
					label = SegmentationPart.EMPTY_LABEL;
				}
				sp.setLabel(label);
			} else if (columnName.equals("Comment")) {
				sp.setComment((String) newData);
			}
		}
		table.getModel().addTableModelListener(this);
		table.revalidate();
		TarsosSegmenterGui.getInstance().updateWaveFormGUI();
		TarsosSegmenterGui.getInstance().updateAudioStructureGUI();
		this.update();
	}

	@Override
	public void calculationStarted() {
		bestSegmentationComboBox.setVisible(false);
		segmentationLevelComboBox.setVisible(false);
		if (bestSegmentationComboBox != null
				&& bestSegmentationComboBox.getItemCount() > 1) {
			bestSegmentationComboBox.setSelectedIndex(0);
		}
	}

	@Override
	public void calculationDone() {
		update();
	}

	public void update() {
		if (!segmentation.isEmpty()) {
			END = segmentation.getEnd();
		}
		constructTable();

		// @TODO: altijd groter dan 0
		if (segmentation.getAmountOfActiveSuggestions() > 1
				&& segmentation.getAmountOfActiveSuggestions() != bestSegmentationComboBox
						.getItemCount()) {
			// bestSegmentationComboBox.removeActionListener(this);
			int length = segmentation.getAmountOfActiveSuggestions();

			String[] suggestions = new String[length];
			for (int i = 0; i < length; i++) {
				suggestions[i] = "Suggestion " + (i + 1);
			}
			if (suggestions.length > 1) {
				DefaultComboBoxModel model = new DefaultComboBoxModel(
						suggestions);
				bestSegmentationComboBox.setModel(model);
				bestSegmentationComboBox.setVisible(true);
			} else {
				bestSegmentationComboBox.setVisible(false);
			}
			bestSegmentationComboBox.setSelectedIndex(0);
			// bestSegmentationComboBox.addActionListener(this);
		} else if (segmentation.getAmountOfActiveSuggestions() > 1) {
			bestSegmentationComboBox.setVisible(true);
		} else {
			bestSegmentationComboBox.setVisible(false);
		}
		if (!segmentation.isEmpty()
				&& segmentation.getSegmentation().hasSubSegmentation()) {
			ArrayList<String> levels = new ArrayList();
			if (segmentation.getSegmentation().hasSubSubSegmentation()) {
				if (segmentationLevelComboBox.getItemCount() < 3) {
					levels.add("Macro");
					levels.add("Meso");
					levels.add("Micro");
					DefaultComboBoxModel model = new DefaultComboBoxModel(
							levels.toArray());
					segmentationLevelComboBox.setModel(model);
				}
			} else if (segmentationLevelComboBox.getItemCount() < 2) {
				levels.add("Macro");
				levels.add("Meso");
				DefaultComboBoxModel model = new DefaultComboBoxModel(
						levels.toArray());
				segmentationLevelComboBox.setModel(model);
			}
			segmentationLevelComboBox.setVisible(true);
		} else {
			segmentationLevelComboBox.setVisible(false);
		}
		// this.revalidate();
	}

	@Override
	public void audioFileChanged() {
		update();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(bestSegmentationComboBox)) {
			segmentation.setActiveSegmenationIndex(bestSegmentationComboBox
					.getSelectedIndex());
			update();
		} else if (e.getSource().equals(segmentationLevelComboBox)) {
			segmentation.setActiveSegmentationLevel(segmentationLevelComboBox
					.getSelectedIndex());
			update();
			if (bestSegmentationComboBox != null
					&& bestSegmentationComboBox.getItemCount() > 0) {
				bestSegmentationComboBox.setSelectedIndex(segmentation
						.getActiveSegmentationIndex());
			}
		}
		TarsosSegmenterGui.getInstance().updateAudioStructureGUI();
		TarsosSegmenterGui.getInstance().updateWaveFormGUI();
	}
}
