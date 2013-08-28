package be.hogent.tarsos.tarsossegmenter.model.segmentation;

public class Segment {
	public float startTime, endTime;
	public String label;
	
	public Segment(){
	}
	
	public Segment(float begin, float end, String label){
		this.startTime = begin;
		this.endTime = end; 
		this.label = label;
	}
	
}
