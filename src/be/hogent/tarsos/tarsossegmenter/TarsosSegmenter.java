package be.hogent.tarsos.tarsossegmenter;

import be.hogent.tarsos.tarsossegmenter.gui.TarsosSegmenterGui;
import be.hogent.tarsos.tarsossegmenter.util.configuration.Configuration;

public class TarsosSegmenter {
	public static void main(String[] args){
		Configuration.checkForConfigurationAndWriteDefaults();
        Configuration.configureDirectories();
		TarsosSegmenterGui.getInstance();
	}
}
