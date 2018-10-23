package guepardoapps.heartratemonitor;

import java.io.FileNotFoundException;

/**
 * Created by LBW on 2018/3/13.
 */

public class Factory {
    public static HeartRateMonitorView getHeartView() throws FileNotFoundException {
        return new HeartRateMonitorView();
    }
}
