/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.tarsos.tarsossegmenter.util.io;

import be.tarsos.tarsossegmenter.model.AASModel;
import be.tarsos.tarsossegmenter.model.segmentation.Segmentation;
import be.tarsos.tarsossegmenter.model.segmentation.SegmentationList;
import be.tarsos.tarsossegmenter.model.segmentation.SegmentationPart;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 
 * @author Thomas
 */
public class SegmentationFileParser {

	static public void parseFile(String file, Segmentation segmentation) {
		segmentation.clearAll();
		// Moeten bijgehouden worden omdat macro als laatste aan bod komt
		LinkedList<SegmentationPart> microParts = new LinkedList();
		LinkedList<SegmentationPart> mesoParts = new LinkedList();
		boolean macroDone = false;
		try {

			int segmentationLevel = AASModel.MICRO_LEVEL;

			BufferedReader in = new BufferedReader(new FileReader(file));

			String strLine = in.readLine();

			while (strLine != null && strLine.trim().contains("item []:")) {
				strLine = in.readLine();
			}

			while ((strLine = in.readLine()) != null) { // lijn per lijn inlezen
				strLine = strLine.trim().toLowerCase();
				if (strLine.contains("name = ")) {
					String level = strLine.split("\"")[1];
					if (level.equals("micro")) {
						segmentationLevel = AASModel.MICRO_LEVEL;
					} else if (level.equals("meso")) {
						segmentationLevel = AASModel.MESO_LEVEL;
					} else if (level.equals("icro")) {
						segmentationLevel = AASModel.MACRO_LEVEL;
						macroDone = true;
					} else {
						segmentationLevel = -1;
					}
				} else if (segmentationLevel != -1
						&& strLine.contains("intervals [")) {
					float begin = Float
							.valueOf(in.readLine().trim().split(" ")[2]); // begin
					float end = Float
							.valueOf(in.readLine().trim().split(" ")[2]);

					SegmentationPart sp = new SegmentationPart(begin, end);
					String[] labelTable = in.readLine().trim().split("\"");
					if (labelTable.length >= 2) {
						String[] temp = labelTable[1].split("@:@");
						if (!temp[0].isEmpty() && !" ".equals(temp[0])) {
							sp.setLabel(temp[0]);
						}
						if (temp.length >= 2) {
							sp.setComment(temp[1]);
						}
					}
					switch (segmentationLevel) {
					case AASModel.MICRO_LEVEL:
						if (!macroDone) {
							microParts.add(sp);
						} else {
							segmentation.addSegmentationPart(sp,
									segmentationLevel);
						}
						break;
					case AASModel.MESO_LEVEL:
						if (!macroDone) {
							mesoParts.add(sp);
						} else {
							segmentation.addSegmentationPart(sp,
									segmentationLevel);
						}
						break;
					case AASModel.MACRO_LEVEL:
						segmentation.addSegmentationPart(sp, segmentationLevel);
						break;
					}
				}
			}
			while (!mesoParts.isEmpty()) {
				segmentation.addSegmentationPart(mesoParts.pop(),
						AASModel.MESO_LEVEL);
			}
			while (!microParts.isEmpty()) {
				segmentation.addSegmentationPart(microParts.pop(),
						AASModel.MICRO_LEVEL);
			}
		} catch (IOException e) {
			System.out
					.println("No segmentationfile was found or error while parsing");
		}

	}

	static public void writeToFile(String file, Segmentation segmentation) {
		FileWriter fstream;
		BufferedWriter out;
		try {
			File f = new File(file);
			f.delete();
			f.createNewFile();
			fstream = new FileWriter(f);
			out = new BufferedWriter(fstream);
			out.write("File type = \"ooTextFile\"\n");
			out.write("Object class = \"TextGrid\"\n\n");

			ArrayList<SegmentationList> macroSegmentation = segmentation
					.getSegmentationLists(AASModel.MACRO_LEVEL);
			ArrayList<SegmentationList> mesoSegmentation = segmentation
					.getSegmentationLists(AASModel.MESO_LEVEL);
			ArrayList<SegmentationList> microSegmentation = segmentation
					.getSegmentationLists(AASModel.MICRO_LEVEL);

			out.write("xmin = " + segmentation.getBegin() + " \n");
			out.write("xmax = " + segmentation.getEnd() + " \n");
			out.write("tiers? <exists> \n");

			int size = 0;
			if (macroSegmentation != null && !macroSegmentation.isEmpty()) {
				size++;
				if (mesoSegmentation != null && !mesoSegmentation.isEmpty()) {
					size++;
					if (microSegmentation != null
							&& !microSegmentation.isEmpty()) {
						size++;
					}
				}
			}

			out.write("size = " + size + " \n");
			out.write("item []: \n");

			// min en max bepalen

			if (microSegmentation != null && !microSegmentation.isEmpty()) {
				out.write("\titem [1]:\n");
				out.write("\t\tclass = \"IntervalTier\" \n");
				out.write("\t\tname = \"micro\" \n");
				out.write("\t\txmin = " + microSegmentation.get(0).getBegin()
						+ " \n");
				out.write("\t\txmax = "
						+ microSegmentation.get(microSegmentation.size() - 1)
								.getEnd() + " \n");
				out.write("\t\tintervals: size = "
						+ segmentation.getActiveMicroSize() + " \n");
				int count = 0;
				for (int i = 0; i < microSegmentation.size(); i++) {
					for (int j = 0; j < microSegmentation.get(i).size(); j++) {
						SegmentationPart sp = microSegmentation.get(i).get(j);
						count++;
						out.write("\t\tintervals [" + (count) + "]:\n");
						out.write("\t\t\txmin = " + sp.getBegin() + " \n");
						out.write("\t\t\txmax = " + sp.getEnd() + " \n");
						out.write("\t\t\ttext = \"" + sp.getLabel());
						if (sp.getComment() != null
								&& !sp.getComment().isEmpty()) {
							out.write("@:@" + sp.getComment());
						}
						out.write("\" \n");
					}
				}
			}

			if (mesoSegmentation != null && !mesoSegmentation.isEmpty()) {
				out.write("\titem [2]:\n");
				out.write("\t\tclass = \"IntervalTier\" \n");
				out.write("\t\tname = \"meso\" \n");
				out.write("\t\txmin = " + mesoSegmentation.get(0).getBegin()
						+ " \n");
				out.write("\t\txmax = "
						+ mesoSegmentation.get(mesoSegmentation.size() - 1)
								.getEnd() + " \n");
				out.write("\t\tintervals: size = "
						+ segmentation.getActiveMesoSize() + " \n");
				int count = 0;
				for (int i = 0; i < mesoSegmentation.size(); i++) {
					for (int j = 0; j < mesoSegmentation.get(i).size(); j++) {
						SegmentationPart sp = mesoSegmentation.get(i).get(j);
						count++;
						out.write("\t\tintervals [" + (count) + "]:\n");
						out.write("\t\t\txmin = " + sp.getBegin() + " \n");
						out.write("\t\t\txmax = " + sp.getEnd() + " \n");
						out.write("\t\t\ttext = \"" + sp.getLabel());
						if (sp.getComment() != null
								&& !sp.getComment().isEmpty()) {
							out.write("@:@" + sp.getComment());
						}
						out.write("\" \n");
					}
				}
			}
			if (macroSegmentation != null && !macroSegmentation.isEmpty()) {
				out.write("\titem [3]:\n");
				out.write("\t\tclass = \"IntervalTier\" \n");
				out.write("\t\tname = \"macro\" \n");
				out.write("\t\txmin = " + macroSegmentation.get(0).getBegin()
						+ " \n");
				out.write("\t\txmax = "
						+ macroSegmentation.get(macroSegmentation.size() - 1)
								.getEnd() + " \n");
				out.write("\t\tintervals: size = "
						+ segmentation.getActiveMacroSize() + " \n");
				int count = 0;
				for (int i = 0; i < macroSegmentation.size(); i++) {
					for (int j = 0; j < macroSegmentation.get(i).size(); j++) {
						SegmentationPart sp = macroSegmentation.get(i).get(j);
						count++;
						out.write("\t\tintervals [" + (count) + "]:\n");
						out.write("\t\t\txmin = " + sp.getBegin() + " \n");
						out.write("\t\t\txmax = " + sp.getEnd() + " \n");
						out.write("\t\t\ttext = \"" + sp.getLabel());
						if (sp.getComment() != null
								&& !sp.getComment().isEmpty()) {
							out.write("@:@" + sp.getComment());
						}
						out.write("\" \n");
					}
				}
			}
			out.close();
		} catch (IOException ex) {
			System.out.println("No access to file/dir or error while parsing");
		}
	}

	static public void parseCSVFile(String file, Segmentation segmentation) {
		segmentation.clearAll();
		try {
			int segmentationLevel = AASModel.MACRO_LEVEL;
			BufferedReader in = new BufferedReader(new FileReader(file));

			String strLine = in.readLine().trim().toUpperCase();

			while (strLine != null
					&& !(strLine.equals("MACRO") || strLine.equals("MESO") || strLine
							.equals("MICRO"))) {
				strLine = in.readLine();
				if (strLine != null) {
					strLine = strLine.trim().toUpperCase();
				}
			}
			if (strLine.equals("MACRO")) {
				segmentationLevel = AASModel.MACRO_LEVEL;
			} else if (strLine.equals("MESO")) {
				segmentationLevel = AASModel.MESO_LEVEL;
			} else if (strLine.equals("MICRO")) {
				segmentationLevel = AASModel.MICRO_LEVEL;
			}
			while ((strLine = in.readLine()) != null) { // lijn per lijn inlezen
				strLine = strLine.trim().toUpperCase();
				if (strLine.equals("MACRO")) {
					segmentationLevel = AASModel.MACRO_LEVEL;
				} else if (strLine.equals("MESO")) {
					segmentationLevel = AASModel.MESO_LEVEL;
				} else if (strLine.equals("MICRO")) {
					segmentationLevel = AASModel.MICRO_LEVEL;
				} else if (strLine.isEmpty()) {
				} else {
					String[] data = strLine.split(";");
					SegmentationPart sp = new SegmentationPart(
							Float.valueOf(data[0]), Float.valueOf(data[1]));
					if (data.length > 2 && data[2] != null
							&& !data[2].isEmpty()) {
						sp.setLabel(data[2]);
					}
					if (data.length > 3) {
						sp.setComment(data[3]);
					}
					segmentation.addSegmentationPart(sp, segmentationLevel);
				}
			}
		} catch (IOException e) {
			System.out
					.println("No segmentationfile was found or error while parsing");
		}
	}

	static public void writeToCSVFile(String file, Segmentation segmentation) {
		FileWriter fstream;
		BufferedWriter out;
		try {
			File f = new File(file);
			f.delete();
			f.createNewFile();
			fstream = new FileWriter(f);
			out = new BufferedWriter(fstream);
			ArrayList<SegmentationList> segmentationLists;

			for (int i = AASModel.MACRO_LEVEL; i <= AASModel.MICRO_LEVEL; i++) {
				segmentationLists = segmentation.getSegmentationLists(i);
				if (segmentationLists != null && !segmentationLists.isEmpty()) {
					switch (i) {
					case AASModel.MACRO_LEVEL:
						out.write("MACRO\n");
						break;
					case AASModel.MESO_LEVEL:
						out.write("MESO\n");
						break;
					case AASModel.MICRO_LEVEL:
						out.write("MICRO\n");
						break;
					}
					for (int j = 0; j < segmentationLists.size(); j++) {
						for (int k = 0; k < segmentationLists.get(j).size(); k++) {
							SegmentationPart sp = segmentationLists.get(j).get(
									k);
							if (sp.getComment() == null) {
								out.write(sp.getBegin() + ";" + sp.getEnd()
										+ ";" + sp.getLabel() + "\n");
							} else {
								out.write(sp.getBegin() + ";" + sp.getEnd()
										+ ";" + sp.getLabel() + ";"
										+ sp.getComment() + "\n");
							}
						}
					}
				}
			}
			out.close();
		} catch (IOException e) {
			System.out.println("No access to file/dir or error while parsing");
		}
	}
}
