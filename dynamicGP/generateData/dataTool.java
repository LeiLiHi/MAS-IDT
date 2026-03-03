package org.lileischeduler.dynamicGP.generateData;

import java.util.Arrays;

public class dataTool {
    // 地球半径（单位：km）
    private static final double EARTH_RADIUS = 6371.0;

    /**** 性能变量 ****/
    private static final double[] velocity = new double[3];
    // 使用静态成员保存工作数组
    private static final double[] workArray = new double[10];
    private static double[] rTarget = new double[3];

    public static void main(String[] args) {
        // 示例数据（经度、维度）
        double[] satPos1 = latLonToECEF(0.02, 0.0, EARTH_RADIUS + 500.0); // 当前时间卫星坐标
        double[] satPos2 = latLonToECEF(0.01, 0.0, EARTH_RADIUS + 500.0); // 下一秒卫星坐标（假设经度变化）

        double[] targetPos = latLonToECEF(-90.0, 0.0, EARTH_RADIUS); // 目标点坐标
        boolean isVisble = isTargetVisible(satPos1, targetPos);
        if (!isVisble) return;
        // 计算俯仰角和偏航角
        double[] angles = calculateAngles(satPos1, satPos2, targetPos);
        System.out.printf("俯仰角 (Pitch): %.4f°\n", angles[0]);
        System.out.printf("偏航角 (Yaw): %.4f°\n", angles[1]);
    }

    /**
     * 将经纬度和高度转换为地心固定坐标（ECEF）
     *
     * @param lat 纬度（单位：度）
     * @param lon 经度（单位：度）
     * @param radius 地心到点的距离（单位：km，地球半径 + 高度）
     * @return ECEF 坐标 [x, y, z]
     */
    public static double[] latLonToECEF(double lat, double lon, double radius) {
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);

        double x = radius * Math.cos(latRad) * Math.cos(lonRad);
        double y = radius * Math.cos(latRad) * Math.sin(lonRad);
        double z = radius * Math.sin(latRad);

        return new double[]{x, y, z};
    }

    /**
     * 计算俯仰角和偏航角
     *
     * @param satPos1 当前时间卫星的 ECEF 坐标 [x, y, z]
     * @param satPos2 下一秒卫星的 ECEF 坐标 [x, y, z]
     * @param targetPos 目标点的 ECEF 坐标 [x, y, z]
     * @return 俯仰角和偏航角 [pitch, yaw]（单位：度）
     */
    public static double[] calculateAngles(double[] satPos1, double[] satPos2, double[] targetPos) {
        // 计算速度矢量 （相机方位的参考）
        for (int i = 0; i < 3; i++) {
            velocity[i] = satPos2[i] - satPos1[i];
        }

        // 构建轨道坐标系
        double[] rAxis = normalize(satPos1); // R轴：地球半径方向
        double[] tAxis = normalize(velocity); // T轴：飞行切向方向
        double[] nAxis = crossProduct(rAxis, tAxis); // N轴：法向方向

        // 计算目标点矢量
        for (int i = 0; i < 3; i++) {
            rTarget[i] = targetPos[i] - satPos1[i];
        }
        rTarget = normalize(rTarget);
        // 计算目标方向在 R、T、N 轴上的夹角
        double angleT = Math.acos(dotProduct(tAxis, rTarget)); // 与T轴的夹角 与RN面的夹角的补交
        double angleN = Math.acos(dotProduct(nAxis, rTarget)); // 与N轴的夹角 与RT面的夹角的补交
        // 返回角度（转为度数）
        double[] angles = new double[3];
        angles[0] = 90 - Math.toDegrees(angleT);  //
        angles[1] = 90 - Math.toDegrees(angleN);  // 与
        return angles;
    }

    /**
     * 计算两个向量的点积
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 点积
     */
    public static double dotProduct(double[] vec1, double[] vec2) {
        double result = 0;
        for (int i = 0; i < vec1.length; i++) {
            result += vec1[i] * vec2[i];
        }
        return result;
    }

    /**
     * 计算两个向量的叉积
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 叉积结果向量
     */
    public static double[] crossProduct(double[] vec1, double[] vec2) {
        return new double[]{
                vec1[1] * vec2[2] - vec1[2] * vec2[1],
                vec1[2] * vec2[0] - vec1[0] * vec2[2],
                vec1[0] * vec2[1] - vec1[1] * vec2[0]
        };
    }

    /**
     * 判断目标点是否可见
     * @param satPos 卫星的 ECEF 坐标 [x, y, z]
     * @param targetPos 目标点的 ECEF 坐标 [x, y, z]
     * @return true 如果目标点可见，否则 false
     */
    public static boolean isTargetVisible(double[] satPos, double[] targetPos) {
        // 计算卫星到地心的距离
//        double rSat = Math.sqrt(satPos[0] * satPos[0] + satPos[1] * satPos[1] + satPos[2] * satPos[2]);

        // 计算目标点到地心的距离
//        double rTarget = Math.sqrt(targetPos[0] * targetPos[0] + targetPos[1] * targetPos[1] + targetPos[2] * targetPos[2]);

        // 计算卫星和目标点的方向余弦
        double dotProduct = satPos[0] * targetPos[0] + satPos[1] * targetPos[1] + satPos[2] * targetPos[2];
//        double cosTheta = dotProduct / (rSat * rTarget);

        // 如果 cosTheta < 0，目标点在地球背面
        return dotProduct > 0;
    }

    /**
     * @param vec 输入向量
     * @return 单位向量
     */
    public static double[] normalize(double[] vec) {
        int length = vec.length;
        double beforeNorm = 0;
        for (double v : vec) {
            beforeNorm += v * v;
        }
        double norm = Math.sqrt(beforeNorm);

        // 直接复用输入数组
        for (int i = 0; i < length; i++) {
            vec[i] /= norm;
        }
        return vec;
    }

    /**
     * 计算两点之间的欧式距离
     * @param point1        目标点1的坐标: x y z
     * @param point2        目标点2的坐标: x y z
     * @return              两点的欧式距离
     */
    public static double calculateDistance(double[] point1, double[] point2) {
        if (point1.length != point2.length) {
            throw new IllegalArgumentException("两个点的维度必须相同");
        }
        double sumSquares = 0;
        for (int i = 0; i < point1.length; i++) {
            double diff = point1[i] - point2[i];
            sumSquares += diff * diff;
        }
        return Math.sqrt(sumSquares);
    }

    /**
     * 计算原点到点a向量 和 a到b向量之间的夹角，单位为度
     * @param a 点a坐标，double数组目标
     * @param b 点b坐标，double数组卫星
     * @return 夹角（度）
     */
    public static double calculateAngle(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            throw new IllegalArgumentException("输入向量不能为空且长度必须相同");
        }

        // 使用workArray的不同区域
        int n = a.length;
        double[] oa = workArray;  // 前n个元素
        double[] ab = Arrays.copyOfRange(workArray, n, n * 2);
        for (int i = 0; i < n; i++) {
            oa[i] = a[i];
            ab[i] = b[i] - a[i];
        }

        double dotProduct = 0;
        double normOA = 0;
        double normAB = 0;

        for (int i = 0; i < n; i++) {
            dotProduct += oa[i] * ab[i];
            normOA += oa[i] * oa[i];
            normAB += ab[i] * ab[i];
        }

        normOA = Math.sqrt(normOA);
        normAB = Math.sqrt(normAB);

        if (normOA == 0 || normAB == 0) {
            throw new IllegalArgumentException("向量长度不能为0");
        }

        double cosTheta = dotProduct / (normOA * normAB);
        // 可能数值略微超出[-1,1]范围，限制一下
        cosTheta = Math.min(1.0, Math.max(-1.0, cosTheta));

        return Math.toDegrees(Math.acos(cosTheta));  // 返回度数
    }
}
