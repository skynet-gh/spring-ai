package skynet;

import skynet.SkynetAIClj;

import com.springrts.ai.AbstractAI;
import com.springrts.ai.AI;
import com.springrts.ai.AICallback;

import java.io.File;
import java.io.PrintStream;

/**
 * Wrapper for Clojure since I can't get it to work with JNI.
 */
public class SkynetAI extends AbstractAI implements AI {

  private SkynetAIClj clj;

  public SkynetAI() throws Exception {
    try {
      clj = new SkynetAIClj();
    } catch (Throwable e) {
      File f = new File("./skynet-fatal-error.log");
      PrintStream ps = new PrintStream(f);
      e.printStackTrace(ps);
      ps.close();
    }
  }

  @Override
  public int init(int skirmishAIId, AICallback callback) {
    return clj.init(skirmishAIId, callback);
  }

  @Override
  public int update(int frame) {
    return clj.update(frame);
  }

  @Override
  public int release(int reason) {
    return clj.release(reason);
  }

  @Override
  public int unitFinished(int unit) {
    return clj.unitFinished(unit);
  }
}
