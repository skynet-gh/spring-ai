package skynet;

import skynet.SkynetAICljOO;

import com.springrts.ai.oo.AbstractOOAI;
import com.springrts.ai.oo.AIFloat3;
import com.springrts.ai.oo.OOAI;

import com.springrts.ai.oo.clb.OOAICallback;
import com.springrts.ai.oo.clb.Unit;
import com.springrts.ai.oo.clb.WeaponDef;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.List;

/**
 * Wrapper for Clojure since I can't get it to work with JNI.
 */
public class SkynetAIOO extends AbstractOOAI {

  private SkynetAICljOO clj;

  public SkynetAIOO() throws Exception {
    try {
      clj = new SkynetAICljOO();
    } catch (Throwable e) {
      File f = new File("./skynet-oo-fatal-error.log");
      PrintStream ps = new PrintStream(f);
      e.printStackTrace(ps);
      ps.close();
    }
  }

  @Override
  public int init(int skirmishAIId, OOAICallback callback) {
    callback.getLog().log("java init");
    try {
      return clj.init(skirmishAIId, callback);
    } catch (Throwable e) {
      callback.getLog().log("java init err: " + e.getMessage());

      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      String st = sw.toString();

      callback.getLog().log("java init err stacktrace: " + "\n" + st);
    }
    return -1;
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
  public int unitFinished(Unit unit) {
    return clj.unitFinished(unit);
  }

  @Override
  public int unitIdle(Unit unit) {
    return clj.unitIdle(unit);
  }

  @Override
  public int unitCreated(Unit unit, Unit builder) {
    return clj.unitCreated(unit, builder);
  }

  @Override
  public int unitMoveFailed(Unit unit) {
    return clj.unitMoveFailed(unit);
  }

  @Override
  public int unitDamaged(Unit unit, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
    return clj.unitDamaged(unit, attacker, damage, dir, weaponDef, paralyzer);
  }

  @Override
  public int unitDestroyed(Unit unit, Unit attacker) {
    return clj.unitDestroyed(unit, attacker);
  }

  @Override
  public int enemyDamaged(Unit enemy, Unit attacker, float damage, AIFloat3 dir, WeaponDef weaponDef, boolean paralyzer) {
    return clj.enemyDamaged(enemy, attacker, damage, dir, weaponDef, paralyzer);
  }

  @Override
  public int enemyDestroyed(Unit enemy, Unit attacker) {
    return clj.enemyDestroyed(enemy, attacker);
  }

  @Override
  public int enemyEnterRadar(Unit enemy) {
    return clj.enemyEnterRadar(enemy);
  }

  @Override
  public int enemyLeaveRadar(Unit enemy) {
    return clj.enemyLeaveRadar(enemy);
  }

  @Override
  public int enemyEnterLOS(Unit enemy) {
    return clj.enemyEnterLOS(enemy);
  }

  @Override
  public int enemyLeaveLOS(Unit enemy) {
    return clj.enemyLeaveLOS(enemy);
  }

  @Override
  public int enemyCreated(Unit enemy) {
    return clj.enemyCreated(enemy);
  }

  @Override
  public int enemyFinished(Unit enemy) {
    return clj.enemyFinished(enemy);
  }

  @Override
  public int weaponFired(Unit unit, WeaponDef weaponDef) {
    return clj.weaponFired(unit, weaponDef);
  }

  @Override
  public int playerCommand(List<Unit> units, int commandTopicId, int playerId) {
    return clj.playerCommand(units, commandTopicId, playerId);
  }

  @Override
  public int commandFinished(Unit unit, int commandId, int commandTopicId) {
    return clj.commandFinished(unit, commandId, commandTopicId);
  }

  @Override
  public int seismicPing(AIFloat3 pos, float strength) {
    return clj.seismicPing(pos, strength);
  }

  @Override
  public int message(int player, String message) {
    return clj.message(player, message);
  }
}
