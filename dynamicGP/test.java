package org.lileischeduler.dynamicGP;


import org.lileischeduler.AlgorithmConfig;
import org.nd4j.shade.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class test {
    public static void main(String[] args) {
        try {
            InputStream inputStream = new FileInputStream("config.yaml");

            Yaml yaml = new Yaml();
            AlgorithmConfig config = yaml.loadAs(inputStream, AlgorithmConfig.class);

            AlgorithmConfig.Algorithm algorithm = config.getAlgorithms().get("algorithm1");

            System.out.println("Algorithm Name: " + algorithm.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

