/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.hogent.tarsos.tarsossegmenter.model.structure;

import be.hogent.tarsos.tarsossegmenter.model.AASModel;
import be.hogent.tarsos.tarsossegmenter.model.preprocessing.PreProcesses;
import be.hogent.tarsos.tarsossegmenter.model.segmentation.Segmentation;
import be.hogent.tarsos.tarsossegmenter.model.segmentation.SegmentationList;
import be.hogent.tarsos.tarsossegmenter.model.segmentation.SegmentationPart;
import be.hogent.tarsos.tarsossegmenter.util.configuration.ConfKey;
import be.hogent.tarsos.tarsossegmenter.util.configuration.Configuration;
import java.util.*;

/**
 *
 * @author Thomas Stubbe
 */
public class StructureDetection {

    private final static int STATUS_NONE = 0;
    private final static int STATUS_EQUAL = 1;
    private final static int STATUS_PART_OF = 2;
    private final static int STATUS_CONFLICT = 3;
    private final static int STATUS_CONTAINING = 4;
    private float binaryTreshholdCte;
    private int amountOfFrames;
    private float frameDuration;
    private float songDuration;
    float[][] selfSimilarityMatrix;
    private Segmentation segmentation;
    private ArrayList<SegmentationPart> allMacroSegmentationParts;
    private ArrayList<SegmentationPart> allMesoSegmentationParts;
    private ArrayList<SegmentationPart> allMicroSegmentationParts;
    private float range;
    private float binaryTreshhold;
    private int currentSegmentationLevel;
    //In general for the CalculationMatchThreads: prevents coping the value
    private float threshold;
    private float minLength;

    public StructureDetection(float songDuration, float[][] selfSimilarityMatrix, float range) {
        this.range = range;
        binaryTreshholdCte = (float) Configuration.getDouble(ConfKey.binary_treshold);
        this.amountOfFrames = selfSimilarityMatrix.length;
        this.songDuration = songDuration;
        this.selfSimilarityMatrix = selfSimilarityMatrix;
        this.binaryTreshhold = range * binaryTreshholdCte;
        this.segmentation = AASModel.getInstance().getSegmentation();
        allMacroSegmentationParts = new ArrayList();
        allMesoSegmentationParts = new ArrayList();
        allMicroSegmentationParts = new ArrayList();
        frameDuration = songDuration / amountOfFrames;
    }

    public void run() {
        currentSegmentationLevel = AASModel.MACRO_LEVEL;
        if (segmentation.getSegmentationPoints(AASModel.MACRO_LEVEL).size() > 0) {
            allMacroSegmentationParts.clear();
            calculateAllPossibleSegments(segmentation.getSegmentationPoints(AASModel.MACRO_LEVEL), allMacroSegmentationParts);
            findStructures(allMacroSegmentationParts, null);
            if (Configuration.getBoolean(ConfKey.enable_meso)) {
                for (int i = 0; i < segmentation.getAmountOfMacroSuggestions(); i++) {
                    SegmentationList macroSegmentationList = segmentation.getMacroSuggestions().get(i);
                    for (int j = 0; j < macroSegmentationList.size(); j++) {
                        currentSegmentationLevel = AASModel.MESO_LEVEL;
                        allMesoSegmentationParts.clear();
                        calculateAllPossibleSegments(segmentation.getSegmentationPoints(AASModel.MESO_LEVEL), allMesoSegmentationParts, macroSegmentationList.get(j).getBegin(), macroSegmentationList.get(j).getEnd());
                        macroSegmentationList.get(j).createSubSegmentationSuggestionList();
                        findStructures(allMesoSegmentationParts, macroSegmentationList.get(j));
                        if (Configuration.getBoolean(ConfKey.enable_micro)) {
                            currentSegmentationLevel = AASModel.MICRO_LEVEL;
                            ArrayList<SegmentationList> mesoSegmentationSuggestionsLists = macroSegmentationList.get(j).getSubSegmentationSuggestions();
                            for (int k = 0; k < mesoSegmentationSuggestionsLists.size(); k++) {
                                SegmentationList mesoSegmentationList = mesoSegmentationSuggestionsLists.get(k);
                                for (int l = 0; l < mesoSegmentationList.size(); l++) {
                                    allMicroSegmentationParts.clear();
                                    calculateAllPossibleSegments(segmentation.getSegmentationPoints(AASModel.MICRO_LEVEL), allMicroSegmentationParts, mesoSegmentationList.get(l).getBegin(), mesoSegmentationList.get(l).getEnd());
                                    mesoSegmentationList.get(l).createSubSegmentationSuggestionList();
                                    findStructures(allMicroSegmentationParts, mesoSegmentationList.get(l));
                                }
                            }
                        }
                    }

                    //@TODO: meso niveau analyseren met elkaar per gelijk labels op macro-niveau
                }
            }
        }
        cleanMemory();
        //segmentation.printSegmentation();
    }

    private void findStructures(ArrayList<SegmentationPart> allSegmentationParts, SegmentationPart parent) {
        if (allSegmentationParts.size() > 0) {
            PriorityQueue<SegmentationMatchItem> queue = new PriorityQueue(allSegmentationParts.size() * allSegmentationParts.size(), new SegmentationMatchItem());
            int start = 0;
            int end = allSegmentationParts.size();
            switch (this.currentSegmentationLevel) {
                case (AASModel.MACRO_LEVEL):
                    minLength = 6f;
                    threshold = 0.09f;
                    if (Configuration.getBoolean(ConfKey.ignore_first_segment)) {
                        start++;
                    }
                    if (Configuration.getBoolean(ConfKey.ignore_last_segment)) {
                        end--;
                    }
                    break;
                case (AASModel.MESO_LEVEL):
                    minLength = 3f;
                    threshold = 0.15f;
                    break;
                case (AASModel.MICRO_LEVEL):
                    minLength = 1f;
                    threshold = 0.2f;
                    break;
            }
            //Etnische muziek: i=1 en allSegmentationParts.size()-1 voor intro en outro niet te vergelijken
            HashSet<MatchCalculationThread> threadSet = new HashSet();
            for (int i = start; i < end; i++) {
                for (int j = i + 1; j < end; j++) {
                    if (allSegmentationParts.get(i).getEnd() - allSegmentationParts.get(i).getBegin() > minLength && allSegmentationParts.get(i).getEnd() <= allSegmentationParts.get(j).getBegin() && Math.abs(1 - (allSegmentationParts.get(i).getEnd() - allSegmentationParts.get(i).getBegin()) / (allSegmentationParts.get(j).getEnd() - allSegmentationParts.get(j).getBegin())) < threshold) {
                        MatchCalculationThread matchCalculationThread = new MatchCalculationThread(allSegmentationParts.get(i), allSegmentationParts.get(j), queue);
                        matchCalculationThread.start();
                        threadSet.add(matchCalculationThread);
                    }
                }
            }
            Iterator it = threadSet.iterator();
            while (it.hasNext()) {
                try {
                    ((Thread) it.next()).join();
                } catch (InterruptedException e) {
                }
            }
            searchEqualParts(queue, parent);
            ArrayList<SegmentationList> top5Segmentations;
            if (parent != null) {
                if (!parent.hasSubSegmentation()) {
                    parent.createSubSegmentationSuggestionList();
                }
                top5Segmentations = parent.getSubSegmentationSuggestions();
            } else {
                top5Segmentations = segmentation.getMacroSuggestions();
            }
            //De overige segmentatiedelen die geen match hebben toevoegen aan de segmentatie
            //Alle suggesties overlopen
            if (top5Segmentations.isEmpty()) {
                top5Segmentations.add(new SegmentationList(parent, currentSegmentationLevel));
            }

            for (int i = 0; i < top5Segmentations.size(); i++) {
                //Alle segmentatiePunten overlopen
                for (int j = 0; j < allSegmentationParts.size(); j++) {
                    //Indien de suggestie het segmentatiepunt nog niet bevat
                    if (!top5Segmentations.get(i).contains(allSegmentationParts.get(j))) {
                        boolean add = true; //toevoegen: standaard = true
                        //alle andere segmentatiepunten uit de suggestie overlopen
                        for (int k = 0; k < top5Segmentations.get(i).size(); k++) {
                            //Indien er een ander segmentatiepunt een conflict geeft (dus indien het een deel van, gelijk of een stuk overlapt) -> niet toevoegen
                            if (checkSegmentation(allSegmentationParts.get(j), top5Segmentations.get(i).get(k)) != STATUS_NONE) {
                                add = false;
                            }
                        }
                        if (add) {
                            top5Segmentations.get(i).add(new SegmentationPart(allSegmentationParts.get(j)));
                        }
                    }
                }
            }
        }
    }

    private class MatchCalculationThread extends Thread {

        SegmentationPart firstSP, secondSP;
        PriorityQueue<SegmentationMatchItem> queue;

        protected MatchCalculationThread(SegmentationPart firstSP, SegmentationPart secondSP, PriorityQueue<SegmentationMatchItem> queue) {
            this.firstSP = firstSP;
            this.secondSP = secondSP;
            this.queue = queue;
        }

        @Override
        public void run() {
            int firstStartFrame = Math.round(firstSP.getBegin() / frameDuration);
            int secondStartFrame = Math.round(secondSP.getBegin() / frameDuration);
            int durationInFrames = (int) ((firstSP.getEnd() - firstSP.getBegin()) / frameDuration);
            durationInFrames = (Math.min(amountOfFrames, secondStartFrame + durationInFrames) - secondStartFrame);
            float match = calculateMatch(firstStartFrame, secondStartFrame, durationInFrames);
            if (match > 0.4f) {
                SegmentationMatchItem queueItem = new SegmentationMatchItem();
                queueItem.firstSP = firstSP;
                queueItem.secondSP = secondSP;
                queueItem.match = match;
                queueItem.durationInFrames = durationInFrames;
                queue.add(queueItem);
            }
        }
    }

    private void calculateAllPossibleSegments(ArrayList<Float> segmentationPoints, ArrayList<SegmentationPart> allSegmentationParts, float begin, float end) {
        for (int i = 0; i < segmentationPoints.size() - 1; i++) {
            if (segmentationPoints.get(i) >= begin && segmentationPoints.get(i + 1) <= end) {
                SegmentationPart sp = new SegmentationPart(segmentationPoints.get(i), segmentationPoints.get(i + 1));
                allSegmentationParts.add(sp);
                for (int j = i + 2; j < segmentationPoints.size(); j++) {
                    if (segmentationPoints.get(j) <= end && segmentationPoints.get(j) - sp.getBegin() <= (end - begin) / 2.2f) {
                        SegmentationPart sp2 = new SegmentationPart(sp.getBegin(), segmentationPoints.get(j));
                        allSegmentationParts.add(sp2);
                    } else {
                        break;
                    }
                }
            } else if (segmentationPoints.get(i + 1) > end) {
                break;
            }
        }
        Collections.sort(allSegmentationParts);
    }

    private void calculateAllPossibleSegments(ArrayList<Float> segmentationPoints, ArrayList<SegmentationPart> allSegmentationParts) {
        int startIndex = 0;
        int endIndex = segmentationPoints.size() - 1;
        if (segmentationPoints.get(0) == 0f && this.currentSegmentationLevel == AASModel.MACRO_LEVEL) {
            if (Configuration.getBoolean(ConfKey.ignore_first_segment)) {
                startIndex++;
                allSegmentationParts.add(new SegmentationPart(segmentationPoints.get(0), segmentationPoints.get(1)));
            }
            if (Configuration.getBoolean(ConfKey.ignore_last_segment)) {
                endIndex--;
                allSegmentationParts.add(new SegmentationPart(segmentationPoints.get(endIndex), segmentationPoints.get(endIndex + 1)));
            }
        }
        calculateAllPossibleSegments(segmentationPoints, allSegmentationParts, segmentationPoints.get(startIndex), segmentationPoints.get(endIndex));
    }
    //       
    //       

    public void preProcessing() {
        if (Configuration.getBoolean(ConfKey.enable_white_area_reducement)) {
            PreProcesses.whiteAreasToDiagonals(selfSimilarityMatrix, range);
        }
        if (Configuration.getBoolean(ConfKey.enable_line_detection)) {

            range = PreProcesses.diagonalEdgeDetection(selfSimilarityMatrix, range);
            range = PreProcesses.sharpen(selfSimilarityMatrix);

            binaryTreshhold = range * binaryTreshholdCte;
        }
        if (Configuration.getBoolean(ConfKey.enable_binary)) {
            range = PreProcesses.makeBinary(selfSimilarityMatrix, binaryTreshhold, range);
            PreProcesses.dilate(selfSimilarityMatrix, range);
        }
    }

    private float calculateMatch(int firstStartFrame, int secondStartFrame, int durationInFrames) {
        float match = 0;
        float toleranceInSec = 0;
        int toleranceInFrames;
        switch (currentSegmentationLevel) {
            case (AASModel.MACRO_LEVEL):
                toleranceInSec = 1.5f;
                break;
            case (AASModel.MESO_LEVEL):
                toleranceInSec = 1f;
                break;
            case (AASModel.MICRO_LEVEL):
                toleranceInSec = 0.5f;
                break;
        }
        toleranceInFrames = Math.round(AASModel.getInstance().getSampleRate() / Configuration.getInt(ConfKey.framesize) * toleranceInSec);

        //Berekeningen voor diagonalen waarbij het X-startpunt kan variëren
        for (int startpoint = 0; startpoint <= toleranceInFrames; startpoint++) {

            boolean spiegel = false;
            if (secondStartFrame < firstStartFrame + startpoint) {
                spiegel = true;
            }

            boolean spiegel2 = false;
            if (secondStartFrame + startpoint < firstStartFrame) {
                spiegel2 = true;
            }

            float temp = 0;
            float temp2 = 0;
            for (int i = 0; i < durationInFrames; i++) {

                //Berekening speling langs X-as
//                if (firstStartFrame + startpoint + i > 0 && firstStartFrame + startpoint + i < selfSimilarityMatrix.length) {
                if (firstStartFrame + startpoint + i < selfSimilarityMatrix.length) {
                    if (spiegel) {
                        temp += (selfSimilarityMatrix[firstStartFrame + startpoint + i][secondStartFrame + i] / range);
                    } else {
                        temp += (selfSimilarityMatrix[secondStartFrame + i][firstStartFrame + startpoint + i] / range);
                    }
                }
                //Berekening speling langs Y-as
//                if (secondStartFrame + startpoint + i > 0 && secondStartFrame + startpoint + i < selfSimilarityMatrix.length) {
                if (startpoint != 0) {
                    if (secondStartFrame + startpoint + i < selfSimilarityMatrix.length) {
                        if (spiegel2) {
                            temp2 += (selfSimilarityMatrix[firstStartFrame + i][secondStartFrame + startpoint + i] / range);
                        } else {
                            temp2 += (selfSimilarityMatrix[secondStartFrame + startpoint + i][firstStartFrame + i] / range);
                        }
                    }
                }
            }
            temp /= (float) durationInFrames;
            temp2 /= (float) durationInFrames;
            temp = Math.max(temp, temp2);
            if (temp > match) {
                match = temp;
            }
        }
        return match;
    }

    private void searchEqualParts(PriorityQueue<SegmentationMatchItem> queue, SegmentationPart parent) {
        SegmentationTree segmentationTree = new SegmentationTree();
        while (!queue.isEmpty()) {
            SegmentationMatchItem temp = queue.poll();
            segmentationTree.addSegmentationMatchItemToSuggestions(temp);
        }
        segmentationTree.setTop5Segmentations(parent);
        ArrayList<SegmentationList> top5Segmentations;
        if (parent != null) {
            top5Segmentations = parent.getSubSegmentationSuggestions();
        } else {
            top5Segmentations = segmentation.getMacroSuggestions();
        }

        //Labels in orde brengen
        if (top5Segmentations != null) { //Enkel als er suggesties zijn
            for (int i = 0; i < top5Segmentations.size(); i++) {
                SegmentationList segmentationSuggestion = top5Segmentations.get(i);
                Collections.sort(segmentationSuggestion);

                Map<Character, Character> changeLabelMap = new HashMap<>();
                String parentLabel = "";
                char currentLabel = 'A';
                if (currentSegmentationLevel != AASModel.MACRO_LEVEL) {
                    parentLabel = segmentationSuggestion.getParent().getLabel();
                    currentLabel = '1';
                }

                for (int j = 0; j < segmentationSuggestion.size(); j++) {
                    SegmentationPart sp = segmentationSuggestion.get(j);
                    if (!sp.getLabel().isEmpty()) {
                        if (changeLabelMap.containsKey(sp.getLabel().charAt(0))) {
                            String label = parentLabel; 
                            if (currentSegmentationLevel == AASModel.MICRO_LEVEL){
                                label += ".";
                            }
                            label += String.valueOf(changeLabelMap.get(sp.getLabel().charAt(0)));
                            sp.setLabel(label);
                        } else {
                            changeLabelMap.put(sp.getLabel().charAt(0), currentLabel);
                            String label = parentLabel; 
                            if (currentSegmentationLevel == AASModel.MICRO_LEVEL){
                                label += ".";
                            }
                            label += String.valueOf(currentLabel);
                            sp.setLabel(label);
                            currentLabel++;
                        }
                    }
                }
            }
        }
    }

    public void cleanMemory() {
        selfSimilarityMatrix = null;
        allMacroSegmentationParts = null;
        allMesoSegmentationParts = null;
        allMicroSegmentationParts = null;
        segmentation.clearAllSegmentationPoints();
        System.gc();
    }

    private static int checkSegmentation(SegmentationPart oldSP, SegmentationPart newSP) {
        //apart voor beide segmenten bekijken en conclusie op basis van statussen nemen
        if (oldSP.equals(newSP) || (oldSP.getBegin() == newSP.getBegin() && oldSP.getEnd() == newSP.getEnd())) { //Bestaat al
            return STATUS_EQUAL;
        }
        if (newSP.getBegin() < oldSP.getBegin() && newSP.getEnd() > oldSP.getBegin() || newSP.getBegin() < oldSP.getEnd() && newSP.getEnd() > oldSP.getEnd()) { //Conflict
            return STATUS_CONFLICT;
        }
        if (newSP.getBegin() >= oldSP.getBegin() && newSP.getEnd() <= oldSP.getEnd()) { //Deel van Segment of gelijk qua tijden
            return STATUS_PART_OF;
        }
        if (newSP.getBegin() <= oldSP.getBegin() && newSP.getEnd() >= oldSP.getEnd()) { //bevat segment of gelijk qua tijden
            return STATUS_CONTAINING;
        }
        return STATUS_NONE;


    }

    private class SegmentationMatchItem implements Comparator<SegmentationMatchItem> {

        public float match;
        public SegmentationPart firstSP;
        public SegmentationPart secondSP;
        public int durationInFrames;

        @Override
        public int compare(SegmentationMatchItem f1, SegmentationMatchItem f2) {
            if (f1.durationInFrames*f1.match < f2.durationInFrames*f2.match) {
                return 1;
            }
            if (f1.durationInFrames*f1.match > f2.durationInFrames*f2.match) {
                return -1;
            }
            return 0;
        }
    }

    private class SegmentationEntry {

        protected char label;
        protected float match;
        protected SegmentationPart segmentationPart;

        protected SegmentationEntry(char label, float match, SegmentationPart segmentationPart) {
            this.label = label;
            this.match = match;
            this.segmentationPart = segmentationPart;
        }
    }

    private class SegmentationEntryList extends ArrayList<SegmentationEntry> {

        private char label = 'A';
        private float score;

        public void incrementLabel() {
            label++;
        }

        public char getLabel() {
            return label;
        }

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }

        protected void setLabel(char label) {
            this.label = label;
        }
    }

    private class SegmentationTree {

        //Bij None -> toevoegen
        //Bij 1 equal -> beide equal maken als match hoog genoeg is
        //Bij 2 equal -> Match updaten
        //Al de rest -> Nieuwe segmentatie sugestie
        private ArrayList<SegmentationEntryList> segmentationSuggestions;

        public SegmentationTree() {
            segmentationSuggestions = new ArrayList();

        }

        //Worden toegevoegd in dalende (niet stijgende) volgorde van lengte
        public void addSegmentationMatchItemToSuggestions(SegmentationMatchItem smi) {

            boolean createNewList = true; //Werd het toegevoegd aan een of andere lijst?
            Stack<SegmentationEntryList> newListStack = new Stack();
            for (int i = 0; i < segmentationSuggestions.size(); i++) {
                SegmentationEntryList list = segmentationSuggestions.get(i);
                //SegmentationMatchItem bestaat uit 2 SegmentationPart's -> Deze 2 Part's worden geprobeerd aan elke mogelijk segmentatieSuggestie te worden toegevoegd.


                int statusFirstSP = STATUS_NONE;
                int statusSecondSP = STATUS_NONE;
                Iterator<SegmentationEntry> it = list.iterator();

                SegmentationEntry relationFirst = null;
                SegmentationEntry relationSecond = null;
                //6 relevante gevallen:
                //een segment overlapt voor een deel met een ander segment (nieuwBegin < oudBegin && nieuwEind > nieuwBegin || nieuwBegin > oudBegin && nieuwEind > oudEind) -> Conflict
                //Beide segmenten zijn deel van een ander segment (nieuwBegin > oudBegin && nieuwEind < oudEind) -> toevoegen aan hun segmentationTree's + nieuwe set?
                //1 segment bestaat al en ander is deel van segment -> nieuwe set waarin ze uit elkaar getrokken worden + toevoegen aan oude set met nieuw label
                //1 segment bestaat al en ander overlapt niet -> label matchen met bestaand segment + nieuwe set met nieuw label (evt treshhold op match voor beslissing)
                //1 segment is deel van ander segment en ander overlapt niet -> nieuwe set waarin ze uit elkaar getrokken worden + toevoegen aan oude set met nieuw label
                //Beide segmenten overlappen niet met ander segment (geen van bovenstaande gevallen) -> entry aanmaken voor beide met nieuwe label
                //IRRELEVANT: Beide segmenten bestaan al (smi.segmentationPart.equals(currentSE.segmentationPart) -> entry aanmaken met zelfde label als match hoog genoeg is of nieuwe set
                //Om aan overeenkomstig of ouder segment te komen

                //while (it.hasNext() && statusFirstSP != STATUS_CONFLICT && statusSecondSP != STATUS_CONFLICT && (statusFirstSP == STATUS_NONE || statusSecondSP == STATUS_NONE)) {

                while (it.hasNext() && (statusFirstSP == STATUS_NONE || statusFirstSP == STATUS_EQUAL) && (statusSecondSP == STATUS_NONE || statusSecondSP == STATUS_EQUAL)) {
                    SegmentationEntry currentSE = it.next();
                    if (statusFirstSP == STATUS_NONE) {
                        relationFirst = currentSE;
                        statusFirstSP = checkSegmentation(currentSE.segmentationPart, smi.firstSP);
                    }

                    if (statusSecondSP == STATUS_NONE) {
                        relationSecond = currentSE;
                        statusSecondSP = checkSegmentation(currentSE.segmentationPart, smi.secondSP);
                    }

                }

                if ((statusFirstSP == STATUS_NONE || statusFirstSP == STATUS_EQUAL) && (statusSecondSP == STATUS_NONE || statusSecondSP == STATUS_EQUAL)) {
                    if (statusFirstSP == STATUS_NONE) {
                        SegmentationPart firstSP = new SegmentationPart(smi.firstSP);
                        if (statusSecondSP == STATUS_NONE) { //Beide kunnen probleemloos toegevoegd worden + kopie maken
                            SegmentationEntryList newList = new SegmentationEntryList();
                            newList.setLabel(list.getLabel());
                            for (int j = 0; j < list.size(); j++) {
                                newList.add(new SegmentationEntry(list.get(j).label, list.get(j).match, new SegmentationPart(list.get(j).segmentationPart)));
                            }
                            char label = newList.getLabel();
                            SegmentationPart secondSP = new SegmentationPart(smi.secondSP);
                            newList.add(new SegmentationEntry(label, smi.match, firstSP));
                            newList.add(new SegmentationEntry(label, smi.match, secondSP));
                            firstSP.setMatch(smi.match);
                            secondSP.setMatch(smi.match);
                            newList.incrementLabel();
                            createNewList = false;
                            newListStack.push(newList);
                        } else if (statusSecondSP == STATUS_EQUAL) {
                            if (smi.match < 0.8 * relationSecond.match) { //indien de match te laag is tov de andere twee -> nieuwe lijst en beide suggereren
                                SegmentationEntryList newList = new SegmentationEntryList();
                                newList.setLabel(list.getLabel());
                                for (int j = 0; j < list.size(); j++) {
                                    newList.add(new SegmentationEntry(list.get(j).label, list.get(j).match, new SegmentationPart(list.get(j).segmentationPart)));
                                    if (checkSegmentation(newList.get(j).segmentationPart, smi.secondSP) == STATUS_EQUAL) {
                                        relationSecond = newList.get(j);
                                    }
                                }
                                newList.add(new SegmentationEntry(relationSecond.label, smi.match, firstSP)); //eerste toevoegen met zelfde label als bestaand segment (als match hoog genoeg) @TODO: match aanpassen?
                                firstSP.setMatch(smi.match);
                                relationSecond.match = (relationSecond.match + smi.match) / 2;
                                relationSecond.segmentationPart.setMatch(relationSecond.match);
                                newListStack.push(newList);
                            } else {
                                list.add(new SegmentationEntry(relationSecond.label, smi.match, firstSP)); //eerste toevoegen met zelfde label als bestaand segment (als match hoog genoeg) @TODO: match aanpassen?
                                firstSP.setMatch(smi.match);
                                relationSecond.match = (relationSecond.match + smi.match) / 2;
                                relationSecond.segmentationPart.setMatch(relationSecond.match);

                            }
                            createNewList = false;
                        }
                    } else if (statusFirstSP == STATUS_EQUAL) {
                        if (statusSecondSP == STATUS_NONE) {
                            SegmentationPart secondSP = new SegmentationPart(smi.secondSP);
                            if (smi.match < 0.8 * relationSecond.match) { //indien de match te laag is tov de andere twee -> nieuwe lijst en beide suggereren
                                SegmentationEntryList newList = new SegmentationEntryList();
                                newList.setLabel(list.getLabel());
                                for (int j = 0; j < list.size(); j++) {
                                    newList.add(new SegmentationEntry(list.get(j).label, list.get(j).match, new SegmentationPart(list.get(j).segmentationPart)));
                                    if (checkSegmentation(newList.get(j).segmentationPart, smi.firstSP) == STATUS_EQUAL) {
                                        relationFirst = newList.get(j);
                                    }
                                }
                                newList.add(new SegmentationEntry(relationFirst.label, smi.match, secondSP)); //eerste toevoegen met zelfde label als bestaand segment (als match hoog genoeg) -> match van andere items moet aangepast worden?
                                secondSP.setMatch(smi.match);
                                relationFirst.match = (relationFirst.match + smi.match) / 2;
                                relationFirst.segmentationPart.setMatch(relationFirst.match);
                                //relationFirst.segmentationPart.setComment(relationFirst.segmentationPart.getComment() + "<- Updated to: " + relationFirst.match + " - Match with " + smi.secondSP.getBegin() + ": " + (int) (smi.match * 100));
                                newListStack.push(newList);
                            } else {
                                list.add(new SegmentationEntry(relationFirst.label, smi.match, secondSP)); //eerste toevoegen met zelfde label als bestaand segment (als match hoog genoeg) -> match van andere items moet aangepast worden?
                                secondSP.setMatch(smi.match);
                                relationFirst.match = (relationFirst.match + smi.match) / 2;
                                relationFirst.segmentationPart.setMatch(relationFirst.match);
                            }
                            createNewList = false;
                        } else if (statusSecondSP == STATUS_EQUAL) { //match should be updated
                            if (relationFirst.label == relationSecond.label){
                                relationFirst.match = (relationFirst.match + smi.match) / 2;
                                relationSecond.match = (relationSecond.match + smi.match) / 2;
                                relationSecond.segmentationPart.setMatch(relationSecond.match);
                                relationFirst.segmentationPart.setMatch(relationFirst.match);
                                createNewList = false;
                            }
                        }
                    }
                }
            }
            if (createNewList) {
                SegmentationEntryList list = new SegmentationEntryList();
                char label = list.getLabel();
                SegmentationPart firstSP = new SegmentationPart(smi.firstSP);
                SegmentationPart secondSP = new SegmentationPart(smi.secondSP);
                list.add(new SegmentationEntry(label, smi.match, firstSP));
                list.add(new SegmentationEntry(label, smi.match, secondSP));
                firstSP.setMatch(smi.match);
                secondSP.setMatch(smi.match);
                list.incrementLabel();
                segmentationSuggestions.add(list);
            }
            while (!newListStack.isEmpty()) {
                segmentationSuggestions.add(newListStack.pop());
            }
            if (segmentationSuggestions.size() > 500) { //Performance optimalisatie: prevents crashing of huge songs
                for (int i = 0; i<segmentationSuggestions.size(); i++){
                    calculateScore(segmentationSuggestions.get(i));
                }
                ArrayList<SegmentationEntryList> tempBestSuggestions = new ArrayList();
                for (int i = 0; i < segmentationSuggestions.size(); i++) {
                    SegmentationEntryList suggestion = segmentationSuggestions.get(i);
                    int j = 0;
                    while (j < tempBestSuggestions.size() && j <= 300) {
                        if (suggestion.getScore() > calculateScore(tempBestSuggestions.get(j))) {
                            SegmentationEntryList temp = tempBestSuggestions.get(j);
                            tempBestSuggestions.set(j, suggestion);
                            suggestion = temp;
                        }
                        j++;
                    }
                    if (tempBestSuggestions.size() <= 300) {
                        tempBestSuggestions.add(j, suggestion);
                    }
                }
                this.segmentationSuggestions = tempBestSuggestions;
            }
        }

        public void setTop5Segmentations(SegmentationPart parent) {
            ArrayList<SegmentationList> bestSegmentations;
            if (parent != null) {
                bestSegmentations = parent.getSubSegmentationSuggestions();
            } else {
                bestSegmentations = segmentation.getMacroSuggestions();
            }
            ArrayList<SegmentationEntryList> bestSegmentationLists = new ArrayList(5);
            for (int i = 0; i < segmentationSuggestions.size(); i++) {
                calculateScore(segmentationSuggestions.get(i));
                addSegmentationToTop5(bestSegmentationLists, segmentationSuggestions.get(i));
            }

            bestSegmentations.clear();
            for (int i = 0; i <= 4; i++) {
                if (bestSegmentationLists.size() > i) {
                    bestSegmentations.add(convertEntryListToSegmentationList(parent, bestSegmentationLists.get(i)));
                }
            }
        }

        private void addSegmentationToTop5(ArrayList<SegmentationEntryList> bestSegmentations, SegmentationEntryList segmentation) {
            int i = 0;
            while (i < bestSegmentations.size() && i <= 4) {
                if (segmentation.getScore() > bestSegmentations.get(i).getScore()) {
                    SegmentationEntryList temp = bestSegmentations.get(i);
                    bestSegmentations.set(i, segmentation);
                    segmentation = temp;
                }
                i++;
            }
            if (bestSegmentations.size() <= 5) {
                bestSegmentations.add(i, segmentation);
            }
        }

        private SegmentationList convertEntryListToSegmentationList(SegmentationPart parent, SegmentationEntryList list) {

            SegmentationList segmentationSuggestion = new SegmentationList(parent, currentSegmentationLevel);
            for (int i = 0; i < list.size(); i++) {
                SegmentationPart sp = new SegmentationPart(list.get(i).segmentationPart);
                sp.setLabel(String.valueOf(list.get(i).label));
                //list.get(i).segmentationPart.setLabel(String.valueOf(list.get(i).label));
                segmentationSuggestion.add(sp);
            }
            return segmentationSuggestion;
        }

        private float calculateScore(SegmentationEntryList list) {

            float avgMatch = 0;
            int amountOfDifferentSegments = 0; //aantal verschillende segmenten = aantal labels
            int amountOfParts = 0; //hoeveel segmenten zijn er -> macro < 10
            float coverage = 0f;
            int labelFreqTable[] = new int[26];
            for (int i = 0; i < 26; i++) {
                labelFreqTable[i] = 0;
            }
            for (int i = 0; i < list.size(); i++) {
                SegmentationEntry entry = list.get(i);
                avgMatch += (entry.match / (float) list.size());
                labelFreqTable[entry.label % 26]++;
                if (labelFreqTable[entry.label % 26] == 1) {
                    amountOfDifferentSegments++;
                }
                coverage += (entry.segmentationPart.getEnd() - entry.segmentationPart.getBegin()) / songDuration;
                amountOfParts++;
            }

            float score;
            if (avgMatch > 0.25) {
                if (currentSegmentationLevel == AASModel.MACRO_LEVEL) {
                    score = (float) ((avgMatch * 0.5 + coverage * 0.5) * (1 - 0.05 * (amountOfDifferentSegments - 1) - 0.01 * (amountOfParts - 2)));
                } else {
                    score = (float) (avgMatch * 0.57 + coverage * 0.43);
                }
            } else {
                score = 0;
            }
            list.setScore(score);
            return score;
        }
    }
}
