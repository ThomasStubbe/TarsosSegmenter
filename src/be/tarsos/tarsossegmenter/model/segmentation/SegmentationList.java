/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.tarsos.tarsossegmenter.model.segmentation;

import be.tarsos.tarsossegmenter.gui.TarsosSegmenterGui;
import be.tarsos.tarsossegmenter.model.AASModel;

import java.awt.Color;
import java.util.Map.Entry;
import java.util.*;

import javax.swing.JOptionPane;

/**
 *
 * @author Thomas Stubbe
 */
public class SegmentationList extends ArrayList<SegmentationPart> {

    private HashMap<String, ArrayList<SegmentationPart>> labelMap;
    private HashMap<String, Color> colorMap;
    private float begin, end;
    private int segmentationLevel;
    private SegmentationPart parent;

    public SegmentationList(SegmentationPart parent, int segmentationLevel) {
        super();
        this.parent = parent;
        this.segmentationLevel = segmentationLevel;
        labelMap = new HashMap();
        colorMap = new HashMap();
    }

    public void editallEqualSegmentationParts(SegmentationPart sp) {
        //@TODO
    }

    @Override
    public void clear() {
        clearSubSegmentation();
        super.clear();
        labelMap.clear();
        colorMap.clear();
    }

    public void clearSubSegmentation() {
        for (int i = 0; i < this.size(); i++) {
            get(i).clearSubSegmentations();
        }
    }

    public float getBegin() {
        return begin;
    }

    public float getEnd() {
        return end;
    }

    public boolean insertSegmentationPart(float begin, float end, String label) throws Exception {
        //Collections.sort(this);
        if (end > begin && begin >= this.begin && end <= this.end) {
            int i = 0;
            while (i < this.size() && begin >= this.get(i).getEnd()) {
                i++;
            }
            if (i < this.size() && end <= this.get(i).getEnd()) { //juiste segment gevonden
                boolean switchLabels = false;
                if (begin > this.get(i).getBegin()) {
                    this.get(i).split(begin);
                    //Collections.sort(this);
                    i++;
                    this.get(i).setLabel(label);
                } else {
                    switchLabels = true;
                }
                if (end < this.get(i).getEnd()) {
                    this.get(i).split(end);
                    //Collections.sort(this);
                    if (switchLabels) {
                        this.get(i + 1).setLabel(this.get(i).getLabel());
                        this.get(i + 1).setColor(this.get(i).getColor());
                        this.get(i).setLabel(label);
                    }
                }
                return true;
            } else {
                throw new Exception("Cannot insert a segment which overlaps more than one other segment! Merge first!");
            }
        } else {
            throw new Exception("Cannot insert a segment which overlaps more than one other segment! Merge first!");
        }
    }

    public boolean insertSegmentationPart(SegmentationPart currentSP, SegmentationPart newSP) {
        //@TODO: subsegmentatie aanpassen!

        //newSP zijn begin valt binnen currentSP
        //3 opties:
        //huidig segment opgesplitst -> rechts ervan komt het nieuwe segment
        //huidig segment opgesplitst -> links ervan komt het nieuwe segment
        //huidig segment wordt in 3 gedeeld -> middelste deel is het nieuwe segment

        if (newSP.getBegin() > currentSP.getBegin()) {
            if (newSP.getEnd() < currentSP.getEnd()) { //optie 3
                SegmentationPart newSP2 = new SegmentationPart(newSP.getEnd(), currentSP.getEnd());
                currentSP.setEnd(newSP.getBegin());
                this.add(newSP2);
            } else if (newSP.getEnd() == currentSP.getEnd()) { //optie 1
                currentSP.setEnd(newSP.getBegin());
            }
        } else if (newSP.getBegin() == currentSP.getBegin()) {
            if (newSP.getEnd() < currentSP.getEnd()) { //optie 2
                currentSP.setBegin(newSP.getEnd());
            }
        } else {
            JOptionPane.showMessageDialog(TarsosSegmenterGui.getInstance(), "Ingevoerd segment kan niet tussengevoegd worden, gelieve mogelijke tijdstippen te nemen!", "Fout!", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        this.add(newSP);
        return true;
    }

    @Override
    public boolean add(SegmentationPart newSP) {
        //@TODO: labels -> map
        newSP.setIndexReference(AASModel.getInstance().getSegmentation().getSegmentationIndexReference(segmentationLevel + 1));
        boolean added = super.add(newSP);
        if (added) {
            newSP.setSegmentationContainer(this);
            Collections.sort(this);
            updateBeginEnd();
            addToLabelMap(newSP);
            
        }
        return added;
    }

    private void getColorForSegment(SegmentationPart sp) {
        Color color = Color.WHITE;
        if (sp.getLabel() != null && !sp.getLabel().equals(SegmentationPart.EMPTY_LABEL)) {
            if (colorMap.containsKey(sp.getLabel())) {
                sp.setColor(colorMap.get(sp.getLabel()));
            } else {
                if (this.segmentationLevel != AASModel.MACRO_LEVEL) {
                    Color newColor = this.parent.getColor();
                    for (int i = 0; i <= colorMap.size(); i++) {
                        newColor = new Color(Math.max(newColor.getRed() - 20, 128), Math.max(newColor.getGreen() - 20, 128),Math.max(newColor.getBlue() - 20, 128));
                    }
                    colorMap.put(sp.getLabel(), newColor);
                    sp.setColor(newColor);
                } else {
                    reconstructColors();
                }
            }
        } else {
            sp.setColor(color);
        }
    }

    public boolean hasSubSegmentation() {
        for (int i = 0; i < size(); i++) {
            if (get(i).hasSubSegmentation()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSubSubSegmentation() {
        for (int i = 0; i < size(); i++) {
            if (get(i).hasSubSegmentation()) {
                return get(i).getSubSegmentation().hasSubSegmentation();
            }
        }
        return false;
    }

    public void merge(SegmentationPart begin, SegmentationPart end) {
        Collections.sort(this);
        if (contains(begin) && contains(end)) {
            Stack<SegmentationPart> toMerge = new Stack();
            float newEnd = end.getEnd();
            int i = indexOf(end);
            while (!this.get(i).equals(begin)) {
                toMerge.add(this.remove(i));
                i--;
            }
            begin.setEnd(newEnd);
            while (!toMerge.empty()) {
                SegmentationPart sp = toMerge.pop();
                if (sp.hasSubSegmentation()) {
                    for (int j = 0; j < sp.getSubSegmentationSuggestions().size(); j++) {
                        if (j == begin.getSubSegmentationSuggestions().size()) {
                            begin.getSubSegmentationSuggestions().add(sp.getSubSegmentationSuggestions().get(j));
                        } else {
                            for (int k=0; k<sp.getSubSegmentationSuggestions().get(j).size(); k++){
                                begin.getSubSegmentationSuggestions().get(j).add(sp.getSubSegmentationSuggestions().get(j).get(k));
                            }
                        }
                        sp.getSubSegmentationSuggestions().get(j).reconstructColors();
                    }
                }
            }
        }
        reconstructColors();
    }

    public void labelChanged(SegmentationPart sp, String oldLabel) {
        if (oldLabel == null ? SegmentationPart.EMPTY_LABEL != null : !oldLabel.equals(SegmentationPart.EMPTY_LABEL)) {
            labelMap.get(oldLabel).remove(sp);
        }
        addToLabelMap(sp);
        reconstructColors();
    }

    private void addToLabelMap(SegmentationPart sp) {
        if (!sp.getLabel().isEmpty()) {
            if (!labelMap.containsKey(sp.getLabel())) {
                labelMap.put(sp.getLabel(), new ArrayList<SegmentationPart>());
            }
            labelMap.get(sp.getLabel()).add(sp);
            getColorForSegment(sp);
        } else {
            getColorForSegment(sp);
        }
    }

    @Override
    public SegmentationPart remove(int index) {
        SegmentationPart sp = this.get(index);
        if (this.remove(sp)) {
            if (sp.getLabel() != null && !sp.getLabel().equals(SegmentationPart.EMPTY_LABEL)) {
                labelMap.get(sp.getLabel()).remove(sp);
                if (labelMap.get(sp.getLabel()).isEmpty()) {
                    labelMap.remove(sp.getLabel());
                    reconstructColors();
                }
            }
            return sp;
        } else {
            return null;
        }
    }

    @Override
    public boolean remove(Object o) {
        if (!((SegmentationPart) o).getLabel().isEmpty()) {
            labelMap.get(((SegmentationPart) o).getLabel()).remove(((SegmentationPart) o));
        }
        boolean removed = super.remove(o);
        if (removed) {
            Collections.sort(this);
            updateBeginEnd();
            reconstructColors();
        }
        return removed;
    }

    public int getSegmentationLevel() {
        return segmentationLevel;
    }

    public void printSegmentation(int tabs) {
        printTabs(tabs);
        System.out.println("SegmentationList: ");
        printTabs(tabs);
        System.out.println("   SegmentationLevel: " + this.segmentationLevel);
        printTabs(tabs);
        System.out.println("   Begin: " + this.begin);
        printTabs(tabs);
        System.out.println("   End: " + this.end);
        printTabs(tabs);
        System.out.println("   Content: ");
        for (int i = 0; i < this.size(); i++) {
            get(i).print(tabs + 2);
        }
    }

    private void printTabs(int tabs) {
        for (int i = 0; i < tabs; i++) {
            System.out.print("   ");
        }
    }

    void updateBeginEnd() {
        if (size() > 0) {
            this.begin = this.get(0).getBegin();
            this.end = this.get(this.size() - 1).getEnd();
        } else {
            this.begin = 0;
            this.end = 0;
        }
    }

    protected void reconstructColors() {
        //reconstruct the colorMap
        colorMap.clear();
        int size = labelMap.size();
        int index = 0;
        Iterator it = labelMap.entrySet().iterator();
        
        while (it.hasNext()) {
            ArrayList<SegmentationPart> segments = (ArrayList<SegmentationPart>)((Entry)it.next()).getValue();
            for (int i = 0; i < segments.size(); i++) {
                if (segmentationLevel != AASModel.MACRO_LEVEL) {
                    this.getColorForSegment(segments.get(i));
                } else {
                    if (!colorMap.containsKey(segments.get(i).getLabel())) {
                        colorMap.put(segments.get(i).getLabel(), Color.getHSBColor(index / (float)(size), 0.2f, 1f));
                    }
                    segments.get(i).setColor(colorMap.get(segments.get(i).getLabel()));
                }
            }
            index++;
        }
    }

    public SegmentationPart getParent() {
        return parent;
    }

}
