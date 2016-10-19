import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.cache.nodes.ItemNode;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.items.Item;

import com.esotericsoftware.minlog.Log;
import com.esotericsoftware.reflectasm.shaded.org.objectweb.asm.Label;
@ScriptManifest(author="AboutChedder",category=Category.COMBAT,description="Kills cows, and loots cowhide. Eats Trout in inventory. Will exit When health is less then half",name="TCombatCows",version=1.0)
public class Main extends AbstractScript{
	NPC selectedMob;
	String mobName;
	ArrayList<String> lootList; 
	NPC target;
	State state;
	Tile targetTile;
	HashMap<String, ArrayList<String>> mobLootTable = new HashMap();
	JFrame frame;
	/**
	 * @wbp.parser.entryPoint
	 */
	
	
	
	@Override
	public void onStart() {
		log("STARTED!!");
		ArrayList items = new ArrayList<String>();
		mobName = "Cow";
		items.add("Cowhide");
//		items.add("Death rune"); 
//		items.add("Law rune"); 
		mobLootTable.put(mobName, items);
		state = State.FINDING_ENEMY;
		if(lootList == null) lootList = mobLootTable.get(mobName);
		super.onStart();
	}
	private enum State{
		FINDING_ENEMY,
		ATTACKING_ENEMY,
		LOOTING
	}
	public interface RandomChoice{
		void choiceOne();
		void choiceTwo();
	}
	private void chooseRandom(int percent, RandomChoice rc){
		if(Calculations.random(1,100) > percent%101){
			rc.choiceOne();
		}else{
			rc.choiceTwo();
		}
	}
	public void loot(){
		log("Target dead?"+(target.getHealth()<=0));
//		if(getInventory().isFull()){
//			log("Invent is full");
//			state = State.FINDING_ENEMY;
//			return;
//		}
		if(target.getHealth()<=0){
			sleepUntil(() -> getGroundItems().closest(loot -> loot!= null && 
					(loot.getName().equals(lootList.get(0))
					|| loot.getName().equals(lootList.get(1)) ) )!= null, 1000);
			for(String itemName:lootList){
				log(String.format("Looking for item %s", itemName));				
				GroundItem item = getGroundItems().closest(loot -> loot!= null && loot.getName().equals(itemName));	
				if(item != null){
					int initialAmt = getInventory().get(itemName).getAmount();
					log(String.format("Item %s found!",itemName));
					item.interact("Take");
					sleepUntil(() -> initialAmt < getInventory().get(itemName).getAmount(),2000);	
				}
			}
			state = State.FINDING_ENEMY;
		}
	}

	public void findMobs(){
		if(!getLocalPlayer().isInCombat()){
			log("FINDING ENEMY");
			target = getNpcs().closest(e -> e.canAttack() && e.getName().equals(mobName));
			log("Found Enemy "+target.getName());
			state = State.ATTACKING_ENEMY;	
		}
	}
	public void attack(){
			if(target != null && !getLocalPlayer().isInCombat() && target.getHealth()>0){
//				if(Calculations.random(1,3) == 2){
//					target.interact();	
//				}else{
//					target.interactForceRight("Attack");
//				}
				chooseRandom(new Random().nextInt(99)+1, new RandomChoice() {
					
					@Override
					public void choiceTwo() {
						target.interact();
						
					}
					
					@Override
					public void choiceOne() {
						target.interactForceRight("Attack");
						
					}
				});
				targetTile = target.getTile();
				log("target tile null: "+(target.getTile() == null));
			}
			if(target.getHealth() <=0)
				state = State.LOOTING;	
	}
	
	public void rotateCamera(){
		log("rotating Camera");
		int cameraYaw = getCamera().getYaw();
		int cameraPitch = getCamera().getPitch();
		
	 	int pitchRandom = new Random().nextInt(200);
		int yawRandom = new Random().nextInt(750);
		
		if(new Random().nextInt(2) == 1){
			cameraPitch+=pitchRandom;
		}else{
			cameraPitch-=pitchRandom;
		}
		if(new Random().nextInt(2) == 1){
			cameraYaw+=yawRandom;			
		}else{
			cameraYaw-=yawRandom;
		}
		int camState = new Random().nextInt(3);
		if(camState == 0){
			getCamera().rotateToYaw(cameraYaw);
			getCamera().rotateToPitch(cameraPitch);	
		}else if(camState == 1){
			getCamera().rotateToYaw(cameraYaw);
		}else{
			getCamera().rotateToPitch(cameraPitch);
		}

	}
	public void eat(String foodName){
		if(getCombat().getHealthPercent() <=(45+Calculations.random(1,10))){
			log("eating "+foodName);
			Item food = getInventory().get(foodName);
			if(food != null){
				food.interact();
			}else{
				log("OUT OF FOOD!");
				stop();
			}
		}
	}
//	public State getState(){
//		if(lootList != null)
//			for(String itemName: lootList)
//				getGroundItems().closest(loot -> loot!= null && loot.getName().equals(itemName));
//			
//		if(!getLocalPlayer().isAnimating())
//		{
//			return State.FINDING_ENEMY;
//		}
//		else{
//			return State.ATTACKING_ENEMY;
//		}
//	}
	@Override
	public int onLoop() {
		eat("Trout");
		switch(state){
			case FINDING_ENEMY:
					findMobs();
				break;
			case ATTACKING_ENEMY:
				if(!getLocalPlayer().isInCombat())
				attack();
				break;
			case LOOTING:
				loot();
				
				break;
		}
//		1 in 20 chance of rotating the camera
		if(new Random().nextInt(100)<5){
			rotateCamera();
		}
		return Calculations.random(500, 2000);
	}

}
