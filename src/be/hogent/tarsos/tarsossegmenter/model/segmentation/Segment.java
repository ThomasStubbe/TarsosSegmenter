package be.hogent.tarsos.tarsossegmenter.model.segmentation;

import java.awt.Color;

public class Segment {
	public float startTime, endTime;
	public String label;
	public Color color;
	
	public Segment(){
	}
	
	public Segment(float begin, float end, String label, Color color){
		this.startTime = begin;
		this.endTime = end; 
		this.label = label;
		this.color = color;
	}
	
}
