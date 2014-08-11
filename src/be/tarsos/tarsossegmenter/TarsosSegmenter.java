package be.tarsos.tarsossegmenter;

import be.tarsos.tarsossegmenter.gui.TarsosSegmenterGui;
import be.tarsos.tarsossegmenter.util.configuration.Configuration;

public class TarsosSegmenter {
	public static void main(String[] args){
		Configuration.checkForConfigurationAndWriteDefaults();
        Configuration.configureDirectories();
		TarsosSegmenterGui.getInstance();
	}
}
