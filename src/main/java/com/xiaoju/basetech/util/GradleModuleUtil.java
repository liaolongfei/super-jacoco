package com.xiaoju.basetech.util;

/**
 * @description:
 * @author: gaoweiwei_v
 * @time: 2019/8/27 4:17 PM
 */

import com.xiaoju.basetech.entity.CoverageReportEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.xiaoju.basetech.util.Constants.*;

@Component
@Slf4j
public class GradleModuleUtil {

    public boolean isGradle(String rootPath) {
        try {
            String buildPath = rootPath + "/build.gradle";
            if (new File(buildPath).exists()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("识别是否为Gradle项目出现异常:{}", rootPath + "/build.gradle", e);
            return false;
        }
    }



    public void gradleBuild(CoverageReportEntity coverageReport) {
        String logFile = coverageReport.getLogFile().replace(LocalIpUtils.getTomcatBaseUrl()+"logs/", LOG_PATH);
        String[] compileCmd = new String[]{"cd " + coverageReport.getNowLocalPath() + " && gradle build -x test --info " + ">> " + logFile};
        long s = System.currentTimeMillis();
        try {
            int exitCode = CmdExecutor.executeCmd(compileCmd, 600000L);
            log.info("uuid={} 编译计算耗时：{}  exitCode：{}", coverageReport.getUuid(), (System.currentTimeMillis() - s),exitCode);

            if (exitCode != 0) {
                coverageReport.setRequestStatus(Constants.JobStatus.COMPILING.val());
                coverageReport.setErrMsg("gradle build出现异常");
            } else {
                coverageReport.setRequestStatus(JobStatus.COMPILE_DONE.val());
            }
        } catch (TimeoutException e) {
            coverageReport.setRequestStatus(JobStatus.COMPILE_FAIL.val());
            coverageReport.setErrMsg("gradle build代码超过了10分钟");
        } catch (Exception e) {
            coverageReport.setErrMsg("gradle build代码发生未知错误");
            coverageReport.setRequestStatus(JobStatus.COMPILE_FAIL.val());
        }
    }

    public ArrayList<String> gradleProjects(String pomPath) {
        ArrayList<String> validModuleList = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(pomPath));
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = reader.readLine()) != null) {
                sb.append(s.trim()+"\n");
            }
            String pomStr = sb.toString();
            String moduleregex = "include(.*)";
            List<String> allMatches = new ArrayList<String>();
            Matcher match = Pattern.compile(moduleregex)
                    .matcher(pomStr);
            while (match.find()) {
                String modules = match.group();
                modules = modules.replaceAll("include", "");
                modules = modules.replaceAll(" ", "");
                modules = modules.replaceAll("\"", "");
                modules = modules.replaceAll("\'", "");
                String[] module = modules.split(",");
                for (String m : module) {
                    if (!m.equals("")) {
                        validModuleList.add(m);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return validModuleList;
    }

    public static void main(String[] args) {
        GradleModuleUtil util = new GradleModuleUtil();
        String path = "D:\\taishan\\gradlesettings\\settings3.gradle";
        List l = util.gradleProjects(path);
        System.out.println(l);
    }

}