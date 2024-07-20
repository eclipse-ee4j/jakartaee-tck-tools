package tck.jakarta.platform.ant;

import org.apache.tools.ant.Target;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to hold information about the currently active package target dependencies that is being executed by
 * ant build.xml package target.
 */
public class TsPackageInfo extends TsBaseInfo {
    Target target;
    private List<TsTaskInfo> tsTaskInfos;

    public TsPackageInfo(Target target) {
        this.target = target;
    }

    public String getTargetName() {
        return target.getName();
    }

    public List<TsTaskInfo> getTsTaskInfos() {
        return tsTaskInfos;
    }

    public void addTaskInfo(TsTaskInfo tsTaskInfo) {
        if (tsTaskInfos == null) {
            tsTaskInfos = new ArrayList<>();
        }
        tsTaskInfos.add(tsTaskInfo);
    }

    public boolean hasTsVehicles() {
        boolean hasTsVehicles = false;
        if (tsTaskInfos != null) {
            for (TsTaskInfo tsTaskInfo : tsTaskInfos) {
                if (tsTaskInfo.getTaskName().equals("ts.vehicle")) {
                    hasTsVehicles = true;
                    break;
                }
            }
        }
        return hasTsVehicles;
    }
}
