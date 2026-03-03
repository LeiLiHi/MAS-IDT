package org.lileischeduler.dynamicGP.solve.operator;

import org.lileischeduler.dynamicGP.model.Satellite;
import org.lileischeduler.dynamicGP.model.Target;

import java.util.List;

import static org.lileischeduler.tool.Tool.random;

public class Allocation {

    public static void allocate(List<Satellite> satellites, List<Target> targets, String strategy) {
        switch (strategy) {
            case "random":
                for (Target target : targets) {
                    // 随机选择一颗卫星
                    int index = random.nextInt(satellites.size());
                    Satellite satellite = satellites.get(index);
                    satellite.calculateWindows(target);
                    satellite.currentTargets.add(target);
                }
                break;
            case "benefit" :
                for (Satellite satellite : satellites) {

                }
                break;
        }

    }
}
