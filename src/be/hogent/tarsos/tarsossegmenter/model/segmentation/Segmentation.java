/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.hogent.tarsos.tarsossegmenter.model.segmentation;

import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.tarsossegmenter.util.configuration.ConfKey;
import be.hogent.tarsos.tarsossegmenter.util.configuration.Configuration;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 
 * @author Stubbe
 */
public class Segmentation {

	private IndexReference microSegmentationIndex;
	private IndexReference macroSegmentationIndex; // Determinates the chosen
													// segmentation
	private IndexReference mesoSegmentationIndex;
	private ArrayList<SegmentationList> segmentationSuggestions; // for every
																	// suggestion
																	// it
																	// contains
																	// the
																	// different
																	// labels
																	// and a
																	// link to a
																	// collection
																	// containing
																	// those
																	// segmentationParts
	private int activeSegmentationLevel;
	private ArrayList<Float> macroSegmentationPoints;
	private ArrayList<Float> mesoSegmentationPoints;
	private ArrayList<Float> microSegmentationPoints;

	public Segmentation() {
		segmentationSuggestions = new ArrayList();
		segmentationSuggestions.add(new SegmentationList(null,
				AASModel.MACRO_LEVEL));
		macroSegmentationIndex = new IndexReference();
		mesoSegmentationIndex = new IndexReference();
		microSegmentationIndex = new IndexReference();
		activeSegmentationLevel = AASModel.MACRO_LEVEL;
		macroSegmentationPoints = new ArrayList();
		mesoSegmentationPoints = new ArrayList();
		microSegmentationPoints = new ArrayList();
	}

	public void setMacroSegmentationIndex(int segmentationIndex) {
		this.macroSegmentationIndex.index = segmentationIndex;
	}

	public void setMicroSegmentationIndex(int microSegmentationIndex) {
		this.microSegmentationIndex.index = microSegmentationIndex;
	}

	public void setMesoSegmentationIndex(int segmentationIndex) {
		this.mesoSegmentationIndex.index = segmentationIndex;
	}

	public SegmentationList getSegmentation() {
		if (macroSegmentationIndex.index < segmentationSuggestions.size()) {
			return (segmentationSuggestions.get(macroSegmentationIndex.index));
		} else {
			return null;
		}
	}

	public void clearAll() {
		for (int i = segmentationSuggestions.size() - 1; i > 0; i--) {
			segmentationSuggestions.get(i).clearSubSegmentation();
			segmentationSuggestions.get(i).clear();
			segmentationSuggestions.remove(i);
		}
		if (segmentationSuggestions.size() > 0) {
			segmentationSuggestions.get(0).clearSubSegmentation();
			segmentationSuggestions.get(0).clear();
		}
		macroSegmentationIndex.index = 0;
		mesoSegmentationIndex.index = 0;
		microSegmentationIndex.index = 0;
	}

	public void clearMesoAndMicro() {
		for (int i = segmentationSuggestions.size() - 1; i >= 0; i--) {
			segmentationSuggestions.get(i).clearSubSegmentation();
		}
		mesoSegmentationIndex.index = 0;
		microSegmentationIndex.index = 0;
	}

	public void clearMicro() {
		for (int i = segmentationSuggestions.size() - 1; i >= 0; i--) {
			for (int j = segmentationSuggestions.get(i).size() - 1; j >= 0; j--) {
				segmentationSuggestions.get(i).get(j).clearSubSegmentations();
			}
		}
		microSegmentationIndex.index = 0;
	}

	public boolean isEmpty() {
		if (this.getSegmentation() != null && this.getSegmentation().size() > 0) {
			return false;
		} else {
			return true;
		}
	}
	
	

	public float getBegin() {
		return getSegmentation().getBegin();
	}

	public float getEnd() {
		return getSegmentation().getEnd();
	}

	public int getAmountOfActiveSuggestions() {
		return getAmountOfSuggestions(activeSegmentationLevel);
	}

	public int getAmountOfSuggestions(int segmentationLevel) {
		int size;
		switch (segmentationLevel) {
		case AASModel.MACRO_LEVEL:
			return this.segmentationSuggestions.size();
		case AASModel.MESO_LEVEL:
			size = 0;
			for (int i = 0; i < getSegmentation().size(); i++) {
				size = Math.max(size, getSegmentation().get(i)
						.getSubSegmentationSuggestions().size());
			}
			return size;
		case AASModel.MICRO_LEVEL:
			size = 0;
			for (int i = 0; i < getSegmentation().size(); i++) {
				for (int j = 0; j < getSegmentation().get(i)
						.getSubSegmentation().size(); j++) {
					size = Math.max(size, getSegmentation().get(i)
							.getSubSegmentation().get(j)
							.getSubSegmentationSuggestions().size());
				}
			}
			return size;
		}
		return -1;
	}

	public void setActiveSegmenationIndex(int selectedIndex) {
		switch (activeSegmentationLevel) {
		case AASModel.MACRO_LEVEL:
			setMacroSegmentationIndex(selectedIndex);
			break;
		case AASModel.MESO_LEVEL:
			setMesoSegmentationIndex(selectedIndex);
			break;
		case AASModel.MICRO_LEVEL:
			setMicroSegmentationIndex(selectedIndex);
			break;
		}
	}

	public int getActiveSegmentationIndex() {
		return getSegmentationIndex(activeSegmentationLevel);
	}

	public IndexReference getActiveSegmentationIndexReference() {
		return getSegmentationIndexReference(activeSegmentationLevel);
	}

	public ArrayList<SegmentationList> getSegmentationLists(
			int segmentationLevel) {
		switch (segmentationLevel) {
		case (AASModel.MACRO_LEVEL):
			return getMacroSegmentationLists();
		case (AASModel.MESO_LEVEL):
			return getMesoSegmentationLists();
		case (AASModel.MICRO_LEVEL):
			return getMicroSegmentationLists();
		}
		return null;
	}

	public int getActiveSegmentationLevel() {
		return activeSegmentationLevel;
	}

	public void setActiveSegmentationLevel(int activeSegmentationLevel) {
		this.activeSegmentationLevel = activeSegmentationLevel;
	}

	public SegmentationPart searchSegmentationPart(double time) {
		time = be.hogent.tarsos.tarsossegmenter.util.math.Math.round(
				(float) time, 2);
		SegmentationPart sp;
		if (!isEmpty()) {
			sp = searchSegmentationPartInList(getSegmentation(), time);
			if (activeSegmentationLevel == AASModel.MESO_LEVEL
					|| activeSegmentationLevel == AASModel.MICRO_LEVEL) { // meso
																			// of
																			// micro
				sp = searchSegmentationPartInList(sp.getSubSegmentation(), time);
				if (activeSegmentationLevel == AASModel.MICRO_LEVEL) { // micro
					sp = searchSegmentationPartInList(sp.getSubSegmentation(),
							time);
				}
			}
			return sp;
		} else {
			return null;
		}
	}

	public SegmentationPart searchSegmentationPart(double time, int level) {
		time = be.hogent.tarsos.tarsossegmenter.util.math.Math.round(
				(float) time, 2);
		SegmentationPart sp;
		if (!isEmpty()) {
			sp = searchSegmentationPartInList(getSegmentation(), time);
			if (level == AASModel.MESO_LEVEL || level == AASModel.MICRO_LEVEL) { // meso
																					// of
																					// micro
				if (sp != null && sp.hasSubSegmentation()) {
					sp = searchSegmentationPartInList(sp.getSubSegmentation(),
							time);
					if (sp != null && sp.hasSubSegmentation()) {
						if (level == AASModel.MICRO_LEVEL) { // micro
							sp = searchSegmentationPartInList(
									sp.getSubSegmentation(), time);
						}
					}
				}
			}
			return sp;
		} else {
			return null;
		}
	}

	public SegmentationPart searchSegmentationPartInList(
			SegmentationList searchList, double time) {
		int i = 0;
		while (i < searchList.size()
				&& !(searchList.get(i).getBegin() <= time && searchList.get(i)
						.getEnd() > time)) {
			i++;
		}
		if (i < searchList.size()) {
			return searchList.get(i);
		} else {
			return null;
		}
	}

	public int getActiveSegmentationSize() {
		switch (activeSegmentationLevel) {
		case (AASModel.MACRO_LEVEL):
			return getActiveMacroSize();
		case (AASModel.MESO_LEVEL):
			return getActiveMesoSize();
		case (AASModel.MICRO_LEVEL):
			return getActiveMicroSize();
		}
		return -1;
	}

	public int getActiveMacroSize() {
		if (getSegmentation() != null) {
			return getSegmentation().size();
		} else {
			return -1;
		}
	}

	public int getActiveMesoSize() {
		int size = 0;
		for (int i = 0; i < getSegmentation().size(); i++) {
			if (getSegmentation().get(i).hasSubSegmentation()) {
				size += getSegmentation().get(i).getSubSegmentation().size();
			}
		}
		return size;
	}

	public int getActiveMicroSize() {
		int size = 0;
		for (int i = 0; i < getSegmentation().size(); i++) {
			for (int j = 0; j < getSegmentation().get(i).getSubSegmentation()
					.size(); j++) {
				size += getSegmentation().get(i).getSubSegmentation().get(j)
						.getSubSegmentation().size();
			}
		}
		return size;
	}

	public ArrayList<SegmentationList> getActiveSegmentationLists() {
		switch (activeSegmentationLevel) {
		case (AASModel.MACRO_LEVEL):
			return getMacroSegmentationLists();
		case (AASModel.MESO_LEVEL):
			return getMesoSegmentationLists();
		case (AASModel.MICRO_LEVEL):
			return getMicroSegmentationLists();
		}
		return null;
	}

	private ArrayList<SegmentationList> getMacroSegmentationLists() {
		ArrayList<SegmentationList> activeSegmentationLists = new ArrayList();
		activeSegmentationLists.add(getSegmentation());
		return activeSegmentationLists;
	}

	private ArrayList<SegmentationList> getMesoSegmentationLists() {
		ArrayList<SegmentationList> activeSegmentationLists = new ArrayList();
		for (int i = 0; i < getSegmentation().size(); i++) {
			if (getSegmentation().get(i).hasSubSegmentation()) {
				activeSegmentationLists.add(getSegmentation().get(i)
						.getSubSegmentation());
			}
		}
		return activeSegmentationLists;
	}

	private ArrayList<SegmentationList> getMicroSegmentationLists() {
		ArrayList<SegmentationList> activeSegmentationLists = new ArrayList();
		for (int i = 0; i < getSegmentation().size(); i++) {
			if (getSegmentation().get(i).hasSubSegmentation()) {
				for (int j = 0; j < getSegmentation().get(i)
						.getSubSegmentation().size(); j++) {
					if (getSegmentation().get(i).getSubSegmentation()
							.hasSubSegmentation()) {
						activeSegmentationLists.add(getSegmentation().get(i)
								.getSubSegmentation().get(j)
								.getSubSegmentation());
					}
				}
			}
		}
		return activeSegmentationLists;
	}

	public int getAmountOfMacroSuggestions() {
		return this.segmentationSuggestions.size();
	}

	public void addSegmentationPart(SegmentationPart sp, int segmentationLevel) {
		switch (segmentationLevel) {
		case AASModel.MACRO_LEVEL:
			getSegmentation().add(sp);
			break;
		case AASModel.MESO_LEVEL:
			if (!this.isEmpty()) {
				if (getSegmentation().getBegin() <= sp.getBegin()
						&& getSegmentation().getEnd() >= sp.getEnd()) {
					SegmentationPart temp = searchSegmentationPart(
							sp.getBegin(), AASModel.MACRO_LEVEL);
					if (temp != null) {
						temp.createSubSegmentationSuggestionList();
						temp.getSubSegmentation().add(sp);
					} else {
						System.out.println("Error in segmentationTimes");
					}
				}
			}
			break;
		case AASModel.MICRO_LEVEL:
			if (!this.isEmpty()) {
				if (getSegmentation().getBegin() <= sp.getBegin()
						&& getSegmentation().getEnd() >= sp.getEnd()) {
					SegmentationPart temp = searchSegmentationPart(
							sp.getBegin(), AASModel.MESO_LEVEL);
					if (temp != null) {
						temp.createSubSegmentationSuggestionList();
						temp.getSubSegmentation().add(sp);
					} else {
						System.out.println("Error in segmentationTimes");
					}
				}
			}
			break;
		}
	}

	// @TODO:
	public void addSegmentationPoint(float time, int segmentationLevel) {
		time = be.hogent.tarsos.tarsossegmenter.util.math.Math.round(
				(float) time, 2);
		int i = 0;
		if (Configuration.getBoolean(ConfKey.enable_micro)) {
			while (i < microSegmentationPoints.size()
					&& microSegmentationPoints.get(i) < time) {
				i++;
			}
			if (i == microSegmentationPoints.size()) {
				if (microSegmentationPoints.size() > 0) {
					if (time
							- microSegmentationPoints
									.get(microSegmentationPoints.size() - 1) > 0.5f) {
						microSegmentationPoints.add(time);
					}
				} else {
					microSegmentationPoints.add(time);
				}
			} else {
				if (microSegmentationPoints.get(i) != time) {
					if (microSegmentationPoints.get(i) - time > 0.5f
							&& time - microSegmentationPoints.get(i - 1) > 0.5f) {
						microSegmentationPoints.add(i, time);
					}
				}
			}
		}
		if (segmentationLevel < AASModel.MICRO_LEVEL
				&& Configuration.getBoolean(ConfKey.enable_meso)) {
			i = 0;
			while (i < mesoSegmentationPoints.size()
					&& mesoSegmentationPoints.get(i) < time) {
				i++;
			}
			if (i == mesoSegmentationPoints.size()) {
				if (mesoSegmentationPoints.size() > 0) {
					if (time
							- mesoSegmentationPoints.get(mesoSegmentationPoints
									.size() - 1) > 0.7f) {
						mesoSegmentationPoints.add(time);
					}
				} else {
					mesoSegmentationPoints.add(time);
				}
			} else {
				if (mesoSegmentationPoints.get(i) != time) {
					if (mesoSegmentationPoints.get(i) - time > 0.7f
							&& time - mesoSegmentationPoints.get(i - 1) > 0.7f) {
						mesoSegmentationPoints.add(i, time);
					}
				}
			}
		}
		if (segmentationLevel == AASModel.MACRO_LEVEL
				&& Configuration.getBoolean(ConfKey.enable_macro)) {
			i = 0;
			while (i < macroSegmentationPoints.size()
					&& macroSegmentationPoints.get(i) < time) {
				i++;
			}
			if (i == macroSegmentationPoints.size()) {
				if (macroSegmentationPoints.size() > 0) {
					if (time
							- macroSegmentationPoints
									.get(macroSegmentationPoints.size() - 1) > 0.8f) {
						macroSegmentationPoints.add(time);
					}
				} else {
					macroSegmentationPoints.add(time);
				}

			} else {
				if (macroSegmentationPoints.get(i) != time) {
					if (macroSegmentationPoints.get(i) - time > 0.8
							&& time - macroSegmentationPoints.get(i - 1) > 0.8f) {
						macroSegmentationPoints.add(i, time);
					}
				}
			}
		}
	}

	public ArrayList<Float> getSegmentationPoints(int segmentationLevel) {
		switch (segmentationLevel) {
		case AASModel.MACRO_LEVEL:
			return macroSegmentationPoints;
		case AASModel.MESO_LEVEL:
			return mesoSegmentationPoints;
		case AASModel.MICRO_LEVEL:
			return microSegmentationPoints;
		}
		return null;
	}

	public void clearAllSegmentationPoints() {
		macroSegmentationPoints.clear();
		mesoSegmentationPoints.clear();
		microSegmentationPoints.clear();
	}

	public void clearSegmentationPoints(int segmentationLevel) {
		switch (segmentationLevel) {
		case AASModel.MACRO_LEVEL:
			macroSegmentationPoints.clear();
		case AASModel.MESO_LEVEL:
			mesoSegmentationPoints.clear();
		case AASModel.MICRO_LEVEL:
			microSegmentationPoints.clear();
		}
	}

	public void sortSegmentationPoints() {
		Collections.sort(macroSegmentationPoints);
		Collections.sort(mesoSegmentationPoints);
		Collections.sort(microSegmentationPoints);
	}

	public int getSegmentationIndex(int segmentationLevel) {
		switch (segmentationLevel) {
		case AASModel.MACRO_LEVEL:
			return macroSegmentationIndex.index;
		case AASModel.MESO_LEVEL:
			return mesoSegmentationIndex.index;
		case AASModel.MICRO_LEVEL:
			return microSegmentationIndex.index;
		}
		return -1;
	}

	protected IndexReference getSegmentationIndexReference(int segmentationLevel) {
		switch (segmentationLevel) {
		case AASModel.MACRO_LEVEL:
			return macroSegmentationIndex;
		case AASModel.MESO_LEVEL:
			return mesoSegmentationIndex;
		case AASModel.MICRO_LEVEL:
			return microSegmentationIndex;
		}
		return null;
	}

	public ArrayList<SegmentationList> getMacroSuggestions() {
		return segmentationSuggestions;
	}

	public class IndexReference {

		public int index;

		public IndexReference() {
			index = 0;
		}
	}

	public void printSegmentation() {
		System.out.print("\n");
		for (int i = 0; i < this.segmentationSuggestions.size(); i++) {
			segmentationSuggestions.get(i).printSegmentation(0);
		}
	}

	public ArrayList<Segment> getSegments(int segmentationLevel) {
		ArrayList<Segment> segments = new ArrayList<Segment>();
		ArrayList<SegmentationList> segmentationLists = null;
		switch (segmentationLevel) {
		case AASModel.MACRO_LEVEL:
			segmentationLists = this.getMacroSegmentationLists();
			break;
		case AASModel.MESO_LEVEL:
			segmentationLists = this.getMesoSegmentationLists();
			break;
		case AASModel.MICRO_LEVEL:
			segmentationLists = this.getMicroSegmentationLists();
			break;
		}
		if (segmentationLists != null && !segmentationLists.isEmpty()){
			for (SegmentationList sl : segmentationLists){
				for (SegmentationPart sp : sl){
					segments.add(new Segment(sp.getBegin(), sp.getEnd(), sp.getLabel(), sp.getColor()));
				}
			}
		}
		return segments;
	}
}
