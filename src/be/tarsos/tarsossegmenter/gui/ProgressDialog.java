package be.tarsos.tarsossegmenter.gui;

/**
 * <p> A progressDialog which pops up when long lasting tasks take place. </p>
 *
 * @author Joren Six
 */
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;

public class ProgressDialog extends JDialog {

    private final int concurrentTasks;
    private final List<BackgroundTask> taskQueue;
    private final List<BackgroundTask> runningTasks;

    public ProgressDialog(String title, BackgroundTask bgTask, final List<BackgroundTask> detectorQueue) {
        super(TarsosSegmenterGui.getInstance(), title, true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(400, 120));
        taskQueue = detectorQueue;
        runningTasks = new ArrayList();
        concurrentTasks = Runtime.getRuntime().availableProcessors();

        getContentPane().setLayout(new GridLayout(0, 1));
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(TarsosSegmenterGui.getInstance());

        bgTask.addHandler(new BackgroundTask.TaskHandler() {

            @Override
            public void taskDone(BackgroundTask backgroundTask) {
                startOtherTasks();
            }

            @Override
            public void taskInterrupted(BackgroundTask backgroundTask, Exception e) {
                //transcoding failed => interrupt the queue
                setVisible(false);
            }
        });

        getContentPane().add(bgTask.ui());

        for (BackgroundTask detectorTask : detectorQueue) {
            detectorTask.addHandler(handler);
            getContentPane().add(detectorTask.ui());
        }
        bgTask.execute();
    }
    private BackgroundTask.TaskHandler handler = new BackgroundTask.TaskHandler() {

        @Override
        public void taskDone(BackgroundTask backgroundTask) {
            stopTask(backgroundTask);
            startNextInQueue();
        }

        @Override
        public void taskInterrupted(BackgroundTask backgroundTask, Exception e) {
        }
    };

    public synchronized void startOtherTasks() {
        for (int i = 0; i < concurrentTasks && taskQueue.size() > 0; i++) {
            startTask(taskQueue.get(0));
        }
        //empty queue => hide dialog
        if (taskQueue.isEmpty() && runningTasks.isEmpty()) {
            setVisible(false);
        }
    }

    private synchronized void startTask(BackgroundTask backgroundTask) {
        taskQueue.remove(backgroundTask);
        backgroundTask.execute();
        runningTasks.add(backgroundTask);
    }

    private synchronized void stopTask(BackgroundTask backgroundTask) {
        runningTasks.remove(backgroundTask);
        if (runningTasks.isEmpty()) {
            //all tasks finished
            firePropertyChange("allTasksFinished", false, true);
            //hide
            setVisible(false);
        }
    }

    private synchronized void startNextInQueue() {
        if (taskQueue.size() > 0) {
            startTask(taskQueue.get(0));
        }
    }
}
