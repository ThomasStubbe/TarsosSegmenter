/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.hogent.tarsos.tarsossegmenter.model.segmentation;

import be.hogent.tarsos.tarsossegmenter.model.segmentation.Segmentation.IndexReference;
import java.awt.Color;
import java.util.ArrayList;

/**
 *
 * @author Thomas Stubbe
 */
public class SegmentationPart implements Comparable<SegmentationPart> {

    private ArrayList<SegmentationList> subSegmentationSuggestions;
    private IndexReference indexRef;
    public static final String EMPTY_LABEL = "";
    private float begin, end; //tijd
    private String label;
    private String comment;
    private SegmentationList segmentationContainer;
    private Color color;
    private float match;

    public SegmentationPart(float begin, float end) {
        this.begin = be.hogent.tarsos.tarsossegmenter.util.math.Math.round(begin, 2);
        this.end = be.hogent.tarsos.tarsossegmenter.util.math.Math.round(end, 2);
        label = EMPTY_LABEL;
        color = Color.WHITE;
    }

    public SegmentationPart(float begin, float end, String label) {
        this(begin, end);
        this.label = label;
        //color = this.createInitialColor(label);
    }

    public SegmentationPart(float begin, float end, String label, String comment) {
        this(begin, end, label);
        this.comment = comment;
    }

    public SegmentationPart(float begin, float end, IndexReference indexRef) {
        this(begin, end);
        this.indexRef = indexRef;
        //subSegmentationSuggestions = new ArrayList();
    }

    public SegmentationPart(float begin, float end, String label, String comment, IndexReference indexRef) {
        this(begin, end, label, comment);
        this.indexRef = indexRef;
    }

    public SegmentationPart(float begin, float end, String label, String comment, IndexReference indexRef, float match) {
        this(begin, end, label, comment, indexRef);
        this.match = match;
    }

    public SegmentationPart(SegmentationPart sp) {
        this(sp.begin, sp.end, sp.label, sp.comment, sp.indexRef, sp.match);
    }

    public float getMatch() {
        return match;
    }

    public void setMatch(float match) {
        this.match = match;
    }

    public SegmentationList getSubSegmentation() {
        //@TODO: vanaf een bepaalde suggestie-niveau wordt enkel nog de eerste suggestie teruggegeven
        if (hasSubSegmentation()) {
            if (subSegmentationSuggestions.size() <= indexRef.index) {
                return subSegmentationSuggestions.get(0);
            } else {
                return subSegmentationSuggestions.get(indexRef.index);
            }
        } else {
            return null;
        }
    }

    void clearSubSegmentations() {
        if (hasSubSegmentation()) {
            for (int i = subSegmentationSuggestions.size() - 1; i > 0; i--) {
                subSegmentationSuggestions.get(i).clear();
                subSegmentationSuggestions.remove(i);
            }
            subSegmentationSuggestions.get(0).clear();
        }
        if (indexRef != null) {
            indexRef.index = 0;
        }
    }

    public boolean hasSubSegmentation() {
        if (subSegmentationSuggestions != null && subSegmentationSuggestions.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(SegmentationPart o) {
        if (end > o.end) {
            return 1;
        }
        if (end < o.end) {
            return -1;
        }
        return 0;
    }

    public float getBegin() {
        return begin;
    }

    public float getEnd() {
        return end;
    }

    public String getComment() {
        return comment;
    }

    public String getLabel() {
        return label;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        if (this.hasSubSegmentation()) {
            for (int i = 0; i < this.subSegmentationSuggestions.size(); i++) {
                subSegmentationSuggestions.get(i).reconstructColors();
            }
        }
    }

    public void setLabel(String label) {
        //@TODO: vragen of andere labels ook aangepast moeten worden
        String oldLabel = this.label;
        this.label = label;
        if (this.getContainer() != null) {
            this.getContainer().labelChanged(this, oldLabel);
        }
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setBeginEditPrevious(float begin) {
        //@TODO: lager niveau aanpassen
        SegmentationPart prev = getPrevious();
        if (prev != null && begin < end && begin > prev.getBegin()) {
            if (this.begin < begin) { //segment wordt verkleind -> subsegmentatieparts naar links verplaatsen
                prev.setEnd(begin);
                setBegin(begin);
                if (this.hasSubSegmentation()) {
                    for (int i = 0; i < this.getSubSegmentationSuggestions().size(); i++) {
                        for (int j = getSubSegmentationSuggestions().get(i).size() - 1; j >= 0; j--) {
                            SegmentationPart sp = getSubSegmentationSuggestions().get(i).get(j);
                            if (sp.getBegin() < begin) { //segment moet verplaatst of opgesplitst worden
                                sp.getContainer().remove(sp);
                                if (sp.getEnd() > begin) {
                                    sp.split(begin);
                                    j++;
                                }
                                if (!prev.hasSubSegmentation()) {
                                    prev.createSubSegmentationSuggestionList();
                                }
                                while (prev.getSubSegmentationSuggestions().size() <= i) {
                                    prev.getSubSegmentationSuggestions().add(new SegmentationList(prev, prev.getContainer().getSegmentationLevel()));
                                }
                                prev.getSubSegmentationSuggestions().get(i).add(sp);
                            }
                        }
                    }
                }
            } else if (this.begin > begin) { //segment wordt vergroot -> in ander segment afhandelen
                prev.setEndEditNext(begin);
            }
        }
    }

    public void setEndEditNext(float end) {
        //@TODO: lager niveau aanpassen -> oa splitten
        SegmentationPart next = getNext();
        if (next != null && end > begin && end < next.getEnd()) {
            if (this.end > end) { //segment wordt verkleind -> subsegmentatieparts naar links verplaatsen
                next.setBegin(end);
                setEnd(end);
                if (this.hasSubSegmentation()) {
                    for (int i = 0; i < this.getSubSegmentationSuggestions().size(); i++) {
                        for (int j = getSubSegmentationSuggestions().get(i).size() - 1; j >= 0; j--) {
                            SegmentationPart sp = getSubSegmentationSuggestions().get(i).get(j);
                            if (sp.getEnd() > end) { //segment moet verplaatst of opgesplitst worden
                                sp.getContainer().remove(sp);
                                if (sp.getBegin() < end) {
                                    sp.split(end);
                                    j++;
                                }
                                if (!next.hasSubSegmentation()) {
                                    next.createSubSegmentationSuggestionList();
                                }
                                while (next.getSubSegmentationSuggestions().size() <= i) {
                                    next.getSubSegmentationSuggestions().add(new SegmentationList(next, next.getContainer().getSegmentationLevel()));
                                }
                                next.getSubSegmentationSuggestions().get(i).add(sp);
                            }
                        }
                    }
                }
            } else if (this.end < end) { //segment wordt vergroot -> in ander segment afhandelen
                next.setBeginEditPrevious(end);
            }
        }

//        
//        SegmentationPart next = getNext();
//        if (next != null) {
//            next.setBegin(end);
//            setEnd(end);
//        }
    }

    //Werkt niet op meso-niveau
    //Buttom-up splitting 
    public void split(float time) {
        time = be.hogent.tarsos.tarsossegmenter.util.math.Math.round((float) time, 2);
        if (time > begin && time < end) {
            if (hasSubSegmentation()) {
                //For every possible subSegmentation -> search for conflicting segmentationPart and split it
                for (int i = 0; i < subSegmentationSuggestions.size(); i++) {
                    for (int j = subSegmentationSuggestions.get(i).size() - 1; j >= 0; j--) {
                        if (subSegmentationSuggestions.get(i).get(j).getBegin() < time && subSegmentationSuggestions.get(i).get(j).getEnd() > time) {
                            subSegmentationSuggestions.get(i).get(j).split(time);
                            //Probleem: indien er meerdere suggesties zijn -> enkel de laatste uit de iteratie wordt effectief opgesplitst
                            //Once the conflicting segmentationPart has been found and splitted -> ignore the rest of the segmentation
                            j = -1;
                        }
                    }
                }
            }
            //Split the segmentationPart in two, remove the original segmentationPart from his parent, readd it (fixes begin and end of the list) and add the new segementationPart
            SegmentationList container = this.getContainer();
            container.remove(this);
            SegmentationPart newSP = new SegmentationPart(time, this.getEnd());
            this.setEnd(time);
            container.add(this);
            container.add(newSP);

            //Reorganise the segmentationParts of the sublevel -> all parts > then time -> added to the new segmentationPart
            if (hasSubSegmentation()) {
                newSP.createSubSegmentationSuggestionList();
                for (int i = 0; i < subSegmentationSuggestions.size(); i++) {
                    if (newSP.getSubSegmentationSuggestions().size() <= i) {
                        newSP.getSubSegmentationSuggestions().add(new SegmentationList(newSP, subSegmentationSuggestions.get(i).getSegmentationLevel()));
                    }
                    for (int j = subSegmentationSuggestions.get(i).size() - 1; j > 0; j--) {
                        SegmentationPart sp = subSegmentationSuggestions.get(i).get(j);
                        if (sp.getBegin() >= time) {
                            subSegmentationSuggestions.get(i).remove(sp);
                            newSP.getSubSegmentationSuggestions().get(i).add(sp);
                        }
                    }
                }
            }
        }
    }

    public void setSegmentationContainer(SegmentationList segmentationContainer) {
        this.segmentationContainer = segmentationContainer;
    }

    public SegmentationList getContainer() {
        return segmentationContainer;
    }

    public SegmentationPart getNext() {
        int index = getContainer().indexOf(this);
        if (index < getContainer().size() - 1) {
            return this.getContainer().get(index + 1);
        }
        return null;
    }

    public SegmentationPart getPrevious() {
        int index = getContainer().indexOf(this);
        if (index > 0) {
            return getContainer().get(index);
        }
        return null;
    }

    protected void setBegin(float begin) {
        this.begin = be.hogent.tarsos.tarsossegmenter.util.math.Math.round(begin, 2);
    }

    protected void setEnd(float end) {
        this.end = be.hogent.tarsos.tarsossegmenter.util.math.Math.round(end, 2);
    }

    public IndexReference getIndexReference() {
        return indexRef;
    }

    protected void setIndexReference(IndexReference indexRef) {
        this.indexRef = indexRef;
    }

    public ArrayList<SegmentationList> getSubSegmentationSuggestions() {
        if (hasSubSegmentation()) {
            return subSegmentationSuggestions;
        } else {
            return null;
        }
    }

    public void createSubSegmentationSuggestionList() {
        if (subSegmentationSuggestions == null) {
            this.subSegmentationSuggestions = new ArrayList();
            this.subSegmentationSuggestions.add(new SegmentationList(this, this.getContainer().getSegmentationLevel() + 1));
        } else if (subSegmentationSuggestions.isEmpty()) {
            this.subSegmentationSuggestions.add(new SegmentationList(this, this.getContainer().getSegmentationLevel() + 1));
        }
    }

    protected void print(int tabs) {
        printTabs(tabs);
        if (!this.label.isEmpty()) {
            System.out.print(this.label + ": ");
        }
        System.out.println(this.begin + " - " + this.end);
        if (hasSubSegmentation()) {
            for (int i = 0; i < subSegmentationSuggestions.size(); i++) {
                subSegmentationSuggestions.get(i).printSegmentation(tabs + 1);
            }
        }
    }

    private void printTabs(int tabs) {
        for (int i = 0; i < tabs; i++) {
            System.out.print("   ");
        }
    }
    /**
     * Returns a color for a label
     *
     * @param c The label of the segmentationpart that needs a color. The color
     * is based on the character
     */
//    private static Color createInitialColor(String label) {
//        if (colors == null) {
//            constructColors();
//        }
//        String labelUpper = label.toUpperCase();
//        if (!labelUpper.isEmpty()) {
//            char c = ' ';
//            int n = 0;
//            if (labelUpper.length() > 0) {
//                if (labelUpper.charAt(0) >= 'A' && labelUpper.charAt(0) <= 'Z') {
//                    c = 'A';
//                }
//                for (int i = 0; i < Math.min(labelUpper.length(), 3); i++) {
//                    if (labelUpper.charAt(i) >= 'A' && labelUpper.charAt(i) <= 'Z') {
//                        c += labelUpper.charAt(i);
//                    } else if (labelUpper.charAt(i) >= '1' && labelUpper.charAt(i) <= '9') {
//                        n += Integer.parseInt(String.valueOf(labelUpper.charAt(i)));
//                    }
//                }
//                //c %= colors.length;
//            } else {
//                c = label.charAt(0);
//            }
//            Color result;
//            if (c != ' ') {
//                result = colors[c % colors.length];
//            } else {
//                result = Color.white;
//            }
//            for (int i = 0; i < n; i++) {
//                result = new Color(result.getRed() - 20, result.getGreen() - 20, result.getBlue() - 20);
//            }
//            return result;
//        }
//        return Color.white;
//    }
//    private static void constructColors() {
//        colors = new Color[4];
//        colors[0] = new Color(255, 255, 220);
//        colors[1] = new Color(200, 235, 255);
//        colors[2] = new Color(220, 255, 220);
//        colors[3] = new Color(255, 220, 210);
////        colors[4] = new Color(200, 255, 220);
////        colors[5] = new Color(180, 200, 240);
////        colors[6] = new Color(220, 180, 200);
////        colors[7] = new Color(255, 200, 255);
////        colors[8] = new Color(200, 180, 235);
////        colors[9] = new Color(225, 200, 255);
////        colors[10] = new Color(180, 250, 200);
//
//    }
//    public void updateColor() {
//        if (this.getContainer()== null || this.getContainer().getParent() == null) {
//            if (!this.label.isEmpty()) {
//                color = SegmentationPart.createInitialColor(label);
//            }
//        }
//    }
}
