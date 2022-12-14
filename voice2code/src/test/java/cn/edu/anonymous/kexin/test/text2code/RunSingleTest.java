package cn.edu.anonymous.kexin.test.text2code;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import cn.edu.anonymous.kexin.text2code.Text2CompilationUnit;

public class RunSingleTest {
  private static boolean isDebug = true;

  public static void main(String[] args) throws IOException {
    RunSingleTest.isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
        getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
    String dir = System.getProperty("user.dir");
    Text2CompilationUnit text2CompilationUnit = new Text2CompilationUnit();
    String filePath = dir
        +
        // "/voice2code/src/test/java/cn/edu/lyun/kexin/test/text2code/testcases/If2.voiceJava";
        "/voice2code/src/test/java/cn/edu/lyun/kexin/test/text2code/all.voiceJava";
    filePath =
    // "/Users/stefanzan/Research/2021/voice2CodeInVoiceJava/util/ListHelper.voiceJava";
    // "/Users/stefanzan/Research/2021/voice2CodeInVoiceJava/util/Pair.voiceJava";
    // "/Users/stefanzan/Research/2021/voice2CodeInVoiceJava/util/StringHelper.voiceJava";
    "/Users/stefanzan/Research/2021/voice2CodeInVoiceJava/kexin/text2code/Text2CompilationUnit.voiceJava";
    BufferedReader br = new BufferedReader(new FileReader(filePath));
    int counter = 1;
    int debugLogStartLine = 21;
    for (String line; (line = br.readLine()) != null;) {
      System.out.println(counter + ": " + line);
      counter++;
      // skip empty line
      if (line.equals("")) {
        continue;
      }
      if(counter == debugLogStartLine){
        System.out.println("stop for inspection");
      }
      text2CompilationUnit.generate(line);

      if (RunSingleTest.isDebug && counter >= debugLogStartLine) {
        text2CompilationUnit.generatePNGofHoleAST();
      }
      if (!RunSingleTest.isDebug) {
        try {
          TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    br.close();

    text2CompilationUnit.generatePNGofHoleAST();
    System.out.println("done");
  }
}
